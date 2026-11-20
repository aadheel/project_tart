package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.domain.model.Alarm

class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(alarm: Alarm, triggerMillis: Long): Boolean {
        if (!canScheduleExactAlarms()) return false

        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val operation = buildTriggerPendingIntent(
            alarm = alarm,
            isSnoozeRing = false
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showIntent)
        return runCatching {
            alarmManager.setAlarmClock(clockInfo, operation)
            true
        }.getOrDefault(false)
    }

    fun scheduleSnooze(alarm: Alarm, snoozeTriggerMillis: Long): Boolean {
        if (!canScheduleExactAlarms()) return false

        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val operation = buildTriggerPendingIntent(
            alarm = alarm,
            isSnoozeRing = true
        )

        val clockInfo = AlarmManager.AlarmClockInfo(snoozeTriggerMillis, showIntent)
        return runCatching {
            alarmManager.setAlarmClock(clockInfo, operation)
            true
        }.getOrDefault(false)
    }

    fun cancel(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmContract.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun triggerDemoAlarmNow(demoAlarm: Alarm = Alarm.createDemoAlarm()) {
        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            action = AlarmContract.ACTION_START_ALARM
            AlarmContract.putAlarmExtras(this, demoAlarm, isDemo = true, isSnoozeRing = false)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun buildTriggerPendingIntent(
        alarm: Alarm,
        isSnoozeRing: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmContract.ACTION_TRIGGER_ALARM
            AlarmContract.putAlarmExtras(
                intent = this,
                alarm = alarm,
                isDemo = false,
                isSnoozeRing = isSnoozeRing
            )
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
