package com.android.contacts.ui.home.screen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.android.contacts.ContactSaveService
import com.android.contacts.R
import com.android.contacts.activities.ContactEditorActivity
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.domain.accounts.model.ContactsAccountFilter
import com.android.contacts.domain.groups.model.GroupDeletion
import com.android.contacts.model.account.AccountWithDataSet
import com.android.contacts.ui.common.util.ContactShareIntents
import com.android.contacts.ui.contactdetails.ContactDetailsActivity
import com.android.contacts.ui.group.list.GroupsActivity
import com.android.contacts.ui.home.screen.model.HomeAction as Action
import com.android.contacts.ui.home.screen.model.HomeEffect as Effect
import com.android.contacts.ui.interactions.account.filter.AccountFilterActivity
import com.android.contacts.ui.search.SearchActivity
import com.android.contacts.ui.settings.SettingsActivity
import com.android.contacts.util.ImplicitIntentsUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface HomeEffectHandler {
    fun handle(effect: Effect)
}

internal class HomeEffectHandlerImpl(
    private val activity: Activity,
    private val accountFilterLauncher: ActivityResultLauncher<Intent>,
    private val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope,
    private val onAction: (Action) -> Unit,
) : HomeEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.CreateContact -> createContact(effect.account)
            is Effect.OpenAccountFilter -> openAccountFilter(effect.currentFilter)
            is Effect.OpenContact -> openContact(effect.contactUri)
            is Effect.OpenGroups -> openGroups(effect.account)
            is Effect.OpenSearch -> openSearch(effect.query, effect.requestFocus)
            Effect.OpenSettings -> openSettings()
            is Effect.ShareContacts -> shareContacts(effect.lookupKeys)
            is Effect.DeleteContacts -> deleteContacts(effect.contactIds, effect.names)
            is Effect.LinkContacts -> linkContacts(effect.contactIds)
            is Effect.ShowGroupDeleted -> showGroupDeleted(effect.deletion)
        }
    }

    private fun createContact(account: AccountModel?) {
        val intent = Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI)
            .setPackage(activity.packageName)
        account?.let {
            intent.putExtra(
                ContactEditorActivity.EXTRA_ACCOUNT_WITH_DATA_SET,
                AccountWithDataSet(it.name, it.type, it.dataSet),
            )
        }
        activity.startActivity(intent)
    }

    private fun openAccountFilter(currentFilter: ContactsAccountFilter) {
        val intent = AccountFilterActivity.buildIntent(activity, currentFilter)
        accountFilterLauncher.launch(intent)
    }

    private fun openContact(contactUri: Uri) {
        val intent = Intent(activity, ContactDetailsActivity::class.java)
            .setData(contactUri)
        activity.startActivity(intent)
    }

    private fun openGroups(account: AccountModel?) {
        val intent = GroupsActivity.buildIntent(activity, account)
        activity.startActivity(intent)
    }

    private fun openSearch(query: String?, requestFocus: Boolean) {
        activity.startActivity(SearchActivity.buildIntent(activity, query, requestFocus))
    }

    private fun openSettings() {
        val intent = Intent(activity, SettingsActivity::class.java)
        activity.startActivity(intent)
    }

    private fun shareContacts(lookupKeys: List<String>) {
        val intent = ContactShareIntents.build(activity, lookupKeys) ?: return

        try {
            ImplicitIntentsUtil.startActivityOutsideApp(activity, intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Could not share contacts", e)
            Toast.makeText(activity, R.string.share_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteContacts(contactIds: List<Long>, names: List<String>) {
        val intent = ContactSaveService.createDeleteMultipleContactsIntent(
            activity,
            contactIds.toLongArray(),
            names.toTypedArray(),
        )
        ContactSaveService.startService(activity, intent)
    }

    private fun linkContacts(contactIds: List<Long>) {
        val intent = ContactSaveService.createJoinSeveralContactsIntent(
            activity,
            contactIds.toLongArray(),
        )
        ContactSaveService.startService(activity, intent)
    }

    private fun showGroupDeleted(deletion: GroupDeletion) {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = activity.getString(R.string.groupDeletedToast),
                actionLabel = activity.getString(R.string.undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onAction(Action.UndoGroupDeletionClick(deletion))
            }
        }
    }

    private companion object {
        const val TAG = "HomeEffectHandler"
    }
}
