package com.android.contacts.domain.contacts.model

import com.android.contacts.domain.accounts.model.AccountDisplayModel
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.model.account.AccountType

internal data class RawContactWithAccount(
    val id: Long,
    val photoId: Long?,
    val displayName: String?,
    val displayNameAlt: String?,
    val account: AccountModel,
    val accountType: AccountType,
)
