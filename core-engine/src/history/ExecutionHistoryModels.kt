package org.andrew.jpad.history

import java.util.UUID

data class ExecutionSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val tabTitle: String,
    val languageMode: String,
    val code: String,
    val status: String,
    val durationMs: Long,
    val consoleOutput: String = "",
    val dumpCount: Int = 0
)

enum class DiffLineType {
    UNCHANGED,
    ADDED,
    REMOVED
}

data class DiffLine(
    val type: DiffLineType,
    val text: String,
    val oldLineNumber: Int? = null,
    val newLineNumber: Int? = null
)

data class DiffResult(
    val lines: List<DiffLine>,
    val addedCount: Int,
    val removedCount: Int,
    val unchangedCount: Int
)

object TextDiffUtil {
    fun computeDiff(oldText: String, newText: String): DiffResult {
        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val newLines = if (newText.isEmpty()) emptyList() else newText.lines()

        val m = oldLines.size
        val n = newLines.size

        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 1..m) {
            for (j in 1..n) {
                if (oldLines[i - 1] == newLines[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        val result = mutableListOf<DiffLine>()
        var i = m
        var j = n

        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1] -> {
                    result.add(DiffLine(DiffLineType.UNCHANGED, oldLines[i - 1], i, j))
                    i--
                    j--
                }
                j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) -> {
                    result.add(DiffLine(DiffLineType.ADDED, newLines[j - 1], null, j))
                    j--
                }
                i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j]) -> {
                    result.add(DiffLine(DiffLineType.REMOVED, oldLines[i - 1], i, null))
                    i--
                }
            }
        }

        result.reverse()

        var added = 0
        var removed = 0
        var unchanged = 0
        for (line in result) {
            when (line.type) {
                DiffLineType.ADDED -> added++
                DiffLineType.REMOVED -> removed++
                DiffLineType.UNCHANGED -> unchanged++
            }
        }

        return DiffResult(result, added, removed, unchanged)
    }
}
