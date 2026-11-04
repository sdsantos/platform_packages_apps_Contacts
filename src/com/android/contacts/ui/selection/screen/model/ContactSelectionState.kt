package com.android.contacts.ui.selection.screen.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ContactSelectionState(
    val searchState: ContactSelectionSearchState = ContactSelectionSearchState.Unavailable,
    val selectedContactIds: Set<Long> = emptySet(),
) : Parcelable
