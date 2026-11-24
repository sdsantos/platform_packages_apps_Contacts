package com.android.contacts.data.contacts.delegate

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.SearchSnippets
import com.android.contacts.data.contacts.delegate.QueryContactsRepositoryDelegateImpl.Companion.PROJECTION
import com.android.contacts.data.contacts.delegate.QueryContactsRepositoryDelegateImpl.Companion.toContactColumn
import com.android.contacts.data.contacts.delegate.util.appendDefaultDirectory
import com.android.contacts.data.contacts.delegate.util.toSortKeyColumn
import com.android.contacts.data.contacts.model.ContactSearchColumn
import com.android.contacts.data.settings.model.SortOrder
import com.android.contacts.di.core.IoDispatcher
import com.android.contacts.util.core.observeContentUri
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface SearchContactsRepositoryDelegate {
    fun searchContacts(
        query: String,
        sortOrder: SortOrder,
    ): Flow<List<ContactSearchColumn>>
}

internal class SearchContactsRepositoryDelegateImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SearchContactsRepositoryDelegate {

    override fun searchContacts(
        query: String,
        sortOrder: SortOrder,
    ): Flow<List<ContactSearchColumn>> {
        return observeContentUri(contentResolver, Contacts.CONTENT_URI)
            .map {
                contentResolver.query(
                    buildUri(query),
                    SEARCH_PROJECTION,
                    null,
                    null,
                    sortOrder.toSortKeyColumn(),
                )?.use { it.toContactSearchColumns() }
                    .orEmpty()
            }
            .flowOn(ioDispatcher)
    }

    private fun buildUri(query: String): Uri {
        return Contacts.ENTERPRISE_CONTENT_FILTER_URI.buildUpon()
            .appendPath(query)
            .appendDefaultDirectory()
            .appendQueryParameter(SearchSnippets.DEFERRED_SNIPPETING_KEY, "1")
            .build()
    }

    private fun Cursor.toContactSearchColumns(): List<ContactSearchColumn> {
        val isSnippetDeferred = extras.getBoolean(ContactsContract.DEFERRED_SNIPPETING)
        return buildList {
            while (moveToNext()) {
                val snippet = getString(SNIPPET)
                add(
                    ContactSearchColumn(
                        contact = toContactColumn(),
                        snippet = when {
                            isSnippetDeferred -> snippet
                            else -> snippet?.let(::extractMarkedLine)
                        },
                        isSnippetDeferred = isSnippetDeferred,
                    ),
                )
            }
        }
    }

    private fun extractMarkedLine(snippet: String): String? {
        val start = snippet.indexOf(SNIPPET_START_MATCH)
        if (start == -1) return null
        val end = snippet.lastIndexOf(SNIPPET_END_MATCH).takeIf { it != -1 } ?: start
        val from = snippet.lastIndexOf('\n', start) + 1
        val to = snippet.indexOf('\n', end).takeIf { it != -1 } ?: snippet.length
        return snippet.substring(from, to)
            .filterNot { it == SNIPPET_START_MATCH || it == SNIPPET_END_MATCH }
    }

    private companion object {
        val SEARCH_PROJECTION = PROJECTION + SearchSnippets.SNIPPET
        val SNIPPET = PROJECTION.lastIndex + 1

        const val SNIPPET_START_MATCH = '['
        const val SNIPPET_END_MATCH = ']'
    }
}
