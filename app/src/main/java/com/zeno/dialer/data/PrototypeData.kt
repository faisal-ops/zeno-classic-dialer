package com.zeno.dialer.data

import android.provider.CallLog

internal object PrototypeData {
    val contacts: List<Contact> = listOf(
        Contact(name = "Alice Johnson", number = "+1 (555) 234-5678"),
        Contact(name = "Bob Martinez", number = "+1 (555) 345-6789"),
        Contact(name = "Carol Chen", number = "+1 (555) 456-7890"),
        Contact(name = "David Williams", number = "+1 (555) 567-8901"),
        Contact(name = "Eve Thompson", number = "+1 (555) 678-9012"),
        Contact(name = "Frank Adams", number = "+1 (555) 789-0123"),
        Contact(name = "Grace Lee", number = "+1 (555) 890-1234"),
        Contact(name = "Henry Wilson", number = "+1 (555) 901-2345"),
        Contact(name = "Iris Brown", number = "+1 (555) 012-3456"),
        Contact(name = "James Davis", number = "+1 (555) 123-4567"),
        Contact(name = "Karen Taylor", number = "+1 (555) 234-5670"),
        Contact(name = "Liam Anderson", number = "+1 (555) 345-6780"),
        Contact(name = "Mia Jackson", number = "+1 (555) 456-7891"),
        Contact(name = "Noah Harris", number = "+1 (555) 567-8902"),
        Contact(name = "Olivia Martin", number = "+1 (555) 678-9013"),
        Contact(name = "Paul Garcia", number = "+1 (555) 789-0124"),
        Contact(name = "Quinn Robinson", number = "+1 (555) 890-1235"),
        Contact(name = "Rachel Clark", number = "+1 (555) 901-2346"),
        Contact(name = "Samuel Lewis", number = "+1 (555) 012-3457"),
        Contact(name = "Tina Walker", number = "+1 (555) 123-4560"),
    )

    val recents: List<Contact> by lazy {
        val now = System.currentTimeMillis()
        listOf(
            recent(contacts[0], now - 11 * 60_000L, CallLog.Calls.MISSED_TYPE),
            recent(contacts[1], now - 37 * 60_000L, CallLog.Calls.INCOMING_TYPE),
            recent(contacts[2], now - 84 * 60_000L, CallLog.Calls.OUTGOING_TYPE),
            recent(contacts[3], now - 126 * 60_000L, CallLog.Calls.MISSED_TYPE),
            recent(contacts[4], now - 180 * 60_000L, CallLog.Calls.OUTGOING_TYPE),
            recent(contacts[5], now - 270 * 60_000L, CallLog.Calls.INCOMING_TYPE),
            recent(contacts[6], now - 25 * 3_600_000L, CallLog.Calls.MISSED_TYPE),
            recent(contacts[7], now - 27 * 3_600_000L, CallLog.Calls.OUTGOING_TYPE),
            recent(contacts[8], now - 30 * 3_600_000L, CallLog.Calls.INCOMING_TYPE),
            recent(contacts[9], now - 35 * 3_600_000L, CallLog.Calls.MISSED_TYPE),
            recent(contacts[10], now - 50 * 3_600_000L, CallLog.Calls.INCOMING_TYPE),
            recent(contacts[11], now - 72 * 3_600_000L, CallLog.Calls.OUTGOING_TYPE),
            recent(contacts[12], now - 96 * 3_600_000L, CallLog.Calls.MISSED_TYPE),
        )
    }

    private fun recent(base: Contact, ts: Long, callType: Int): Contact = base.copy(
        isRecent = true,
        lastCallTime = ts,
        callType = callType
    )
}

