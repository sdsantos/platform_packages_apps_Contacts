package com.android.contacts.ui.home.screen.mapper

import com.android.contacts.domain.contacts.model.ContactDataRequirement
import com.android.contacts.ui.home.screen.model.HomeContactsFilter

internal fun HomeContactsFilter.toContactDataRequirement(): ContactDataRequirement {
    return when (this) {
        HomeContactsFilter.PHONE_CONTACTS -> ContactDataRequirement.PHONE_NUMBER
        HomeContactsFilter.EMAIL_CONTACTS -> ContactDataRequirement.EMAIL
    }
}

internal fun ContactDataRequirement.toHomeFilter(): HomeContactsFilter {
    return when (this) {
        ContactDataRequirement.PHONE_NUMBER -> HomeContactsFilter.PHONE_CONTACTS
        ContactDataRequirement.EMAIL -> HomeContactsFilter.EMAIL_CONTACTS
    }
}
