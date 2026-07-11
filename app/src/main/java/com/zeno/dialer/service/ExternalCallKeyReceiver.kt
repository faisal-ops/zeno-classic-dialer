package com.zeno.dialer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zeno.dialer.AppPreferences
import com.zeno.dialer.CallStateHolder
import com.zeno.dialer.MainActivity

/**
 * Bridge receiver for the Q25 hardware-keyboard case's companion app, which forwards
 * physical Call/End key presses as broadcasts rather than standard [android.view.KeyEvent]s
 * (unlike [ButtonInterceptService], which intercepts standard KEYCODE_CALL/KEYCODE_ENDCALL
 * events on devices that deliver them normally).
 *
 * This receiver is exported (any app can broadcast [ACTION_CALL_KEY]/[ACTION_END_KEY]), so it's
 * gated behind [AppPreferences.KEY_EXTERNAL_KEYBOARD_BRIDGE_ENABLED] — otherwise an unrelated
 * app could silently answer, hang up, or trigger a dial by sending the same broadcast. Defaults
 * to enabled since this hardware is this app's primary target device; users on hardware without
 * this bridge can turn it off in Settings.
 */
class ExternalCallKeyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(AppPreferences.KEY_EXTERNAL_KEYBOARD_BRIDGE_ENABLED, true)) return

        when (intent.action) {
            ACTION_CALL_KEY -> {
                val callHandler = ToolbarButtonHandler.onCallPressed
                if (callHandler != null) {
                    callHandler.invoke()
                } else {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_MAIN
                            putExtra(ButtonInterceptService.EXTRA_CALL_BUTTON_PRESSED, true)
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            )
                        }
                    )
                }
            }
            ACTION_END_KEY -> {
                val endHandler = ToolbarButtonHandler.onEndPressed
                if (endHandler != null) {
                    endHandler.invoke()
                } else if (prefs.getBoolean(AppPreferences.KEY_END_CALL_ANYWHERE, false)) {
                    CallStateHolder.hangup()
                }
            }
        }
    }

    companion object {
        const val ACTION_CALL_KEY = "com.duc1607.q25keyboard.ACTION_CALL_KEY"
        const val ACTION_END_KEY = "com.duc1607.q25keyboard.ACTION_END_KEY"
    }
}
