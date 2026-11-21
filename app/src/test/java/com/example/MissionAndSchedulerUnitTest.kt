package com.example

import com.example.domain.mission.MathMissionEngine
import com.example.domain.mission.MemoryGridEngine
import com.example.domain.mission.MemoryTapOutcome
import com.example.domain.mission.TypingMissionEngine
import com.example.domain.model.Alarm
import com.example.domain.model.MathDifficulty
import com.example.domain.time.NextTriggerCalculator
import com.example.ui.editor.AlarmEditorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.random.Random

class MissionAndSchedulerUnitTest {

    @Test
    fun mathMission_generatesValidExpressionsAcrossAllDifficulties() {
        val engine = MathMissionEngine(Random(42))

        MathDifficulty.entries.forEach { difficulty ->
            val batch = engine.generateBatch(difficulty, 10)
            assertEquals(10, batch.size)
            batch.forEach { problem ->
                assertTrue(problem.expectedAnswer > 0)
                assertTrue(engine.validate(problem, problem.expectedAnswer.toString()))
                assertFalse(engine.validate(problem, (problem.expectedAnswer + 1).toString()))
                assertFalse(engine.validate(problem, "not-a-number"))
            }
        }
    }

    @Test
    fun memoryGrid_validatesCorrectSequenceAndDetectsWrongTile() {
        val engine = MemoryGridEngine(Random(99))
        val challenge = engine.generate(gridSize = 4, roundIndex = 1)

        assertEquals(4, challenge.gridSize)
        assertEquals(16, challenge.totalCells)
        assertTrue(challenge.targetIndices.size in 5..7)

        val selected = mutableSetOf<Int>()
        val targets = challenge.targetIndices.toList()

        targets.dropLast(1).forEach { idx ->
            val outcome = engine.evaluateTap(challenge, selected, idx)
            assertEquals(MemoryTapOutcome.CORRECT_PROGRESS, outcome)
            selected.add(idx)
        }

        val wrongTile = (0 until challenge.totalCells).first { it !in challenge.targetIndices }
        assertEquals(
            MemoryTapOutcome.WRONG_TILE,
            engine.evaluateTap(challenge, selected, wrongTile)
        )

        assertEquals(
            MemoryTapOutcome.ROUND_COMPLETE,
            engine.evaluateTap(challenge, selected, targets.last())
        )
    }

    @Test
    fun typingMission_detectsPasteAndValidatesExactMatch() {
        val engine = TypingMissionEngine(Random(7))
        val challenge = engine.generate(roundIndex = 1)

        assertTrue(challenge.prompt.isNotBlank())
        assertTrue(engine.validate(challenge, challenge.prompt))
        assertFalse(engine.validate(challenge, challenge.prompt.lowercase()))
        assertFalse(engine.isPasteAttempt("TA", "TAK"))
        assertTrue(engine.isPasteAttempt("TA", "TAKT-482-KM"))
    }

    @Test
    fun nextTriggerCalculator_schedulesNextDayOrNextRepeatDayAccurately() {
        val zone = ZoneOffset.UTC
        // Monday 2026-11-23 at 08:00 UTC
        val mondayMorning = ZonedDateTime.of(2026, 11, 23, 8, 0, 0, 0, zone)
            .toInstant().toEpochMilli()

        // One-shot at 06:30 should move to Tuesday 06:30
        val nextOneShot = NextTriggerCalculator.calculateNextTriggerMillis(
            hour = 6,
            minute = 30,
            repeatDays = emptySet(),
            nowMillis = mondayMorning,
            zoneId = zone
        )
        val expectedTuesday = ZonedDateTime.of(2026, 11, 24, 6, 30, 0, 0, zone)
            .toInstant().toEpochMilli()
        assertEquals(expectedTuesday, nextOneShot)

        // Repeating Wednesday + Friday at 07:00 should pick Wednesday 07:00
        val nextRepeating = NextTriggerCalculator.calculateNextTriggerMillis(
            hour = 7,
            minute = 0,
            repeatDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            nowMillis = mondayMorning,
            zoneId = zone
        )
        val expectedWednesday = ZonedDateTime.of(2026, 11, 25, 7, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        assertEquals(expectedWednesday, nextRepeating)

        // Countdown formatting check: 6h 42m
        val countdown = NextTriggerCalculator.formatCountdown(
            targetMillis = mondayMorning + (6 * 60 + 42) * 60_000L,
            nowMillis = mondayMorning
        )
        assertEquals("Alarm in 6h 42m", countdown)
    }

    @Test
    fun nextActiveTrigger_skipsOneTimeAlarmThatAlreadyFired() {
        val zone = ZoneOffset.UTC
        val now = ZonedDateTime.of(2026, 11, 23, 16, 33, 0, 0, zone).toInstant().toEpochMilli()
        val firedAt = ZonedDateTime.of(2026, 11, 23, 16, 32, 0, 0, zone).toInstant().toEpochMilli()

        // A ringing one-time alarm must not show "Alarm in 23h 59m"
        val ringingOnce = Alarm(id = 1, hour = 16, minute = 32, nextTriggerMillis = firedAt)
        assertEquals(
            null,
            NextTriggerCalculator.findNextActiveTriggerMillis(listOf(ringingOnce), now, zone)
        )

        // A repeating alarm that just fired still counts down to its next day
        val ringingDaily = ringingOnce.copy(id = 2, repeatDays = DayOfWeek.entries.toSet())
        val expectedTomorrow = ZonedDateTime.of(2026, 11, 24, 16, 32, 0, 0, zone)
            .toInstant().toEpochMilli()
        assertEquals(
            expectedTomorrow,
            NextTriggerCalculator.findNextActiveTriggerMillis(
                listOf(ringingOnce, ringingDaily), now, zone
            )
        )
    }

    @Test
    fun defaultNewAlarmTime_roundsUpToNextFiveMinutesAtLeastOneMinuteAhead() {
        assertEquals(LocalTime.of(16, 35), AlarmEditorViewModel.defaultNewAlarmTime(LocalTime.of(16, 34, 10)))
        assertEquals(LocalTime.of(16, 40), AlarmEditorViewModel.defaultNewAlarmTime(LocalTime.of(16, 35, 0)))
        assertEquals(LocalTime.of(0, 0), AlarmEditorViewModel.defaultNewAlarmTime(LocalTime.of(23, 57)))
    }

    @Test
    fun repeatDaysBitmask_roundTripsAllDays() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)
        val mask = Alarm.daysToBitmask(days)
        assertEquals(days, Alarm.bitmaskToDays(mask))
    }
}
