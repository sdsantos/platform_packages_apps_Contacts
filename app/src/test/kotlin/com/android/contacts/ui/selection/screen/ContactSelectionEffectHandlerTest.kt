package com.android.contacts.ui.selection.screen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.widget.Toast
import androidx.core.net.toUri
import com.android.contacts.activities.ContactEditorActivity
import com.android.contacts.list.UiIntentActions
import com.android.contacts.ui.editor.springboard.ContactEditorSpringBoardActivity
import com.android.contacts.ui.interactions.shortcut.BuildShortcutIntent
import com.android.contacts.ui.selection.screen.model.ContactSelectionEffect as Effect
import com.android.contacts.ui.selection.screen.model.ContactSelectionResult as Result
import com.android.contacts.ui.selection.screen.model.SentToGroupTarget
import com.android.contacts.ui.selection.screen.model.ShortcutTarget
import com.android.contacts.util.ImplicitIntentsUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ContactSelectionEffectHandlerTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val buildShortcutIntent = mockk<BuildShortcutIntent>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val intentSlot = slot<Intent>()

    @Before
    fun setUp() {
        mockkStatic(ImplicitIntentsUtil::class)
        mockkStatic(Toast::class)
        every { ImplicitIntentsUtil.startActivityInApp(any(), any()) } returns Unit
        every { Toast.makeText(any(), any<Int>(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // region Close

    @Test
    fun close_picked_setsUriResultAndFinishes() {
        val uri = "content://com.android.contacts/contacts/1".toUri()

        buildSubject().handle(Effect.Close(Result.Picked(uri)))

        val intent = captureOkResult()
        assertEquals(uri, intent.data)
        assertGrantsReadPermission(intent)
        verify { activity.finish() }
    }

    @Test
    fun close_pickedMultiple_setsClipDataResult() {
        val uris = listOf("content://data/1".toUri(), "content://data/2".toUri())

        buildSubject().handle(Effect.Close(Result.PickedMultiple(uris, Phone.CONTENT_ITEM_TYPE)))

        val intent = captureOkResult()
        val clipData = intent.clipData!!
        assertEquals(Phone.CONTENT_ITEM_TYPE, clipData.description.getMimeType(0))
        assertEquals(uris, (0 until clipData.itemCount).map { clipData.getItemAt(it).uri })
        assertGrantsReadPermission(intent)
        verify { activity.finish() }
    }

    @Test
    fun close_pickedMultiple_empty_cancels() {
        buildSubject().handle(Effect.Close(Result.PickedMultiple(emptyList(), "type")))

        verifyCanceledAndFinished()
    }

    @Test
    fun close_groupMember_setsContactIdExtra() {
        buildSubject().handle(Effect.Close(Result.GroupMember(42L)))

        val intent = captureOkResult()
        assertEquals(42L, intent.getLongExtra(UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, -1))
        verify { activity.finish() }
    }

    @Test
    fun close_groupMembers_setsContactIdsExtra() {
        buildSubject().handle(Effect.Close(Result.GroupMembers(setOf(1L, 2L))))

        val intent = captureOkResult()
        assertArrayEquals(
            longArrayOf(1L, 2L),
            intent.getLongArrayExtra(UiIntentActions.TARGET_CONTACT_IDS_EXTRA_KEY),
        )
        verify { activity.finish() }
    }

    @Test
    fun close_canceled_setsCanceledAndFinishes() {
        buildSubject().handle(Effect.Close(Result.Canceled))

        verifyCanceledAndFinished()
    }

    // endregion

    // region Shortcut

    @Test
    fun close_contactShortcut_buildsContactShortcut() {
        val uri = "content://com.android.contacts/contacts/1".toUri()
        coEvery { buildShortcutIntent.forContact(uri) } returns Intent("shortcut")

        buildSubject().handle(Effect.Close(Result.Shortcut(ShortcutTarget.CONTACT, uri)))

        coVerify { buildShortcutIntent.forContact(uri) }
    }

    @Test
    fun close_callShortcut_buildsCallShortcut() {
        val uri = "content://com.android.contacts/data/1".toUri()
        coEvery { buildShortcutIntent.forCall(uri) } returns Intent("shortcut")

        buildSubject().handle(Effect.Close(Result.Shortcut(ShortcutTarget.CALL, uri)))

        coVerify { buildShortcutIntent.forCall(uri) }
    }

    @Test
    fun close_messageShortcut_buildsMessageShortcut() {
        val uri = "content://com.android.contacts/data/1".toUri()
        coEvery { buildShortcutIntent.forMessage(uri) } returns Intent("shortcut")

        buildSubject().handle(Effect.Close(Result.Shortcut(ShortcutTarget.MESSAGE, uri)))

        coVerify { buildShortcutIntent.forMessage(uri) }
    }

    @Test
    fun close_shortcut_whilePending_doesNotFinish() {
        val uri = "content://com.android.contacts/contacts/1".toUri()
        coEvery { buildShortcutIntent.forContact(uri) } coAnswers {
            CompletableDeferred<Intent?>().await()
        }

        buildSubject().handle(Effect.Close(Result.Shortcut(ShortcutTarget.CONTACT, uri)))

        verify(exactly = 0) { activity.setResult(any()) }
        verify(exactly = 0) { activity.setResult(any(), any()) }
        verify(exactly = 0) { activity.finish() }
    }

    @Test
    fun close_shortcut_whenIntentIsCreated_setsResultAndFinishes() {
        val uri = "content://com.android.contacts/contacts/1".toUri()
        val shortcutIntent = Intent("shortcut")
        coEvery { buildShortcutIntent.forContact(uri) } returns shortcutIntent

        buildSubject().handle(Effect.Close(Result.Shortcut(ShortcutTarget.CONTACT, uri)))

        val intent = captureOkResult()
        assertEquals(shortcutIntent, intent)
        assertGrantsReadPermission(intent)
        verify { activity.finish() }
    }

    @Test
    fun close_shortcut_whenIntentIsNull_cancels() {
        val uri = "content://com.android.contacts/contacts/1".toUri()
        coEvery { buildShortcutIntent.forContact(uri) } returns null

        buildSubject().handle(Effect.Close(Result.Shortcut(ShortcutTarget.CONTACT, uri)))

        verifyCanceledAndFinished()
    }

    @Test
    fun close_shortcut_whenActivityIsFinishing_doesNotSetResult() {
        val uri = "content://com.android.contacts/contacts/1".toUri()
        coEvery { buildShortcutIntent.forContact(uri) } returns Intent("shortcut")
        every { activity.isFinishing } returns true

        buildSubject().handle(Effect.Close(Result.Shortcut(ShortcutTarget.CONTACT, uri)))

        verify(exactly = 0) { activity.setResult(any(), any()) }
        verify(exactly = 0) { activity.finish() }
    }

    @Test
    fun close_shortcut_whenActivityIsDestroyed_doesNotSetResult() {
        val uri = "content://com.android.contacts/contacts/1".toUri()
        coEvery { buildShortcutIntent.forContact(uri) } returns Intent("shortcut")
        every { activity.isDestroyed } returns true

        buildSubject().handle(Effect.Close(Result.Shortcut(ShortcutTarget.CONTACT, uri)))

        verify(exactly = 0) { activity.setResult(any(), any()) }
        verify(exactly = 0) { activity.finish() }
    }

    // endregion

    // region ForwardToEditor

    @Test
    fun createContact_forwardsInsertIntentWithCallerExtras() {
        val extras = Bundle().apply { putString("key", "value") }

        buildSubject(extras).handle(Effect.ForwardToEditor.CreateContact)

        verifyOrder {
            ImplicitIntentsUtil.startActivityInApp(activity, capture(intentSlot))
            activity.finish()
        }
        val intent = intentSlot.captured
        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals(Contacts.CONTENT_URI, intent.data)
        assertTrue(
            intent.getBooleanExtra(
                ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED,
                false,
            ),
        )
        assertEquals("value", intent.getStringExtra("key"))
        assertForwardsResult(intent)
    }

    @Test
    fun editContact_forwardsEditIntent() {
        val lookupUri = "content://com.android.contacts/contacts/lookup/key/1".toUri()

        buildSubject().handle(Effect.ForwardToEditor.EditContact(lookupUri))

        verify { ImplicitIntentsUtil.startActivityInApp(activity, capture(intentSlot)) }
        val intent = intentSlot.captured
        assertEquals(Intent.ACTION_EDIT, intent.action)
        assertEquals(lookupUri, intent.data)
        assertEquals(ContactEditorSpringBoardActivity::class.java.name, intent.component?.className)
        assertForwardsResult(intent)
        verify { activity.finish() }
    }

    @Test
    fun forwardToEditor_whenNoActivity_showsToastAndFinishes() {
        every {
            ImplicitIntentsUtil.startActivityInApp(any(), any())
        } throws ActivityNotFoundException()

        buildSubject().handle(Effect.ForwardToEditor.CreateContact)

        verify { Toast.makeText(activity, any<Int>(), Toast.LENGTH_SHORT) }
        verify { activity.finish() }
    }

    // endregion

    // region SendTo

    @Test
    fun sendTo_startsSendToChooserAndFinishes() {
        buildSubject().handle(
            Effect.SendTo(
                target = SentToGroupTarget.SMS,
                addresses = listOf("123", "456"),
                chooserTitle = "Send",
            ),
        )

        verifyOrder {
            activity.startActivity(capture(intentSlot))
            activity.finish()
        }
        val chooser = intentSlot.captured
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals("Send", chooser.getCharSequenceExtra(Intent.EXTRA_TITLE))
        @Suppress("DEPRECATION")
        val sendIntent = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SENDTO, sendIntent.action)
        assertEquals("smsto", sendIntent.data?.scheme)
        assertEquals("123,456", sendIntent.data?.schemeSpecificPart)
    }

    @Test
    fun sendTo_email_usesMailtoScheme() {
        buildSubject().handle(
            Effect.SendTo(
                target = SentToGroupTarget.EMAIL,
                addresses = listOf("a@b.c"),
                chooserTitle = null,
            ),
        )

        verify { activity.startActivity(capture(intentSlot)) }
        @Suppress("DEPRECATION")
        val sendIntent = intentSlot.captured.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("mailto", sendIntent.data?.scheme)
    }

    @Test
    fun sendTo_whenNoActivity_showsToastAndFinishes() {
        every { activity.startActivity(any()) } throws ActivityNotFoundException()

        buildSubject().handle(Effect.SendTo(SentToGroupTarget.SMS, listOf("123"), null))

        verify { Toast.makeText(activity, any<Int>(), Toast.LENGTH_SHORT) }
        verify { activity.finish() }
    }

    // endregion

    private fun buildSubject(extras: Bundle? = null) =
        ContactSelectionEffectHandlerImpl(
            activity = activity,
            originalExtras = extras,
            buildShortcutIntent = buildShortcutIntent,
            mainDispatcher = testDispatcher,
        )

    private fun captureOkResult(): Intent {
        verify { activity.setResult(Activity.RESULT_OK, capture(intentSlot)) }
        return intentSlot.captured
    }

    private fun verifyCanceledAndFinished() {
        verifyOrder {
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
    }

    private fun assertGrantsReadPermission(intent: Intent) {
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    private fun assertForwardsResult(intent: Intent) {
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_FORWARD_RESULT != 0)
    }
}
