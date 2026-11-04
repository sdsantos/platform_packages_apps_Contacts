package com.android.contacts.data.contacts.delegate

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Contacts.AggregationSuggestions
import com.android.contacts.data.contacts.delegate.QueryContactsRepositoryDelegateImpl.Companion.PROJECTION
import com.android.contacts.data.contacts.delegate.QueryContactsRepositoryDelegateImpl.Companion.toContactSummaries
import com.android.contacts.data.settings.model.DisplayOrder
import com.android.contacts.di.core.IoDispatcher
import com.android.contacts.domain.contacts.model.ContactSummary
import com.android.contacts.util.core.observeContentUri
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface QueryJoinSuggestionsRepositoryDelegate {
    fun queryJoinSuggestions(
        targetContactId: Long,
        searchQuery: String?,
        displayOrder: DisplayOrder,
    ): Flow<List<ContactSummary>>
}

internal class QueryJoinSuggestionsRepositoryDelegateImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : QueryJoinSuggestionsRepositoryDelegate {

    override fun queryJoinSuggestions(
        targetContactId: Long,
        searchQuery: String?,
        displayOrder: DisplayOrder,
    ): Flow<List<ContactSummary>> {
        return observeContentUri(contentResolver, Contacts.CONTENT_URI)
            .map {
                contentResolver.query(
                    buildUri(targetContactId, searchQuery),
                    PROJECTION,
                    null,
                    null,
                    null,
                )?.use { cursor -> cursor.toContactSummaries(displayOrder) }
                    .orEmpty()
            }
            .flowOn(ioDispatcher)
    }

    private fun buildUri(targetContactId: Long, searchQuery: String?): Uri {
        return Contacts.CONTENT_URI.buildUpon()
            .appendEncodedPath(targetContactId.toString())
            .appendEncodedPath(AggregationSuggestions.CONTENT_DIRECTORY)
            .apply { searchQuery?.let(::appendPath) }
            .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, MAX_SUGGESTIONS.toString())
            .build()
    }

    private companion object {
        const val MAX_SUGGESTIONS = 4
    }
}
