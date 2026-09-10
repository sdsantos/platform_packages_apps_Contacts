package com.android.contacts.ui.editor.springboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.android.contacts.activities.RequestPermissionsActivity
import com.android.contacts.ui.core.AppTheme
import com.android.contacts.ui.editor.springboard.screen.ContactEditorSpringBoardDialog
import com.android.contacts.ui.editor.springboard.screen.ContactEditorSpringBoardEffectHandlerImpl
import com.android.contacts.ui.editor.springboard.screen.ContactEditorSpringBoardViewModel
import com.android.contacts.ui.editor.springboard.screen.model.ContactEditorSpringBoardAction as Action
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactEditorSpringBoardComposeActivity : ComponentActivity() {

    private val viewModel by viewModels<ContactEditorSpringBoardViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return
        }

        if (Intent.ACTION_EDIT != intent.action) {
            finish()
            return
        }

        val contactSelectLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::onContactSelectResult,
        )
        val originalExtras = intent.extras?.deepCopy() ?: Bundle()
        val effectHandler = ContactEditorSpringBoardEffectHandlerImpl(
            activity = this,
            contactSelectLauncher = contactSelectLauncher,
            originalExtras = originalExtras,
        )

        intent.putExtra(ContactEditorSpringBoardViewModel.EXTRA_URI, intent.data)

        setContent {
            AppTheme {
                ContactEditorSpringBoardDialog(
                    effectHandler = effectHandler,
                )
            }
        }
    }

    private fun onContactSelectResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        result.data?.data?.let { uri ->
            viewModel.onAction(Action.AddContactSelected(uri))
        }
    }
}
