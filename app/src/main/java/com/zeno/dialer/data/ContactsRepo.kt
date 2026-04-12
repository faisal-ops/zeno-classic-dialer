package com.zeno.dialer.data

import android.content.Context
import android.provider.ContactsContract
import com.zeno.dialer.AppPreferences

class ContactsRepo(private val context: Context) {

    @Volatile private var cache: List<Contact>? = null
    @Volatile private var indexedCache: List<FuzzySearch.IndexedContact>? = null
    @Volatile private var numberLookup: Map<String, Contact>? = null

    fun search(query: String): List<Contact> {
        val all = ensureLoaded()
        if (query.isBlank()) return all
        val indexed = indexedCache ?: return FuzzySearch.rank(query, all)
        val qLower = query.lowercase()
        val qDigits = query.filter { it.isDigit() }

        val ranked = FuzzySearch.rankIndexed(qLower, qDigits, indexed)
        if (qDigits.isNotEmpty() && qDigits.length == query.length) {
            val t9Matches = ArrayList<Contact>()
            for (ic in indexed) {
                if (ic.t9Digits.contains(qDigits)) {
                    t9Matches.add(ic.contact)
                }
            }
            t9Matches.sortBy { ic ->
                val idx = indexedCache!!.first { it.contact === ic || (it.contact.id == ic.id && it.contact.number == ic.number) }.t9Digits.indexOf(qDigits)
                if (idx >= 0) idx else Int.MAX_VALUE
            }
            if (t9Matches.isNotEmpty()) {
                val seen = HashSet<Long>(ranked.size + t9Matches.size)
                val combined = ArrayList<Contact>(ranked.size + t9Matches.size)
                for (c in ranked) { if (seen.add(c.id * 31L + c.number.hashCode())) combined.add(c) }
                for (c in t9Matches) { if (seen.add(c.id * 31L + c.number.hashCode())) combined.add(c) }
                return combined
            }
        }
        return ranked
    }

    fun lookupByNumber(digits: String): Contact? = numberLookup?.get(digits)

    fun getIndexed(): List<FuzzySearch.IndexedContact> {
        ensureLoaded()
        return indexedCache ?: emptyList()
    }

    private fun ensureLoaded(): List<Contact> {
        cache?.let { return it }
        val all = loadAll()
        cache = all
        buildIndex(all)
        return all
    }

    private fun buildIndex(contacts: List<Contact>) {
        val indexed = ArrayList<FuzzySearch.IndexedContact>(contacts.size)
        val lookup = HashMap<String, Contact>(contacts.size * 2)
        for (c in contacts) {
            indexed.add(FuzzySearch.IndexedContact(c))
            val digits = c.number.filter { it.isDigit() }
            if (digits.isNotEmpty()) lookup[digits] = c
        }
        indexedCache = indexed
        numberLookup = lookup
    }

    fun invalidate() {
        cache = null
        indexedCache = null
        numberLookup = null
    }

    fun getStarredContacts(): List<Contact> {
        val allowedContactIds = loadNonSimContactIds()
        val filterBySim = allowedContactIds.isNotEmpty()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1"
        val cursor = try {
            context.contentResolver.query(uri, projection, selection, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC")
        } catch (_: SecurityException) { null } ?: return emptyList()

        val contacts = mutableListOf<Contact>()
        val seenIds = mutableSetOf<Long>()
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                if (filterBySim && id !in allowedContactIds) continue
                if (id in seenIds) continue
                seenIds.add(id)
                val number = it.getString(numCol).orEmpty().trim()
                if (number.isEmpty()) continue
                val name = it.getString(nameCol).orEmpty().trim().ifEmpty { number }
                val photo = if (photoCol >= 0) it.getString(photoCol) else null
                contacts.add(Contact(id = id, name = name, number = number, photoUri = photo))
            }
        }
        return contacts
    }

    private fun loadAll(): List<Contact> {
        val allowedContactIds = loadNonSimContactIds()
        val filterBySim = allowedContactIds.isNotEmpty()
        val prefs = context.getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE)
        val sortBy = prefs.getInt("sort_by", 0)
        val nameFormat = prefs.getInt("name_format", 0)

        val uri        = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        val cursor = try {
            context.contentResolver.query(
                uri, projection, null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
        } catch (e: SecurityException) {
            null
        } ?: return emptyList()

        val contacts = ArrayList<Contact>(cursor.count.coerceAtLeast(64))
        val seenIds  = HashSet<Long>(cursor.count.coerceAtLeast(64))

        cursor.use {
            val idCol    = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol  = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol   = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                if (filterBySim && id !in allowedContactIds) continue
                if (!seenIds.add(id)) continue

                val number = it.getString(numCol).orEmpty().trim()
                if (number.isEmpty()) continue

                val rawName = it.getString(nameCol).orEmpty().trim().ifEmpty { number }
                val name = if (nameFormat == 1 && rawName != number) {
                    val parts = rawName.split(" ", limit = 2)
                    if (parts.size == 2) "${parts[1]}, ${parts[0]}" else rawName
                } else rawName
                val photo = if (photoCol >= 0) it.getString(photoCol) else null

                contacts.add(Contact(id = id, name = name, number = number, photoUri = photo))
            }
        }

        return if (sortBy == 1) {
            contacts.sortedBy { c ->
                val parts = c.name.split(" ")
                if (parts.size > 1) parts.last() else c.name
            }
        } else {
            contacts
        }
    }

    private fun loadNonSimContactIds(): Set<Long> {
        val uri = ContactsContract.RawContacts.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.RawContacts.CONTACT_ID,
            ContactsContract.RawContacts.ACCOUNT_TYPE
        )
        val selection = "${ContactsContract.RawContacts.DELETED} = 0"
        val cursor = try {
            context.contentResolver.query(uri, projection, selection, null, null)
        } catch (_: SecurityException) {
            null
        } ?: return emptySet()

        val allowed = HashSet<Long>(cursor.count.coerceAtLeast(64))
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID)
            val accountTypeCol = it.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)
            while (it.moveToNext()) {
                val contactId = it.getLong(idCol)
                val accountType = it.getString(accountTypeCol).orEmpty().lowercase()
                val isSim = accountType.contains("sim") || accountType.contains("icc")
                if (!isSim) allowed.add(contactId)
            }
        }
        return allowed
    }
}
