package com.android.contacts.ui.editor.springboard.screen.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ContactEditorSpringBoardUiState {

    @Immutable
    data object Loading : ContactEditorSpringBoardUiState

    @Immutable
    sealed interface ShowDialog : ContactEditorSpringBoardUiState

    @Immutable
    data class ShowPickContactToEdit(
        val yes: Boolean,
    ) : ShowDialog

    @Immutable
    data class ShowLinkedContacts(
        val yes: Boolean,
    ) : ShowDialog
}
