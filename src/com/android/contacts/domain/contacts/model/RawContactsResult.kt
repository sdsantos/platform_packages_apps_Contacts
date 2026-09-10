package com.android.contacts.domain.contacts.model

internal data class RawContactsResult(
    val contactId: Long,
    val isUserProfile: Boolean,
    val rawContacts: List<RawContactWithAccount>
)
