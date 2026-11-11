package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.util.SmartFoodNutritionEngine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.HunterSystemState
import com.example.data.model.JournalEntry
import com.example.data.model.StreakDateUtils
import com.example.ui.theme.AppThemeIsDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.HunterCrimson
import com.example.ui.theme.HunterCyan
import com.example.ui.theme.HunterEmerald
import com.example.ui.theme.HunterGold
import com.example.ui.theme.HunterViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DataLoadState
import com.example.ui.viewmodel.NutritionChatMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class RechartsMetricMode(val label: String) {
    COMPOSED_ALL("COMPOSED (ALL 3)"),
    WEIGHT_KG("WEIGHT (KG)"),
    BODY_FAT_PCT("BODY FAT (%)"),
    MUSCLE_MASS("MUSCLE MASS (%)")
}

@Composable
fun BiometricChartsScreen(
    state: HunterSystemState,
    journalState: DataLoadState<List<JournalEntry>>,
    nutritionChatMessages: List<NutritionChatMessage> = emptyList(),
    isNutritionChatLoading: Boolean = false,
    onSendNutritionMessage: (String) -> Unit = {},
    onClearNutritionChat: () -> Unit = {},
    onLogBiometricPoint: (weightKg: Double, bodyFatPercent: Double?, muscleMassPercent: Double?, note: String) -> Unit,
    onLogFoodItem: (mealName: String, calories: Int, proteinG: Double) -> Unit,
    onLogExerciseSet: (exerciseName: String, sets: Int, reps: String, overload: Boolean) -> Unit,
    onUndoFood: () -> Unit = {},
    onUndoWater: () -> Unit = {},
    onUpdateHistoricalDayFoodLog: (
        dateStr: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatsG: Double,
        waterLiters: Double,
        foodSummary: String
    ) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val bio = state.biometrics
    var selectedChartMode by rememberSaveable { mutableStateOf(RechartsMetricMode.COMPOSED_ALL) }

    // Inputs for plotting a new Firestore biometric point (empty by default until user types)
    var weightInput by rememberSaveable { mutableStateOf("") }
    var bodyFatInput by rememberSaveable { mutableStateOf("") }
    var muscleMassInput by rememberSaveable { mutableStateOf("") }
    var noteInput by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("biometric_charts_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Daily Productive Streak Check-In Card
        item {
            DailyProductiveStreakCard(
                state = state,
                onLogFoodItem = onLogFoodItem,
                onLogExerciseSet = onLogExerciseSet,
                onUndoFood = onUndoFood,
                onUndoWater = onUndoWater,
                onUpdateHistoricalDayFoodLog = onUpdateHistoricalDayFoodLog
            )
        }

        // 2. Recharts Data Visualization Header & Firestore Time-Series Chart
        item {
            when (journalState) {
                DataLoadState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ObsidianCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = HunterCyan)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pulling biometric time-series from Firestore...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                is DataLoadState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(ObsidianCard)
                            .border(1.dp, HunterCrimson, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Firestore Telemetry Error: ${journalState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HunterCrimson
                        )
                    }
                }
                is DataLoadState.Success -> {
                    val chronologicalEntries = remember(journalState.data, bio) {
                        val sorted = journalState.data.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                        if (sorted.isNotEmpty()) {
                            sorted
                        } else {
                            listOf(
                                JournalEntry(
                                    id = "fallback_now",
                                    title = "Current Biometric Baseline",
                                    poseTag = "Baseline",
                                    weightKg = bio.weightKg,
                                    heightCm = bio.heightCm,
                                    age = bio.age.toLong(),
                                    bmi = bio.bmi,
                                    bodyFatPercent = bio.bodyFatPercent,
                                    muscleMassPercent = bio.muscleMassPercent,
                                    createdAt = null
                                )
                            )
                        }
                    }

                    RechartsBiometricVisualizerCard(
                        entries = chronologicalEntries,
                        selectedMode = selectedChartMode,
                        onSelectMode = { selectedChartMode = it }
                    )
                }
            }
        }

        // 3. Compact Plot New Biometric Data Point Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianCard)
                    .border(1.dp, HunterCyan.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .testTag("log_biometric_point_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timeline,
                            contentDescription = null,
                            tint = HunterCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PLOT BIOMETRIC DATA POINT",
                            style = MaterialTheme.typography.titleSmall,
                            color = HunterCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = HunterCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, HunterCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "LIVE SYNC",
                            style = MaterialTheme.typography.labelSmall,
                            color = HunterCyan,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)", fontSize = 11.sp, maxLines = 1) },
                        placeholder = { Text("${bio.weightKg}", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_input_weight_kg"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = bodyFatInput,
                        onValueChange = { bodyFatInput = it },
                        label = { Text("Body Fat %", fontSize = 11.sp, maxLines = 1) },
                        placeholder = { Text("${bio.bodyFatPercent}", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_input_body_fat_pct"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = muscleMassInput,
                        onValueChange = { muscleMassInput = it },
                        label = { Text("Muscle %", fontSize = 11.sp, maxLines = 1) },
                        placeholder = { Text("${bio.muscleMassPercent}", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_input_muscle_pct"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterEmerald,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Note (Optional)", fontSize = 11.sp, maxLines = 1) },
                        placeholder = { Text("e.g. Morning scale", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("chart_input_note"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Button(
                        onClick = {
                            val w = weightInput.toDoubleOrNull()?.coerceIn(25.0, 350.0) ?: bio.weightKg
                            val bf = bodyFatInput.toDoubleOrNull()?.coerceIn(3.0, 65.0)
                            val mm = muscleMassInput.toDoubleOrNull()?.coerceIn(20.0, 90.0)
                            onLogBiometricPoint(w, bf, mm, noteInput)
                            weightInput = ""
                            bodyFatInput = ""
                            muscleMassInput = ""
                            noteInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HunterCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("plot_biometric_point_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PLOT (+50 XP)",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyProductiveStreakCard(
    state: HunterSystemState,
    onLogFoodItem: (mealName: String, calories: Int, proteinG: Double) -> Unit,
    onLogExerciseSet: (exerciseName: String, sets: Int, reps: String, overload: Boolean) -> Unit,
    onUndoFood: () -> Unit = {},
    onUndoWater: () -> Unit = {},
    onUpdateHistoricalDayFoodLog: (
        dateStr: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatsG: Double,
        waterLiters: Double,
        foodSummary: String
    ) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val ps = state.playerStatus
    val vit = state.statBreakdown.vitality
    val stam = state.statBreakdown.stamina
    val isCheckedIn = ps.checkedInToday
    val accentColor = if (isCheckedIn) HunterEmerald else HunterGold

    var expandedActionMode by rememberSaveable { mutableIntStateOf(0) } // 0 = None, 1 = Food Form, 2 = Exercise Form
    var editingDateStr by rememberSaveable { mutableStateOf<String?>(null) }
    var mealName by rememberSaveable { mutableStateOf("") }
    var mealCalories by rememberSaveable { mutableStateOf("") }
    var mealProtein by rememberSaveable { mutableStateOf("") }

    var exerciseName by rememberSaveable { mutableStateOf("") }
    var exerciseSets by rememberSaveable { mutableStateOf("") }
    var exerciseReps by rememberSaveable { mutableStateOf("") }

    if (editingDateStr != null) {
        EditHistoricalDayFoodLogDialog(
            initialDateStr = editingDateStr ?: StreakDateUtils.daysAgoDateString(1),
            state = state,
            onDismiss = { editingDateStr = null },
            onSaveDayLog = { dateStr, cals, pro, carbs, fats, water, summary ->
                onUpdateHistoricalDayFoodLog(dateStr, cals, pro, carbs, fats, water, summary)
                editingDateStr = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ObsidianCardElevated, ObsidianCard)
                )
            )
            .border(1.5.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("daily_productive_streak_card")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.5.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "DAILY PRODUCTIVE STREAK",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor
                    )
                    Text(
                        text = if (isCheckedIn) {
                            "${ps.streakDays} DAY STREAK • CHECKED IN!"
                        } else {
                            "${ps.streakDays} DAY STREAK • CHECK-IN PENDING"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("streak_days_headline")
                    )
                }
            }

            Surface(
                color = accentColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, accentColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCheckedIn) Icons.Filled.CheckCircle else Icons.Filled.PendingActions,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCheckedIn) "VERIFIED" else "UNVERIFIED",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        modifier = Modifier.testTag("streak_checkin_status_badge")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Rule Banner
        Surface(
            color = ObsidianBg,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isCheckedIn) {
                        "✔ Productive day verified! ${ps.lastCheckInTrigger}"
                    } else {
                        "⚠ Logging into the app alone does NOT count as a productive day. Input any Food or Exercise data today to check in and advance to Day ${ps.streakDays + 1}!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCheckedIn) HunterEmerald else TextPrimary,
                    modifier = Modifier.testTag("streak_rule_status_text")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductiveCounterPill(
                        label = "FOOD INPUTS",
                        count = ps.todayFoodInputs,
                        color = if (ps.todayFoodInputs > 0) HunterEmerald else HunterGold,
                        modifier = Modifier.weight(1f).testTag("today_food_inputs_pill")
                    )
                    ProductiveCounterPill(
                        label = "EXERCISE INPUTS",
                        count = ps.todayExerciseInputs,
                        color = if (ps.todayExerciseInputs > 0) HunterEmerald else HunterCyan,
                        modifier = Modifier.weight(1f).testTag("today_exercise_inputs_pill")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 7-Day Productive Check-In Calendar Strip (Tap any day or Yesterday to edit its food log)
        Text(
            text = "Tap any streak day (e.g. Yesterday) below to view or edit that day's Food & Calorie Log:",
            style = MaterialTheme.typography.labelSmall,
            color = HunterGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        SevenDayStreakStrip(
            historyDates = ps.productiveHistoryDates,
            checkedInToday = ps.checkedInToday,
            dailyLogsByDate = ps.dailyLogsByDate,
            onClickDate = { clickedDate ->
                editingDateStr = clickedDate
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Direct "Edit Yesterday's Food Log" button
        OutlinedButton(
            onClick = { editingDateStr = StreakDateUtils.daysAgoDateString(1) },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, HunterGold),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_yesterday_food_log_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit Yesterday's Food Log",
                tint = HunterGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val yesterdayStr = StreakDateUtils.daysAgoDateString(1)
            val yesterdayLog = ps.dailyLogsByDate[yesterdayStr]
            val yesterdaySummary = if (yesterdayLog != null && yesterdayLog.calories > 0) {
                " (${yesterdayLog.calories} kcal • ${yesterdayLog.proteinG.toInt()}g P)"
            } else {
                ""
            }
            Text(
                text = "EDIT YESTERDAY'S FOOD LOG$yesterdaySummary",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = HunterGold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons to Input Food or Exercise Data
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { expandedActionMode = if (expandedActionMode == 1) 0 else 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HunterGold,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("streak_open_food_input_button")
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "LOG FOOD",
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Button(
                onClick = { expandedActionMode = if (expandedActionMode == 2) 0 else 2 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HunterCyan,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("streak_open_exercise_input_button")
            ) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "LOG EXERCISE",
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }

        // Quick Undo Food & Water Row when data has been logged
        if (vit.caloriesLogged > 0 || stam.waterLiters > 0.0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onUndoFood,
                    enabled = vit.caloriesLogged > 0,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (vit.caloriesLogged > 0) HunterCrimson else BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("charts_undo_food_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Food Entry",
                        tint = if (vit.caloriesLogged > 0) HunterCrimson else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UNDO FOOD",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (vit.caloriesLogged > 0) HunterCrimson else TextMuted
                    )
                }

                OutlinedButton(
                    onClick = onUndoWater,
                    enabled = stam.waterLiters > 0.0,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (stam.waterLiters > 0.0) HunterCyan else BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("charts_undo_water_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Water Entry",
                        tint = if (stam.waterLiters > 0.0) HunterCyan else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UNDO WATER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stam.waterLiters > 0.0) HunterCyan else TextMuted
                    )
                }
            }
        }

        // Expandable Food Input Form
        AnimatedVisibility(visible = expandedActionMode == 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianBg)
                    .border(1.dp, HunterGold.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOG FOOD / MEAL DATA (VERIFIES DAILY STREAK)",
                        style = MaterialTheme.typography.labelLarge,
                        color = HunterGold,
                        modifier = Modifier.weight(1f)
                    )
                    if (vit.caloriesLogged > 0) {
                        TextButton(
                            onClick = onUndoFood,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Last Food",
                                tint = HunterCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "UNDO LAST",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = HunterCrimson
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = { Text("Meal / Food Name (e.g., Steak & Rice Bowl)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("streak_food_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mealCalories,
                        onValueChange = { mealCalories = it },
                        label = { Text("Calories (kcal)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("streak_food_calories_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = mealProtein,
                        onValueChange = { mealProtein = it },
                        label = { Text("Protein (g)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("streak_food_protein_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val cals = mealCalories.toIntOrNull()?.coerceIn(10, 10000) ?: 550
                        val pro = mealProtein.toDoubleOrNull()?.coerceIn(0.0, 500.0) ?: 40.0
                        onLogFoodItem(
                            mealName.ifBlank { "Anabolic Recovery Meal" },
                            cals,
                            pro
                        )
                        mealName = ""
                        expandedActionMode = 0
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HunterEmerald,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("streak_submit_food_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SUBMIT FOOD INPUT & CHECK IN STREAK", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Expandable Exercise Input Form
        AnimatedVisibility(visible = expandedActionMode == 2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianBg)
                    .border(1.dp, HunterCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "LOG EXERCISE DATA (VERIFIES DAILY STREAK)",
                    style = MaterialTheme.typography.labelLarge,
                    color = HunterCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("Exercise Name (e.g., Weighted Pull-ups)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("streak_exercise_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = exerciseSets,
                        onValueChange = { exerciseSets = it },
                        label = { Text("Sets") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("streak_exercise_sets_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = exerciseReps,
                        onValueChange = { exerciseReps = it },
                        label = { Text("Reps") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("streak_exercise_reps_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val sets = exerciseSets.toIntOrNull()?.coerceIn(1, 20) ?: 3
                        onLogExerciseSet(
                            exerciseName.ifBlank { "Progressive Overload Set" },
                            sets,
                            exerciseReps.ifBlank { "10" },
                            true
                        )
                        exerciseName = ""
                        expandedActionMode = 0
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HunterEmerald,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("streak_submit_exercise_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SUBMIT EXERCISE INPUT & CHECK IN STREAK", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ProductiveCounterPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianCardElevated)
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SevenDayStreakStrip(
    historyDates: List<String>,
    checkedInToday: Boolean,
    dailyLogsByDate: Map<String, com.example.data.model.DailyDateLog> = emptyMap(),
    onClickDate: (String) -> Unit = {}
) {
    data class StreakDayPill(
        val dateStr: String,
        val label: String,
        val isVerified: Boolean,
        val isToday: Boolean,
        val isYesterday: Boolean,
        val loggedKcal: Int
    )

    val days = remember(historyDates, checkedInToday, dailyLogsByDate) {
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now = System.currentTimeMillis()
        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dateStr = dateFmt.format(cal.time)
            val label = if (daysAgo == 0) "TODAY" else dayFmt.format(cal.time).uppercase()
            val verified = if (daysAgo == 0) {
                checkedInToday
            } else {
                historyDates.contains(dateStr) || (dailyLogsByDate[dateStr]?.calories ?: 0) > 0
            }
            val loggedKcal = dailyLogsByDate[dateStr]?.calories ?: 0
            StreakDayPill(
                dateStr = dateStr,
                label = label,
                isVerified = verified,
                isToday = daysAgo == 0,
                isYesterday = daysAgo == 1,
                loggedKcal = loggedKcal
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { day ->
            val borderColor = when {
                day.isVerified -> HunterEmerald
                day.isToday -> HunterGold
                day.isYesterday -> HunterCyan
                else -> BorderSubtle
            }
            val bgColor = when {
                day.isVerified -> HunterEmerald.copy(alpha = 0.15f)
                day.isToday -> HunterGold.copy(alpha = 0.10f)
                day.isYesterday -> HunterCyan.copy(alpha = 0.10f)
                else -> ObsidianBg
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable { onClickDate(day.dateStr) }
                    .padding(vertical = 8.dp)
                    .testTag("streak_day_pill_${day.label.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (day.isVerified) Icons.Filled.LocalFireDepartment else Icons.Filled.PendingActions,
                    contentDescription = "Edit ${day.label} Food Log",
                    tint = when {
                        day.isVerified -> HunterEmerald
                        day.isToday -> HunterGold
                        day.isYesterday -> HunterCyan
                        else -> TextMuted
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = (-0.3).sp,
                        fontWeight = if (day.isToday || day.isYesterday) FontWeight.ExtraBold else FontWeight.SemiBold
                    ),
                    color = if (day.isToday || day.isYesterday) TextPrimary else TextSecondary,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = if (day.loggedKcal > 0) "${day.loggedKcal}" else "Edit",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    color = if (day.loggedKcal > 0) HunterEmerald else HunterGold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun EditHistoricalDayFoodLogDialog(
    initialDateStr: String,
    state: HunterSystemState,
    onDismiss: () -> Unit,
    onSaveDayLog: (
        dateStr: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatsG: Double,
        waterLiters: Double,
        foodSummary: String
    ) -> Unit
) {
    val recentDates = remember {
        val labelFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now = System.currentTimeMillis()
        (0..6).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dStr = dateFmt.format(cal.time)
            val prefix = when (daysAgo) {
                0 -> "Today"
                1 -> "Yesterday"
                else -> "${daysAgo}d ago"
            }
            dStr to "$prefix (${labelFmt.format(cal.time)})"
        }
    }

    var selectedDateStr by rememberSaveable(initialDateStr) { mutableStateOf(initialDateStr) }
    val existingLog = remember(selectedDateStr, state.playerStatus.dailyLogsByDate, state.statBreakdown) {
        val todayStr = StreakDateUtils.todayDateString()
        state.playerStatus.dailyLogsByDate[selectedDateStr] ?: if (selectedDateStr == todayStr) {
            com.example.data.model.DailyDateLog(
                date = todayStr,
                calories = state.statBreakdown.vitality.caloriesLogged,
                proteinG = state.statBreakdown.vitality.proteinG,
                carbsG = state.statBreakdown.vitality.carbsG,
                fatsG = state.statBreakdown.vitality.fatsG,
                waterLiters = state.statBreakdown.stamina.waterLiters,
                foodSummary = ""
            )
        } else {
            com.example.data.model.DailyDateLog(date = selectedDateStr)
        }
    }

    var foodQueryInput by remember(selectedDateStr) { mutableStateOf(existingLog.foodSummary) }
    var caloriesInput by remember(selectedDateStr) {
        mutableStateOf(if (existingLog.calories > 0) existingLog.calories.toString() else "")
    }
    var proteinInput by remember(selectedDateStr) {
        mutableStateOf(if (existingLog.proteinG > 0.0) existingLog.proteinG.toString() else "")
    }
    var carbsInput by remember(selectedDateStr) {
        mutableStateOf(if (existingLog.carbsG > 0.0) existingLog.carbsG.toString() else "")
    }
    var fatsInput by remember(selectedDateStr) {
        mutableStateOf(if (existingLog.fatsG > 0.0) existingLog.fatsG.toString() else "")
    }
    var waterInput by remember(selectedDateStr) {
        mutableStateOf(if (existingLog.waterLiters > 0.0) existingLog.waterLiters.toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = HunterGold,
        textContentColor = TextPrimary,
        title = {
            Column {
                Text(
                    text = "📅 EDIT STREAK DAY FOOD LOG",
                    style = MaterialTheme.typography.titleMedium,
                    color = HunterGold,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Tap Yesterday or any day below to edit or add missed food & calories",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Day selector row (Yesterday, Today, etc.)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recentDates.forEach { (dStr, dLabel) ->
                        val isSelected = dStr == selectedDateStr
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDateStr = dStr },
                            label = { Text(dLabel, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HunterGold.copy(alpha = 0.22f),
                                selectedLabelColor = HunterGold,
                                containerColor = ObsidianBg,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Smart Food Auto-Calculate helper
                OutlinedTextField(
                    value = foodQueryInput,
                    onValueChange = { newText ->
                        foodQueryInput = newText
                        if (newText.isNotBlank()) {
                            val calc = SmartFoodNutritionEngine.calculateForGramsOrQuantity(
                                foodInput = newText,
                                amountValue = 1,
                                isQuantityMode = true
                            )
                            caloriesInput = calc.calories.toString()
                            proteinInput = calc.proteinG.toString()
                            carbsInput = calc.carbsG.toString()
                            fatsInput = calc.fatsG.toString()
                        }
                    },
                    label = { Text("Food / Meals Logged (auto-calculates macros)", fontSize = 11.5.sp) },
                    placeholder = { Text("e.g. 2 Chapati + Chicken Curry", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("historical_day_food_summary_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = { caloriesInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Calories (kcal)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("historical_day_calories_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = proteinInput,
                        onValueChange = { proteinInput = it },
                        label = { Text("Protein (g)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("historical_day_protein_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterEmerald,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbsInput,
                        onValueChange = { carbsInput = it },
                        label = { Text("Carbs (g)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = fatsInput,
                        onValueChange = { fatsInput = it },
                        label = { Text("Fats (g)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterViolet,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = waterInput,
                        onValueChange = { waterInput = it },
                        label = { Text("Water (L)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveDayLog(
                        selectedDateStr,
                        caloriesInput.toIntOrNull()?.coerceIn(0, 15000) ?: 0,
                        proteinInput.toDoubleOrNull()?.coerceIn(0.0, 600.0) ?: 0.0,
                        carbsInput.toDoubleOrNull()?.coerceIn(0.0, 1500.0) ?: 0.0,
                        fatsInput.toDoubleOrNull()?.coerceIn(0.0, 500.0) ?: 0.0,
                        waterInput.toDoubleOrNull()?.coerceIn(0.0, 15.0) ?: 0.0,
                        foodQueryInput.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HunterEmerald,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier.testTag("save_historical_day_food_button")
            ) {
                Text("SAVE DAY LOG & UPDATE STREAK", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun RechartsBiometricVisualizerCard(
    entries: List<JournalEntry>,
    selectedMode: RechartsMetricMode,
    onSelectMode: (RechartsMetricMode) -> Unit
) {
    var selectedPointIndex by remember(entries.size) {
        mutableIntStateOf((entries.size - 1).coerceAtLeast(0))
    }
    val safeIndex = selectedPointIndex.coerceIn(0, (entries.size - 1).coerceAtLeast(0))
    val inspectedEntry = entries.getOrNull(safeIndex)
    val firstEntry = entries.firstOrNull()
    val latestEntry = entries.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianCard)
            .border(1.5.dp, HunterCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("recharts_visualizer_card")
    ) {
        // Card Title + Firestore Source Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = HunterCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RECHARTS BIOMETRIC TELEMETRY",
                        style = MaterialTheme.typography.titleSmall,
                        color = HunterCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Weight, Body Fat % & Muscle Mass Over Time • ${entries.size} Firestore Points",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Recharts Series Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RechartsMetricMode.entries.forEach { mode ->
                val chipColor = when (mode) {
                    RechartsMetricMode.COMPOSED_ALL -> HunterCyan
                    RechartsMetricMode.WEIGHT_KG -> HunterCyan
                    RechartsMetricMode.BODY_FAT_PCT -> HunterGold
                    RechartsMetricMode.MUSCLE_MASS -> HunterEmerald
                }
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onSelectMode(mode) },
                    label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor.copy(alpha = 0.22f),
                        selectedLabelColor = chipColor,
                        containerColor = ObsidianCardElevated,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedMode == mode,
                        borderColor = chipColor.copy(alpha = 0.5f),
                        selectedBorderColor = chipColor
                    ),
                    modifier = Modifier.testTag("recharts_mode_${mode.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedMode == RechartsMetricMode.COMPOSED_ALL || selectedMode == RechartsMetricMode.WEIGHT_KG) {
                ChartLegendItem(color = HunterCyan, label = "Weight (kg)")
            }
            if (selectedMode == RechartsMetricMode.COMPOSED_ALL || selectedMode == RechartsMetricMode.BODY_FAT_PCT) {
                ChartLegendItem(color = HunterGold, label = "Body Fat (%)")
            }
            if (selectedMode == RechartsMetricMode.COMPOSED_ALL || selectedMode == RechartsMetricMode.MUSCLE_MASS) {
                ChartLegendItem(color = HunterEmerald, label = "Muscle Mass (%)")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Recharts Cartesian Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianBg)
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .testTag("recharts_canvas_box")
        ) {
            RechartsTimeSeriesCanvas(
                entries = entries,
                mode = selectedMode,
                selectedIndex = safeIndex,
                onSelectIndex = { idx -> selectedPointIndex = idx }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // X-Axis Date Selector Chips (Tap any date to inspect its exact Firestore snapshot)
        val shortDateFmt = remember { SimpleDateFormat("MMM dd", Locale.US) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            entries.forEachIndexed { idx, entry ->
                val dateLabel = entry.createdAt?.toDate()?.let { shortDateFmt.format(it) } ?: "Point ${idx + 1}"
                val isSelected = idx == safeIndex
                Surface(
                    color = if (isSelected) HunterCyan.copy(alpha = 0.22f) else ObsidianCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) HunterCyan else BorderSubtle),
                    modifier = Modifier
                        .clickable { selectedPointIndex = idx }
                        .testTag("chart_point_chip_$idx")
                ) {
                    Text(
                        text = "$dateLabel (${entry.weightKg}kg)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) HunterCyan else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Interactive Recharts Tooltip Card for Selected Point
        if (inspectedEntry != null) {
            Spacer(modifier = Modifier.height(12.dp))
            RechartsInspectTooltip(
                entry = inspectedEntry,
                baselineEntry = firstEntry,
                pointNumber = safeIndex + 1,
                totalPoints = entries.size
            )
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun RechartsTimeSeriesCanvas(
    entries: List<JournalEntry>,
    mode: RechartsMetricMode,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val weights = remember(entries) { entries.map { it.weightKg.toFloat() } }
    val bodyFats = remember(entries) { entries.map { it.bodyFatPercent.toFloat() } }
    val muscles = remember(entries) { entries.map { it.muscleMassPercent.toFloat() } }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(entries.size) {
                detectTapGestures { tapOffset ->
                    if (entries.size <= 1) {
                        onSelectIndex(0)
                    } else {
                        val fraction = (tapOffset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val nearestIdx = (fraction * (entries.size - 1)).roundToInt().coerceIn(0, entries.size - 1)
                        onSelectIndex(nearestIdx)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        // 1. Recharts Cartesian Grid (Horizontal & Vertical dashed lines)
        val horizontalRows = 4
        for (i in 0..horizontalRows) {
            val y = (h / horizontalRows) * i
            drawLine(
                color = BorderSubtle.copy(alpha = 0.65f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        val count = entries.size
        if (count > 1) {
            for (i in 0 until count) {
                val x = (w / (count - 1).toFloat()) * i
                drawLine(
                    color = BorderSubtle.copy(alpha = 0.35f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }
        }

        // Helper to normalize a metric series into Y coordinates with generous visual breathing room
        fun computeSeriesPoints(values: List<Float>, topBand: Float = 0.08f, bottomBand: Float = 0.92f): List<Offset> {
            if (values.isEmpty()) return emptyList()
            val minVal = (values.minOrNull() ?: 0f) - 0.8f
            val maxVal = (values.maxOrNull() ?: 100f) + 0.8f
            val span = (maxVal - minVal).coerceAtLeast(1f)
            val usableH = h * (bottomBand - topBand)
            val offsetY = h * topBand

            return values.mapIndexed { idx, v ->
                val x = if (values.size <= 1) w / 2f else (w / (values.size - 1).toFloat()) * idx
                val normalized = ((v - minVal) / span).coerceIn(0f, 1f)
                val y = offsetY + usableH * (1f - normalized)
                Offset(x, y)
            }
        }

        // Helper to draw smooth cubic Bezier curve + Recharts Area gradient + Node Dots
        fun drawRechartsSeries(
            points: List<Offset>,
            seriesColor: Color,
            fillAlpha: Float
        ) {
            if (points.isEmpty()) return
            if (points.size == 1) {
                val pt = points.first()
                drawCircle(color = seriesColor.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = pt)
                drawCircle(color = seriesColor, radius = 5.dp.toPx(), center = pt)
                return
            }

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX1 = (p0.x + p1.x) / 2f
                    val controlX2 = (p0.x + p1.x) / 2f
                    cubicTo(controlX1, p0.y, controlX2, p1.y, p1.x, p1.y)
                }
            }

            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, h)
                lineTo(points.first().x, h)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(seriesColor.copy(alpha = fillAlpha), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = linePath,
                color = seriesColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            points.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawCircle(
                        color = seriesColor.copy(alpha = 0.30f),
                        radius = 9.dp.toPx(),
                        center = pt
                    )
                }
                drawCircle(
                    color = ObsidianBg,
                    radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = seriesColor,
                    radius = if (isSelected) 4.dp.toPx() else 2.8.dp.toPx(),
                    center = pt
                )
            }
        }

        // Draw active series based on mode
        when (mode) {
            RechartsMetricMode.COMPOSED_ALL -> {
                val musclePts = computeSeriesPoints(muscles, topBand = 0.06f, bottomBand = 0.36f)
                val weightPts = computeSeriesPoints(weights, topBand = 0.38f, bottomBand = 0.66f)
                val bfPts = computeSeriesPoints(bodyFats, topBand = 0.68f, bottomBand = 0.94f)

                drawRechartsSeries(musclePts, HunterEmerald, fillAlpha = 0.16f)
                drawRechartsSeries(weightPts, HunterCyan, fillAlpha = 0.18f)
                drawRechartsSeries(bfPts, HunterGold, fillAlpha = 0.16f)
            }
            RechartsMetricMode.WEIGHT_KG -> {
                val weightPts = computeSeriesPoints(weights)
                drawRechartsSeries(weightPts, HunterCyan, fillAlpha = 0.28f)
            }
            RechartsMetricMode.BODY_FAT_PCT -> {
                val bfPts = computeSeriesPoints(bodyFats)
                drawRechartsSeries(bfPts, HunterGold, fillAlpha = 0.28f)
            }
            RechartsMetricMode.MUSCLE_MASS -> {
                val musclePts = computeSeriesPoints(muscles)
                drawRechartsSeries(musclePts, HunterEmerald, fillAlpha = 0.28f)
            }
        }

        // Draw active Recharts Vertical Crosshair Cursor on selected point
        if (entries.isNotEmpty()) {
            val activeX = if (entries.size <= 1) {
                w / 2f
            } else {
                (w / (entries.size - 1).toFloat()) * selectedIndex.coerceIn(0, entries.size - 1)
            }
            drawLine(
                color = HunterCyan.copy(alpha = 0.75f),
                start = Offset(activeX, 0f),
                end = Offset(activeX, h),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }
    }
}

@Composable
private fun RechartsInspectTooltip(
    entry: JournalEntry,
    baselineEntry: JournalEntry?,
    pointNumber: Int,
    totalPoints: Int
) {
    val formattedDate = remember(entry.createdAt) {
        val dt = entry.createdAt?.toDate() ?: Date()
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(dt)
    }

    val weightDiff = if (baselineEntry != null) round1(entry.weightKg - baselineEntry.weightKg) else 0.0
    val bfDiff = if (baselineEntry != null) round1(entry.bodyFatPercent - baselineEntry.bodyFatPercent) else 0.0
    val muscleDiff = if (baselineEntry != null) round1(entry.muscleMassPercent - baselineEntry.muscleMassPercent) else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCardElevated)
            .border(1.dp, HunterCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("recharts_inspect_tooltip")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INSPECTED POINT ($pointNumber / $totalPoints) • $formattedDate",
                style = MaterialTheme.typography.labelMedium,
                color = HunterCyan
            )
            Text(
                text = entry.poseTag.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TooltipMetricColumn(
                label = "WEIGHT",
                value = "${entry.weightKg} kg",
                sub = "${if (weightDiff >= 0) "+" else ""}$weightDiff kg",
                color = HunterCyan,
                modifier = Modifier.weight(1f)
            )
            TooltipMetricColumn(
                label = "BODY FAT %",
                value = "${entry.bodyFatPercent}%",
                sub = "${entry.fatMassKg}kg (${if (bfDiff >= 0) "+" else ""}$bfDiff%)",
                color = HunterGold,
                modifier = Modifier.weight(1f)
            )
            TooltipMetricColumn(
                label = "MUSCLE MASS",
                value = "${entry.muscleMassPercent}%",
                sub = "${entry.muscleMassKg}kg (${if (muscleDiff >= 0) "+" else ""}$muscleDiff%)",
                color = HunterEmerald,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TooltipMetricColumn(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextSecondary,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DeltaSummaryTile(
    title: String,
    currentValue: String,
    deltaText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBg)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = accentColor,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = currentValue,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = deltaText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0
