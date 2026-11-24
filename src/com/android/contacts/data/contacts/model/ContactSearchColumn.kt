package com.android.contacts.data.contacts.model

internal data class ContactSearchColumn(
    val contact: ContactColumn,
    val snippet: String? = null,
    val isSnippetDeferred: Boolean = false,
)
