package com.android.contacts.ui.editor.springboard.screen.model

import android.net.Uri
import android.os.Bundle

internal sealed interface ContactEditorSpringBoardEffect {
    data object Close : ContactEditorSpringBoardEffect

    data object ShowErrorAndClose : ContactEditorSpringBoardEffect

    data class EditContact(
        val uri: Uri,
        val rawContactId: Long,
    ) : ContactEditorSpringBoardEffect

    data class CreateContact(
        val uri: Uri,
    ) : ContactEditorSpringBoardEffect

    data class SelectContactToJoin(
        val contactId: Long,
    ) : ContactEditorSpringBoardEffect
}
