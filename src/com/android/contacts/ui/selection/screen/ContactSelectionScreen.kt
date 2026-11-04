package com.android.contacts.ui.selection.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.contacts.R
import com.android.contacts.ui.common.components.delayedIndicator
import com.android.contacts.ui.core.ContactsPreviewTheme
import com.android.contacts.ui.core.scrollingScreenPadding
import com.android.contacts.ui.selection.screen.model.ContactListSection
import com.android.contacts.ui.selection.screen.model.ContactSelectionAction as Action
import com.android.contacts.ui.selection.screen.model.ContactSelectionListState
import com.android.contacts.ui.selection.screen.model.ContactSelectionSearchState as SearchState
import com.android.contacts.ui.selection.screen.model.ContactSelectionUiState as State
import com.android.contacts.ui.selection.screen.model.ContactUiModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Composable
internal fun ContactSelectionScreen(
    effectHandler: ContactSelectionEffectHandler,
    modifier: Modifier = Modifier,
    screenModel: ContactSelectionScreenModel =
        viewModel<ContactSelectionViewModel>(),
) {
    val state by screenModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(screenModel) {
        screenModel.effects.collect(effectHandler::handle)
    }

    ContactSelectionContent(
        state = state,
        onAction = screenModel::onAction,
        modifier = modifier,
    )
}

@Composable
internal fun ContactSelectionContent(
    state: State,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ContactSelectionTopAppBar(
                state = state,
                onAction = onAction,
            )
        },
        modifier = modifier,
    ) { contentPadding ->
        Column(
            Modifier
                .padding(scrollingScreenPadding(contentPadding))
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            if (state.description != null) {
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    state.listState !is ContactSelectionListState.Loaded -> {
                        ContactSelectionLoading()
                    }
                    state.listState.contacts.isEmpty() -> {
                        ContactSelectionEmpty(state.emptyMessageId)
                    }
                    else -> {
                        ContactSelectionList(
                            contactSections = state.listState.contacts,
                            canCreate = state.canCreate,
                            allowMultiple = state.allowMultiple,
                            showAvatars = state.showAvatars,
                            onCreateClick = { onAction(Action.CreateClick) },
                            onContactClick = { onAction(Action.ContactClick(it)) },
                            contentPadding = contentPadding,
                            lazyListState = lazyListState,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(state.searchState.effectiveQuery) {
        lazyListState.scrollToItem(0)
    }
}

@Composable
private fun BoxScope.ContactSelectionLoading() {
    CircularProgressIndicator(
        modifier = Modifier
            .size(64.dp)
            .align(Alignment.Center)
            .delayedIndicator(),
    )
}

@Composable
private fun BoxScope.ContactSelectionEmpty(emptyMessageId: Int?) {
    Text(
        text = stringResource(emptyMessageId ?: return),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(32.dp)
            .align(Alignment.Center),
    )
}

@PreviewLightDark
@Composable
private fun ContactSelectionLoadingPreview() {
    ContactsPreviewTheme {
        ContactSelectionContent(
            state = State(
                title = "Choose a contact",
                emptyMessageId = R.string.noContacts,
                searchState = SearchState.Regular,
                listState = ContactSelectionListState.Loading,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContactSelectionEmptyPreview() {
    ContactsPreviewTheme {
        ContactSelectionContent(
            state = State(
                title = "Choose a contact",
                emptyMessageId = R.string.noContacts,
                searchState = SearchState.Regular,
                listState = ContactSelectionListState.Loaded(),
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContactSelectionListPreview() {
    ContactsPreviewTheme {
        ContactSelectionContent(
            state = State(
                title = "Choose Contacts",
                emptyMessageId = R.string.noContacts,
                listState = ContactSelectionListState.Loaded(
                    contacts = persistentMapOf(
                        ContactListSection.Favorites to persistentListOf(
                            ContactUiModel(contactId = 1L, name = "Alice", isSelected = true),
                        ),
                        ContactListSection.Alphabetical("B") to persistentListOf(
                            ContactUiModel(contactId = 2L, name = "Brian"),
                            ContactUiModel(contactId = 3L, name = "Bob"),
                        ),
                        ContactListSection.Alphabetical("C") to persistentListOf(
                            ContactUiModel(contactId = 4L, name = "Carl", isSelected = true),
                        ),
                    ),
                ),
                searchState = SearchState.Regular,
                allowMultiple = true,
            ),
            onAction = {},
        )
    }
}
