package com.zeno.dialer

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.CallLog
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeno.dialer.data.Contact
import com.zeno.dialer.data.BlockedNumbersRepo
import com.zeno.dialer.data.ContactsRepo
import com.zeno.dialer.data.FavoritesRepo
import com.zeno.dialer.data.FilterMode
import com.zeno.dialer.data.RecentsDiskCache
import com.zeno.dialer.data.RecentsRepo
import com.zeno.dialer.data.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@androidx.compose.runtime.Immutable
data class DialerUiState(
    val query: String = "",
    val cursorPos: Int = 0,
    val results: List<Contact> = emptyList(),
    val keypadContactMatch: Contact? = null,
    /** -1 = no row highlighted (touch/trackpad scroll); D-pad moves to 0+ */
    val selectedIndex: Int = -1,
    /**
     * Trackpad/scroll focus for the Calls list.
     * When `selectedIndex == -1`, we still want Call key to dial the currently
     * visible row (e.g. "Calls" tab without a click).
     */
    val scrollFocusedIndex: Int = -1,
    val filterMode: FilterMode = FilterMode.ALL,
    val pinnedFavorites: List<Contact> = emptyList(),
    val favorites: List<Contact> = emptyList(),
    val favoriteSuggestions: List<Contact> = emptyList(),
    val blockedNumbers: Set<String> = emptySet(),
    val currentTabIndex: Int = 0,
    val isKeypadActive: Boolean = false,
    /** -1 = list is focused; 0 = Settings tile; 1+ = pinned contact tiles */
    val favoriteFocusIndex: Int = -1,
) {
    val displayQuery: String
        get() = if (query.isEmpty()) ""
        else query.substring(0, cursorPos) + "|" + query.substring(cursorPos)
}

@OptIn(FlowPreview::class)
class DialerViewModel(application: Application) : AndroidViewModel(application) {

    private val contactsRepo = ContactsRepo(application)
    private val recentsRepo = RecentsRepo(application)
    private val favoritesRepo = FavoritesRepo(application)
    private val blockedNumbersRepo = BlockedNumbersRepo(application)
    private val searchEngine = SearchEngine(contactsRepo, recentsRepo)

    private val _uiState = MutableStateFlow(DialerUiState())

