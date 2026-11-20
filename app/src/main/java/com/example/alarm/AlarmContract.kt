package com.example.alarm

import android.content.Intent
import com.example.domain.model.Alarm
import com.example.domain.model.MathDifficulty
import com.example.domain.model.MissionConfig
import com.example.domain.model.MissionType

object AlarmContract {
    const val CHANNEL_ID = "takt_active_alarm_channel"
    const val NOTIFICATION_ID = 7001

    const val ACTION_TRIGGER_ALARM = "com.aistudio.takt.ACTION_TRIGGER_ALARM"
    const val ACTION_START_ALARM = "com.aistudio.takt.ACTION_START_ALARM"
    const val ACTION_PAUSE_AUDIO = "com.aistudio.takt.ACTION_PAUSE_AUDIO"
    const val ACTION_RESUME_AUDIO = "com.aistudio.takt.ACTION_RESUME_AUDIO"
    const val ACTION_DISMISS = "com.aistudio.takt.ACTION_DISMISS"
    const val ACTION_SNOOZE = "com.aistudio.takt.ACTION_SNOOZE"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_IS_DEMO = "extra_is_demo"
    const val EXTRA_IS_SNOOZE_RING = "extra_is_snooze_ring"
    const val EXTRA_REMAINING_SNOOZES = "extra_remaining_snoozes"
    const val EXTRA_LABEL = "extra_label"
    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"
    const val EXTRA_MISSION_TYPE = "extra_mission_type"
    const val EXTRA_MATH_DIFFICULTY = "extra_math_difficulty"
    const val EXTRA_MISSION_ROUNDS = "extra_mission_rounds"
    const val EXTRA_GRID_SIZE = "extra_grid_size"
    const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
    const val EXTRA_GRADUAL_VOLUME = "extra_gradual_volume"
    const val EXTRA_VIBRATION = "extra_vibration"
    const val EXTRA_MAX_SNOOZES = "extra_max_snoozes"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    fun putAlarmExtras(
        intent: Intent,
        alarm: Alarm,
        isDemo: Boolean = false,
        isSnoozeRing: Boolean = false
    ): Intent = intent.apply {
        putExtra(EXTRA_ALARM_ID, alarm.id)
        putExtra(EXTRA_IS_DEMO, isDemo)
        putExtra(EXTRA_IS_SNOOZE_RING, isSnoozeRing)
        putExtra(EXTRA_REMAINING_SNOOZES, alarm.remainingSnoozes)
        putExtra(EXTRA_LABEL, alarm.label)
        putExtra(EXTRA_HOUR, alarm.hour)
        putExtra(EXTRA_MINUTE, alarm.minute)
        putExtra(EXTRA_MISSION_TYPE, alarm.mission.type.name)
        putExtra(EXTRA_MATH_DIFFICULTY, alarm.mission.mathDifficulty.name)
        putExtra(EXTRA_MISSION_ROUNDS, alarm.mission.rounds)
        putExtra(EXTRA_GRID_SIZE, alarm.mission.memoryGridSize)
        putExtra(EXTRA_RINGTONE_URI, alarm.ringtoneUri)
        putExtra(EXTRA_GRADUAL_VOLUME, alarm.gradualVolumeEnabled)
        putExtra(EXTRA_VIBRATION, alarm.vibrationEnabled)
        putExtra(EXTRA_MAX_SNOOZES, alarm.maxSnoozes)
        putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeDurationMinutes)
    }

    fun extractAlarmFromIntent(intent: Intent): Alarm {
        val missionType = intent.getStringExtra(EXTRA_MISSION_TYPE)
            ?.let { runCatching { MissionType.valueOf(it) }.getOrNull() }
            ?: MissionType.MATH
        val difficulty = intent.getStringExtra(EXTRA_MATH_DIFFICULTY)
            ?.let { runCatching { MathDifficulty.valueOf(it) }.getOrNull() }
            ?: MathDifficulty.EASY

        return Alarm(
            id = intent.getLongExtra(EXTRA_ALARM_ID, Alarm.DEMO_ALARM_ID),
            hour = intent.getIntExtra(EXTRA_HOUR, 6),
            minute = intent.getIntExtra(EXTRA_MINUTE, 30),
            label = intent.getStringExtra(EXTRA_LABEL).orEmpty(),
            isEnabled = true,
            mission = MissionConfig(
                type = missionType,
                mathDifficulty = difficulty,
                rounds = intent.getIntExtra(EXTRA_MISSION_ROUNDS, 2),
                memoryGridSize = intent.getIntExtra(EXTRA_GRID_SIZE, 3)
            ).normalized(),
            ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI),
            gradualVolumeEnabled = intent.getBooleanExtra(EXTRA_GRADUAL_VOLUME, true),
            vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION, true),
            maxSnoozes = intent.getIntExtra(EXTRA_MAX_SNOOZES, 1),
            snoozeDurationMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5),
            remainingSnoozes = intent.getIntExtra(EXTRA_REMAINING_SNOOZES, 1)
        )
    }
}
