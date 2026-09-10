package com.android.contacts.domain.contacts.usecase

import android.net.Uri
import com.android.contacts.data.accounts.repository.AccountsRepository
import com.android.contacts.data.appinfo.repository.AppInfoRepository
import com.android.contacts.data.contacts.repository.ContactsRepository
import com.android.contacts.data.permissions.repository.PermissionsRepository
import com.android.contacts.data.settings.model.DisplaySettings
import com.android.contacts.data.settings.repository.DisplaySettingsRepository
import com.android.contacts.data.settings.repository.SettingsAvailabilityRepository
import com.android.contacts.domain.contacts.model.RawContactsResult
import com.android.contacts.domain.settings.model.SettingsData
import com.android.contacts.model.AccountTypeManager
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal fun interface LoadRawContacts {
    operator fun invoke(
        contactUri: Uri,
        onlyWritable: Boolean,
    ): Flow<RawContactsResult?>
}

internal class LoadRawContactsImpl @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val accountTypeManager: AccountTypeManager,
) : LoadRawContacts {

    override fun invoke(
        contactUri: Uri,
        onlyWritable: Boolean,
    ): Flow<RawContactsResult?> {
        val metadata = contactsRepository.loadRawContacts(contactUri)

        // trim read only
    }
}
