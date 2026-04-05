package com.zeno.dialer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.SmsManager
import com.zeno.dialer.AppPreferences
import com.zeno.dialer.CallHistoryDetailActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import android.os.SystemClock
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.zeno.dialer.ui.AccentGreen
import com.zeno.dialer.data.Contact
import com.zeno.dialer.data.RecentsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ClassicSheetText = Color(0xFF1A1A1A)
private val ClassicSheetTextMuted = Color(0xFF626262)
private val ClassicSheetDivider = Color(0xFFDDDDDD)

// ── Row types ────────────────────────────────────────────────────────────────

private sealed class ListRow {
    data class DateHeader(val label: String)              : ListRow()
    data class SectionLabel(val title: String)            : ListRow()
    data class Item(val contact: Contact, val idx: Int)   : ListRow()
}

private fun buildRows(results: List<Contact>): List<ListRow> {
    if (results.isEmpty()) return emptyList()

    val today = todayStartMillis()
    val yesterday = today - 86_400_000L
    val rows = ArrayList<ListRow>(results.size + 10)
    var lastDateLabel = ""
    for (i in results.indices) {
        val c = results[i]
        if (!c.isRecent) continue
        val label = c.lastCallTime.toDateLabel(today, yesterday)
        if (label != lastDateLabel) {
            rows.add(ListRow.DateHeader(label))
            lastDateLabel = label
        }
        rows.add(ListRow.Item(c, i))
    }
    return rows
}

