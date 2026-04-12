package com.zeno.dialer

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.TelecomManager
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.zeno.dialer.data.Contact
import com.zeno.dialer.ui.DialerScreen
import com.zeno.dialer.ui.DialerTab
import com.zeno.dialer.ui.theme.DialerTheme
import com.zeno.dialer.data.FilterMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: DialerViewModel by viewModels()
    private lateinit var keyHandler: KeyHandler
    private val showAccessibilityPrompt = mutableStateOf(false)
    private val requestedTab = mutableStateOf<DialerTab?>(null)
    private val isDefaultDialer = mutableStateOf(false)
    // True while the system role-picker is on screen — prevents re-launching on the
    // onResume that fires right after the picker is dismissed.
    private var roleRequestInFlight = false
    // True if the user explicitly cancelled/declined this session. We still show a
    // banner (see below) but don't force the system picker again until next launch.
    private var roleDeclinedThisSession = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        viewModel.clearQuery()
        if (grants[android.Manifest.permission.READ_CALL_LOG] == true) {
            viewModel.onPermissionsReady()
        }
    }

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        roleRequestInFlight = false
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            isDefaultDialer.value = true
        } else {
            // User declined — don't spam the picker again this session
            roleDeclinedThisSession = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BB Classic: blue status bar with light icons.
        window.statusBarColor = 0xFF1278C8.toInt()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        isDefaultDialer.value = getSystemService(RoleManager::class.java)
            .isRoleHeld(RoleManager.ROLE_DIALER)

        keyHandler = KeyHandler(
            viewModel = viewModel,
            onFinish = { finish() },
            onDial = { number -> placeCall(number) },
            onOpenContact = { contact -> openContactInContactsApp(contact) },
            onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
            onDebugTrace = {},
        )

        requestRequiredPermissions()
        observeCallEvents()
        observeCallEndToResetCallKeyTargets()
        val fromToolbarCallKeyOpen =
            intent?.getBooleanExtra(
                com.zeno.dialer.service.ButtonInterceptService.EXTRA_CALL_BUTTON_PRESSED,
                false
            ) == true
        if (fromToolbarCallKeyOpen) {
            // Same as onNewIntent: cold start after Main was finished must not leave stale
            // row focus / lastDial retention from a previous session.
            intent?.removeExtra(com.zeno.dialer.service.ButtonInterceptService.EXTRA_CALL_BUTTON_PRESSED)
            viewModel.resetCallKeyTargetState()
            viewModel.setCurrentTab(0)
        }
        routeDialerIntents(intent)
        if (intent?.getBooleanExtra("open_keypad", false) == true) {
            requestedTab.value = DialerTab.KEYPAD
        }
        registerToolbarButtons()

        setContent {
            DialerTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!BuildConfig.DEBUG) {
                        DefaultDialerOverlay()
                    }
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        DialerScreen(
                            viewModel = viewModel,
                            requestedTab = requestedTab.value,
                            onOpenContact = { contact -> openContactInContactsApp(contact) },
                        )
                    }
                }
                if (!BuildConfig.DEBUG) {
                    AccessibilityPromptDialog()
                }
            }
        }
    }

    // Clear callbacks when Activity goes to background so that
    // ButtonInterceptService falls back to openDefaultDialer() which
    // properly brings the Activity to the foreground via an Intent.
    override fun onStop() {
        super.onStop()
        com.zeno.dialer.service.ToolbarButtonHandler.onCallPressed = null
        com.zeno.dialer.service.ToolbarButtonHandler.onEndPressed = null
    }

    // Re-register toolbar button callbacks every time MainActivity comes to foreground.
    // This restores them after onStop cleared them.
    override fun onResume() {
        super.onResume()
        viewModel.onPermissionsReady()

        // Debug preview mode: skip default-dialer and accessibility prompting.
        if (BuildConfig.DEBUG) return

        // Sync the observable role state so the banner reacts immediately.
        isDefaultDialer.value = getSystemService(RoleManager::class.java)
            .isRoleHeld(RoleManager.ROLE_DIALER)

        val prefs = getSharedPreferences(AppPreferences.FILE_ZENO, MODE_PRIVATE)

        // First launch flow: permissions → accessibility prompt → default dialer.
        // Only show accessibility prompt after all permissions are granted.
        val allPermissionsGranted = requiredPermissions().all {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            val prompted = prefs.contains(AppPreferences.KEY_ACCESSIBILITY_PROMPTED) &&
                prefs.getBoolean(AppPreferences.KEY_ACCESSIBILITY_PROMPTED, false)
            val promptKeyMissing = !prefs.contains(AppPreferences.KEY_ACCESSIBILITY_PROMPTED)
            val accessibilityEnabled = isAccessibilityServiceEnabled()
            if (!accessibilityEnabled && (promptKeyMissing || !prompted)) {
                prefs.edit().putBoolean(AppPreferences.KEY_ACCESSIBILITY_PROMPTED, true).apply()
                promptAccessibilityService()
            } else {
                launchRolePickerIfNeeded()
            }
        }
        com.zeno.dialer.service.ToolbarButtonHandler.onCallPressed = {
            runOnUiThread {
                Log.i("ZenoDialer", "Toolbar onCallPressed callback fired (onResume)")
                dialOrOpenKeypad()
            }
        }
        com.zeno.dialer.service.ToolbarButtonHandler.onEndPressed = {
            runOnUiThread {
                val callInfo = CallStateHolder.info.value
                if (callInfo != null &&
                    callInfo.state != android.telecom.Call.STATE_DISCONNECTED &&
                    callInfo.state != android.telecom.Call.STATE_DISCONNECTING
                ) {
                    CallStateHolder.hangup()
                } else {
                    // No active call — close the app (same as physical BACK)
                    finish()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_keypad", false)) {
            // Force a change so DialerScreen's LaunchedEffect re-runs even if it's already on Keypad.
            requestedTab.value = null
            requestedTab.value = DialerTab.KEYPAD
        }
        // Call button pressed from background via AccessibilityService
        if (intent.getBooleanExtra(
                com.zeno.dialer.service.ButtonInterceptService.EXTRA_CALL_BUTTON_PRESSED, false
            )) {
            intent.removeExtra(com.zeno.dialer.service.ButtonInterceptService.EXTRA_CALL_BUTTON_PRESSED)
            // Background open via hardware Call key should only foreground the dialer.
            // Do not auto-dial based on any stale selection/focus.
            viewModel.resetCallKeyTargetState()
            viewModel.setCurrentTab(0)
            return
        }
        routeDialerIntents(intent)
    }

    /**
     * Routes [Intent.ACTION_DIAL], [Intent.ACTION_CALL], and [Intent.ACTION_VIEW].
     * [ACTION_VIEW] with [tel:] or call-log [content:] opens the UI; [ACTION_CALL] places a call.
     */
    private fun routeDialerIntents(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_CALL -> {
                val number = intent.data?.schemeSpecificPart?.trim().orEmpty()
                if (number.isNotBlank()) placeCall(number)
                // singleTop keeps the launch Intent; a later REORDER_TO_FRONT must not re-run CALL.
                neutralizeStickyDialIntent()
            }
            Intent.ACTION_DIAL -> {
                val number = intent.data?.schemeSpecificPart?.trim().orEmpty()
                if (number.isNotBlank()) {
                    viewModel.setCurrentTab(2)
                    viewModel.setQueryDirect(number)
                } else {
                    dialOrOpenKeypad()
                }
                neutralizeStickyDialIntent()
            }
            Intent.ACTION_VIEW -> {
                val data = intent.data ?: return
                when (data.scheme) {
                    "tel" -> {
                        val number = data.schemeSpecificPart?.trim().orEmpty()
                        if (number.isNotBlank()) {
                            viewModel.setCurrentTab(0)
                            viewModel.setQueryDirect(number)
                            neutralizeStickyDialIntent()
                        }
                    }
                    "content" -> openFromCallLogUri(data)
                    else -> { }
                }
            }
            else -> { }
        }
    }

    /** Replace a consumed dial deep-link so it cannot fire again on the same activity instance. */
    private fun neutralizeStickyDialIntent() {
        setIntent(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
            }
        )
    }

    /** Missed-call / call-history notification taps (content://call_log/…). */
    private fun openFromCallLogUri(uri: Uri) {
        val host = uri.host ?: return
        if (host != "call_log" && !uri.toString().contains("call_log")) return

        viewModel.setCurrentTab(0)
        viewModel.setFilter(FilterMode.MISSED)

        val id = try {
            ContentUris.parseId(uri)
        } catch (_: Exception) {
            0L
        }
        if (id <= 0L) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.query(
                    ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, id),
                    arrayOf(CallLog.Calls.NUMBER),
                    null,
                    null,
                    null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val num = c.getString(0)?.trim().orEmpty()
                        if (num.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                viewModel.setQueryDirect(num)
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun dialOrOpenKeypad() {
        val state = viewModel.uiState.value
        when {
            state.query.isNotBlank() -> viewModel.callSelected()
            state.selectedIndex >= 0 &&
                state.selectedIndex < state.results.size -> viewModel.callSelected()
            state.scrollFocusedIndex >= 0 &&
                state.scrollFocusedIndex < state.results.size -> viewModel.callSelected()
            // Do not dial results[0] with no explicit focus — after End + Call, that
            // wrongly redialed the top recent. Open Calls tab only (same as KEYCODE_CALL fallback).
            else -> viewModel.setCurrentTab(0)
        }
    }

    // ── Call event observer ──────────────────────────────────────────────────

    private fun observeCallEvents() {
        lifecycleScope.launch {
            viewModel.callEvent.collect { number ->
                placeCall(number)
            }
        }
    }

    /**
     * After a call disconnects, clear row focus and the 3s post-dial selection retention in
     * [DialerViewModel.forceRefresh]; otherwise the next toolbar/Call key redials that row.
     */
    private fun observeCallEndToResetCallKeyTargets() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var hadActiveCall = false
                CallStateHolder.info
                    .map { info ->
                        info != null &&
                            info.state != Call.STATE_DISCONNECTED &&
                            info.state != Call.STATE_DISCONNECTING
                    }
                    .distinctUntilChanged()
                    .collect { active ->
                        if (hadActiveCall && !active) {
                            viewModel.resetCallKeyTargetState()
                        }
                        hadActiveCall = active
                    }
            }
        }
    }

    private fun placeCall(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Phone permission required", Toast.LENGTH_SHORT).show()
            requestRequiredPermissions()
            return
        }
        val target = number.trim()
        if (target.isBlank()) {
            return
        }
        val uri = Uri.fromParts("tel", target, null)
        try {
            com.zeno.dialer.service.MyInCallService.armOutgoingCallActiveVibration()
            val telecom = getSystemService(TelecomManager::class.java)
            if (telecom == null) {
                tryActionCallFallback(uri)
                return
            }
            telecom.placeCall(uri, android.os.Bundle.EMPTY)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Telecom failed (${e.javaClass.simpleName}), trying ACTION_CALL",
                Toast.LENGTH_LONG
            ).show()
            tryActionCallFallback(uri)
        }
    }

    private fun tryActionCallFallback(uri: Uri) {
        try {
            startActivity(
                Intent(Intent.ACTION_CALL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (fallbackError: Exception) {
            Toast.makeText(
                this,
                "Fallback failed: ${fallbackError.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openContactInContactsApp(contact: Contact) {
        try {
            if (contact.id > 0L) {
                val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id)
                startActivity(
                    Intent(Intent.ACTION_VIEW, contactUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                return
            }
            val target = contact.number.trim()
            if (target.isBlank()) return
            val telUri = Uri.fromParts("tel", target, null)
            startActivity(Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, telUri))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open contact: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Physical keyboard / D-pad / toolbar key routing ──────────────────────
    // dispatchKeyEvent fires BEFORE the Compose view hierarchy, so we always
    // get first pick on every key — BACK, CALL, ENDCALL, DPAD, letters, etc.

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_CALL) {
                Log.i("ZenoDialer", "dispatchKeyEvent KEYCODE_CALL")
            }
            // Key-repeat for DPAD_UP/DOWN is now intentionally allowed so holding the
            // key scrolls continuously. The allowDpadNavStep() throttle in KeyHandler
            // (80 ms) prevents runaway scrolling on fast auto-repeat.

            if (viewModel.isOnKeypad) {
                val kcm = event.device?.keyCharacterMap
                    ?: KeyCharacterMap.load(event.deviceId)
                val numberChar = kcm.getNumber(event.keyCode)
                if (keyHandler.handleKeypad(event.keyCode, numberChar.code)) {
                    return true
                }
            }

            val altChar = event.getUnicodeChar(KeyEvent.META_ALT_ON)
                .takeIf { it > 0 }
                ?: event.getUnicodeChar(KeyEvent.META_ALT_LEFT_ON).takeIf { it > 0 }
                ?: 0
            if (keyHandler.handle(event.keyCode, event.unicodeChar, altChar)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ── Toolbar button callbacks (via AccessibilityService) ────────────────

    private fun registerToolbarButtons() {
        com.zeno.dialer.service.ToolbarButtonHandler.onCallPressed = {
            runOnUiThread {
                Log.i("ZenoDialer", "Toolbar onCallPressed callback fired (registerToolbarButtons)")
                dialOrOpenKeypad()
            }
        }
        com.zeno.dialer.service.ToolbarButtonHandler.onEndPressed = {
            runOnUiThread {
                val callInfo = CallStateHolder.info.value
                if (
                    callInfo != null &&
                    callInfo.state != android.telecom.Call.STATE_DISCONNECTED &&
                    callInfo.state != android.telecom.Call.STATE_DISCONNECTING
                ) {
                    CallStateHolder.hangup()
                } else {
                    finish()
                }
            }
        }

        // Accessibility prompt is now shown before the default dialer role request
        // in onResume(), so it's not needed here.
    }

    private fun promptAccessibilityService() {
        showAccessibilityPrompt.value = true
    }

    private fun launchRolePickerIfNeeded() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (!isDefaultDialer.value
            && !roleRequestInFlight
            && !roleDeclinedThisSession
        ) {
            roleRequestInFlight = true
            roleRequestLauncher.launch(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            )
        }
    }

    @Composable
    private fun AccessibilityPromptDialog() {
        if (!showAccessibilityPrompt.value) return

        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showAccessibilityPrompt.value = false
                launchRolePickerIfNeeded()
            },
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                androidx.compose.material3.Text(
                    text = "Enable toolbar buttons",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                androidx.compose.material3.Text(
                    text = "Allow Zeno Classic Dialer to use the Call and End " +
                           "hardware buttons on your keyboard toolbar.\n\n" +
                           "Find \"Zeno Classic Dialer\" in the list and enable it.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showAccessibilityPrompt.value = false
                    startActivity(
                        Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    )
                }) {
                    androidx.compose.material3.Text("Open Settings")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showAccessibilityPrompt.value = false
                    launchRolePickerIfNeeded()
                }) {
                    androidx.compose.material3.Text("Not now")
                }
            }
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains("$packageName/") &&
               enabledServices.contains("ButtonInterceptService")
    }

    override fun onDestroy() {
        super.onDestroy()
        com.zeno.dialer.service.ToolbarButtonHandler.onCallPressed = null
        com.zeno.dialer.service.ToolbarButtonHandler.onEndPressed = null
    }

    // ── Default dialer banner ────────────────────────────────────────────────
    // Shown at the top of the screen when ROLE_DIALER is not held.
    // Non-blocking: the dialer is fully usable, but calls from the system
    // Contacts app won't reach us until we are the default phone app.

    @Composable
    private fun DefaultDialerOverlay() {
        if (isDefaultDialer.value) return

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    if (!roleRequestInFlight) {
                        roleRequestInFlight = true
                        roleDeclinedThisSession = false
                        val rm = getSystemService(RoleManager::class.java)
                        roleRequestLauncher.launch(
                            rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.compose.material3.Text(
                text = "Tap to set as default phone app",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    private fun requestRequiredPermissions() {
        // Debug "preview mode": don't prompt for the full permission set while iterating on UI.
        if (BuildConfig.DEBUG) {
            viewModel.onPermissionsReady()
            return
        }
        val needed = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    companion object {
        private fun requiredPermissions(): Array<String> {
            val list = mutableListOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECORD_AUDIO
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return list.toTypedArray()
        }
    }
}
