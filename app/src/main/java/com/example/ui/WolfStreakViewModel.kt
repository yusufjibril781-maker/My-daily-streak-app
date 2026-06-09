package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DailyRecord
import com.example.data.DailyRecordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int
)

data class DayActivity(
    val dateString: String,
    val label: String, // e.g. "Mon"
    val mathDone: Boolean,
    val pythonDone: Boolean,
    val electronicsDone: Boolean,
    val completedCount: Int,
    val isToday: Boolean
)

class WolfStreakViewModel(
    application: Application,
    private val repository: DailyRecordRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("wolf_streak_prefs", Context.MODE_PRIVATE)

    // Dark Mode Local State
    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val newVal = !_isDarkMode.value
        _isDarkMode.value = newVal
        sharedPrefs.edit().putBoolean("dark_mode", newVal).apply()
    }

    // Today's date string
    private val _todayDate = MutableStateFlow(getTodayDateString())
    val todayDate: StateFlow<String> = _todayDate.asStateFlow()

    // Triggered periodically or on resume to ensure calendar date is correct
    fun refreshTodayDate() {
        _todayDate.value = getTodayDateString()
    }

    // Today's record (derived dynamically from repository's record list of today)
    val todayRecord: StateFlow<DailyRecord> = repository.allRecords
        .map { records ->
            val todayStr = _todayDate.value
            records.find { it.dateString == todayStr } ?: DailyRecord(dateString = todayStr)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyRecord(dateString = getTodayDateString())
        )

    // Streak status
    val streakInfo: StateFlow<StreakInfo> = repository.allRecords
        .map { records ->
            calculateStreaks(records, _todayDate.value)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StreakInfo(0, 0)
        )

    // Last 7 days history activity list (to display a cute visual 7-day log)
    val pastWeekActivity: StateFlow<List<DayActivity>> = combine(repository.allRecords, _todayDate) { records, todayStr ->
        val recordMap = records.associateBy { it.dateString }
        val week = mutableListOf<DayActivity>()
        val cal = Calendar.getInstance()
        cal.time = Date()
        
        // Let's gather the last 7 calendar days chronologically ending with today
        // Walk backwards 6 days to today
        for (i in 6 downTo 0) {
            val checkCal = Calendar.getInstance()
            checkCal.time = Date()
            checkCal.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = formatDate(checkCal)
            val rec = recordMap[dStr]
            
            val dayOfWeekLabel = checkCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US) ?: ""
            week.add(
                DayActivity(
                    dateString = dStr,
                    label = dayOfWeekLabel.substring(0, 1), // "M", "T", "W" etc.
                    mathDone = rec?.mathDone ?: false,
                    pythonDone = rec?.pythonDone ?: false,
                    electronicsDone = rec?.electronicsDone ?: false,
                    completedCount = rec?.completedCount ?: 0,
                    isToday = dStr == todayStr
                )
            )
        }
        week
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Interactive Focus Mode State
    private val _focusTaskIndex = MutableStateFlow<Int?>(null) // null = not in focus mode, 1 = Math, 2 = Python, 3 = Electronics
    val focusTaskIndex: StateFlow<Int?> = _focusTaskIndex.asStateFlow()

    fun enterFocusMode() {
        val today = todayRecord.value
        _focusTaskIndex.value = when {
            !today.mathDone -> 1
            !today.pythonDone -> 2
            !today.electronicsDone -> 3
            else -> null // All done! Or we can default back to 1.
        }
    }

    fun nextFocusTask() {
        val today = todayRecord.value
        val currentFocus = _focusTaskIndex.value ?: return
        
        // Find next unchecked task starting after currentFocus
        _focusTaskIndex.value = when (currentFocus) {
            1 -> if (!today.pythonDone) 2 else if (!today.electronicsDone) 3 else null
            2 -> if (!today.electronicsDone) 3 else null
            else -> null
        }
    }

    fun exitFocusMode() {
        _focusTaskIndex.value = null
    }

    // Toggle Habits
    fun toggleMath(testTagDone: Boolean? = null) {
        val rec = todayRecord.value
        val updated = rec.copy(mathDone = testTagDone ?: !rec.mathDone)
        saveRecord(updated)
    }

    fun togglePython(testTagDone: Boolean? = null) {
        val rec = todayRecord.value
        val updated = rec.copy(pythonDone = testTagDone ?: !rec.pythonDone)
        saveRecord(updated)
    }

    fun toggleElectronics(testTagDone: Boolean? = null) {
        val rec = todayRecord.value
        val updated = rec.copy(electronicsDone = testTagDone ?: !rec.electronicsDone)
        saveRecord(updated)
    }

    private fun saveRecord(record: DailyRecord) {
        viewModelScope.launch {
            repository.insertRecord(record)
        }
    }

    // Clear history for testing or resetting
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllRecords()
        }
    }

    // --- Date Mechanics Helpers ---
    private fun getTodayDateString(): String {
        return formatDate(Calendar.getInstance())
    }

    private fun formatDate(calendar: Calendar): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(calendar.time)
    }

    private fun parseDate(dateStr: String): Calendar {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        try {
            val date = sdf.parse(dateStr)
            if (date != null) {
                cal.time = date
            }
        } catch (e: Exception) {
            // fallback
        }
        return cal
    }

    private fun getAdjacentDate(dateStr: String, daysOffset: Int): String {
        val cal = parseDate(dateStr)
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return formatDate(cal)
    }

    // --- Streak Computation Algorithm ---
    private fun calculateStreaks(records: List<DailyRecord>, todayStr: String): StreakInfo {
        if (records.isEmpty()) return StreakInfo(0, 0)

        // Map date to DailyRecord
        val recordMap = records.associateBy { it.dateString }
        val sortedDates = recordMap.keys.sorted()
        
        var currentLocalDateStr = sortedDates.first()
        if (currentLocalDateStr > todayStr) {
            currentLocalDateStr = todayStr
        }

        var activeStreak = 0
        var maxStreak = 0
        var dateStr = currentLocalDateStr

        while (true) {
            val rec = recordMap[dateStr]
            val count = rec?.completedCount ?: 0

            if (count == 3) {
                activeStreak++
                if (activeStreak > maxStreak) {
                    maxStreak = activeStreak
                }
            } else if (count in 1..2) {
                // Streak is maintained (activeStreak remains the same)
            } else {
                // Zero tasks completed on this day
                // Today having 0 is ignored as it is ongoing
                if (dateStr != todayStr) {
                    activeStreak = 0
                }
            }

            if (dateStr == todayStr) {
                break
            }
            dateStr = getAdjacentDate(dateStr, 1)
        }

        return StreakInfo(activeStreak, maxStreak)
    }
}

class WolfStreakViewModelFactory(
    private val application: Application,
    private val repository: DailyRecordRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WolfStreakViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WolfStreakViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
