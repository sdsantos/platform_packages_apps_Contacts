package com.android.contacts.domain.contacts.usecase

import com.android.contacts.data.contacts.repository.ContactsRepository
import com.android.contacts.data.settings.repository.DisplaySettingsRepository
import com.android.contacts.di.core.DefaultDispatcher
import com.android.contacts.domain.contacts.mapper.ContactItemMapper
import com.android.contacts.domain.contacts.mapper.ContactSnippetMapper
import com.android.contacts.domain.contacts.model.ContactSearchResult
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Searches the local contacts, across all accounts. */
internal fun interface SearchContacts {
    operator fun invoke(query: String): Flow<List<ContactSearchResult>>
}

internal class SearchContactsImpl @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val displaySettingsRepository: DisplaySettingsRepository,
    private val contactItemMapper: ContactItemMapper,
    private val contactSnippetMapper: ContactSnippetMapper,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : SearchContacts {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(query: String): Flow<List<ContactSearchResult>> {
        return displaySettingsRepository.observeDisplaySettings()
            .map { it.sortOrder to it.displayOrder }
            .distinctUntilChanged()
            .flatMapLatest { (sortOrder, displayOrder) ->
                contactsRepository.searchContacts(
                    query = query,
                    sortOrder = sortOrder,
                ).map { columns ->
                    columns.map { column ->
                        val contact = contactItemMapper.map(column.contact, displayOrder)
                        ContactSearchResult(
                            contact = contact,
                            matchedData = contactSnippetMapper.map(
                                snippet = column.snippet,
                                isSnippetDeferred = column.isSnippetDeferred,
                                query = query,
                                displayName = contact.name,
                            ),
                        )
                    }
                }
            }
            .flowOn(defaultDispatcher)
    }
}
