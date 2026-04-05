package com.zeno.dialer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zeno.dialer.BroadcastActions
import com.zeno.dialer.CallStateHolder

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BroadcastActions.HANGUP -> CallStateHolder.hangup()
            BroadcastActions.ANSWER -> CallStateHolder.answer()
            BroadcastActions.DECLINE -> CallStateHolder.reject()
        }
    }
}
