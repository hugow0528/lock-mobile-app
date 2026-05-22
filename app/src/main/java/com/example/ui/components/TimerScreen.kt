package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ServiceState

@Composable
fun TimerScreen(
    serviceState: ServiceState,
    viewModel: com.example.ui.FocusViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onStartSession: (durationSeconds: Int, category: String, emergencyAllowed: Boolean, emergencyChances: Int, emergencyDurationMinutes: Int) -> Unit,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMinutes by remember { mutableStateOf(25) }
    var selectedCategory by remember { mutableStateOf("Work") }

    var emergencyAllowed by remember { mutableStateOf(viewModel.getEmergencyAllowed()) }
    var emergencyChances by remember { mutableStateOf(viewModel.getEmergencyChances()) }
    var emergencyMinutes by remember { mutableStateOf(viewModel.getEmergencyMinutes()) }

    val presetMinutes = listOf(15, 25, 45, 60)
    val categories = listOf("Work", "Study", "Coding", "Reading")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Goal Headline
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "FOCUS GUARD",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Resist social media. Reclaim your attention.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Circular Ticker & Dial
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(260.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            val progress: Float
            val timeDisplay: String

            when (serviceState) {
                is ServiceState.Active -> {
                    progress = serviceState.remainingSeconds.toFloat() / serviceState.totalSeconds
                    val mins = serviceState.remainingSeconds / 60
                    val secs = serviceState.remainingSeconds % 60
                    timeDisplay = String.format("%02d:%02d", mins, secs)
                }
                else -> {
                    progress = 1.0f
                    timeDisplay = String.format("%02d:00", selectedMinutes)
                }
            }

            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                label = "TimerProgress"
            )

            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

            Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = 16.dp.toPx())
                )
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("timer_display")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (serviceState is ServiceState.Active) "LOCK GUARD ACTIVE" else "READY TO FOCUS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (serviceState is ServiceState.Active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        // Configuration Panel (only clickable if Idle)
        if (serviceState is ServiceState.Idle) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preset List
                Column {
                    Text(
                        text = "SELECT DURATION",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetMinutes.forEach { mins ->
                            val isSelected = selectedMinutes == mins
                            Card(
                                onClick = { selectedMinutes = mins },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .testTag("duration_preset_${mins}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "${mins}M",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Selector
                Column {
                    Text(
                        text = "FOCUS PATH",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("category_chip_${cat.lowercase()}")
                            )
                        }
                    }
                }

                // Emergency Use Settings Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_use_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "EMERGENCY PASSES",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Set limit and period to bypass block",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = emergencyAllowed,
                                onCheckedChange = { emergencyAllowed = it },
                                modifier = Modifier.testTag("emergency_toggle_switch")
                            )
                        }

                        if (emergencyAllowed) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Step selectors for count of chances
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Allowed Passes",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            onClick = { if (emergencyChances > 1) emergencyChances-- },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            modifier = Modifier.size(36.dp).testTag("dec_chances")
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                        Text(
                                            text = "$emergencyChances times",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.testTag("emergency_chances_value")
                                        )
                                        Card(
                                            onClick = { if (emergencyChances < 10) emergencyChances++ },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            modifier = Modifier.size(36.dp).testTag("inc_chances")
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                    }
                                }

                                // Step selectors for duration minutes
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Minutes Per Use",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            onClick = { if (emergencyMinutes > 1) emergencyMinutes-- },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            modifier = Modifier.size(36.dp).testTag("dec_minutes")
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                        Text(
                                            text = "$emergencyMinutes min",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.testTag("emergency_minutes_value")
                                        )
                                        Card(
                                            onClick = { if (emergencyMinutes < 60) emergencyMinutes++ },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            modifier = Modifier.size(36.dp).testTag("inc_minutes")
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Stats detail card of active session
            val activeState = serviceState as ServiceState.Active
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MODE",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = activeState.category.uppercase(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TARGET",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${activeState.totalSeconds / 60} min",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (activeState.emergencyAllowed) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "EMERGENCY PROTECTION",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${activeState.emergencyChancesRemaining} of ${activeState.emergencyChancesMax} passes remaining",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (System.currentTimeMillis() < activeState.emergencyBypassUntilMillis) {
                                val activeRemainingSeconds = ((activeState.emergencyBypassUntilMillis - System.currentTimeMillis()) / 1000).toInt()
                                val bypassMins = activeRemainingSeconds / 60
                                val bypassSecs = activeRemainingSeconds % 60
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = String.format("PASS ACTIVE: %02d:%02d remaining", bypassMins, bypassSecs),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text(
                                text = "Emergency pass is disabled for this session.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Primary Action Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            if (serviceState is ServiceState.Idle) {
                Button(
                    onClick = { onStartSession(selectedMinutes * 60, selectedCategory, emergencyAllowed, emergencyChances, emergencyMinutes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_focus_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                        Text(
                            text = "ENGAGE LOCK GUARD",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onStopSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("stop_focus_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Stop")
                        Text(
                            text = "BREAK FOCUS GUARD",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
