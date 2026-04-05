package com.zeno.dialer

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
    /** Display options theme: 0=system default, 1=light, 2=dark — must match [DialerTheme] listener. */
    const val KEY_CHOOSE_THEME = "choose_theme"
    const val KEY_SORT_BY = "sort_by"
    const val KEY_NAME_FORMAT = "name_format"
    const val KEY_QUICK_RESPONSE_PREFIX = "quick_response_"
    const val KEY_ASSISTED_DIALING = "assisted_dialing"
    const val KEY_CALLER_SPAM_ID = "see_caller_spam_id"
    const val KEY_FILTER_SPAM = "filter_spam_calls"
    const val KEY_END_CALL_ANYWHERE = "end_call_from_any_app"
}
