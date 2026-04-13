package com.zeno.dialer.data

import android.content.Context
import android.provider.CallLog
import java.io.File

/**
 * Persists the last fetched recents list to a flat file so the app can show call
 * history immediately on startup without waiting for a ContentProvider query.
 *
 * Format (one contact per line, fields separated by TAB):
 *   id \t name \t number \t callType \t lastCallTime \t photoUri
 */
object RecentsDiskCache {

    private const val FILE_NAME = "recents_cache.tsv"
    private const val MAX_ENTRIES = 100
    private const val SEP = "\t"

    fun save(context: Context, contacts: List<Contact>) {
        if (contacts.isEmpty()) return
        try {
            val file = cacheFile(context)
            val sb = StringBuilder(contacts.size * 80)
            contacts.take(MAX_ENTRIES).forEach { c ->
                sb.append(c.id).append(SEP)
                sb.append(escape(c.name)).append(SEP)
                sb.append(escape(c.number)).append(SEP)
                sb.append(c.callType).append(SEP)
                sb.append(c.lastCallTime).append(SEP)
                sb.append(escape(c.photoUri ?: ""))
                sb.append('\n')
            }
            file.writeText(sb.toString())
        } catch (_: Exception) {}
    }

    fun load(context: Context): List<Contact> {
        return try {
            val file = cacheFile(context)
            if (!file.exists()) return emptyList()
            val lines = file.readLines()
            val result = ArrayList<Contact>(lines.size)
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split(SEP)
                if (parts.size < 6) continue
                result.add(
                    Contact(
                        id          = parts[0].toLongOrNull() ?: 0L,
                        name        = unescape(parts[1]),
                        number      = unescape(parts[2]),
                        callType    = parts[3].toIntOrNull() ?: CallLog.Calls.OUTGOING_TYPE,
                        lastCallTime = parts[4].toLongOrNull() ?: 0L,
                        photoUri    = unescape(parts[5]).ifEmpty { null },
                        isRecent    = true
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        try { cacheFile(context).delete() } catch (_: Exception) {}
    }

    private fun cacheFile(context: Context) = File(context.filesDir, FILE_NAME)

    // Escape newlines and tabs so they don't break the format
    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")
    private fun unescape(s: String) = s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\")
}
