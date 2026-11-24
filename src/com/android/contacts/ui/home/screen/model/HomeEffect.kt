package com.android.contacts.ui.home.screen.model

import android.net.Uri
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.domain.accounts.model.ContactsAccountFilter
import com.android.contacts.domain.groups.model.GroupDeletion

internal sealed interface HomeEffect {

    data class OpenSearch(
        val query: String? = null,
        val requestFocus: Boolean = query.isNullOrBlank(),
    ) : HomeEffect

    data object OpenSettings : HomeEffect

    data class OpenAccountFilter(
        val currentFilter: ContactsAccountFilter,
    ) : HomeEffect

    data class OpenGroups(
        val account: AccountModel?,
    ) : HomeEffect

    data class CreateContact(
        val account: AccountModel?,
    ) : HomeEffect

    data class OpenContact(
        val contactUri: Uri,
    ) : HomeEffect

    data class ShareContacts(
        val lookupKeys: List<String>,
    ) : HomeEffect

    data class DeleteContacts(
        val contactIds: List<Long>,
        val names: List<String>,
    ) : HomeEffect

    data class LinkContacts(
        val contactIds: List<Long>,
    ) : HomeEffect

    data class ShowGroupDeleted(
        val deletion: GroupDeletion,
    ) : HomeEffect
}
