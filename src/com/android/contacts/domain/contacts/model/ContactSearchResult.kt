package com.android.contacts.domain.contacts.model

internal data class ContactSearchResult(
    val contact: ContactItem,
    val matchedData: String? = null,
)
