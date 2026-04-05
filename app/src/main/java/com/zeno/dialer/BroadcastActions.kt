package com.zeno.dialer

/**
 * Custom broadcast actions for in-call notification buttons. Must match
 * [AndroidManifest.xml] intent-filters. Prefix is fixed so debug `.debug` [applicationId]
 * still uses the same actions; delivery is scoped with [Intent.setPackage].
 */
object BroadcastActions {
    const val HANGUP = "com.zeno.zenoclassicdialer.action.HANGUP"
    const val ANSWER = "com.zeno.zenoclassicdialer.action.ANSWER"
    const val DECLINE = "com.zeno.zenoclassicdialer.action.DECLINE"
}
