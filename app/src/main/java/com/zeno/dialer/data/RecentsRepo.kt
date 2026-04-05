package com.zeno.dialer.data

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import com.zeno.dialer.BuildConfig

class RecentsRepo(private val context: Context) {

    private val phoneLookupCache = HashMap<String, Triple<String?, String?, Long>>(128)

    fun search(
        query: String,
        missedOnly: Boolean = false,
        incomingOnly: Boolean = false,
        limit: Int = 20,
    ): List<Contact> {
        if (BuildConfig.DEBUG) {
            var list = PrototypeData.recents
            if (missedOnly) list = list.filter { it.callType == CallLog.Calls.MISSED_TYPE }
            else if (incomingOnly) list = list.filter { it.callType == CallLog.Calls.INCOMING_TYPE }
            if (query.isNotBlank()) {
                list = FuzzySearch.rank(query, list).take(limit)
            }
            return list.take(limit)
        }
        val conditions = mutableListOf<String>()
        val args       = mutableListOf<String>()

        when {
            missedOnly ->
                conditions.add("${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE}")
            incomingOnly ->
                conditions.add("${CallLog.Calls.TYPE} = ${CallLog.Calls.INCOMING_TYPE}")
        }

        val selection = conditions.joinToString(" AND ").ifEmpty { null }

        val cursor = try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.CACHED_PHOTO_URI
                ),
                selection,
                args.toTypedArray().ifEmpty { null },
                "${CallLog.Calls.DATE} DESC"
            )
        } catch (e: SecurityException) {
            null
        } ?: return emptyList()

        val recents     = ArrayList<Contact>(minOf(limit * 2, 200))
        val seenNumbers = HashSet<String>(200)

        val fetchCap = if (query.isBlank()) limit
        else minOf(500, maxOf(limit * 15, 200))

        cursor.use {
            val nameCol  = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val numCol   = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateCol  = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val typeCol  = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val photoCol = it.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)

            while (it.moveToNext() && recents.size < fetchCap) {
                val number = it.getString(numCol).orEmpty().trim()
                if (number.isEmpty() || !seenNumbers.add(number)) continue

                val cachedName  = it.getString(nameCol).orEmpty().trim()
                val cachedPhoto = if (photoCol >= 0) it.getString(photoCol) else null

                val resolvedName: String
                val resolvedPhoto: String?
                val resolvedId: Long

                if (cachedName.isNotEmpty()) {
                    resolvedName = cachedName
                    resolvedPhoto = cachedPhoto
                    resolvedId = phoneLookupCached(number).third
                } else {
                    val (lookupName, lookupPhoto, lookupId) = phoneLookupCached(number)
                    resolvedName = lookupName ?: number
                    resolvedPhoto = cachedPhoto ?: lookupPhoto
                    resolvedId = lookupId
                }

                recents.add(
                    Contact(
                        id           = resolvedId,
                        name         = resolvedName,
                        number       = number,
                        isRecent     = true,
                        lastCallTime = it.getLong(dateCol),
                        callType     = it.getInt(typeCol),
                        photoUri     = resolvedPhoto
                    )
                )
            }
        }

        return if (query.isBlank()) recents else FuzzySearch.rank(query, recents).take(limit)
    }

    fun getHistoryForNumber(number: String, limit: Int = 100): List<Contact> {
        if (BuildConfig.DEBUG) {
            return PrototypeData.recents
                .filter { it.number == number }
                .take(limit)
        }
        if (number.isBlank()) return emptyList()
        val cursor = try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.CACHED_PHOTO_URI
                ),
                "${CallLog.Calls.NUMBER} = ?",
                arrayOf(number),
                "${CallLog.Calls.DATE} DESC"
            )
        } catch (e: SecurityException) { null } ?: return emptyList()

        val (lookupName, lookupPhoto, lookupId) = phoneLookupCached(number)

        val history = ArrayList<Contact>(minOf(limit, 64))
        cursor.use {
            val nameCol  = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val numCol   = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateCol  = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val typeCol  = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val photoCol = it.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)
            while (it.moveToNext() && history.size < limit) {
                val num        = it.getString(numCol).orEmpty().trim()
                val cachedName = it.getString(nameCol).orEmpty().trim()
                val cachedPhoto = if (photoCol >= 0) it.getString(photoCol) else null
                val resolvedName  = cachedName.ifEmpty { lookupName }
                val resolvedPhoto = cachedPhoto ?: lookupPhoto
                history.add(Contact(
                    id           = lookupId,
                    name         = resolvedName ?: num,
                    number       = num,
                    isRecent     = true,
                    lastCallTime = it.getLong(dateCol),
                    callType     = it.getInt(typeCol),
                    photoUri     = resolvedPhoto
                ))
            }
        }
        return history
    }

    fun deleteByNumber(number: String): Int {
        if (BuildConfig.DEBUG) return 0
        if (number.isBlank()) return 0
        return try {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls.NUMBER} = ?",
                arrayOf(number)
            ) ?: 0
        } catch (_: Exception) { 0 }
    }

    fun clearLookupCache() { phoneLookupCache.clear() }

    private fun phoneLookupCached(number: String): Triple<String?, String?, Long> {
        if (number.isBlank()) return Triple(null, null, 0L)
        phoneLookupCache[number]?.let { return it }
        val result = phoneLookupDirect(number)
        phoneLookupCache[number] = result
        return result
    }

    private fun phoneLookupDirect(number: String): Triple<String?, String?, Long> {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                    ContactsContract.PhoneLookup.CONTACT_ID
                ),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) Triple(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getLong(2)
                )
                else Triple(null, null, 0L)
            } ?: Triple(null, null, 0L)
        } catch (_: Exception) {
            Triple(null, null, 0L)
        }
    }
}
