package com.android.contacts.ui.interactions.attachphoto.screen

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contacts.data.photo.model.TempPhotoUris
import com.android.contacts.data.photo.repository.ContactPhotoRepository
import com.android.contacts.domain.accounts.usecase.GetDefaultAccount
import com.android.contacts.domain.photo.model.AttachPhotoResult
import com.android.contacts.domain.photo.model.AttachPhotoTarget
import com.android.contacts.domain.photo.usecase.AttachPhotoToContact
import com.android.contacts.domain.photo.usecase.GetPhotoCropSize
import com.android.contacts.domain.photo.usecase.ResolveAttachPhotoTarget
import com.android.contacts.ui.interactions.attachphoto.AttachPhotoActivity.Companion.EXTRA_MIME_TYPE
import com.android.contacts.ui.interactions.attachphoto.AttachPhotoActivity.Companion.EXTRA_SOURCE_URI
import com.android.contacts.ui.interactions.attachphoto.screen.model.AttachPhotoAction as Action
import com.android.contacts.ui.interactions.attachphoto.screen.model.AttachPhotoEffect as Effect
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal interface AttachPhotoScreenModel {
    val effects: Flow<Effect>

    fun onAction(action: Action)
}

@HiltViewModel
internal class AttachPhotoViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val contactPhotoRepository: ContactPhotoRepository,
    private val getPhotoCropSize: GetPhotoCropSize,
    private val resolveAttachPhotoTarget: ResolveAttachPhotoTarget,
    private val attachPhotoToContact: AttachPhotoToContact,
    private val getDefaultAccount: GetDefaultAccount,
) : ViewModel(),
    AttachPhotoScreenModel {

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    override val effects: Flow<Effect> = _effects.receiveAsFlow()

    private val sourcePhotoUri: Uri? = savedStateHandle[EXTRA_SOURCE_URI]
    private val sourcePhotoMimeType: String? = savedStateHandle[EXTRA_MIME_TYPE]

    private var tempPhotoUris: TempPhotoUris?
        get() = savedStateHandle[KEY_TEMP_PHOTO_URIS]
        set(value) = savedStateHandle.set(KEY_TEMP_PHOTO_URIS, value)

    private var contactUri: Uri?
        get() = savedStateHandle[KEY_CONTACT_URI]
        set(value) = savedStateHandle.set(KEY_CONTACT_URI, value)

    private var readyPhotoUri: Uri?
        get() = savedStateHandle[KEY_PHOTO_URI]
        set(value) = savedStateHandle.set(KEY_PHOTO_URI, value)

    init {
        when {
            sourcePhotoUri == null -> {
                Log.w(TAG, "No photo to attach")
                emitEffect(Effect.Close(isSuccess = false))
            }
            tempPhotoUris == null -> {
                viewModelScope.launch {
                    tempPhotoUris = contactPhotoRepository.generateTempPhotoUris()
                    emitEffect(Effect.PickContact)
                }
            }
        }
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.ContactPicked -> onContactPicked(action.contactUri)
            Action.CropUnavailable -> onCropUnavailable()
            is Action.CropFinished -> onCropFinished(action.isSuccess)
            Action.DefaultAccountPicked -> onDefaultAccountPicked()
        }
    }

    private fun onContactPicked(pickedContactUri: Uri?) {
        val source = sourcePhotoUri
        val uris = tempPhotoUris
        if (pickedContactUri == null || source == null || uris == null) {
            emitEffect(Effect.Close(isSuccess = false))
            return
        }
        contactUri = pickedContactUri

        viewModelScope.launch {
            // Save the URI into a temporary file provider URI so that
            // we can add the FLAG_GRANT_WRITE_URI_PERMISSION flag to the eventual
            // crop intent for read-only URI's.
            if (!contactPhotoRepository.copyPhoto(source, uris.temp)) {
                emitEffect(Effect.Close(isSuccess = false))
                return@launch
            }
            emitEffect(
                Effect.CropPhoto(
                    inputUri = uris.temp,
                    outputUri = uris.cropped,
                    mimeType = sourcePhotoMimeType,
                    size = getPhotoCropSize(),
                ),
            )
        }
    }

    private fun onCropUnavailable() {
        val uris = tempPhotoUris ?: return emitEffect(Effect.Close(isSuccess = false))
        viewModelScope.launch { save(uris.temp) }
    }

    private fun onCropFinished(isSuccess: Boolean) {
        val uris = tempPhotoUris ?: return emitEffect(Effect.Close(isSuccess = false))
        viewModelScope.launch {
            contactPhotoRepository.deletePhoto(uris.temp)
            when {
                isSuccess -> {
                    save(uris.cropped)
                }
                else -> {
                    emitEffect(Effect.Close(isSuccess = false))
                    return@launch
                }
            }
        }
    }

    private fun onDefaultAccountPicked() {
        viewModelScope.launch {
            val account = getDefaultAccount() ?: run {
                Log.w(TAG, "Account selector was not successful")
                emitEffect(Effect.Close(isSuccess = false))
                return@launch
            }
            attach(AttachPhotoTarget.Ready.NewRawContact(account = account))
        }
    }

    private suspend fun save(photoUri: Uri) {
        readyPhotoUri = photoUri
        val contactUri = contactUri ?: return emitEffect(Effect.Close(isSuccess = false))
        when (val target = resolveAttachPhotoTarget(contactUri)) {
            is AttachPhotoTarget.Ready -> {
                attach(target)
            }
            AttachPhotoTarget.NeedsAccountSelection -> {
                emitEffect(Effect.PickDefaultAccount)
            }
            AttachPhotoTarget.NotFound -> {
                Log.w(TAG, "No raw contacts found for contact")
                emitEffect(Effect.Close(isSuccess = false))
            }
        }
    }

    private suspend fun attach(target: AttachPhotoTarget.Ready) {
        val contactUri = this@AttachPhotoViewModel.contactUri
        val photoUri = readyPhotoUri
        when {
            contactUri != null && photoUri != null -> {
                val result = attachPhotoToContact(contactUri, target, photoUri)
                Log.d(TAG, "Attach photo result: $result")
                emitEffect(Effect.Close(isSuccess = result == AttachPhotoResult.SUCCESS))
            }
            else -> {
                emitEffect(Effect.Close(isSuccess = false))
            }
        }
    }

    private fun emitEffect(effect: Effect) {
        _effects.trySend(effect)
    }

    private companion object {
        const val TAG = "AttachPhotoViewModel"
        const val KEY_TEMP_PHOTO_URIS = "temp_photo_uris"
        const val KEY_CONTACT_URI = "contact_uri"
        const val KEY_PHOTO_URI = "photo_uri"
    }
}
