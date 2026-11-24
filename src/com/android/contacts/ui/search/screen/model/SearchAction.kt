package com.android.contacts.ui.search.screen.model

import com.android.contacts.domain.contacts.model.ContactItemKey

internal sealed interface SearchAction {

    data class QueryChanged(
        val query: String,
    ) : SearchAction

    data object BackClick : SearchAction

    data class ResultClick(
        val key: ContactItemKey,
    ) : SearchAction
}
