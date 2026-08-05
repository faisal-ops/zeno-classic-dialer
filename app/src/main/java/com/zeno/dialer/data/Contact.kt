package com.zeno.dialer.data

import androidx.compose.runtime.Immutable

@Immutable
data class Contact(
    val id: Long = 0L,
    val name: String,
    val number: String,
    val isRecent: Boolean = false,
    val lastCallTime: Long = 0L,
    val callType: Int = 0,
    val photoUri: String? = null,
    /** ContactsContract.CommonDataKinds.Phone.TYPE for this number, when loaded from contacts. */
    val numberType: Int? = null,
    /** Custom label text when [numberType] == Phone.TYPE_CUSTOM. */
    val numberTypeLabel: String? = null,
    /** CallLog.Calls.IS_READ for this entry (only meaningful when [isRecent] is true). Defaults
     * to true so non-call-log-derived contacts never spuriously look "unread". */
    val isCallLogRead: Boolean = true
)

enum class FilterMode { ALL, MISSED, RECEIVED, VOICEMAIL, CONTACTS, RECENTS }
