package com.android.contacts.ui.home.screen.model

import android.os.Parcelable
import com.android.contacts.domain.contacts.model.ContactItemKey
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class HomeState(
    val selectedKeys: Set<ContactItemKey> = emptySet(),
    val isDeleteConfirmationVisible: Boolean = false,
    val activeFilters: Set<HomeContactsFilter> = emptySet(),
) : Parcelable
