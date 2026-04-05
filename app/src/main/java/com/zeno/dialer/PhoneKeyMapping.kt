package com.zeno.dialer

import android.view.KeyEvent

/**
 * Shared mapping from hardware key codes to phone / DTMF characters (BB Classic QWERTY + dedicated keys).
 * Used by [KeyHandler] and [InCallActivity] for consistent IVR digit entry.
 */
object PhoneKeyMapping {

    fun keyCodeToPhoneChar(keyCode: Int): Char? = when (keyCode) {
        KeyEvent.KEYCODE_0     -> '0'
        KeyEvent.KEYCODE_1     -> '1'
        KeyEvent.KEYCODE_2     -> '2'
        KeyEvent.KEYCODE_3     -> '3'
        KeyEvent.KEYCODE_4     -> '4'
        KeyEvent.KEYCODE_5     -> '5'
        KeyEvent.KEYCODE_6     -> '6'
        KeyEvent.KEYCODE_7     -> '7'
        KeyEvent.KEYCODE_8     -> '8'
        KeyEvent.KEYCODE_9     -> '9'
        KeyEvent.KEYCODE_STAR  -> '*'
        KeyEvent.KEYCODE_POUND -> '#'
        KeyEvent.KEYCODE_PLUS  -> '+'
        else                   -> null
    }

    /**
     * BB Classic QWERTY keyboard layout:
     *   #=Q  1=W  2=E  3=R
     *   *=A  4=S  5=D  6=F
     *        7=Z  8=X  9=C
     */
    fun qwertyKeyCodeToDigit(keyCode: Int): Char? = when (keyCode) {
        KeyEvent.KEYCODE_Q -> '#'
        KeyEvent.KEYCODE_A -> '*'
        KeyEvent.KEYCODE_W -> '1'
        KeyEvent.KEYCODE_E -> '2'
        KeyEvent.KEYCODE_R -> '3'
        KeyEvent.KEYCODE_S -> '4'
        KeyEvent.KEYCODE_D -> '5'
        KeyEvent.KEYCODE_F -> '6'
        KeyEvent.KEYCODE_Z -> '7'
        KeyEvent.KEYCODE_X -> '8'
        KeyEvent.KEYCODE_C -> '9'
        else               -> null
    }

    fun keyCodeToDtmfChar(keyCode: Int): Char? =
        keyCodeToPhoneChar(keyCode) ?: qwertyKeyCodeToDigit(keyCode)
}
