package com.android.contacts.domain.contacts.usecase

import com.android.contacts.data.contacts.model.ContactDataKind
import com.android.contacts.data.contacts.model.LegacyContactsKind
import com.android.contacts.data.contacts.repository.ContactsRepository
import com.android.contacts.data.settings.model.DisplayOrder
import com.android.contacts.data.settings.model.SortOrder
import com.android.contacts.data.settings.repository.DisplaySettingsRepository
import com.android.contacts.domain.contacts.model.ContactSelectionFilter
import com.android.contacts.domain.contacts.model.ContactSummary
import com.android.contacts.domain.contacts.model.GetContactsForSelectionResult
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal fun interface GetContactsForSelection {
    operator fun invoke(
        filter: ContactSelectionFilter,
        searchQuery: String?,
    ): Flow<GetContactsForSelectionResult>
}

internal class GetContactsForSelectionImpl @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val displaySettingsRepository: DisplaySettingsRepository,
) : GetContactsForSelection {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(
        filter: ContactSelectionFilter,
        searchQuery: String?,
    ): Flow<GetContactsForSelectionResult> {
        return displaySettingsRepository.observeDisplaySettings()
            .map { it.sortOrder to it.displayOrder }
            .distinctUntilChanged()
            .flatMapLatest { (sortOrder, displayOrder) ->
                query(filter, searchQuery, sortOrder, displayOrder)
            }
    }

    private fun query(
        filter: ContactSelectionFilter,
        searchQuery: String?,
        sortOrder: SortOrder,
        displayOrder: DisplayOrder,
    ): Flow<GetContactsForSelectionResult> {
        // TODO: Query remote directories when filter.includesDirectories is set
        return when (filter) {
            is ContactSelectionFilter.Contacts ->
                contactsRepository.queryContacts(
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    displayOrder = displayOrder,
                    account = filter.account,
                ).excludeContacts(filter.excludeIds)
                    .toResult()

            is ContactSelectionFilter.JoinCandidates ->
                queryJoinCandidates(
                    targetContactId = filter.targetContactId,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    displayOrder = displayOrder,
                )

            is ContactSelectionFilter.PhoneNumbers ->
                contactsRepository.queryContactData(
                    kind = ContactDataKind.Phone(callableOnly = filter.callableOnly),
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    displayOrder = displayOrder,
                    restrictToContactIds = filter.restrictToContactIds,
                ).toResult()

            is ContactSelectionFilter.Emails ->
                contactsRepository.queryContactData(
                    kind = ContactDataKind.Email,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    displayOrder = displayOrder,
                    restrictToContactIds = filter.restrictToContactIds,
                ).toResult()

            ContactSelectionFilter.PostalAddresses ->
                contactsRepository.queryContactData(
                    kind = ContactDataKind.PostalAddress,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    displayOrder = displayOrder,
                ).toResult()

            ContactSelectionFilter.Legacy.People ->
                contactsRepository
                    .queryLegacyContacts(LegacyContactsKind.PEOPLE, searchQuery)
                    .toResult()

            ContactSelectionFilter.Legacy.Phones ->
                contactsRepository
                    .queryLegacyContacts(LegacyContactsKind.PHONES, searchQuery)
                    .toResult()

            ContactSelectionFilter.Legacy.PostalAddresses ->
                contactsRepository
                    .queryLegacyContacts(LegacyContactsKind.POSTAL_ADDRESSES, searchQuery)
                    .toResult()
        }
    }

    private fun queryJoinCandidates(
        targetContactId: Long,
        searchQuery: String?,
        sortOrder: SortOrder,
        displayOrder: DisplayOrder,
    ): Flow<GetContactsForSelectionResult> {
        return combine(
            contactsRepository.queryContacts(
                searchQuery = searchQuery,
                sortOrder = sortOrder,
                displayOrder = displayOrder,
                excludedContactId = targetContactId,
            ),
            contactsRepository.queryJoinSuggestions(
                targetContactId = targetContactId,
                searchQuery = searchQuery,
                displayOrder = displayOrder,
            ),
        ) { contacts, suggestions ->
            GetContactsForSelectionResult(
                contacts = contacts,
                suggestions = suggestions,
            )
        }
    }

    private fun Flow<List<ContactSummary>>.excludeContacts(
        excludeIds: List<Long>,
    ): Flow<List<ContactSummary>> {
        return map { contacts ->
            contacts.filterNot { excludeIds.contains(it.contactId) }
        }
    }

    private fun Flow<List<ContactSummary>>.toResult(): Flow<GetContactsForSelectionResult> {
        return map { contacts -> GetContactsForSelectionResult(contacts = contacts) }
    }
}
