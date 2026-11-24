package com.android.contacts.ui.search.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.contacts.R
import com.android.contacts.domain.contacts.model.ContactItemKey
import com.android.contacts.ui.common.components.delayedIndicator
import com.android.contacts.ui.core.CollectEvents
import com.android.contacts.ui.core.scrollingScreenPadding
import com.android.contacts.ui.search.screen.model.SearchAction as Action
import com.android.contacts.ui.search.screen.model.SearchResultUiModel
import com.android.contacts.ui.search.screen.model.SearchUiState as UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

private val ITEMS_HORIZONTAL_PADDING = 16.dp
private val ITEMS_VERTICAL_PADDING = 16.dp

@Composable
internal fun SearchScreen(
    effectHandler: SearchEffectHandler,
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = viewModel<SearchViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    CollectEvents(screenModel.effects, effectHandler::handle)

    SearchContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        modifier = modifier,
    )
}

@Composable
internal fun SearchContent(
    uiState: UiState,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transitionState = rememberSearchTransitionState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val closeWithAnimation = {
        if (!transitionState.isClosing) {
            keyboardController?.hide()
            focusManager.clearFocus()
            coroutineScope.launch {
                transitionState.collapse()
                onAction(Action.BackClick)
            }
        }
    }

    BackHandler(enabled = !transitionState.isClosing, onBack = closeWithAnimation)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            SearchTopBar(
                initialQuery = uiState.query,
                requestInitialFocus = uiState.requestInitialFocus,
                expandProgress = transitionState.progress,
                onQueryChanged = { onAction(Action.QueryChanged(it)) },
                onBackClick = closeWithAnimation,
            )
        },
        modifier = modifier,
    ) { contentPadding ->
        Box(
            Modifier
                .padding(scrollingScreenPadding(contentPadding))
                .consumeWindowInsets(contentPadding)
                .imePadding()
                .fillMaxSize()
                .graphicsLayer { alpha = transitionState.progress },
        ) {
            when {
                uiState.isEmpty -> {
                    SearchEmpty()
                }
                else -> {
                    SearchResults(
                        results = uiState.results,
                        onResultClick = { onAction(Action.ResultClick(it)) },
                        bottomPadding = contentPadding.calculateBottomPadding(),
                    )
                }
            }
            if (uiState.isSearching) {
                SearchLoading()
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: ImmutableList<SearchResultUiModel>,
    onResultClick: (ContactItemKey) -> Unit,
    bottomPadding: Dp,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = ITEMS_HORIZONTAL_PADDING,
            end = ITEMS_HORIZONTAL_PADDING,
            top = ITEMS_VERTICAL_PADDING,
            bottom = ITEMS_VERTICAL_PADDING + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(
            items = results,
            key = { _, result -> result.key.toString() },
        ) { index, result ->
            SearchResultCell(
                result = result,
                onClick = { onResultClick(result.key) },
                isFirst = index == 0,
                isLast = index == results.lastIndex,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun SearchLoading() {
    LinearProgressIndicator(
        modifier = Modifier
            .height(2.dp)
            .fillMaxWidth()
            .delayedIndicator(),
    )
}

@Composable
private fun BoxScope.SearchEmpty() {
    Text(
        text = stringResource(R.string.listFoundAllContactsZero),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(32.dp)
            .align(Alignment.Center),
    )
}
