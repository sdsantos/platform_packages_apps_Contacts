package com.android.contacts.ui.home.screen.model

import androidx.compose.runtime.Immutable
import com.android.contacts.domain.accounts.model.AccountDisplayModel

@Immutable
internal sealed interface HomeAccountUiState {

    @Immutable
    data object Loading : HomeAccountUiState

    @Immutable
    sealed interface Ready : HomeAccountUiState {
        val isSelectable: Boolean
    }

    data object AllAccounts : Ready {
        override val isSelectable = true
    }

    data class OneAccount(
        val account: AccountDisplayModel,
        override val isSelectable: Boolean = true,
    ) : Ready
}
