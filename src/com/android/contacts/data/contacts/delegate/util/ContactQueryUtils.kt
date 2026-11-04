package com.android.contacts.data.contacts.delegate.util

import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.android.contacts.data.contactdetails.model.ContactPhoto
import com.android.contacts.data.settings.model.DisplayOrder
import com.android.contacts.data.settings.model.SortOrder
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.domain.contacts.model.ContactSummary

internal fun Uri.Builder.appendAddressBookIndex(): Uri.Builder {
    return appendQueryParameter(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
}

internal fun Uri.Builder.appendDefaultDirectory(): Uri.Builder {
    return appendQueryParameter(
        ContactsContract.DIRECTORY_PARAM_KEY,
        ContactsContract.Directory.DEFAULT.toString(),
    )
}

internal fun Uri.Builder.appendAccount(account: AccountModel?): Uri.Builder {
    if (account?.name != null) {
        appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
    }
    if (account?.dataSet != null) {
        appendQueryParameter(ContactsContract.RawContacts.DATA_SET, account.dataSet)
    }
    return this
}

internal fun SortOrder.toSortKeyColumn(): String {
    return when (this) {
        SortOrder.GIVEN_NAME_FIRST -> ContactsContract.Contacts.SORT_KEY_PRIMARY
        SortOrder.FAMILY_NAME_FIRST -> ContactsContract.Contacts.SORT_KEY_ALTERNATIVE
    }
}

internal fun Cursor.addressBookSectionLabels(): List<String> {
    val titles = extras?.getStringArray(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES)
    val counts = extras?.getIntArray(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS)
    return when {
        titles == null || counts == null || titles.size != counts.size -> emptyList()
        else -> titles.flatMapIndexed { index, title -> List(counts[index]) { title } }
    }
}

internal fun Cursor.getDisplayName(
    displayOrder: DisplayOrder,
    primaryIndex: Int,
    alternativeIndex: Int,
): String {
    val displayNameIndex = when (displayOrder) {
        DisplayOrder.GIVEN_NAME_FIRST -> primaryIndex
        DisplayOrder.FAMILY_NAME_FIRST -> alternativeIndex
    }
    return getString(displayNameIndex)
        ?.takeIf { it.isNotBlank() }
        ?: getString(primaryIndex)
        ?: ""
}

internal fun Cursor.getContactPhoto(index: Int): ContactPhoto? {
    return getString(index)
        ?.takeIf { it.isNotBlank() }
        ?.let(ContactPhoto::Uri)
}

internal fun Cursor.getField(
    kind: ContactSummary.FieldKind,
    valueIndex: Int,
    typeIndex: Int,
    labelIndex: Int,
): ContactSummary.Field? {
    val value = getString(valueIndex)
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return ContactSummary.Field(
        value = value,
        kind = kind,
        type = getIntOrNull(typeIndex),
        label = getString(labelIndex),
    )
}

internal fun Cursor.getIntOrNull(index: Int): Int? {
    return if (isNull(index)) null else getInt(index)
}
