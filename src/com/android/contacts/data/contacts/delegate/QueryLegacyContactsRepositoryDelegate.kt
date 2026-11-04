@file:Suppress("DEPRECATION")

package com.android.contacts.data.contacts.delegate

import android.content.ContentResolver
import android.database.Cursor
import android.provider.Contacts
import android.provider.Contacts.ContactMethods
import android.provider.Contacts.People
import android.provider.Contacts.Phones
import android.provider.ContactsContract
import androidx.annotation.VisibleForTesting
import com.android.contacts.data.contacts.delegate.util.getField
import com.android.contacts.data.contacts.model.LegacyContactsKind
import com.android.contacts.di.core.IoDispatcher
import com.android.contacts.domain.contacts.model.ContactSummary
import com.android.contacts.util.core.observeContentUri
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface QueryLegacyContactsRepositoryDelegate {
    fun queryLegacyContacts(
        kind: LegacyContactsKind,
        searchQuery: String?,
    ): Flow<List<ContactSummary>>
}

internal class QueryLegacyContactsRepositoryDelegateImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : QueryLegacyContactsRepositoryDelegate {

    override fun queryLegacyContacts(
        kind: LegacyContactsKind,
        searchQuery: String?,
    ): Flow<List<ContactSummary>> {
        return observeContentUri(contentResolver, ContactsContract.AUTHORITY_URI)
            .map {
                query(kind).filter { contact ->
                    searchQuery == null || contact.name.contains(searchQuery, ignoreCase = true)
                }
            }
            .flowOn(ioDispatcher)
    }

    private fun query(kind: LegacyContactsKind): List<ContactSummary> {
        return when (kind) {
            LegacyContactsKind.PEOPLE -> {
                contentResolver.query(
                    People.CONTENT_URI,
                    PEOPLE_PROJECTION,
                    null,
                    null,
                    People.DISPLAY_NAME,
                )?.use { it.toPeopleSummaries() }
            }

            LegacyContactsKind.PHONES -> {
                contentResolver.query(
                    Phones.CONTENT_URI,
                    PHONES_PROJECTION,
                    null,
                    null,
                    People.DISPLAY_NAME,
                )?.use { it.toDataSummaries(kind) }
            }

            LegacyContactsKind.POSTAL_ADDRESSES -> {
                contentResolver.query(
                    ContactMethods.CONTENT_URI,
                    POSTAL_PROJECTION,
                    POSTAL_SELECTION,
                    null,
                    People.DISPLAY_NAME,
                )?.use { it.toDataSummaries(kind) }
            }
        }.orEmpty()
    }

    private fun Cursor.toPeopleSummaries(): List<ContactSummary> {
        return buildList {
            while (moveToNext()) {
                add(
                    ContactSummary(
                        contactId = getLong(PEOPLE_ID),
                        name = getString(PEOPLE_DISPLAY_NAME).orEmpty(),
                        isFavorite = getInt(PEOPLE_STARRED) != 0,
                    ),
                )
            }
        }
    }

    private fun Cursor.toDataSummaries(kind: LegacyContactsKind): List<ContactSummary> {
        return buildList {
            while (moveToNext()) {
                add(
                    ContactSummary(
                        contactId = getLong(DATA_ID),
                        name = getString(DATA_DISPLAY_NAME).orEmpty(),
                        field = getField(
                            kind = kind.toFieldKind(),
                            valueIndex = DATA_VALUE,
                            typeIndex = DATA_TYPE,
                            labelIndex = DATA_LABEL,
                        ),
                    ),
                )
            }
        }
    }

    private fun LegacyContactsKind.toFieldKind(): ContactSummary.FieldKind {
        return when (this) {
            LegacyContactsKind.PEOPLE -> ContactSummary.FieldKind.UNSPECIFIED
            LegacyContactsKind.PHONES -> ContactSummary.FieldKind.PHONE
            LegacyContactsKind.POSTAL_ADDRESSES -> ContactSummary.FieldKind.POSTAL_ADDRESS
        }
    }

    companion object {
        @VisibleForTesting
        val PEOPLE_PROJECTION = arrayOf(
            People._ID,
            People.DISPLAY_NAME,
            People.STARRED,
        )
        private const val PEOPLE_ID = 0
        private const val PEOPLE_DISPLAY_NAME = 1
        private const val PEOPLE_STARRED = 2

        @VisibleForTesting
        val PHONES_PROJECTION = arrayOf(
            Phones._ID,
            Phones.TYPE,
            Phones.LABEL,
            Phones.NUMBER,
            People.DISPLAY_NAME,
        )

        @VisibleForTesting
        val POSTAL_PROJECTION = arrayOf(
            ContactMethods._ID,
            ContactMethods.TYPE,
            ContactMethods.LABEL,
            ContactMethods.DATA,
            People.DISPLAY_NAME,
        )
        private const val DATA_ID = 0
        private const val DATA_TYPE = 1
        private const val DATA_LABEL = 2
        private const val DATA_VALUE = 3
        private const val DATA_DISPLAY_NAME = 4

        @VisibleForTesting
        const val POSTAL_SELECTION = ContactMethods.KIND + "=" + Contacts.KIND_POSTAL
    }
}
