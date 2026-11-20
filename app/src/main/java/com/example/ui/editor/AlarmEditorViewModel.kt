package com.example.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AlarmRepository
import com.example.domain.model.Alarm
import com.example.domain.model.MissionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class AlarmEditorUiState(
    val isEditing: Boolean = false,
    val isLoaded: Boolean = false,
    val alarmId: Long = 0L,
    val hour: Int = 6,
    val minute: Int = 30,
    val label: String = "",
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val mission: MissionConfig = MissionConfig(),
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Takt Built-in Pulse",
    val gradualVolumeEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val maxSnoozes: Int = 1,
    val snoozeDurationMinutes: Int = 5
)

class AlarmEditorViewModel(
    private val alarmId: Long?,
    private val repository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmEditorUiState(isEditing = alarmId != null && alarmId > 0L))
    val uiState: StateFlow<AlarmEditorUiState> = _uiState.asStateFlow()

    init {
        if (alarmId != null && alarmId > 0L) {
            viewModelScope.launch {
                val existing = repository.getAlarm(alarmId)
                if (existing != null) {
                    _uiState.value = AlarmEditorUiState(
                        isEditing = true,
                        isLoaded = true,
                        alarmId = existing.id,
                        hour = existing.hour,
                        minute = existing.minute,
                        label = existing.label,
                        repeatDays = existing.repeatDays,
                        mission = existing.mission,
                        ringtoneUri = existing.ringtoneUri,
                        ringtoneTitle = existing.ringtoneTitle,
                        gradualVolumeEnabled = existing.gradualVolumeEnabled,
                        vibrationEnabled = existing.vibrationEnabled,
                        maxSnoozes = existing.maxSnoozes,
                        snoozeDurationMinutes = existing.snoozeDurationMinutes
                    )
                } else {
                    _uiState.update { it.copy(isEditing = false, isLoaded = true) }
                }
            }
        } else {
            _uiState.update { it.copy(isLoaded = true) }
        }
    }

    fun updateTime(hour24: Int, minute: Int) {
        _uiState.update { it.copy(hour = hour24.coerceIn(0, 23), minute = minute.coerceIn(0, 59)) }
    }

    fun toggleRepeatDay(day: DayOfWeek) {
        _uiState.update { state ->
            val updated = if (day in state.repeatDays) {
                state.repeatDays - day
            } else {
                state.repeatDays + day
            }
            state.copy(repeatDays = updated)
        }
    }

    fun updateLabel(label: String) {
        _uiState.update { it.copy(label = label.take(36)) }
    }

    fun updateMission(mission: MissionConfig) {
        _uiState.update { it.copy(mission = mission.normalized()) }
    }

    fun updateRingtone(uri: String?, title: String) {
        _uiState.update { it.copy(ringtoneUri = uri, ringtoneTitle = title) }
    }

    fun setGradualVolume(enabled: Boolean) {
        _uiState.update { it.copy(gradualVolumeEnabled = enabled) }
    }

    fun setVibration(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
    }

    fun setMaxSnoozes(maxSnoozes: Int) {
        _uiState.update { it.copy(maxSnoozes = maxSnoozes.coerceIn(0, 2)) }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val alarm = Alarm(
            id = state.alarmId,
            hour = state.hour,
            minute = state.minute,
            label = state.label.trim(),
            repeatDays = state.repeatDays,
            isEnabled = true,
            mission = state.mission.normalized(),
            ringtoneUri = state.ringtoneUri,
            ringtoneTitle = state.ringtoneTitle,
            gradualVolumeEnabled = state.gradualVolumeEnabled,
            vibrationEnabled = state.vibrationEnabled,
            maxSnoozes = state.maxSnoozes,
            snoozeDurationMinutes = state.snoozeDurationMinutes,
            remainingSnoozes = state.maxSnoozes
        )
        viewModelScope.launch {
            repository.saveAndSchedule(alarm)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val state = _uiState.value
        if (state.alarmId <= 0L) {
            onDeleted()
            return
        }
        viewModelScope.launch {
            repository.deleteAlarm(
                Alarm(id = state.alarmId, hour = state.hour, minute = state.minute)
            )
            onDeleted()
        }
    }

    companion object {
        fun factory(
            alarmId: Long?,
            repository: AlarmRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AlarmEditorViewModel(alarmId, repository) as T
            }
        }
    }
}
