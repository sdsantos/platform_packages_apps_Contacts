package com.android.contacts.data.photo.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.DisplayPhoto
import android.provider.ContactsContract.RawContacts
import androidx.core.net.toUri
import com.android.contacts.ContactSaveService
import com.android.contacts.ContactsUtils
import com.android.contacts.data.photo.model.TempPhotoUris
import com.android.contacts.domain.accounts.model.AccountModel
import com.android.contacts.model.Contact
import com.android.contacts.model.ContactLoader
import com.android.contacts.model.RawContactDelta
import com.android.contacts.model.RawContactDeltaList
import com.android.contacts.model.RawContactModifier
import com.android.contacts.model.ValuesDelta
import com.android.contacts.model.account.AccountType
import com.android.contacts.tests.factory.AccountModelFactory
import com.android.contacts.util.ContactPhotoUtils
import com.google.common.collect.ImmutableList
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.FileNotFoundException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ContactPhotoRepositoryImplTest {

    private val context = mockk<Context>(relaxed = true)
    private val contentResolver = mockk<ContentResolver>(relaxed = true)

    private val contact = mockk<Contact>()
    private val deltaList = mockk<RawContactDeltaList>(relaxed = true)
    private val accountType = mockk<AccountType>()
    private val photoValues = mockk<ValuesDelta>(relaxed = true)
    private val saveIntent = Intent("save")

    private val repository = ContactPhotoRepositoryImpl(
        context = context,
        contentResolver = contentResolver,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Before
    fun setUp() {
        mockkStatic(ContactPhotoUtils::class)
        mockkStatic(ContactsUtils::class)
        mockkStatic(RawContactModifier::class)
        mockkStatic(ContactSaveService::class)
        mockkConstructor(ContactLoader::class)
        mockkConstructor(RawContactDelta::class)

        every { anyConstructed<ContactLoader>().loadInBackground() } returns contact
        every { contact.isLoaded } returns true
        every { contact.rawContacts } returns ImmutableList.of()
        every { contact.isUserProfile } returns false
        every { contact.createRawContactDeltaList() } returns deltaList
        every {
            RawContactModifier.ensureKindExists(any(), any(), Photo.CONTENT_ITEM_TYPE)
        } returns photoValues
        every {
            ContactSaveService.createSaveContactIntent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any<Long>(),
                any(),
            )
        } returns saveIntent
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getMaxPhotoDimension_returnsProviderValueAndCachesIt() = runTest {
        givenMaxDimensionCursor(1080)

        assertEquals(1080, repository.getMaxPhotoDimension())
        assertEquals(1080, repository.getMaxPhotoDimension())

        verify(exactly = 1) {
            contentResolver.query(
                DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
                arrayOf(DisplayPhoto.DISPLAY_MAX_DIM),
                null,
                null,
                null,
            )
        }
    }

    @Test
    fun getMaxPhotoDimension_whenValueIsZero_returnsNull() = runTest {
        givenMaxDimensionCursor(0)

        assertNull(repository.getMaxPhotoDimension())
    }

    @Test
    fun getMaxPhotoDimension_whenQueryReturnsNull_returnsNullAndDoesNotCache() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        assertNull(repository.getMaxPhotoDimension())
        assertNull(repository.getMaxPhotoDimension())

        verify(exactly = 2) { contentResolver.query(any(), any(), any(), any(), any()) }
    }

    @Test
    fun generateTempPhotoUris_returnsTempAndCroppedUris() = runTest {
        every { ContactPhotoUtils.generateTempImageUri(context) } returns TEMP_URI
        every { ContactPhotoUtils.generateTempCroppedImageUri(context) } returns CROPPED_URI

        assertEquals(TempPhotoUris(TEMP_URI, CROPPED_URI), repository.generateTempPhotoUris())
    }

    @Test
    fun copyPhoto_copiesWithoutDeletingSource() = runTest {
        every {
            ContactPhotoUtils.savePhotoFromUriToUri(context, SOURCE_URI, TEMP_URI, false)
        } returns true

        assertTrue(repository.copyPhoto(SOURCE_URI, TEMP_URI))
    }

    @Test
    fun deletePhoto_deletesUri() = runTest {
        repository.deletePhoto(TEMP_URI)

        verify { contentResolver.delete(TEMP_URI, null, null) }
    }

    @Test
    fun buildThumbnail_whenFileIsMissing_returnsNull() = runTest {
        every { ContactPhotoUtils.getBitmapFromUri(context, PHOTO_URI) } throws
            FileNotFoundException()

        assertNull(repository.buildThumbnail(PHOTO_URI))
    }

    @Test
    fun buildThumbnail_whenBitmapCannotBeDecoded_returnsNull() = runTest {
        every { ContactPhotoUtils.getBitmapFromUri(context, PHOTO_URI) } returns null

        assertNull(repository.buildThumbnail(PHOTO_URI))
    }

    @Test
    fun buildThumbnail_scalesToThumbnailSizeAndCompresses() = runTest {
        val scaled = slot<Bitmap>()
        every { ContactPhotoUtils.getBitmapFromUri(context, PHOTO_URI) } returns
            Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        every { ContactsUtils.getThumbnailSize(context) } returns 96
        every { ContactPhotoUtils.compressBitmap(capture(scaled)) } returns THUMBNAIL

        assertArrayEquals(THUMBNAIL, repository.buildThumbnail(PHOTO_URI))
        assertEquals(96, scaled.captured.width)
        assertEquals(96, scaled.captured.height)
    }

    @Test
    fun savePhotoToRawContact_setsPhotoAndStartsSaveService() = runTest {
        val rawContactDelta = givenExistingRawContactDelta()

        val result = repository.savePhotoToRawContact(
            contactUri = CONTACT_URI,
            rawContactId = RAW_CONTACT_ID,
            thumbnail = THUMBNAIL,
            photoUri = PHOTO_URI,
        )

        assertTrue(result)
        verify {
            RawContactModifier.ensureKindExists(
                rawContactDelta,
                accountType,
                Photo.CONTENT_ITEM_TYPE,
            )
        }
        verify { photoValues.photo = THUMBNAIL }
        verify {
            ContactSaveService.createSaveContactIntent(
                context,
                deltaList,
                "",
                0,
                false,
                null,
                null,
                RAW_CONTACT_ID,
                PHOTO_URI,
            )
        }
        verify { context.startService(saveIntent) }
    }

    @Test
    fun savePhotoToRawContact_whenContactCannotBeLoaded_returnsFalse() = runTest {
        every { anyConstructed<ContactLoader>().loadInBackground() } returns null

        assertFalse(savePhotoToExistingRawContact())
        verify(exactly = 0) { context.startService(any()) }
    }

    @Test
    fun savePhotoToRawContact_whenContactIsNotLoaded_returnsFalse() = runTest {
        every { contact.isLoaded } returns false

        assertFalse(savePhotoToExistingRawContact())
        verify(exactly = 0) { context.startService(any()) }
    }

    @Test
    fun savePhotoToRawContact_whenRawContactIsNotFound_returnsFalse() = runTest {
        every { deltaList.getByRawContactId(RAW_CONTACT_ID) } returns null

        assertFalse(savePhotoToExistingRawContact())
        verify(exactly = 0) { context.startService(any()) }
    }

    @Test
    fun savePhotoToRawContact_whenAccountTypeCannotHoldPhotos_returnsFalse() = runTest {
        givenExistingRawContactDelta()
        every { RawContactModifier.ensureKindExists(any(), any(), any()) } returns null

        assertFalse(savePhotoToExistingRawContact())
        verify(exactly = 0) { context.startService(any()) }
    }

    @Test
    fun savePhotoToRawContact_whenServiceCannotStart_returnsFalse() = runTest {
        givenExistingRawContactDelta()
        every { context.startService(any()) } throws IllegalStateException("background")

        assertFalse(savePhotoToExistingRawContact())
    }

    @Test
    fun savePhotoToNewRawContact_addsRawContactInAccountAndStartsSaveService() = runTest {
        val added = slot<RawContactDelta>()
        every { deltaList.add(capture(added)) } returns true
        every { anyConstructed<RawContactDelta>().getRawContactAccountType(context) } returns
            accountType
        val account = AccountModelFactory.build(
            name = "user@example.org",
            type = "com.example",
            dataSet = "plus",
        )

        val result = repository.savePhotoToNewRawContact(
            contactUri = CONTACT_URI,
            account = account,
            thumbnail = THUMBNAIL,
            photoUri = PHOTO_URI,
        )

        assertTrue(result)
        val values = added.captured.values
        assertEquals("com.example", values.getAsString(RawContacts.ACCOUNT_TYPE))
        assertEquals("user@example.org", values.getAsString(RawContacts.ACCOUNT_NAME))
        assertEquals("plus", values.getAsString(RawContacts.DATA_SET))
        verify { photoValues.photo = THUMBNAIL }
        verify { context.startService(saveIntent) }
    }

    @Test
    fun savePhotoToNewRawContact_withNullAccount_addsLocalRawContact() = runTest {
        val added = slot<RawContactDelta>()
        every { deltaList.add(capture(added)) } returns true
        every { anyConstructed<RawContactDelta>().getRawContactAccountType(context) } returns
            accountType

        repository.savePhotoToNewRawContact(
            contactUri = CONTACT_URI,
            account = AccountModel.NULL,
            thumbnail = THUMBNAIL,
            photoUri = PHOTO_URI,
        )

        val values = added.captured.values
        assertNull(values.getAsString(RawContacts.ACCOUNT_TYPE))
        assertNull(values.getAsString(RawContacts.ACCOUNT_NAME))
        assertNull(values.getAsString(RawContacts.DATA_SET))
    }

    private suspend fun savePhotoToExistingRawContact(): Boolean {
        return repository.savePhotoToRawContact(
            contactUri = CONTACT_URI,
            rawContactId = RAW_CONTACT_ID,
            thumbnail = THUMBNAIL,
            photoUri = PHOTO_URI,
        )
    }

    private fun givenExistingRawContactDelta(): RawContactDelta {
        val rawContactDelta = mockk<RawContactDelta>()
        every { rawContactDelta.getRawContactAccountType(context) } returns accountType
        every { rawContactDelta.rawContactId } returns RAW_CONTACT_ID
        every { deltaList.getByRawContactId(RAW_CONTACT_ID) } returns rawContactDelta
        return rawContactDelta
    }

    private fun givenMaxDimensionCursor(value: Int) {
        val cursor = MatrixCursor(arrayOf(DisplayPhoto.DISPLAY_MAX_DIM)).apply {
            addRow(arrayOf(value))
        }
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor
    }

    private companion object {
        const val RAW_CONTACT_ID = 42L
        val CONTACT_URI = "content://com.android.contacts/contacts/lookup/abc/1".toUri()
        val SOURCE_URI = "content://media/external/images/media/794".toUri()
        val TEMP_URI = "content://com.android.contacts.files/temp.jpg".toUri()
        val CROPPED_URI = "content://com.android.contacts.files/cropped.jpg".toUri()
        val PHOTO_URI = CROPPED_URI
        val THUMBNAIL = byteArrayOf(1, 2, 3)
    }
}
