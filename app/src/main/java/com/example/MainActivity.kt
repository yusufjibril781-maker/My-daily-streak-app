package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.DailyRecord
import com.example.data.DailyRecordRepository
import com.example.ui.*
import com.example.ui.theme.WolfStreakTheme

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { DailyRecordRepository(database.dailyRecordDao) }
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            WolfStreakViewModelFactory(application, repository)
        )[WolfStreakViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            
            WolfStreakTheme(darkTheme = isDarkMode) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    WolfStreakScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure accurate today's date checking when app is resumed
        viewModel.refreshTodayDate()
    }
}

@Composable
fun WolfStreakScreen(
    viewModel: WolfStreakViewModel,
    modifier: Modifier = Modifier
) {
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val streakInfo by viewModel.streakInfo.collectAsStateWithLifecycle()
    val pastWeekActivity by viewModel.pastWeekActivity.collectAsStateWithLifecycle()
    val focusTaskIndex by viewModel.focusTaskIndex.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    // Celebrate when all 3 are completed today!
    val allCompletedToday = todayRecord.isAllCompleted
    
    // Pulse animation logic for the focus button or celebratory state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WOLF STREAK",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "showing up is 90% of the battle",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = { viewModel.toggleDarkMode() },
                    modifier = Modifier.testTag("dark_mode_toggle")
                ) {
                    Text(
                        text = if (isDarkMode) "🌙" else "☀️",
                        fontSize = 20.sp
                    )
                }
            }

            // --- STREAK STATS PANEL ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Streak Panel
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "STREAK",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🔥 ${streakInfo.currentStreak} Days",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.testTag("streak_counter")
                            )
                        }

                        // Vertical separator
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )

                        // Best Streak Panel
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "BEST SCORE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🏆 ${streakInfo.bestStreak} Days",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.testTag("best_streak_counter")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 7-day chronological dots
                    Text(
                        text = "LAST 7 DAYS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        pastWeekActivity.forEach { day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val borderBrush = when {
                                    day.completedCount == 3 -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    day.completedCount > 0 -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                }
                                val bg = when {
                                    day.completedCount == 3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    day.completedCount > 0 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(bg)
                                        .then(if (day.isToday) Modifier.background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else Modifier)
                                        .clickable {
                                            // Click status message to encourage
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        day.completedCount == 3 -> {
                                            Text("🔥", fontSize = 16.sp)
                                        }
                                        day.completedCount > 0 -> {
                                            // Show fraction
                                            Text(
                                                "${day.completedCount}/3",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        else -> {
                                            // Uncompleted dot
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.outline)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = day.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // --- MISSION / STATEMENT ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (allCompletedToday) {
                        "✨ ALL SET TODAY! YOU ARE UNSTOPPABLE. ✨"
                    } else {
                        "WOLVES DO NOT DELAY. FOCUS AND CRUSH TODAY."
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (allCompletedToday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            // --- THREE DAILY HABIT ITEMS ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TODAY'S MISSION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    // Habit 1: Math Study
                    HabitTaskRow(
                        title = "Math Study",
                        emoji = "📐",
                        detail = "Solve or read logic problems",
                        isDone = todayRecord.mathDone,
                        onToggle = { viewModel.toggleMath() },
                        primaryColor = MaterialTheme.colorScheme.primary,
                        checkboxTag = "math_checkbox"
                    )

                    // Habit 2: Python Practice
                    HabitTaskRow(
                        title = "Python Practice",
                        emoji = "🐍",
                        detail = "Write some code, debug or study scripts",
                        isDone = todayRecord.pythonDone,
                        onToggle = { viewModel.togglePython() },
                        primaryColor = MaterialTheme.colorScheme.secondary,
                        checkboxTag = "python_checkbox"
                    )

                    // Habit 3: Electronics Learning
                    HabitTaskRow(
                        title = "Electronics Learning",
                        emoji = "⚡",
                        detail = "Schematics, Arduino, physics or circuits",
                        isDone = todayRecord.electronicsDone,
                        onToggle = { viewModel.toggleElectronics() },
                        primaryColor = MaterialTheme.colorScheme.tertiary,
                        checkboxTag = "electronics_checkbox"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LOWER MAIN ACTION BUTTON ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val buttonBrush = when {
                    allCompletedToday -> Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.outline,
                            MaterialTheme.colorScheme.outline
                        )
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                Button(
                    onClick = {
                        if (!allCompletedToday) {
                            viewModel.enterFocusMode()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("start_today_button")
                        .then(
                            if (!allCompletedToday) Modifier.graphicsLayer(
                                scaleX = glowScale,
                                scaleY = glowScale
                            ) else Modifier
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(16.dp),
                    border = if (allCompletedToday) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    enabled = !allCompletedToday
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(buttonBrush)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (allCompletedToday) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (allCompletedToday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (allCompletedToday) "ALL TASKS COMPLETED TODAY" else "START TODAY FOCUS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = if (allCompletedToday) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hidden clean dev command button: "Clear Records" (useful debug option kept clean at the bottom)
                Text(
                    text = "reset stats",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .clickable { viewModel.clearHistory() }
                        .padding(8.dp)
                        .testTag("dev_reset_button")
                )
            }
        }

        // --- EXTREMELY IMMERSIVE ADHD FOCUS VIEW (DIALOG OVERLAY) ---
        if (focusTaskIndex != null) {
            Dialog(
                onDismissRequest = { viewModel.exitFocusMode() },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                FocusModeOverlay(
                    taskIndex = focusTaskIndex!!,
                    todayRecord = todayRecord,
                    onToggleDone = { index ->
                        when(index) {
                            1 -> viewModel.toggleMath(true)
                            2 -> viewModel.togglePython(true)
                            3 -> viewModel.toggleElectronics(true)
                        }
                    },
                    onSkipNext = {
                        viewModel.nextFocusTask()
                    },
                    onDismiss = { viewModel.exitFocusMode() }
                )
            }
        }
    }
}

@Composable
fun HabitTaskRow(
    title: String,
    emoji: String,
    detail: String,
    isDone: Boolean,
    onToggle: () -> Unit,
    primaryColor: Color,
    checkboxTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .testTag("task_row_$checkboxTag"),
        color = if (isDone) primaryColor.copy(alpha = 0.08f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isDone) primaryColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Emoji Icon bubble
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDone) primaryColor.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isDone) primaryColor else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag(checkboxTag),
                colors = CheckboxDefaults.colors(
                    checkedColor = primaryColor,
                    uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// --- FOCUS VIEW DIALOG ---
@Composable
fun FocusModeOverlay(
    taskIndex: Int,
    todayRecord: DailyRecord,
    onToggleDone: (Int) -> Unit,
    onSkipNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val taskTitle = when (taskIndex) {
        1 -> "Math Study"
        2 -> "Python Practice"
        else -> "Electronics Learning"
    }
    val taskEmoji = when (taskIndex) {
        1 -> "📐"
        2 -> "🐍"
        else -> "⚡"
    }
    val taskDesc = when (taskIndex) {
        1 -> "📐 Open some exercises or explore formula concepts"
        2 -> "🐍 Write Python statements, review functions, code snippet"
        else -> "⚡ Inspect schematics, circuits, components or simulation"
    }

    val isDone = when(taskIndex) {
        1 -> todayRecord.mathDone
        2 -> todayRecord.pythonDone
        else -> todayRecord.electronicsDone
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.93f)) // Extra heavy dark background to remove ALL surrounding clutter
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Focus Header tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SINGLE-POINT FOCUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Oversized Emoji representation
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(taskEmoji, fontSize = 54.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large title
                Text(
                    text = taskTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = taskDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Toggle Done interactive element
                Button(
                    onClick = {
                        onToggleDone(taskIndex)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("focus_complete_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isDone) Color.White else MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDone) "COMPLETED! (CLICK TO UNDO)" else "COMPLETED today!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isDone) Color.White else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skip / Next task or close focus option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("focus_close_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Target")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Exit Focus",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace)
                        )
                    }

                    Button(
                        onClick = { onSkipNext() },
                        modifier = Modifier
                            .weight(12f)
                            .height(50.dp)
                            .testTag("focus_next_button")
                            .weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Next Target",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Next task focus",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
