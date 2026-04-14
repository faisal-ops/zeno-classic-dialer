package com.zeno.dialer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchTest {

    @Test
    fun shortQuery_doesNotUseLooseSequenceFallback() {
        val score = FuzzySearch.score("shin", "Rishi Kant Excitel")
        assertEquals(0, score)
    }

    @Test
    fun shortQuery_doesNotUseContainsFallback() {
        val score = FuzzySearch.score("aman", "Naman")
        assertEquals(0, score)
    }

    @Test
    fun longerQuery_keepsSequenceFallback() {
        val score = FuzzySearch.score("rshkn", "Rishi Kant Excitel")
        assertEquals(40, score)
    }

    @Test
    fun longerQuery_keepsContainsFallback() {
        val score = FuzzySearch.score("xcite", "Rishi Kant Excitel")
        assertEquals(70, score)
    }

    @Test
    fun shortQuery_ranksPrefixMatchAboveLooseMatches() {
        val contacts = listOf(
            Contact(id = 1L, name = "Rishi Kant Excitel", number = "11111"),
            Contact(id = 2L, name = "Shin The Dwarf", number = "22222")
        )

        val ranked = FuzzySearch.rank("shin", contacts)

        assertTrue(ranked.isNotEmpty())
        assertEquals("Shin The Dwarf", ranked.first().name)
    }

    @Test
    fun shortQuery_filtersOutSubstringOnlyMatches() {
        val contacts = listOf(
            Contact(id = 1L, name = "Aman Jindal", number = "11111"),
            Contact(id = 2L, name = "Amandeep Singh", number = "22222"),
            Contact(id = 3L, name = "Naman", number = "33333")
        )

        val ranked = FuzzySearch.rank("aman", contacts)
        val names = ranked.map { it.name }

        assertTrue(names.contains("Aman Jindal"))
        assertTrue(names.contains("Amandeep Singh"))
        assertTrue(!names.contains("Naman"))
    }
}
