package com.zeno.dialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeno.dialer.data.Contact
import com.zeno.dialer.data.RecentsRepo
import com.zeno.dialer.ui.Accent
import com.zeno.dialer.ui.AccentGreen
import com.zeno.dialer.ui.AccentMuted
import com.zeno.dialer.ui.BgElevated
import com.zeno.dialer.ui.BgPage
import com.zeno.dialer.ui.BgSurface
import com.zeno.dialer.ui.Border
import com.zeno.dialer.ui.ContactAvatar
import com.zeno.dialer.ui.TextPrimary
import com.zeno.dialer.ui.TextSecondary
import com.zeno.dialer.ui.theme.DialerTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CallHistoryDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val number = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty().ifBlank { number }

        setContent {
            DialerTheme {
                CallHistoryDetailScreen(
                    number = number,
                    name = name,
                    onBack = { finish() },
                    onCall = {
                        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
                    },
                    onMessage = {
                        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")))
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_NUMBER = "extra_number"
        const val EXTRA_NAME = "extra_name"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallHistoryDetailScreen(
    number: String,
    name: String,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    val context = LocalContext.current
    var allItems by remember { mutableStateOf<List<Contact>>(emptyList()) }

    LaunchedEffect(number) {
        allItems = RecentsRepo(context).getHistoryForNumber(number)
    }

    // Group entries by date label
    val grouped = remember(allItems) {
        allItems.groupBy { it.lastCallTime.toDateLabel() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // ── Top bar: back + avatar + name/number + overflow ──────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            Spacer(Modifier.width(4.dp))

            ContactAvatar(name = name, photoUri = null, size = 40)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Mobile • $number",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { /* overflow menu placeholder */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
            }
        }

        // ── Call history entries ─────────────────────────────────────────
        if (allItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No history", color = TextSecondary)
            }
        } else {
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                grouped.forEach { (dateLabel, entries) ->
                    // Date header
                    item(key = "hdr_$dateLabel", contentType = 0) {
                        Text(
                            text = dateLabel,
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                    }

                    // Call entries as cards
                    items(
                        items = entries,
                        key = { "${it.lastCallTime}_${it.callType}_${it.number}" },
                        contentType = { 1 }
                    ) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(BgElevated, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, tint) = callTypeIcon(item.callType)
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = callTypeLabel(item.callType),
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatTime(item.lastCallTime),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        // ── Bottom action bar: Call + Message ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Call button (pill shape, green)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(AccentGreen)
                    .clickable { onCall() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // Message button
            ActionButton(
                icon = Icons.AutoMirrored.Filled.Message,
                label = "Message",
                onClick = onMessage
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(BgElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

private fun callTypeIcon(type: Int) = when (type) {
    CallLog.Calls.MISSED_TYPE    -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFCC4444)
    CallLog.Calls.INCOMING_TYPE  -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF6BCB77)
    CallLog.Calls.OUTGOING_TYPE  -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF6BCB77)
    else                         -> Icons.AutoMirrored.Filled.PhoneCallback to Color(0xFF8E8E93)
}

private fun callTypeLabel(type: Int) = when (type) {
    CallLog.Calls.MISSED_TYPE    -> "Missed call"
    CallLog.Calls.INCOMING_TYPE  -> "Incoming call"
    CallLog.Calls.OUTGOING_TYPE  -> "Outgoing call"
    CallLog.Calls.BLOCKED_TYPE   -> "Blocked call"
    CallLog.Calls.REJECTED_TYPE  -> "Rejected call"
    else                         -> "Call"
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

private fun Long.toDateLabel(): String {
    val cal = Calendar.getInstance()
    val now = Calendar.getInstance()
    cal.timeInMillis = this

    return when {
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> "Today"

        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 -> "Yesterday"

        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(this))
    }
}
