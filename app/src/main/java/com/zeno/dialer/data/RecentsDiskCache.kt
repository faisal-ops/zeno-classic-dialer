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

    /** Serializes writes and pairs with a temp-file-then-rename so concurrent callers never interleave or read a partial file. */
    private val writeLock = Any()

    fun save(context: Context, contacts: List<Contact>) {
        if (contacts.isEmpty()) return
        synchronized(writeLock) {
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
                val tmp = File(file.parentFile, "$FILE_NAME.tmp")
                tmp.writeText(sb.toString())
                tmp.renameTo(file)
            } catch (_: Exception) {}
        }
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

    // Single-pass escape/unescape so they're true inverses — sequential global replace() calls
    // can misinterpret a literal backslash immediately followed by 'n'/'t' after the first pass.
    private fun escape(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\t'  -> sb.append("\\t")
                '\n'  -> sb.append("\\n")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    't' -> { sb.append('\t'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    else -> { sb.append(c); i += 1 }
                }
            } else {
                sb.append(c); i += 1
            }
        }
        return sb.toString()
    }
}
