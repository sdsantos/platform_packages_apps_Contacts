package com.android.contacts.ui.home.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.contacts.domain.accounts.model.AccountDisplayModel
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.domain.contacts.model.ContactItemKey
import com.android.contacts.ui.common.model.ContactListSection
import com.android.contacts.ui.common.model.ContactSectionedList
import com.android.contacts.ui.common.model.ContactUiModel
import com.android.contacts.ui.core.ContactsPreviewTheme
import com.android.contacts.ui.home.screen.model.HomeAccountUiState
import com.android.contacts.ui.home.screen.model.HomeUiState as State
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@PreviewLightDark
@Composable
private fun HomeLoadingPreview() {
    ContactsPreviewTheme {
        HomeContent(uiState = State.Loading, onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun HomeEmptyPreview() {
    ContactsPreviewTheme {
        HomeContent(
            uiState = State.Content(isCreateVisible = true),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeContentPreview() {
    ContactsPreviewTheme {
        HomeContent(
            uiState = State.Content(
                accountState = HomeAccountUiState.AllAccounts,
                contacts = previewContacts(),
                isCreateVisible = true,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeSingleAccountPreview() {
    ContactsPreviewTheme {
        HomeContent(
            uiState = State.Content(
                accountState = HomeAccountUiState.OneAccount(
                    account = AccountDisplayModel(
                        account = AccountModel(name = "user@example.com", type = "com.google"),
                        name = "user@example.com",
                    ),
                    isSelectable = false,
                ),
                contacts = previewContacts(),
                isCreateVisible = true,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeDeviceAccountPreview() {
    ContactsPreviewTheme {
        HomeContent(
            uiState = State.Content(
                accountState = HomeAccountUiState.OneAccount(
                    account = AccountDisplayModel(account = AccountModel.NULL, name = null),
                    isSelectable = false,
                ),
                contacts = previewContacts(),
                isCreateVisible = true,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeSelectionPreview() {
    ContactsPreviewTheme {
        HomeContent(
            uiState = State.Content(
                contacts = previewContacts(selectedIds = setOf(1L, 3L)),
                selectionTitle = "2 selected",
                isSelecting = true,
                isSelectAllVisible = true,
                isLinkVisible = true,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeDeleteConfirmationPreview() {
    ContactsPreviewTheme {
        HomeContent(
            uiState = State.Content(
                contacts = previewContacts(selectedIds = setOf(1L)),
                selectionTitle = "1 selected",
                isSelecting = true,
                deleteConfirmationMessage = "Delete selected contacts?",
            ),
            onAction = {},
        )
    }
}

private fun previewContacts(selectedIds: Set<Long> = emptySet()): ContactSectionedList {
    return persistentMapOf(
        ContactListSection.Alphabetical("A") to persistentListOf(
            previewContact(1L, "Alice Anderson", selectedIds),
            previewContact(2L, "Andrew Adams", selectedIds),
        ),
        ContactListSection.Alphabetical("B") to persistentListOf(
            @Suppress("MagicNumber")
            previewContact(3L, "Bob Brown", selectedIds),
        ),
    )
}

private fun previewContact(id: Long, name: String, selectedIds: Set<Long>): ContactUiModel {
    return ContactUiModel(
        key = ContactItemKey.Local(id),
        name = name,
        isSelected = id in selectedIds,
    )
}
