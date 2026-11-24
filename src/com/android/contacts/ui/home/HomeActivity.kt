package com.android.contacts.ui.home

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.lifecycleScope
import com.android.contacts.activities.RequestPermissionsActivity
import com.android.contacts.domain.accounts.model.ContactsAccountFilter
import com.android.contacts.ui.core.AppTheme
import com.android.contacts.ui.group.detail.GroupActivity
import com.android.contacts.ui.group.list.GroupsActivity
import com.android.contacts.ui.home.mapper.HomeRequestMapper
import com.android.contacts.ui.home.model.HomeRequest
import com.android.contacts.ui.home.screen.HomeEffectHandlerImpl
import com.android.contacts.ui.home.screen.HomeScreen
import com.android.contacts.ui.home.screen.HomeViewModel
import com.android.contacts.ui.home.screen.model.HomeAction
import com.android.contacts.ui.interactions.account.filter.AccountFilterActivity
import com.android.contacts.util.ImplicitIntentsUtil
import com.android.contacts.util.PermissionsUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    @Inject
    internal lateinit var requestMapper: HomeRequestMapper

    private val viewModel by viewModels<HomeViewModel>()

    private val accountFilterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onAccountFilterPicked,
    )

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* No action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return
        }

        if (savedInstanceState == null) {
            val request = requestMapper.map(intent)
            handleRequest(request)
            if (request is HomeRequest.Redirect) {
                finish()
                return
            }
            requestNotificationPermissionIfNeeded()
        }

        enableEdgeToEdge()
        val snackbarHostState = SnackbarHostState()
        val effectHandler = HomeEffectHandlerImpl(
            activity = this,
            accountFilterLauncher = accountFilterLauncher,
            snackbarHostState = snackbarHostState,
            coroutineScope = lifecycleScope,
            onAction = viewModel::onAction,
        )

        setContent {
            AppTheme {
                HomeScreen(
                    effectHandler = effectHandler,
                    snackbarHostState = snackbarHostState,
                    screenModel = viewModel,
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return viewModel.onKeyDown(event) || super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRequest(requestMapper.map(intent))
    }

    private fun handleRequest(request: HomeRequest) {
        when (request) {
            HomeRequest.Default -> Unit
            is HomeRequest.Internal -> viewModel.onAction(HomeAction.RequestReceived(request))
            is HomeRequest.Redirect -> redirect(request)
        }
    }

    private fun redirect(request: HomeRequest.Redirect) {
        when (request) {
            is HomeRequest.Redirect.ViewContact ->
                ImplicitIntentsUtil.startQuickContact(this, request.contactUri)
            HomeRequest.Redirect.CreateGroup ->
                startActivity(GroupsActivity.buildIntent(this))
            is HomeRequest.Redirect.ViewGroup ->
                startActivity(GroupActivity.buildIntent(this, request.groupUri))
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (!PermissionsUtil.hasPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun onAccountFilterPicked(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        val filter = result.data?.getParcelableExtra(
            AccountFilterActivity.EXTRA_FILTER,
            ContactsAccountFilter::class.java,
        ) ?: return
        viewModel.onAction(HomeAction.AccountFilterPicked(filter))
    }
}
