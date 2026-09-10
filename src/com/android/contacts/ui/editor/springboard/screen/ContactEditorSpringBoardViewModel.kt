package com.android.contacts.ui.editor.springboard.screen

import android.content.ContentUris
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contacts.data.contacts.repository.ContactsRepository
import com.android.contacts.domain.contacts.model.RawContactsResult
import com.android.contacts.domain.contacts.usecase.LoadRawContacts
import com.android.contacts.logging.EditorEvent
import com.android.contacts.logging.Logger
import com.android.contacts.ui.editor.springboard.screen.model.ContactEditorSpringBoardAction as Action
import com.android.contacts.ui.editor.springboard.screen.model.ContactEditorSpringBoardEffect as Effect
import com.android.contacts.ui.editor.springboard.screen.model.ContactEditorSpringBoardUiState as State
import com.android.contacts.util.core.GetUriType
import com.google.i18n.phonenumbers.NumberParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal interface ContactEditorSpringBoardScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
}

@HiltViewModel
internal class ContactEditorSpringBoardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUriType: GetUriType,
    private val loadRawContacts: LoadRawContacts,
) : ViewModel(),
    ContactEditorSpringBoardScreenModel {

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    override val effects: Flow<Effect> = _effects.receiveAsFlow()

    private val _uiState = MutableStateFlow<State>(State.Loading)
    override val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val uri: Uri? = savedStateHandle[EXTRA_URI] as? Uri
    private val showReadOnly: Boolean = savedStateHandle.get<Boolean>(EXTRA_SHOW_READ_ONLY) == true

    private var result: RawContactsResult? = null

    init {
        if (uri != null) {
            viewModelScope.launch {
                val authority = uri.authority
                val type = getUriType(uri)

                if (ContactsContract.AUTHORITY == authority &&
                    ContactsContract.RawContacts.CONTENT_ITEM_TYPE == type
                ) {
                    Logger.logEditorEvent(EditorEvent.EventType.SHOW_RAW_CONTACT_PICKER, 0)
                    emitEffect(
                        when (val rawContactId = parseContentUriId(uri)) {
                            null -> Effect.ShowErrorAndClose
                            else -> Effect.EditContact(uri, rawContactId)
                        },
                    )
                } else if (android.provider.Contacts.AUTHORITY == authority) {
                    Log.e(TAG, "Legacy Uri was passed to editor.", IllegalArgumentException())
                    emitEffect(Effect.ShowErrorAndClose)
                } else {
                    val result = loadRawContacts(
                        contactUri = uri,
                        onlyWritable = !showReadOnly,
                    ).first()
                    this@ContactEditorSpringBoardViewModel.result = result

                    if (result == null) {
                        emitEffect(Effect.ShowErrorAndClose)
                    } else {

                        val hasRawContactWithWritableAccount =
                            result.rawContacts.any { it.accountType.areContactsWritable() }

                        if (showReadOnly ||
                            result.rawContacts.isNotEmpty() && hasRawContactWithWritableAccount
                        ) {
                            uiState.value = State.ShowLinkedContacts()
                        } else {
                            emitEffect(Effect.CreateContact(uri))
                        }

                        // has writtable account
                        // if read more or more than 1 contact && writable account
                        // -> show dialog
                        // else
                        // -> load editor
                    }
                }
            }
        } else {
            emitEffect(Effect.ShowErrorAndClose)
        }
    }

    override fun onAction(action: Action) {
        when (action) {
            Action.CloseClicked -> emitEffect(Effect.Close)
            Action.AddClicked -> emitEffect(Effect.SelectContactToJoin())
            is Action.ContactClicked -> TODO()
            Action.UnlinkClicked -> TODO()
            is Action.AddContactSelected -> TODO()
        }
    }

    private fun emitEffect(effect: Effect) {
        _effects.trySend(effect)
    }

    private fun parseContentUriId(uri: Uri): Long? {
        return try {
            ContentUris.parseId(uri).takeIf { it != -1L }
        } catch (e: NumberParseException) {
            Log.w(TAG, "Could not parse ContentUri ID", e)
            null
        }
    }

    companion object {
        private const val TAG = "ContactEditorSpringBoardViewModel"
        const val EXTRA_URI = "uri"
        const val EXTRA_SHOW_READ_ONLY = "showReadOnly"
    }
}
