package com.example.domain.time

import com.example.domain.model.Alarm
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object NextTriggerCalculator {

    fun calculateNextTriggerMillis(
        hour: Int,
        minute: Int,
        repeatDays: Set<DayOfWeek>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
        val todayCandidate = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (repeatDays.isEmpty()) {
            val trigger = if (todayCandidate.isAfter(now)) {
                todayCandidate
            } else {
                todayCandidate.plusDays(1)
            }
            return trigger.toInstant().toEpochMilli()
        }

        for (dayOffset in 0L..7L) {
            val candidate = todayCandidate.plusDays(dayOffset)
            if (candidate.dayOfWeek in repeatDays && candidate.isAfter(now)) {
                return candidate.toInstant().toEpochMilli()
            }
        }

        return todayCandidate.plusDays(7).toInstant().toEpochMilli()
    }

    fun calculateSnoozeTriggerMillis(
        snoozeMinutes: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val safeMinutes = snoozeMinutes.coerceAtLeast(1)
        return nowMillis + safeMinutes * 60_000L
    }

    fun findNextActiveTriggerMillis(
        alarms: List<Alarm>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        return alarms
            .asSequence()
            .filter { it.isEnabled }
            .map { alarm ->
                if (alarm.nextTriggerMillis > nowMillis) {
                    alarm.nextTriggerMillis
                } else {
                    calculateNextTriggerMillis(
                        hour = alarm.hour,
                        minute = alarm.minute,
                        repeatDays = alarm.repeatDays,
                        nowMillis = nowMillis,
                        zoneId = zoneId
                    )
                }
            }
            .minOrNull()
    }

    fun formatCountdown(
        targetMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val diffMillis = (targetMillis - nowMillis).coerceAtLeast(0L)
        val totalMinutes = Duration.ofMillis(diffMillis).toMinutes()
        if (totalMinutes < 1L) {
            return "Alarm in < 1m"
        }
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60

        return when {
            days > 0L && hours > 0L -> "Alarm in ${days}d ${hours}h"
            days > 0L -> "Alarm in ${days}d ${minutes}m"
            hours > 0L && minutes > 0L -> "Alarm in ${hours}h ${minutes}m"
            hours > 0L -> "Alarm in ${hours}h"
            else -> "Alarm in ${minutes}m"
        }
    }
}
