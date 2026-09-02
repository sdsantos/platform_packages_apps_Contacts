package com.android.contacts.ui.interactions.showorcreate

import android.provider.ContactsContract.Intents
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.contacts.data.contacts.repository.ContactsRepository
import com.android.contacts.tests.MainDispatcherRule
import com.android.contacts.ui.interactions.showorcreate.screen.ShowOrCreateViewModel
import com.android.contacts.ui.interactions.showorcreate.screen.model.ShowOrCreateEffect as Effect
import com.android.contacts.ui.interactions.showorcreate.screen.model.ShowOrCreateUiState as State
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShowOrCreateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactsRepository = mockk<ContactsRepository>(relaxed = true)

    @Test
    fun whenTheSchemeIsInvalid_close() =
        runTest(mainDispatcherRule.testDispatcher) {
            val subject = createViewModel(
                savedState = mapOf(
                    ShowOrCreateViewModel.EXTRA_DATA to "invalid".toUri(),
                ),
            )
            subject.effects.test {
                advanceUntilIdle()
                assertEquals(Effect.Close, awaitItem())
            }
        }

    @Test
    fun whenTelSchemeIsProvided_lookupContactsByPhone() =
        runTest(mainDispatcherRule.testDispatcher) {
            // TODO
        }

    @Test
    fun whenMailtoSchemeIsProvided_lookupContactsByEmail() =
        runTest(mainDispatcherRule.testDispatcher) {
            // TODO
        }

    @Test
    fun whenOneContactIsFound_openIt() =
        runTest(mainDispatcherRule.testDispatcher) {
            // TODO
        }

    @Test
    fun whenMultipleContactsAreFound_openList() =
        runTest(mainDispatcherRule.testDispatcher) {
            // TODO
        }

    @Test
    fun whenNoContactIsFound_showDialog() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { contactsRepository.lookup(any()) } returns flowOf(emptyList())
            val subject = createViewModel(
                savedState = mapOf(
                    ShowOrCreateViewModel.EXTRA_DATA to "mailto:user@example.org".toUri(),
                ),
            )
            advanceUntilIdle()
            assertEquals(
                State.ConfirmingCreate::class.java,
                subject.uiState.value.javaClass,
            )
        }

    @Test
    fun whenCreateIsConfirmed_openCreateContact() =
        runTest(mainDispatcherRule.testDispatcher) {
            // TODO
        }

    @Test
    fun whenCreateIsDismissed_close() =
        runTest(mainDispatcherRule.testDispatcher) {
            // TODO
        }

    @Test
    fun whenNoContactIsFoundAndForceCreate_openCreateContact() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { contactsRepository.lookup(any()) } returns flowOf(emptyList())
            val subject = createViewModel(
                savedState = mapOf(
                    ShowOrCreateViewModel.EXTRA_DATA to "mailto:user@example.org".toUri(),
                    Intents.EXTRA_FORCE_CREATE to true,
                ),
            )
            subject.effects.test {
                advanceUntilIdle()
                val effect = awaitItem()
                assertEquals(
                    Effect.CreateContact::class.java,
                    effect.javaClass,
                )
            }
        }

    private fun createViewModel(
        savedState: Map<String, Any?>,
    ) = ShowOrCreateViewModel(
        savedStateHandle = SavedStateHandle(savedState),
        contactsRepository = contactsRepository,
    )
}
