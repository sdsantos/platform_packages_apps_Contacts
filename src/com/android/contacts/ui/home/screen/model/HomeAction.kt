package com.android.contacts.ui.home.screen.model

import com.android.contacts.domain.accounts.model.ContactsAccountFilter
import com.android.contacts.domain.contacts.model.ContactItemKey
import com.android.contacts.domain.groups.model.GroupDeletion
import com.android.contacts.ui.home.model.HomeRequest

internal sealed interface HomeAction {

    data class RequestReceived(
        val request: HomeRequest.Internal,
    ) : HomeAction

    data object ScreenStarted : HomeAction

    data object ScreenStopped : HomeAction

    data class UndoGroupDeletionClick(
        val deletion: GroupDeletion,
    ) : HomeAction

    data object SearchClick : HomeAction

    data object SettingsClick : HomeAction

    data object AccountsClick : HomeAction

    data class AccountFilterPicked(
        val filter: ContactsAccountFilter,
    ) : HomeAction

    data object GroupsClick : HomeAction

    data class FilterToggled(
        val filter: HomeContactsFilter,
    ) : HomeAction

    data object CreateClick : HomeAction

    data class ContactClick(
        val key: ContactItemKey,
    ) : HomeAction

    data class ContactLongClick(
        val key: ContactItemKey,
    ) : HomeAction

    data object CancelSelectionClick : HomeAction

    data object SelectAllClick : HomeAction

    data object ShareClick : HomeAction

    data object DeleteClick : HomeAction

    data object DeleteConfirmed : HomeAction

    data object DeleteDismissed : HomeAction

    data object LinkClick : HomeAction
}
