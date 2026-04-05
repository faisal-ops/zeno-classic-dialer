package com.zeno.dialer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zeno.dialer.AppPreferences
import com.zeno.dialer.CallStateHolder
import com.zeno.dialer.MainActivity

/**
 * Optional bridge receiver for external apps (e.g. keyboard IME) to forward
 * call/end hardware-key intents to Zeno without requiring a direct API.
 */
class ExternalCallKeyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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
                } else {
                    val prefs = context.getSharedPreferences(
                        AppPreferences.FILE_SETTINGS,
                        Context.MODE_PRIVATE
                    )
                    if (prefs.getBoolean(AppPreferences.KEY_END_CALL_ANYWHERE, false)) {
                        CallStateHolder.hangup()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_CALL_KEY = "com.duc1607.q25keyboard.ACTION_CALL_KEY"
        const val ACTION_END_KEY = "com.duc1607.q25keyboard.ACTION_END_KEY"
    }
}
