package com.android.contacts.ui.selection.screen.mapper

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.Contacts.ContactMethods as LegacyContactMethods
import android.provider.Contacts.People as LegacyPeople
import android.provider.Contacts.Phones as LegacyPhones
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Groups
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.list.UiIntentActions
import com.android.contacts.ui.selection.screen.model.ContactSelectionMode
import com.android.contacts.ui.selection.screen.model.SentToGroupTarget
import com.android.contacts.ui.selection.screen.model.ShortcutTarget
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Suppress("DEPRECATION")
internal class ContactSelectionModeMapperTest {

    private val contentResolver: ContentResolver = mockk(relaxed = true)

    private val mapper: ContactSelectionModeMapper = ContactSelectionModeMapperImpl(
        contentResolver = contentResolver,
    )

    // region ACTION_PICK

    @Test
    fun pick_contacts_mapsToSinglePick() {
        assertEquals(ContactSelectionMode.Pick(), mapper.map(pick(Contacts.CONTENT_TYPE)))
    }

    @Test
    fun pick_contacts_withAllowMultiple_mapsToMultiplePick() {
        assertEquals(
            ContactSelectionMode.Pick(multiple = true),
            mapper.map(pick(Contacts.CONTENT_TYPE, multiple = true)),
        )
    }

    @Test
    fun pick_legacyPeople_mapsToLegacyPick() {
        assertEquals(
            ContactSelectionMode.Pick(legacy = true),
            mapper.map(pick(LegacyPeople.CONTENT_TYPE)),
        )
    }

    @Test
    fun pick_legacyPeople_ignoresAllowMultiple() {
        assertEquals(
            ContactSelectionMode.Pick(legacy = true),
            mapper.map(pick(LegacyPeople.CONTENT_TYPE, multiple = true)),
        )
    }

    @Test
    fun pick_phones_mapsToPhoneNumberPick() {
        assertEquals(
            ContactSelectionMode.PickPhoneNumber(),
            mapper.map(pick(Phone.CONTENT_TYPE)),
        )
    }

    @Test
    fun pick_phones_withAllowMultiple_mapsToMultiplePhoneNumberPick() {
        assertEquals(
            ContactSelectionMode.PickPhoneNumber(multiple = true),
            mapper.map(pick(Phone.CONTENT_TYPE, multiple = true)),
        )
    }

    @Test
    fun pick_legacyPhones_mapsToLegacyPhoneNumberPick() {
        assertEquals(
            ContactSelectionMode.PickPhoneNumber(legacy = true),
            mapper.map(pick(LegacyPhones.CONTENT_TYPE)),
        )
    }

    @Test
    fun pick_emails_mapsToEmailPick() {
        assertEquals(ContactSelectionMode.PickEmail(), mapper.map(pick(Email.CONTENT_TYPE)))
    }

    @Test
    fun pick_emails_withAllowMultiple_mapsToMultipleEmailPick() {
        assertEquals(
            ContactSelectionMode.PickEmail(multiple = true),
            mapper.map(pick(Email.CONTENT_TYPE, multiple = true)),
        )
    }

    @Test
    fun pick_postal_mapsToPostalPick() {
        assertEquals(
            ContactSelectionMode.PickPostal(),
            mapper.map(pick(StructuredPostal.CONTENT_TYPE)),
        )
    }

    @Test
    fun pick_legacyPostal_mapsToLegacyPostalPick() {
        assertEquals(
            ContactSelectionMode.PickPostal(legacy = true),
            mapper.map(pick(LegacyContactMethods.CONTENT_POSTAL_TYPE)),
        )
    }

    @Test
    fun pick_groups_mapsToAddGroupMembers() {
        val intent = pick(Groups.CONTENT_TYPE)
            .putExtra(UiIntentActions.GROUP_ACCOUNT_NAME, "name")
            .putExtra(UiIntentActions.GROUP_ACCOUNT_TYPE, "type")
            .putExtra(UiIntentActions.GROUP_ACCOUNT_DATA_SET, "dataSet")
            .putStringArrayListExtra(UiIntentActions.GROUP_CONTACT_IDS, arrayListOf("1", "2"))

        assertEquals(
            ContactSelectionMode.AddGroupMembers(
                account = AccountModel(name = "name", type = "type", dataSet = "dataSet"),
                existingMemberIds = setOf(1, 2),
            ),
            mapper.map(intent),
        )
    }

