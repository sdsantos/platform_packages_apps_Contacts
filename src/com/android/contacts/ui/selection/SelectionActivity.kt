package com.android.contacts.ui.selection

import android.app.SearchManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.contacts.activities.RequestPermissionsActivity
import com.android.contacts.di.core.MainImmediateDispatcher
import com.android.contacts.list.UiIntentActions
import com.android.contacts.ui.core.AppTheme
import com.android.contacts.ui.interactions.shortcut.BuildShortcutIntent
import com.android.contacts.ui.selection.screen.ContactSelectionEffectHandlerImpl
import com.android.contacts.ui.selection.screen.ContactSelectionScreen
import com.android.contacts.ui.selection.screen.mapper.ContactSelectionModeMapper
import com.android.contacts.ui.selection.screen.mapper.ContactSelectionModeMapperImpl
import com.android.contacts.ui.selection.screen.model.ContactSelectionRequest
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

@AndroidEntryPoint
class SelectionActivity : ComponentActivity() {

    @Inject
    internal lateinit var contactSelectionModeMapper: ContactSelectionModeMapper

    @Inject
    internal lateinit var buildShortcutIntent: BuildShortcutIntent

    @Inject
    @MainImmediateDispatcher
    internal lateinit var mainImmediateDispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setHideOverlayWindows(true)
        enableEdgeToEdge()

        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return
        }

        val originalExtras = intent.extras?.deepCopy() ?: Bundle()
        val effectHandler = ContactSelectionEffectHandlerImpl(
            activity = this,
            originalExtras = originalExtras,
            buildShortcutIntent = buildShortcutIntent,
            mainDispatcher = mainImmediateDispatcher,
        )

        setContent {
            AppTheme {
                ContactSelectionScreen(
                    effectHandler = effectHandler,
                )
            }
        }
    }

    override val defaultViewModelCreationExtras: CreationExtras
        get() {
            return CreationExtras(super.defaultViewModelCreationExtras) {
                set(
                    DEFAULT_ARGS_KEY,
                    Bundle().apply { putParcelable(EXTRA_REQUEST, buildRequest()) },
                )
            }
        }

    private fun buildRequest(): ContactSelectionRequest? {
        val mode = ContactSelectionModeMapperImpl(contentResolver).map(intent) ?: return null
        return ContactSelectionRequest(
            mode = mode,
            titleOverride = intent.getStringExtra(UiIntentActions.TITLE_EXTRA_KEY),
            initialQuery = initialQuery(),
        )
    }

    private fun initialQuery(): String? {
        return listOf(
            SearchManager.QUERY,
            ContactsContract.Intents.Insert.PHONE,
            ContactsContract.Intents.Insert.EMAIL,
        ).firstNotNullOfOrNull { key ->
            intent.getStringExtra(key)
                ?.takeIf { it.isNotEmpty() }
        }
    }

    internal companion object {
        const val EXTRA_REQUEST = "request"
    }
}
