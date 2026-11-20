package com.example.domain.mission

import com.example.domain.model.MathDifficulty
import kotlin.random.Random

data class MathProblem(
    val expression: String,
    val expectedAnswer: Int,
    val difficulty: MathDifficulty
)

class MathMissionEngine(
    private val random: Random = Random.Default
) {
    fun generate(difficulty: MathDifficulty): MathProblem = when (difficulty) {
        MathDifficulty.EASY -> generateEasy()
        MathDifficulty.MEDIUM -> generateMedium()
        MathDifficulty.HARD -> generateHard()
    }

    fun generateBatch(difficulty: MathDifficulty, count: Int): List<MathProblem> {
        val safeCount = count.coerceIn(1, 10)
        return List(safeCount) { generate(difficulty) }
    }

    fun validate(problem: MathProblem, userInput: String): Boolean {
        val parsed = userInput.trim().toIntOrNull() ?: return false
        return parsed == problem.expectedAnswer
    }

    private fun generateEasy(): MathProblem {
        val isAddition = random.nextBoolean()
        return if (isAddition) {
            val a = random.nextInt(11, 49)
            val b = random.nextInt(6, 39)
            MathProblem(
                expression = "$a + $b",
                expectedAnswer = a + b,
                difficulty = MathDifficulty.EASY
            )
        } else {
            val a = random.nextInt(24, 79)
            val b = random.nextInt(7, a - 5)
            MathProblem(
                expression = "$a − $b",
                expectedAnswer = a - b,
                difficulty = MathDifficulty.EASY
            )
        }
    }

    private fun generateMedium(): MathProblem {
        val a = random.nextInt(12, 34)
        val b = random.nextInt(3, 9)
        val product = a * b
        val isAddition = random.nextBoolean()
        return if (isAddition) {
            val c = random.nextInt(11, 47)
            MathProblem(
                expression = "$a × $b + $c",
                expectedAnswer = product + c,
                difficulty = MathDifficulty.MEDIUM
            )
        } else {
            val maxSub = (product - 10).coerceAtLeast(12)
            val c = random.nextInt(11, maxSub.coerceAtMost(49))
            MathProblem(
                expression = "$a × $b − $c",
                expectedAnswer = product - c,
                difficulty = MathDifficulty.MEDIUM
            )
        }
    }

    private fun generateHard(): MathProblem {
        val a = random.nextInt(13, 29)
        val b = random.nextInt(11, 19)
        val product = a * b
        val isAddition = random.nextBoolean()
        val c = random.nextInt(19, 69)
        return if (isAddition) {
            MathProblem(
                expression = "$a × $b + $c",
                expectedAnswer = product + c,
                difficulty = MathDifficulty.HARD
            )
        } else {
            MathProblem(
                expression = "$a × $b − $c",
                expectedAnswer = product - c,
                difficulty = MathDifficulty.HARD
            )
        }
    }
}
