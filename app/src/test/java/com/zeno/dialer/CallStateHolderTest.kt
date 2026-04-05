package com.zeno.dialer

import org.junit.Assert.assertNull
import org.junit.Test

class CallStateHolderTest {

    @Test
    fun clear_resetsPrimaryAndSecondaryFlows() {
        CallStateHolder.clear()
        assertNull(CallStateHolder.info.value)
        assertNull(CallStateHolder.secondCall.value)
    }
}
