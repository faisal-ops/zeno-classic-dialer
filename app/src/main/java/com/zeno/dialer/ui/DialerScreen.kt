package com.zeno.dialer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.telecom.Call
import com.zeno.dialer.ActiveCallInfo
import com.zeno.dialer.CallStateHolder
import com.zeno.dialer.InCallActivity
import com.zeno.dialer.SettingsActivity
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import com.zeno.dialer.ui.theme.LocalDialerColors
import com.zeno.dialer.ui.theme.LocalDialerMotion
import com.zeno.dialer.ui.theme.LocalDialerStyle
import com.zeno.dialer.ui.theme.DialerStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeno.dialer.DialerUiState
import com.zeno.dialer.FavoritesScrollController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberUpdatedState
import com.zeno.dialer.DialerViewModel
import com.zeno.dialer.data.Contact
import com.zeno.dialer.data.FilterMode
import kotlin.math.abs
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.zeno.dialer.R

// ── Static palette ───────────────────────────────────────────────────────────

internal val BgPage: Color
    @Composable get() = LocalDialerColors.current.bgPage

internal val BgSurface: Color
    @Composable get() = LocalDialerColors.current.bgSurface

internal val BgElevated: Color
    @Composable get() = LocalDialerColors.current.bgElevated

internal val TextPrimary: Color
    @Composable get() = LocalDialerColors.current.textPrimary

internal val TextSecondary: Color
    @Composable get() = LocalDialerColors.current.textSecondary

internal val TextHint: Color
    @Composable get() = LocalDialerColors.current.textHint

internal val Border: Color
    @Composable get() = LocalDialerColors.current.border

internal val FocusBorder: Color
    @Composable get() = LocalDialerColors.current.focusBorder

internal val SurfaceActive: Color
    @Composable get() = LocalDialerColors.current.surfaceActive

internal val Accent: Color
    @Composable get() = LocalDialerColors.current.accent

internal val AccentMuted: Color
    @Composable get() = LocalDialerColors.current.accentMuted

internal val AccentGreen: Color
    @Composable get() = LocalDialerColors.current.accent

internal val Danger: Color
    @Composable get() = LocalDialerColors.current.danger

internal val BadgeStar: Color
    @Composable get() = LocalDialerColors.current.badgeStar

internal val MotionMicroMs: Int
    @Composable get() = LocalDialerMotion.current.microMs

internal val MotionStandardMs: Int
    @Composable get() = LocalDialerMotion.current.standardMs

internal val IsModernClassic: Boolean
    @Composable get() = LocalDialerStyle.current == DialerStyle.MODERN_CLASSIC

internal val IsPixel: Boolean
    @Composable get() = LocalDialerStyle.current == DialerStyle.PIXEL

internal val AccentGreenBright = Color(0xFF69F0AE)

internal val avatarPalette = listOf(
    Color(0xFF5B6ABF), Color(0xFF00796B), Color(0xFF2E7D32),
    Color(0xFF7B1FA2), Color(0xFFC62828), Color(0xFF0277BD),
    Color(0xFFEF6C00), Color(0xFF00838F), Color(0xFF6A1B9A),
    Color(0xFF4E342E),
)
internal fun avatarColor(name: String): Color =
    avatarPalette[abs(name.hashCode()) % avatarPalette.size]

// ── Tabs ─────────────────────────────────────────────────────────────────────

enum class DialerTab(val label: String, @DrawableRes val iconRes: Int) {
    CALLS("Calls", R.drawable.ic_bb_calls),
    CONTACTS("Contacts", R.drawable.ic_bb_contacts),
    KEYPAD("Dial Pad", R.drawable.ic_bb_dialpad),
    // Pixel theme tabs — icon drawn via ImageVector in BottomNavBar; iconRes unused
    FAVORITES("Favorites", R.drawable.ic_bb_calls),
    HOME("Home", R.drawable.ic_bb_calls),
}

// ── Root screen ──────────────────────────────────────────────────────────────

@Composable
fun DialerScreen(
    viewModel: DialerViewModel,
    requestedTab: DialerTab? = null,
    onOpenContact: (com.zeno.dialer.data.Contact) -> Unit = {},
) {
    val state      by viewModel.uiState.collectAsStateWithLifecycle()
    val activeCall by CallStateHolder.info.collectAsStateWithLifecycle()
    val isPixelTheme = IsPixel
    val tab        = if (isPixelTheme) pixelTabFromIndex(state.currentTabIndex) else tabFromIndex(state.currentTabIndex)
    val hasMissedCalls = remember(state.results, tab) {
        tab == DialerTab.CALLS && state.results.any { it.isRecent && it.callType == android.provider.CallLog.Calls.MISSED_TYPE }
    }
    var showMenu   by remember { mutableStateOf(false) }
    val context    = LocalContext.current

    SideEffect { viewModel.setKeypadActive(tab == DialerTab.KEYPAD) }

    LaunchedEffect(requestedTab) {
        requestedTab?.let { viewModel.setCurrentTab(tabToIndex(it)) }
    }

    // Show banner when there is an ongoing/held call but InCallActivity is not in front.
    val showCallBanner = activeCall != null && activeCall!!.state !in listOf(
        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPage)
        ) {
            if (showCallBanner) {
                ReturnToCallBanner(
                    info = activeCall!!,
                    onClick = {
                        context.startActivity(
                            Intent(context, InCallActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                        )
                    },
                    onEndCall = { CallStateHolder.hangup() }
                )
            }

            Crossfade(
                targetState   = tab,
                modifier      = Modifier.weight(1f),
                animationSpec = tween(MotionStandardMs),
                label         = "tab_transition"
            ) { currentTab ->
                when (currentTab) {
                    DialerTab.FAVORITES -> PixelFavoritesContent(state, viewModel, onMenuOpen = { showMenu = true })
                    DialerTab.HOME -> PixelHomeContent(
                        state,
                        viewModel,
                        onMenuOpen = { showMenu = true },
                        onEditNumber = { number ->
                            viewModel.setQueryDirect(number)
                            viewModel.setCurrentTab(tabToIndex(DialerTab.KEYPAD))
                        }
                    )
                    DialerTab.CALLS -> CallsContent(state, viewModel, onMenuOpen = { showMenu = true })
                    DialerTab.CONTACTS   -> ContactsContent(
                        state,
                        viewModel,
                        onMenuOpen = { showMenu = true },
                        onOpenContact = onOpenContact,
                        onEditNumber = { number ->
                            viewModel.setQueryDirect(number)
                            viewModel.setCurrentTab(tabToIndex(DialerTab.KEYPAD))
                        }
                    )
                    DialerTab.KEYPAD -> KeypadContent(state, viewModel)
                }
            }

            BottomNavBar(current = tab, showCallsBadge = hasMissedCalls, isPixel = isPixelTheme, onSelect = { viewModel.setCurrentTab(tabToIndex(it)) })
        }

        // ── Scrim ────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showMenu,
            enter   = fadeIn(tween(MotionStandardMs)),
            exit    = fadeOut(tween(MotionStandardMs))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showMenu = false }
            )
        }

        // ── Drawer panel ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showMenu,
            enter   = slideInHorizontally(tween(MotionStandardMs, easing = EaseInOutCubic)) { -it },
            exit    = slideOutHorizontally(tween(MotionStandardMs, easing = EaseInOutCubic)) { -it }
        ) {
            PhoneMenuDrawer(
                onDismiss = { showMenu = false },
                viewModel = viewModel
            )
        }
    }
}

