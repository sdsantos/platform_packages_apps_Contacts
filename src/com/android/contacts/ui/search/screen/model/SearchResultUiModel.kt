package com.android.contacts.ui.search.screen.model

import androidx.compose.runtime.Immutable
import com.android.contacts.domain.contacts.model.ContactItemKey
import com.android.contacts.ui.common.components.ContactAvatarImage
import com.android.contacts.ui.common.model.HighlightedText

@Immutable
internal data class SearchResultUiModel(
    val key: ContactItemKey,
    val name: HighlightedText,
    val matchedData: HighlightedText? = null,
    val avatarImage: ContactAvatarImage? = null,
)
