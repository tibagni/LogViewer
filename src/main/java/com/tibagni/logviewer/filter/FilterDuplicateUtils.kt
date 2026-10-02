package com.tibagni.logviewer.filter

data class FilterMatch(
    val group: String,
    val filter: Filter,
    val index: Int
)

data class DuplicateCluster(
    val representative: Filter,
    val matches: List<FilterMatch>
) {
    val isCrossGroup: Boolean
        get() = matches.map { it.group }.distinct().size > 1

    val affectedGroups: List<String>
        get() = matches.map { it.group }.distinct()
}

interface FilterDuplicateUtils {
    fun isDuplicate(f1: Filter, f2: Filter): Boolean
    fun findDuplicateClusters(groups: Map<String, List<Filter>>): List<DuplicateCluster>
    fun findFirstDuplicate(
        pattern: String,
        caseSensitive: Boolean,
        editingFilter: Filter?,
        allGroups: Map<String, List<Filter>>
    ): FilterMatch?
}

class FilterDuplicateUtilsImpl : FilterDuplicateUtils {

    data class FilterKey(val pattern: String, val isCaseSensitive: Boolean)

    private fun getFilterKey(pattern: String, isCaseSensitive: Boolean): FilterKey {
        val trimmed = pattern.trim()
        val normalized = if (isCaseSensitive) trimmed else trimmed.lowercase()
        return FilterKey(normalized, isCaseSensitive)
    }

    override fun isDuplicate(f1: Filter, f2: Filter): Boolean {
        return getFilterKey(f1.patternString, f1.isCaseSensitive) ==
                getFilterKey(f2.patternString, f2.isCaseSensitive)
    }

    override fun findDuplicateClusters(
        groups: Map<String, List<Filter>>
    ): List<DuplicateCluster> {
        val allFilters = mutableListOf<FilterMatch>()
        for ((group, filters) in groups) {
            filters.forEachIndexed { idx, filter ->
                allFilters.add(FilterMatch(group, filter, idx))
            }
        }

        if (allFilters.size < 2) return emptyList()

        return allFilters
            .groupBy { getFilterKey(it.filter.patternString, it.filter.isCaseSensitive) }
            .filter { it.value.size > 1 }
            .values
            .map { clusterMatches ->
                DuplicateCluster(clusterMatches[0].filter, clusterMatches)
            }
    }

    override fun findFirstDuplicate(
        pattern: String,
        caseSensitive: Boolean,
        editingFilter: Filter?,
        allGroups: Map<String, List<Filter>>
    ): FilterMatch? {
        val targetKey = getFilterKey(pattern, caseSensitive)
        for ((group, filters) in allGroups) {
            for ((index, existing) in filters.withIndex()) {
                if (existing === editingFilter) {
                    continue
                }
                if (getFilterKey(existing.patternString, existing.isCaseSensitive) == targetKey) {
                    return FilterMatch(group, existing, index)
                }
            }
        }
        return null
    }
}
