package com.android.contacts.ui.common.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class HighlightedText(
    val text: String,
    val highlights: ImmutableList<IntRange> = persistentListOf(),
)
