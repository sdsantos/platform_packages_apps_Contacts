package com.android.contacts.ui.selection.screen.mapper

import android.content.res.Resources
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import androidx.annotation.StringRes
import com.android.contacts.R
import com.android.contacts.data.contactdetails.model.ContactPhoto
import com.android.contacts.domain.contacts.model.ContactSummary
import com.android.contacts.domain.contacts.model.GetContactsForSelectionResult
import com.android.contacts.model.Contact
import com.android.contacts.ui.common.components.ContactAvatarImage
import com.android.contacts.ui.selection.screen.model.ContactListSection as Section
import com.android.contacts.ui.selection.screen.model.ContactSelectionListState
import com.android.contacts.ui.selection.screen.model.ContactSelectionMode as Mode
import com.android.contacts.ui.selection.screen.model.ContactSelectionRequest
import com.android.contacts.ui.selection.screen.model.ContactSelectionSearchState as SearchState
import com.android.contacts.ui.selection.screen.model.ContactSelectionState
import com.android.contacts.ui.selection.screen.model.ContactSelectionUiState
import com.android.contacts.ui.selection.screen.model.ContactUiModel
import com.android.contacts.ui.selection.screen.model.ShortcutTarget
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

internal fun interface ContactSelectionUiStateMapper {
    fun map(
        request: ContactSelectionRequest,
        state: ContactSelectionState,
        contactsResult: GetContactsForSelectionResult?,
        joinTarget: Contact?,
    ): ContactSelectionUiState
}

