package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.domain.model.Alarm
import com.example.ui.active.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAlarmRuntime(
    val alarm: Alarm,
    val isDemo: Boolean,
    val isAudioPaused: Boolean
)

class AlarmForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var audioEngine: AlarmAudioEngine
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        audioEngine = AlarmAudioEngine(applicationContext, serviceScope)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AlarmContract.ACTION_START_ALARM -> handleStartAlarm(intent)
            AlarmContract.ACTION_PAUSE_AUDIO -> {
                audioEngine.pauseForMission()
                _activeRuntime.value = _activeRuntime.value?.copy(isAudioPaused = true)
            }
            AlarmContract.ACTION_RESUME_AUDIO -> {
                audioEngine.resumeAtFullVolume()
                _activeRuntime.value = _activeRuntime.value?.copy(isAudioPaused = false)
            }
            AlarmContract.ACTION_DISMISS,
            AlarmContract.ACTION_SNOOZE -> stopAlarmAndSelf()
        }
        return START_NOT_STICKY
    }

    private fun handleStartAlarm(intent: Intent) {
        val alarm = AlarmContract.extractAlarmFromIntent(intent)
        val isDemo = intent.getBooleanExtra(AlarmContract.EXTRA_IS_DEMO, false)

        acquireWakeLock()

        val notification = buildFullScreenNotification(alarm, isDemo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                AlarmContract.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(AlarmContract.NOTIFICATION_ID, notification)
        }

        _activeRuntime.value = ActiveAlarmRuntime(
            alarm = alarm,
            isDemo = isDemo,
            isAudioPaused = false
        )

        audioEngine.start(
            ringtoneUri = alarm.ringtoneUri,
            gradualVolume = alarm.gradualVolumeEnabled,
            vibrate = alarm.vibrationEnabled
        )

        // Launch full-screen activity directly when permitted (in addition to fullScreenIntent)
        val activityIntent = buildAlarmActivityIntent(alarm, isDemo)
        runCatching { startActivity(activityIntent) }
    }

    private fun stopAlarmAndSelf() {
        audioEngine.stop()
        releaseWakeLock()
        _activeRuntime.value = null
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(AlarmContract.NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildAlarmActivityIntent(alarm: Alarm, isDemo: Boolean): Intent {
        return Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            AlarmContract.putAlarmExtras(this, alarm, isDemo = isDemo)
        }
    }

    private fun buildFullScreenNotification(alarm: Alarm, isDemo: Boolean): Notification {
        val fullScreenIntent = buildAlarmActivityIntent(alarm, isDemo)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarm.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (alarm.label.isNotBlank()) {
            "${alarm.formattedTime12} ${alarm.amPmLabel} · ${alarm.label}"
        } else {
            "${alarm.formattedTime12} ${alarm.amPmLabel} · ${getString(R.string.notif_alarm_ringing)}"
        }

        return NotificationCompat.Builder(this, AlarmContract.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(
                if (isDemo) getString(R.string.active_demo_badge)
                else "${alarm.mission.badgeLabel()} · ${getString(R.string.notif_tap_to_solve)}"
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            AlarmContract.CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notif_channel_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            // Sound & vibration are handled directly by AlarmAudioEngine on USAGE_ALARM
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun acquireWakeLock() {
        releaseWakeLock()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Takt::ActiveAlarmWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                runCatching { lock.release() }
            }
        }
        wakeLock = null
    }

    override fun onDestroy() {
        audioEngine.stop()
        releaseWakeLock()
        _activeRuntime.value = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private val _activeRuntime = MutableStateFlow<ActiveAlarmRuntime?>(null)
        val activeRuntime: StateFlow<ActiveAlarmRuntime?> = _activeRuntime.asStateFlow()

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                this.action = action
            }
            runCatching { context.startService(intent) }
        }
    }
}
