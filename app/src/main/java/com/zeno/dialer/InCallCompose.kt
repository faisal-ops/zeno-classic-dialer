package com.zeno.dialer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.Call
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.zeno.dialer.service.CallRecorder
import com.zeno.dialer.ui.Accent
import com.zeno.dialer.ui.AccentMuted
import com.zeno.dialer.ui.BgElevated
import com.zeno.dialer.ui.BgSurface
import com.zeno.dialer.ui.Border
import com.zeno.dialer.ui.IsModernClassic
import com.zeno.dialer.ui.TextHint
import com.zeno.dialer.ui.TextPrimary
import com.zeno.dialer.ui.TextSecondary
import com.zeno.dialer.ui.avatarColor

// ── In-call Compose UI ──────────────────────────────────────────────────────

/** Matches BB Classic HTML `aI(name)`: two-word → first + last initial; else first character. */
private fun contactInitials(displayName: String): String {
    val n = displayName.trim()
    if (n.isEmpty()) return "?"
    val parts = n.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> {
            val a = parts.first().firstOrNull()?.uppercaseChar() ?: '?'
            val b = parts.last().firstOrNull()?.uppercaseChar() ?: '?'
            "$a$b"
        }
        else -> n.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
}

@Composable
private fun InCallHeaderSection(
    displayName: String,
    number: String,
    photoUri: String?,
    state: Int,
    elapsedSeconds: Int,
    secondCall: ActiveCallInfo?,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val avatarDp = if (compact) 72.dp else 96.dp
    val initialsSp = if (compact) 40.sp else 56.sp
    val gradH = if (compact) 56.dp else 80.dp
    Box(modifier = modifier.background(avatarColor(displayName))) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                val req = remember(photoUri) {
                    ImageRequest.Builder(context)
                        .data(photoUri)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(150)
                        .size(288)
                        .build()
                }
                AsyncImage(
                    model = req,
                    contentDescription = "Contact photo for $displayName",
                    modifier = Modifier
                        .size(avatarDp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = contactInitials(displayName),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = initialsSp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(gradH)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = if (IsModernClassic) 30.sp else 22.sp,
                        fontWeight = if (IsModernClassic) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = if (IsModernClassic) 33.sp else 25.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (number.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = number,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                val timerOrState = when {
                    state == Call.STATE_ACTIVE -> elapsedSeconds.toCallDuration()
                    else -> stateLabel(state)
                }
                Text(
                    text = timerOrState,
                    color = Color.White,
                    fontSize = if (IsModernClassic) 20.sp else 17.sp,
                    fontWeight = if (IsModernClassic) FontWeight.Medium else FontWeight.Light,
                    letterSpacing = 1.sp,
                    fontFamily = if (state == Call.STATE_ACTIVE) FontFamily.Monospace else FontFamily.Default,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            if (secondCall != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryCallBanner(
                    info = secondCall,
                    onSwitch = { CallStateHolder.swap() }
                )
            }
        }
    }
}

@Composable
internal fun InCallScreen(vm: InCallViewModel, onQuickReply: (String) -> Unit) {
    // Ensures we can close immediately on button taps (without waiting for state propagation).
    // This reduces "stuck on Call Ended" flashes.
    val info       by CallStateHolder.info.collectAsStateWithLifecycle()
    val secondCall by CallStateHolder.secondCall.collectAsStateWithLifecycle()
    val elapsed    by vm.elapsedSeconds.collectAsStateWithLifecycle()

    val state = info?.state ?: Call.STATE_DISCONNECTED
    val context = LocalContext.current

    var recordingActive by remember { mutableStateOf(false) }
    var speakerEnabledForRecording by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun startRecording(displayName: String, number: String) {
        val svc = com.zeno.dialer.service.MyInCallService.instance
        val wasSpeaker = svc?.isSpeakerOn() ?: false
        if (!wasSpeaker) {
            svc?.applySpeaker(true)
            speakerEnabledForRecording = true
        }
        // Delay 300ms to let audio routing settle before starting capture.
        coroutineScope.launch {
            kotlinx.coroutines.delay(300)
            val path = CallRecorder.start(context.applicationContext, displayName, number)
            recordingActive = CallRecorder.isRecording
            if (path == null) {
                if (speakerEnabledForRecording) {
                    svc?.applySpeaker(false)
                    speakerEnabledForRecording = false
                }
                Toast.makeText(context, "Could not start recording", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Recording started — speaker enabled to capture both sides", Toast.LENGTH_LONG).show()
            }
        }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Microphone permission required for recording", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val i = CallStateHolder.info.value ?: return@rememberLauncherForActivityResult
        startRecording(i.displayName, i.number)
    }

    LaunchedEffect(info?.state) {
        info?.state?.let { vm.onStateChanged(it) }
    }

    LaunchedEffect(info?.call) {
        recordingActive = CallRecorder.isRecording
    }

    if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) return

    val displayName = info?.displayName ?: "Unknown"
    val number = info?.number.orEmpty()

    // Audio controls via InCallService (the correct Telecom API for default dialers).
    val svc = com.zeno.dialer.service.MyInCallService.instance
    var micMuted  by remember { mutableStateOf(svc?.isMuted()     ?: false) }
    var speakerOn by remember { mutableStateOf(svc?.isSpeakerOn() ?: false) }

    val endBarHeight = 48.dp * 1.2f * 1.1f * 1.1f
    val controlRowHeight = 48.dp * 1.1f
    // Active call: header vs controls +20% then +10% twice from base 0.38; end bar same scale on 48.dp.
    val headerWeightActive = 0.38f * 1.2f * 1.1f * 1.1f
    val controlsWeightActive = 1f - headerWeightActive

    if (state == Call.STATE_RINGING) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2C2C2C))
        ) {
            val halfHeight = maxHeight / 2
            Column(Modifier.fillMaxSize()) {
                InCallHeaderSection(
                    displayName = displayName,
                    number = number,
                    photoUri = info?.photoUri,
                    state = state,
                    elapsedSeconds = elapsed,
                    secondCall = secondCall,
                    compact = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(halfHeight)
                )
                RingingPanel(
                    onQuickReply = onQuickReply,
                    onDecline = { CallStateHolder.reject() },
                    onAnswer = { CallStateHolder.answer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(halfHeight)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (IsModernClassic) Color.Black else Color(0xFF2C2C2C))
    ) {
        // Header uses upper portion; white control panel expands into the rest (no grey spacer band).
        Box(
            modifier = Modifier
                .weight(headerWeightActive)
                .fillMaxWidth()
        ) {
            InCallHeaderSection(
                displayName = displayName,
                number = number,
                photoUri = info?.photoUri,
                state = state,
                elapsedSeconds = elapsed,
                secondCall = secondCall,
                compact = false,
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth()
                    .heightIn(min = 145.dp)
            )
        }
        InCallControlsPanel(
                state = state,
                micMuted = micMuted,
                speakerOn = speakerOn,
                controlRowHeight = controlRowHeight,
                expandRows = true,
                onToggleMute = {
                    micMuted = !micMuted
                    com.zeno.dialer.service.MyInCallService.instance?.applyMute(micMuted)
                },
                onToggleSpeaker = {
                    speakerOn = !speakerOn
                    com.zeno.dialer.service.MyInCallService.instance?.applySpeaker(speakerOn)
                },
                onKeypad = {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            putExtra("open_keypad", true)
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                    )
                },
                onHoldToggle = {
                    if (state == Call.STATE_HOLDING) CallStateHolder.unhold()
                    else CallStateHolder.hold()
                },
                recordingActive = recordingActive,
                onRecord = {
                    if (CallRecorder.isRecording) {
                        val savedPath = CallRecorder.stop(context)
                        recordingActive = false
                        if (speakerEnabledForRecording) {
                            com.zeno.dialer.service.MyInCallService.instance?.applySpeaker(false)
                            speakerEnabledForRecording = false
                        }
                        if (savedPath != null) {
                            Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Recording ended (file too short or error)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        startRecording(displayName, number)
                    }
                },
                onAddCall = {
                    CallStateHolder.hold()
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            putExtra("open_keypad", true)
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                    )
                },
            modifier = Modifier
                .weight(controlsWeightActive)
                .fillMaxWidth()
        )
        EndCallBar(
            onEnd = { CallStateHolder.hangup() },
            modifier = Modifier
                .fillMaxWidth()
                .height(endBarHeight)
        )
    }
}

