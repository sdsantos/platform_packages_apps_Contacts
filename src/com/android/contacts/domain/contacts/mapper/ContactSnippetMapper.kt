package com.android.contacts.domain.contacts.mapper

import android.content.res.Resources
import com.android.contacts.R
import com.android.contacts.util.SearchUtil
import javax.inject.Inject

internal fun interface ContactSnippetMapper {
    fun map(
        snippet: String?,
        isSnippetDeferred: Boolean,
        query: String,
        displayName: String?,
    ): String?
}

internal class ContactSnippetMapperImpl @Inject constructor(
    private val resources: Resources,
) : ContactSnippetMapper {

    override fun map(
        snippet: String?,
        isSnippetDeferred: Boolean,
        query: String,
        displayName: String?,
    ): String? {
        if (snippet.isNullOrBlank() || snippet == displayName) return null
        val cleanQuery = SearchUtil.cleanStartAndEndOfSearchQuery(query.lowercase())
        if (cleanQuery.isEmpty() || displayName.matches(cleanQuery)) return null
        if (!isSnippetDeferred) return snippet

        val matched = SearchUtil.findMatchingLine(snippet, cleanQuery)
        val line = matched.line ?: return null

        val maxLength = resources.getInteger(R.integer.snippet_length_before_tokenize)
        return when {
            line.length > maxLength -> snippetize(line, matched.startIndex, maxLength)
            else -> line
        }
    }

    private fun String?.matches(query: String): Boolean {
        if (isNullOrEmpty()) return false
        return TOKEN_PATTERN.findAll(lowercase()).any { it.value.startsWith(query) }
    }

    /**
     * Shows up to [maxLength] characters around the match at [matchIndex], only with full tokens.
     * As many tokens before the match as possible are kept, before trying tokens after it.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun snippetize(line: String, matchIndex: Int, maxLength: Int): String {
        var remainingLength = maxLength
        var tempRemainingLength = remainingLength

        // Find the end of the match token first. Unlike the legacy code, a match token that
        // reaches the end of the line is kept, instead of being cut off.
        var index = matchIndex
        var endTokenIndex = line.length
        while (index < line.length) {
            if (!line[index].isLetterOrDigit()) {
                endTokenIndex = index
                break
            }
            tempRemainingLength--
            index++
        }
        remainingLength = tempRemainingLength

        // Find as much content before the match
        index = matchIndex - 1
        tempRemainingLength = remainingLength
        var startTokenIndex = matchIndex
        while (index > -1 && tempRemainingLength > 0) {
            if (!line[index].isLetterOrDigit()) {
                startTokenIndex = index
                remainingLength = tempRemainingLength
            }
            tempRemainingLength--
            index--
        }

        // Find remaining content after the match
        index = endTokenIndex
        tempRemainingLength = remainingLength
        while (index < line.length && tempRemainingLength > 0) {
            if (!line[index].isLetterOrDigit()) {
                endTokenIndex = index
            }
            tempRemainingLength--
            index++
        }

        // The boundaries land on separators, which aren't shown next to the ellipses
        var start = startTokenIndex
        while (start < matchIndex && !line[start].isLetterOrDigit()) start++
        var end = endTokenIndex
        while (end > matchIndex && !line[end - 1].isLetterOrDigit()) end--

        return buildString {
            if (start > 0) append(ELLIPSIS)
            append(line, start, end)
            if (end < line.length) append(ELLIPSIS)
        }
    }

    private companion object {
        const val ELLIPSIS = "…"

        // E-mail addresses are parsed as a single token, otherwise splits on non-alphanumerics
        val TOKEN_PATTERN = Regex("""([\w\-.]+)@((?:\w+\.)+)([a-zA-Z]{2,4})|\w+""")
    }
}
