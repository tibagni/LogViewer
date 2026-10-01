package com.tibagni.logviewer.filter

import com.tibagni.logviewer.util.StringUtils

data class FilterSearchResult(
  val group: String,
  val filter: Filter
)

object FiltersSearchLogic {
  fun matches(filter: Filter, query: String): Boolean {
    if (query.isBlank()) return true
    val lowerQuery = query.lowercase().trim()
    val name = filter.name?.lowercase() ?: ""
    val pattern = filter.patternString?.lowercase() ?: ""
    return name.contains(lowerQuery) || pattern.contains(lowerQuery)
  }

  fun search(openedFilters: Map<String, List<Filter>>, query: String): List<FilterSearchResult> {
    val results = mutableListOf<FilterSearchResult>()
    for ((group, filters) in openedFilters) {
      for (filter in filters) {
        if (matches(filter, query)) {
          results.add(FilterSearchResult(group, filter))
        }
      }
    }
    return results
  }

  fun highlightSearchText(text: String, query: String): String {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return StringUtils.htmlEscape(text)
    val idx = text.indexOf(trimmed, ignoreCase = true)
    return if (idx >= 0) {
      StringUtils.htmlHighlightAndEscape(text, idx, idx + trimmed.length)
    } else {
      StringUtils.htmlEscape(text)
    }
  }

  fun truncateAndHighlight(text: String, query: String, maxLength: Int): String {
    val trimmedQuery = query.trim()
    if (text.length <= maxLength) {
      return highlightSearchText(text, trimmedQuery)
    }

    if (trimmedQuery.isEmpty()) {
      return StringUtils.htmlEscape(text.substring(0, maxLength - 3)) + "..."
    }

    val matchIdx = text.indexOf(trimmedQuery, ignoreCase = true)
    if (matchIdx < 0) {
      return StringUtils.htmlEscape(text.substring(0, maxLength - 3)) + "..."
    }

    val matchEnd = matchIdx + trimmedQuery.length
    return if (matchEnd <= maxLength - 3) {
      val truncated = text.substring(0, maxLength - 3)
      highlightSearchText(truncated, trimmedQuery) + "..."
    } else {
      val contextBefore = 15
      val start = maxOf(0, matchIdx - contextBefore)
      val end = minOf(text.length, start + maxLength - 6)
      val prefix = if (start > 0) "..." else ""
      val suffix = if (end < text.length) "..." else ""
      val visiblePart = text.substring(start, end)
      prefix + highlightSearchText(visiblePart, trimmedQuery) + suffix
    }
  }
}