    private val callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            recentsRepo.clearLookupCache()
            forceRefresh()
        }
    }
    val uiState: StateFlow<DialerUiState> = _uiState.asStateFlow()

    private val _callEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val callEvent: SharedFlow<String> = _callEvent.asSharedFlow()

    private val _contactsScrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val contactsScrollToTopEvent: SharedFlow<Unit> = _contactsScrollToTopEvent.asSharedFlow()

    private val _toggleExpandEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val toggleExpandEvent: SharedFlow<Int> = _toggleExpandEvent.asSharedFlow()

    /**
     * When we place a call, the call log can update quickly and trigger `forceRefresh()`,
     * which clears selection. Keep selection highlighted briefly so the user doesn’t
     * see focus jump away immediately after pressing Dial.
     */
    private var lastDialEventMs: Long = 0L

    fun toggleExpandSelected() {
        val idx = _uiState.value.selectedIndex
        if (idx >= 0) _toggleExpandEvent.tryEmit(idx)
    }

    /** Emits an expand/collapse toggle event for a specific row index. */
    fun toggleExpandIndex(index: Int) {
        if (index >= 0) _toggleExpandEvent.tryEmit(index)
    }

    private var callLogObserverRegistered = false

    /** Tracks the last style we applied a default tab for, to detect theme changes. -1 = never applied. */
    private var lastDefaultTabStyle: Int = -1

    /** True when Pixel theme is active — used by KeyHandler to route tab-0 correctly. */
    val isPixelTheme: Boolean
        get() = lastDefaultTabStyle == AppPreferences.DIALER_STYLE_PIXEL

    /**
     * Apply the theme-appropriate default tab.
     * Pixel → Home (index 1), Classic → Calls (index 0).
     * Only changes the tab if the style has changed since we last applied it,
     * or if this is the very first call (cold start).
     */
    fun applyDefaultTabForStyle(style: Int) {
        if (style == lastDefaultTabStyle) return   // style unchanged, user may have navigated — don't override
        lastDefaultTabStyle = style
        val defaultIndex = if (style == AppPreferences.DIALER_STYLE_PIXEL) 1 else 0
        _uiState.update { it.copy(currentTabIndex = defaultIndex) }
    }

    init {
        // ── Instant startup: show disk-cached recents on the very first frame ──
        val diskCached = RecentsDiskCache.load(getApplication())
        if (diskCached.isNotEmpty()) {
            _uiState.update { it.copy(results = diskCached) }
        }

        // ── Background refresh: fetch live data and update + persist ────────
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contactsRepo.search("")           // warm contacts index in parallel
                val fresh = searchEngine.search("", FilterMode.ALL)
                _uiState.update { it.copy(results = fresh) }
                RecentsDiskCache.save(getApplication(), fresh)
            } catch (_: SecurityException) { }
        }

        // Reactive search with collectLatest for automatic cancellation
        viewModelScope.launch {
            _uiState
                .map { it.query to it.filterMode }
                .distinctUntilChanged()
                .debounce(40L)
                .collectLatest { (query, mode) ->
                    val (results, keypadMatch) = try {
                        withContext(Dispatchers.Default) {
                            val r = searchEngine.search(query, mode)
                            val k = searchEngine.keypadContactMatch(query, mode, r)
                            r to k
                        }
                    } catch (_: SecurityException) {
                        emptyList<Contact>() to null
                    }
                    _uiState.update {
                        it.copy(
                            results = results,
                            selectedIndex = -1,
                            scrollFocusedIndex = -1,
                            keypadContactMatch = keypadMatch
                        )
                    }
                }
        }

        if (hasCallLogPermission()) {
            registerCallLogObserver()
            refreshFavorites()
        }
        refreshBlockedNumbers()
    }

    fun onPermissionsReady() {
        registerCallLogObserver()
        viewModelScope.launch(Dispatchers.IO) {
            try { contactsRepo.search("") } catch (_: SecurityException) { }
            blockedNumbersRepo.syncFromSystem()
            refreshBlockedNumbers()
        }
        refreshFavorites()
        refreshBlockedNumbers()
    }

    private fun registerCallLogObserver() {
        if (callLogObserverRegistered) return
        try {
            getApplication<Application>().contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI, true, callLogObserver
            )
            callLogObserverRegistered = true
        } catch (_: SecurityException) { }
    }

    private fun hasCallLogPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

    // ── Query editing ────────────────────────────────────────────────────────

    fun typeUnicode(code: Int): Boolean {
        if (code <= 0) return false
        val char = code.toChar()
        if (char.isISOControl()) return false
        CallStateHolder.tryPlayDtmfOnPrimaryCall(char)
        val state = _uiState.value
        val newQuery = state.query.substring(0, state.cursorPos) + char +
                state.query.substring(state.cursorPos)
        _uiState.update { it.copy(query = newQuery, cursorPos = state.cursorPos + 1) }
        return true
    }

    fun deleteChar() {
        val state = _uiState.value
        if (state.cursorPos == 0) return
        val newQuery = state.query.substring(0, state.cursorPos - 1) +
                state.query.substring(state.cursorPos)
        _uiState.update { it.copy(query = newQuery, cursorPos = state.cursorPos - 1) }
    }

    fun nudgeCursorLeft()  = _uiState.update { it.copy(cursorPos = (it.cursorPos - 1).coerceAtLeast(0)) }
    fun nudgeCursorRight() = _uiState.update { it.copy(cursorPos = (it.cursorPos + 1).coerceAtMost(it.query.length)) }

    fun clearQuery() {
        _uiState.update {
            it.copy(
                query = "",
                cursorPos = 0,
                selectedIndex = -1,
                scrollFocusedIndex = -1,
                keypadContactMatch = null
            )
        }
    }

    fun setQueryDirect(text: String) {
        _uiState.update {
            it.copy(
                query = text,
                cursorPos = text.length,
                selectedIndex = -1,
                scrollFocusedIndex = -1
            )
        }
    }

    // ── Selection ────────────────────────────────────────────────────────────

    /**
     * Contacts tab list order matches [ContactsContent]: non-recent rows only; when the query
     * is blank, sorted by [Contact.name] case-insensitive — not raw [DialerUiState.results] order.
     * Returns result indices in that visual order, or null when not on Contacts.
     */
    private fun buildContactsNavResultIndices(state: DialerUiState): List<Int>? {
        if (state.currentTabIndex != 1 || state.filterMode != FilterMode.CONTACTS) return null
        val filtered = state.results.filter { !it.isRecent }
        if (filtered.isEmpty()) return emptyList()
        val ordered = if (state.query.isNotBlank()) {
            filtered
        } else {
            filtered.sortedWith(
                compareBy<Contact> { it.name.lowercase(Locale.ROOT) }
                    .thenBy { it.id }
                    .thenBy { it.number }
            )
        }
        return ordered.map { c -> resultIndexForContact(state, c) }
    }

    private fun resultIndexForContact(state: DialerUiState, target: Contact): Int {
        val exact = state.results.indexOfFirst {
            !it.isRecent && it.id == target.id && it.number == target.number
        }
        if (exact >= 0) return exact
        return state.results.indexOfFirst { !it.isRecent && it.number == target.number }
    }

    private fun effectiveSelectionIndex(state: DialerUiState): Int? = when {
        state.selectedIndex >= 0 -> state.selectedIndex
        state.scrollFocusedIndex >= 0 -> state.scrollFocusedIndex
        else -> null
    }

    /** True when selection is on the first row in Contacts visual order (not necessarily results[0]). */
    fun isContactsNavAtStart(state: DialerUiState): Boolean {
        val nav = buildContactsNavResultIndices(state) ?: return false
        if (nav.isEmpty()) return false
        val cur = effectiveSelectionIndex(state) ?: return false
        return cur == nav[0]
    }

    fun nudgeSelectionUp() {
        _uiState.update { state ->
            if (state.results.isEmpty()) return@update state
            val nav = buildContactsNavResultIndices(state)
            if (nav != null && nav.isNotEmpty()) {
                val cur = effectiveSelectionIndex(state) ?: return@update state
                var navPos = nav.indexOfFirst { it == cur }
                if (navPos < 0) {
                    val c = state.results.getOrNull(cur) ?: return@update state
                    navPos = nav.indexOfFirst { ri ->
                        val rc = state.results.getOrNull(ri)
                        rc != null && rc.id == c.id && rc.number == c.number
                    }
                }
                if (navPos < 0) return@update state
                val newIdx = if (navPos <= 0) nav[0] else nav[navPos - 1]
                return@update state.copy(selectedIndex = newIdx, scrollFocusedIndex = newIdx)
            }
            val base = effectiveSelectionIndex(state) ?: return@update state
            val next = (base - 1).coerceAtLeast(0)
            state.copy(selectedIndex = next, scrollFocusedIndex = next)
        }
    }

    fun nudgeSelectionDown() {
        _uiState.update { state ->
            val max = (state.results.size - 1).coerceAtLeast(0)
            if (state.results.isEmpty()) {
                return@update state.copy(selectedIndex = -1, scrollFocusedIndex = -1)
            }
            val nav = buildContactsNavResultIndices(state)
            if (nav != null && nav.isNotEmpty()) {
                val cur = effectiveSelectionIndex(state)
                if (cur == null) {
                    val first = nav[0]
                    return@update state.copy(selectedIndex = first, scrollFocusedIndex = first)
                }
                var navPos = nav.indexOfFirst { it == cur }
                if (navPos < 0) {
                    val c = state.results.getOrNull(cur) ?: return@update state
                    navPos = nav.indexOfFirst { ri ->
                        val rc = state.results.getOrNull(ri)
                        rc != null && rc.id == c.id && rc.number == c.number
                    }
                }
                if (navPos < 0) return@update state
                val newIdx = if (navPos >= nav.lastIndex) nav[nav.lastIndex] else nav[navPos + 1]
                return@update state.copy(selectedIndex = newIdx, scrollFocusedIndex = newIdx)
            }
            val base = effectiveSelectionIndex(state) ?: -1
            val next = when {
                base < 0 -> 0
                else -> base + 1
            }.coerceAtMost(max)
            state.copy(selectedIndex = next, scrollFocusedIndex = next)
        }
    }

    fun selectItem(index: Int) {
        _uiState.update { it.copy(selectedIndex = index, scrollFocusedIndex = index) }
    }

    /** Selects the first contact in Contacts *visual* order and scrolls the list to the top. */
    fun scrollContactsToTop() {
        _uiState.update { state ->
            val nav = buildContactsNavResultIndices(state)
            val idx = nav?.firstOrNull()?.takeIf { it >= 0 } ?: 0
            state.copy(selectedIndex = idx, scrollFocusedIndex = idx)
        }
        _contactsScrollToTopEvent.tryEmit(Unit)
    }

    fun setFavoriteFocus(index: Int) {
        _uiState.update { it.copy(favoriteFocusIndex = index, selectedIndex = -1) }
    }

    /** Navigate DOWN through the Pixel Favorites suggestions list. */
    fun nudgeFavoriteSuggestionDown() {
        _uiState.update { state ->
            val max = state.favoriteSuggestions.lastIndex
            if (max < 0) return@update state
            val next = (state.favoriteFocusIndex + 1).coerceAtMost(max)
            state.copy(favoriteFocusIndex = next, selectedIndex = -1)
        }
    }

    /** Navigate UP through the Pixel Favorites suggestions list. */
    fun nudgeFavoriteSuggestionUp() {
        _uiState.update { state ->
            val next = state.favoriteFocusIndex - 1
            state.copy(favoriteFocusIndex = if (next < 0) -1 else next, selectedIndex = -1)
        }
    }

    fun setScrollFocusedIndex(index: Int) {
        // Clear selectedIndex so the CALL key falls through to scrollFocusedIndex instead of
        // dialing a stale D-pad selection that is no longer visible.
        _uiState.update { it.copy(selectedIndex = -1, scrollFocusedIndex = index) }
    }

    // ── Filter ───────────────────────────────────────────────────────────────

    fun setFilter(mode: FilterMode) {
        _uiState.update { it.copy(filterMode = mode, selectedIndex = -1, scrollFocusedIndex = -1) }
    }

    fun cycleFilter() {
        val next = when (_uiState.value.filterMode) {
            FilterMode.ALL      -> FilterMode.MISSED
            FilterMode.MISSED   -> FilterMode.RECEIVED
            FilterMode.RECEIVED -> FilterMode.RECENTS
            FilterMode.RECENTS  -> FilterMode.ALL
            FilterMode.CONTACTS -> FilterMode.ALL
        }
        _uiState.update { it.copy(filterMode = next, selectedIndex = -1, scrollFocusedIndex = -1) }
    }

    fun setFilterContacts() = setFilter(FilterMode.CONTACTS)
    fun setFilterRecents()  = setFilter(FilterMode.RECENTS)

    // ── Tab state ─────────────────────────────────────────────────────────────

    @Volatile
    var isOnKeypad = false
        private set

    fun setKeypadActive(active: Boolean) {
        isOnKeypad = active
        _uiState.update { it.copy(isKeypadActive = active) }
    }

    val currentTabIndex: Int
        get() = _uiState.value.currentTabIndex

    fun setCurrentTab(index: Int) {
        val clamped = index.coerceIn(0, 2)
        _uiState.update { state ->
            val filter =
                if (clamped == 0 && state.filterMode == FilterMode.CONTACTS) FilterMode.ALL
                else state.filterMode
            state.copy(
                currentTabIndex = clamped,
                selectedIndex = -1,
                scrollFocusedIndex = -1,
                favoriteFocusIndex = -1,
                filterMode = filter
            )
        }
        isOnKeypad = clamped == 2
        _uiState.update { it.copy(isKeypadActive = isOnKeypad) }
    }

    fun moveTabLeft() = setCurrentTab(currentTabIndex - 1)
    fun moveTabRight() = setCurrentTab(currentTabIndex + 1)

    // ── Live refresh ─────────────────────────────────────────────────────────

    private var refreshJob: Job? = null

    private fun forceRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val results = searchEngine.searchAsync(state.query, state.filterMode)
            val keypadMatch = searchEngine.keypadContactMatch(state.query, state.filterMode, results)
            val keepSelection =
                (lastDialEventMs != 0L) && (SystemClock.elapsedRealtime() - lastDialEventMs < 3_000L)
            _uiState.update {
                it.copy(
                    results = results,
                    selectedIndex = if (keepSelection) it.selectedIndex else -1,
                    scrollFocusedIndex = if (keepSelection) it.scrollFocusedIndex else -1,
                    keypadContactMatch = keypadMatch
                )
            }
            // Persist for next cold start
            if (state.query.isBlank() && state.filterMode == FilterMode.ALL) {
                RecentsDiskCache.save(getApplication(), results)
            }
        }
        refreshFavorites()
        refreshBlockedNumbers()
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(callLogObserver)
    }

    // ── Call history ─────────────────────────────────────────────────────────

    fun deleteCallLogForNumber(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.delete(
                    CallLog.Calls.CONTENT_URI,
                    "${CallLog.Calls.NUMBER} = ?",
                    arrayOf(number)
                )
            } catch (_: Exception) { }
        }
    }

    fun clearCallHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.delete(
                    CallLog.Calls.CONTENT_URI, null, null
                )
            } catch (_: Exception) { }
        }
        refreshFavorites()
    }

    fun addBlockedNumber(number: String) {
        blockedNumbersRepo.add(number)
        refreshBlockedNumbers()
    }

    fun removeBlockedNumber(number: String) {
        blockedNumbersRepo.remove(number)
        refreshBlockedNumbers()
    }

    /** Toggle from UI — uses repo as source of truth (avoids stale [DialerUiState] in sheet callbacks). */
    fun toggleBlockedContact(contact: Contact) {
        if (contact.number.isBlank()) return
        if (blockedNumbersRepo.contains(contact.number)) removeBlockedNumber(contact.number)
        else addBlockedNumber(contact.number)
    }

    fun isBlocked(number: String): Boolean = blockedNumbersRepo.contains(number)

    private fun refreshBlockedNumbers() {
        _uiState.update { it.copy(blockedNumbers = blockedNumbersRepo.getAll()) }
    }

    fun pinFavorite(contact: Contact) {
        favoritesRepo.add(contact.number)
        refreshFavorites()
    }

    fun unpinFavorite(number: String) {
        favoritesRepo.remove(number)
        refreshFavorites()
    }

    private fun refreshFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pinnedNumbers = favoritesRepo.getPinnedNumbers()
                val starredContacts = contactsRepo.getStarredContacts()
                val recents = searchEngine.getTopRecents(20)
                val contacts = contactsRepo.search("")
                val pool = (recents + contacts).distinctBy { normalize(it.number) }

                val appPinned = pinnedNumbers.map { pinnedNum ->
                    pool.firstOrNull { normalize(it.number) == normalize(pinnedNum) }
                        ?: Contact(name = pinnedNum, number = pinnedNum)
                }

                val pinned = (starredContacts + appPinned)
                    .distinctBy { normalize(it.number) }

                val pinnedDigits = HashSet<String>(pinned.size)
                for (p in pinned) pinnedDigits.add(normalize(p.number))

                val suggestions = ArrayList<Contact>(12)
                for (r in recents) {
                    if (suggestions.size >= 12) break
                    if (normalize(r.number) !in pinnedDigits) suggestions.add(r)
                }

                val combined = (pinned + suggestions).distinctBy { normalize(it.number) }.take(12)

                _uiState.update {
                    it.copy(
                        pinnedFavorites = pinned,
                        favorites = combined,
                        favoriteSuggestions = suggestions
                    )
                }
            } catch (_: SecurityException) {
                _uiState.update {
                    it.copy(
                        pinnedFavorites = emptyList(),
                        favorites = emptyList(),
                        favoriteSuggestions = emptyList()
                    )
                }
            }
        }
    }

    private fun normalize(number: String): String = number.filter { it.isDigit() }

    // ── Calling ──────────────────────────────────────────────────────────────

    fun callSelected() {
        val state = _uiState.value
        val number = when {
            state.selectedIndex >= 0 && state.results.isNotEmpty() && state.selectedIndex < state.results.size ->
                state.results[state.selectedIndex].number
            state.scrollFocusedIndex >= 0 &&
                state.results.isNotEmpty() &&
                state.scrollFocusedIndex < state.results.size ->
                state.results[state.scrollFocusedIndex].number
            // Prefer top search result over raw query text — raw query is only a phone number
            // when there are no matches (e.g. user typed a full number with no contacts found).
            state.results.isNotEmpty() -> state.results[0].number
            state.query.isNotBlank() -> state.query
            else -> return
        }
        Log.i(
            "ZenoDialer",
            "callSelected selectedIndex=${state.selectedIndex} scrollFocusedIndex=${state.scrollFocusedIndex} query='${state.query}' -> dialing='$number'"
        )
        emitCall(number)
    }

    fun callItem(index: Int) {
        val results = _uiState.value.results
        if (index < results.size) emitCall(results[index].number)
    }

    fun callNumber(number: String) = emitCall(number)

    private fun emitCall(number: String) {
        lastDialEventMs = SystemClock.elapsedRealtime()
        viewModelScope.launch { _callEvent.emit(number) }
    }

    /**
     * Clears list focus and the post-dial selection retention window ([lastDialEventMs] /
     * [forceRefresh] keepSelection) so the hardware Call key does not redial the previous
     * row after a call ends.
     */
    fun resetCallKeyTargetState() {
        lastDialEventMs = 0L
        _uiState.update {
            it.copy(selectedIndex = -1, scrollFocusedIndex = -1)
        }
    }
}
