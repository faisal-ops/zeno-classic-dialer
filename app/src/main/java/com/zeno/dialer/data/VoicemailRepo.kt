package com.zeno.dialer.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.VoicemailContract.Voicemails
import android.telephony.CarrierConfigManager
import com.zeno.dialer.BuildConfig

/**
 * Reads Android's platform `VoicemailContract.Voicemails` provider — populated automatically by
 * the telephony stack on carriers with platform-level OMTP/CVVM support (Verizon, AT&T,
 * T-Mobile, others), once the app holds ROLE_DIALER (which auto-grants READ_VOICEMAIL). This is
 * read-only: no carrier VVM protocol is implemented here, so on carriers/devices the platform
 * never populates, [list] simply returns empty.
 */
class VoicemailRepo(private val context: Context, private val contactsRepo: ContactsRepo) {

    fun list(): List<Voicemail> {
        if (BuildConfig.DEBUG) return PrototypeData.voicemails
        val cursor = try {
            context.contentResolver.query(
                Voicemails.CONTENT_URI,
                arrayOf(
                    Voicemails._ID,
                    Voicemails.NUMBER,
                    Voicemails.DATE,
                    Voicemails.DURATION,
                    Voicemails.IS_READ,
                    Voicemails.HAS_CONTENT,
                ),
                null, null,
                "${Voicemails.DATE} DESC"
            )
        } catch (_: SecurityException) { null } ?: return emptyList()

        val result = ArrayList<Voicemail>()
        cursor.use {
            val idCol       = it.getColumnIndexOrThrow(Voicemails._ID)
            val numCol      = it.getColumnIndexOrThrow(Voicemails.NUMBER)
            val dateCol     = it.getColumnIndexOrThrow(Voicemails.DATE)
            val durCol      = it.getColumnIndex(Voicemails.DURATION)
            val isReadCol   = it.getColumnIndex(Voicemails.IS_READ)
            val hasContentCol = it.getColumnIndex(Voicemails.HAS_CONTENT)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val number = it.getString(numCol).orEmpty()
                val duration = if (durCol >= 0 && !it.isNull(durCol)) it.getLong(durCol) else null
                val isRead = if (isReadCol >= 0) it.getInt(isReadCol) != 0 else true
                val hasContent = if (hasContentCol >= 0) it.getInt(hasContentCol) != 0 else true

                val match = contactsRepo.lookupByNumber(number.filter(Char::isDigit))

                result.add(
                    Voicemail(
                        id = id,
                        number = number,
                        name = match?.name,
                        photoUri = match?.photoUri,
                        date = it.getLong(dateCol),
                        durationSeconds = duration,
                        isRead = isRead,
                        hasContent = hasContent,
                        contentUri = ContentUris.withAppendedId(Voicemails.CONTENT_URI, id),
                    )
                )
            }
        }
        return result
    }

    /** Tab-independent check for the Voicemail-chip unread dot. */
    fun hasUnread(): Boolean {
        if (BuildConfig.DEBUG) return false
        return try {
            context.contentResolver.query(
                Voicemails.CONTENT_URI,
                arrayOf(Voicemails._ID),
                "${Voicemails.IS_READ} = 0",
                null, null
            )?.use { it.count > 0 } ?: false
        } catch (_: Exception) { false }
    }

    fun markRead(id: Long) {
        if (BuildConfig.DEBUG) return
        try {
            val values = ContentValues(1).apply { put(Voicemails.IS_READ, 1) }
            context.contentResolver.update(
                ContentUris.withAppendedId(Voicemails.CONTENT_URI, id),
                values, null, null
            )
        } catch (_: Exception) { }
    }

    fun delete(id: Long) {
        if (BuildConfig.DEBUG) return
        try {
            context.contentResolver.delete(
                ContentUris.withAppendedId(Voicemails.CONTENT_URI, id),
                null, null
            )
        } catch (_: Exception) { }
    }

    /**
     * Whether the active SIM's carrier config declares OMTP/CVVM support at all — independent of
     * whether any voicemails exist yet. Most non-US carriers (including Indian carriers) leave
     * this empty and only offer dial-in voicemail, in which case [list] will always be empty
     * regardless of what this app does.
     *
     * CarrierConfigManager#getConfig is annotated as requiring READ_PHONE_STATE (already declared
     * and held by this app as the default dialer); suppressed since lint doesn't recognize the
     * surrounding try/catch here as a guard.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun carrierSupportsVisualVoicemail(): Boolean {
        if (BuildConfig.DEBUG) return true
        return try {
            val ccm = context.getSystemService(CarrierConfigManager::class.java)
            val type = ccm?.config?.getString(CarrierConfigManager.KEY_VVM_TYPE_STRING)
            !type.isNullOrEmpty()
        } catch (_: Exception) { false }
    }
}