@Composable
private fun RingingPanel(
    onQuickReply: (String) -> Unit,
    onDecline: () -> Unit,
    onAnswer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showQuickReplies by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (!showQuickReplies) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(BgElevated)
                    .clickable { showQuickReplies = true }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Message",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Message",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CallActionButton(
                        icon = Icons.Default.Call,
                        backgroundColor = Color(0xFF5A8A1C),
                        iconColor = Color.White,
                        accessibilityLabel = "Answer call",
                        onClick = onAnswer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Answer", color = TextSecondary, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CallActionButton(
                        icon = Icons.Default.CallEnd,
                        backgroundColor = Color(0xFFC0392B),
                        iconColor = Color.White,
                        accessibilityLabel = "Decline call",
                        onClick = onDecline
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Decline", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            val prefs = context.getSharedPreferences(AppPreferences.FILE_SETTINGS, android.content.Context.MODE_PRIVATE)
            val quickReplies = (0..3).map { i ->
                prefs.getString("${AppPreferences.KEY_QUICK_RESPONSE_PREFIX}$i", null) ?: listOf(
                    "Can't talk now. What's up?",
                    "I'll call you right back.",
                    "I'll call you later.",
                    "Can't talk now. Call me later?"
                )[i]
            }
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(horizontal = 2.dp)
                        .padding(top = 12.dp)
                ) {
                    quickReplies.forEach { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQuickReply(msg) }
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Message,
                                contentDescription = "Quick reply",
                                tint = Accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = msg, color = TextPrimary, fontSize = 15.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CallActionButton(
                            icon = Icons.Default.Call,
                            backgroundColor = Color(0xFF5A8A1C),
                            iconColor = Color.White,
                            accessibilityLabel = "Answer call",
                            onClick = onAnswer
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Answer", color = TextSecondary, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CallActionButton(
                            icon = Icons.Default.CallEnd,
                            backgroundColor = Color(0xFFC0392B),
                            iconColor = Color.White,
                            accessibilityLabel = "Decline call",
                            onClick = onDecline
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Decline", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InCallControlsPanel(
    state: Int,
    micMuted: Boolean,
    speakerOn: Boolean,
    controlRowHeight: Dp,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onKeypad: () -> Unit,
    onHoldToggle: () -> Unit,
    recordingActive: Boolean,
    onRecord: () -> Unit,
    onAddCall: () -> Unit,
    modifier: Modifier = Modifier,
    expandRows: Boolean = false,
) {
    val isMC = IsModernClassic
    val controlIconDp = 19.dp * 1.1f
    val controlLabelSp = (10f * 1.1f).sp
    val controlLineSp = (11f * 1.1f).sp
    val controlIconLabelGap = 4.dp * 1.1f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (expandRows) Modifier.fillMaxHeight() else Modifier)
            .background(if (isMC) BgSurface else Color.White)
            .then(if (isMC) Modifier.padding(horizontal = 8.dp, vertical = 6.dp) else Modifier)
    ) {
        ControlRow(
            height = controlRowHeight,
            iconSize = controlIconDp,
            labelFontSize = controlLabelSp,
            labelLineHeight = controlLineSp,
            iconLabelGap = controlIconLabelGap,
            expand = expandRows,
            left = ControlCellDef(Icons.AutoMirrored.Filled.VolumeUp, "Speaker", speakerOn, onToggleSpeaker),
            middle = ControlCellDef(if (micMuted) Icons.Default.MicOff else Icons.Default.Mic, "Mute", micMuted, onToggleMute),
            right = ControlCellDef(Icons.Default.Dialpad, "Dial Pad", false, onKeypad),
        )
        if (isMC) Spacer(Modifier.height(6.dp))
        else HorizontalDivider(color = Border, thickness = 1.dp)
        ControlRow(
            height = controlRowHeight,
            iconSize = controlIconDp,
            labelFontSize = controlLabelSp,
            labelLineHeight = controlLineSp,
            iconLabelGap = controlIconLabelGap,
            expand = expandRows,
            left = ControlCellDef(Icons.Default.Pause, if (state == Call.STATE_HOLDING) "Unhold" else "Hold", state == Call.STATE_HOLDING, onHoldToggle),
            middle = ControlCellDef(Icons.Filled.FiberManualRecord, "Record", recordingActive, onClick = onRecord),
            right = ControlCellDef(Icons.Default.PersonAdd, "Add a Call", false, onAddCall),
        )
    }
}

