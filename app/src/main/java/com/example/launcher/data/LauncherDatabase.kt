package com.example.launcher.data

import android.content.Context
import androidx.room.*
import com.example.launcher.model.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "home_items")
data class HomeItemEntity(
  @PrimaryKey val id: String,
  val type: String,
  val page: Int,
  val row: Int,
  val col: Int,
  val spanX: Int,
  val spanY: Int,
  val packageName: String,
  val activityName: String,
  val customLabel: String?,
  val customIconStyle: String?,
  val folderTitle: String,
  val folderColorHex: Long,
  val folderStyle: String,
  val folderLayout: String,
  val folderAppPackagesCsv: String,
  val customWidgetKind: String?,
  val appWidgetId: Int,
  val widgetTransparent: Boolean,
  val widgetGlass: Boolean,
  val photoUri: String?,
  val noteText: String,
)

@Entity(tableName = "app_usage")
data class AppUsageEntity(
  @PrimaryKey val packageName: String,
  val launchCount: Int,
  val lastLaunchedAt: Long,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
  @PrimaryKey val query: String,
  val searchedAt: Long,
)

@Dao
interface LauncherDao {
  @Query("SELECT * FROM home_items ORDER BY page ASC, row ASC, col ASC")
  fun observeHomeItems(): Flow<List<HomeItemEntity>>

  @Query("SELECT COUNT(*) FROM home_items")
  suspend fun countHomeItems(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHomeItem(item: HomeItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHomeItems(items: List<HomeItemEntity>)

  @Query("DELETE FROM home_items WHERE id = :id")
  suspend fun deleteHomeItem(id: String)

  @Query("DELETE FROM home_items")
  suspend fun clearAllHomeItems()

  @Query("SELECT * FROM app_usage")
  fun observeAppUsage(): Flow<List<AppUsageEntity>>

  @Query("SELECT * FROM app_usage WHERE packageName = :pkg LIMIT 1")
  suspend fun getUsage(pkg: String): AppUsageEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertUsage(usage: AppUsageEntity)

  @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 12")
  fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSearchHistory(entry: SearchHistoryEntity)

  @Query("DELETE FROM search_history")
  suspend fun clearSearchHistory()
}

@Database(
  entities = [HomeItemEntity::class, AppUsageEntity::class, SearchHistoryEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class LauncherDatabase : RoomDatabase() {
  abstract fun launcherDao(): LauncherDao

  companion object {
    @Volatile private var INSTANCE: LauncherDatabase? = null

    fun getInstance(context: Context): LauncherDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                LauncherDatabase::class.java,
                "launcher_layout_offline.db",
              )
              .build()
              .also { INSTANCE = it }
        }
  }
}

fun HomeItemEntity.toDomain(): HomeItem =
  HomeItem(
    id = id,
    type = HomeItemType.entries.firstOrNull { it.name == type } ?: HomeItemType.APP,
    page = page,
    row = row,
    col = col,
    spanX = spanX,
    spanY = spanY,
    packageName = packageName,
    activityName = activityName,
    customLabel = customLabel,
    customIconStyle = customIconStyle?.let { s -> LauncherIconStyle.entries.firstOrNull { it.name == s } },
    folderTitle = folderTitle,
    folderColorHex = folderColorHex,
    folderStyle = FolderStyle.entries.firstOrNull { it.name == folderStyle } ?: FolderStyle.GLASS,
    folderLayout = FolderLayout.entries.firstOrNull { it.name == folderLayout } ?: FolderLayout.GRID,
    folderAppPackages = folderAppPackagesCsv.split("|").filter { it.isNotBlank() },
    customWidgetKind = customWidgetKind?.let { k -> CustomWidgetKind.entries.firstOrNull { it.name == k } },
    appWidgetId = appWidgetId,
    widgetTransparent = widgetTransparent,
    widgetGlass = widgetGlass,
    photoUri = photoUri,
    noteText = noteText,
  )

fun HomeItem.toEntity(): HomeItemEntity =
  HomeItemEntity(
    id = id,
    type = type.name,
    page = page,
    row = row,
    col = col,
    spanX = spanX,
    spanY = spanY,
    packageName = packageName,
    activityName = activityName,
    customLabel = customLabel,
    customIconStyle = customIconStyle?.name,
    folderTitle = folderTitle,
    folderColorHex = folderColorHex,
    folderStyle = folderStyle.name,
    folderLayout = folderLayout.name,
    folderAppPackagesCsv = folderAppPackages.joinToString("|"),
    customWidgetKind = customWidgetKind?.name,
    appWidgetId = appWidgetId,
    widgetTransparent = widgetTransparent,
    widgetGlass = widgetGlass,
    photoUri = photoUri,
    noteText = noteText,
  )
