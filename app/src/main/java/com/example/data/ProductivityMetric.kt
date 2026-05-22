package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productivity_metrics")
data class ProductivityMetric(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val focusMinutes: Int,
    val completedSessions: Int,
    val distractionsBlocked: Int
)