private fun tabToIndex(tab: DialerTab): Int = when (tab) {
    DialerTab.CALLS -> 0
    DialerTab.CONTACTS -> 1
    DialerTab.KEYPAD -> 2
    DialerTab.FAVORITES -> 0
    DialerTab.HOME -> 1
}

private fun tabFromIndex(index: Int): DialerTab = when (index) {
    0 -> DialerTab.CALLS
    2 -> DialerTab.KEYPAD
    else -> DialerTab.CONTACTS
}

private fun pixelTabFromIndex(index: Int): DialerTab = when (index) {
    0 -> DialerTab.FAVORITES
    2 -> DialerTab.KEYPAD
    else -> DialerTab.HOME
}

/** Number of items in the Contacts LazyColumn (section headers + rows, or empty placeholder). */
private fun contactsLazyItemCount(contacts: List<Contact>): Int {
    if (contacts.isEmpty()) return 1
    var lastLetter: Char? = null
    var n = 0
    for (c in contacts) {
        val letter = c.name.firstOrNull()?.uppercaseChar() ?: '#'
        if (lastLetter != letter) {
            n++
            lastLetter = letter
        }
        n++
    }
    return n
}

/** Flat index in Contacts LazyColumn (A/B/… headers + rows) for scroll-to-selection. */
private fun contactsFlatIndexForSelection(contacts: List<Contact>, selected: Contact?): Int {
    if (selected == null) return -1
    val targetDigits = selected.number.filter { it.isDigit() }
    var lastLetter: Char? = null
    var flat = 0
    for (c in contacts) {
        val letter = c.name.firstOrNull()?.uppercaseChar() ?: '#'
        if (lastLetter != letter) {
            flat++
            lastLetter = letter
        }
        val match = c.number == selected.number ||
            (targetDigits.isNotEmpty() && c.number.filter { it.isDigit() } == targetDigits)
        if (match) return flat
        flat++
    }
    return -1
}

/** Index in [contacts] list for the selected contact (for [BringIntoViewRequester] row). */
private fun contactsIndexForSelection(contacts: List<Contact>, selected: Contact?): Int {
    if (selected == null) return -1
    val targetDigits = selected.number.filter { it.isDigit() }
    contacts.forEachIndexed { i, c ->
        val match = c.number == selected.number ||
            (targetDigits.isNotEmpty() && c.number.filter { it.isDigit() } == targetDigits)
        if (match) return i
    }
    return -1
}

private sealed interface ContactRow {
    data class Header(val letter: Char, val rowIndex: Int) : ContactRow
    data class Item(val contact: Contact, val rowIndex: Int) : ContactRow
}

/** LazyColumn index → index in [contactsOnly] for each contact row (skips section headers). */
private fun buildLazyIndexToContactsRowIndex(flatRows: List<ContactRow>): Map<Int, Int> {
    val m = HashMap<Int, Int>(flatRows.size)
    flatRows.forEachIndexed { lazyIdx, row ->
        if (row is ContactRow.Item) m[lazyIdx] = row.rowIndex
    }
    return m
}

/** First occurrence in [results] for each non-recent number (matches scroll-focus lookup). */
private fun buildNonRecentNumberToResultIndex(results: List<Contact>): Map<String, Int> {
    val m = LinkedHashMap<String, Int>()
    results.forEachIndexed { i, c ->
        if (!c.isRecent && !m.containsKey(c.number)) m[c.number] = i
    }
    return m
}

