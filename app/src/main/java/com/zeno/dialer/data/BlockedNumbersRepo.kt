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

    /**
     * Reconcile the local prefs list against the platform [BlockedNumbers] provider — the
     * "Blocked numbers" settings entry point opens the *system* management screen directly
     * (see [com.zeno.dialer.SettingsActivity]), so that's the primary place users add/remove
     * blocks. Called on every app resume via `onPermissionsReady()`.
     *
     * Bidirectional, but safely: a number is only removed locally if it was seen in the system
     * list on a *previous* sync and is now gone (i.e. the user unblocked it via system Settings).
     * Numbers added locally whose system mirror write failed (e.g. role not held yet) are left
     * alone rather than being silently dropped — we only "trust" system state we've actually seen.
     */
    fun syncFromSystem() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val systemNumbers = mutableSetOf<String>()
            val queried = context.contentResolver.query(
                BlockedNumbers.CONTENT_URI,
                arrayOf(BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val num = normalize(cursor.getString(0) ?: continue)
                    if (num.isNotBlank()) systemNumbers.add(num)
                }
                true
            } ?: false
            if (!queried) return

            val local = getAll()
            val lastKnownSystem = prefs.getStringSet(KEY_LAST_SYNCED_SYSTEM, emptySet()).orEmpty()

            val removedRemotely = local.filter { it in lastKnownSystem && it !in systemNumbers }.toSet()
            val addedRemotely = systemNumbers - local

            if (removedRemotely.isNotEmpty() || addedRemotely.isNotEmpty()) {
                save((local - removedRemotely) + addedRemotely)
            }
            prefs.edit().putStringSet(KEY_LAST_SYNCED_SYSTEM, systemNumbers).apply()
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

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
        } catch (_: Exception) {
        }
    }

    /**
     * Deletes every system-provider row whose stored number normalizes to [normalizedDigits],
     * rather than guessing at specific formats — a number added via the system "Blocked
     * numbers" UI, another dialer, or with punctuation intact may not be stored as bare digits
     * or "+digits", and a miss here leaves the number silently blocked at the OS level even
     * after the user "unblocks" it in Zeno.
     */
    private fun syncRemoveFromSystem(normalizedDigits: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val resolver = context.contentResolver
            val matches = mutableListOf<String>()
            resolver.query(
                BlockedNumbers.CONTENT_URI,
                arrayOf(BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val original = cursor.getString(0) ?: continue
                    if (normalize(original) == normalizedDigits) matches.add(original)
                }
            }
            for (original in matches) {
                resolver.delete(
                    BlockedNumbers.CONTENT_URI,
                    "${BlockedNumbers.COLUMN_ORIGINAL_NUMBER}=?",
                    arrayOf(original)
                )
            }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        } catch (_: UnsupportedOperationException) {
        } catch (_: Exception) {
        }
    }

    private companion object {
        private const val KEY_BLOCKED_NUMBERS = "blocked_numbers"
        private const val SEPARATOR = "|:|"
        /** Snapshot of the system-provider numbers seen on the last successful [syncFromSystem]. */
        private const val KEY_LAST_SYNCED_SYSTEM = "blocked_numbers_last_synced_system"
    }
}
