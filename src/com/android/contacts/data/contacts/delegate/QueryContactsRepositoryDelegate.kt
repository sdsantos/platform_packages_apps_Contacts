package com.android.contacts.data.contacts.delegate

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.contacts.data.contacts.delegate.util.addressBookSectionLabels
import com.android.contacts.data.contacts.delegate.util.appendAccount
import com.android.contacts.data.contacts.delegate.util.appendAddressBookIndex
import com.android.contacts.data.contacts.delegate.util.appendDefaultDirectory
import com.android.contacts.data.contacts.delegate.util.getContactPhoto
import com.android.contacts.data.contacts.delegate.util.getDisplayName
import com.android.contacts.data.contacts.delegate.util.toSortKeyColumn
import com.android.contacts.data.settings.model.DisplayOrder
import com.android.contacts.data.settings.model.SortOrder
import com.android.contacts.di.core.IoDispatcher
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.domain.contacts.model.ContactSummary
import com.android.contacts.util.core.observeContentUri
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface QueryContactsRepositoryDelegate {
    fun queryContacts(
        searchQuery: String?,
        sortOrder: SortOrder,
        displayOrder: DisplayOrder,
        account: AccountModel? = null,
        excludedContactId: Long? = null,
    ): Flow<List<ContactSummary>>
}

internal class QueryContactsRepositoryDelegateImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : QueryContactsRepositoryDelegate {

    override fun queryContacts(
        searchQuery: String?,
        sortOrder: SortOrder,
        displayOrder: DisplayOrder,
        account: AccountModel?,
        excludedContactId: Long?,
    ): Flow<List<ContactSummary>> {
        return observeContentUri(contentResolver, Contacts.CONTENT_URI)
            .map {
                query(
                    uri = buildUri(searchQuery, account),
                    sortOrder = sortOrder,
                    displayOrder = displayOrder,
                    excludedContactId = excludedContactId,
                )
            }
            .flowOn(ioDispatcher)
    }

    private fun buildUri(searchQuery: String?, account: AccountModel?): Uri {
        val builder = when (searchQuery) {
            null -> Contacts.CONTENT_URI.buildUpon().appendAddressBookIndex()
            else -> Contacts.ENTERPRISE_CONTENT_FILTER_URI.buildUpon().appendPath(searchQuery)
        }
        return builder
            .appendDefaultDirectory()
            .appendAccount(account)
            .build()
    }

    private fun query(
        uri: Uri,
        sortOrder: SortOrder,
        displayOrder: DisplayOrder,
        excludedContactId: Long?,
    ): List<ContactSummary> {
        return contentResolver.query(
            uri,
            PROJECTION,
            excludedContactId?.let { EXCLUDE_CONTACT_SELECTION },
            excludedContactId?.let { arrayOf(it.toString()) },
            sortOrder.toSortKeyColumn(),
        )?.use { it.toContactSummaries(displayOrder) }
            .orEmpty()
    }

    companion object {
        internal val PROJECTION = arrayOf(
            Contacts._ID,
            Contacts.DISPLAY_NAME_PRIMARY,
            Contacts.DISPLAY_NAME_ALTERNATIVE,
            Contacts.PHOTO_THUMBNAIL_URI,
            Contacts.STARRED,
        )
        private const val ID = 0
        private const val DISPLAY_NAME_PRIMARY = 1
        private const val DISPLAY_NAME_ALTERNATIVE = 2
        private const val PHOTO_THUMBNAIL_URI = 3
        private const val STARRED = 4

        @VisibleForTesting
        const val EXCLUDE_CONTACT_SELECTION = Contacts._ID + "!=?"

        internal fun Cursor.toContactSummaries(displayOrder: DisplayOrder): List<ContactSummary> {
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
                            sectionLabel = sectionLabels.getOrNull(position),
                            isFavorite = getInt(STARRED) != 0,
                        ),
                    )
                }
            }
        }
    }
}
