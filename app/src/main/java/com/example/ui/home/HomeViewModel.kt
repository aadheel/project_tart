package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmPermissionStatus
import com.example.data.local.ThemePreferencesRepository
import com.example.data.repository.AlarmRepository
import com.example.domain.model.Alarm
import com.example.domain.time.NextTriggerCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val alarms: List<Alarm> = emptyList(),
    val nextAlarmCountdown: String? = null,
    val isDarkMode: Boolean = true,
    val permissions: AlarmPermissionStatus = AlarmPermissionStatus()
)

class HomeViewModel(
    private val alarmRepository: AlarmRepository,
    private val themePreferences: ThemePreferencesRepository
) : ViewModel() {

    private val permissionStatus = MutableStateFlow(AlarmPermissionStatus())
    private var lastDeletedAlarm: Alarm? = null

    private val clockTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(15_000L)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        alarmRepository.alarms,
        themePreferences.isDarkMode,
        permissionStatus,
        clockTicker
    ) { alarms, isDark, perms, nowMillis ->
        val nextTrigger = NextTriggerCalculator.findNextActiveTriggerMillis(
            alarms = alarms,
            nowMillis = nowMillis
        )
        val countdown = nextTrigger?.let {
            NextTriggerCalculator.formatCountdown(targetMillis = it, nowMillis = nowMillis)
        }
        HomeUiState(
            alarms = alarms,
            nextAlarmCountdown = countdown,
            isDarkMode = isDark,
            permissions = perms
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState()
    )

    fun updatePermissions(status: AlarmPermissionStatus) {
        permissionStatus.value = status
    }

    fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            alarmRepository.setEnabled(alarm, enabled)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        lastDeletedAlarm = alarm
        viewModelScope.launch {
            alarmRepository.deleteAlarm(alarm)
        }
    }

    fun undoDelete() {
        val toRestore = lastDeletedAlarm ?: return
        lastDeletedAlarm = null
        viewModelScope.launch {
            alarmRepository.restoreAlarm(toRestore)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            themePreferences.toggleTheme()
        }
    }

    fun triggerDemoAlarm() {
        alarmRepository.triggerDemoAlarm()
    }

    companion object {
        fun factory(
            alarmRepository: AlarmRepository,
            themePreferences: ThemePreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(alarmRepository, themePreferences) as T
            }
        }
    }
}
