package com.android.contacts.ui.search.screen.model

import com.android.contacts.domain.contacts.model.ContactSearchResult

internal sealed interface SearchContentState {

    data object Idle : SearchContentState

    data object Searching : SearchContentState

    data class Loaded(
        val query: String,
        val results: List<ContactSearchResult>,
        val isRefreshing: Boolean = false,
    ) : SearchContentState
}
