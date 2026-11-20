package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Alarm
import com.example.domain.model.MathDifficulty
import com.example.domain.model.MissionConfig
import com.example.domain.model.MissionType

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDaysBitmask: Int,
    val isEnabled: Boolean,
    val missionType: String,
    val mathDifficulty: String,
    val missionRounds: Int,
    val memoryGridSize: Int,
    val ringtoneUri: String?,
    val ringtoneTitle: String,
    val gradualVolumeEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val maxSnoozes: Int,
    val snoozeDurationMinutes: Int,
    val remainingSnoozes: Int,
    val nextTriggerMillis: Long
)

fun AlarmEntity.toDomain(): Alarm {
    val parsedType = runCatching { MissionType.valueOf(missionType) }
        .getOrDefault(MissionType.MATH)
    val parsedDifficulty = runCatching { MathDifficulty.valueOf(mathDifficulty) }
        .getOrDefault(MathDifficulty.MEDIUM)

    return Alarm(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        repeatDays = Alarm.bitmaskToDays(repeatDaysBitmask),
        isEnabled = isEnabled,
        mission = MissionConfig(
            type = parsedType,
            mathDifficulty = parsedDifficulty,
            rounds = missionRounds,
            memoryGridSize = memoryGridSize
        ).normalized(),
        ringtoneUri = ringtoneUri,
        ringtoneTitle = ringtoneTitle,
        gradualVolumeEnabled = gradualVolumeEnabled,
        vibrationEnabled = vibrationEnabled,
        maxSnoozes = maxSnoozes,
        snoozeDurationMinutes = snoozeDurationMinutes,
        remainingSnoozes = remainingSnoozes,
        nextTriggerMillis = nextTriggerMillis
    )
}

fun Alarm.toEntity(): AlarmEntity {
    val normalizedMission = mission.normalized()
    return AlarmEntity(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        repeatDaysBitmask = Alarm.daysToBitmask(repeatDays),
        isEnabled = isEnabled,
        missionType = normalizedMission.type.name,
        mathDifficulty = normalizedMission.mathDifficulty.name,
        missionRounds = normalizedMission.rounds,
        memoryGridSize = normalizedMission.memoryGridSize,
        ringtoneUri = ringtoneUri,
        ringtoneTitle = ringtoneTitle,
        gradualVolumeEnabled = gradualVolumeEnabled,
        vibrationEnabled = vibrationEnabled,
        maxSnoozes = maxSnoozes,
        snoozeDurationMinutes = snoozeDurationMinutes,
        remainingSnoozes = remainingSnoozes,
        nextTriggerMillis = nextTriggerMillis
    )
}
