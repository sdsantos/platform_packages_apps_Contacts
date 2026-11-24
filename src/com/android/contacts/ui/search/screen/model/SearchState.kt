package com.android.contacts.ui.search.screen.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class SearchState(
    val query: String = "",
    val requestInitialFocus: Boolean = true,
) : Parcelable
