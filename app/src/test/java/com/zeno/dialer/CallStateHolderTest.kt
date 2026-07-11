package com.zeno.dialer

import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// CallStateHolder eagerly creates a Handler(Looper.getMainLooper()) at object-init time,
// which needs Robolectric's Android framework shadows rather than the plain unit-test stub jar.
@RunWith(RobolectricTestRunner::class)
class CallStateHolderTest {

    @Test
    fun clear_resetsPrimaryAndSecondaryFlows() {
        CallStateHolder.clear()
        assertNull(CallStateHolder.info.value)
        assertNull(CallStateHolder.secondCall.value)
    }
}
