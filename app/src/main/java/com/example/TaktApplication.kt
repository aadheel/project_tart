package com.example

import android.app.Application
import com.example.alarm.AlarmScheduler
import com.example.data.local.TaktDatabase
import com.example.data.local.ThemePreferencesRepository
import com.example.data.repository.AlarmRepository

class AppContainer(application: Application) {
    private val database: TaktDatabase = TaktDatabase.getInstance(application)
    val alarmScheduler: AlarmScheduler = AlarmScheduler(application)
    val alarmRepository: AlarmRepository = AlarmRepository(
        alarmDao = database.alarmDao(),
        scheduler = alarmScheduler
    )
    val themePreferences: ThemePreferencesRepository = ThemePreferencesRepository(application)
}

class TaktApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
