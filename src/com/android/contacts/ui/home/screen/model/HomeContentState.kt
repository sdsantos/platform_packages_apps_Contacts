package com.android.contacts.ui.home.screen.model

import com.android.contacts.domain.accounts.model.AccountDisplayModel
import com.android.contacts.domain.contacts.model.ContactItem

internal sealed interface HomeContentState {

    data object Loading : HomeContentState

    data class Loaded(
        val contacts: List<ContactItem>,
        val accounts: List<AccountDisplayModel>,
        val canCreateContact: Boolean,
    ) : HomeContentState
}
