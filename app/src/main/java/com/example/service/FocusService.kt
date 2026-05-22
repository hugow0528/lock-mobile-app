package com.example.service

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.BlockedActivity
import com.example.data.AppDatabase
import com.example.data.FocusRepository
import com.example.data.FocusSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FocusService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var repository: FocusRepository
    private var tickerJob: Job? = null

    companion object {
        const val ACTION_START_FOCUS = "com.example.action.START_FOCUS"
        const val ACTION_STOP_FOCUS = "com.example.action.STOP_FOCUS"
        const val ACTION_ACTIVATE_EMERGENCY = "com.example.action.ACTIVATE_EMERGENCY"
        const val EXTRA_DURATION = "duration_seconds"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_EMERGENCY_ALLOWED = "emergency_allowed"
        const val EXTRA_EMERGENCY_CHANCES = "emergency_chances"
        const val EXTRA_EMERGENCY_MINUTES = "emergency_minutes"

        private const val CHANNEL_ID = "focus_service_channel"
        private const val NOTIFICATION_ID = 1001

        private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Idle)
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

        fun startSession(
            context: Context,
            durationSeconds: Int,
            category: String,
            emergencyAllowed: Boolean,
            emergencyChances: Int,
            emergencyMinutes: Int
        ) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_START_FOCUS
                putExtra(EXTRA_DURATION, durationSeconds)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_EMERGENCY_ALLOWED, emergencyAllowed)
                putExtra(EXTRA_EMERGENCY_CHANCES, emergencyChances)
                putExtra(EXTRA_EMERGENCY_MINUTES, emergencyMinutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopSession(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_STOP_FOCUS
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val dao = AppDatabase.getDatabase(this).focusDao()
        repository = FocusRepository(dao)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOCUS -> {
                val duration = intent.getIntExtra(EXTRA_DURATION, 1500)
                val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Work"
                val emergencyAllowed = intent.getBooleanExtra(EXTRA_EMERGENCY_ALLOWED, false)
                val emergencyChances = intent.getIntExtra(EXTRA_EMERGENCY_CHANCES, 2)
                val emergencyMinutes = intent.getIntExtra(EXTRA_EMERGENCY_MINUTES, 5)
                startFocusSession(duration, category, emergencyAllowed, emergencyChances, emergencyMinutes)
            }
            ACTION_STOP_FOCUS -> {
                stopFocusSession()
            }
            ACTION_ACTIVATE_EMERGENCY -> {
                activateEmergencyBypass()
            }
        }
        return START_NOT_STICKY
    }

    private fun activateEmergencyBypass() {
        val currentState = _serviceState.value
        if (currentState is ServiceState.Active && currentState.emergencyAllowed && currentState.emergencyChancesRemaining > 0) {
            val bypassDurationMs = currentState.emergencyDurationMinutes * 60 * 1000L
            val bypassUntil = System.currentTimeMillis() + bypassDurationMs
            _serviceState.value = currentState.copy(
                emergencyChancesRemaining = currentState.emergencyChancesRemaining - 1,
                emergencyBypassUntilMillis = bypassUntil
            )
        }
    }

    private fun startFocusSession(
        durationSeconds: Int,
        category: String,
        emergencyAllowed: Boolean,
        emergencyChances: Int,
        emergencyMinutes: Int
    ) {
        createNotificationChannel()
        val notification = buildNotification(durationSeconds)
        startForeground(NOTIFICATION_ID, notification)

        tickerJob?.cancel()
        val startTime = System.currentTimeMillis()
        _serviceState.value = ServiceState.Active(
            remainingSeconds = durationSeconds,
            totalSeconds = durationSeconds,
            category = category,
            startTimeMillis = startTime,
            emergencyAllowed = emergencyAllowed,
            emergencyChancesMax = emergencyChances,
            emergencyChancesRemaining = emergencyChances,
            emergencyDurationMinutes = emergencyMinutes,
            emergencyBypassUntilMillis = 0L
        )

        tickerJob = serviceScope.launch {
            var remaining = durationSeconds
            var lastBlockedDetectionTime = 0L

            while (remaining > 0) {
                delay(1000)
                remaining--
                val currentState = _serviceState.value
                if (currentState is ServiceState.Active) {
                    _serviceState.value = currentState.copy(remainingSeconds = remaining)
                } else {
                    _serviceState.value = ServiceState.Active(
                        remainingSeconds = remaining,
                        totalSeconds = durationSeconds,
                        category = category,
                        startTimeMillis = startTime,
                        emergencyAllowed = emergencyAllowed,
                        emergencyChancesMax = emergencyChances,
                        emergencyChancesRemaining = emergencyChances,
                        emergencyDurationMinutes = emergencyMinutes,
                        emergencyBypassUntilMillis = 0L
                    )
                }

                updateNotification(remaining)

                if (hasUsageAccessPermission()) {
                    val currentPkg = getForegroundPackage()
                    if (currentPkg != null && currentPkg != packageName) {
                        val lockedPkgs = repository.getActiveLockedPackages()
                        if (lockedPkgs.contains(currentPkg)) {
                            val activeState = _serviceState.value
                            val isBypassed = if (activeState is ServiceState.Active) {
                                System.currentTimeMillis() < activeState.emergencyBypassUntilMillis
                            } else {
                                false
                            }

                            if (!isBypassed && !BlockedActivity.isShowing) {
                                val now = SystemClock.elapsedRealtime()
                                if (now - lastBlockedDetectionTime > 3000) {
                                    lastBlockedDetectionTime = now
                                    repository.incrementDistractionsBlocked()
                                    triggerGuardOverlay(currentPkg)
                                }
                            }
                        }
                    }
                }
            }

            val endTime = System.currentTimeMillis()
            val minutes = durationSeconds / 60
            repository.insertSession(
                FocusSession(
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = minutes,
                    completed = true,
                    category = category
                )
            )
            repository.incrementFocusMinutes(minutes)

            triggerCompletionNotification()
            stopFocusSession()
        }
    }

    private fun stopFocusSession() {
        tickerJob?.cancel()
        _serviceState.value = ServiceState.Idle
        stopForeground(true)
        stopSelf()
    }

    private fun triggerGuardOverlay(blockedPackage: String) {
        val intent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("blocked_package", blockedPackage)
        }
        startActivity(intent)
    }

    private fun getForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (stats.isNullOrEmpty()) return null
        var activeStats: UsageStats? = null
        for (stat in stats) {
            if (activeStats == null || stat.lastTimeUsed > activeStats.lastTimeUsed) {
                activeStats = stat
            }
        }
        return activeStats?.packageName
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Focus Lock Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active status and duration during focus sessions."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(remainingSeconds: Int): Notification {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lock Guard Active")
            .setContentText("Focus session ends in $timeStr")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(remainingSeconds: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(remainingSeconds))
    }

    private fun triggerCompletionNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val completionChannelId = "focus_completion_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                completionChannelId,
                "Focus Completion Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val completionNotification = NotificationCompat.Builder(this, completionChannelId)
            .setContentTitle("Focus Session Complete!")
            .setContentText("Outstanding job! You resisted social media distractions.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, completionNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

sealed class ServiceState {
    object Idle : ServiceState()
    data class Active(
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val category: String,
        val startTimeMillis: Long,
        val emergencyAllowed: Boolean = false,
        val emergencyChancesMax: Int = 0,
        val emergencyChancesRemaining: Int = 0,
        val emergencyDurationMinutes: Int = 0,
        val emergencyBypassUntilMillis: Long = 0L
    ) : ServiceState()
}
