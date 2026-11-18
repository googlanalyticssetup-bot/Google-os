package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {
  @Query("SELECT * FROM themes ORDER BY isBuiltIn ASC, updatedAt DESC")
  fun observeAllThemes(): Flow<List<ThemeEntity>>

  @Query("SELECT * FROM themes WHERE id = :id LIMIT 1")
  suspend fun getThemeById(id: String): ThemeEntity?

  @Query("SELECT COUNT(*) FROM themes")
  suspend fun countThemes(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTheme(theme: ThemeEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertBuiltInsIgnoreExisting(themes: List<ThemeEntity>)

  @Query("DELETE FROM themes WHERE id = :id AND isBuiltIn = 0")
  suspend fun deleteCustomTheme(id: String)
}
