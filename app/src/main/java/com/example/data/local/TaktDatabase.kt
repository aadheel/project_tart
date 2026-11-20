package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AlarmEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TaktDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: TaktDatabase? = null

        fun getInstance(context: Context): TaktDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaktDatabase::class.java,
                    "takt_alarms.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
