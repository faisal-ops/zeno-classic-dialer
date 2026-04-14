package com.zeno.dialer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Voicemail
import android.provider.CallLog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeno.dialer.ui.Accent
import com.zeno.dialer.ui.BgPage
import com.zeno.dialer.ui.BgElevated
import com.zeno.dialer.ui.Border
import com.zeno.dialer.ui.SurfaceActive
import com.zeno.dialer.ui.TextHint
import com.zeno.dialer.ui.TextPrimary
import com.zeno.dialer.ui.TextSecondary
import com.zeno.dialer.ui.AccentGreen
import com.zeno.dialer.ui.theme.DialerStyle
import com.zeno.dialer.ui.theme.DialerTheme
import com.zeno.dialer.ui.theme.LocalDialerStyle
import com.zeno.dialer.AppPreferences
import com.zeno.dialer.R

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DialerTheme {
                SettingsRoot(onBack = { finish() })
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Let the Compose BackHandler handle BACK key presses
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            return super.dispatchKeyEvent(event)
        }
        // Consume ENDCALL so it doesn't close the activity unexpectedly
        if (event.keyCode == KeyEvent.KEYCODE_ENDCALL) {
            if (event.action == KeyEvent.ACTION_DOWN) finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

// ── Defaults ─────────────────────────────────────────────────────────────────

private val DEFAULT_QUICK_RESPONSES = listOf(
    "Can't talk now. What's up?",
    "I'll call you right back.",
    "I'll call you later.",
    "Can't talk now. Call me later?"
)

private val SettingsTileTitleSize = 16.sp
private val SettingsTileSecondarySize = 13.sp

// ── Navigation ───────────────────────────────────────────────────────────────

private enum class SettingsPage {
    MAIN, CALLS, FLIP_TO_SILENCE, CALLER_ID_ANNOUNCEMENT, VOICEMAIL, CONTACT_RINGTONES,
    CALLING_CARD, QUICK_RESPONSES, DISPLAY_OPTIONS, CALLER_ID_SPAM, ASSISTED_DIALING
}

@Composable
private fun SettingsRoot(onBack: () -> Unit) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    val goMain = { currentPage = SettingsPage.MAIN }

    // Always-present BackHandler that routes based on current page
    BackHandler {
        if (currentPage != SettingsPage.MAIN) {
            currentPage = SettingsPage.MAIN
        } else {
            onBack()
        }
    }

    when (currentPage) {
        SettingsPage.MAIN  -> SettingsScreen(onBack = onBack, onNavigate = { currentPage = it })
        SettingsPage.CALLS -> CallsScreen(onBack = goMain)
        SettingsPage.FLIP_TO_SILENCE -> FlipToSilenceScreen(onBack = goMain)
        SettingsPage.CALLER_ID_ANNOUNCEMENT -> CallerIdAnnouncementScreen(onBack = goMain)
        SettingsPage.VOICEMAIL -> VoicemailScreen(onBack = goMain)
        SettingsPage.CONTACT_RINGTONES -> ContactRingtonesScreen(onBack = goMain)
        SettingsPage.CALLING_CARD -> CallingCardScreen(onBack = goMain)
        SettingsPage.QUICK_RESPONSES -> QuickResponsesScreen(onBack = goMain)
        SettingsPage.DISPLAY_OPTIONS -> DisplayOptionsScreen(onBack = goMain)
        SettingsPage.CALLER_ID_SPAM -> CallerIdSpamScreen(onBack = goMain)
        SettingsPage.ASSISTED_DIALING -> AssistedDialingScreen(onBack = goMain)
    }
}

// ── Main settings screen ─────────────────────────────────────────────────────

