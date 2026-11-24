package com.android.contacts.ui.search.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.contacts.domain.contacts.model.ContactItemKey
import com.android.contacts.ui.common.components.ContactAvatar
import com.android.contacts.ui.common.components.contactAvatarColorSeed
import com.android.contacts.ui.common.components.contactAvatarLabel
import com.android.contacts.ui.common.text.highlightSearch
import com.android.contacts.ui.common.text.toAnnotatedString
import com.android.contacts.ui.core.ContactsPreviewColumn
import com.android.contacts.ui.core.itemClipShape
import com.android.contacts.ui.search.screen.model.SearchResultUiModel

private val ITEM_HORIZONTAL_PADDING = 16.dp
private val ITEM_VERTICAL_PADDING = 10.dp
private val CONTACT_AVATAR_SIZE = 36.dp

@Composable
internal fun SearchResultCell(
    result: SearchResultUiModel,
    onClick: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = itemClipShape(isFirst = isFirst, isLast = isLast),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(
                    horizontal = ITEM_HORIZONTAL_PADDING,
                    vertical = ITEM_VERTICAL_PADDING,
                ),
        ) {
            ContactAvatar(
                avatarImage = result.avatarImage,
                size = CONTACT_AVATAR_SIZE,
                fallbackLabel = contactAvatarLabel(result.name.text),
                colorSeed = contactAvatarColorSeed(result.name.text),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = result.name.toAnnotatedString(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                result.matchedData?.let { matchedData ->
                    Text(
                        text = matchedData.toAnnotatedString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SearchResultCellPreview() {
    val results = listOf(
        SearchResultUiModel(
            key = ContactItemKey.Local(1L),
            name = "Alice Smith".highlightSearch("al"),
        ),
        SearchResultUiModel(
            key = ContactItemKey.Local(2L),
            name = "Brian Jones".highlightSearch("555 12"),
            matchedData = "+1 555-1234".highlightSearch("555 12"),
        ),
        SearchResultUiModel(
            key = ContactItemKey.Local(3L),
            name = "Carl".highlightSearch("acme"),
            matchedData = "Acme Corporation".highlightSearch("acme"),
        ),
    )
    ContactsPreviewColumn {
        results.forEachIndexed { index, result ->
            SearchResultCell(
                result = result,
                onClick = {},
                isFirst = index == 0,
                isLast = index == results.lastIndex,
            )
        }
    }
}
