package com.android.contacts.ui.search.screen.model

import android.net.Uri

internal sealed interface SearchEffect {

    data object Close : SearchEffect

    data class OpenContact(
        val contactUri: Uri,
    ) : SearchEffect
}
