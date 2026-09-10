package com.android.contacts.ui.editor.springboard.screen.mapper

import com.android.contacts.domain.contacts.model.RawContactWithAccount
import com.android.contacts.ui.editor.springboard.screen.model.RawContactUiModel
import javax.inject.Inject

internal fun interface RawContactUiModelMapper {
    fun map(rawContact: RawContactWithAccount): RawContactUiModel
}

internal class RawContactUiModelMapperImpl @Inject constructor() : RawContactUiModelMapper {
    override fun map(rawContact: RawContactWithAccount): RawContactUiModel {
        return RawContactUiModel(
            account = accountDisplayModel.account,
            name = accountDisplayModel.name,
            type = accountDisplayModel.type,
            iconData = accountDisplayModel.iconData,
        )
    }
}
