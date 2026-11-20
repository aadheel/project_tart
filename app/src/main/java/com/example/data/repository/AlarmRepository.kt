package com.example.data.repository

import com.example.alarm.AlarmScheduler
import com.example.data.local.AlarmDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.domain.model.Alarm
import com.example.domain.time.NextTriggerCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val scheduler: AlarmScheduler
) {
    val alarms: Flow<List<Alarm>> = alarmDao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAlarm(id: Long): Alarm? = alarmDao.getById(id)?.toDomain()

    suspend fun saveAndSchedule(alarm: Alarm): Alarm {
        val normalizedMission = alarm.mission.normalized()
        val nextTrigger = if (alarm.isEnabled) {
            NextTriggerCalculator.calculateNextTriggerMillis(
                hour = alarm.hour,
                minute = alarm.minute,
                repeatDays = alarm.repeatDays
            )
        } else {
            0L
        }

        val prepared = alarm.copy(
            mission = normalizedMission,
            remainingSnoozes = alarm.maxSnoozes,
            nextTriggerMillis = nextTrigger
        )

        val savedId = alarmDao.upsert(prepared.toEntity())
        val persisted = prepared.copy(id = if (prepared.id == 0L) savedId else prepared.id)

        if (persisted.isEnabled) {
            scheduler.schedule(persisted, persisted.nextTriggerMillis)
        } else {
            scheduler.cancel(persisted.id)
        }
        return persisted
    }

    suspend fun setEnabled(alarm: Alarm, enabled: Boolean) {
        val nextTrigger = if (enabled) {
            NextTriggerCalculator.calculateNextTriggerMillis(
                hour = alarm.hour,
                minute = alarm.minute,
                repeatDays = alarm.repeatDays
            )
        } else {
            0L
        }
        val updated = alarm.copy(
            isEnabled = enabled,
            remainingSnoozes = alarm.maxSnoozes,
            nextTriggerMillis = nextTrigger
        )
        alarmDao.updateScheduleState(
            id = updated.id,
            enabled = enabled,
            nextTriggerMillis = nextTrigger,
            remainingSnoozes = updated.maxSnoozes
        )
        if (enabled) {
            scheduler.schedule(updated, nextTrigger)
        } else {
            scheduler.cancel(updated.id)
        }
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        scheduler.cancel(alarm.id)
        alarmDao.deleteById(alarm.id)
    }

    suspend fun restoreAlarm(alarm: Alarm) {
        saveAndSchedule(alarm)
    }

    suspend fun handleAlarmDismissed(alarmId: Long) {
        if (alarmId == Alarm.DEMO_ALARM_ID) return
        val alarm = alarmDao.getById(alarmId)?.toDomain() ?: return

        if (alarm.isRepeating) {
            val nextTrigger = NextTriggerCalculator.calculateNextTriggerMillis(
                hour = alarm.hour,
                minute = alarm.minute,
                repeatDays = alarm.repeatDays
            )
            val updated = alarm.copy(
                isEnabled = true,
                remainingSnoozes = alarm.maxSnoozes,
                nextTriggerMillis = nextTrigger
            )
            alarmDao.updateScheduleState(
                id = updated.id,
                enabled = true,
                nextTriggerMillis = nextTrigger,
                remainingSnoozes = updated.maxSnoozes
            )
            scheduler.schedule(updated, nextTrigger)
        } else {
            alarmDao.updateScheduleState(
                id = alarm.id,
                enabled = false,
                nextTriggerMillis = 0L,
                remainingSnoozes = alarm.maxSnoozes
            )
            scheduler.cancel(alarm.id)
        }
    }

    suspend fun handleAlarmSnoozed(alarm: Alarm, isDemo: Boolean): Long {
        val remainingAfterSnooze = (alarm.remainingSnoozes - 1).coerceAtLeast(0)
        val snoozeTriggerMillis = NextTriggerCalculator.calculateSnoozeTriggerMillis(
            snoozeMinutes = alarm.snoozeDurationMinutes
        )

        if (!isDemo && alarm.id != Alarm.DEMO_ALARM_ID) {
            val snoozed = alarm.copy(
                isEnabled = true,
                remainingSnoozes = remainingAfterSnooze,
                nextTriggerMillis = snoozeTriggerMillis
            )
            alarmDao.updateScheduleState(
                id = snoozed.id,
                enabled = true,
                nextTriggerMillis = snoozeTriggerMillis,
                remainingSnoozes = remainingAfterSnooze
            )
            scheduler.scheduleSnooze(snoozed, snoozeTriggerMillis)
        }
        return snoozeTriggerMillis
    }

    suspend fun rescheduleAllEnabled() {
        val enabledAlarms = alarmDao.getAllEnabled().map { it.toDomain() }
        val now = System.currentTimeMillis()
        for (alarm in enabledAlarms) {
            val nextTrigger = if (alarm.nextTriggerMillis > now) {
                alarm.nextTriggerMillis
            } else {
                NextTriggerCalculator.calculateNextTriggerMillis(
                    hour = alarm.hour,
                    minute = alarm.minute,
                    repeatDays = alarm.repeatDays,
                    nowMillis = now
                )
            }
            val updated = alarm.copy(nextTriggerMillis = nextTrigger)
            alarmDao.updateScheduleState(
                id = updated.id,
                enabled = true,
                nextTriggerMillis = nextTrigger,
                remainingSnoozes = updated.remainingSnoozes
            )
            scheduler.schedule(updated, nextTrigger)
        }
    }

    fun triggerDemoAlarm() {
        scheduler.triggerDemoAlarmNow()
    }
}
