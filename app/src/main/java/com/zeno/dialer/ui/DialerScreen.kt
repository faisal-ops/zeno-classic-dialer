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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
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
    KEYPAD("Dial Pad", R.drawable.ic_bb_dialpad)
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
    val tab        = tabFromIndex(state.currentTabIndex)
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
                animationSpec = tween(150),
                label         = "tab_transition"
            ) { currentTab ->
                when (currentTab) {
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

            BottomNavBar(current = tab, onSelect = { viewModel.setCurrentTab(tabToIndex(it)) })
        }

        // ── Scrim ────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showMenu,
            enter   = fadeIn(tween(150)),
            exit    = fadeOut(tween(150))
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
            enter   = slideInHorizontally(tween(150, easing = EaseInOutCubic)) { -it },
            exit    = slideOutHorizontally(tween(150, easing = EaseInOutCubic)) { -it }
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
}

private fun tabFromIndex(index: Int): DialerTab = when (index) {
    0 -> DialerTab.CALLS
    2 -> DialerTab.KEYPAD
    else -> DialerTab.CONTACTS
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

    val contactsOnly = remember(state.results) {
        if (state.results.isEmpty()) emptyList()
        else {
            val filtered = ArrayList<Contact>(state.results.size)
            for (c in state.results) { if (!c.isRecent) filtered.add(c) }
            filtered.sortBy { it.name.lowercase() }
            filtered
        }
    }

    // Flat list built once per contactsOnly change — avoids re-running the loop on every recomposition.
    val flatRows = remember(contactsOnly) {
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

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val selectedContact = remember(state.selectedIndex, state.results) {
        state.results.getOrNull(state.selectedIndex)?.takeIf { !it.isRecent }
    }
    val rowBringers = remember(contactsOnly) {
        List(contactsOnly.size) { BringIntoViewRequester() }
    }

    // Gate: only act on scroll-stop caused by a real user drag (trackpad/touch).
    // D-pad navigation scrollToItem() never fires DragInteraction.Start — so we skip it,
    // preventing selectedIndex from being cleared on programmatic scrolls.
    val currentContactsOnly by rememberUpdatedState(contactsOnly)
    val currentResults by rememberUpdatedState(state.results)
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
                    focusManager.clearFocus()
                } else {
                    if (!hadUserDrag) return@collect
                    hadUserDrag = false
                    if (currentContactsOnly.isEmpty()) return@collect
                    var lazyIdx = 0
                    var lastLetter: Char? = null
                    val lazyToRowIndex = HashMap<Int, Int>()
                    currentContactsOnly.forEachIndexed { rowIndex, contact ->
                        val letter = contact.name.firstOrNull()?.uppercaseChar() ?: '#'
                        if (lastLetter != letter) { lazyIdx += 1; lastLetter = letter }
                        lazyToRowIndex[lazyIdx] = rowIndex
                        lazyIdx += 1
                    }
                    val anchor = listState.firstVisibleItemIndex + 1
                    for (li in anchor..(anchor + 6)) {
                        val rowIndex = lazyToRowIndex[li] ?: continue
                        val contact = currentContactsOnly.getOrNull(rowIndex) ?: continue
                        val resultIdx = currentResults.indexOfFirst { !it.isRecent && it.number == contact.number }
                        if (resultIdx >= 0) viewModel.setScrollFocusedIndex(resultIdx)
                        break
                    }
                }
            }
    }

    // Contacts-like behavior: move one row at a time and only adjust viewport when needed.
    LaunchedEffect(state.selectedIndex) {
        if (state.selectedIndex < 0 || contactsOnly.isEmpty()) return@LaunchedEffect
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
                listState.scrollToItem((flatIdx - 2).coerceAtLeast(0))
            }
        } catch (_: NoSuchElementException) { }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Contacts search bar (matches HTML prototype)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White)
                .border(width = 2.dp, color = Accent, shape = RoundedCornerShape(0.dp))
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(BgPage)
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
                            is ContactRow.Header -> "section_${row.letter}_${row.rowIndex}"
                            is ContactRow.Item -> "contact_${row.contact.id}_${row.rowIndex}_${row.contact.number}"
                        }
                    }
                ) { row ->
                    when (row) {
                        is ContactRow.Header -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .background(Color(0xFFF2F2F2))
                                    .border(1.dp, Border, RoundedCornerShape(0.dp)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = row.letter.toString(),
                                    color = Accent,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 10.dp)
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
    ) {
        // Prototype pass: keep exactly 5 tiles visible across.
        val tileWidth = maxWidth / 5
        val tileHeight = 108.dp
        LazyRow(
            state = rowState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
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
    Box(
        modifier = Modifier
            .width(tileW)
            .height(tileH)
            .background(Color.White)
            .border(
                width = if (focused) 5.dp else 1.dp,
                color = if (focused) Accent else Border,
                shape = RoundedCornerShape(0.dp)
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
            if (focused) {
                // Strong focus state so tile selection is obvious during D-pad navigation.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, Color.White, RoundedCornerShape(0.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                )
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = title.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(44.dp)
                .background(Color.Black.copy(alpha = 0.35f))
                .alpha(0.85f)
        )

        Text(
            text = title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp)
            .border(width = 1.dp, color = Border, shape = RoundedCornerShape(0.dp)),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { idx, (mode, label) ->
            val isSel = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSel) BgPage else BgSurface)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSel) Accent else TextHint,
                    fontSize = 13.sp,
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
    val hasInput = state.query.isNotBlank()
    val canCall  = hasInput
    val keypadMatch = state.keypadContactMatch

    val view = LocalView.current
    val context = LocalContext.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Slightly smaller tiles (~10%) so the Enter Number bar can use more vertical space.
        val cellSize = maxWidth / 3 * 0.9f

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── BB Classic blue header: typed digits + contact hint (search-as-you-type) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Accent)
                    .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    var showDialpadMenu by remember { mutableStateOf(false) }
                    val clipboard = remember(context) {
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    }
                    val pasteEnabled = remember(showDialpadMenu) {
                        val item = clipboard.primaryClip?.getItemAt(0) ?: return@remember false
                        normalizePastedDialText(
                            item.coerceToText(context)?.toString().orEmpty()
                        ).isNotEmpty()
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
                                    showDialpadMenu = true
                                }
                            )
                            .semantics {
                                contentDescription =
                                    context.getString(R.string.dialpad_number_bar_a11y)
                            },
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (hasInput) {
                            val digitShadow = Shadow(
                                color = Color.Black.copy(alpha = 0.45f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 4f
                            )
                            Text(
                                text = state.query,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp,
                                lineHeight = 36.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(shadow = digitShadow)
                            )
                            if (keypadMatch != null) {
                                Text(
                                    text = keypadMatch.name,
                                    color = Color.White.copy(alpha = 0.96f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.35f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 3f
                                        )
                                    ),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Enter Number",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp,
                                lineHeight = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showDialpadMenu,
                        onDismissRequest = { showDialpadMenu = false },
                        modifier = Modifier
                            .widthIn(min = 212.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = Color.Black.copy(alpha = 0.12f),
                                spotColor = Color.Black.copy(alpha = 0.18f)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.Black.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        offset = DpOffset(0.dp, 6.dp)
                    ) {
                        val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
                        val titleStyle = MaterialTheme.typography.bodyLarge
                        val hintStyle = MaterialTheme.typography.bodySmall

                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.dialpad_paste),
                                        style = titleStyle,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.dialpad_paste_hint),
                                        style = hintStyle,
                                        color = hintColor.copy(
                                            alpha = if (pasteEnabled) 0.85f else 0.38f
                                        )
                                    )
                                }
                            },
                            onClick = {
                                showDialpadMenu = false
                                val item = clipboard.primaryClip?.getItemAt(0) ?: return@DropdownMenuItem
                                val pasted = normalizePastedDialText(
                                    item.coerceToText(context)?.toString().orEmpty()
                                )
                                if (pasted.isNotEmpty()) viewModel.setQueryDirect(pasted)
                            },
                            enabled = pasteEnabled,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (pasteEnabled) 1f else 0.38f
                                    )
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            contentPadding = MenuDefaults.DropdownMenuItemContentPadding
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = Color.Black.copy(alpha = 0.06f),
                            thickness = 1.dp
                        )
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.dialpad_copy),
                                        style = titleStyle,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.dialpad_copy_hint),
                                        style = hintStyle,
                                        color = hintColor.copy(
                                            alpha = if (hasInput) 0.85f else 0.38f
                                        )
                                    )
                                }
                            },
                            onClick = {
                                showDialpadMenu = false
                                if (state.query.isNotBlank()) {
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("phone", state.query)
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.dialpad_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = hasInput,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (hasInput) 1f else 0.38f
                                    )
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            contentPadding = MenuDefaults.DropdownMenuItemContentPadding
                        )
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
                        tint = Color.White.copy(alpha = 0.9f),
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

            // ── Call + Backspace row (flush under dialpad; taller green bar) ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .border(width = 1.dp, color = Border, shape = RoundedCornerShape(0.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // Prototype parity: call bar remains green even when number is empty.
                        .background(Color(0xFF5A8A1C))
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text("Call", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF9E9E9E))
                        .clickable(enabled = hasInput) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.deleteChar()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = Color.White,
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
    val padDesc = when (digit) {
        '*' -> "star"
        '#' -> "pound"
        else -> digit.toString()
    }
    Box(
        modifier         = Modifier
            .size(size)
            .semantics { contentDescription = "Dial pad key $padDesc" }
            .background(if (pressed) BgElevated else Color.White)
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (showRightDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Border)
            )
        }
        if (showBottomDivider) {
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

// ── Bottom nav bar ───────────────────────────────────────────────────────────

@Composable
private fun BottomNavBar(current: DialerTab, onSelect: (DialerTab) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgPage)
            // Prevent bottom bar from being clipped by system nav/gesture area.
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
                // Prototype nav is 48px; insets handled by windowInsetsPadding above.
                .height(70.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DialerTab.entries.forEach { tab ->
                NavItem(
                    iconRes = tab.iconRes,
                    label = tab.label,
                    selected = current == tab,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    @DrawableRes iconRes: Int,
    label:    String,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = if (selected) Accent else TextSecondary
    val textColor = if (selected) Accent else TextSecondary

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(2.5.dp)
                .background(if (selected) Accent else Color.Transparent)
        )
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Icon(
                imageVector        = Icons.Default.Phone,
                contentDescription = null,
                tint               = Accent,
                modifier           = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text       = info.displayName,
                    color      = TextPrimary,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(text = stateText, color = Accent, fontSize = 14.sp)
            }
        }
        Text(
            text       = "End Call",
            color      = Color(0xFFC0392B),
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFE8E8))
                .clickable { onEndCall() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
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
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = TextSecondary,
            modifier           = Modifier.size(24.dp)
        )
        Text(
            text  = label,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge
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
