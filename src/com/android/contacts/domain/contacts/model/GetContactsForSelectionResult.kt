package com.android.contacts.domain.contacts.model

internal data class GetContactsForSelectionResult(
    val contacts: List<ContactSummary> = emptyList(),
    val suggestions: List<ContactSummary> = emptyList(),
)
