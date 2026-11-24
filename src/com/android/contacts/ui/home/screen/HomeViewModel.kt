@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.contacts.ui.home.screen

import android.content.ContentUris
import android.provider.ContactsContract.Contacts
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contacts.data.accounts.repository.AccountFilterRepository
import com.android.contacts.data.groups.repository.GroupsRepository
import com.android.contacts.domain.accounts.model.AccountFilter
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.domain.accounts.model.ContactsAccountFilter
import com.android.contacts.domain.accounts.usecase.CanCreateContactInAccount
import com.android.contacts.domain.accounts.usecase.LoadAccounts
import com.android.contacts.domain.contacts.model.ContactDataRequirement
import com.android.contacts.domain.contacts.model.ContactItem
import com.android.contacts.domain.contacts.model.ContactItemKey
import com.android.contacts.domain.contacts.model.ContactSelectionFilter
import com.android.contacts.domain.contacts.usecase.GetContacts
import com.android.contacts.domain.groups.GroupEvents
import com.android.contacts.domain.groups.model.GroupDeletion
import com.android.contacts.ui.home.model.HomeRequest
import com.android.contacts.ui.home.screen.mapper.HomeUiStateMapper
import com.android.contacts.ui.home.screen.mapper.toContactDataRequirement
import com.android.contacts.ui.home.screen.model.HomeAction as Action
import com.android.contacts.ui.home.screen.model.HomeContactsFilter
import com.android.contacts.ui.home.screen.model.HomeContentState as Content
import com.android.contacts.ui.home.screen.model.HomeEffect as Effect
import com.android.contacts.ui.home.screen.model.HomeState as State
import com.android.contacts.ui.home.screen.model.HomeUiState as UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface HomeScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<UiState>

    fun onAction(action: Action)

    /** Returns whether the key press was handled. */
    fun onKeyDown(event: KeyEvent): Boolean
}

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getContacts: GetContacts,
    loadAccounts: LoadAccounts,
    canCreateContactInAccount: CanCreateContactInAccount,
    uiStateMapper: HomeUiStateMapper,
    groupEvents: GroupEvents,
    private val accountFilterRepository: AccountFilterRepository,
    private val groupsRepository: GroupsRepository,
) : ViewModel(),
    HomeScreenModel {

    private val state = savedStateHandle.getMutableStateFlow(KEY_STATE, State())

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    override val effects: Flow<Effect> = _effects.receiveAsFlow()

    private val isScreenStarted = MutableStateFlow(false)

    private val accountFilter: StateFlow<ContactsAccountFilter?> =
        accountFilterRepository.get()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = null,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val content: StateFlow<Content> =
        combine(
            combine(onAccount(), onRequiredData(), ::Pair)
                .flatMapLatest { (account, requiredData) ->
                    getContacts(
                        filter = ContactSelectionFilter.Contacts(
                            account = account,
                            requiredData = requiredData,
                        ),
                    )
                }
                .map { it.contacts },
            loadAccounts(AccountFilter.DRAWER_DISPLAYABLE),
            onAccount()
                .map { account -> account?.let { canCreateContactInAccount(it) } ?: true }
                .distinctUntilChanged(),
        ) { contacts, accounts, canCreateContact ->
            Content.Loaded(
                contacts = contacts,
                accounts = accounts,
                canCreateContact = canCreateContact,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = Content.Loading,
            )

    override val uiState: StateFlow<UiState> =
        combine(state, accountFilter.filterNotNull(), content, uiStateMapper::map)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    init {
        isScreenStarted
            .flatMapLatest { started ->
                when {
                    started -> groupEvents.deleted
                    else -> emptyFlow()
                }
            }
            .onEach { deletion -> emitEffect(Effect.ShowGroupDeleted(deletion)) }
            .launchIn(viewModelScope)
    }

    @Suppress("CyclomaticComplexMethod")
    override fun onAction(action: Action) {
        when (action) {
            is Action.RequestReceived -> onRequestReceived(action.request)
            Action.ScreenStarted -> isScreenStarted.value = true
            Action.ScreenStopped -> isScreenStarted.value = false
            is Action.UndoGroupDeletionClick -> undoGroupDeletion(action.deletion)
            Action.SearchClick -> emitEffect(Effect.OpenSearch())
            Action.SettingsClick -> emitEffect(Effect.OpenSettings)
            Action.AccountsClick -> emitEffect(Effect.OpenAccountFilter(currentAccountFilter()))
            is Action.AccountFilterPicked -> onAccountFilterPicked(action.filter)
            Action.GroupsClick -> emitEffect(Effect.OpenGroups(currentAccount()))
            is Action.FilterToggled -> toggleFilter(action.filter)
            Action.CreateClick -> emitEffect(Effect.CreateContact(currentAccount()))
            is Action.ContactClick -> onContactClick(action.key)
            is Action.ContactLongClick -> toggleSelection(action.key)
            Action.CancelSelectionClick -> clearSelection()
            Action.SelectAllClick -> selectAll()
            Action.ShareClick -> onShareClick()
            Action.DeleteClick -> onDeleteClick()
            Action.DeleteConfirmed -> onDeleteConfirmed()
            Action.DeleteDismissed -> onDeleteDismissed()
            Action.LinkClick -> onLinkClick()
        }
    }

    override fun onKeyDown(event: KeyEvent): Boolean {
        val state = state.value
        if (state.selectedKeys.isNotEmpty() || state.isDeleteConfirmationVisible) return false

        val isSearchShortcut = event.keyCode == KeyEvent.KEYCODE_SEARCH ||
            (event.keyCode == KeyEvent.KEYCODE_F && event.isCtrlPressed)
        val query = when {
            isSearchShortcut -> null
            else -> event.typedText() ?: return false
        }
        emitEffect(Effect.OpenSearch(query = query, requestFocus = true))
        return true
    }

    private fun KeyEvent.typedText(): String? {
        if (isCtrlPressed || isMetaPressed) return null
        return when {
            unicodeChar == 0 -> null
            // Dead keys for accents, combined with the next key typed
            unicodeChar and KeyCharacterMap.COMBINING_ACCENT != 0 -> null
            Character.isWhitespace(unicodeChar) -> null
            else -> String(Character.toChars(unicodeChar))
        }
    }

    private fun onRequestReceived(request: HomeRequest.Internal) {
        when (request) {
            HomeRequest.Internal.ShowAllContacts -> {
                onAccountFilterPicked(ContactsAccountFilter.All)
            }
            HomeRequest.Internal.ShowContactsWithPhones -> {
                onAccountFilterPicked(ContactsAccountFilter.All)
                state.update { it.copy(activeFilters = setOf(HomeContactsFilter.PHONE_CONTACTS)) }
            }
            is HomeRequest.Internal.Search -> {
                emitEffect(Effect.OpenSearch(request.query))
            }
        }
    }

    private fun undoGroupDeletion(deletion: GroupDeletion) {
        viewModelScope.launch { groupsRepository.restoreGroup(deletion) }
    }

    private fun onAccountFilterPicked(filter: ContactsAccountFilter) {
        clearSelection()
        viewModelScope.launch { accountFilterRepository.save(filter) }
    }

    private fun toggleFilter(filter: HomeContactsFilter) {
        state.update { state ->
            val activeFilters = state.activeFilters
            state.copy(
                activeFilters = when (filter) {
                    in activeFilters -> activeFilters - filter
                    else -> activeFilters + filter
                },
            )
        }
    }

    private fun onContactClick(key: ContactItemKey) {
        when {
            selectedKeys().isNotEmpty() -> {
                toggleSelection(key)
            }
            key is ContactItemKey.Local -> {
                emitEffect(
                    Effect.OpenContact(ContentUris.withAppendedId(Contacts.CONTENT_URI, key.id)),
                )
            }
        }
    }

    private fun toggleSelection(key: ContactItemKey) {
        state.update { state ->
            val selectedKeys = state.selectedKeys
            state.copy(
                selectedKeys = when (key) {
                    in selectedKeys -> selectedKeys - key
                    else -> selectedKeys + key
                },
            )
        }
    }

    private fun selectAll() {
        val contacts = loadedContacts() ?: return
        state.update { state ->
            state.copy(selectedKeys = contacts.mapTo(mutableSetOf()) { it.key })
        }
    }

    private fun clearSelection() {
        state.update { it.copy(selectedKeys = emptySet()) }
    }

    private fun onShareClick() {
        val selectedKeys = selectedKeys()
        val lookupKeys = loadedContacts()
            .orEmpty()
            .filter { it.key in selectedKeys }
            .mapNotNull { it.lookupKey }
        if (lookupKeys.isNotEmpty()) {
            emitEffect(Effect.ShareContacts(lookupKeys))
        }
        clearSelection()
    }

    private fun onDeleteClick() {
        setDeleteConfirmationVisible(true)
    }

    private fun onDeleteConfirmed() {
        val selectedKeys = selectedKeys()
        val contacts = loadedContacts()
            .orEmpty()
            .filter { it.key in selectedKeys && it.key is ContactItemKey.Local }
        if (contacts.isNotEmpty()) {
            emitEffect(
                Effect.DeleteContacts(
                    contactIds = contacts.map { (it.key as ContactItemKey.Local).id },
                    names = contacts.map { it.name },
                ),
            )
        }
        state.update { it.copy(selectedKeys = emptySet(), isDeleteConfirmationVisible = false) }
    }

    private fun onDeleteDismissed() {
        setDeleteConfirmationVisible(false)
    }

    private fun setDeleteConfirmationVisible(isVisible: Boolean) {
        state.update { it.copy(isDeleteConfirmationVisible = isVisible) }
    }

    private fun onLinkClick() {
        val contactIds = selectedKeys()
            .filterIsInstance<ContactItemKey.Local>()
            .map { it.id }
        if (contactIds.size >= MIN_CONTACTS_TO_LINK) {
            emitEffect(Effect.LinkContacts(contactIds))
        }
        clearSelection()
    }

    private fun onAccount(): Flow<AccountModel?> {
        return accountFilter
            .filterNotNull()
            .map { it.account() }
            .distinctUntilChanged()
    }

    private fun onRequiredData(): Flow<Set<ContactDataRequirement>> {
        return state
            .map { it.activeFilters }
            .distinctUntilChanged()
            .map { filters -> filters.mapTo(mutableSetOf()) { it.toContactDataRequirement() } }
    }

    private fun currentAccountFilter(): ContactsAccountFilter =
        accountFilter.value ?: ContactsAccountFilter.All

    private fun currentAccount(): AccountModel? = currentAccountFilter().account()

    private fun ContactsAccountFilter.account(): AccountModel? {
        return (this as? ContactsAccountFilter.One)?.account
    }

    private fun loadedContacts(): List<ContactItem>? {
        return (content.value as? Content.Loaded)?.contacts
    }

    private fun selectedKeys(): Set<ContactItemKey> {
        val contactKeys = loadedContacts()
            ?.mapTo(mutableSetOf()) { it.key }
            ?: return emptySet()
        return state.value.selectedKeys intersect contactKeys
    }

    private fun emitEffect(effect: Effect) {
        _effects.trySend(effect)
    }

    companion object {
        private const val KEY_STATE = "state"
        private const val MIN_CONTACTS_TO_LINK = 2

        @VisibleForTesting
        const val STATE_TIMEOUT_MILLIS = 5_000L
    }
}