@Composable
private fun SettingsScreen(onBack: () -> Unit, onNavigate: (SettingsPage) -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().background(BgPage)
    ) {
        SettingsTopBar(title = "Settings", onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Call Assist
            item { SectionHeader("Call Assist") }
            item {
                SettingsNavItem(icon = Icons.Default.Block, title = "Caller ID & spam",
                    showDividerBelow = false,
                    onClick = { onNavigate(SettingsPage.CALLER_ID_SPAM) })
            }

            // General
            item { SectionHeader("General") }
            item {
                SettingsNavItem(icon = Icons.Default.Settings, title = "Accessibility",
                    onClick = {
                        launchSafe(context,
                            Intent("android.telecom.action.SHOW_CALL_ACCESSIBILITY_SETTINGS"),
                            fallbackAction = Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    })
            }
            item {
                SettingsNavItem(icon = Icons.Default.Phone, title = "Assisted dialing",
                    onClick = { onNavigate(SettingsPage.ASSISTED_DIALING) })
            }
            item {
                SettingsNavItem(icon = Icons.Default.Block, title = "Blocked numbers",
                    onClick = {
                        launchSafe(context,
                            Intent("android.telecom.action.MANAGE_BLOCKED_NUMBERS"),
                            fallbackAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            fallbackData = "package:${context.packageName}")
                    })
            }
            item {
                SettingsNavItem(icon = Icons.Default.Call, title = "Calls",
                    onClick = { onNavigate(SettingsPage.CALLS) })
            }
            item {
                SettingsNavItem(icon = Icons.Default.Settings, title = "Display options",
                    onClick = { onNavigate(SettingsPage.DISPLAY_OPTIONS) })
            }
            item {
                SettingsNavItem(icon = Icons.Default.QuestionAnswer, title = "Quick responses",
                    onClick = { onNavigate(SettingsPage.QUICK_RESPONSES) })
            }
            item {
                SettingsNavItem(icon = Icons.Default.MusicNote, title = "Sounds and vibration",
                    onClick = { launchSafe(context, Intent(Settings.ACTION_SOUND_SETTINGS)) })
            }
            item {
                SettingsNavItem(icon = Icons.Default.Voicemail, title = "Voicemail",
                    onClick = { onNavigate(SettingsPage.VOICEMAIL) })
            }
            item {
                SettingsNavItem(icon = Icons.Default.MusicNote, title = "Contact ringtones",
                    onClick = { onNavigate(SettingsPage.CONTACT_RINGTONES) })
            }
            item {
                SettingsNavItem(icon = Icons.Default.Person, title = "Calling card",
                    showDividerBelow = false,
                    onClick = { onNavigate(SettingsPage.CALLING_CARD) })
            }

            // Advanced
            item { SectionHeader("Advanced") }
            item {
                SettingsNavItem(icon = Icons.Default.Notifications, title = "Caller ID announcement",
                    onClick = { onNavigate(SettingsPage.CALLER_ID_ANNOUNCEMENT) })
            }
            item {
                SettingsNavItem(icon = Icons.Default.Phone, title = "Flip To Silence",
                    onClick = { onNavigate(SettingsPage.FLIP_TO_SILENCE) })
            }

            // About
            item { SectionHeader("About") }
            item {
                SettingsNavItem(icon = Icons.Default.Info, title = "About Zeno Classic Dialer",
                    subtitle = "Version ${getAppVersion(context)}",
                    showDividerBelow = false,
                    onClick = {
                        launchSafe(context,
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            })
                    })
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

// ── Calls ─────────────────────────────────────────────────────────────────────

@Composable
private fun CallsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        SettingsTopBar(title = "Calls", onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionHeader("Call history") }
            item {
                SettingsNavItem(
                    icon           = Icons.Default.Delete,
                    title          = "Clear call history",
                    showDividerBelow = false,
                    onClick        = { showClearDialog = true }
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title   = { Text("Clear call history?") },
            text    = { Text("All call log entries will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        context.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
                    } catch (_: Exception) { }
                    showClearDialog = false
                    onBack()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Caller ID & Spam ─────────────────────────────────────────────────────────

@Composable
private fun CallerIdSpamScreen(onBack: () -> Unit) {
    val prefs = rememberPrefs()
    var seeCallerId by remember { mutableStateOf(prefs.getBoolean(AppPreferences.KEY_CALLER_SPAM_ID, true)) }
    var filterSpam by remember { mutableStateOf(prefs.getBoolean(AppPreferences.KEY_FILTER_SPAM, false)) }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        SettingsTopBar(title = "Caller ID & spam", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        ToggleRow(
            title = "See caller and spam ID",
            subtitle = "Identify business and spam numbers",
            checked = seeCallerId,
            showDividerBelow = true,
            onToggle = { seeCallerId = it; prefs.edit().putBoolean(AppPreferences.KEY_CALLER_SPAM_ID, it).apply() }
        )

        ToggleRow(
            title = "Filter spam calls",
            subtitle = "Prevent suspected spam calls from disturbing you",
            checked = filterSpam,
            onToggle = { filterSpam = it; prefs.edit().putBoolean(AppPreferences.KEY_FILTER_SPAM, it).apply() }
        )

        Spacer(Modifier.height(24.dp))

        InfoRow("Zeno Classic Dialer will attempt to show you useful information when you make or receive a call, such as a name for a number not in your contacts or a warning when an incoming call is suspected to be spam.")
    }
}

// ── Assisted Dialing ─────────────────────────────────────────────────────────

@Composable
private fun AssistedDialingScreen(onBack: () -> Unit) {
    val prefs = rememberPrefs()
    var enabled by remember { mutableStateOf(prefs.getBoolean(AppPreferences.KEY_ASSISTED_DIALING, false)) }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        SettingsTopBar(title = "Assisted dialing", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        ToggleRow(
            title = "Assisted dialing",
            checked = enabled,
            onToggle = { enabled = it; prefs.edit().putBoolean(AppPreferences.KEY_ASSISTED_DIALING, it).apply() }
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Default home country", color = TextPrimary, fontSize = SettingsTileTitleSize)
                Spacer(Modifier.height(4.dp))
                Text("Automatically detected", color = TextSecondary, fontSize = SettingsTileSecondarySize)
            }
        }

        Spacer(Modifier.height(8.dp))
        SettingsDivider()
        Spacer(Modifier.height(16.dp))

        InfoRow("Assisted dialing predicts and adds a country code when you call while traveling abroad.")
    }
}

// ── Quick Responses ──────────────────────────────────────────────────────────

@Composable
private fun QuickResponsesScreen(onBack: () -> Unit) {
    val prefs = rememberPrefs()
    val responses = remember {
        (0..3).map { i ->
            mutableStateOf(
                prefs.getString("${AppPreferences.KEY_QUICK_RESPONSE_PREFIX}$i", DEFAULT_QUICK_RESPONSES[i])
                    ?: DEFAULT_QUICK_RESPONSES[i]
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        SettingsTopBar(title = "Edit quick responses", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        responses.forEachIndexed { index, state ->
            var text by state

            BasicTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    prefs.edit().putString("${AppPreferences.KEY_QUICK_RESPONSE_PREFIX}$index", newText).apply()
                },
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 17.sp
                ),
                cursorBrush = SolidColor(AccentGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )

            if (index < responses.lastIndex) {
                SettingsDivider()
            }
        }
    }
}

// ── Display Options ──────────────────────────────────────────────────────────

@Composable
private fun DisplayOptionsScreen(onBack: () -> Unit) {
    val prefs = rememberPrefs()
    var portraitMode by remember { mutableStateOf(prefs.getBoolean(AppPreferences.KEY_PORTRAIT_MODE, true)) }
    var themeChoice by remember { mutableIntStateOf(prefs.getInt(AppPreferences.KEY_DIALER_STYLE, AppPreferences.DIALER_STYLE_MODERN_CLASSIC)) }
    val themes = listOf("Original Classic", "Modern Classic", "Pixel")

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        SettingsTopBar(title = "Display options", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        SectionLabel("Appearance")
        PickerRow(
            title = "Choose theme",
            currentValue = themes[themeChoice.coerceIn(0, themes.lastIndex)],
            options = themes,
            selectedIndex = themeChoice.coerceIn(0, themes.lastIndex),
            onSelect = {
                themeChoice = it
                prefs.edit()
                    .putInt(AppPreferences.KEY_DIALER_STYLE, it)
                    .apply()
            }
        )

        Spacer(Modifier.height(16.dp))

        SectionLabel("Controls")
        ToggleRow(
            title = "Keep portrait mode on calls",
            subtitle = "Prevents accidental auto-rotation when on a call",
            checked = portraitMode,
            onToggle = { portraitMode = it; prefs.edit().putBoolean(AppPreferences.KEY_PORTRAIT_MODE, it).apply() }
        )

    }
}

// ── Flip To Silence ──────────────────────────────────────────────────────────

@Composable
private fun FlipToSilenceScreen(onBack: () -> Unit) {
    val prefs = rememberPrefs()
    var enabled by remember { mutableStateOf(prefs.getBoolean(AppPreferences.KEY_FLIP_TO_SILENCE, false)) }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        SettingsTopBar(title = "Flip To Silence", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        ToggleRow(
            title = "Flip To Silence",
            subtitle = "To silence an incoming call, place your device face down on a flat surface",
            checked = enabled,
            onToggle = { enabled = it; prefs.edit().putBoolean(AppPreferences.KEY_FLIP_TO_SILENCE, it).apply() }
        )
    }
}

// ── Caller ID Announcement ───────────────────────────────────────────────────

@Composable
private fun CallerIdAnnouncementScreen(onBack: () -> Unit) {
    val prefs = rememberPrefs()
    var selected by remember { mutableIntStateOf(prefs.getInt(AppPreferences.KEY_CALLER_ID_ANNOUNCE, 0)) }
    val options = listOf("Never", "Always", "Only when using a headset")

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        SettingsTopBar(title = "Caller ID announcement", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        Text("Announce caller ID", color = TextPrimary, fontSize = SettingsTileTitleSize,
            modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(4.dp))
        Text(options[selected], color = TextSecondary, fontSize = SettingsTileSecondarySize,
            modifier = Modifier.padding(horizontal = 20.dp))

        Spacer(Modifier.height(12.dp))

        options.forEachIndexed { index, label ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable {
                        selected = index
                        prefs.edit().putInt(AppPreferences.KEY_CALLER_ID_ANNOUNCE, index).apply()
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == index,
                    onClick = {
                        selected = index
                        prefs.edit().putInt(AppPreferences.KEY_CALLER_ID_ANNOUNCE, index).apply()
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                )
                Spacer(Modifier.width(12.dp))
                Text(label, color = if (selected == index) TextPrimary else TextSecondary, fontSize = SettingsTileTitleSize)
            }
        }

        Spacer(Modifier.height(24.dp))
        InfoRow("The caller\u2019s name and number will be read out loud for incoming calls.")
    }
}

// ── Voicemail ────────────────────────────────────────────────────────────────

@Composable
private fun VoicemailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = rememberPrefs()
    var visualVoicemail by remember { mutableStateOf(prefs.getBoolean(AppPreferences.KEY_VISUAL_VOICEMAIL, false)) }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        SettingsTopBar(title = "Voicemail", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        // Notifications
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable {
                    launchSafe(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                }
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notifications", color = TextPrimary, fontSize = SettingsTileTitleSize, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.height(8.dp))

        ToggleRow(
            title = "Visual voicemail",
            subtitle = "The carrier may not support visual voicemail",
            checked = visualVoicemail,
            onToggle = { visualVoicemail = it; prefs.edit().putBoolean(AppPreferences.KEY_VISUAL_VOICEMAIL, it).apply() }
        )

        Spacer(Modifier.height(16.dp))

        // Advanced Settings
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { launchSafe(context, Intent("android.settings.CALL_SETTINGS")) }
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Advanced Settings", color = TextPrimary, fontSize = SettingsTileTitleSize, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Contact Ringtones ────────────────────────────────────────────────────────

@Composable
private fun ContactRingtonesScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().background(BgPage),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsTopBar(title = "Contact ringtones", onBack = onBack)
        Spacer(Modifier.weight(0.3f))

        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(BgElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, null, tint = AccentGreen, modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Add a custom ringtone to individual contacts to help you recognize who\u2019s calling. To manage the default ringtone for this device, visit Settings.",
            color = TextSecondary, fontSize = 17.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier.clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .clickable {
                    launchSafe(context, Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI))
                }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PersonAdd, null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add contact ringtone",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.weight(0.5f))
    }
}

// ── Calling Card ─────────────────────────────────────────────────────────────

@Composable
private fun CallingCardScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        SettingsTopBar(title = "Calling card", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        // Illustration
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp)).background(BgElevated).padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = AccentGreen, modifier = Modifier.size(64.dp))
        }

        Spacer(Modifier.height(24.dp))

        // Your calling card
        Text("Your calling card", color = TextSecondary, fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        CardRow(
            text = "How you\u2019ll appear to others when making or receiving calls",
            buttonLabel = "Create",
            onClick = {
                launchSafe(context,
                    Intent(Intent.ACTION_VIEW, ContactsContract.Profile.CONTENT_URI),
                    fallbackAction = Intent.ACTION_VIEW,
                    fallbackData = ContactsContract.Contacts.CONTENT_URI.toString())
            }
        )

        Spacer(Modifier.height(24.dp))

        // Contact calling card
        Text("Contact calling card", color = TextSecondary, fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        CardRow(
            text = "How you see your contact\u2019s name and image when they call",
            buttonLabel = "Create",
            onClick = {
                launchSafe(context, Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
            }
        )
    }
}

// ── Shared components ────────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(22.dp))
        }
        Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionHeader(title: String) {
    val spacing = rememberSettingsListSpacing()
    Text(
        text = title.uppercase(),
        color = AccentGreen,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = spacing.sectionHeaderTop,
            bottom = spacing.sectionHeaderBottom
        )
    )
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title, color = TextSecondary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showDividerBelow: Boolean = true,
    onClick: () -> Unit
) {
    val spacing = rememberSettingsListSpacing()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val highlighted = isPressed || isFocused

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                .background(if (highlighted) SurfaceActive else Color.Transparent)
                .padding(vertical = spacing.navRowVertical),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Blue focus bar (same style as call log rows)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(spacing.focusBarHeight)
                    .background(if (highlighted) Accent else Color.Transparent)
            )
            Spacer(Modifier.width(17.dp))
            Box(
                modifier = Modifier
                    .size(spacing.iconContainerSize)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (highlighted) Accent.copy(alpha = 0.15f) else BgElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, title, tint = if (highlighted) Accent else AccentGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (highlighted) Accent else TextPrimary,
                    fontSize = SettingsTileTitleSize,
                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = TextSecondary, fontSize = SettingsTileSecondarySize)
                }
            }
            Icon(
                Icons.Default.ChevronRight, null,
                tint = if (highlighted) Accent else TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(16.dp))
        }
        if (showDividerBelow) {
            ClassicSettingsRowDivider()
        }
    }
}

