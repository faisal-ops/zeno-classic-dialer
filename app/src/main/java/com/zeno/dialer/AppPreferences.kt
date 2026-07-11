package com.zeno.dialer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo

/**
 * Central [SharedPreferences] file names and keys so settings stay consistent across
 * activities, repos, and services.
 */
object AppPreferences {
    const val FILE_SETTINGS = "zeno_settings"
    /** First-run / setup flag (MainActivity). */
    const val FILE_ZENO = "zeno"

    const val KEY_SETUP_COMPLETE = "setup_complete"
    const val KEY_ACCESSIBILITY_PROMPTED = "accessibility_prompted"

    const val KEY_FLIP_TO_SILENCE = "flip_to_silence"
    const val KEY_CALLER_ID_ANNOUNCE = "caller_id_announce"
    const val KEY_VISUAL_VOICEMAIL = "visual_voicemail"
    const val KEY_PORTRAIT_MODE = "keep_portrait_mode"
    /** Legacy display theme key; now mirrors dialer style choice (0=Original Classic, 1=Modern Classic). */
    const val KEY_CHOOSE_THEME = "choose_theme"
    const val KEY_SORT_BY = "sort_by"
    const val KEY_NAME_FORMAT = "name_format"
    const val KEY_QUICK_RESPONSE_PREFIX = "quick_response_"
    const val KEY_QUICK_RESPONSE_INSTANT_SEND = "quick_response_instant_send"
    const val KEY_ASSISTED_DIALING = "assisted_dialing"
    const val KEY_CALLER_SPAM_ID = "see_caller_spam_id"
    const val KEY_FILTER_SPAM = "filter_spam_calls"
    const val KEY_END_CALL_ANYWHERE = "end_call_from_any_app"
    /**
     * Gate for [com.zeno.dialer.service.ExternalCallKeyReceiver]. That receiver is exported
     * so the Q25 keyboard-case companion app can forward hardware Call/End key presses via
     * broadcast — but an exported, unprotected receiver means *any* installed app could send
     * the same broadcast to answer/end/dial calls. Defaults to true because this hardware is
     * this app's primary target device; users on hardware without this bridge (where the
     * exported-broadcast surface is pure downside) can turn it off in Settings.
     */
    const val KEY_EXTERNAL_KEYBOARD_BRIDGE_ENABLED = "external_keyboard_bridge_enabled"
    /** Dialer style: 0=Original Classic (v1.0.0), 1=Modern Classic, 2=Pixel. */
    const val KEY_DIALER_STYLE = "dialer_style"
    const val DIALER_STYLE_ORIGINAL_CLASSIC = 0
    const val DIALER_STYLE_MODERN_CLASSIC = 1
    const val DIALER_STYLE_PIXEL = 2
}

/**
 * Applies the "Keep portrait mode" Settings toggle (Display Options) to this activity.
 * Call from `onCreate` and `onResume` — activities no longer hardcode
 * `android:screenOrientation="portrait"` in the manifest, so this is the only thing enforcing
 * portrait lock, and must reflect changes made in Settings while this activity was backgrounded.
 */
fun Activity.applyPortraitModePreference() {
    val keepPortrait = getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE)
        .getBoolean(AppPreferences.KEY_PORTRAIT_MODE, true)
    requestedOrientation = if (keepPortrait) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
