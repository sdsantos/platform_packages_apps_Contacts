package com.android.contacts.ui.search.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.contacts.domain.contacts.model.ContactItemKey
import com.android.contacts.ui.common.text.highlightSearch
import com.android.contacts.ui.core.ContactsPreviewTheme
import com.android.contacts.ui.search.screen.model.SearchResultUiModel
import com.android.contacts.ui.search.screen.model.SearchUiState as UiState
import kotlinx.collections.immutable.persistentListOf

private const val PREVIEW_QUERY = "al"

private val previewResults = persistentListOf(
    SearchResultUiModel(
        key = ContactItemKey.Local(1L),
        name = "Alice Smith".highlightSearch(PREVIEW_QUERY),
    ),
    SearchResultUiModel(
        key = ContactItemKey.Local(2L),
        name = "Brian Jones".highlightSearch(PREVIEW_QUERY),
        matchedData = "alpha@example.com".highlightSearch(PREVIEW_QUERY),
    ),
    SearchResultUiModel(
        key = ContactItemKey.Local(3L),
        name = "Carl Alvarez".highlightSearch(PREVIEW_QUERY),
    ),
    SearchResultUiModel(
        key = ContactItemKey.Local(4L),
        name = "Diana Prince".highlightSearch(PREVIEW_QUERY),
        matchedData = "Alpine Industries".highlightSearch(PREVIEW_QUERY),
    ),
)

@PreviewLightDark
@Composable
private fun SearchBlankPreview() {
    ContactsPreviewTheme {
        SearchContent(
            uiState = UiState(requestInitialFocus = false),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchResultsPreview() {
    ContactsPreviewTheme {
        SearchContent(
            uiState = UiState(
                query = PREVIEW_QUERY,
                requestInitialFocus = false,
                results = previewResults,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchSearchingPreview() {
    ContactsPreviewTheme {
        SearchContent(
            uiState = UiState(
                query = PREVIEW_QUERY,
                requestInitialFocus = false,
                isSearching = true,
                results = previewResults,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchEmptyPreview() {
    ContactsPreviewTheme {
        SearchContent(
            uiState = UiState(
                query = "zzz",
                requestInitialFocus = false,
                isEmpty = true,
            ),
            onAction = {},
        )
    }
}
