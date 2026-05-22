package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FocusRepository(private val focusDao: FocusDao) {

    val allSessions: Flow<List<FocusSession>> = focusDao.getAllSessionsFlow()
    val allLockedApps: Flow<List<LockedApp>> = focusDao.getAllLockedAppsFlow()
    val allMetrics: Flow<List<ProductivityMetric>> = focusDao.getAllMetricsFlow()

    fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    suspend fun getActiveLockedPackages(): Set<String> {
        return focusDao.getAllLockedApps().map { it.packageName }.toSet()
    }

    suspend fun insertSession(session: FocusSession): Long {
        return focusDao.insertSession(session)
    }

    suspend fun addLockedApp(packageName: String, appName: String) {
        focusDao.insertLockedApp(LockedApp(packageName, appName, true))
    }

    suspend fun removeLockedApp(packageName: String) {
        focusDao.deleteLockedApp(packageName)
    }

    suspend fun getTodayMetric(): ProductivityMetric {
        val today = getTodayDateString()
        return focusDao.getMetricForDate(today) ?: ProductivityMetric(today, 0, 0, 0)
    }

    suspend fun incrementFocusMinutes(minutes: Int) {
        val today = getTodayDateString()
        val current = focusDao.getMetricForDate(today) ?: ProductivityMetric(today, 0, 0, 0)
        val updated = current.copy(
            focusMinutes = current.focusMinutes + minutes,
            completedSessions = current.completedSessions + 1
        )
        focusDao.insertMetric(updated)
    }

    suspend fun incrementDistractionsBlocked() {
        val today = getTodayDateString()
        val current = focusDao.getMetricForDate(today) ?: ProductivityMetric(today, 0, 0, 0)
        val updated = current.copy(
            distractionsBlocked = current.distractionsBlocked + 1
        )
        focusDao.insertMetric(updated)
    }
}
