package com.android.contacts.ui.editor.springboard.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.contacts.ui.core.ContactsPreviewTheme
import com.android.contacts.ui.editor.springboard.screen.model.ContactEditorSpringBoardAction as Action
import com.android.contacts.ui.editor.springboard.screen.model.ContactEditorSpringBoardUiState as State

@Composable
internal fun ContactEditorSpringBoardDialog(
    effectHandler: ContactEditorSpringBoardEffectHandler,
    modifier: Modifier = Modifier,
    screenModel: ContactEditorSpringBoardScreenModel =
        viewModel<ContactEditorSpringBoardViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(screenModel) {
        screenModel.effects.collect(effectHandler::handle)
    }

    ContactEditorSpringBoardContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        modifier = modifier,
    )
}

@Composable
internal fun ContactEditorSpringBoardContent(
    uiState: State,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        State.Loading -> {}
        is State.ShowLinkedContacts -> {}
        is State.ShowPickContactToEdit -> {}
    }
}

@PreviewLightDark
@Composable
private fun ContactEditorSpringBoardScreenLoadingPreview() {
    ContactsPreviewTheme {
        ContactEditorSpringBoardContent(
            uiState = State.Loading,
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContactEditorSpringBoardDialogLoadingPreview() {
    ContactsPreviewTheme {
        ContactEditorSpringBoardContent(
            uiState = State.Loading,
            onAction = {},
        )
    }
}
