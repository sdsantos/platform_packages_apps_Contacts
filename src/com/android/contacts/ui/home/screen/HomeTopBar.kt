@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.contacts.ui.home.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.contacts.R
import com.android.contacts.ui.common.components.ContactsSearchBarDefaults
import com.android.contacts.ui.common.components.OverflowMenu
import com.android.contacts.ui.common.components.OverflowMenuItem
import com.android.contacts.ui.core.ContactsPreviewColumn
import com.android.contacts.ui.home.screen.model.HomeAction as Action
import com.android.contacts.ui.home.screen.model.HomeUiState as State

@Composable
internal fun HomeTopBar(
    content: State.Content?,
    onAction: (Action) -> Unit,
) {
    when {
        content?.isSelecting == true -> {
            HomeSelectionTopAppBar(
                content = content,
                onAction = onAction,
            )
        }
        else -> {
            HomeSearchBar(
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun HomeSearchBar(
    onAction: (Action) -> Unit,
) {
    Surface(
        onClick = { onAction(Action.SearchClick) },
        shape = SearchBarDefaults.inputFieldShape,
        color = SearchBarDefaults.colors().containerColor,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(SearchBarDefaults.windowInsets)
            .padding(
                horizontal = ContactsSearchBarDefaults.HorizontalPadding,
                vertical = ContactsSearchBarDefaults.VerticalPadding,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = ContactsSearchBarDefaults.Height)
                .padding(start = 16.dp, end = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.hint_findContacts),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            IconButton(onClick = { onAction(Action.SettingsClick) }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.menu_settings),
                )
            }
        }
    }
}

@Composable
private fun HomeSelectionTopAppBar(
    content: State.Content,
    onAction: (Action) -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = { onAction(Action.CancelSelectionClick) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel_button_content_description),
                )
            }
        },
        title = {
            content.selectionTitle?.let {
                Text(
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            IconButton(onClick = { onAction(Action.ShareClick) }) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.menu_share),
                )
            }
            IconButton(onClick = { onAction(Action.DeleteClick) }) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.menu_deleteContact),
                )
            }
            if (content.isLinkVisible || content.isSelectAllVisible) {
                OverflowMenu { dismiss ->
                    if (content.isLinkVisible) {
                        OverflowMenuItem(
                            labelResId = R.string.menu_joinAggregate,
                            onClick = {
                                dismiss()
                                onAction(Action.LinkClick)
                            },
                        )
                    }
                    if (content.isSelectAllVisible) {
                        OverflowMenuItem(
                            labelResId = R.string.menu_select_all,
                            onClick = {
                                dismiss()
                                onAction(Action.SelectAllClick)
                            },
                        )
                    }
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun HomeSearchBarPreview() {
    ContactsPreviewColumn {
        HomeSearchBar(onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun HomeSelectionTopAppBarPreview() {
    ContactsPreviewColumn {
        HomeSelectionTopAppBar(
            content = State.Content(
                selectionTitle = "2 selected",
                isSelecting = true,
                isSelectAllVisible = true,
                isLinkVisible = true,
            ),
            onAction = {},
        )
    }
}