private data class SettingsListSpacing(
    val sectionHeaderTop: Dp,
    val sectionHeaderBottom: Dp,
    val navRowVertical: Dp,
    val focusBarHeight: Dp,
    val iconContainerSize: Dp,
    val toggleRowVertical: Dp,
    val pickerRowVertical: Dp,
    val compactFocusBarHeight: Dp,
    val cardRowVertical: Dp
)

@Composable
private fun rememberSettingsListSpacing(): SettingsListSpacing {
    return when (LocalDialerStyle.current) {
        DialerStyle.PIXEL -> SettingsListSpacing(
            sectionHeaderTop = 12.dp,
            sectionHeaderBottom = 3.dp,
            navRowVertical = 8.dp,
            focusBarHeight = 30.dp,
            iconContainerSize = 32.dp,
            toggleRowVertical = 8.dp,
            pickerRowVertical = 8.dp,
            compactFocusBarHeight = 30.dp,
            cardRowVertical = 12.dp
        )
        DialerStyle.ORIGINAL_CLASSIC,
        DialerStyle.MODERN_CLASSIC -> SettingsListSpacing(
            sectionHeaderTop = 12.dp,
            sectionHeaderBottom = 3.dp,
            navRowVertical = 8.dp,
            focusBarHeight = 30.dp,
            iconContainerSize = 32.dp,
            toggleRowVertical = 8.dp,
            pickerRowVertical = 8.dp,
            compactFocusBarHeight = 30.dp,
            cardRowVertical = 12.dp
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    showDividerBelow: Boolean = false,
    onToggle: (Boolean) -> Unit
) {
    val spacing = rememberSettingsListSpacing()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val highlighted = isPressed || isFocused

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(interactionSource = interactionSource, indication = null) { onToggle(!checked) }
                .background(if (highlighted) SurfaceActive else Color.Transparent)
                .padding(vertical = spacing.toggleRowVertical),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(spacing.compactFocusBarHeight)
                    .background(if (highlighted) Accent else Color.Transparent)
            )
            Spacer(Modifier.width(17.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (highlighted) Accent else TextPrimary, fontSize = SettingsTileTitleSize,
                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = TextSecondary, fontSize = SettingsTileSecondarySize)
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, checkedTrackColor = AccentGreen,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = BgElevated
                )
            )
            Spacer(Modifier.width(16.dp))
        }
        if (showDividerBelow) {
            SettingsDivider()
        }
    }
}

