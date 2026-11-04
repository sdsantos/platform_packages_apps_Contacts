package com.android.contacts.ui.selection.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.contacts.R
import com.android.contacts.ui.common.components.ContactCell
import com.android.contacts.ui.common.components.ContactListSectionHeader
import com.android.contacts.ui.core.ContactsPreviewColumn
import com.android.contacts.ui.selection.screen.model.ContactListSection
import com.android.contacts.ui.selection.screen.model.ContactListSection as Section
import com.android.contacts.ui.selection.screen.model.ContactUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

private val ITEMS_HORIZONTAL_PADDING = 16.dp
private val ITEMS_BOTTOM_PADDING = 32.dp

@Composable
internal fun ContactSelectionList(
    contactSections: ImmutableMap<ContactListSection, ImmutableList<ContactUiModel>>,
    canCreate: Boolean,
    allowMultiple: Boolean,
    showAvatars: Boolean,
    onCreateClick: () -> Unit,
    onContactClick: (Long) -> Unit,
    contentPadding: PaddingValues,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            start = ITEMS_HORIZONTAL_PADDING,
            end = ITEMS_HORIZONTAL_PADDING,
            bottom = ITEMS_BOTTOM_PADDING + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (canCreate) {
            item("create") {
                ContactCreateButton(onClick = onCreateClick)
            }
        }
        contactSections.forEach { (section, contacts) ->
            if (section !is Section.Undefined) {
                stickyHeader(sectionKey(section)) {
                    ContactListSectionHeader(section)
                }
            }
            itemsIndexed(
                items = contacts,
                key = { _, contact -> sectionKey(section) + "_" + contact.contactId },
            ) { index, contact ->
                ContactCell(
                    contact = contact,
                    isSelectable = allowMultiple,
                    showAvatar = showAvatars,
                    onClick = { onContactClick(contact.contactId) },
                    isFirst = index == 0,
                    isLast = index == contacts.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun ContactCreateButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonAddAlt,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.header_entry_contact_list_adapter_header_title))
        }
    }
}

private fun sectionKey(section: Section): String {
    return when (section) {
        is Section.Alphabetical -> section.letter
        Section.Favorites -> "favorites"
        Section.Undefined -> "undefined"
        Section.Suggestions -> "suggestions"
    }
}

@PreviewLightDark
@Composable
private fun ContactCreateButtonPreview() {
    ContactsPreviewColumn {
        ContactCreateButton({})
    }
}
