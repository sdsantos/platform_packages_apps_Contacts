package com.android.contacts.domain.accounts.usecase

import com.android.contacts.di.core.IoDispatcher
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.preference.ContactsPreferences
import com.android.contacts.util.DeviceLocalAccountTypeFactory
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal fun interface CanCreateContactInAccount {
    suspend operator fun invoke(account: AccountModel): Boolean
}

internal class CanCreateContactInAccountImpl @Inject constructor(
    private val contactsPreferences: ContactsPreferences,
    private val deviceLocalAccountTypeFactory: DeviceLocalAccountTypeFactory,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CanCreateContactInAccount {
    override suspend operator fun invoke(account: AccountModel): Boolean {
        return !isLocalAccount(account) || withContext(ioDispatcher) {
            contactsPreferences.canInsertIntoLocalAccounts()
        }
    }

    private fun isLocalAccount(account: AccountModel): Boolean {
        return DeviceLocalAccountTypeFactory.Util.isLocalAccountType(
            deviceLocalAccountTypeFactory,
            account.type,
        )
    }
}