internal class ContactSelectionUiStateMapperImpl @Inject constructor(
    private val resources: Resources,
) : ContactSelectionUiStateMapper {

    override fun map(
        request: ContactSelectionRequest,
        state: ContactSelectionState,
        contactsResult: GetContactsForSelectionResult?,
        joinTarget: Contact?,
    ): ContactSelectionUiState {
        val mode = request.mode
        val isSearching = (state.searchState as? SearchState.Searching)?.query?.isNotBlank() == true

        return ContactSelectionUiState(
            title = title(request, state.selectedContactIds),
            description = description(mode, joinTarget),
            emptyMessageId = when {
                isSearching -> R.string.listFoundAllContactsZero
                else -> R.string.noContacts
            },
            listState = when {
                contactsResult == null -> ContactSelectionListState.Loading
                else -> ContactSelectionListState.Loaded(
                    contacts = mapSections(
                        contactsResult = contactsResult,
                        selectedIds = state.selectedContactIds,
                        showFavorites = mode is Mode.Pick && mode.includeFavorites && !isSearching,
                        showLetters = !isSearching,
                    )
                )
            },
            searchState = state.searchState,
            allowMultiple = mode.isMultiple,
            canCreate = mode.canCreateContact && !isSearching,
            showAvatars = !mode.isLegacy,
        )
    }

    private fun title(request: ContactSelectionRequest, selectedContactIds: Set<Long>): String {
        return when {
            selectedContactIds.isEmpty() -> {
                request.titleOverride?.takeIf { it.isNotBlank() }
                    ?: resources.getString(titleId(request.mode))
            }
            else -> {
                resources.getQuantityString(
                    R.plurals.contactPickerSelectedTitle,
                    selectedContactIds.size,
                    selectedContactIds.size,
                )
            }
        }
    }

    private fun description(mode: Mode, joinTarget: Contact?): String? {
        if (mode !is Mode.Join || joinTarget == null) return null
        val name = joinTarget.displayName?.takeIf { it.isNotBlank() }
            ?: resources.getString(R.string.missing_name)
        return resources.getString(R.string.blurbJoinContactDataWith, name)
    }

    @StringRes
    private fun titleId(mode: Mode): Int {
        return when (mode) {
            is Mode.Pick -> {
                when {
                    mode.multiple -> R.string.pickerSelectContactsActivityTitle
                    else -> R.string.contactPickerActivityTitle
                }
            }
            Mode.InsertOrEdit -> {
                R.string.contactInsertOrEditActivityTitle
            }
            is Mode.PickPhoneNumber -> {
                when {
                    mode.multiple -> R.string.pickerSelectContactsActivityTitle
                    else -> R.string.contactPickerActivityTitle
                }
            }
            is Mode.PickEmail -> {
                when {
                    mode.multiple -> R.string.pickerSelectContactsActivityTitle
                    else -> R.string.contactPickerActivityTitle
                }
            }
            is Mode.PickPostal -> {
                R.string.contactPickerActivityTitle
            }
            is Mode.CreateShortcut -> {
                when (mode.target) {
                    ShortcutTarget.CONTACT,
                    ShortcutTarget.CALL,
                    ShortcutTarget.MESSAGE,
                    -> R.string.shortcutActivityTitle
                }
            }
            is Mode.Join -> {
                R.string.titleJoinContactDataWith
            }
            is Mode.AddGroupMembers -> {
                R.string.groupMemberPickerActivityTitle
            }
            is Mode.SendToGroup -> {
                R.string.pickerSelectContactsActivityTitle
            }
        }
    }

    private fun mapSections(
        contactsResult: GetContactsForSelectionResult,
        selectedIds: Set<Long>,
        showFavorites: Boolean,
        showLetters: Boolean,
    ): ImmutableMap<Section, ImmutableList<ContactUiModel>> {
        val sections = LinkedHashMap<Section, List<ContactUiModel>>()
        val contacts = contactsResult.contacts

        if (contactsResult.suggestions.any()) {
            sections[Section.Suggestions] = contactsResult.suggestions.toUiModels(selectedIds)
        }

        if (showFavorites) {
            contacts
                .filter { it.isFavorite }
                .takeIf { it.any() }
                ?.let { favorites ->
                    sections[Section.Favorites] = favorites.toUiModels(selectedIds)
                }
        }

        when {
            showLetters -> {
                sections.putAll(
                    contacts
                        .groupBy { contact -> Section.Alphabetical(sectionLetter(contact)) }
                        .mapValues { (_, contacts) -> contacts.toUiModels(selectedIds) },
                )
            }
            else -> {
                if (contacts.any()) {
                    sections[Section.Undefined] = contacts.toUiModels(selectedIds)
                }
            }
        }

        return sections
            .mapValues { (_, list) -> list.toImmutableList() }
            .toImmutableMap()
    }

    private fun List<ContactSummary>.toUiModels(selectedIds: Set<Long>): List<ContactUiModel> {
        return map { it.toUiModel(selectedIds = selectedIds) }
    }

    private fun ContactSummary.toUiModel(selectedIds: Set<Long>): ContactUiModel {
        return ContactUiModel(
            contactId = contactId,
            name = displayName(),
            description = description(),
            avatarImage = contactPhoto?.toAvatarImage(),
            isSelected = contactId in selectedIds,
        )
    }

    private fun ContactSummary.displayName(): String {
        return name.takeIf { it.isNotBlank() }
            ?: resources.getString(R.string.missing_name)
    }

    private fun ContactSummary.description(): String? {
        // Postal addresses may span across multiple lines
        val value = field?.value
            ?.lines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.any() }
            ?.joinToString(", ")
        val label = fieldLabel()
        return when {
            value == null -> null
            label == null -> value
            else -> "$label · $value"
        }
    }

    private fun ContactSummary.fieldLabel(): String? {
        val type = field?.type ?: return null
        return when (field.kind) {
            ContactSummary.FieldKind.PHONE ->
                Phone.getTypeLabel(resources, type, field.label)
            ContactSummary.FieldKind.EMAIL ->
                Email.getTypeLabel(resources, type, field.label)
            ContactSummary.FieldKind.POSTAL_ADDRESS ->
                StructuredPostal.getTypeLabel(resources, type, field.label)
            ContactSummary.FieldKind.UNSPECIFIED ->
                null
        }
            ?.toString()
            ?.takeIf { it.isNotBlank() }
    }

    private fun sectionLetter(contact: ContactSummary): String {
        return contact.sectionLabel
            ?.takeIf { it.isNotBlank() }
            ?: firstLetter(contact)
    }

    private fun firstLetter(contact: ContactSummary): String {
        val firstChar = contact.name
            .trim()
            .dropWhile { it.category == CharCategory.FORMAT }
            .firstOrNull()
        return when {
            firstChar == null || !firstChar.isLetter() -> OTHER_SECTION
            else -> firstChar.uppercaseChar().toString()
        }
    }

    private fun ContactPhoto.toAvatarImage(): ContactAvatarImage {
        return when (this) {
            is ContactPhoto.Bytes -> ContactAvatarImage.Bytes(value)
            is ContactPhoto.Uri -> ContactAvatarImage.Uri(value)
        }
    }

    private companion object {
        const val OTHER_SECTION = "#"
    }
}
