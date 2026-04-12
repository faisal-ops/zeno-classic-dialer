package com.zeno.dialer

import android.view.KeyEvent
import android.util.Log
import android.os.SystemClock
import com.zeno.dialer.data.Contact
import com.zeno.dialer.FavoritesScrollController

class KeyHandler(
    private val viewModel: DialerViewModel,
    private val onFinish: () -> Unit,
    private val onDial: (String) -> Unit,
    private val onOpenContact: (Contact) -> Unit = {},
    private val onOpenSettings: () -> Unit,
    private val onDebugTrace: (String) -> Unit = {},
) {
    private var lastDpadNavAtMs: Long = 0L
    private val dpadNavMinIntervalMs = 80L

    /**
     * Fast path for the Keypad tab: called with the unicode char from a
     * synthesized Alt-KeyEvent. If the device's key map produced a digit,
     * type it immediately. Navigation/control keys are forwarded to [handle].
     */
    fun handleKeypad(keyCode: Int, nativeAltChar: Int): Boolean {
        // Let control keys fall through to the normal handler
        if (isControlKey(keyCode)) return false

        val ch = PhoneKeyMapping.keyCodeToPhoneChar(keyCode)
            ?: nativeAltChar.takeIf { it > 0 }?.toChar()
                ?.takeIf { it.isDigit() || it == '*' || it == '#' || it == '+' }
            ?: PhoneKeyMapping.qwertyKeyCodeToDigit(keyCode)

        if (ch != null) { viewModel.typeUnicode(ch.code); return true }
        return false
    }

    private fun isControlKey(keyCode: Int): Boolean = keyCode in setOf(
        KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL, KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_TAB
    )

    private fun activateFavoriteTile(state: DialerUiState): Boolean {
        if (state.favoriteFocusIndex < 0) return false
        if (state.favoriteFocusIndex == 0) {
            onDebugTrace("Tile action: open Settings")
            onOpenSettings()
            return true
        }
        val number = state.pinnedFavorites
            .getOrNull(state.favoriteFocusIndex - 1)
            ?.number
        if (number.isNullOrBlank()) {
            onDebugTrace("Tile action failed: missing favorite number")
            return true
        }
        onDebugTrace("Tile action: dial $number")
        onDial(number)
        return true
    }

    private fun allowDpadNavStep(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if ((now - lastDpadNavAtMs) < dpadNavMinIntervalMs) return false
        lastDpadNavAtMs = now
        return true
    }

    fun handle(keyCode: Int, unicodeChar: Int, altUnicodeChar: Int = 0): Boolean = when (keyCode) {

        KeyEvent.KEYCODE_CALL -> {
            val state = viewModel.uiState.value
            if (activateFavoriteTile(state)) {
                onDebugTrace("CALL key handled by favorite tile")
            } else {
                // Resolve number directly via onDial — bypasses SharedFlow.
                val number = when {
                    state.query.isNotBlank() -> state.query
                    state.selectedIndex >= 0 &&
                        state.selectedIndex < state.results.size ->
                        state.results[state.selectedIndex].number
                    state.scrollFocusedIndex >= 0 &&
                        state.scrollFocusedIndex < state.results.size ->
                        state.results[state.scrollFocusedIndex].number
                    else -> null
                }
                if (number != null) {
                    onDebugTrace("CALL key dialing $number")
                    onDial(number)
                } else {
                    onDebugTrace("CALL key: no number, switching to calls tab")
                    viewModel.setCurrentTab(0)
                }
            }
            true
        }

        KeyEvent.KEYCODE_ENDCALL -> {
            onFinish()
            true
        }

        KeyEvent.KEYCODE_MENU -> true

        KeyEvent.KEYCODE_BACK -> {
            if (viewModel.uiState.value.query.isNotEmpty()) {
                viewModel.clearQuery()
            } else {
                onFinish()
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_UP -> {
            if (allowDpadNavStep()) {
                val state = viewModel.uiState.value
                when {
                    state.favoriteFocusIndex >= 0 -> { /* already at top row, nowhere to go */ }
                    // Contacts tab: at first *visual* row, UP scrolls list to top (not results[0]).
                    viewModel.currentTabIndex == 1 && state.results.isNotEmpty() && viewModel.isContactsNavAtStart(state) ->
                        viewModel.scrollContactsToTop()
                    viewModel.currentTabIndex == 0 && state.selectedIndex <= 0 ->
                        viewModel.setFavoriteFocus(0)
                    else -> viewModel.nudgeSelectionUp()
                }
            }
            true
        }
        KeyEvent.KEYCODE_DPAD_DOWN -> {
            if (allowDpadNavStep()) {
                val state = viewModel.uiState.value
                if (state.favoriteFocusIndex >= 0) {
                    viewModel.setFavoriteFocus(-1)
                    viewModel.nudgeSelectionDown()
                } else {
                    viewModel.nudgeSelectionDown()
                }
            }
            true
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> {
            val state = viewModel.uiState.value
            if (state.favoriteFocusIndex >= 0) {
                viewModel.setFavoriteFocus((state.favoriteFocusIndex - 1).coerceAtLeast(0))
            } else {
                viewModel.nudgeCursorLeft()
            }
            true
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            val state = viewModel.uiState.value
            if (state.favoriteFocusIndex >= 0) {
                val max = state.pinnedFavorites.size // 0=Settings, 1..size=contacts
                viewModel.setFavoriteFocus((state.favoriteFocusIndex + 1).coerceAtMost(max))
            } else {
                viewModel.nudgeCursorRight()
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER -> {
            val state = viewModel.uiState.value
            when {
                // Contacts tab: open contact details in the Contacts app.
                viewModel.currentTabIndex == 1 &&
                    state.selectedIndex >= 0 &&
                    state.selectedIndex < state.results.size -> {
                    val contact = state.results[state.selectedIndex]
                    onDebugTrace("Open contact: ${contact.name}")
                    onOpenContact(contact)
                }
                activateFavoriteTile(state) -> {
                    onDebugTrace("CENTER/ENTER handled by favorite tile")
                }
                // Calls tab with a selected row and no query: expand inline history (don't dial).
                viewModel.currentTabIndex == 0
                    && state.query.isBlank()
                    && state.selectedIndex >= 0 -> viewModel.toggleExpandSelected()
                // Keypad tab with no digits entered: toggle expand.
                viewModel.currentTabIndex == 2 && state.query.isBlank() -> viewModel.toggleExpandSelected()
                else -> {
                    Log.i(
                        "ZenoDialer",
                        "KEYCODE_${if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) "DPAD_CENTER" else "ENTER"} tab=${viewModel.currentTabIndex} selectedIndex=${state.selectedIndex} query='${state.query}' resultsSize=${state.results.size}"
                    )
                    viewModel.callSelected()
                }
            }
            true
        }

        KeyEvent.KEYCODE_DEL    -> { viewModel.deleteChar(); true }
        KeyEvent.KEYCODE_ESCAPE -> { viewModel.clearQuery(); true }
        KeyEvent.KEYCODE_TAB    -> { viewModel.cycleFilter(); true }

        KeyEvent.KEYCODE_C -> {
            if (viewModel.isOnKeypad) {
                typeDigitOnKeypad(keyCode, unicodeChar, altUnicodeChar)
            } else if (viewModel.uiState.value.query.isEmpty()) {
                if (viewModel.currentTabIndex == 0) viewModel.setCurrentTab(1)
                viewModel.setFilterContacts(); true
            } else {
                if (viewModel.currentTabIndex == 0) viewModel.setCurrentTab(1)
                viewModel.typeUnicode(unicodeChar)
            }
        }
        KeyEvent.KEYCODE_R -> {
            if (viewModel.isOnKeypad) {
                typeDigitOnKeypad(keyCode, unicodeChar, altUnicodeChar)
            } else if (viewModel.uiState.value.query.isEmpty()) {
                if (viewModel.currentTabIndex == 0) viewModel.setCurrentTab(1)
                viewModel.setFilterRecents(); true
            } else {
                if (viewModel.currentTabIndex == 0) viewModel.setCurrentTab(1)
                viewModel.typeUnicode(unicodeChar)
            }
        }

        else -> {
            if (viewModel.isOnKeypad) {
                typeDigitOnKeypad(keyCode, unicodeChar, altUnicodeChar)
            } else {
                // If on Favorites tab, switch to Home before typing
                if (viewModel.currentTabIndex == 0 && unicodeChar > 0) {
                    viewModel.setCurrentTab(1)
                }
                viewModel.typeUnicode(unicodeChar)
            }
        }
    }

    /**
     * Resolves a key press to a phone digit on the Keypad tab.
     * Priority:
     *   1. Dedicated digit keycode (KEYCODE_0–9, STAR, POUND, PLUS)
     *   2. The unicode char itself if already a digit / symbol
     *   3. The Alt-layer unicode char if it's a digit / symbol
     *   4. BB QWERTY row mapping (Q=1, W=2, E=3, … P=0)
     */
    private fun typeDigitOnKeypad(keyCode: Int, unicodeChar: Int, altUnicodeChar: Int): Boolean {
        val ch = PhoneKeyMapping.keyCodeToPhoneChar(keyCode)
            ?: unicodeChar.takeIf { it > 0 }?.toChar()
                ?.takeIf { it.isDigit() || it == '*' || it == '#' || it == '+' }
            ?: altUnicodeChar.takeIf { it > 0 }?.toChar()
                ?.takeIf { it.isDigit() || it == '*' || it == '#' || it == '+' }
            ?: PhoneKeyMapping.qwertyKeyCodeToDigit(keyCode)
        if (ch != null) { viewModel.typeUnicode(ch.code); return true }
        return false
    }
}
