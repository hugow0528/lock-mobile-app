package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FocusRepository
import com.example.data.FocusSession
import com.example.data.LockedApp
import com.example.data.ProductivityMetric
import com.example.service.FocusService
import com.example.service.ServiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppInfo(
    val packageName: String,
    val appName: String
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FocusRepository

    init {
        val dao = AppDatabase.getDatabase(application).focusDao()
        repository = FocusRepository(dao)
    }

    val lockedApps: StateFlow<List<LockedApp>> = repository.allLockedApps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val metrics: StateFlow<List<ProductivityMetric>> = repository.allMetrics.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sessions: StateFlow<List<FocusSession>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val serviceState: StateFlow<ServiceState> = FocusService.serviceState

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    fun loadInstalledApps(context: Context) {
        if (_installedApps.value.isNotEmpty()) return
        _isLoadingApps.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolvedInfos = pm.queryIntentActivities(mainIntent, 0)
                val list = resolvedInfos.mapNotNull { resolveInfo ->
                    val pkgName = resolveInfo.activityInfo.packageName
                    if (pkgName == context.packageName) return@mapNotNull null
                    val label = resolveInfo.loadLabel(pm).toString()
                    AppInfo(pkgName, label)
                }.distinctBy { it.packageName }.sortedBy { it.appName }

                _installedApps.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    fun toggleAppBlock(app: AppInfo, shouldBlock: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (shouldBlock) {
                repository.addLockedApp(app.packageName, app.appName)
            } else {
                repository.removeLockedApp(app.packageName)
            }
        }
    }

    fun getEmergencyAllowed(): Boolean {
        val prefs = getApplication<Application>().getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("emergency_allowed", false)
    }

    fun setEmergencyAllowed(allowed: Boolean) {
        val prefs = getApplication<Application>().getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("emergency_allowed", allowed).apply()
    }

    fun getEmergencyChances(): Int {
        val prefs = getApplication<Application>().getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("emergency_chances", 2)
    }

    fun setEmergencyChances(chances: Int) {
        val prefs = getApplication<Application>().getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("emergency_chances", chances).apply()
    }

    fun getEmergencyMinutes(): Int {
        val prefs = getApplication<Application>().getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("emergency_minutes", 5)
    }

    fun setEmergencyMinutes(minutes: Int) {
        val prefs = getApplication<Application>().getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("emergency_minutes", minutes).apply()
    }

    fun startFocusSession(
        context: Context,
        durationSeconds: Int,
        category: String,
        emergencyAllowed: Boolean,
        emergencyChances: Int,
        emergencyMinutes: Int
    ) {
        setEmergencyAllowed(emergencyAllowed)
        setEmergencyChances(emergencyChances)
        setEmergencyMinutes(emergencyMinutes)
        FocusService.startSession(context, durationSeconds, category, emergencyAllowed, emergencyChances, emergencyMinutes)
    }

    fun activateEmergency(context: Context) {
        val intent = Intent(context, FocusService::class.java).apply {
            action = FocusService.ACTION_ACTIVATE_EMERGENCY
        }
        context.startService(intent)
    }

    fun stopFocusSession(context: Context) {
        FocusService.stopSession(context)
    }
}
