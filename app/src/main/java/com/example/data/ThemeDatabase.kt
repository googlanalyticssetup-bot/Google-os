package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ThemeEntity::class], version = 1, exportSchema = false)
abstract class ThemeDatabase : RoomDatabase() {
  abstract fun themeDao(): ThemeDao

  companion object {
    @Volatile private var INSTANCE: ThemeDatabase? = null

    fun getInstance(context: Context): ThemeDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                ThemeDatabase::class.java,
                "theme_studio_offline.db",
              )
              .build()
              .also { INSTANCE = it }
        }
  }
}
