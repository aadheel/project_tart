package com.example.domain.mission

import kotlin.random.Random

data class MemoryGridChallenge(
    val gridSize: Int,
    val targetIndices: Set<Int>
) {
    val totalCells: Int get() = gridSize * gridSize
}

enum class MemoryTapOutcome {
    ALREADY_SELECTED,
    CORRECT_PROGRESS,
    ROUND_COMPLETE,
    WRONG_TILE
}

class MemoryGridEngine(
    private val random: Random = Random.Default
) {
    fun generate(gridSize: Int, roundIndex: Int = 0): MemoryGridChallenge {
        val normalizedSize = if (gridSize == 4) 4 else 3
        val totalCells = normalizedSize * normalizedSize
        val targetCount = when (normalizedSize) {
            3 -> (4 + (roundIndex / 2)).coerceAtMost(5)
            else -> (5 + (roundIndex / 2)).coerceAtMost(7)
        }
        val indices = (0 until totalCells).shuffled(random).take(targetCount).toSet()
        return MemoryGridChallenge(
            gridSize = normalizedSize,
            targetIndices = indices
        )
    }

    fun evaluateTap(
        challenge: MemoryGridChallenge,
        selectedIndices: Set<Int>,
        tappedIndex: Int
    ): MemoryTapOutcome {
        if (tappedIndex !in 0 until challenge.totalCells) {
            return MemoryTapOutcome.WRONG_TILE
        }
        if (tappedIndex in selectedIndices) {
            return MemoryTapOutcome.ALREADY_SELECTED
        }
        if (tappedIndex !in challenge.targetIndices) {
            return MemoryTapOutcome.WRONG_TILE
        }
        val updated = selectedIndices + tappedIndex
        return if (updated == challenge.targetIndices) {
            MemoryTapOutcome.ROUND_COMPLETE
        } else {
            MemoryTapOutcome.CORRECT_PROGRESS
        }
    }
}
