package com.android.contacts.data.contacts.model

internal data class RawContactsMetadata(
    val contactId: Int,
    val isUserProfile: Boolean,
    val rawContacts: List<RawContact>,
)