@Composable
private fun InfoRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = TextSecondary, fontSize = 16.sp)
    }
}

@Composable
private fun CardRow(text: String, buttonLabel: String, onClick: () -> Unit) {
    val spacing = rememberSettingsListSpacing()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp)).background(BgElevated)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = spacing.cardRowVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = TextSecondary, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                buttonLabel,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PickerRow(
    title: String, currentValue: String, options: List<String>,
    selectedIndex: Int, onSelect: (Int) -> Unit
) {
    val spacing = rememberSettingsListSpacing()
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val highlighted = isPressed || isFocused

    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(interactionSource = interactionSource, indication = null) { expanded = !expanded }
                .background(if (highlighted) SurfaceActive else Color.Transparent)
                .padding(vertical = spacing.pickerRowVertical),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(spacing.compactFocusBarHeight)
                    .background(if (highlighted) Accent else Color.Transparent)
            )
            Spacer(Modifier.width(17.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(title, color = if (highlighted) Accent else TextPrimary, fontSize = SettingsTileTitleSize,
                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal)
                Spacer(Modifier.height(4.dp))
                Text(currentValue, color = TextSecondary, fontSize = SettingsTileSecondarySize)
            }
        }

        if (expanded) {
            options.forEachIndexed { index, label ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onSelect(index); expanded = false }
                        .padding(horizontal = 32.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index); expanded = false },
                        colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label,
                        color = if (selectedIndex == index) TextPrimary else TextSecondary,
                        fontSize = SettingsTileTitleSize
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = Border,
        thickness = 0.8.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

/** Inset divider aligned with row text (past icon column), classic list style. */
@Composable
private fun ClassicSettingsRowDivider() {
    HorizontalDivider(
        color = Border,
        thickness = 0.8.dp,
        modifier = Modifier.padding(start = 58.dp, end = 20.dp)
    )
}

@Composable
private fun rememberPrefs(): SharedPreferences {
    val context = LocalContext.current
    return remember { context.getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE) }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun launchSafe(
    context: Context, intent: Intent, fallbackAction: String? = null, fallbackData: String? = null
) {
    runCatching { context.startActivity(intent) }
        .onFailure {
            if (fallbackAction != null) {
                runCatching {
                    context.startActivity(Intent(fallbackAction).apply {
                        if (fallbackData != null) data = Uri.parse(fallbackData)
                    })
                }
            }
        }
}

private fun getAppVersion(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (_: Exception) { "1.0.0" }
}
