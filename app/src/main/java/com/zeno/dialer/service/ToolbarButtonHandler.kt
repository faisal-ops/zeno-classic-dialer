package com.zeno.dialer.service

/**
 * Singleton bridge between [ButtonInterceptService] (AccessibilityService)
 * and the main app. The service invokes these callbacks when the BB toolbar
 * Call / End buttons are pressed.
 */
object ToolbarButtonHandler {
    var onCallPressed: (() -> Unit)? = null
    var onEndPressed: (() -> Unit)? = null

    /**
     * Clears the callbacks only if they still reference [callHandler]/[endHandler] — i.e. only
     * if nobody else has already taken over the registration.
     *
     * Both MainActivity and InCallActivity register/clear these on their own lifecycle
     * (onResume/onStop), and Android's activity-transition order for "bring activity B to front
     * over activity A" is: A.onPause() -> B.onResume() -> A.onStop(). So when B re-registers its
     * own callback in onResume() and A then unconditionally nulls the (shared) callback in its
     * own onStop() right after, A wipes out B's fresh registration even though B is now the
     * foreground activity. Comparing by reference before clearing avoids that race.
     */
    fun clearIfOwnedBy(callHandler: (() -> Unit)?, endHandler: (() -> Unit)?) {
        if (onCallPressed === callHandler) onCallPressed = null
        if (onEndPressed === endHandler) onEndPressed = null
    }
}
