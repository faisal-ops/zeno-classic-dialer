package com.zeno.dialer.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.BlockedNumberContract.BlockedNumbers
import com.zeno.dialer.AppPreferences

class BlockedNumbersRepo(private val context: Context) {
    private val prefs = context.getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE)

    fun getAll(): Set<String> {
        val raw = prefs.getString(KEY_BLOCKED_NUMBERS, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(SEPARATOR)
            .map { it.trim() }
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun add(number: String) {
        val normalized = normalize(number)
        if (normalized.isBlank()) return
        val values = getAll().toMutableSet()
        values.add(normalized)
        save(values)
        syncAddToSystem(normalized)
    }

    fun remove(number: String) {
        val normalized = normalize(number)
        if (normalized.isBlank()) return
        val values = getAll().toMutableSet()
        values.remove(normalized)
        save(values)
        syncRemoveFromSystem(normalized)
    }

    fun contains(number: String): Boolean = normalize(number) in getAll()

    private fun save(values: Set<String>) {
        prefs.edit().putString(KEY_BLOCKED_NUMBERS, values.joinToString(SEPARATOR)).apply()
    }

    private fun normalize(number: String): String = number.filter { it.isDigit() }

    /**
     * Mirror blocks into the platform [BlockedNumbers] provider so the OS (and OEM stacks)
     * honor “block” the same way as the in-app list. Requires default dialer (or eligible role);
     * failures are ignored — [ScreeningService] still reads local prefs.
     */
    private fun syncAddToSystem(normalizedDigits: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val values = ContentValues().apply {
                put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, normalizedDigits)
            }
            context.contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        } catch (_: UnsupportedOperationException) {
        }
    }

    private fun syncRemoveFromSystem(normalizedDigits: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val n = context.contentResolver.delete(
                BlockedNumbers.CONTENT_URI,
                "${BlockedNumbers.COLUMN_ORIGINAL_NUMBER}=?",
                arrayOf(normalizedDigits)
            )
            if (n > 0) return
            // Some builds match on formatted numbers — try common variants.
            context.contentResolver.delete(
                BlockedNumbers.CONTENT_URI,
                "${BlockedNumbers.COLUMN_ORIGINAL_NUMBER}=?",
                arrayOf("+${normalizedDigits}")
            )
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        } catch (_: UnsupportedOperationException) {
        }
    }

    private companion object {
        private const val KEY_BLOCKED_NUMBERS = "blocked_numbers"
        private const val SEPARATOR = "|:|"
    }
}
