package com.android.contacts.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.contacts.R
import com.android.contacts.ui.core.ContactsPreviewColumn
import com.android.contacts.ui.selection.screen.model.ContactListSection

@Composable
internal fun ContactListSectionHeader(
    section: ContactListSection,
) {
    Text(
        text = when (section) {
            ContactListSection.Suggestions ->
                stringResource(R.string.separatorJoinAggregateSuggestions)
            ContactListSection.Favorites ->
                stringResource(R.string.contactsFavoritesLabel)
            is ContactListSection.Alphabetical ->
                section.letter
            ContactListSection.Undefined ->
                ""
        },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@PreviewLightDark
@Composable
private fun ContactListSectionHeaderPreview() {
    ContactsPreviewColumn {
        ContactListSectionHeader(ContactListSection.Suggestions)
        ContactListSectionHeader(ContactListSection.Favorites)
        ContactListSectionHeader(ContactListSection.Alphabetical("A"))
        ContactListSectionHeader(ContactListSection.Alphabetical("B"))
        ContactListSectionHeader(ContactListSection.Alphabetical("#"))
    }
}