    @Test
    fun pick_groups_withoutExtras_mapsToNullAccountAndNoMembers() {
        assertEquals(
            ContactSelectionMode.AddGroupMembers(
                account = AccountModel.NULL,
                existingMemberIds = emptySet(),
            ),
            mapper.map(pick(Groups.CONTENT_TYPE)),
        )
    }

    @Test
    fun pick_withoutType_resolvesTypeFromData() {
        val uri = Uri.parse("content://com.android.contacts/data/phones")
        every { contentResolver.getType(uri) } returns Phone.CONTENT_TYPE

        assertEquals(
            ContactSelectionMode.PickPhoneNumber(),
            mapper.map(Intent(Intent.ACTION_PICK, uri)),
        )
    }

    @Test
    fun pick_withoutTypeOrData_mapsToNull() {
        assertNull(mapper.map(Intent(Intent.ACTION_PICK)))
    }

    @Test
    fun pick_unknownType_mapsToNull() {
        assertNull(mapper.map(pick("vnd.android.cursor.dir/unknown")))
    }

    // endregion

    // region ACTION_GET_CONTENT

    @Test
    fun getContent_contact_mapsToSinglePick() {
        assertEquals(
            ContactSelectionMode.Pick(),
            mapper.map(getContent(Contacts.CONTENT_ITEM_TYPE)),
        )
    }

    @Test
    fun getContent_legacyPerson_mapsToLegacyPick() {
        assertEquals(
            ContactSelectionMode.Pick(legacy = true),
            mapper.map(getContent(LegacyPeople.CONTENT_ITEM_TYPE)),
        )
    }

    @Test
    fun getContent_phone_mapsToPhoneNumberPick() {
        assertEquals(
            ContactSelectionMode.PickPhoneNumber(),
            mapper.map(getContent(Phone.CONTENT_ITEM_TYPE)),
        )
    }

    @Test
    fun getContent_legacyPhone_mapsToLegacyPhoneNumberPick() {
        assertEquals(
            ContactSelectionMode.PickPhoneNumber(legacy = true),
            mapper.map(getContent(LegacyPhones.CONTENT_ITEM_TYPE)),
        )
    }

    @Test
    fun getContent_postal_mapsToPostalPick() {
        assertEquals(
            ContactSelectionMode.PickPostal(),
            mapper.map(getContent(StructuredPostal.CONTENT_ITEM_TYPE)),
        )
    }

    @Test
    fun getContent_legacyPostal_mapsToLegacyPostalPick() {
        assertEquals(
            ContactSelectionMode.PickPostal(legacy = true),
            mapper.map(getContent(LegacyContactMethods.CONTENT_POSTAL_ITEM_TYPE)),
        )
    }

    @Test
    fun getContent_email_mapsToNull() {
        assertNull(mapper.map(getContent(Email.CONTENT_ITEM_TYPE)))
    }

    @Test
    fun getContent_withoutType_doesNotResolveTypeFromData() {
        val uri = Uri.parse("content://com.android.contacts/contacts/1")
        every { contentResolver.getType(uri) } returns Contacts.CONTENT_ITEM_TYPE

        assertNull(mapper.map(Intent(Intent.ACTION_GET_CONTENT, uri)))
    }

    // endregion

    @Test
    fun insertOrEdit_mapsToInsertOrEdit() {
        assertEquals(
            ContactSelectionMode.InsertOrEdit,
            mapper.map(Intent(Intent.ACTION_INSERT_OR_EDIT).setType(Contacts.CONTENT_ITEM_TYPE)),
        )
    }

    // region ACTION_CREATE_SHORTCUT

    @Test
    fun createShortcut_dialAlias_mapsToCallShortcut() {
        assertEquals(
            ContactSelectionMode.CreateShortcut(ShortcutTarget.CALL),
            mapper.map(createShortcut("alias.DialShortcut")),
        )
    }

