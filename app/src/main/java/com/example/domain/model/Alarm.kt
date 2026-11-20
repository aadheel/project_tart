package com.example.domain.model

import java.time.DayOfWeek

enum class MissionType {
    NONE,
    MATH,
    MEMORY_GRID,
    TYPING
}

enum class MathDifficulty(val label: String, val example: String) {
    EASY("Easy", "14 + 9"),
    MEDIUM("Medium", "27 × 4 + 13"),
    HARD("Hard", "18 × 14 − 37")
}

data class MissionConfig(
    val type: MissionType = MissionType.MATH,
    val mathDifficulty: MathDifficulty = MathDifficulty.MEDIUM,
    val rounds: Int = 3,
    val memoryGridSize: Int = 3
) {
    fun normalized(): MissionConfig = when (type) {
        MissionType.NONE -> copy(rounds = 1)
        MissionType.MATH -> copy(rounds = rounds.coerceIn(1, 10))
        MissionType.MEMORY_GRID -> copy(
            rounds = rounds.coerceIn(1, 5),
            memoryGridSize = if (memoryGridSize == 4) 4 else 3
        )
        MissionType.TYPING -> copy(rounds = rounds.coerceIn(1, 5))
    }

    fun badgeLabel(): String = when (type) {
        MissionType.NONE -> "No Mission"
        MissionType.MATH -> "Math · ${mathDifficulty.label} × $rounds"
        MissionType.MEMORY_GRID -> "Grid ${memoryGridSize}×${memoryGridSize} · × $rounds"
        MissionType.TYPING -> "Typing · × $rounds"
    }
}

data class Alarm(
    val id: Long = 0L,
    val hour: Int = 6,
    val minute: Int = 30,
    val label: String = "",
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val isEnabled: Boolean = true,
    val mission: MissionConfig = MissionConfig(),
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Takt Built-in Pulse",
    val gradualVolumeEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val maxSnoozes: Int = 1,
    val snoozeDurationMinutes: Int = 5,
    val remainingSnoozes: Int = maxSnoozes,
    val nextTriggerMillis: Long = 0L
) {
    val isRepeating: Boolean get() = repeatDays.isNotEmpty()

    val displayHour12: Int
        get() {
            val h = hour % 12
            return if (h == 0) 12 else h
        }

    val isPm: Boolean get() = hour >= 12

    val amPmLabel: String get() = if (isPm) "PM" else "AM"

    val formattedTime12: String
        get() = "%02d:%02d".format(displayHour12, minute)

    companion object {
        val ORDERED_DAYS: List<DayOfWeek> = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        fun dayInitial(day: DayOfWeek): String = when (day) {
            DayOfWeek.MONDAY -> "M"
            DayOfWeek.TUESDAY -> "T"
            DayOfWeek.WEDNESDAY -> "W"
            DayOfWeek.THURSDAY -> "T"
            DayOfWeek.FRIDAY -> "F"
            DayOfWeek.SATURDAY -> "S"
            DayOfWeek.SUNDAY -> "S"
        }

        fun daysToBitmask(days: Set<DayOfWeek>): Int {
            var mask = 0
            for (day in days) {
                mask = mask or (1 shl (day.value - 1))
            }
            return mask
        }

        fun bitmaskToDays(mask: Int): Set<DayOfWeek> {
            val result = mutableSetOf<DayOfWeek>()
            for (day in ORDERED_DAYS) {
                if ((mask and (1 shl (day.value - 1))) != 0) {
                    result.add(day)
                }
            }
            return result
        }

        fun createDemoAlarm(): Alarm = Alarm(
            id = DEMO_ALARM_ID,
            hour = 6,
            minute = 30,
            label = "Demo Wake Mission",
            repeatDays = emptySet(),
            isEnabled = false,
            mission = MissionConfig(
                type = MissionType.MATH,
                mathDifficulty = MathDifficulty.EASY,
                rounds = 2
            ),
            ringtoneUri = null,
            ringtoneTitle = "Takt Built-in Pulse",
            gradualVolumeEnabled = true,
            vibrationEnabled = true,
            maxSnoozes = 1,
            snoozeDurationMinutes = 5,
            remainingSnoozes = 1
        )

        const val DEMO_ALARM_ID = -99L
    }
}
