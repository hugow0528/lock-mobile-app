package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long

    @Query("SELECT * FROM locked_apps")
    fun getAllLockedAppsFlow(): Flow<List<LockedApp>>

    @Query("SELECT * FROM locked_apps")
    suspend fun getAllLockedApps(): List<LockedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockedApp(app: LockedApp)

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun deleteLockedApp(packageName: String)

    @Query("SELECT * FROM productivity_metrics ORDER BY date DESC")
    fun getAllMetricsFlow(): Flow<List<ProductivityMetric>>

    @Query("SELECT * FROM productivity_metrics WHERE date = :date LIMIT 1")
    suspend fun getMetricForDate(date: String): ProductivityMetric?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: ProductivityMetric)
}
