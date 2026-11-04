@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.contacts.ui.selection.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.inputFieldColors
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.contacts.R
import com.android.contacts.ui.core.ContactsPreviewColumn
import com.android.contacts.ui.selection.screen.model.ContactSelectionAction as Action
import com.android.contacts.ui.selection.screen.model.ContactSelectionSearchState
import com.android.contacts.ui.selection.screen.model.ContactSelectionUiState as State

private const val ENTER_MILLIS = 220
private const val EXIT_FADE_MILLIS = 80
private const val SLIDE_DIVISOR = 4

// So both TopAppBars match
private val TopAppBarHeight = SearchBarDefaults.InputFieldHeight + 16.dp

@Composable
internal fun ContactSelectionTopAppBar(
    state: State,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state.searchState is ContactSelectionSearchState.Searching,
        transitionSpec = {
            val direction = if (targetState) 1 else -1
            val enter = fadeIn(tween(ENTER_MILLIS, delayMillis = EXIT_FADE_MILLIS)) +
                slideInHorizontally(tween(ENTER_MILLIS)) { direction * it / SLIDE_DIVISOR }
            val exit = fadeOut(tween(EXIT_FADE_MILLIS)) +
                slideOutHorizontally(tween(ENTER_MILLIS)) { -direction * it / SLIDE_DIVISOR }
            enter togetherWith exit
        },
        contentAlignment = Alignment.CenterStart,
        label = "ContactSelectionTopAppBar",
        modifier = modifier,
    ) { isSearching ->
        when {
            isSearching ->
                ContactSelectionSearchingTopAppBar(
                    query = (state.searchState as? ContactSelectionSearchState.Searching)
                        ?.query
                        .orEmpty(),
                    onClose = { onAction(Action.CloseClick) },
                    onQueryChange = { onAction(Action.SearchQueryChange(it)) },
                )
            else ->
                ContactSelectionRegularTopAppBar(
                    title = state.title,
                    showSubmitButton = state.allowMultiple,
                    showSearchButton = state.searchState is ContactSelectionSearchState.Regular,
                    onClose = { onAction(Action.CloseClick) },
                    onSearch = { onAction(Action.SearchClick) },
                    onSubmit = { onAction(Action.SubmitClick) },
                )
        }
    }
}

@Composable
private fun ContactSelectionRegularTopAppBar(
    title: String,
    showSubmitButton: Boolean,
    showSearchButton: Boolean,
    onClose: () -> Unit,
    onSearch: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel_button_content_description),
                )
            }
        },
        actions = {
            if (showSearchButton) {
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.searchHint),
                    )
                }
            }
            if (showSubmitButton) {
                SubmitButton(onSubmit)
            }
        },
        expandedHeight = TopAppBarHeight,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}

@Composable
private fun SubmitButton(onSubmit: () -> Unit) {
    Button(
        onClick = onSubmit,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Row {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.contactPickerDoneButton))
        }
    }
}

@Composable
private fun ContactSelectionSearchingTopAppBar(
    query: String,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchBarStart = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
    val textFieldState = rememberTextFieldState(initialText = query)
    val focusRequester = remember { FocusRequester() }

    TopSearchBar(
        scrollBehavior = null,
        state = searchBarStart,
        inputField = {
            SearchBarDefaults.InputField(
                modifier = Modifier.focusRequester(focusRequester),
                searchBarState = searchBarStart,
                textFieldState = textFieldState,
                onSearch = {},
                placeholder = { Text(stringResource(R.string.searchHint)) },
                leadingIcon = { SearchBackButton(onClick = onClose) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        SearchClearButton(onClear = { textFieldState.clearText() })
                    }
                },
                colors = inputFieldColors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        modifier = modifier,
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(textFieldState.text) {
        onQueryChange(textFieldState.text.toString())
    }
}

@Composable
private fun SearchBackButton(onClick: () -> Unit) {
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(stringResource(R.string.cancel_button_content_description))
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = stringResource(R.string.cancel_button_content_description),
            )
        }
    }
}

@Composable
private fun SearchClearButton(onClear: () -> Unit) {
    IconButton(onClick = onClear) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = stringResource(R.string.description_clear_search),
        )
    }
}

@PreviewLightDark
@Composable
private fun ContactSelectionTopAppBarRegularPreview() {
    ContactsPreviewColumn {
        ContactSelectionTopAppBar(
            state = State(
                title = "Choose a contact",
                searchState = ContactSelectionSearchState.Regular,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContactSelectionTopAppBarMultiplePreview() {
    ContactsPreviewColumn {
        ContactSelectionTopAppBar(
            state = State(
                title = "Choose Contacts",
                searchState = ContactSelectionSearchState.Regular,
                allowMultiple = true,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContactSelectionTopAppBarSearchingEmptyPreview() {
    ContactsPreviewColumn {
        ContactSelectionTopAppBar(
            state = State(
                title = "Choose a contact",
                searchState = ContactSelectionSearchState.Searching(""),
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContactSelectionTopAppBarSearchingPreview() {
    ContactsPreviewColumn {
        ContactSelectionTopAppBar(
            state = State(
                title = "Choose a contact",
                searchState = ContactSelectionSearchState.Searching("Alice"),
            ),
            onAction = {},
        )
    }
}
