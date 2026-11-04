package com.android.contacts.ui.selection.screen.mapper

import android.content.ContentUris
import android.net.Uri
import android.provider.Contacts.ContactMethods as LegacyContactMethods
import android.provider.Contacts.People as LegacyPeople
import android.provider.Contacts.Phones as LegacyPhones
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import com.android.contacts.data.contacts.model.ContactDataKind
import com.android.contacts.data.contacts.model.ContactLookupQuery
import com.android.contacts.data.contacts.repository.ContactsRepository
import com.android.contacts.ui.selection.screen.model.ContactSelectionEffect as Effect
import com.android.contacts.ui.selection.screen.model.ContactSelectionMode as Mode
import com.android.contacts.ui.selection.screen.model.ContactSelectionResult as Result
import com.android.contacts.ui.selection.screen.model.SentToGroupTarget
import com.android.contacts.ui.selection.screen.model.ShortcutTarget
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

internal fun interface ContactSelectionEffectMapper {
    suspend fun map(
        mode: Mode,
        contactIds: Set<Long>,
    ): Effect
}

@Suppress("DEPRECATION")
internal class ContactSelectionEffectMapperImpl @Inject constructor(
    private val contactsRepository: ContactsRepository,
) : ContactSelectionEffectMapper {

    override suspend fun map(
        mode: Mode,
        contactIds: Set<Long>,
    ): Effect {
        if (contactIds.isEmpty()) return Effect.Close(Result.Canceled)
        val contactId = contactIds.first()

        return when (mode) {
            is Mode.Pick -> {
                when {
                    mode.legacy -> {
                        close(Result.Picked(LegacyPeople.CONTENT_URI.withId(contactId)))
                    }
                    mode.multiple -> {
                        close(
                            Result.PickedMultiple(
                                uris = contactIds.map { Contacts.CONTENT_URI.withId(it) },
                                mimeType = Contacts.CONTENT_ITEM_TYPE,
                            ),
                        )
                    }
                    else -> {
                        close(Result.Picked(getContactLookupUri(contactId)))
                    }
                }
            }

            Mode.InsertOrEdit -> {
                Effect.ForwardToEditor.EditContact(getContactLookupUri(contactId))
            }

            is Mode.PickPhoneNumber -> {
                when {
                    mode.legacy -> {
                        close(Result.Picked(LegacyPhones.CONTENT_URI.withId(contactId)))
                    }
                    mode.multiple -> {
                        close(
                            Result.PickedMultiple(
                                uris = contactIds.map { Phone.CONTENT_URI.withId(it) },
                                mimeType = Phone.CONTENT_ITEM_TYPE,
                            ),
                        )
                    }
                    else -> {
                        close(Result.Picked(Data.CONTENT_URI.withId(contactId)))
                    }
                }
            }

            is Mode.PickEmail -> {
                when {
                    mode.multiple -> {
                        close(
                            Result.PickedMultiple(
                                uris = contactIds.map { Email.CONTENT_URI.withId(it) },
                                mimeType = Email.CONTENT_ITEM_TYPE,
                            ),
                        )
                    }
                    else -> {
                        close(Result.Picked(Data.CONTENT_URI.withId(contactId)))
                    }
                }
            }

            is Mode.PickPostal -> {
                when {
                    mode.legacy -> {
                        close(Result.Picked(LegacyContactMethods.CONTENT_URI.withId(contactId)))
                    }
                    else -> {
                        close(Result.Picked(Data.CONTENT_URI.withId(contactId)))
                    }
                }
            }

            is Mode.CreateShortcut -> {
                val uri = when (mode.target) {
                    ShortcutTarget.CONTACT -> getContactLookupUri(contactId)
                    ShortcutTarget.CALL,
                    ShortcutTarget.MESSAGE,
                    -> Data.CONTENT_URI.withId(contactId)
                }
                close(Result.Shortcut(mode.target, uri))
            }

            is Mode.Join -> {
                close(Result.Picked(getContactLookupUri(contactId)))
            }

            is Mode.AddGroupMembers -> {
                close(
                    when {
                        contactIds.size == 1 -> Result.GroupMember(contactIds.first())
                        else -> Result.GroupMembers(contactIds)
                    },
                )
            }

            is Mode.SendToGroup -> {
                val addresses = queryAddresses(mode.target, contactIds)
                when {
                    addresses.isEmpty() -> close(Result.Canceled)
                    else -> Effect.SendTo(mode.target, addresses, mode.chooserTitle)
                }
            }
        }
    }

    private fun close(result: Result): Effect = Effect.Close(result)

    private fun Uri.withId(id: Long): Uri = ContentUris.withAppendedId(this, id)

    private suspend fun getContactLookupUri(contactId: Long): Uri {
        return contactsRepository.lookup(ContactLookupQuery.Id(contactId))
            .first()
            ?.firstOrNull()
            ?.uri
            ?: ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
    }

    private suspend fun queryAddresses(
        target: SentToGroupTarget,
        dataIds: Set<Long>,
    ): List<String> {
        val kind = when (target) {
            SentToGroupTarget.SMS -> ContactDataKind.Phone()
            SentToGroupTarget.EMAIL -> ContactDataKind.Email
        }
        return contactsRepository.getContactDataValues(kind, dataIds)
    }
}
