package com.android.contacts.data.accounts.repository

import com.android.contacts.di.core.IoDispatcher
import com.android.contacts.di.core.MainDispatcher
import com.android.contacts.domain.accounts.mapper.ContactsAccountFilterMapper
import com.android.contacts.domain.accounts.model.ContactsAccountFilter
import com.android.contacts.list.ContactListFilterController
import com.android.contacts.list.ContactListFilterController.ContactListFilterListener
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

internal interface AccountFilterRepository {
    fun get(): Flow<ContactsAccountFilter>

    suspend fun save(filter: ContactsAccountFilter)
}

internal class AccountFilterRepositoryImpl @Inject constructor(
    private val controller: ContactListFilterController,
    private val mapper: ContactsAccountFilterMapper,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : AccountFilterRepository {

    override fun get(): Flow<ContactsAccountFilter> {
        return callbackFlow {
            val listener = ContactListFilterListener {
                trySend(mapper.map(controller.filter))
            }
            controller.checkFilterValidity(false)
            send(mapper.map(controller.filter))
            controller.addListener(listener)
            awaitClose { controller.removeListener(listener) }
        }
            .distinctUntilChanged()
    }

    override suspend fun save(filter: ContactsAccountFilter) {
        val legacyFilter = withContext(ioDispatcher) { mapper.map(filter) }
        withContext(mainDispatcher) {
            controller.setContactListFilter(legacyFilter, true)
        }
    }
}
