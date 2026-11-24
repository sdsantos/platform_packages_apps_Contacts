package com.android.contacts.ui.common.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.android.contacts.ui.common.model.HighlightedText
import kotlinx.collections.immutable.toImmutableList

internal fun findSearchHighlights(text: String, query: String): List<IntRange> {
    val queryTokens = query.splitTokens()
    if (queryTokens.isEmpty() || text.isEmpty()) return emptyList()

    val matches = buildList {
        text.wordStarts().forEach { wordStart ->
            queryTokens.forEach { queryToken ->
                text.matchTokenAt(wordStart, queryToken)
                    ?.let(::add)
            }
        }
    }
    return matches.sortAndMergeOverlaps()
}

internal fun String.highlightSearch(query: String): HighlightedText {
    return HighlightedText(
        text = this,
        highlights = findSearchHighlights(this, query).toImmutableList(),
    )
}

@Composable
internal fun HighlightedText.toAnnotatedString(
    color: Color = MaterialTheme.colorScheme.primary,
): AnnotatedString {
    return remember(this, color) {
        buildAnnotatedString {
            var index = 0
            highlights.forEach { range ->
                append(text, index, range.first)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                    append(text, range.first, range.last + 1)
                }
                index = range.last + 1
            }
            append(text, index, text.length)
        }
    }
}

private fun String.splitTokens(): List<String> {
    return buildList {
        val current = StringBuilder()
        fun addCurrent() {
            if (current.isNotEmpty()) {
                add(current.toString())
                current.clear()
            }
        }
        this@splitTokens.forEach { char ->
            when {
                char.isLetterOrDigit() -> current.append(char)
                else -> addCurrent()
            }
        }
        addCurrent()
    }
}

private fun String.wordStarts(): Sequence<Int> {
    return indices.asSequence().filter { index ->
        this[index].isLetterOrDigit() && (index == 0 || !this[index - 1].isLetterOrDigit())
    }
}

private fun String.matchTokenAt(start: Int, token: String): IntRange? {
    return matchTextAt(start, token)
        ?: matchNumberIgnoringSeparatorsAt(start, token)
}

private fun String.matchTextAt(start: Int, token: String): IntRange? {
    return (start until start + token.length).takeIf {
        regionMatches(start, token, 0, token.length, ignoreCase = true)
    }
}

private fun String.matchNumberIgnoringSeparatorsAt(start: Int, token: String): IntRange? {
    if (!token.all { it.isDigit() } || !this[start].isDigit()) return null

    var tokenIndex = 0
    var index = start
    while (index < length && tokenIndex < token.length) {
        val char = this[index]
        when (char) {
            token[tokenIndex] -> tokenIndex++
            !in NUMBER_SEPARATORS -> return null
        }
        index++
    }
    return (start until index).takeIf { tokenIndex == token.length }
}

private fun List<IntRange>.sortAndMergeOverlaps(): List<IntRange> {
    return sortedBy { it.first }.fold(mutableListOf()) { merged, range ->
        val last = merged.lastOrNull()
        when {
            last != null && range.first <= last.last + 1 -> {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            }
            else -> merged.add(range)
        }
        merged
    }
}

private const val NUMBER_SEPARATORS = " -.()/"
