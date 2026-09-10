package com.android.contacts.ui.editor.springboard.screen.model

import com.android.contacts.ui.simimport.screen.model.AccountUiModel

internal data class RawContactUiModel(
    val id: Long,
    val photoId: Long?,
    val displayName: String?,
    val account: AccountUiModel,
)
