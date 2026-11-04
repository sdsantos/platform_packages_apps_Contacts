package com.android.contacts.ui.selection.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contacts.domain.contacts.model.GetContactsForSelectionResult
import com.android.contacts.domain.contacts.usecase.GetContact
import com.android.contacts.domain.contacts.usecase.GetContactsForSelection
import com.android.contacts.model.Contact
import com.android.contacts.ui.selection.SelectionActivity
import com.android.contacts.ui.selection.screen.mapper.ContactSelectionEffectMapper
import com.android.contacts.ui.selection.screen.mapper.ContactSelectionFilterMapper
import com.android.contacts.ui.selection.screen.mapper.ContactSelectionUiStateMapper
import com.android.contacts.ui.selection.screen.model.ContactSelectionAction as Action
import com.android.contacts.ui.selection.screen.model.ContactSelectionEffect as Effect
import com.android.contacts.ui.selection.screen.model.ContactSelectionMode
import com.android.contacts.ui.selection.screen.model.ContactSelectionMode as Mode
import com.android.contacts.ui.selection.screen.model.ContactSelectionRequest
import com.android.contacts.ui.selection.screen.model.ContactSelectionResult as Result
import com.android.contacts.ui.selection.screen.model.ContactSelectionSearchState as SearchState
import com.android.contacts.ui.selection.screen.model.ContactSelectionState as State
import com.android.contacts.ui.selection.screen.model.ContactSelectionUiState as UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface ContactSelectionScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<UiState>

    fun onAction(action: Action)
}

@HiltViewModel
internal class ContactSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getContactsForSelection: GetContactsForSelection,
    private val getContact: GetContact,
    private val filterMapper: ContactSelectionFilterMapper,
    uiStateMapper: ContactSelectionUiStateMapper,
    private val effectMapper: ContactSelectionEffectMapper,
) : ViewModel(),
    ContactSelectionScreenModel {

    private val request: ContactSelectionRequest? =
        savedStateHandle[SelectionActivity.EXTRA_REQUEST]

    private val mode: ContactSelectionMode = request?.mode ?: Mode.Pick()

    private val state = savedStateHandle.getMutableStateFlow(KEY_STATE, buildInitialState())

    private val _effects = Channel<Effect>(capacity = Channel.BUFFERED)
    override val effects: Flow<Effect> = _effects.receiveAsFlow()

    private var isSubmitting: Boolean = false

    override val uiState: StateFlow<UiState> =
        combine(
            flowOf(request).filterNotNull(),
            state,
            getContacts(),
            getJoinTarget(),
            uiStateMapper::map,
        )
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = UiState(),
            )

    init {
        if (request == null) {
            emitEffect(Effect.Close(Result.Canceled))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getContacts(): Flow<GetContactsForSelectionResult?> {
        val filter = filterMapper.map(mode)
        return state
            .map { it.searchState.effectiveQuery }
            .distinctUntilChanged()
            .flatMapLatest { searchQuery ->
                getContactsForSelection(filter, searchQuery)
                    .onStart<GetContactsForSelectionResult?> { emit(null) }
            }
    }

    private fun getJoinTarget(): Flow<Contact?> {
        val join = mode as? Mode.Join ?: return flowOf(null)
        return flow {
            emit(
                getContact(join.targetContactId)
                    .map<Contact, Contact?> { it }
                    .first(),
            )
        }
    }

    private fun buildInitialState(): State {
        return State(
            searchState = when {
                !mode.isSearchSupported -> SearchState.Unavailable
                request?.initialQuery != null -> SearchState.Searching(request.initialQuery)
                else -> SearchState.Regular
            },
            selectedContactIds = when (mode) {
                is Mode.SendToGroup -> mode.defaultSelection.toSet()
                else -> emptySet()
            },
        )
    }

    override fun onAction(action: Action) {
        when (action) {
            Action.CloseClick -> onCloseClick()
            is Action.ContactClick -> onContactClick(action.contactId)
            Action.SearchClick -> onSearchClick()
            is Action.SearchQueryChange -> onSearchQueryChange(action.query)
            Action.CreateClick -> onCreateClick()
            Action.SubmitClick -> onSubmitClick()
        }
    }

    private fun onCloseClick() {
        when (state.value.searchState) {
            is SearchState.Searching ->
                state.update { it.copy(searchState = SearchState.Regular) }
            else ->
                emitEffect(Effect.Close(Result.Canceled))
        }
    }

    private fun onContactClick(contactId: Long) {
        when {
            mode.isMultiple -> {
                state.update { state ->
                    val selectedIds = state.selectedContactIds
                    state.copy(
                        selectedContactIds = when {
                            selectedIds.contains(contactId) -> selectedIds - contactId
                            else -> selectedIds + contactId
                        },
                    )
                }
            }
            else -> {
                finishWithResult(setOf(contactId))
            }
        }
    }

    private fun onSearchClick() {
        state.update { state ->
            state.copy(searchState = SearchState.Searching(""))
        }
    }

    private fun onSearchQueryChange(newQuery: String) {
        state.update { state ->
            state.copy(searchState = SearchState.Searching(newQuery))
        }
    }

    private fun onCreateClick() {
        emitEffect(Effect.ForwardToEditor.CreateContact)
    }

    private fun onSubmitClick() {
        finishWithResult(state.value.selectedContactIds)
    }

    private fun finishWithResult(contactIds: Set<Long>) {
        if (isSubmitting) return
        isSubmitting = true
        viewModelScope.launch {
            emitEffect(effectMapper.map(mode, contactIds))
        }
    }

    private fun emitEffect(effect: Effect) {
        _effects.trySend(effect)
    }

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L
        const val KEY_STATE = "state"
    }
}
