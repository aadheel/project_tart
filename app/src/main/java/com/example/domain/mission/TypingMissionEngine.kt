package com.example.domain.mission

import kotlin.random.Random

data class TypingChallenge(
    val prompt: String
)

class TypingMissionEngine(
    private val random: Random = Random.Default
) {
    private val curatedPhrases = listOf(
        "WAKE WITH PURPOSE",
        "FIRST LIGHT BEGINS",
        "CLEAR MIND TODAY",
        "START ON TIME NOW",
        "SHARP FOCUS AHEAD",
        "QUIET MORNING AIR",
        "RISE AND DELIVER",
        "STEADY STEP FORWARD",
        "ZERO SNOOZE TODAY",
        "PRECISION IN MOTION"
    )

    private val codePrefixes = listOf(
        "TAKT", "RAMS", "KRON", "PULS", "NORD", "SOLA", "VIGL", "ZURK"
    )

    // Excludes ambiguous characters: 0/O, 1/I/L
    private val codeAlphabet = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"

    fun generate(roundIndex: Int = 0): TypingChallenge {
        val useCode = (roundIndex % 2 == 1) || random.nextBoolean()
        val prompt = if (useCode) {
            val prefix = codePrefixes[random.nextInt(codePrefixes.size)]
            val digits = buildString {
                repeat(3) { append(random.nextInt(2, 10)) }
            }
            val suffix = buildString {
                repeat(2) { append(codeAlphabet[random.nextInt(codeAlphabet.length)]) }
            }
            "$prefix-$digits-$suffix"
        } else {
            curatedPhrases[random.nextInt(curatedPhrases.size)]
        }
        return TypingChallenge(prompt = prompt)
    }

    fun isPasteAttempt(previousValue: String, candidateValue: String): Boolean {
        val delta = candidateValue.length - previousValue.length
        return delta > 1
    }

    fun validate(challenge: TypingChallenge, userInput: String): Boolean {
        return userInput.trim().equals(challenge.prompt, ignoreCase = false)
    }
}
