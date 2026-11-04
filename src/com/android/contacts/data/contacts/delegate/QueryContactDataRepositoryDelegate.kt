package com.android.contacts.data.contacts.delegate

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Callable
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.Data
import androidx.annotation.VisibleForTesting
import com.android.contacts.data.contacts.delegate.util.addressBookSectionLabels
import com.android.contacts.data.contacts.delegate.util.appendAddressBookIndex
import com.android.contacts.data.contacts.delegate.util.appendDefaultDirectory
import com.android.contacts.data.contacts.delegate.util.getContactPhoto
import com.android.contacts.data.contacts.delegate.util.getDisplayName
import com.android.contacts.data.contacts.delegate.util.getField
import com.android.contacts.data.contacts.delegate.util.toSortKeyColumn
import com.android.contacts.data.contacts.model.ContactDataKind
import com.android.contacts.data.settings.model.DisplayOrder
import com.android.contacts.data.settings.model.SortOrder
import com.android.contacts.di.core.IoDispatcher
import com.android.contacts.domain.contacts.model.ContactSummary
import com.android.contacts.util.core.observeContentUri
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface QueryContactDataRepositoryDelegate {
    fun queryContactData(
        kind: ContactDataKind,
        searchQuery: String?,
        sortOrder: SortOrder,
        displayOrder: DisplayOrder,
        restrictToContactIds: Set<Long>? = null,
    ): Flow<List<ContactSummary>>
}

internal class QueryContactDataRepositoryDelegateImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : QueryContactDataRepositoryDelegate {

    override fun queryContactData(
        kind: ContactDataKind,
        searchQuery: String?,
        sortOrder: SortOrder,
        displayOrder: DisplayOrder,
        restrictToContactIds: Set<Long>?,
    ): Flow<List<ContactSummary>> {
        return observeContentUri(contentResolver, Data.CONTENT_URI)
            .map {
                val uri = buildUri(kind, searchQuery)
                val (selection, selectionArgs) = buildSelection(kind, restrictToContactIds)
                contentResolver.query(
                    uri,
                    PROJECTION,
                    selection,
                    selectionArgs,
                    sortOrder.toSortKeyColumn(),
                )?.use { cursor -> cursor.toContactSummaries(kind, displayOrder) }
                    .orEmpty()
            }
            .flowOn(ioDispatcher)
    }

    private fun buildUri(kind: ContactDataKind, searchQuery: String?): Uri {
        val filterUri = when (kind) {
            is ContactDataKind.Phone -> when {
                kind.callableOnly -> Callable.ENTERPRISE_CONTENT_FILTER_URI
                else -> Phone.ENTERPRISE_CONTENT_FILTER_URI
            }
            ContactDataKind.Email -> Email.CONTENT_FILTER_URI
            ContactDataKind.PostalAddress -> null
        }
        val builder = when {
            searchQuery != null && filterUri != null ->
                filterUri.buildUpon().appendPath(searchQuery)
            else ->
                contentUri(kind).buildUpon().appendAddressBookIndex()
        }
        return builder
            .appendDefaultDirectory()
            .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true")
            .build()
    }

    private fun contentUri(kind: ContactDataKind): Uri {
        return when (kind) {
            is ContactDataKind.Phone -> when {
                kind.callableOnly -> Callable.CONTENT_URI
                else -> Phone.CONTENT_URI
            }
            ContactDataKind.Email -> Email.CONTENT_URI
            ContactDataKind.PostalAddress -> StructuredPostal.CONTENT_URI
        }
    }

    private fun buildSelection(
        kind: ContactDataKind,
        restrictToContactIds: Set<Long>?,
    ): Pair<String?, Array<String>?> {
        val clauses = buildList {
            if (kind is ContactDataKind.Phone) {
                // Ignore invalid phone numbers that are too long. These can potentially cause
                // freezes in the UI and there is no reason to display them.
                add(IGNORE_NUMBER_TOO_LONG_CLAUSE)
            }
            if (restrictToContactIds != null) {
                val placeholders = restrictToContactIds.joinToString(",") { "?" }
                add("${Data.CONTACT_ID} IN ($placeholders)")
            }
        }
        val selection = clauses
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" AND ")
        val selectionArgs = restrictToContactIds
            ?.map { it.toString() }
            ?.toTypedArray()
        return selection to selectionArgs
    }

    private fun Cursor.toContactSummaries(
        kind: ContactDataKind,
        displayOrder: DisplayOrder,
    ): List<ContactSummary> {
        val sectionLabels = addressBookSectionLabels()
        return buildList {
            while (moveToNext()) {
                add(
                    ContactSummary(
                        contactId = getLong(ID),
                        name = getDisplayName(
                            displayOrder = displayOrder,
                            primaryIndex = DISPLAY_NAME_PRIMARY,
                            alternativeIndex = DISPLAY_NAME_ALTERNATIVE,
                        ),
                        contactPhoto = getContactPhoto(PHOTO_THUMBNAIL_URI),
                        field = getField(
                            kind = kind.toFieldKind(),
                            valueIndex = VALUE,
                            typeIndex = TYPE,
                            labelIndex = LABEL
                        ),
                        sectionLabel = sectionLabels.getOrNull(position),
                        isFavorite = getInt(STARRED) != 0,
                    ),
                )
            }
        }
    }

    private fun ContactDataKind.toFieldKind(): ContactSummary.FieldKind {
        return when (this) {
            ContactDataKind.Email -> ContactSummary.FieldKind.EMAIL
            is ContactDataKind.Phone -> ContactSummary.FieldKind.PHONE
            ContactDataKind.PostalAddress -> ContactSummary.FieldKind.POSTAL_ADDRESS
        }
    }

    companion object {
        @VisibleForTesting
        val PROJECTION = arrayOf(
            Data._ID,
            Data.DISPLAY_NAME_PRIMARY,
            Data.DISPLAY_NAME_ALTERNATIVE,
            Data.PHOTO_THUMBNAIL_URI,
            Data.STARRED,
            Data.DATA1,
            Data.DATA2,
            Data.DATA3,
        )
        private const val ID = 0
        private const val DISPLAY_NAME_PRIMARY = 1
        private const val DISPLAY_NAME_ALTERNATIVE = 2
        private const val PHOTO_THUMBNAIL_URI = 3
        private const val STARRED = 4

        // Phone.NUMBER, Email.ADDRESS and StructuredPostal.FORMATTED_ADDRESS
        private const val VALUE = 5
        private const val TYPE = 6
        private const val LABEL = 7

        @VisibleForTesting
        const val IGNORE_NUMBER_TOO_LONG_CLAUSE = "length(" + Phone.NUMBER + ") < 1000"
    }
}
