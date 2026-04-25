package com.zeno.dialer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton bridge between [com.zeno.dialer.service.MyInCallService]
 * (which owns the live [Call] objects from Telecom) and [InCallActivity].
 *
 * Supports up to two simultaneous calls:
 *   - [info]       : foreground (active / ringing) call — drives the primary UI.
 *   - [secondCall] : background / held call — shown as a compact banner.
 *
 * Thread-safe: MutableStateFlow updates are always safe from any thread.
 */
data class ActiveCallInfo(
    val call: Call,
    val state: Int,
    val displayName: String,
    val number: String,
    val photoUri: String? = null
)

object CallStateHolder {

    private val _info       = MutableStateFlow<ActiveCallInfo?>(null)
    private val _secondCall = MutableStateFlow<ActiveCallInfo?>(null)

    /** Foreground / primary call. */
    val info: StateFlow<ActiveCallInfo?> = _info.asStateFlow()

    /** Background / held call (non-null when there are two simultaneous calls). */
    val secondCall: StateFlow<ActiveCallInfo?> = _secondCall.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var dtmfStopRunnable: Runnable? = null

    // ── Update / remove ──────────────────────────────────────────────────────

    /**
     * Called by [MyInCallService] whenever call state or details change.
     * Pass a non-null [context] on the first call so a photo lookup can be performed;
     * subsequent updates reuse the already-resolved photo.
     */
    fun update(call: Call, context: Context? = null) {
        val number = call.details.handle?.schemeSpecificPart.orEmpty()

        val primary   = _info.value
        val secondary = _secondCall.value

        // Always do a contacts DB lookup first — it's the most reliable source
        val lookup = if (context != null && number.isNotBlank()) lookupContact(context, number) else null
        val contactName = lookup?.name?.takeIf { it.isNotBlank() }

        // Fall back to telecom framework names only when contacts DB has no match
        val telecomName = listOfNotNull(
            call.details.callerDisplayName?.takeIf { it.isNotBlank() && !isPhoneNumber(it) },
            call.details.contactDisplayName?.takeIf { it.isNotBlank() && !isPhoneNumber(it) }
        ).firstOrNull()

        val resolvedName = contactName
            ?: telecomName
            ?: number.ifBlank { "Unknown" }
        val resolvedPhoto = lookup?.photoUri

        when {
            // Update existing primary call in-place.
            primary?.call === call -> {
                // Always prefer a real contact name over what we had before
                val bestName = if (contactName != null) contactName
                    else if (isPhoneNumber(primary.displayName)) resolvedName
                    else primary.displayName
                _info.value = primary.copy(
                    state       = call.state,
                    displayName = bestName,
                    photoUri    = resolvedPhoto ?: primary.photoUri
                )
            }
            // Update existing secondary call in-place.
            secondary?.call === call -> {
                val bestName = if (contactName != null) contactName
                    else if (isPhoneNumber(secondary.displayName)) resolvedName
                    else secondary.displayName
                _secondCall.value = secondary.copy(
                    state       = call.state,
                    displayName = bestName,
                    photoUri    = resolvedPhoto ?: secondary.photoUri
                )
            }
            // New call — no primary yet.
            primary == null -> {
                _info.value = ActiveCallInfo(call, call.state, resolvedName, number, resolvedPhoto)
            }
            // Second new call — slot it as secondary.
            secondary == null -> {
                _secondCall.value = ActiveCallInfo(call, call.state, resolvedName, number, resolvedPhoto)
            }
            // Third call — replace primary.
            else -> {
                _info.value = ActiveCallInfo(call, call.state, resolvedName, number, resolvedPhoto)
            }
        }
    }

    /** Remove a specific call (called from [MyInCallService.onCallRemoved]). */
    fun remove(call: Call) {
        when {
            _info.value?.call === call -> {
                // Promote secondary to primary when foreground call ends.
                _info.value       = _secondCall.value
                _secondCall.value = null
            }
            _secondCall.value?.call === call -> {
                _secondCall.value = null
            }
        }
    }

    fun clear() {
        _info.value       = null
        _secondCall.value = null
    }

    // ── Multi-call actions ───────────────────────────────────────────────────

    /**
     * Swap foreground and background calls.
     * Puts the current primary on hold and resumes the secondary.
     */
    fun swap() {
        val p = _info.value ?: return
        val s = _secondCall.value ?: return
        p.call.hold()
        s.call.unhold()
        _info.value       = s
        _secondCall.value = p
    }

    // ── Primary call actions ─────────────────────────────────────────────────

    fun answer()  { _info.value?.call?.answer(0 /* VideoProfile.STATE_AUDIO_ONLY */) }
    fun reject()  { _info.value?.call?.reject(false, null) }
    fun hangup()  { _info.value?.call?.disconnect() }
    fun hold()    { _info.value?.call?.hold() }
    fun unhold()  { _info.value?.call?.unhold() }

    /**
     * In-band DTMF for IVR (0–9, *, #) on the primary call while [active][Call.STATE_ACTIVE].
     * @return true if a tone was sent (used to consume hardware keys on [InCallActivity]).
     */
    fun tryPlayDtmfOnPrimaryCall(digit: Char): Boolean {
        val info = _info.value ?: return false
        if (info.state != Call.STATE_ACTIVE) return false
        if (digit !in '0'..'9' && digit != '*' && digit != '#') return false
        return try {
            // Some device/framework combos can leave the DTMF tone "stuck"
            // unless it's explicitly stopped. We schedule a stop shortly after.
            dtmfStopRunnable?.let { mainHandler.removeCallbacks(it) }

            info.call.playDtmfTone(digit)
            val runnable = Runnable {
                stopDtmfInternal()
            }
            dtmfStopRunnable = runnable
            mainHandler.postDelayed(runnable, 350L)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Stops the current DTMF tone on the primary call (best-effort). */
    fun stopDtmfOnPrimaryCall() {
        dtmfStopRunnable?.let { mainHandler.removeCallbacks(it) }
        dtmfStopRunnable = null
        stopDtmfInternal()
    }

    private fun stopDtmfInternal() {
        val info = _info.value ?: return
        if (info.state != Call.STATE_ACTIVE) return
        try {
            info.call.stopDtmfTone()
        } catch (_: Exception) { }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Returns true if the string looks like a phone number rather than a contact name. */
    private fun isPhoneNumber(s: String): Boolean =
        s == "Unknown" || s.count { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' } == s.length

    /** Lookup contact name + photo from the system contacts by phone number. */
    private data class ContactLookup(val name: String?, val photoUri: String?)

    private fun lookupContact(context: Context, number: String): ContactLookup {
        if (number.isBlank()) return ContactLookup(null, null)

        // Try with the original number first, then with a cleaned version
        val numbersToTry = mutableListOf(number)
        val cleaned = number.replace(Regex("[\\s\\-().]"), "")
        if (cleaned != number) numbersToTry.add(cleaned)

        for (num in numbersToTry) {
            try {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(num)
                )
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                        ContactsContract.PhoneLookup.PHOTO_URI
                    ),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return ContactLookup(
                            name = cursor.getString(0),
                            photoUri = cursor.getString(1) ?: cursor.getString(2)
                        )
                    }
                }
            } catch (_: Exception) { /* try next format */ }
        }
        return ContactLookup(null, null)
    }

    @Suppress("DEPRECATION")
    private fun lookupPhoto(context: Context, number: String): String? =
        lookupContact(context, number).photoUri
}
