package com.tibagni.logviewer.filter

import kotlin.math.max
import kotlin.math.min

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

object FilterSimilarityUtils {
    const val DEFAULT_SIMILARITY_THRESHOLD = 0.90

    private class DisjointSet(size: Int) {
        private val parent = IntArray(size) { it }
        fun find(i: Int): Int {
            if (parent[i] == i) return i
            parent[i] = find(parent[i])
            return parent[i]
        }
        fun union(i: Int, j: Int) {
            val rootI = find(i)
            val rootJ = find(j)
            if (rootI != rootJ) {
                parent[rootI] = rootJ
            }
        }
    }

    fun findDuplicateClusters(
        groups: Map<String, List<Filter>>,
        threshold: Double = DEFAULT_SIMILARITY_THRESHOLD
    ): List<DuplicateCluster> {
        val allFilters = mutableListOf<FilterMatch>()
        for ((group, filters) in groups) {
            filters.forEachIndexed { idx, filter ->
                allFilters.add(FilterMatch(group, filter, idx))
            }
        }

        if (allFilters.size < 2) return emptyList()

        val dsu = DisjointSet(allFilters.size)
        for (i in 0 until allFilters.size) {
            for (j in i + 1 until allFilters.size) {
                if (isDuplicateOrSimilar(allFilters[i].filter, allFilters[j].filter, threshold)) {
                    dsu.union(i, j)
                }
            }
        }

        val clustersByRoot = mutableMapOf<Int, MutableList<FilterMatch>>()
        for (i in 0 until allFilters.size) {
            val root = dsu.find(i)
            clustersByRoot.getOrPut(root) { mutableListOf() }.add(allFilters[i])
        }

        return clustersByRoot.values
            .filter { it.size > 1 }
            .map { clusterMatches ->
                DuplicateCluster(clusterMatches[0].filter, clusterMatches)
            }
    }

    fun levenshteinDistance(s1: CharSequence, s2: CharSequence): Int {
        val len1 = s1.length
        val len2 = s2.length

        if (len1 == 0) return len2
        if (len2 == 0) return len1

        var prev = IntArray(len2 + 1) { it }
        var curr = IntArray(len2 + 1)

        for (i in 1..len1) {
            curr[0] = i
            val c1 = s1[i - 1]
            for (j in 1..len2) {
                val c2 = s2[j - 1]
                val cost = if (c1 == c2) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }

        return prev[len2]
    }

    fun calculateSimilarity(s1: String, s2: String, ignoreCase: Boolean = false): Double {
        val str1 = if (ignoreCase) s1.trim().lowercase() else s1.trim()
        val str2 = if (ignoreCase) s2.trim().lowercase() else s2.trim()

        if (str1 == str2) return 1.0
        val maxLen = max(str1.length, str2.length)
        if (maxLen == 0) return 1.0

        val distance = levenshteinDistance(str1, str2)
        return 1.0 - (distance.toDouble() / maxLen.toDouble())
    }

    fun isDuplicateOrSimilar(
        f1: Filter,
        f2: Filter,
        threshold: Double = DEFAULT_SIMILARITY_THRESHOLD
    ): Boolean {
        if (f1.patternString.trim() == f2.patternString.trim()) return true
        val similarity = calculateSimilarity(f1.patternString, f2.patternString, ignoreCase = !f1.isCaseSensitive && !f2.isCaseSensitive)
        return similarity >= threshold
    }

    fun isPatternDuplicateOrSimilar(
        pattern1: String,
        pattern2: String,
        threshold: Double = DEFAULT_SIMILARITY_THRESHOLD,
        ignoreCase: Boolean = false
    ): Boolean {
        if (pattern1.trim().equals(pattern2.trim(), ignoreCase = ignoreCase)) return true
        return calculateSimilarity(pattern1, pattern2, ignoreCase = ignoreCase) >= threshold
    }

    @JvmOverloads
    fun findFirstDuplicateOrSimilar(
        pattern: String,
        caseSensitive: Boolean,
        editingFilter: Filter?,
        allGroups: Map<String, List<Filter>>,
        threshold: Double = DEFAULT_SIMILARITY_THRESHOLD
    ): FilterMatch? {
        for ((group, filters) in allGroups) {
            for ((index, existing) in filters.withIndex()) {
                if (existing === editingFilter) {
                    continue
                }
                val ignoreCase = !caseSensitive && !existing.isCaseSensitive
                if (isPatternDuplicateOrSimilar(pattern, existing.patternString, threshold, ignoreCase)) {
                    return FilterMatch(group, existing, index)
                }
            }
        }
        return null
    }
}
