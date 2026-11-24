package com.android.contacts.ui.home.screen.model

import androidx.compose.runtime.Immutable
import com.android.contacts.ui.common.model.ContactSectionedList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Content(
        val accountState: HomeAccountUiState = HomeAccountUiState.Loading,
        val activeFilters: ImmutableSet<HomeContactsFilter> = persistentSetOf(),
        val contacts: ContactSectionedList = persistentMapOf(),
        val selectionTitle: String? = null,
        val isSelecting: Boolean = false,
        val isSelectAllVisible: Boolean = false,
        val isLinkVisible: Boolean = false,
        val isCreateVisible: Boolean = false,
        val deleteConfirmationMessage: String? = null,
    ) : HomeUiState
}
