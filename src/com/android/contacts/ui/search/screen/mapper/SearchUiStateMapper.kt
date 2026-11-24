package com.android.contacts.ui.search.screen.mapper

import com.android.contacts.domain.contacts.model.ContactSearchResult
import com.android.contacts.ui.common.mapper.ContactUiModelMapper
import com.android.contacts.ui.common.text.highlightSearch
import com.android.contacts.ui.search.screen.model.SearchContentState as Content
import com.android.contacts.ui.search.screen.model.SearchResultUiModel
import com.android.contacts.ui.search.screen.model.SearchState as State
import com.android.contacts.ui.search.screen.model.SearchUiState as UiState
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal fun interface SearchUiStateMapper {
    fun map(state: State, content: Content): UiState
}

internal class SearchUiStateMapperImpl @Inject constructor(
    private val contactUiModelMapper: ContactUiModelMapper,
) : SearchUiStateMapper {

    override fun map(state: State, content: Content): UiState {
        return UiState(
            query = state.query,
            requestInitialFocus = state.requestInitialFocus,
            isSearching = when (content) {
                Content.Idle -> false
                Content.Searching -> true
                is Content.Loaded -> content.isRefreshing
            },
            results = when (content) {
                is Content.Loaded -> {
                    content.results
                        .map { it.toUiModel(content.query) }
                        .toImmutableList()
                }
                else -> {
                    persistentListOf()
                }
            },
            isEmpty = content is Content.Loaded &&
                !content.isRefreshing &&
                content.results.isEmpty(),
        )
    }

    private fun ContactSearchResult.toUiModel(query: String): SearchResultUiModel {
        val contactUiModel = contactUiModelMapper.map(contact)
        return SearchResultUiModel(
            key = contact.key,
            name = contactUiModel.name.highlightSearch(query),
            matchedData = matchedData?.highlightSearch(query),
            avatarImage = contactUiModel.avatarImage,
        )
    }
}
