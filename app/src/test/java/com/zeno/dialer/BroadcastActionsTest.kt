package com.zeno.dialer

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards notification / PendingIntent wiring: actions must stay aligned with [AndroidManifest.xml]
 * receiver intent-filters and [CallActionReceiver].
 */
class BroadcastActionsTest {

    @Test
    fun action_constants_useReleaseApplicationIdPrefix() {
        val prefix = "com.zeno.zenoclassicdialer.action."
        assertTrue(BroadcastActions.HANGUP.startsWith(prefix))
        assertTrue(BroadcastActions.ANSWER.startsWith(prefix))
        assertTrue(BroadcastActions.DECLINE.startsWith(prefix))
    }
}
