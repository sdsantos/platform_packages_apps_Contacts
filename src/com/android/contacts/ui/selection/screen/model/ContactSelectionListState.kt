package com.android.contacts.ui.selection.screen.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Immutable
internal sealed interface ContactSelectionListState {

    data object Loading : ContactSelectionListState

    data class Loaded(
        val contacts: ImmutableMap<ContactListSection, ImmutableList<ContactUiModel>> =
            persistentMapOf(),
    ) : ContactSelectionListState
}
