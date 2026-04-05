package com.zeno.dialer

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneKeyMappingTest {

    @Test
    fun keyCodeToPhoneChar_mapsDigitsStarPoundPlus() {
        assertEquals('0', PhoneKeyMapping.keyCodeToPhoneChar(KeyEvent.KEYCODE_0))
        assertEquals('9', PhoneKeyMapping.keyCodeToPhoneChar(KeyEvent.KEYCODE_9))
        assertEquals('*', PhoneKeyMapping.keyCodeToPhoneChar(KeyEvent.KEYCODE_STAR))
        assertEquals('#', PhoneKeyMapping.keyCodeToPhoneChar(KeyEvent.KEYCODE_POUND))
        assertEquals('+', PhoneKeyMapping.keyCodeToPhoneChar(KeyEvent.KEYCODE_PLUS))
    }

    @Test
    fun keyCodeToPhoneChar_unknown_returnsNull() {
        assertNull(PhoneKeyMapping.keyCodeToPhoneChar(KeyEvent.KEYCODE_A))
    }

    @Test
    fun qwertyKeyCodeToDigit_matchesBbLayout() {
        assertEquals('#', PhoneKeyMapping.qwertyKeyCodeToDigit(KeyEvent.KEYCODE_Q))
        assertEquals('*', PhoneKeyMapping.qwertyKeyCodeToDigit(KeyEvent.KEYCODE_A))
        assertEquals('1', PhoneKeyMapping.qwertyKeyCodeToDigit(KeyEvent.KEYCODE_W))
        assertEquals('9', PhoneKeyMapping.qwertyKeyCodeToDigit(KeyEvent.KEYCODE_C))
    }

    @Test
    fun keyCodeToDtmfChar_prefersPhoneCharThenQwerty() {
        assertEquals('5', PhoneKeyMapping.keyCodeToDtmfChar(KeyEvent.KEYCODE_5))
        assertEquals('9', PhoneKeyMapping.keyCodeToDtmfChar(KeyEvent.KEYCODE_C))
    }
}
