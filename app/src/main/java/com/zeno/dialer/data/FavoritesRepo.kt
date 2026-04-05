package com.zeno.dialer.data

import android.content.Context

class FavoritesRepo(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPinnedNumbers(): List<String> {
        val raw = prefs.getString(KEY_FAVORITES, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
    }

    fun add(number: String) {
        val normalized = normalize(number)
        if (normalized.isBlank()) return
        val current = getPinnedNumbers().toMutableList()
        if (current.none { normalize(it) == normalized }) {
            current.add(number)
            save(current)
        }
    }

    fun remove(number: String) {
        val normalized = normalize(number)
        if (normalized.isBlank()) return
        val updated = getPinnedNumbers().filterNot { normalize(it) == normalized }
        save(updated)
    }

    private fun save(values: List<String>) {
        prefs.edit().putString(KEY_FAVORITES, values.joinToString(SEPARATOR)).apply()
    }

    private fun normalize(number: String): String = number.filter { it.isDigit() }

    private companion object {
        private const val PREFS_NAME = "zeno_favorites"
        private const val KEY_FAVORITES = "pinned_numbers"
        private const val SEPARATOR = "|:|"
    }
}
