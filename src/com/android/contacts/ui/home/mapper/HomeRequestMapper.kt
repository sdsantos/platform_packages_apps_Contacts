package com.android.contacts.ui.home.mapper

import android.app.SearchManager
import android.content.ContentResolver
import android.content.Intent
import android.provider.Contacts.People as LegacyPeople
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.Intents
import android.provider.ContactsContract.Intents.Insert
import com.android.contacts.group.GroupUtil
import com.android.contacts.list.UiIntentActions
import com.android.contacts.ui.home.model.HomeRequest
import javax.inject.Inject

internal fun interface HomeRequestMapper {
    fun map(intent: Intent): HomeRequest
}

internal class HomeRequestMapperImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : HomeRequestMapper {

    override fun map(intent: Intent): HomeRequest {
        return when (intent.action) {
            UiIntentActions.LIST_ALL_CONTACTS_ACTION ->
                HomeRequest.Internal.ShowAllContacts
            UiIntentActions.LIST_CONTACTS_WITH_PHONES_ACTION ->
                HomeRequest.Internal.ShowContactsWithPhones
            Intent.ACTION_SEARCH ->
                HomeRequest.Internal.Search(mapSearchQuery(intent))
            Intent.ACTION_VIEW ->
                mapView(intent)
            Intent.ACTION_EDIT ->
                mapEdit(intent)
            Intent.ACTION_INSERT ->
                mapInsert(intent)
            Intents.SEARCH_SUGGESTION_CLICKED ->
                intent.data
                    ?.let { HomeRequest.Redirect.ViewContact(it) }
                    ?: HomeRequest.Default
            else ->
                HomeRequest.Default
        }
    }

    private fun mapView(intent: Intent): HomeRequest {
        val data = intent.data
        return when {
            resolveType(intent) in CONTACT_LIST_TYPES -> HomeRequest.Internal.ShowAllContacts
            data == null -> HomeRequest.Default
            GroupUtil.isGroupUri(data) -> HomeRequest.Redirect.ViewGroup(data)
            else -> HomeRequest.Redirect.ViewContact(data)
        }
    }

    private fun mapEdit(intent: Intent): HomeRequest {
        return intent.data
            ?.takeIf { GroupUtil.isGroupUri(it) }
            ?.let { HomeRequest.Redirect.ViewGroup(it) }
            ?: HomeRequest.Default
    }

    private fun mapInsert(intent: Intent): HomeRequest {
        return when (intent.type) {
            Groups.CONTENT_TYPE -> HomeRequest.Redirect.CreateGroup
            else -> HomeRequest.Default
        }
    }

    private fun mapSearchQuery(intent: Intent): String? {
        return listOf(
            SearchManager.QUERY,
            Insert.PHONE,
            Insert.EMAIL,
        ).firstNotNullOfOrNull { key ->
            intent.getStringExtra(key)?.takeIf { it.isNotEmpty() }
        }
    }

    private fun resolveType(intent: Intent): String? {
        return intent.type
            ?: intent.data?.let { contentResolver.getType(it) }
    }

    private companion object {
        @Suppress("DEPRECATION")
        val CONTACT_LIST_TYPES = setOf(
            Contacts.CONTENT_TYPE,
            LegacyPeople.CONTENT_TYPE,
        )
    }
}
