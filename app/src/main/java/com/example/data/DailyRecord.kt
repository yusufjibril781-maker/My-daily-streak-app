package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val dateString: String, // Format: yyyy-MM-dd
    val mathDone: Boolean = false,
    val pythonDone: Boolean = false,
    val electronicsDone: Boolean = false
) {
    val completedCount: Int
        get() = (if (mathDone) 1 else 0) + (if (pythonDone) 1 else 0) + (if (electronicsDone) 1 else 0)

    val isAllCompleted: Boolean
        get() = mathDone && pythonDone && electronicsDone
}
