package com.example.ui.active

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.TaktApplication
import com.example.alarm.AlarmContract
import com.example.alarm.AlarmForegroundService
import com.example.domain.model.Alarm
import com.example.ui.theme.TaktTheme
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    private var currentAlarm: Alarm = Alarm.createDemoAlarm()
    private var isDemo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenWindow()
        extractLaunchPayload(intent)

        val container = (application as TaktApplication).container

        setContent {
            val isDarkMode by container.themePreferences.isDarkMode
                .collectAsStateWithLifecycle(initialValue = true)

            LaunchedEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            TaktTheme(darkTheme = isDarkMode) {
                ActiveAlarmScreen(
                    alarm = currentAlarm,
                    isDemo = isDemo,
                    onPauseAudio = {
                        AlarmForegroundService.sendAction(this, AlarmContract.ACTION_PAUSE_AUDIO)
                    },
                    onResumeAudio = {
                        AlarmForegroundService.sendAction(this, AlarmContract.ACTION_RESUME_AUDIO)
                    },
                    onSnooze = {
                        lifecycleScope.launch {
                            container.alarmRepository.handleAlarmSnoozed(currentAlarm, isDemo)
                            AlarmForegroundService.sendAction(this@AlarmActivity, AlarmContract.ACTION_SNOOZE)
                            finish()
                        }
                    },
                    onMissionSolved = {
                        AlarmForegroundService.sendAction(this, AlarmContract.ACTION_DISMISS)
                        lifecycleScope.launch {
                            container.alarmRepository.handleAlarmDismissed(currentAlarm.id)
                        }
                    },
                    onCloseFinished = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractLaunchPayload(intent)
    }

    private fun extractLaunchPayload(intent: Intent?) {
        val runtime = AlarmForegroundService.activeRuntime.value
        if (intent != null && intent.hasExtra(AlarmContract.EXTRA_ALARM_ID)) {
            currentAlarm = AlarmContract.extractAlarmFromIntent(intent)
            isDemo = intent.getBooleanExtra(AlarmContract.EXTRA_IS_DEMO, false)
        } else if (runtime != null) {
            currentAlarm = runtime.alarm
            isDemo = runtime.isDemo
        }
    }

    private fun configureLockScreenWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager != null && keyguardManager.isKeyguardLocked) {
            runCatching { keyguardManager.requestDismissKeyguard(this, null) }
        }
    }
}