// ── CONTACTS tab ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactsContent(
    state: DialerUiState,
    viewModel: DialerViewModel,
    onMenuOpen: () -> Unit,
    onOpenContact: (com.zeno.dialer.data.Contact) -> Unit,
    onEditNumber: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.setFilter(FilterMode.CONTACTS)
    }

    val isSearching = state.query.isNotBlank()

    val contactsOnly = remember(state.results, isSearching) {
        if (state.results.isEmpty()) emptyList()
        else {
            val filtered = ArrayList<Contact>(state.results.size)
            for (c in state.results) { if (!c.isRecent) filtered.add(c) }
            // Only sort alphabetically when there's no query — preserve fuzzy rank order during search.
            if (!isSearching) filtered.sortBy { it.name.lowercase() }
            filtered
        }
    }

    // Flat list built once per contactsOnly/query change.
    val flatRows = remember(contactsOnly, isSearching) {
        if (isSearching) {
            // Fuzzy results: flat list without section headers so rank order is preserved.
            ArrayList<ContactRow>(contactsOnly.size).also { list ->
                contactsOnly.forEachIndexed { idx, c -> list.add(ContactRow.Item(c, idx)) }
            }
        } else {
            val list = ArrayList<ContactRow>(contactsOnly.size + 26)
            var lastLetter: Char? = null
            contactsOnly.forEachIndexed { rowIndex, contact ->
                val letter = contact.name.firstOrNull()?.uppercaseChar() ?: '#'
                if (lastLetter != letter) {
                    list.add(ContactRow.Header(letter, rowIndex))
                    lastLetter = letter
                }
                list.add(ContactRow.Item(contact, rowIndex))
            }
            list
        }
    }

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    // Match Calls tab: after a drag scroll, scrollFocusedIndex may hold the row while
    // selectedIndex is -1. Include scroll focus so the highlight and DPAD nav stay aligned.
    val selectedContact = remember(state.selectedIndex, state.scrollFocusedIndex, state.results) {
        val idx = when {
            state.selectedIndex >= 0 -> state.selectedIndex
            state.scrollFocusedIndex >= 0 -> state.scrollFocusedIndex
            else -> -1
        }
        if (idx < 0) null
        else state.results.getOrNull(idx)?.takeIf { !it.isRecent }
    }
    val rowBringers = remember(contactsOnly) {
        List(contactsOnly.size) { BringIntoViewRequester() }
    }

    val lazyIndexToContactsRowIndex = remember(flatRows) { buildLazyIndexToContactsRowIndex(flatRows) }
    val numberToNonRecentResultIndex = remember(state.results) {
        buildNonRecentNumberToResultIndex(state.results)
    }
    val currentLazyIndexToContactsRow by rememberUpdatedState(lazyIndexToContactsRowIndex)
    val currentNumberToResultIndex by rememberUpdatedState(numberToNonRecentResultIndex)

    // Gate: only act on scroll-stop caused by a real user drag (trackpad/touch).
    // D-pad navigation scrollToItem() never fires DragInteraction.Start — so we skip it,
    // preventing selectedIndex from being cleared on programmatic scrolls.
    val currentContactsOnly by rememberUpdatedState(contactsOnly)
    LaunchedEffect(listState) {
        var hadUserDrag = false
        launch {
            listState.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is androidx.compose.foundation.interaction.DragInteraction.Start ->
                        hadUserDrag = true
                    else -> Unit
                }
            }
        }
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) {
                    // Only clear Compose focus on real touch/trackpad drags.
                    // Programmatic animateScrollToItem() also sets isScrollInProgress=true,
                    // but we must NOT clear selection in that case — it would drop the
                    // D-pad highlight mid-navigation.
                    if (hadUserDrag) focusManager.clearFocus()
                } else {
                    if (!hadUserDrag) return@collect
                    hadUserDrag = false
                    if (currentContactsOnly.isEmpty()) return@collect
                    val anchor = listState.firstVisibleItemIndex + 1
                    for (li in anchor..(anchor + 6)) {
                        val rowIndex = currentLazyIndexToContactsRow[li] ?: continue
                        val contact = currentContactsOnly.getOrNull(rowIndex) ?: continue
                        val resultIdx = currentNumberToResultIndex[contact.number] ?: continue
                        // Keep both indices in sync on Contacts so the row stays highlighted
                        // and DPAD_UP/DOWN work (setScrollFocusedIndex clears selectedIndex).
                        viewModel.selectItem(resultIdx)
                        break
                    }
                }
            }
    }

    // Scroll to top whenever scrollContactsToTop() is called — fires even if selectedIndex
    // was already 0 (LaunchedEffect on selectedIndex alone wouldn't re-trigger in that case).
    LaunchedEffect(Unit) {
        viewModel.contactsScrollToTopEvent.collect {
            listState.animateScrollToItem(0)
        }
    }

    // Contacts-like behavior: move one row at a time and only adjust viewport when needed.
    LaunchedEffect(state.selectedIndex, state.scrollFocusedIndex, selectedContact, contactsOnly.size, flatRows.size) {
        if (contactsOnly.isEmpty()) return@LaunchedEffect
        val sel = selectedContact ?: return@LaunchedEffect
        val flatIdx = contactsFlatIndexForSelection(contactsOnly, sel)
        if (flatIdx < 0) return@LaunchedEffect
        // Use flatRows.size as the ground truth item count — avoids drift with contactsLazyItemCount().
        if (flatIdx >= flatRows.size) return@LaunchedEffect
        val cIdx = contactsIndexForSelection(contactsOnly, sel)
        if (cIdx < 0 || cIdx >= rowBringers.size) return@LaunchedEffect
        try {
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it > flatIdx }
            val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == flatIdx }
            if (isVisible) {
                rowBringers[cIdx].bringIntoView()
            } else {
                listState.animateScrollToItem((flatIdx - 2).coerceAtLeast(0))
            }
        } catch (_: NoSuchElementException) { }
    }

    val isModernContacts = IsModernClassic
    val scope = rememberCoroutineScope()

    // Map letter → flat-list index of its section header (for sidebar scroll-jump).
    val letterToFlatIndex = remember(flatRows) {
        val map = LinkedHashMap<Char, Int>()
        flatRows.forEachIndexed { index, row ->
            if (row is ContactRow.Header && row.letter !in map) map[row.letter] = index
        }
        map
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Contacts search bar (matches HTML prototype)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(if (isModernContacts) BgSurface else Color.White)
                .border(
                    width = if (isModernContacts) 1.dp else 2.dp,
                    color = if (isModernContacts) Border else Accent,
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(7.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = state.query,
                onValueChange = { viewModel.setQueryDirect(it) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, color = TextPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (state.query.isBlank()) {
                        Text("Search contacts", color = TextHint, fontSize = 17.sp)
                    }
                    inner()
                }
            )
            if (state.query.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .clickable { viewModel.clearQuery() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(BgPage),
            contentPadding = PaddingValues(
                end = if (!isSearching && letterToFlatIndex.isNotEmpty()) 22.dp else 0.dp
            )
        ) {
            if (flatRows.isEmpty()) {
                item {
                    Text(
                        text = "No contacts",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(
                    items = flatRows,
                    key = { row ->
                        when (row) {
                            is ContactRow.Header -> "h_${row.letter}"
                            is ContactRow.Item -> "c_${row.contact.id}_${row.contact.number}"
                        }
                    },
                    contentType = { row ->
                        when (row) {
                            is ContactRow.Header -> 0
                            is ContactRow.Item -> 1
                        }
                    }
                ) { row ->
                    when (row) {
                        is ContactRow.Header -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isModernContacts) 32.dp else 36.dp)
                                    .background(if (isModernContacts) BgElevated else Color(0xFFF2F2F2))
                                    .border(1.dp, Border, RoundedCornerShape(0.dp)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (isModernContacts) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .width(3.dp)
                                            .height(16.dp)
                                            .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                            .background(Accent.copy(alpha = 0.65f))
                                    )
                                }
                                Text(
                                    text = row.letter.toString(),
                                    color = Accent,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = if (isModernContacts) 14.dp else 10.dp)
                                )
                            }
                        }
                        is ContactRow.Item -> {
                            val contact = row.contact
                            val isSelected = selectedContact != null &&
                                contact.number == selectedContact.number
                            Row(
                                modifier = Modifier
                                    .bringIntoViewRequester(rowBringers[row.rowIndex])
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .background(if (isSelected) SurfaceActive else Color.Transparent)
                                    .border(1.dp, Border, RoundedCornerShape(0.dp))
                                    .clickable { onOpenContact(contact) }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 42)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = contact.name,
                                        color = TextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = if ((contact.name.hashCode() and 1) == 0) "Mobile" else "Work",
                                        color = TextSecondary,
                                        fontSize = 15.sp,
                                        lineHeight = 16.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // ── Alphabet index sidebar ───────────────────────────────────────────
        if (!isSearching && letterToFlatIndex.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(22.dp)
                    .background(
                        if (isModernContacts) BgElevated.copy(alpha = 0.95f)
                        else Color(0xFFEEEEEE).copy(alpha = 0.95f)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                letterToFlatIndex.forEach { (letter, flatIndex) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { scope.launch { listState.animateScrollToItem(flatIndex) } }
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = letter.toString(),
                            color      = Accent,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }
        }
        } // end Box
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallsContent(state: DialerUiState, viewModel: DialerViewModel, onMenuOpen: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        FavoriteTilesRow(
            pinned = state.pinnedFavorites,
            focusedIndex = state.favoriteFocusIndex,
            onOpenSettings = {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            },
            onTap = { c -> viewModel.callNumber(c.number) },
            onLongPress = { c -> viewModel.unpinFavorite(c.number) },
        )

        AllMissedTabs(
            current = state.filterMode,
            onSelect = { mode -> viewModel.setFilter(mode) },
            modifier = Modifier.fillMaxWidth()
        )

        ResultsList(
            results       = state.results,
            selectedIndex = state.selectedIndex,
            modifier      = Modifier.weight(1f).fillMaxWidth(),
            onTap         = { i ->
                viewModel.selectItem(i)
                viewModel.toggleExpandIndex(i) // single tap expands history panel
            },
            // Calls screen UX:
            // - single tap selects/expands inline history (no dialing)
            // - double tap dials the selected number
            onDoubleTap   = { i -> viewModel.selectItem(i); viewModel.callItem(i) },
            onCallNumber  = { number -> viewModel.callNumber(number) },
            onEditNumber  = { number ->
                viewModel.setQueryDirect(number)
                viewModel.setCurrentTab(tabToIndex(DialerTab.KEYPAD))
            },
            onAddFavorite = { contact -> viewModel.pinFavorite(contact) },
            onToggleBlocked = { contact -> viewModel.toggleBlockedContact(contact) },
            isBlocked = { contact -> contact.number.filter { it.isDigit() } in state.blockedNumbers },
            onDelete      = { number -> viewModel.deleteCallLogForNumber(number) },
            toggleExpandFlow = viewModel.toggleExpandEvent,
            onScrollFocusIndexChanged = { i -> viewModel.setScrollFocusedIndex(i) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteTilesRow(
    pinned: List<Contact>,
    focusedIndex: Int = -1,
    onOpenSettings: () -> Unit,
    onTap: (Contact) -> Unit,
    onLongPress: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowState = rememberLazyListState()

    // Keep focused tile visible while navigating horizontally with D-pad.
    LaunchedEffect(focusedIndex, pinned.size) {
        if (focusedIndex < 0) return@LaunchedEffect
        val maxIndex = pinned.size // 0 = Settings tile, 1..pinned.size = contacts
        val target = focusedIndex.coerceIn(0, maxIndex)
        val visible = rowState.layoutInfo.visibleItemsInfo.any { it.index == target }
        if (!visible) {
            rowState.animateScrollToItem(target.coerceAtLeast(0))
        }
    }

    val isModernRow = IsModernClassic
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .background(if (isModernRow) BgPage else Color.Transparent)
    ) {
        // Prototype pass: keep exactly 5 tiles visible across.
        val tileGap   = if (isModernRow) 1.dp else 0.dp
        val tileWidth = if (isModernRow) (maxWidth - tileGap * 4) / 5 else maxWidth / 5
        val tileHeight = 108.dp
        LazyRow(
            state = rowState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(tileGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "settings_tile") {
                FavoriteTile(
                    title = "Settings",
                    background = Color(0xFF4B3A68),
                    focused = focusedIndex == 0,
                    onClick = onOpenSettings,
                    onLongClick = null,
                    icon = Icons.Default.Settings,
                    tileW = tileWidth,
                    tileH = tileHeight
                )
            }

            itemsIndexed(pinned, key = { _, c -> c.number }) { idx, contact ->
                FavoriteTile(
                    title = contact.name,
                    background = avatarColor(contact.name),
                    focused = focusedIndex == idx + 1,
                    onClick = { onTap(contact) },
                    onLongClick = { onLongPress(contact) },
                    icon = null,
                    tileW = tileWidth,
                    tileH = tileHeight
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteTile(
    title: String,
    background: Color,
    focused: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    icon: ImageVector?,
    tileW: Dp,
    tileH: Dp,
) {
    val isModernTile = IsModernClassic
    val cornerRadius = if (isModernTile) RoundedCornerShape(6.dp) else RoundedCornerShape(0.dp)
    Box(
        modifier = Modifier
            .width(tileW)
            .height(tileH)
            .then(if (isModernTile) Modifier.clip(cornerRadius) else Modifier)
            .then(
                if (!isModernTile) Modifier
                    .background(Color.White)
                    .border(
                        width = if (focused) 5.dp else 1.dp,
                        color = if (focused) Accent else Border,
                        shape = RoundedCornerShape(0.dp)
                    )
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            if (focused && !isModernTile) {
                // Original Classic: strong white overlay for D-pad focus
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, Color.White, RoundedCornerShape(0.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                )
            } else if (focused && isModernTile) {
                // Modern Classic: subtle accent inner border only — no white fill
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, Accent, cornerRadius)
                )
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(if (isModernTile) 26.dp else 24.dp)
                )
            } else {
                Text(
                    text = title.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                    color = Color.White,
                    fontSize = if (isModernTile) 30.sp else 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom gradient overlay — shorter and crisper for Modern Classic
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (isModernTile) 38.dp else 44.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = if (isModernTile) 0.55f else 0.35f)
                        )
                    )
                )
        )

        Text(
            text = title,
            color = Color.White,
            fontSize = if (isModernTile) 12.sp else 11.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
        )
    }
}

@Composable
private fun AllMissedTabs(
    current: FilterMode,
    onSelect: (FilterMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        FilterMode.ALL to "All",
        FilterMode.MISSED to "Missed",
        FilterMode.RECEIVED to "Received",
    )
    val selected = when (current) {
        FilterMode.MISSED -> FilterMode.MISSED
        FilterMode.RECEIVED -> FilterMode.RECEIVED
        FilterMode.ALL, FilterMode.RECENTS -> FilterMode.ALL
        FilterMode.CONTACTS -> FilterMode.ALL
    }
    val isModernTabs = IsModernClassic
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isModernTabs) 48.dp else 45.dp)
            .background(if (isModernTabs) BgSurface else Color.Transparent)
            .border(width = 1.dp, color = Border, shape = RoundedCornerShape(0.dp)),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { idx, (mode, label) ->
            val isSel = selected == mode
            if (isModernTabs) {
                // Modern Classic: filled capsule chip — no underline
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSel) Accent.copy(alpha = 0.18f) else Color.Transparent
                            )
                    )
                    Text(
                        text       = label,
                        color      = if (isSel) Accent else TextHint,
                        fontSize   = 13.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isSel) BgPage else BgSurface)
                        .clickable { onSelect(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        color      = if (isSel) Accent else TextHint,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSel) Accent else Color.Transparent)
                    )
                }
                if (idx < tabs.lastIndex) {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Border))
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    contact: Contact,
    isPinned: Boolean,
    onCall: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = contact.name,
                color    = TextPrimary,
                style    = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text  = contact.number,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        // Pin / unpin button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { if (isPinned) onUnpin() else onPin() }
                .background(BgSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Favorite,
                contentDescription = if (isPinned) "Unpin" else "Pin",
                tint               = if (isPinned) AccentGreen else TextSecondary,
                modifier           = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        // Call button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onCall() }
                .background(BgSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Phone,
                contentDescription = "Call",
                tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

// ── KEYPAD tab ───────────────────────────────────────────────────────────────

private val dialpadKeys = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf('*', '0', '#')
)

private val dialpadLetters = mapOf(
    '2' to "ABC",
    '3' to "DEF",
    '4' to "GHI",
    '5' to "JKL",
    '6' to "MNO",
    '7' to "PQRS",
    '8' to "TUV",
    '9' to "WXYZ",
)

/** Strips tel: URLs and keeps dial-pad–relevant characters for paste. */
private fun normalizePastedDialText(raw: String): String {
    var s = raw.trim()
    if (s.startsWith("tel:", ignoreCase = true)) {
        s = s.substring(4).trim()
    }
    val cut = s.indexOfAny(charArrayOf(';', '?'))
    if (cut >= 0) s = s.substring(0, cut)
    return buildString {
        for (c in s) {
            when {
                c.isDigit() -> append(c)
                c == '*' || c == '#' || c == '+' -> append(c)
                c.isLetter() -> append(c)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadContent(state: DialerUiState, viewModel: DialerViewModel) {
    if (IsPixel) {
        PixelKeypadContent(state = state, viewModel = viewModel)
        return
    }

    val hasInput = state.query.isNotBlank()
    val canCall  = hasInput
    val keypadMatch = state.keypadContactMatch

    val view = LocalView.current
    val context = LocalContext.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Slightly smaller tiles (~10%) so the Enter Number bar can use more vertical space.
        val cellSize = maxWidth / 3 * 0.9f

        val isModernKeypad = IsModernClassic
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .background(if (isModernKeypad) BgPage else Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header: typed digits + contact hint (search-as-you-type) ──────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(if (isModernKeypad) BgElevated else Accent)
                    .then(
                        if (isModernKeypad) Modifier.border(
                            width = 1.dp,
                            color = Border,
                            shape = RoundedCornerShape(0.dp)
                        ) else Modifier
                    )
                    .padding(start = 14.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    val clipboard = remember(context) {
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {},
                                onLongClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    val item = clipboard.primaryClip?.getItemAt(0)
                                    if (item != null) {
                                        val pasted = normalizePastedDialText(
                                            item.coerceToText(context)?.toString().orEmpty()
                                        )
                                        if (pasted.isNotEmpty()) {
                                            viewModel.setQueryDirect(pasted)
                                            Toast.makeText(context, "Pasted", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Nothing to paste", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Nothing to paste", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            .semantics {
                                contentDescription =
                                    context.getString(R.string.dialpad_number_bar_a11y)
                            },
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (hasInput) {
                            val digitShadow = if (isModernKeypad) null else Shadow(
                                color = Color.Black.copy(alpha = 0.45f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 4f
                            )
                            Text(
                                text          = state.query,
                                color         = if (isModernKeypad) TextPrimary else Color.White,
                                fontSize      = 24.sp,
                                fontWeight    = FontWeight.Normal,
                                letterSpacing = 1.sp,
                                lineHeight    = 24.sp,
                                maxLines      = 1,
                                overflow      = TextOverflow.Ellipsis,
                                style         = if (digitShadow != null) TextStyle(shadow = digitShadow) else TextStyle.Default
                            )
                            if (keypadMatch != null) {
                                Text(
                                    text       = keypadMatch.name,
                                    color      = if (isModernKeypad) Accent else Color.White.copy(alpha = 0.96f),
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 14.sp,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis,
                                    style      = if (!isModernKeypad) TextStyle(
                                        shadow = Shadow(
                                            color      = Color.Black.copy(alpha = 0.35f),
                                            offset     = Offset(0f, 1f),
                                            blurRadius = 3f
                                        )
                                    ) else TextStyle.Default,
                                    modifier   = Modifier.padding(top = 0.dp)
                                )
                            }
                        } else {
                            Text(
                                text          = "Enter Number",
                                color         = if (isModernKeypad) TextHint else Color.White.copy(alpha = 0.72f),
                                fontSize      = if (isModernKeypad) 17.sp else 16.sp,
                                fontWeight    = FontWeight.Medium,
                                letterSpacing = 0.3.sp,
                                lineHeight    = 20.sp,
                                maxLines      = 1,
                                overflow      = TextOverflow.Ellipsis
                            )
                        }
                    }

                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .clickable {
                            val raw = state.query.trim()
                            context.startActivity(
                                Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                                    type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                                    if (raw.isNotEmpty()) {
                                        putExtra(ContactsContract.Intents.Insert.PHONE, raw)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add contact",
                        tint = if (isModernKeypad) AccentMuted else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ── Dialpad grid (pin to bottom so no gap above the green Call bar) ─
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 0.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border)
                )
                dialpadKeys.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        row.forEachIndexed { colIndex, digit ->
                            DialpadButton(
                                digit   = digit,
                                size    = cellSize,
                                showRightDivider  = colIndex < 2,
                                showBottomDivider = rowIndex < 3,
                                onClick = { viewModel.typeUnicode(digit.code) }
                            )
                        }
                    }
                }
            }

            // ── Call + Backspace row ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isModernKeypad) 72.dp else 58.dp)
                    .background(if (isModernKeypad) BgPage else Color.Transparent)
                    .then(
                        if (isModernKeypad)
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        else
                            Modifier.border(1.dp, Border, RoundedCornerShape(0.dp))
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (isModernKeypad)
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(Color(0xFF72BE2E), Color(0xFF488A14))
                                        )
                                    )
                            else
                                Modifier.background(Color(0xFF5A8A1C))
                        )
                        .clickable(enabled = canCall) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.callSelected()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(if (isModernKeypad) 20.dp else 18.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Call",
                        color      = Color.White,
                        fontSize   = if (isModernKeypad) 15.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (isModernKeypad) Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .then(if (isModernKeypad) Modifier.width(60.dp) else Modifier.width(64.dp))
                        .fillMaxHeight()
                        .then(if (isModernKeypad) Modifier.clip(RoundedCornerShape(10.dp)) else Modifier)
                        .background(if (isModernKeypad) BgElevated else Color(0xFF9E9E9E))
                        .clickable(enabled = hasInput) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.deleteChar()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = if (isModernKeypad) TextSecondary else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialpadButton(
    digit: Char,
    size: Dp,
    showRightDivider: Boolean,
    showBottomDivider: Boolean,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val isModern = IsModernClassic
    val padDesc = when (digit) {
        '*' -> "star"
        '#' -> "pound"
        else -> digit.toString()
    }
    Box(
        modifier         = Modifier
            .size(size)
            .semantics { contentDescription = "Dial pad key $padDesc" }
            .then(
                if (isModern) Modifier
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (pressed) BgElevated else BgSurface)
                else Modifier
                    .background(if (pressed) BgElevated else Color.White)
            )
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (!isModern && showRightDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Border)
            )
        }
        if (!isModern && showBottomDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(Border)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            when (digit) {
                '1' -> {
                    Text(
                        text = "1",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                    Row(
                        modifier = Modifier.padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .border(1.5.dp, TextSecondary, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .height(1.2.dp)
                                .width(9.dp)
                                .background(TextSecondary)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .border(1.5.dp, TextSecondary, CircleShape)
                        )
                    }
                }
                '*', '#' -> {
                    Text(
                        text = digit.toString(),
                        color = TextPrimary,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraLight
                    )
                }
                '0' -> {
                    Text(
                        text = "0",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "+",
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                else -> {
                    Text(
                        text = digit.toString(),
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = dialpadLetters[digit].orEmpty(),
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

// ── Pixel keypad ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PixelKeypadContent(state: DialerUiState, viewModel: DialerViewModel) {
    val matched     = state.results.firstOrNull()
    val hasInput    = state.query.isNotBlank()
    val activeCall  by CallStateHolder.info.collectAsStateWithLifecycle()
    val duringCall  = activeCall != null
    // During an active call typing keys sends DTMF, not a new call — hide the call button
    val canCall     = hasInput && !duringCall

    val view    = LocalView.current
    val context = LocalContext.current
    val clipboard = remember(context) {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }

    val isLight = BgPage.luminance() > 0.5f
    val keyBg   = if (isLight) Color(0xFFE4E7EC) else Color(0xFF2C2D31)  // stronger contrast on white
    val keyText = if (isLight) Color(0xFF1A1A2E) else TextPrimary
    val subText = if (isLight) Color(0xFF5F6368) else TextSecondary

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val g            = 10.dp                       // inter-key gap
        val hPad         = 18.dp                       // side padding
        val displayH     = 62.dp                       // number display row height
        val callBtnH     = 54.dp                       // call pill height
        val callGap      = 14.dp                       // gap between grid and call button
        val bottomSpacer = 14.dp                       // gap below call button (above nav bar)
        val topSpacer    = 10.dp                       // fixed top breathing room
        val numRows      = dialpadKeys.size            // 4

        val gridW = maxWidth - hPad * 2
        val keyW  = (gridW - g * 2) / 3               // single key width

        // Fill available space, allow taller pills for a premium feel
        val idealRowH = (keyW.value * 0.65f).dp.coerceIn(52.dp, 90.dp)
        // During a call the call button is hidden — give that space back to the key rows
        val fixedH = topSpacer + displayH + g + g * (numRows - 1) + bottomSpacer +
                if (duringCall) 0.dp else callGap + callBtnH
        val availForRows = (maxHeight - fixedH).coerceAtLeast(0.dp)
        val rowH      = minOf(idealRowH, (availForRows / numRows).coerceAtLeast(52.dp))

        val digitFs  = (rowH.value * 0.42f).coerceIn(22f, 30f).sp  // digit numeral
        val subFs    = (rowH.value * 0.145f).coerceIn(9f, 12f).sp   // sub-label letters

        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(topSpacer))

            // ── Number display ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(displayH)
                    .padding(horizontal = hPad)
                    .combinedClickable(
                        onClick     = {},
                        onLongClick = {
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val raw    = clip.getItemAt(0).coerceToText(context)?.toString().orEmpty()
                                val pasted = normalizePastedDialText(raw)
                                if (pasted.isNotEmpty()) {
                                    viewModel.setQueryDirect(state.query + pasted)
                                    Toast.makeText(context, "Pasted", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Nothing to paste", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Nothing to paste", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
            ) {
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasInput) {
                        Text(
                            text     = state.query,
                            color    = TextPrimary,
                            style    = TextStyle(
                                fontSize      = 30.sp,
                                fontWeight    = FontWeight.W300,
                                lineHeight    = 32.sp,
                                textAlign     = TextAlign.Center,
                                letterSpacing = 1.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (matched != null && hasInput) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text     = matched.name,
                            color    = TextSecondary,
                            style    = TextStyle(
                                fontSize   = 12.5.sp,
                                lineHeight = 14.sp,
                                textAlign  = TextAlign.Center
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (hasInput) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.deleteChar()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Delete",
                            tint               = TextSecondary,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(g))

            // ── Key grid ──────────────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad),
                verticalArrangement = Arrangement.spacedBy(g)
            ) {
                dialpadKeys.forEach { rowKeys ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(g)
                    ) {
                        rowKeys.forEach { digit ->
                            PixelDialpadKeyPill(
                                digit    = digit,
                                keyBg    = keyBg,
                                keyText  = keyText,
                                subText  = subText,
                                isLight  = isLight,
                                digitFs  = digitFs,
                                subFs    = subFs,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(rowH),
                                onClick  = { viewModel.typeUnicode(digit.code) }
                            )
                        }
                    }
                }
            }

            // ── Call pill (hidden during active call) ─────────────────────────
            if (!duringCall) {
                Spacer(Modifier.height(callGap))
                Box(
                    modifier = Modifier
                        .width(gridW * 0.52f)
                        .height(callBtnH)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (canCall) Color(0xFF1EA446)
                            else Color(0xFF1EA446).copy(alpha = if (isLight) 0.55f else 0.32f)
                        )
                        .clickable(enabled = canCall) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.callSelected()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint               = Color.White,
                        modifier           = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.height(bottomSpacer))
            }
        }
    }
}

@Composable
private fun PixelDialpadKeyPill(
    digit:   Char,
    keyBg:   Color,
    keyText: Color,
    subText: Color,
    isLight: Boolean,
    digitFs: TextUnit,
    subFs:   TextUnit,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val view  = LocalView.current
    val shape = RoundedCornerShape(50)
    // * and # have no sub-label — center the digit perfectly
    val hasSubLabel = digit != '*' && digit != '#'

    Box(
        modifier         = modifier
            .shadow(if (isLight) 1.5.dp else 0.dp, shape, clip = false)
            .clip(shape)
            .background(keyBg)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (hasSubLabel) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text       = digit.toString(),
                    color      = keyText,
                    fontSize   = digitFs,
                    fontWeight = FontWeight.Normal,
                    lineHeight = (digitFs.value * 1.05f).sp
                )
                when {
                    digit == '1' -> {
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                        ) {
                            Box(Modifier.size(5.5.dp).background(subText, CircleShape))
                            Box(Modifier.size(11.dp, 1.5.dp).background(subText))
                            Box(Modifier.size(5.5.dp).background(subText, CircleShape))
                        }
                    }
                    digit == '0' -> {
                        Text(
                            text       = "+",
                            color      = subText,
                            fontSize   = subFs,
                            lineHeight = (subFs.value * 1.1f).sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    else -> {
                        val letters = dialpadLetters[digit]
                        if (letters != null) {
                            Text(
                                text          = letters,
                                color         = subText,
                                fontSize      = subFs,
                                lineHeight    = (subFs.value * 1.1f).sp,
                                fontWeight    = FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
            }
        } else {
            // * and # — digit only, perfectly centered
            Text(
                text       = digit.toString(),
                color      = keyText,
                fontSize   = digitFs,
                fontWeight = FontWeight.Normal,
                lineHeight = (digitFs.value * 1.05f).sp
            )
        }
    }
}

// ── Bottom nav bar ───────────────────────────────────────────────────────────

private val pixelNavTabs = listOf(
    Triple(DialerTab.FAVORITES, "Favorites", Icons.Default.Favorite),
    Triple(DialerTab.HOME,      "Home",      Icons.Default.Home),
    Triple(DialerTab.KEYPAD,    "Keypad",    Icons.Default.Dialpad),
)

@Composable
private fun BottomNavBar(current: DialerTab, showCallsBadge: Boolean, isPixel: Boolean, onSelect: (DialerTab) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPixel) BgSurface else BgPage)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPixel) {
                pixelNavTabs.forEach { (tab, label, icon) ->
                    PixelNavItem(
                        icon     = icon,
                        label    = label,
                        selected = current == tab,
                        onClick  = { onSelect(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                listOf(DialerTab.CALLS, DialerTab.CONTACTS, DialerTab.KEYPAD).forEach { tab ->
                    NavItem(
                        iconRes   = tab.iconRes,
                        label     = tab.label,
                        selected  = current == tab,
                        showBadge = IsModernClassic && showCallsBadge && tab == DialerTab.CALLS,
                        onClick   = { onSelect(tab) },
                        modifier  = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelNavItem(
    icon:     ImageVector,
    label:    String,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor  = if (selected) TextPrimary else TextSecondary
    val textColor  = if (selected) TextPrimary else TextSecondary

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = iconColor,
            modifier           = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text       = label,
            color      = textColor,
            fontSize   = 10.sp,
            lineHeight = 10.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines   = 1
        )
        if (selected) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(Accent, RoundedCornerShape(1.dp))
            )
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun NavItem(
    @DrawableRes iconRes: Int,
    label:    String,
    selected: Boolean,
    showBadge: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = if (selected) Accent else TextSecondary
    val textColor = if (selected) Accent else TextSecondary
    val isModernNav = IsModernClassic

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        if (isModernNav) {
            // Glow dot beneath icon for Modern Classic — more contemporary than a top line
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (selected) Accent else Color.Transparent)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(2.5.dp)
                    .background(if (selected) Accent else Color.Transparent)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(27.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        AnimatedVisibility(
            visible = showBadge,
            enter = fadeIn(tween(MotionMicroMs)),
            exit = fadeOut(tween(MotionMicroMs)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 33.dp, top = 2.dp)
        ) {
            Text("*", color = BadgeStar, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Search bar ───────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    displayQuery:  String,
    onMenuClick:   () -> Unit,
    onVoiceSearch: () -> Unit,
    onContactsOpen: () -> Unit = {},
    modifier:      Modifier = Modifier
) {
    Box(
        modifier         = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgElevated)
            .border(1.dp, Border, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier          = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint               = TextSecondary,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Text(
                text     = if (displayQuery.isEmpty()) "Search contacts" else displayQuery,
                color    = if (displayQuery.isEmpty()) TextSecondary else TextPrimary,
                style    = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onVoiceSearch() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Mic,
                    contentDescription = "Voice search",
                    tint               = TextSecondary,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onContactsOpen() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Contacts,
                    contentDescription = "Contacts",
                    tint               = TextSecondary,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Filter chips ─────────────────────────────────────────────────────────────

private data class ChipDef(val mode: FilterMode, val label: String)
private val filterChips = listOf(
    ChipDef(FilterMode.ALL,      "All"),
    ChipDef(FilterMode.MISSED,   "Missed"),
    ChipDef(FilterMode.RECEIVED, "Received"),
    ChipDef(FilterMode.CONTACTS, "Contacts"),
    ChipDef(FilterMode.RECENTS,  "Recents"),
)

@Composable
private fun FilterChipRow(current: FilterMode, onSelect: (FilterMode) -> Unit) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filterChips, key = { it.mode.name }) { chip ->
            val sel = chip.mode == current

            val bgColor = if (sel) SurfaceActive else Color.Transparent
            val borderColor = if (sel) Accent else Border
            val textColor = if (sel) Accent else TextSecondary

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable { onSelect(chip.mode) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = chip.label,
                    color = textColor,
                    style = if (sel) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            else MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal)
                )
            }
        }
    }
}

// ── Return-to-call banner ─────────────────────────────────────────────────────

@Composable
private fun ReturnToCallBanner(info: ActiveCallInfo, onClick: () -> Unit, onEndCall: () -> Unit) {
    val stateText = when (info.state) {
        Call.STATE_ACTIVE  -> "Active call"
        Call.STATE_HOLDING -> "On hold"
        Call.STATE_DIALING -> "Calling…"
        else               -> "In call"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceActive)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Phone,
                    contentDescription = null,
                    tint               = Accent,
                    modifier           = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text       = info.displayName,
                    color      = TextPrimary,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(text = stateText, color = Accent, fontSize = 13.sp)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Danger.copy(alpha = 0.15f))
                .clickable { onEndCall() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "End Call",
                color      = Danger,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Phone menu drawer ────────────────────────────────────────────────────────

@Composable
private fun PhoneMenuDrawer(onDismiss: () -> Unit, viewModel: DialerViewModel) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.78f)
                .align(Alignment.CenterStart)
                .background(BgPage)
                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(0.dp))
                .clickable(enabled = false) { }
        ) {
            Spacer(Modifier.height(40.dp))

            Text(
                text     = "Phone",
                color    = Accent,
                style    = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(12.dp))

            DrawerMenuItem(
                icon  = Icons.Default.Contacts,
                label = "Contacts"
            ) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                )
                onDismiss()
            }

            DrawerMenuItem(
                icon  = Icons.Default.Settings,
                label = "Settings"
            ) {
                context.startActivity(Intent(context, com.zeno.dialer.SettingsActivity::class.java))
                onDismiss()
            }

            DrawerMenuItem(
                icon  = Icons.Default.History,
                label = "Clear call history"
            ) {
                showClearDialog = true
            }

            DrawerMenuItem(
                icon  = Icons.AutoMirrored.Filled.Help,
                label = "Help & feedback"
            ) {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO).apply {
                        data    = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL,   arrayOf("support@zeno.app"))
                        putExtra(Intent.EXTRA_SUBJECT, "Zeno Classic Dialer — Feedback")
                    }
                )
                onDismiss()
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title   = { Text("Clear call history?") },
            text    = { Text("All call log entries will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCallHistory()
                    showClearDialog = false
                    onDismiss()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}


@Composable
private fun DrawerMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val isHighlighted = isPressed || isFocused
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .background(if (isHighlighted) SurfaceActive else Color.Transparent)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (isHighlighted) {
            Box(modifier = Modifier.width(3.dp).height(24.dp).background(Accent))
            Spacer(Modifier.width(0.dp))
        }
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (isHighlighted) Accent else TextSecondary,
            modifier           = Modifier.size(24.dp)
        )
        Text(
            text       = label,
            color      = if (isHighlighted) Accent else TextPrimary,
            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
            style      = MaterialTheme.typography.bodyLarge
        )
    }
}

// ── Favorites row ────────────────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FavoritesRow(
    favorites: List<Contact>,
    onTap: (Contact) -> Unit,
    onLongPress: ((Contact) -> Unit)? = null
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(favorites, key = { it.number }) { contact ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier
                    .width(74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { onTap(contact) },
                        onLongClick = { onLongPress?.invoke(contact) }
                    )
                    .padding(vertical = 4.dp)
            ) {
                ContactAvatar(
                    name     = contact.name,
                    photoUri = contact.photoUri,
                    size     = 46
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = contact.name,
                    color     = TextSecondary,
                    style     = MaterialTheme.typography.bodyMedium,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// ── Pixel: HOME tab ──────────────────────────────────────────────────────────

@Composable
private fun PixelHomeContent(
    state: DialerUiState,
    viewModel: DialerViewModel,
    onMenuOpen: () -> Unit,
    onEditNumber: (String) -> Unit
) {
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) viewModel.setQueryDirect(text)
        }
    }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(12.dp))

        PixelSearchBar(
            displayQuery  = state.displayQuery,
            onMenuClick   = onMenuOpen,
            onVoiceSearch = {
                voiceLauncher.launch(
                    Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                 android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Search contacts…")
                    }
                )
            },
            onContactsOpen = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        data = android.provider.ContactsContract.Contacts.CONTENT_URI
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        FilterChipRow(current = state.filterMode, onSelect = { viewModel.setFilter(it) })

        Spacer(Modifier.height(4.dp))

        ResultsList(
            results          = state.results,
            selectedIndex    = state.selectedIndex,
            modifier         = Modifier.weight(1f).fillMaxWidth(),
            onTap            = { i ->
                viewModel.selectItem(i)
                // Pixel: single tap expands the action panel (Call / Message / History)
                viewModel.toggleExpandIndex(i)
            },
            onDoubleTap      = { i -> viewModel.selectItem(i); viewModel.callItem(i) },
            onCallNumber     = { number -> viewModel.callNumber(number) },
            onEditNumber     = onEditNumber,
            onAddFavorite    = { contact -> viewModel.pinFavorite(contact) },
            onToggleBlocked  = { contact -> viewModel.toggleBlockedContact(contact) },
            isBlocked        = { contact -> contact.number.filter { it.isDigit() } in state.blockedNumbers },
            onDelete         = { number -> viewModel.deleteCallLogForNumber(number) },
            toggleExpandFlow = viewModel.toggleExpandEvent,
            onScrollFocusIndexChanged = { i -> viewModel.setScrollFocusedIndex(i) },
            scrollToTopKey   = state.query + state.filterMode.name,
        )
    }
}

// ── Pixel: FAVORITES tab ─────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PixelFavoritesContent(
    state: DialerUiState,
    viewModel: DialerViewModel,
    onMenuOpen: () -> Unit
) {
    val listState   = rememberLazyListState()
    val suggestions = state.favoriteSuggestions
    val selIdx      = state.favoriteFocusIndex

    LaunchedEffect(selIdx, suggestions.size) {
        if (suggestions.isEmpty()) return@LaunchedEffect
        val flatPos = 3 + selIdx.coerceIn(0, suggestions.lastIndex)
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) {
            listState.scrollToItem(flatPos)
            return@LaunchedEffect
        }
        val first = visible.first().index
        val last  = visible.last().index
        when {
            flatPos < first -> listState.animateScrollToItem(flatPos)
            flatPos > last  -> listState.animateScrollToItem((flatPos - 1).coerceAtLeast(0))
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(BgPage)) {

        item {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onMenuOpen() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextSecondary)
                }
                Text("Favorites", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(44.dp))
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
            Text(
                text     = "Pinned",
                color    = TextSecondary,
                style    = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            if (state.pinnedFavorites.isEmpty()) {
                Text(
                    text     = "Long-press any contact below to pin them here",
                    color    = TextHint,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            } else {
                FavoritesRow(
                    favorites   = state.pinnedFavorites,
                    onTap       = { c -> viewModel.callNumber(c.number) },
                    onLongPress = { c -> viewModel.unpinFavorite(c.number) }
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text     = "Suggestions",
                color    = TextSecondary,
                style    = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(4.dp))
        }

        if (suggestions.isEmpty()) {
            item {
                Text(
                    text     = "No suggestions yet",
                    color    = TextHint,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            itemsIndexed(suggestions, key = { _, c -> c.number }) { idx, contact ->
                PixelSuggestionRow(
                    contact  = contact,
                    selected = idx == selIdx,
                    isPinned = state.pinnedFavorites.any { it.number == contact.number },
                    onCall   = { viewModel.callNumber(contact.number) },
                    onPin    = { viewModel.pinFavorite(contact) },
                    onUnpin  = { viewModel.unpinFavorite(contact.number) }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PixelSuggestionRow(
    contact:  Contact,
    selected: Boolean,
    isPinned: Boolean,
    onCall:   () -> Unit,
    onPin:    () -> Unit,
    onUnpin:  () -> Unit
) {
    val rowBg         = if (selected) SurfaceActive else Color.Transparent
    val nameColor     = if (selected) Accent else TextPrimary
    val secondaryColor= if (selected) AccentMuted else TextSecondary
    val pinTint       = when {
        isPinned -> Accent
        selected -> AccentMuted
        else     -> TextSecondary
    }

    Row(
        modifier          = Modifier.fillMaxWidth().background(rowBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(50.dp)
                    .background(Accent)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start  = if (selected) 10.dp else 12.dp,
                    end    = 8.dp,
                    top    = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 44)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = contact.name,
                    color    = nameColor,
                    style    = contactListPrimaryTextStyle(selected),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text     = contact.number,
                    color    = secondaryColor,
                    style    = contactListSecondaryTextStyle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BgElevated)
                    .clickable { if (isPinned) onUnpin() else onPin() }
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Favorite,
                    contentDescription = if (isPinned) "Unpin" else "Pin",
                    tint               = pinTint,
                    modifier           = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BgElevated)
                    .clickable { onCall() }
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Phone,
                    contentDescription = "Call",
                    tint               = Accent,
                    modifier           = Modifier.size(15.dp)
                )
            }
        }
    }
}

// ── Pixel: search bar ────────────────────────────────────────────────────────

@Composable
private fun PixelSearchBar(
    displayQuery:   String,
    onMenuClick:    () -> Unit,
    onVoiceSearch:  () -> Unit,
    onContactsOpen: () -> Unit = {},
    modifier:       Modifier = Modifier
) {
    Box(
        modifier         = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BgElevated)
            .border(1.dp, Border, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(44.dp).clip(CircleShape).clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }

            Text(
                text     = if (displayQuery.isEmpty()) "Search contacts" else displayQuery,
                color    = if (displayQuery.isEmpty()) TextSecondary else TextPrimary,
                style    = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier         = Modifier.size(44.dp).clip(CircleShape).clickable { onVoiceSearch() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }

            Box(
                modifier         = Modifier.size(44.dp).clip(CircleShape).clickable { onContactsOpen() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Contacts, contentDescription = "Contacts", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
