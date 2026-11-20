package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmContract.ACTION_TRIGGER_ALARM) return

        val alarm = AlarmContract.extractAlarmFromIntent(intent)
        val isSnoozeRing = intent.getBooleanExtra(AlarmContract.EXTRA_IS_SNOOZE_RING, false)

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            action = AlarmContract.ACTION_START_ALARM
            AlarmContract.putAlarmExtras(
                intent = this,
                alarm = alarm,
                isDemo = false,
                isSnoozeRing = isSnoozeRing
            )
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