private data class ControlCellDef(
    val icon: ImageVector,
    val label: String,
    val active: Boolean,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
private fun ColumnScope.ControlRow(
    height: Dp,
    left: ControlCellDef,
    middle: ControlCellDef,
    right: ControlCellDef,
    iconSize: Dp = 19.dp,
    labelFontSize: TextUnit = 10.sp,
    labelLineHeight: TextUnit = 11.sp,
    iconLabelGap: Dp = 4.dp,
    expand: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (expand) Modifier.weight(1f)
                else Modifier.height(height)
            )
    ) {
        ControlCell(
            def = left,
            iconSize = iconSize,
            labelFontSize = labelFontSize,
            labelLineHeight = labelLineHeight,
            iconLabelGap = iconLabelGap,
            modifier = Modifier.weight(1f)
        )
        Box(Modifier.width(1.dp).fillMaxSize().background(Border))
        ControlCell(
            def = middle,
            iconSize = iconSize,
            labelFontSize = labelFontSize,
            labelLineHeight = labelLineHeight,
            iconLabelGap = iconLabelGap,
            modifier = Modifier.weight(1f)
        )
        Box(Modifier.width(1.dp).fillMaxSize().background(Border))
        ControlCell(
            def = right,
            iconSize = iconSize,
            labelFontSize = labelFontSize,
            labelLineHeight = labelLineHeight,
            iconLabelGap = iconLabelGap,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ControlCell(
    def: ControlCellDef,
    modifier: Modifier = Modifier,
    iconSize: Dp = 19.dp,
    labelFontSize: TextUnit = 10.sp,
    labelLineHeight: TextUnit = 11.sp,
    iconLabelGap: Dp = 4.dp,
) {
    val isMC = IsModernClassic
    val tint = when {
        !def.enabled -> TextHint
        def.active   -> Accent
        else         -> if (isMC) TextPrimary else TextSecondary
    }
    val cardBg = when {
        def.active -> Accent.copy(alpha = 0.15f)
        else       -> BgElevated
    }
    if (isMC) {
        // Modern Classic: each control is an individual dark rounded card
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(cardBg)
                .clickable(enabled = def.enabled) { def.onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = def.icon,
                    contentDescription = def.label,
                    tint = tint,
                    modifier = Modifier.size(iconSize * 1.1f)
                )
                Spacer(Modifier.height(iconLabelGap))
                Text(
                    text       = def.label,
                    color      = tint,
                    fontSize   = labelFontSize,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 2,
                    lineHeight = labelLineHeight,
                    overflow   = TextOverflow.Ellipsis,
                    textAlign  = TextAlign.Center
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .clickable(enabled = def.enabled) { def.onClick() }
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = def.icon, contentDescription = def.label, tint = tint, modifier = Modifier.size(iconSize))
            Spacer(Modifier.height(iconLabelGap))
            Text(
                text       = def.label,
                color      = tint,
                fontSize   = labelFontSize,
                fontWeight = FontWeight.Medium,
                maxLines   = 2,
                lineHeight = labelLineHeight,
                overflow   = TextOverflow.Ellipsis,
                textAlign  = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EndCallBar(onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val isMC = IsModernClassic
    if (isMC) {
        // Modern Classic: floating rounded pill on dark background
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(BgSurface)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .semantics { contentDescription = "End call" },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFD9534F), Color(0xFFA93226))
                        )
                    )
                    .clickable { onEnd() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "End",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFC0392B))
                .semantics { contentDescription = "End call" }
                .clickable { onEnd() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "End", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Secondary call banner ─────────────────────────────────────────────────────

@Composable
private fun SecondaryCallBanner(info: ActiveCallInfo, onSwitch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .border(1.dp, Border, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(BgSurface, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text       = info.displayName,
                color      = MaterialTheme.colorScheme.onSurface,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text     = stateLabel(info.state),
                color    = stateColor(info.state),
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BgElevated)
                .clickable { onSwitch() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.SwapCalls,
                contentDescription = "Switch call",
                tint               = Accent,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

// ── Call action button (large circle) ────────────────────────────────────────

@Composable
private fun CallActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    accessibilityLabel: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .semantics { contentDescription = accessibilityLabel }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ── Small action button (mid-call controls) ──────────────────────────────────

@Composable
private fun SmallActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    bgColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val resolvedBg = if (bgColor == Color.Unspecified) BgElevated else bgColor
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(69.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(resolvedBg)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun stateLabel(state: Int): String = when (state) {
    Call.STATE_RINGING       -> "Incoming call"
    Call.STATE_DIALING       -> "Calling..."
    Call.STATE_CONNECTING    -> "Connecting..."
    Call.STATE_ACTIVE        -> "Active"
    Call.STATE_HOLDING       -> "On Hold"
    Call.STATE_DISCONNECTING -> "Ending..."
    Call.STATE_DISCONNECTED  -> "Call Ended"
    else -> ""
}

private fun stateColor(state: Int): Color = when (state) {
    Call.STATE_RINGING                          -> Color(0xFFD4A033)
    Call.STATE_ACTIVE                           -> Color(0xFF6BCB77)
    Call.STATE_HOLDING                          -> Color(0xFF8E8E93)
    Call.STATE_DISCONNECTED,
    Call.STATE_DISCONNECTING                    -> Color(0xFFCC4444)
    else                                        -> Color(0xFF5A9EC7)
}
