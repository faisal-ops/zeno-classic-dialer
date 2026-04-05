package com.zeno.dialer.service

/**
 * Singleton bridge between [ButtonInterceptService] (AccessibilityService)
 * and the main app. The service invokes these callbacks when the BB toolbar
 * Call / End buttons are pressed.
 */
object ToolbarButtonHandler {
    var onCallPressed: (() -> Unit)? = null
    var onEndPressed: (() -> Unit)? = null
}
