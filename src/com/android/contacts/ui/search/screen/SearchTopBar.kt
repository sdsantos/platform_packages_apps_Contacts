package com.android.contacts.ui.search.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.android.contacts.R
import com.android.contacts.ui.common.components.ContactsSearchBarDefaults
import com.android.contacts.ui.core.ContactsPreviewColumn
import kotlinx.coroutines.flow.drop

private val BAR_HEIGHT = 64.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchTopBar(
    initialQuery: String,
    requestInitialFocus: Boolean,
    expandProgress: Float,
    onQueryChanged: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldState = rememberTextFieldState(initialText = initialQuery)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var shouldRequestFocus by rememberSaveable { mutableStateOf(requestInitialFocus) }
    val barColor = MaterialTheme.colorScheme.surfaceContainer

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(barColor)
                .windowInsetsPadding(SearchBarDefaults.windowInsets)
                .padding(
                    horizontal = lerp(
                        ContactsSearchBarDefaults.HorizontalPadding,
                        0.dp,
                        expandProgress,
                    ),
                    vertical = lerp(
                        ContactsSearchBarDefaults.VerticalPadding,
                        0.dp,
                        expandProgress,
                    ),
                ),
        ) {
            Surface(
                shape = RoundedCornerShape(
                    lerp(ContactsSearchBarDefaults.Height / 2, 0.dp, expandProgress),
                ),
                color = lerp(SearchBarDefaults.colors().containerColor, barColor, expandProgress),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                SearchField(
                    textFieldState = textFieldState,
                    focusRequester = focusRequester,
                    onBackClick = onBackClick,
                    modifier = Modifier.heightIn(
                        min = lerp(ContactsSearchBarDefaults.Height, BAR_HEIGHT, expandProgress),
                    ),
                )
            }
        }
        HorizontalDivider(Modifier.alpha(expandProgress))
    }

    LaunchedEffect(Unit) {
        if (shouldRequestFocus) {
            shouldRequestFocus = false
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .drop(1)
            .collect { onQueryChanged(it) }
    }
}

@Composable
private fun SearchField(
    textFieldState: TextFieldState,
    focusRequester: FocusRequester,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )

    BasicTextField(
        state = textFieldState,
        textStyle = textStyle,
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { keyboardController?.hide() },
        decorator = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                BackButton(onClick = onBackClick)
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                ) {
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.hint_findContacts),
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
                if (textFieldState.text.isNotEmpty()) {
                    ClearButton(
                        onClick = {
                            textFieldState.clearText()
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowBack,
            contentDescription = stringResource(R.string.back_arrow_content_description),
        )
    }
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = stringResource(R.string.description_clear_search),
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchTopBarEmptyPreview() {
    ContactsPreviewColumn {
        SearchTopBar(
            initialQuery = "",
            requestInitialFocus = false,
            expandProgress = 1f,
            onQueryChanged = {},
            onBackClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchTopBarQueryPreview() {
    ContactsPreviewColumn {
        SearchTopBar(
            initialQuery = "Alice",
            requestInitialFocus = false,
            expandProgress = 1f,
            onQueryChanged = {},
            onBackClick = {},
        )
    }
}
