package com.android.contacts.ui.selection.screen.model

internal sealed interface ContactSelectionAction {

    data object CloseClick : ContactSelectionAction

    data object SearchClick : ContactSelectionAction

    data object SubmitClick : ContactSelectionAction

    data class SearchQueryChange(
        val query: String,
    ) : ContactSelectionAction

    data class ContactClick(
        val contactId: Long,
    ) : ContactSelectionAction

    data object CreateClick : ContactSelectionAction
}
