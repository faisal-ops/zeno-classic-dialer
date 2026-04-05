package com.zeno.dialer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SearchEngine(
    private val contactsRepo: ContactsRepo,
    private val recentsRepo: RecentsRepo
) {

    fun search(query: String, mode: FilterMode): List<Contact> {
        if (mode == FilterMode.CONTACTS) {
            return contactsRepo.search(query)
        }
        val recents = when (mode) {
            FilterMode.MISSED ->
                recentsRepo.search(query, missedOnly = true, limit = 100)
            FilterMode.RECEIVED ->
                recentsRepo.search(query, incomingOnly = true, limit = 100)
            FilterMode.ALL, FilterMode.RECENTS ->
                recentsRepo.search(query, limit = 100)
            FilterMode.CONTACTS ->
                recentsRepo.search(query, limit = 100)
        }
        return enrichRecentsFromContacts(recents)
    }

    suspend fun searchAsync(query: String, mode: FilterMode): List<Contact> = coroutineScope {
        if (mode == FilterMode.CONTACTS) {
            return@coroutineScope contactsRepo.search(query)
        }

        val recentsDeferred = async(Dispatchers.IO) {
            when (mode) {
                FilterMode.MISSED ->
                    recentsRepo.search(query, missedOnly = true, limit = 100)
                FilterMode.RECEIVED ->
                    recentsRepo.search(query, incomingOnly = true, limit = 100)
                FilterMode.ALL, FilterMode.RECENTS ->
                    recentsRepo.search(query, limit = 100)
                FilterMode.CONTACTS ->
                    recentsRepo.search(query, limit = 100)
            }
        }

        // Pre-warm contacts cache in parallel with recents fetch
        async(Dispatchers.Default) { contactsRepo.search("") }.await()
        val recents = recentsDeferred.await()
        enrichRecentsFromContacts(recents)
    }

    fun keypadContactMatch(query: String, mode: FilterMode, results: List<Contact>): Contact? {
        if (query.isBlank()) return null
        if (mode == FilterMode.CONTACTS) return results.firstOrNull()
        return contactsRepo.search(query).firstOrNull()
    }

    fun getTopRecents(n: Int): List<Contact> = recentsRepo.search("", limit = n)

    private fun enrichRecentsFromContacts(recents: List<Contact>): List<Contact> {
        if (recents.isEmpty()) return recents
        val merged = ArrayList<Contact>(recents.size)
        val seen = HashSet<String>(recents.size * 2)
        for (recent in recents) {
            val key = recent.number.filter { it.isDigit() }
            if (!seen.add(key)) continue
            val enriched = contactsRepo.lookupByNumber(key)
            if (enriched != null && enriched.name != recent.number) {
                merged.add(recent.copy(name = enriched.name, id = enriched.id, photoUri = recent.photoUri ?: enriched.photoUri))
            } else {
                merged.add(recent)
            }
        }
        return merged
    }
}
