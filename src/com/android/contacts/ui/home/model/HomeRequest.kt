package com.android.contacts.ui.home.model

import android.net.Uri

internal sealed interface HomeRequest {

    data object Default : HomeRequest

    sealed interface Internal : HomeRequest {

        data object ShowAllContacts : Internal

        data object ShowContactsWithPhones : Internal

        data class Search(
            val query: String?,
        ) : Internal
    }

    sealed interface Redirect : HomeRequest {

        data class ViewContact(
            val contactUri: Uri,
        ) : Redirect

        data object CreateGroup : Redirect

        data class ViewGroup(
            val groupUri: Uri,
        ) : Redirect
    }
}
