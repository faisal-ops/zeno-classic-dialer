package com.zeno.dialer.service

import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import com.zeno.dialer.AppPreferences
import com.zeno.dialer.data.BlockedNumbersRepo

private const val KEY_BLOCK_UNKNOWN = "block_unknown_callers"

/**
 * Screens incoming calls before they ring.
 *
 * As the default dialer on Android 14, this service is invoked automatically
 * for every incoming call without requiring extra user permission.
 *
 * Behaviour:
 *  - Known contacts         → always allow
 *  - Unknown numbers        → block (reject + silence) when user enabled "Block unknown callers";
 *                             allow normally otherwise
 */
class ScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number  = callDetails.handle?.schemeSpecificPart.orEmpty()
        val isKnown = number.isNotEmpty() && isInContacts(number)
        val isExplicitlyBlocked = isInBlockedList(number)

        val blockUnknown = getSharedPreferences(AppPreferences.FILE_SETTINGS, MODE_PRIVATE)
            .getBoolean(KEY_BLOCK_UNKNOWN, false)

        val shouldBlock = isExplicitlyBlocked || (blockUnknown && !isKnown)

        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(shouldBlock)
                .setRejectCall(shouldBlock)
                .setSilenceCall(shouldBlock)
                .setSkipCallLog(false)        // always log — even blocked calls
                .setSkipNotification(false)
                .build()
        )
    }

    // ── Contact lookup ────────────────────────────────────────────────────────

    private fun isInContacts(number: String): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        return try {
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun isInBlockedList(number: String): Boolean = BlockedNumbersRepo(this).contains(number)
}
