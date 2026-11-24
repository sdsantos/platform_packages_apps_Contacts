package com.android.contacts.ui.search.screen

import android.content.ContentUris
import android.provider.ContactsContract.Contacts
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contacts.domain.contacts.model.ContactItemKey
import com.android.contacts.domain.contacts.usecase.SearchContacts
import com.android.contacts.ui.search.SearchActivity
import com.android.contacts.ui.search.screen.mapper.SearchUiStateMapper
import com.android.contacts.ui.search.screen.model.SearchAction as Action
import com.android.contacts.ui.search.screen.model.SearchContentState as Content
import com.android.contacts.ui.search.screen.model.SearchEffect as Effect
import com.android.contacts.ui.search.screen.model.SearchState as State
import com.android.contacts.ui.search.screen.model.SearchUiState as UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch

internal interface SearchScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<UiState>
    fun onAction(action: Action)
}

@HiltViewModel
internal class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchContacts: SearchContacts,
    uiStateMapper: SearchUiStateMapper,
) : ViewModel(),
    SearchScreenModel {

    private val initialQuery =
        savedStateHandle.get<String>(SearchActivity.EXTRA_QUERY)
    private val requestInitialFocus =
        savedStateHandle.get<Boolean>(SearchActivity.EXTRA_REQUEST_FOCUS)

    private val state = savedStateHandle.getMutableStateFlow(
        key = KEY_STATE,
        initialValue = State(
            query = initialQuery.orEmpty(),
            requestInitialFocus = requestInitialFocus ?: initialQuery.isNullOrBlank(),
        ),
    )

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    override val effects: Flow<Effect> = _effects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val content: StateFlow<Content> =
        state
            .map { it.query.trim() }
            .distinctUntilChanged()
            .debounceSearch()
            .flatMapLatest(::search)
            .keepResultsWhileSearching()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = Content.Idle,
            )

    override val uiState: StateFlow<UiState> =
        combine(state, content, uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = uiStateMapper.map(state.value, Content.Idle),
            )

    override fun onAction(action: Action) {
        when (action) {
            is Action.QueryChanged -> state.update { it.copy(query = action.query) }
            Action.BackClick -> emitEffect(Effect.Close)
            is Action.ResultClick -> onResultClick(action.key)
        }
    }

    private fun onResultClick(key: ContactItemKey) {
        when (key) {
            is ContactItemKey.Local -> {
                val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, key.id)
                emitEffect(Effect.OpenContact(contactUri))
            }
            // TODO: Directory contacts aren't part of the search results yet
            is ContactItemKey.Directory -> Unit
        }
    }

    private fun emitEffect(effect: Effect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    @OptIn(FlowPreview::class)
    private fun Flow<String>.debounceSearch(): Flow<String> {
        return withIndex()
            .debounce { (index, query) ->
                when {
                    index == 0 || query.isEmpty() -> 0L
                    else -> SEARCH_DEBOUNCE_MILLIS
                }
            }
            .map { it.value }
    }

    private suspend fun search(query: String): Flow<Content> {
        return when {
            query.isEmpty() -> {
                flowOf(Content.Idle)
            }
            else -> {
                searchContacts(query)
                    .map { Content.Loaded(query = query, results = it) }
                    .onStart<Content> { emit(Content.Searching) }
            }
        }
    }

    private fun Flow<Content>.keepResultsWhileSearching(): Flow<Content> {
        return runningFold<_, Content>(Content.Idle) { previous, next ->
            when {
                next is Content.Searching && previous is Content.Loaded ->
                    previous.copy(isRefreshing = true)
                else -> next
            }
        }
    }

    companion object {
        private const val KEY_STATE = "state"

        @VisibleForTesting
        const val STATE_TIMEOUT_MILLIS = 5_000L

        @VisibleForTesting
        const val SEARCH_DEBOUNCE_MILLIS = 200L
    }
}
