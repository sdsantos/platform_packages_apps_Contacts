package com.android.contacts.ui.search.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SearchUiState(
    val query: String = "",
    val requestInitialFocus: Boolean = true,
    val isSearching: Boolean = false,
    val results: ImmutableList<SearchResultUiModel> = persistentListOf(),
    val isEmpty: Boolean = false,
)
