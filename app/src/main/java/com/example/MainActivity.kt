package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.FocusService
import com.example.ui.FocusViewModel
import com.example.ui.components.BlockListScreen
import com.example.ui.components.BlockedScreenOverlay
import com.example.ui.components.MetricsScreen
import com.example.ui.components.TimerScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: FocusViewModel = viewModel()
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: FocusViewModel = viewModel()) {
    val context = LocalContext.current
    val serviceState by viewModel.serviceState.collectAsState()
    val lockedApps by viewModel.lockedApps.collectAsState()
    val listInstalledApps by viewModel.installedApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val metrics by viewModel.metrics.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Timer") },
                    label = { Text("Timer") },
                    modifier = Modifier.testTag("nav_tab_timer")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Blocks") },
                    label = { Text("Locks") },
                    modifier = Modifier.testTag("nav_tab_blocks")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Metrics") },
                    label = { Text("Metrics") },
                    modifier = Modifier.testTag("nav_tab_metrics")
                )
            }
        }
    ) { paddingValues ->
        val modifier = Modifier.padding(paddingValues)

        when (selectedTab) {
            0 -> TimerScreen(
                serviceState = serviceState,
                viewModel = viewModel,
                onStartSession = { duration, category, emergencyAllowed, chances, minutes ->
                    viewModel.startFocusSession(context, duration, category, emergencyAllowed, chances, minutes)
                },
                onStopSession = {
                    viewModel.stopFocusSession(context)
                },
                modifier = modifier
            )
            1 -> BlockListScreen(
                installedApps = listInstalledApps,
                lockedApps = lockedApps,
                isLoading = isLoadingApps,
                onToggleBlock = { app, shouldBlock ->
                    viewModel.toggleAppBlock(app, shouldBlock)
                },
                onRefreshApps = {
                    viewModel.loadInstalledApps(context)
                },
                modifier = modifier
            )
            2 -> MetricsScreen(
                sessions = sessions,
                metrics = metrics,
                modifier = modifier
            )
        }
    }
}