    @Test
    fun createShortcut_messageAlias_mapsToMessageShortcut() {
        assertEquals(
            ContactSelectionMode.CreateShortcut(ShortcutTarget.MESSAGE),
            mapper.map(createShortcut("alias.MessageShortcut")),
        )
    }

    @Test
    fun createShortcut_otherComponent_mapsToContactShortcut() {
        assertEquals(
            ContactSelectionMode.CreateShortcut(ShortcutTarget.CONTACT),
            mapper.map(createShortcut("alias.ShortcutContactsAction")),
        )
    }

    @Test
    fun createShortcut_withoutComponent_mapsToContactShortcut() {
        assertEquals(
            ContactSelectionMode.CreateShortcut(ShortcutTarget.CONTACT),
            mapper.map(Intent(Intent.ACTION_CREATE_SHORTCUT)),
        )
    }

    // endregion

    // region ACTION_SELECT_ITEMS

    @Test
    fun selectItems_phones_mapsToSmsSendToGroup() {
        val intent = selectItems(Phone.CONTENT_TYPE)
            .putExtra(UiIntentActions.SELECTION_ITEM_LIST, longArrayOf(1, 2, 3))
            .putExtra(UiIntentActions.SELECTION_DEFAULT_SELECTION, longArrayOf(2))
            .putExtra(UiIntentActions.SELECTION_SEND_TITLE, "Send")

        assertEquals(
            ContactSelectionMode.SendToGroup(
                target = SentToGroupTarget.SMS,
                itemIds = listOf(1L, 2L, 3L),
                defaultSelection = listOf(2L),
                chooserTitle = "Send",
            ),
            mapper.map(intent),
        )
    }

    @Test
    fun selectItems_emails_mapsToEmailSendToGroup() {
        assertEquals(
            ContactSelectionMode.SendToGroup(
                target = SentToGroupTarget.EMAIL,
                itemIds = emptyList(),
                defaultSelection = emptyList(),
                chooserTitle = null,
            ),
            mapper.map(selectItems(Email.CONTENT_TYPE)),
        )
    }

    @Test
    fun selectItems_unsupportedType_mapsToNull() {
        assertNull(mapper.map(selectItems(Contacts.CONTENT_TYPE)))
    }

    // endregion

    // region PICK_JOIN_CONTACT_ACTION

    @Test
    fun join_withTargetContactId_mapsToJoin() {
        val intent = Intent(UiIntentActions.PICK_JOIN_CONTACT_ACTION)
            .putExtra(UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, 42L)

        assertEquals(ContactSelectionMode.Join(42L), mapper.map(intent))
    }

    @Test
    fun join_withoutTargetContactId_mapsToNull() {
        assertNull(mapper.map(Intent(UiIntentActions.PICK_JOIN_CONTACT_ACTION)))
    }

    // endregion

    @Test
    fun listDefault_mapsToSinglePick() {
        assertEquals(
            ContactSelectionMode.Pick(),
            mapper.map(Intent(UiIntentActions.LIST_DEFAULT)),
        )
    }

    @Test
    fun search_mapsToSinglePick() {
        assertEquals(ContactSelectionMode.Pick(), mapper.map(Intent(Intent.ACTION_SEARCH)))
    }

    @Test
    fun unsupportedAction_mapsToNull() {
        assertNull(mapper.map(Intent(Intent.ACTION_VIEW)))
    }

    @Test
    fun noAction_mapsToNull() {
        assertNull(mapper.map(Intent()))
    }

    private fun pick(type: String, multiple: Boolean = false) =
        Intent(Intent.ACTION_PICK)
            .setType(type)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)

    private fun getContent(type: String) =
        Intent(Intent.ACTION_GET_CONTENT).setType(type)

    private fun createShortcut(className: String) =
        Intent(Intent.ACTION_CREATE_SHORTCUT)
            .setComponent(ComponentName("com.android.contacts", className))

    private fun selectItems(type: String) =
        Intent(UiIntentActions.ACTION_SELECT_ITEMS).setType(type)
}
