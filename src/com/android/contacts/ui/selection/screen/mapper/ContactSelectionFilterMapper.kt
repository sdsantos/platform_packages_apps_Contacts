package com.android.contacts.ui.selection.screen.mapper

import com.android.contacts.domain.contacts.model.ContactSelectionFilter
import com.android.contacts.domain.contacts.model.ContactSelectionFilter as Filter
import com.android.contacts.ui.selection.screen.model.ContactSelectionMode as Mode
import com.android.contacts.ui.selection.screen.model.SentToGroupTarget
import com.android.contacts.ui.selection.screen.model.ShortcutTarget
import javax.inject.Inject

internal fun interface ContactSelectionFilterMapper {
    fun map(mode: Mode): ContactSelectionFilter
}

internal class ContactSelectionFilterMapperImpl @Inject constructor() :
    ContactSelectionFilterMapper {

    override fun map(mode: Mode): ContactSelectionFilter {
        return when (mode) {
            is Mode.Pick -> when {
                mode.legacy -> Filter.Legacy.People
                else -> Filter.Contacts(includesDirectories = !mode.multiple)
            }

            Mode.InsertOrEdit -> Filter.Contacts()

            is Mode.CreateShortcut -> when (mode.target) {
                ShortcutTarget.CONTACT -> Filter.Contacts(includesDirectories = true)
                ShortcutTarget.CALL -> Filter.PhoneNumbers(callableOnly = true)
                ShortcutTarget.MESSAGE -> Filter.PhoneNumbers()
            }

            is Mode.PickPhoneNumber -> when {
                mode.legacy -> Filter.Legacy.Phones
                else -> Filter.PhoneNumbers()
            }

            is Mode.PickEmail -> Filter.Emails(includesDirectories = !mode.multiple)

            is Mode.PickPostal -> when {
                mode.legacy -> Filter.Legacy.PostalAddresses
                else -> Filter.PostalAddresses
            }

            is Mode.Join -> Filter.JoinCandidates(mode.targetContactId)

            is Mode.AddGroupMembers -> Filter.Contacts(
                account = mode.account,
                excludeIds = mode.existingMemberIds,
            )

            is Mode.SendToGroup -> when (mode.target) {
                SentToGroupTarget.SMS -> Filter.PhoneNumbers(
                    restrictToContactIds = mode.itemIds.toSet(),
                )
                SentToGroupTarget.EMAIL -> Filter.Emails(
                    restrictToContactIds = mode.itemIds.toSet(),
                )
            }
        }
    }
}
