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
    val photoUri: String? = null
)

enum class FilterMode { ALL, MISSED, RECEIVED, CONTACTS, RECENTS }
