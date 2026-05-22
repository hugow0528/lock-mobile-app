package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusSession
import com.example.data.ProductivityMetric
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MetricsScreen(
    sessions: List<FocusSession>,
    metrics: List<ProductivityMetric>,
    modifier: Modifier = Modifier
) {
    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val todayMetric = remember(metrics, todayDateStr) {
        metrics.firstOrNull { it.date == todayDateStr } ?: ProductivityMetric(todayDateStr, 0, 0, 0)
    }

    val sortedMetricsForChart = remember(metrics) {
        metrics.take(7).reversed()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "DAILY METRICS",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track daily resilience milestones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Focus Minutes Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_minutes_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Focus Minutes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${todayMetric.focusMinutes}M",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Focused Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Completed Sessions Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_sessions_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Sessions",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${todayMetric.completedSessions}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Sessions Lock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Distractions Blocked Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_distractions_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Distractions Checked",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${todayMetric.distractionsBlocked}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Blocks Avoided",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("productivity_bar_chart_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WEEKLY RESOLVE PROFILE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (sortedMetricsForChart.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Metrics will collect here as sessions finish.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val maxMinutes = remember(sortedMetricsForChart) {
                            val maxVal = sortedMetricsForChart.maxOfOrNull { it.focusMinutes } ?: 60
                            if (maxVal > 0) maxVal.toFloat() else 60f
                        }

                        val primaryColor = MaterialTheme.colorScheme.primary
                        val strokeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = 4.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val barSpacing = canvasWidth / 7f
                                val topPadding = 16.dp.toPx()
                                val bottomPadding = 24.dp.toPx()
                                val usableHeight = canvasHeight - topPadding - bottomPadding

                                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                for (i in 0..3) {
                                    val y = topPadding + (usableHeight * i / 3)
                                    drawLine(
                                        color = strokeColor,
                                        start = Offset(0f, y),
                                        end = Offset(canvasWidth, y),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = pathEffect
                                    )
                                }

                                sortedMetricsForChart.forEachIndexed { index, metric ->
                                    val xCenter = (index * barSpacing) + (barSpacing / 2f)
                                    val barWidth = 18.dp.toPx()
                                    val barHeight = if (maxMinutes > 0) {
                                        (metric.focusMinutes / maxMinutes) * usableHeight
                                    } else 0f

                                    val top = topPadding + usableHeight - barHeight

                                    drawRoundRect(
                                        color = primaryColor,
                                        topLeft = Offset(xCenter - (barWidth / 2f), top),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )

                                    val label = try {
                                        val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(metric.date)
                                        SimpleDateFormat("MM/dd", Locale.getDefault()).format(dateObj ?: Date())
                                    } catch (e: Exception) {
                                        metric.date.takeLast(5)
                                    }

                                    drawContext.canvas.nativeCanvas.drawText(
                                        label,
                                        xCenter,
                                        canvasHeight - 4.dp.toPx(),
                                        android.graphics.Paint().apply {
                                            color = primaryColor.toArgb()
                                            textSize = 9.sp.toPx()
                                            textAlign = android.graphics.Paint.Align.CENTER
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "LOG TIMELINE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No focus history available yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_timeline_item")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Log",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = session.category,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val format = remember { SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()) }
                                Text(
                                    text = format.format(Date(session.startTime)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+${session.durationMinutes}m",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "LOCKED SUCCESS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