// ── ResultsList ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ResultsList(
    results: List<Contact>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onTap: (Int) -> Unit,
    onDoubleTap: (Int) -> Unit,
    onCallNumber: (String) -> Unit = {},
    onEditNumber: (String) -> Unit = {},
    onAddFavorite: (Contact) -> Unit = {},
    onToggleBlocked: (Contact) -> Unit = {},
    isBlocked: (Contact) -> Boolean = { false },
    onDelete: (String) -> Unit = {},
    toggleExpandFlow: kotlinx.coroutines.flow.SharedFlow<Int>? = null,
    onScrollFocusIndexChanged: (Int) -> Unit = {},
) {
    val context   = LocalContext.current
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    val rows = remember(results) { buildRows(results) }
    val rowBringers = remember(results.size) {
        List(results.size) { BringIntoViewRequester() }
    }

    // Inline history panel (Calls screen)
    // History expands only on user tap; when the row is only focused via scroll/navigation,
    // we collapse the history panel.
    var expandedIndex by remember { mutableIntStateOf(-1) }
    var lastExpandEventMs by remember { mutableStateOf(0L) }

    LaunchedEffect(toggleExpandFlow) {
        toggleExpandFlow?.collect { idx ->
            expandedIndex = if (expandedIndex == idx) -1 else idx
            lastExpandEventMs = SystemClock.elapsedRealtime()
        }
    }

    // If selection moves (scroll/navigation focus) and the user hasn't just tapped to expand,
    // hide the inline history.
    LaunchedEffect(selectedIndex) {
        if (expandedIndex < 0) return@LaunchedEffect
        if (selectedIndex == expandedIndex) return@LaunchedEffect
        val age = SystemClock.elapsedRealtime() - lastExpandEventMs
        if (age > 250L) expandedIndex = -1
    }

    val recentsRepo = remember { RecentsRepo(context) }
    val expandedContact = results.getOrNull(expandedIndex)
    var expandedHistory by remember(expandedContact?.number) {
        androidx.compose.runtime.mutableStateOf(emptyList<Contact>())
    }

    LaunchedEffect(expandedContact?.number) {
        val number = expandedContact?.number.orEmpty()
        if (number.isBlank() || expandedContact?.isRecent != true) {
            expandedHistory = emptyList()
            return@LaunchedEffect
        }
        expandedHistory = withContext(Dispatchers.IO) {
            recentsRepo.getHistoryForNumber(number, limit = 3)
        }
    }

    // Contacts-like behavior: move one row at a time and only adjust viewport when needed.
    LaunchedEffect(selectedIndex) {
        if (results.isEmpty() || selectedIndex < 0 || selectedIndex >= rowBringers.size) return@LaunchedEffect
        val flatPos = rows.indexOfFirst { it is ListRow.Item && it.idx == selectedIndex }
        if (flatPos < 0) return@LaunchedEffect
        val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == flatPos }
        if (isVisible) {
            rowBringers[selectedIndex].bringIntoView()
        } else {
            listState.scrollToItem((flatPos - 2).coerceAtLeast(0))
        }
    }

    // Trackpad scroll focus: when the user stops scrolling, mark the contact near the top of
    // the viewport as the CALL-key target (scrollFocusedIndex) without changing selectedIndex.
    // Using a stable key (listState only) prevents the coroutine from restarting on every
    // D-pad key press, which would re-trigger the scroll-stop handler spuriously.
    // rememberUpdatedState ensures the collect closure always sees the latest values without
    // restarting the coroutine.
    val currentRows by rememberUpdatedState(rows)
    val currentResults by rememberUpdatedState(results)
    val currentOnScrollFocusIndexChanged by rememberUpdatedState(onScrollFocusIndexChanged)
    LaunchedEffect(listState) {
        // Gate: only act on scroll-stop events caused by a real user drag (trackpad/touch).
        // D-pad navigation calls scrollToItem() programmatically which also flips
        // isScrollInProgress, but never fires DragInteraction.Start — so we skip it.
        var hadUserDrag = false
        launch {
            listState.interactionSource.interactions.collect { interaction ->
                if (interaction is androidx.compose.foundation.interaction.DragInteraction.Start) {
                    hadUserDrag = true
                }
            }
        }
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                if (!hadUserDrag) return@collect
                hadUserDrag = false
                if (currentResults.isEmpty()) return@collect
                // Build a one-shot map from lazy-list position → result index for this emission.
                val lazyToResultIdx = HashMap<Int, Int>()
                var lazyIdx = 0
                for (row in currentRows) {
                    when (row) {
                        is ListRow.DateHeader, is ListRow.SectionLabel -> lazyIdx += 1
                        is ListRow.Item -> { lazyToResultIdx[lazyIdx] = row.idx; lazyIdx += 1 }
                    }
                }
                val anchor = listState.firstVisibleItemIndex + 1
                for (li in anchor..(anchor + 10)) {
                    val candidate = lazyToResultIdx[li] ?: continue
                    if (candidate in currentResults.indices) {
                        currentOnScrollFocusIndexChanged(candidate)
                    }
                    break
                }
            }
    }

    // ── Bottom-sheet state ────────────────────────────────────────────────────
    var actionTarget    by remember { mutableStateOf<Pair<Contact, Int>?>(null) }
    val sheetState      = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Quick-response message state
    var quickMsgTarget by remember { mutableStateOf<Contact?>(null) }
    val quickMsgSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val prefs = context.getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE)
    val quickResponses = remember {
        (0..3).map { i ->
            prefs.getString("${AppPreferences.KEY_QUICK_RESPONSE_PREFIX}$i", null)
                ?: when (i) {
                    0 -> "Can't talk now. What's up?"
                    1 -> "I'll call you right back."
                    2 -> "I'll call you later."
                    else -> "Can't talk now. Call me later?"
                }
        }
    }

    fun dismissSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            actionTarget  = null
        }
    }

    if (results.isEmpty()) {
        EmptyState(modifier = modifier)
        return
    }

    // No nestedScroll dampening: BB Classic Q20 / Zinwa Q25 trackpads send small deltas;
    // the old "slow wheel" hack made lists feel stuck and fighting the user.

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        rows.forEach { row ->
            when (row) {

                is ListRow.DateHeader -> item(
                    key = "dh_${row.label}",
                    contentType = "header"
                ) {
                    DateHeaderRow(label = row.label)
                }

                is ListRow.SectionLabel -> item(
                    key = "sl_${row.title}",
                    contentType = "header"
                ) {
                    SectionLabelRow(title = row.title)
                }

                is ListRow.Item -> {
                    val c   = row.contact
                    val idx = row.idx
                    item(
                        key = "${c.id}_${c.number}",
                        contentType = "contact"
                    ) {
                        Column(
                            modifier = Modifier.bringIntoViewRequester(rowBringers[idx])
                        ) {
                            ContactRow(
                                contact  = c,
                                selected = selectedIndex >= 0 && idx == selectedIndex,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick       = { onTap(idx) },
                                        onDoubleClick = { onDoubleTap(idx) },
                                        onLongClick   = { actionTarget = c to idx }
                                    ),
                            )

                            // Show inline last-2-3 call history below the expanded row (tap-only).
                            if (expandedIndex == idx && expandedHistory.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BgSurface)
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    HistoryList(items = expandedHistory.take(3))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Action bottom sheet ───────────────────────────────────────────────────
    actionTarget?.let { (contact, _) ->
        ModalBottomSheet(
            onDismissRequest = { dismissSheet() },
            sheetState       = sheetState,
            shape            = RectangleShape,
            containerColor   = Color.White,
            tonalElevation   = 0.dp,
            dragHandle       = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .background(Color(0xFFBBBBBB))
                    )
                }
            }

        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
            // Header — flat classic bar
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 48)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = contact.name,
                        color      = ClassicSheetText,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text     = contact.number,
                        color    = ClassicSheetTextMuted,
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(color = ClassicSheetDivider, thickness = 1.dp)

            // Options — BB-style flat list; primary “Call” in accent blue
            SheetOption(
                icon  = Icons.Default.Phone,
                label = "Call",
                tint  = AccentGreen
            ) {
                onCallNumber(contact.number)
                dismissSheet()
            }

            if (contact.id > 0L) {
                SheetOption(
                    icon  = Icons.Default.Edit,
                    label = "Edit contact"
                ) {
                    context.startActivity(
                        Intent(Intent.ACTION_EDIT).apply {
                            data = ContentUris.withAppendedId(
                                ContactsContract.Contacts.CONTENT_URI, contact.id
                            )
                        }
                    )
                    dismissSheet()
                }
            } else {
                SheetOption(
                    icon  = Icons.Default.PersonAdd,
                    label = "Add to contacts"
                ) {
                    context.startActivity(
                        Intent(Intent.ACTION_INSERT).apply {
                            type  = ContactsContract.Contacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.PHONE, contact.number)
                            putExtra(ContactsContract.Intents.Insert.NAME, contact.name.takeIf { it != contact.number })
                        }
                    )
                    dismissSheet()
                }
            }

            SheetOption(
                icon  = Icons.Default.Favorite,
                label = "Add to favorites"
            ) {
                onAddFavorite(contact)
                dismissSheet()
            }

            SheetOption(
                icon  = Icons.Default.Block,
                label = if (isBlocked(contact)) "Remove from blocked list" else "Add to blocked list"
            ) {
                onToggleBlocked(contact)
                dismissSheet()
            }

            SheetOption(
                icon  = Icons.Default.ContentCopy,
                label = "Copy number"
            ) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("phone", contact.number))
                dismissSheet()
            }

            if (contact.isRecent) {
                SheetOption(
                    icon  = Icons.Default.History,
                    label = "View call history"
                ) {
                    context.startActivity(
                        Intent(context, CallHistoryDetailActivity::class.java).apply {
                            putExtra(CallHistoryDetailActivity.EXTRA_NUMBER, contact.number)
                            putExtra(CallHistoryDetailActivity.EXTRA_NAME, contact.name)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                    dismissSheet()
                }
            }

            SheetOption(
                icon  = Icons.Default.Delete,
                label = "Delete from log history",
                tint  = Color(0xFFCC4444)
            ) {
                showDeleteConfirm = true
            }

            Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ── Quick-response message sheet ─────────────────────────────────────────
    quickMsgTarget?.let { contact ->
        ModalBottomSheet(
            onDismissRequest = { quickMsgTarget = null },
            sheetState       = quickMsgSheetState,
            shape            = RectangleShape,
            containerColor   = Color.White,
            tonalElevation   = 0.dp,
            dragHandle       = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .background(Color(0xFFBBBBBB))
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 40)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Send message to",
                            color      = TextSecondary,
                            style      = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text       = contact.name,
                            color      = TextPrimary,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = Border)

                // Quick responses
                quickResponses.forEach { msg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                quickMsgTarget = null
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("smsto:${contact.number}")
                                        putExtra("sms_body", msg)
                                    }
                                )
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.Message,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text  = msg,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Custom message option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            quickMsgTarget = null
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${contact.number}")
                                }
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = null,
                        tint               = TextSecondary,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text  = "Write your own…",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    if (showDeleteConfirm) {
        val number = actionTarget?.first?.number ?: ""
        Dialog(
            onDismissRequest = { showDeleteConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = BgElevated,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 22.dp, vertical = 18.dp)
                        .widthIn(max = 420.dp)
                ) {
                    Text(
                        text = "Delete call log?",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "All call history for $number will be permanently removed.",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancel", color = AccentGreen, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        TextButton(
                            onClick = {
                                showDeleteConfirm = false
                                dismissSheet()
                                onDelete(number)
                            }
                        ) {
                            Text("Delete", color = Color(0xFFCC4444), fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── History sub-list ──────────────────────────────────────────────────────────

@Composable
private fun HistoryList(items: List<Contact>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        if (items.isEmpty()) {
            Text(
                text     = "No history found",
                color    = TextSecondary,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            return@Column
        }
        items.forEach { entry ->
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallTypeIcon(callType = entry.callType)
                Spacer(Modifier.width(10.dp))
                Text(
                    text  = callTypeLabel(entry.callType),
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text  = entry.lastCallTime.toTimeAgo(),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun callTypeLabel(type: Int) = when (type) {
    CallLog.Calls.MISSED_TYPE   -> "Missed"
    CallLog.Calls.INCOMING_TYPE -> "Incoming"
    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
    else                        -> "Call"
}

// ── Sheet option row ─────────────────────────────────────────────────────────

@Composable
private fun SheetOption(
    icon: ImageVector,
    label: String,
    tint: Color = ClassicSheetText,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = tint,
                modifier           = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                color = tint,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
        HorizontalDivider(color = ClassicSheetDivider, thickness = 1.dp)
    }
}

// ── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.History,
                contentDescription = null,
                tint               = TextHint,
                modifier           = Modifier.size(56.dp)
            )
            Text(
                text  = "No results",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text  = "Your recent calls and contacts will appear here",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ── Section headers ──────────────────────────────────────────────────────────

@Composable
private fun DateHeaderRow(label: String) {
    Text(
        text     = label,
        color    = TextPrimary,
        style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp),
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .padding(top = 7.dp, bottom = 4.dp)
    )
}

@Composable
private fun SectionLabelRow(title: String) {
    Text(
        text     = title.uppercase(),
        color    = TextSecondary,
        style    = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 3.dp)
    )
}

// ── Contact row ──────────────────────────────────────────────────────────────

@Composable
private fun ContactRow(
    contact: Contact,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val rowBg = if (selected) SurfaceActive else Color.Transparent
    val isMissed = contact.isRecent && contact.callType == CallLog.Calls.MISSED_TYPE
    val nameColor = if (isMissed) Color(0xFFC0392B) else TextPrimary
    val secondaryColor = TextSecondary

    Row(
        modifier = modifier
            .background(rowBg)
            .height(68.dp)
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(0.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMissed) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxSize()
                    .background(Color(0xFFC0392B))
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 11.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 42)

            Spacer(Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    color = nameColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (contact.isRecent) {
                        CallTypeIcon(callType = contact.callType)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = contact.number,
                        color = secondaryColor,
                        fontSize = 14.sp,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right side: time + chevron (matches HTML layout)
            if (contact.isRecent && contact.lastCallTime > 0L) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = contact.lastCallTime.toTimeAgo(),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 14.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Expanded inline panel (stock-dialer style) ──────────────────────────────

@Composable
private fun ExpandedPanel(
    contact: Contact,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface)
            .padding(start = 66.dp, end = 14.dp, top = 4.dp, bottom = 8.dp)
    ) {
        // Last call time
        if (contact.isRecent && contact.lastCallTime > 0) {
            Text(
                text  = formatRelativeTime(contact.lastCallTime),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "See more in History",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onHistory() }
            )
            Spacer(Modifier.height(8.dp))
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpandedActionButton(
                icon = Icons.Default.Phone,
                label = "Call",
                onClick = onCall,
                modifier = Modifier.weight(1f)
            )
            ExpandedActionButton(
                icon = Icons.AutoMirrored.Filled.Message,
                label = "Message",
                onClick = onMessage,
                modifier = Modifier.weight(1f)
            )
            ExpandedActionButton(
                icon = Icons.Default.History,
                label = "History",
                onClick = onHistory,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ExpandedActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1  -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24   -> "$hours hr ago"
        days < 7     -> "$days days ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

// ── Avatar ───────────────────────────────────────────────────────────────────

@Composable
internal fun ContactAvatar(name: String, photoUri: String?, size: Int) {
    Box(
        modifier         = Modifier
            .size(size.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier         = Modifier
                .matchParentSize()
                .background(avatarColor(name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = name.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                color      = Color.White,
                fontSize   = (size * 0.42f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (photoUri != null) {
            val context = LocalContext.current
            val imageRequest = remember(photoUri) {
                ImageRequest.Builder(context)
                    .data(photoUri)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(150)
                    .size(size * 3)
                    .build()
            }
            AsyncImage(
                model              = imageRequest,
                contentDescription = null,
                modifier           = Modifier.matchParentSize(),
                contentScale       = ContentScale.Crop
            )
        }
    }
}

// ── Call type icon ────────────────────────────────────────────────────────────

@Composable
private fun CallTypeIcon(callType: Int) {
    val (icon, color) = when (callType) {
        CallLog.Calls.MISSED_TYPE ->
            Icons.AutoMirrored.Filled.CallMissed to Color(0xFFC0392B)
        CallLog.Calls.INCOMING_TYPE ->
            Icons.AutoMirrored.Filled.CallReceived to TextSecondary
        CallLog.Calls.OUTGOING_TYPE ->
            Icons.AutoMirrored.Filled.CallMade to Accent
        else ->
            Icons.AutoMirrored.Filled.PhoneCallback to TextSecondary
    }
    Icon(
        imageVector        = icon,
        contentDescription = null,
        tint               = color,
        modifier           = Modifier.size(14.dp)
    )
}

// ── Secondary line ───────────────────────────────────────────────────────────

private fun buildSecondaryLine(contact: Contact): String {
    return if (contact.isRecent && contact.lastCallTime > 0L) {
        "${contact.number}  •  ${contact.lastCallTime.toTimeAgo()}"
    } else {
        contact.number
    }
}

// ── Date / time formatters ───────────────────────────────────────────────────

private fun Long.toDateLabel(): String = toDateLabel(todayStartMillis(), yesterdayStartMillis())

private fun todayStartMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun yesterdayStartMillis(): Long = todayStartMillis() - 86_400_000L

private fun Long.toDateLabel(todayStart: Long, yesterdayStart: Long): String {
    return when {
        this >= todayStart     -> "Today"
        this >= yesterdayStart -> "Yesterday"
        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(this))
    }
}

private fun Long.toTimeAgo(): String {
    val diff = System.currentTimeMillis() - this
    val min  = diff / 60_000
    val hr   = diff / 3_600_000
    val day  = diff / 86_400_000
    return when {
        diff < 60_000 -> "Just now"
        min  < 60     -> "${min}m ago"
        hr   < 24     -> "${hr}h ago"
        day  < 7      -> "${day}d ago"
        else          -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
    }
}
