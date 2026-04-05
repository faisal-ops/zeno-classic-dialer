package com.zeno.dialer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationIdSmokeTest {

    @Test
    fun targetContext_packageName_matchesZenoClassicDialerId() {
        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        assertTrue(
            "Expected com.zeno.zenoclassicdialer or .debug suffix, was $pkg",
            pkg == "com.zeno.zenoclassicdialer" || pkg == "com.zeno.zenoclassicdialer.debug"
        )
    }
}
