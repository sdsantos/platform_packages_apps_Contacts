package com.android.contacts.ui.home.screen.mapper

import android.content.res.Resources
import com.android.contacts.R
import com.android.contacts.domain.accounts.model.AccountDisplayModel
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.domain.accounts.model.ContactsAccountFilter
import com.android.contacts.ui.common.mapper.ContactSectionedListMapper
import com.android.contacts.ui.home.screen.model.HomeAccountUiState
import com.android.contacts.ui.home.screen.model.HomeContentState
import com.android.contacts.ui.home.screen.model.HomeState
import com.android.contacts.ui.home.screen.model.HomeUiState
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableSet

internal fun interface HomeUiStateMapper {
    fun map(
        state: HomeState,
        accountFilter: ContactsAccountFilter,
        content: HomeContentState,
    ): HomeUiState
}

internal class HomeUiStateMapperImpl @Inject constructor(
    private val resources: Resources,
    private val contactSectionedListMapper: ContactSectionedListMapper,
) : HomeUiStateMapper {

    override fun map(
        state: HomeState,
        accountFilter: ContactsAccountFilter,
        content: HomeContentState,
    ): HomeUiState {
        return when (content) {
            HomeContentState.Loading -> HomeUiState.Loading
            is HomeContentState.Loaded -> mapLoaded(state, accountFilter, content)
        }
    }

    private fun mapLoaded(
        state: HomeState,
        accountFilter: ContactsAccountFilter,
        content: HomeContentState.Loaded,
    ): HomeUiState.Content {
        val contacts = content.contacts
        val selectedKeys = state.selectedKeys intersect contacts.mapTo(mutableSetOf()) { it.key }
        val isSelecting = selectedKeys.isNotEmpty()

        return HomeUiState.Content(
            accountState = mapAccountState(accountFilter, content.accounts),
            activeFilters = state.activeFilters.toImmutableSet(),
            contacts = contactSectionedListMapper.map(
                contacts = contacts,
                selectedKeys = selectedKeys,
                showFavorites = true,
            ),
            selectionTitle = when {
                isSelecting -> resources.getQuantityString(
                    R.plurals.contactPickerSelectedTitle,
                    selectedKeys.size,
                    selectedKeys.size,
                )
                else -> null
            },
            isSelecting = isSelecting,
            isSelectAllVisible = isSelecting && selectedKeys.size < contacts.size,
            isLinkVisible = selectedKeys.size > 1,
            isCreateVisible = !isSelecting && content.canCreateContact,
            deleteConfirmationMessage = when {
                state.isDeleteConfirmationVisible && isSelecting ->
                    resources.getString(R.string.batch_delete_confirmation)
                else -> null
            },
        )
    }

    private fun mapAccountState(
        accountFilter: ContactsAccountFilter,
        accounts: List<AccountDisplayModel>,
    ): HomeAccountUiState {
        return when {
            accounts.size <= 1 -> {
                HomeAccountUiState.OneAccount(
                    account = accounts.singleOrNull()
                        ?: AccountDisplayModel(account = AccountModel.NULL, name = null),
                    isSelectable = false,
                )
            }
            accountFilter is ContactsAccountFilter.One -> {
                accounts.firstOrNull { it.account == accountFilter.account }
                    ?.let { HomeAccountUiState.OneAccount(it) }
                    ?: HomeAccountUiState.AllAccounts
            }
            else -> {
                HomeAccountUiState.AllAccounts
            }
        }
    }
}
