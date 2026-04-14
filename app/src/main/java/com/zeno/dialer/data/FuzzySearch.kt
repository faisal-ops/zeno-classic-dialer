package com.zeno.dialer.data

/**
 * High-performance fuzzy scorer optimized for sub-millisecond per-contact scoring.
 *
 * All string normalization (lowercase, digit extraction, word splitting) is done once
 * at cache-build time via [IndexedContact], not per-query.
 *
 * Scoring tiers (higher = better match):
 *   100 – exact match
 *    90 – target starts with query / phone digits match
 *    82 – partial phone match (query is prefix of digits and ≥4 chars)
 *    80 – any word in target starts with query
 *    70 – target contains query as substring
 *    40 – query chars appear in order in target ("jhn" → "John"), only for longer queries
 *     0 – no match
 */
object FuzzySearch {
    private const val MIN_QUERY_LEN_FOR_SEQUENCE_FALLBACK = 5
    private const val MIN_QUERY_LEN_FOR_CONTAINS_FALLBACK = 5

    /**
     * Pre-indexed contact for O(1) field access during scoring.
     * Built once when contacts are loaded; reused across all queries.
     */
    class IndexedContact(val contact: Contact) {
        val nameLower: String = contact.name.lowercase()
        val numberDigits: String = contact.number.filter { it.isDigit() }
        val words: List<String> = nameLower.splitToWords()
        val t9Digits: String = nameToT9(contact.name)

        companion object {
            private fun String.splitToWords(): List<String> {
                val result = mutableListOf<String>()
                var start = -1
                for (i in indices) {
                    val c = this[i]
                    if (c.isLetterOrDigit()) {
                        if (start < 0) start = i
                    } else {
                        if (start >= 0) {
                            result.add(substring(start, i))
                            start = -1
                        }
                    }
                }
                if (start >= 0) result.add(substring(start))
                return result
            }

            private fun nameToT9(name: String): String {
                val sb = StringBuilder(name.length)
                for (ch in name) {
                    val lower = ch.lowercaseChar()
                    val d = when (lower) {
                        in 'a'..'c' -> '2'
                        in 'd'..'f' -> '3'
                        in 'g'..'i' -> '4'
                        in 'j'..'l' -> '5'
                        in 'm'..'o' -> '6'
                        in 'p'..'s' -> '7'
                        in 't'..'v' -> '8'
                        in 'w'..'z' -> '9'
                        else -> null
                    }
                    if (d != null) sb.append(d)
                }
                return sb.toString()
            }
        }
    }

    fun scoreIndexed(queryLower: String, queryDigits: String, ic: IndexedContact): Int {
        if (queryLower.isEmpty()) return 50

        if (queryDigits.isNotEmpty() && ic.numberDigits.isNotEmpty()) {
            when {
                ic.numberDigits == queryDigits -> return 100
                ic.numberDigits.startsWith(queryDigits) || ic.numberDigits.contains(queryDigits) -> return 90
                queryDigits.startsWith(ic.numberDigits) && ic.numberDigits.length >= 4 -> return 82
            }
        }

        val t = ic.nameLower
        return when {
            t == queryLower -> 100
            t.startsWith(queryLower) -> 90
            ic.words.any { it.startsWith(queryLower) } -> 80
            queryLower.length >= MIN_QUERY_LEN_FOR_CONTAINS_FALLBACK &&
                t.contains(queryLower) -> 70
            queryLower.length >= MIN_QUERY_LEN_FOR_SEQUENCE_FALLBACK &&
                charSequenceMatch(queryLower, t) -> 40
            else -> 0
        }
    }

    fun score(query: String, target: String): Int {
        if (query.isEmpty()) return 50
        val q = query.lowercase()
        val t = target.lowercase()
        val qDigits = query.filter { it.isDigit() }
        val tDigits = target.filter { it.isDigit() }
        if (qDigits.isNotEmpty() && tDigits.isNotEmpty()) {
            when {
                tDigits == qDigits -> return 100
                tDigits.startsWith(qDigits) || tDigits.contains(qDigits) -> return 90
                qDigits.startsWith(tDigits) && tDigits.length >= 4 -> return 82
            }
        }
        return when {
            t == q -> 100
            t.startsWith(q) -> 90
            wordStartsWith(t, q) -> 80
            q.length >= MIN_QUERY_LEN_FOR_CONTAINS_FALLBACK &&
                t.contains(q) -> 70
            q.length >= MIN_QUERY_LEN_FOR_SEQUENCE_FALLBACK &&
                charSequenceMatch(q, t) -> 40
            else -> 0
        }
    }

    private fun wordStartsWith(text: String, prefix: String): Boolean {
        var i = 0
        while (i < text.length) {
            if (!text[i].isLetterOrDigit()) { i++; continue }
            var match = true
            for (j in prefix.indices) {
                if (i + j >= text.length || text[i + j] != prefix[j]) { match = false; break }
            }
            if (match) return true
            while (i < text.length && text[i].isLetterOrDigit()) i++
        }
        return false
    }

    fun matches(query: String, name: String, number: String): Boolean {
        if (query.isEmpty()) return true
        return score(query, name) > 0 || score(query, number) > 0
    }

    fun rankIndexed(queryLower: String, queryDigits: String, indexed: List<IndexedContact>, limit: Int = Int.MAX_VALUE): List<Contact> {
        if (queryLower.isEmpty()) return indexed.map { it.contact }

        val initCap = if (limit < Int.MAX_VALUE / 2) minOf(indexed.size, limit * 2) else indexed.size
        val scored = ArrayList<Pair<Contact, Int>>(initCap)
        for (ic in indexed) {
            val nameScore = scoreIndexed(queryLower, queryDigits, ic)
            val numScore = if (queryDigits.isNotEmpty() && ic.numberDigits.isNotEmpty()) {
                when {
                    ic.numberDigits == queryDigits -> 100
                    ic.numberDigits.startsWith(queryDigits) || ic.numberDigits.contains(queryDigits) -> 90
                    queryDigits.startsWith(ic.numberDigits) && ic.numberDigits.length >= 4 -> 82
                    else -> 0
                }
            } else 0
            val s = maxOf(nameScore, numScore)
            if (s > 0) scored.add(ic.contact to s)
        }
        scored.sortByDescending { it.second }
        val cap = minOf(scored.size, limit)
        val out = ArrayList<Contact>(cap)
        for (i in 0 until cap) out.add(scored[i].first)
        return out
    }

    fun rank(query: String, contacts: List<Contact>): List<Contact> {
        if (query.isEmpty()) return contacts
        return contacts
            .map { c -> c to maxOf(score(query, c.name), score(query, c.number)) }
            .filter { (_, s) -> s > 0 }
            .sortedByDescending { (_, s) -> s }
            .map { (c, _) -> c }
    }

    private fun charSequenceMatch(query: String, target: String): Boolean {
        var qi = 0
        for (c in target) {
            if (qi < query.length && c == query[qi]) qi++
        }
        return qi == query.length
    }
}
