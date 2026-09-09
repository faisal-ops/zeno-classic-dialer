package com.zeno.dialer.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.zeno.dialer.AppPreferences
import com.zeno.dialer.MainActivity

/**
 * AccessibilityService that intercepts the BB toolbar Call and End hardware
 * buttons. These keys (KEYCODE_CALL / KEYCODE_ENDCALL) are normally consumed
 * by the system before reaching the activity — this service catches them first.
 *
 * The user must enable this service in Settings → Accessibility.
 */
class ButtonInterceptService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            Log.i("ButtonInterceptService", "KEY down code=${event.keyCode} repeat=${event.repeatCount}")
            when (event.keyCode) {
                KeyEvent.KEYCODE_CALL -> {
                    Log.i("ButtonInterceptService", "KEYCODE_CALL intercepted")
                    val callback = ToolbarButtonHandler.onCallPressed
                    if (callback != null) {
                        Log.i("ButtonInterceptService", "KEYCODE_CALL -> invoking callback")
                        callback.invoke()
                    } else {
                        Log.i("ButtonInterceptService", "KEYCODE_CALL -> callback null, opening dialer")
                        openDefaultDialer()
                    }
                    return true
                }
                // KEYCODE_HEADSETHOOK intentionally NOT intercepted here.
                // Headset buttons (wired/BT) send this key for play/pause/answer — consuming
                // it here would trigger an outgoing call whenever the user presses a headset
                // button while the dialer is in the foreground.
                KeyEvent.KEYCODE_VOLUME_MUTE -> {
                    // Q25's side mute key — confirmed via logcat (KEY down code=164) that this
                    // device's keylayout maps it to KEYCODE_VOLUME_MUTE (call-output-volume mute,
                    // handled by MediaSessionService), not KEYCODE_MUTE (mic mute) as the SDK name
                    // would suggest for a "mute button". Repurposed for call-mute only while a
                    // call is active, so it behaves as an ordinary volume-mute key otherwise.
                    if (com.zeno.dialer.CallStateHolder.info.value?.state == android.telecom.Call.STATE_ACTIVE) {
                        ToolbarButtonHandler.onMutePressed?.invoke()
                        return true
                    }
                }
                KeyEvent.KEYCODE_GRAVE -> {
                    // Q25's "currency/speaker" key is a regular keyboard-matrix key that happens
                    // to send the standard GRAVE code — only hijack it for speaker-toggle while a
                    // call is active, so it still types a backtick everywhere else (this service
                    // intercepts system-wide, not just within our own app).
                    if (com.zeno.dialer.CallStateHolder.info.value?.state == android.telecom.Call.STATE_ACTIVE) {
                        ToolbarButtonHandler.onSpeakerPressed?.invoke()
                        return true
                    }
                }
                KeyEvent.KEYCODE_ENDCALL -> {
                    val callback = ToolbarButtonHandler.onEndPressed
                    if (callback != null) {
                        // A callback is registered — either InCallActivity or MainActivity is in foreground
                        callback.invoke()
                    } else {
                        // No callback — app is in background. Only end call if setting allows it.
                        val prefs = getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE)
                        if (prefs.getBoolean(AppPreferences.KEY_END_CALL_ANYWHERE, false)) {
                            com.zeno.dialer.CallStateHolder.hangup()
                        }
                    }
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — we only need key event interception
    }

    override fun onInterrupt() {}

    private fun openDefaultDialer() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                putExtra(EXTRA_CALL_BUTTON_PRESSED, true)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
        )
    }

    companion object {
        const val EXTRA_CALL_BUTTON_PRESSED = "call_button_pressed"
    }
}
