package com.tibagni.logviewer.filter

object FiltersSearchLogic {

    fun matches(filter: Filter, query: String): Boolean {
        if (query.isEmpty()) return true
        val lower = query.lowercase()
        val pattern = filter.patternString?.lowercase() ?: ""
        val name = filter.name?.lowercase() ?: ""
        return pattern.contains(lower) || name.contains(lower)
    }

    fun filterGroup(filters: List<Filter>, query: String): List<Filter> {
        if (query.isEmpty()) return filters
        return filters.filter { matches(it, query) }
    }

    fun filterAllGroups(groups: Map<String, List<Filter>>, query: String): Map<String, List<Filter>> {
        if (query.isEmpty()) return groups
        return groups.mapValues { (_, filters) -> filterGroup(filters, query) }
    }

    fun countTotalMatches(filteredGroups: Map<String, List<Filter>>): Int {
        return filteredGroups.values.map { it.size }.sum()
    }
}
