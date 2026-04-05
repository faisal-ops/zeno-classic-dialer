package com.zeno.dialer.service

import org.junit.Assert.assertEquals
import org.junit.Test

class CallRecorderTest {

    @Test
    fun sanitizeFileStem_stripsUnsafeChars() {
        assertEquals("John Doe", CallRecorder.sanitizeFileStem("John Doe"))
        assertEquals("Tarannum", CallRecorder.sanitizeFileStem("Tarannum"))
        assertEquals("ab", CallRecorder.sanitizeFileStem("a@#b"))
        assertEquals("", CallRecorder.sanitizeFileStem("@#$"))
    }
}
