package com.zeno.dialer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.Call
import android.telephony.SmsManager
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.zeno.dialer.service.CallRecorder
import com.zeno.dialer.service.MyInCallService
import com.zeno.dialer.ui.theme.DialerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InCallActivity : ComponentActivity() {

    private val vm: InCallViewModel by viewModels()
    private var pendingQuickReply: String? = null

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "SMS permission required for quick reply", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val msg = pendingQuickReply ?: return@registerForActivityResult
        sendQuickReply(msg)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch {
            CallStateHolder.info.collect { info ->
                val s = info?.state
                if (info == null || s == Call.STATE_DISCONNECTED || s == Call.STATE_DISCONNECTING) {
                    CallRecorder.stop(this@InCallActivity)
                }
                // Wait for full DISCONNECTED (not DISCONNECTING) to avoid premature finish.
                // info == null means all calls were cleared (CallStateHolder.clear() was called).
                if (info == null || s == Call.STATE_DISCONNECTED) {
                    // Brief debounce: ignore transient null/disconnected flicker from Telecom.
                    delay(280)
                    val latest = CallStateHolder.info.value
                    val s2 = latest?.state
                    if (latest == null || s2 == Call.STATE_DISCONNECTED) {
                        if (!isFinishing) {
                            // Bring our own dialer to front so the system phone app
                            // doesn't surface after the call ends.
                            startActivity(
                                Intent(this@InCallActivity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                }
                            )
                            finish()
                        }
                    }
                }
            }
        }

        setContent {
            DialerTheme {
                InCallScreen(
                    vm = vm,
                    onQuickReply = { message ->
                        pendingQuickReply = message
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            sendQuickReply(message)
                        } else {
                            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                        }
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (CallStateHolder.info.value?.state == Call.STATE_RINGING) {
            MyInCallService.instance?.setIncomingCallUiForeground(true)
        }
    }

    // Register hardware toolbar button callbacks while this activity is foreground.
    // This ensures End/Call keys work even if MainActivity was never started or was destroyed.
    override fun onResume() {
        super.onResume()
        com.zeno.dialer.service.ToolbarButtonHandler.onCallPressed = {
            val s = CallStateHolder.info.value?.state
            if (s == Call.STATE_RINGING) CallStateHolder.answer()
        }
        com.zeno.dialer.service.ToolbarButtonHandler.onEndPressed = {
            val s = CallStateHolder.info.value?.state
            if (s == Call.STATE_RINGING) CallStateHolder.reject() else CallStateHolder.hangup()
        }
    }

    override fun onStop() {
        if (CallStateHolder.info.value?.state == Call.STATE_RINGING) {
            MyInCallService.instance?.setIncomingCallUiForeground(false)
        }
        super.onStop()
        // Clear so MainActivity can re-register its own callbacks when it resumes.
        com.zeno.dialer.service.ToolbarButtonHandler.onCallPressed = null
        com.zeno.dialer.service.ToolbarButtonHandler.onEndPressed = null
    }

    override fun onDestroy() {
        CallRecorder.stop(this)
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val state = CallStateHolder.info.value?.state
            when (event.keyCode) {
                KeyEvent.KEYCODE_CALL,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    if (state == Call.STATE_RINGING) CallStateHolder.answer()
                    return true
                }
                KeyEvent.KEYCODE_ENDCALL -> {
                    // BB toolbar red button — always end/reject.
                    if (state == Call.STATE_RINGING) CallStateHolder.reject()
                    else CallStateHolder.hangup()
                    return true
                }
                // KEYCODE_BACK is intentionally NOT handled here.
                // It should navigate back (Back key ≠ End call).
            }
            // IVR DTMF: dedicated keys + BB QWERTY mapping (same as dialer keypad).
            if (state == Call.STATE_ACTIVE) {
                val ch = PhoneKeyMapping.keyCodeToDtmfChar(event.keyCode)
                    ?: event.unicodeChar.takeIf { it > 0 }?.toChar()
                        ?.takeIf { it.isDigit() || it == '*' || it == '#' }
                    ?: run {
                        val kcm = event.device?.keyCharacterMap
                            ?: KeyCharacterMap.load(event.deviceId)
                        kcm.getNumber(event.keyCode).takeIf { it.code > 0 }
                    }
                if (ch != null && CallStateHolder.tryPlayDtmfOnPrimaryCall(ch)) {
                    return true
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            // Best-effort: stop a possibly "stuck" DTMF tone as soon as the key is released.
            val state = CallStateHolder.info.value?.state
            if (state == Call.STATE_ACTIVE) {
                val maybeCh = PhoneKeyMapping.keyCodeToDtmfChar(event.keyCode)
                    ?: event.unicodeChar.takeIf { it > 0 }?.toChar()
                        ?.takeIf { it.isDigit() || it == '*' || it == '#' }
                if (maybeCh != null) {
                    CallStateHolder.stopDtmfOnPrimaryCall()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun sendQuickReply(message: String) {
        val number = CallStateHolder.info.value?.number.orEmpty()
        if (number.isBlank()) return
        try {
            SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
            CallStateHolder.reject()
            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
        }
    }
}
