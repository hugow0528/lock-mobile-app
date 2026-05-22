package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.service.FocusService
import com.example.ui.components.BlockedScreenOverlay
import com.example.ui.theme.MyApplicationTheme

class BlockedActivity : ComponentActivity() {

    companion object {
        @Volatile
        var isShowing = false
    }

    private val blockedPackageState = mutableStateOf("Unknown")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        blockedPackageState.value = intent.getStringExtra("blocked_package") ?: "Unknown"

        setContent {
            MyApplicationTheme {
                val serviceState by FocusService.serviceState.collectAsState()
                val blockedPackage by blockedPackageState

                BlockedScreenOverlay(
                    blockedPackage = blockedPackage,
                    serviceState = serviceState,
                    onDismissOverlay = {
                        goHome()
                        finish()
                    },
                    onActivateEmergency = {
                        activateEmergency()
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        blockedPackageState.value = intent.getStringExtra("blocked_package") ?: "Unknown"
    }

    override fun onStart() {
        super.onStart()
        isShowing = true
    }

    override fun onStop() {
        super.onStop()
        isShowing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
    }

    private fun goHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (_: Exception) {
            // Fallback
        }
    }

    private fun activateEmergency() {
        val intent = Intent(this, FocusService::class.java).apply {
            action = FocusService.ACTION_ACTIVATE_EMERGENCY
        }
        startService(intent)
    }
}
