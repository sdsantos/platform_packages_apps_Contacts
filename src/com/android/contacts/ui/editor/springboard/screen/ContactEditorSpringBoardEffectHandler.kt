package com.android.contacts.ui.editor.springboard.screen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.android.contacts.R
import com.android.contacts.activities.ContactEditorActivity
import com.android.contacts.activities.ContactSelectionActivity
import com.android.contacts.editor.EditorIntents
import com.android.contacts.list.UiIntentActions
import com.android.contacts.ui.editor.springboard.screen.model.ContactEditorSpringBoardEffect as Effect
import com.android.contacts.util.ImplicitIntentsUtil

internal interface ContactEditorSpringBoardEffectHandler {
    fun handle(effect: Effect)
}

internal class ContactEditorSpringBoardEffectHandlerImpl(
    private val activity: Activity,
    private val contactSelectLauncher: ActivityResultLauncher<Intent>,
    private val originalExtras: Bundle,
) : ContactEditorSpringBoardEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.Close -> {
                activity.finish()
            }

            is Effect.EditContact -> {
                val intent = EditorIntents.createEditContactIntentForRawContact(
                    activity,
                    effect.uri,
                    effect.rawContactId,
                    null,
                )
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtras(originalExtras)
                ImplicitIntentsUtil.startActivityInApp(activity, intent)
                activity.finish()
            }

            is Effect.CreateContact -> {
                val intent = EditorIntents.createEditContactIntent(
                    activity,
                    effect.uri,
                    null,
                    -1,
                )
                    .setClass(activity, ContactEditorActivity::class.java)
                    .putExtras(originalExtras)
                ImplicitIntentsUtil.startActivityInApp(activity, intent)
                activity.finish()
            }

            is Effect.SelectContactToJoin -> {
                val intent = Intent(activity, ContactSelectionActivity::class.java)
                    .setAction(UiIntentActions.PICK_JOIN_CONTACT_ACTION)
                    .putExtra(UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, effect.contactId)
                contactSelectLauncher.launch(intent)
            }

            Effect.ShowErrorAndClose -> {
                Toast.makeText(
                    activity,
                    R.string.editor_failed_to_load,
                    Toast.LENGTH_SHORT,
                ).show()
                activity.setResult(Activity.RESULT_CANCELED)
                activity.finish()
            }
        }
    }
}
