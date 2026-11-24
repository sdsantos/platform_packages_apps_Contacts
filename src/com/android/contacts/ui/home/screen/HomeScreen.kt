@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.contacts.ui.home.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.contacts.R
import com.android.contacts.ui.common.components.ContactSectionedListColumn
import com.android.contacts.ui.common.components.delayedIndicator
import com.android.contacts.ui.core.CollectEvents
import com.android.contacts.ui.core.scrollingScreenPadding
import com.android.contacts.ui.home.screen.model.HomeAction as Action
import com.android.contacts.ui.home.screen.model.HomeUiState as State

private val FAB_CLEARANCE = 56.dp + 16.dp

@Composable
internal fun HomeScreen(
    effectHandler: HomeEffectHandler,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    screenModel: HomeScreenModel = viewModel<HomeViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    CollectEvents(screenModel.effects, effectHandler::handle)

    LifecycleStartEffect(screenModel) {
        screenModel.onAction(Action.ScreenStarted)
        onStopOrDispose { screenModel.onAction(Action.ScreenStopped) }
    }

    HomeContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )

    BackHandler(enabled = (uiState as? State.Content)?.isSelecting == true) {
        screenModel.onAction(Action.CancelSelectionClick)
    }
}

@Composable
internal fun HomeContent(
    uiState: State,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val content = uiState as? State.Content

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = { HomeTopBar(content, onAction) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            CreateContactButton(
                isVisible = content?.isCreateVisible == true,
                onClick = { onAction(Action.CreateClick) },
            )
        },
        modifier = modifier,
    ) { contentPadding ->
        Box(
            Modifier
                .padding(scrollingScreenPadding(contentPadding))
                .consumeWindowInsets(contentPadding)
                .fillMaxSize(),
        ) {
            HomeContentInner(content, onAction, contentPadding)
        }
    }

    content?.deleteConfirmationMessage?.let { message ->
        DeleteConfirmationDialog(
            message = message,
            onConfirm = { onAction(Action.DeleteConfirmed) },
            onDismiss = { onAction(Action.DeleteDismissed) },
        )
    }
}

@Composable
private fun HomeContentInner(
    content: State.Content?,
    onAction: (Action) -> Unit,
    contentPadding: PaddingValues,
) {
    when {
        content == null -> {
            HomeLoading()
        }
        else -> {
            val headerState = remember { CollapsingHeaderState() }
            var isFiltersExpanded by rememberSaveable { mutableStateOf(false) }
            val isEmpty = content.contacts.isEmpty()
            LaunchedEffect(content.isSelecting, isEmpty) {
                if (!content.isSelecting) {
                    headerState.show()
                }
            }
            CollapsingHeaderColumn(
                state = headerState,
                header = {
                    AnimatedVisibility(visible = !content.isSelecting) {
                        HomeFilterRow(
                            accountState = content.accountState,
                            activeFilters = content.activeFilters,
                            isFiltersExpanded = isFiltersExpanded,
                            onFiltersClick = { isFiltersExpanded = !isFiltersExpanded },
                            onAction = onAction,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    isEmpty -> {
                        HomeEmpty(
                            isCreateVisible = content.isCreateVisible,
                            onCreateClick = { onAction(Action.CreateClick) },
                        )
                    }
                    else -> {
                        val bottomClearance = when {
                            content.isCreateVisible -> FAB_CLEARANCE
                            else -> 0.dp
                        }
                        ContactSectionedListColumn(
                            contactSections = content.contacts,
                            isSelectable = content.isSelecting,
                            showAvatars = true,
                            onContactClick = { onAction(Action.ContactClick(it)) },
                            onContactLongClick = { onAction(Action.ContactLongClick(it)) },
                            contentPadding = PaddingValues(
                                bottom = contentPadding.calculateBottomPadding() + bottomClearance,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateContactButton(
    isVisible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(),
        exit = scaleOut(),
    ) {
        FloatingActionButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.action_menu_add_new_contact_button),
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    LinearProgressIndicator(
        modifier = Modifier
            .height(2.dp)
            .fillMaxWidth()
            .delayedIndicator(),
    )
}

@Composable
private fun HomeEmpty(
    isCreateVisible: Boolean,
    onCreateClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.home_empty),
            contentDescription = null,
        )

        Spacer(Modifier.size(32.dp))

        Text(
            text = stringResource(R.string.noContacts),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )

        if (isCreateVisible) {
            Spacer(Modifier.size(8.dp))

            TextButton(onClick = onCreateClick) {
                Text(
                    text = stringResource(R.string.action_menu_add_new_contact_button),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.deleteConfirmation_positive_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
