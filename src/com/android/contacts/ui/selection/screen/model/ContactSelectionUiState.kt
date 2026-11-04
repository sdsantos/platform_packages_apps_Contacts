package com.android.contacts.ui.selection.screen.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Immutable
internal data class ContactSelectionUiState(
    val title: String = "",
    val description: String? = null,
    @param:StringRes val emptyMessageId: Int? = null,
    val listState: ContactSelectionListState = ContactSelectionListState.Loading,
    val searchState: ContactSelectionSearchState = ContactSelectionSearchState.Unavailable,
    val allowMultiple: Boolean = false,
    val canCreate: Boolean = false,
    val showAvatars: Boolean = true,
)
