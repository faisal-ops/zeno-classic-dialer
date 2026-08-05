package com.zeno.dialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeno.dialer.R
import com.zeno.dialer.data.Voicemail

/** Playback state for whichever voicemail is currently expanded, if any. */
data class VoicemailPlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
)

@Composable
fun VoicemailList(
    voicemails: List<Voicemail>,
    expandedId: Long?,
    playback: VoicemailPlaybackState,
    carrierSupported: Boolean = true,
    modifier: Modifier = Modifier,
    onTap: (Voicemail) -> Unit,
    onPlayPause: (Voicemail) -> Unit,
    onDelete: (Voicemail) -> Unit,
    onCallBack: (Voicemail) -> Unit,
) {
    if (voicemails.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(
                    if (carrierSupported) R.string.no_voicemails
                    else R.string.voicemail_carrier_unsupported
                ),
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(voicemails, key = { it.id }) { vm ->
            Column {
                VoicemailRow(
                    voicemail = vm,
                    expanded = vm.id == expandedId,
                    onTap = { onTap(vm) }
                )
                if (vm.id == expandedId) {
                    VoicemailExpandedPanel(
                        voicemail = vm,
                        playback = playback,
                        onPlayPause = { onPlayPause(vm) },
                        onDelete = { onDelete(vm) },
                        onCallBack = { onCallBack(vm) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoicemailRow(voicemail: Voicemail, expanded: Boolean, onTap: () -> Unit) {
    if (IsPixel) PixelVoicemailRow(voicemail, expanded, onTap)
    else ClassicVoicemailRow(voicemail, expanded, onTap)
}

@Composable
private fun PixelVoicemailRow(voicemail: Voicemail, expanded: Boolean, onTap: () -> Unit) {
    val rowBg = if (expanded) SurfaceActive else Color.Transparent
    val nameColor = if (voicemail.isRead) TextPrimary else Accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(name = voicemail.name ?: voicemail.number, photoUri = voicemail.photoUri, size = 44)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = voicemail.name ?: voicemail.number,
                color = nameColor,
                style = contactListPrimaryTextStyle(selected = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = formatRelativeTime(voicemail.date),
                color = TextSecondary,
                style = contactListSecondaryTextStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!voicemail.isRead) {
            Box(modifier = Modifier.size(8.dp).background(Accent, CircleShape))
        }
    }
}

@Composable
private fun ClassicVoicemailRow(voicemail: Voicemail, expanded: Boolean, onTap: () -> Unit) {
    val rowBg = if (expanded) SurfaceActive else Color.Transparent
    val nameColor = if (voicemail.isRead) TextPrimary else Accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .height(68.dp)
            .border(width = 1.dp, color = Border, shape = RoundedCornerShape(0.dp))
            .clickable { onTap() }
            .padding(start = 11.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(name = voicemail.name ?: voicemail.number, photoUri = voicemail.photoUri, size = 42)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = voicemail.name ?: voicemail.number,
                color = nameColor,
                fontSize = 17.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = formatRelativeTime(voicemail.date),
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!voicemail.isRead) {
            Box(modifier = Modifier.size(8.dp).background(Accent, CircleShape))
        }
    }
}

@Composable
private fun VoicemailExpandedPanel(
    voicemail: Voicemail,
    playback: VoicemailPlaybackState,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit,
    onCallBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface)
            .padding(start = 66.dp, end = 14.dp, top = 4.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (voicemail.hasContent) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BgElevated)
                        .clickable { onPlayPause() }
                        .border(1.dp, Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val durationMs = if (playback.durationMs > 0) playback.durationMs
                        else ((voicemail.durationSeconds ?: 0L) * 1000).toInt()
                    val progress = if (durationMs > 0) playback.positionMs.toFloat() / durationMs else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = Accent,
                        trackColor = Border,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatDurationMs(if (playback.isPlaying || playback.positionMs > 0) playback.positionMs else durationMs),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Voicemail,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.voicemail_downloading),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpandedActionButton(
                icon = Icons.Default.Phone,
                label = stringResource(R.string.call),
                onClick = onCallBack,
                modifier = Modifier.weight(1f)
            )
            ExpandedActionButton(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.delete),
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatDurationMs(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
