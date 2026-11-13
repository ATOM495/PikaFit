package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyDateLog
import com.example.data.model.HunterSystemState
import com.example.data.model.LoggedFoodItem
import com.example.data.model.StreakDateUtils
import com.example.data.model.TrainerRankProgression
import com.example.ui.theme.AppThemeIsDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.HunterCrimson
import com.example.ui.theme.HunterCyan
import com.example.ui.theme.HunterEmerald
import com.example.ui.theme.HunterGold
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

private data class HeatmapDayCell(
    val dateStr: String,
    val displayLabel: String,
    val isToday: Boolean,
    val isRestored: Boolean,
    val intensityLevel: Int, // 0..4
    val calories: Int,
    val proteinG: Double,
    val tasksCount: Int,
    val isProductive: Boolean
)

@Composable
fun StreaksTab(
    state: HunterSystemState,
    isDark: Boolean = AppThemeIsDark,
    onRestoreMissedStreak: () -> Unit,
    onLogFoodItem: (mealName: String, calories: Int, proteinG: Double) -> Unit,
    onLogExerciseSet: (exerciseName: String, sets: Int, reps: String, overload: Boolean) -> Unit,
    onLogProductiveTask: (title: String, category: String, dateStr: String) -> Unit,
    onRemoveProductiveTask: (dateStr: String, taskId: String) -> Unit,
    onUndoFood: () -> Unit,
    onUndoWater: () -> Unit,
    onUpdateHistoricalDayFoodLog: (
        dateStr: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatsG: Double,
        waterLiters: Double,
        foodSummary: String,
        foodItems: List<LoggedFoodItem>
    ) -> Unit
) {
    val ps = state.playerStatus
    val currentTier = ps.currentTier
    val nextTier = ps.nextTier
    val restoreCost = TrainerRankProgression.streakRestoreCostXp

    var editingDateStr by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGridDate by rememberSaveable { mutableStateOf(StreakDateUtils.todayDateString()) }

    if (editingDateStr != null) {
        EditHistoricalDayFoodLogDialog(
            initialDateStr = editingDateStr ?: StreakDateUtils.todayDateString(),
            state = state,
            onDismiss = { editingDateStr = null },
            onSaveDayLog = { dateStr, cals, pro, carbs, fats, water, summary, items ->
                onUpdateHistoricalDayFoodLog(dateStr, cals, pro, carbs, fats, water, summary, items)
                editingDateStr = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("streaks_tab_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Streak Restore Alert Banner (if 1 day was missed) OR Streak Shield Status
        item {
            if (ps.restorableStreakDays > 0 && ps.missedStreakDate.isNotBlank()) {
                val canAfford = ps.currentXp >= restoreCost
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    HunterCrimson.copy(alpha = if (isDark) 0.24f else 0.14f),
                                    ObsidianCardElevated
                                )
                            )
                        )
                        .border(2.dp, HunterCrimson, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                        .testTag("missed_streak_restore_card"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(HunterCrimson.copy(alpha = 0.2f))
                                    .border(1.5.dp, HunterCrimson, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Restore,
                                    contentDescription = "Restore Streak",
                                    tint = HunterCrimson,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "1-DAY MISSED STREAK DETECTED!",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = HunterCrimson,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Restore your ${ps.restorableStreakDays}-Day Streak (${ps.missedStreakDate})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Text(
                        text = "Missing 1 day paused your class streak. Spend $restoreCost XP from your ${ps.currentXp} XP balance to recover your ${ps.restorableStreakDays}D streak and restore your Pokémon class immediately — your lifetime XP Level stays untouched!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Button(
                        onClick = onRestoreMissedStreak,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) HunterGold else BorderSubtle,
                            contentColor = if (canAfford) Color(0xFF0F172A) else TextMuted
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("restore_streak_with_xp_button")
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (canAfford) {
                                "RESTORE ${ps.restorableStreakDays}D STREAK (-$restoreCost XP)"
                            } else {
                                "NEED ${ps.xpNeededForRestore} MORE XP (COST: $restoreCost XP)"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            } else {
                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, HunterCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = if (ps.currentXp >= restoreCost) HunterEmerald else HunterGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Streak Shield Ready • Cost: $restoreCost XP",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (ps.currentXp >= restoreCost) {
                                    "You have ${ps.currentXp} spendable XP — protected if you ever miss 1 day!"
                                } else {
                                    "Spendable XP: ${ps.currentXp} / $restoreCost XP needed for 1-day missed streak restore."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 2. Dual Progression Card: Continuous XP Level + Streak-Based Class Promotion
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ObsidianCard)
                    .border(1.5.dp, HunterGold.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
                    .testTag("streak_vs_xp_summary_card"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianCardElevated)
                            .border(1.5.dp, HunterGold, RoundedCornerShape(16.dp))
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PikachuMascotBadge(
                            drawableRes = currentTier.pokemonDrawableRes,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CLASS LVL ${currentTier.tierLevel}: ${currentTier.classTitle.uppercase()} (${currentTier.pokemonName})",
                            style = MaterialTheme.typography.titleSmall,
                            color = HunterGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${ps.streakDays} Verified Days • Best Streak: ${maxOf(ps.bestStreakDays, ps.streakDays)}D",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Continuous XP Level ${ps.level} • ${ps.lifetimeXp} Total XP (${ps.currentXp} Spendable)",
                            style = MaterialTheme.typography.labelMedium,
                            color = HunterCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Class Promotion Progress Bar (by Streak Days)
                val classFraction = (ps.streakClassProgressPercent / 100.0).toFloat().coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (nextTier != null) {
                                "Next Class: Lv ${nextTier.tierLevel} ${nextTier.classTitle} (${nextTier.pokemonName})"
                            } else {
                                "Supreme Class Reached: Champion (Arceus)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        )
                        Text(
                            text = if (nextTier != null) {
                                "${ps.streakDays} / ${nextTier.requiredStreakDays} days"
                            } else {
                                "${ps.streakDays} / 500+ days"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = HunterGold,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    LinearProgressIndicator(
                        progress = { classFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = HunterGold,
                        trackColor = ObsidianBg,
                        strokeCap = StrokeCap.Round
                    )
                }

                // Continuous XP Level Progress Bar
                val xpFraction = (ps.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "XP Level Progress (Lv ${ps.level} → ${ps.level + 1})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        )
                        Text(
                            text = "${ps.lifetimeXp} / ${ps.nextLevelXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = HunterEmerald,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    LinearProgressIndicator(
                        progress = { xpFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = HunterEmerald,
                        trackColor = ObsidianBg,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // 3. 5x5 Rounded-Square Streak Heatmap Card (Matches reference format, Light & Dark theme aware!)
        item {
            StreakHeatmapGridCard(
                state = state,
                isDark = isDark,
                selectedDateStr = selectedGridDate,
                onSelectDate = { selectedGridDate = it },
                onEditSelectedDate = { editingDateStr = it }
            )
        }

        // 4. Daily Productive Streak Check-In Card (Food / Exercise / Task inputs + 7-day quick strip)
        item {
            DailyProductiveStreakCard(
                state = state,
                onLogFoodItem = onLogFoodItem,
                onLogExerciseSet = onLogExerciseSet,
                onLogProductiveTask = onLogProductiveTask,
                onRemoveProductiveTask = onRemoveProductiveTask,
                onUndoFood = onUndoFood,
                onUndoWater = onUndoWater,
                onUpdateHistoricalDayFoodLog = onUpdateHistoricalDayFoodLog
            )
        }

        // 5. Complete 0..7 Streak-Based Pokémon Class Promotion Ladder
        item {
            StreakClassPromotionLadderCard(
                streakDays = ps.streakDays,
                bestStreakDays = maxOf(ps.bestStreakDays, ps.streakDays),
                currentTierLevel = currentTier.tierLevel
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun StreakHeatmapGridCard(
    state: HunterSystemState,
    isDark: Boolean,
    selectedDateStr: String,
    onSelectDate: (String) -> Unit,
    onEditSelectedDate: (String) -> Unit
) {
    val ps = state.playerStatus
    val calTarget = state.statBreakdown.vitality.caloriesTarget.coerceAtLeast(1000)
    val todayStr = StreakDateUtils.todayDateString()
    val inFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val outFmt = remember { SimpleDateFormat("MMM d", Locale.US) }

    val gridCells = remember(
        ps.productiveHistoryDates,
        ps.dailyLogsByDate,
        ps.restoredDates,
        ps.checkedInToday,
        state.statBreakdown.vitality.caloriesLogged,
        todayStr
    ) {
        (24 downTo 0).map { daysAgo ->
            val dStr = StreakDateUtils.daysAgoDateString(daysAgo)
            val log: DailyDateLog? = ps.dailyLogsByDate[dStr]
            val cals = if (dStr == todayStr) {
                maxOf(state.statBreakdown.vitality.caloriesLogged, log?.calories ?: 0)
            } else {
                log?.calories ?: 0
            }
            val pro = if (dStr == todayStr) {
                maxOf(state.statBreakdown.vitality.proteinG, log?.proteinG ?: 0.0)
            } else {
                log?.proteinG ?: 0.0
            }
            val tasksCount = log?.tasksCompleted?.size ?: if (dStr == todayStr) ps.todayTaskInputs else 0
            val isRestored = ps.restoredDates.contains(dStr)
            val isProductive = ps.productiveHistoryDates.contains(dStr) ||
                cals > 0 ||
                tasksCount > 0 ||
                isRestored ||
                (dStr == todayStr && ps.checkedInToday)

            val level = when {
                !isProductive -> 0
                cals >= (calTarget * 0.90) || (tasksCount >= 2 && cals >= calTarget * 0.6) -> 4
                cals >= (calTarget * 0.65) || isRestored || tasksCount >= 2 -> 3
                cals >= (calTarget * 0.30) || tasksCount >= 1 -> 2
                else -> 1
            }

            val disp = try {
                val parsed = inFmt.parse(dStr)
                if (parsed != null) outFmt.format(parsed) else dStr
            } catch (_: Exception) {
                dStr
            }

            HeatmapDayCell(
                dateStr = dStr,
                displayLabel = disp,
                isToday = dStr == todayStr,
                isRestored = isRestored,
                intensityLevel = level,
                calories = cals,
                proteinG = pro,
                tasksCount = tasksCount,
                isProductive = isProductive
            )
        }
    }

    // Theme-aware 5x5 Heatmap Palette matching the attached rounded-square aesthetic
    val outerCardBg = if (isDark) Color(0xFF14161F) else Color(0xFFFFFFFF)
    val outerBorder = if (isDark) Color(0xFF2B2F44) else Color(0xFFE2E8F0)
    val shadePalette = if (isDark) {
        listOf(
            Color(0xFF232533), // 0: Inactive
            Color(0xFF421C82), // 1: Spark
            Color(0xFF6D28D9), // 2: Consistent
            Color(0xFF8B5CF6), // 3: Strong / Restored
            Color(0xFFC4B5FD)  // 4: Peak Target Hit
        )
    } else {
        listOf(
            Color(0xFFF1F5F9), // 0: Inactive
            Color(0xFFDDD6FE), // 1: Spark
            Color(0xFFA78BFA), // 2: Consistent
            Color(0xFF7C3AED), // 3: Strong / Restored
            Color(0xFF4C1D95)  // 4: Peak Target Hit
        )
    }
    val highlightAccent = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9)
    val selectedCell = gridCells.firstOrNull { it.dateStr == selectedDateStr } ?: gridCells.last()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(outerCardBg)
            .border(1.5.dp, outerBorder, RoundedCornerShape(28.dp))
            .padding(20.dp)
            .testTag("streak_heatmap_grid_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card Title & Summary Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "25-DAY CONSISTENCY MATRIX",
                    style = MaterialTheme.typography.titleSmall,
                    color = highlightAccent,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${gridCells.count { it.isProductive }} / 25 verified days • Tap any square",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Surface(
                color = highlightAccent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, highlightAccent.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "${ps.streakDays}D STREAK",
                    style = MaterialTheme.typography.labelMedium,
                    color = highlightAccent,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // 5x5 Heatmap Squircle Grid (matches attached image format!)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            for (rowIdx in 0 until 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    for (colIdx in 0 until 5) {
                        val cell = gridCells[rowIdx * 5 + colIdx]
                        val tileColor = shadePalette[cell.intensityLevel.coerceIn(0, 4)]
                        val isSelected = cell.dateStr == selectedCell.dateStr
                        val borderStroke = when {
                            isSelected -> BorderStroke(2.2.dp, HunterGold)
                            cell.isToday -> BorderStroke(1.5.dp, highlightAccent)
                            else -> null
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(tileColor)
                                .then(
                                    if (borderStroke != null) {
                                        Modifier.border(borderStroke, RoundedCornerShape(11.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelectDate(cell.dateStr) }
                                .testTag("heatmap_cell_${cell.dateStr}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cell.isRestored) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(HunterGold)
                                )
                            } else if (cell.isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (cell.intensityLevel >= 3 && !isDark) Color.White else HunterGold
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Intensity Legend Row ("Gold dot = Restored" + "Less [][][][][] More")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(HunterGold)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Gold dot = Restored day",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    softWrap = false
                )
                shadePalette.forEach { shade ->
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(shade)
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(3.dp))
                    )
                }
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Selected Tile Details & Quick Edit Button
        Surface(
            color = ObsidianBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            append(selectedCell.displayLabel)
                            if (selectedCell.isToday) append(" (Today)")
                            if (selectedCell.isRestored) append(" • Restored via 1,000 XP")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selectedCell.isProductive) highlightAccent else TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (selectedCell.isProductive) {
                            "${selectedCell.calories} kcal • ${selectedCell.proteinG.toInt()}g P • ${selectedCell.tasksCount} tasks"
                        } else {
                            "No logged food, workout, or task on this date."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                OutlinedButton(
                    onClick = { onEditSelectedDate(selectedCell.dateStr) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, highlightAccent),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("edit_selected_heatmap_day_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit day log",
                        tint = highlightAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "EDIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = highlightAccent,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakClassPromotionLadderCard(
    streakDays: Int,
    bestStreakDays: Int,
    currentTierLevel: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianCard)
            .border(1.5.dp, HunterGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("streak_class_promotion_ladder"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = HunterGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STREAK CLASS ROADMAP (LV 0 – LV 7)",
                    style = MaterialTheme.typography.titleSmall,
                    color = HunterGold,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "Best: ${bestStreakDays}D",
                style = MaterialTheme.typography.labelMedium,
                color = HunterCyan,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Numeric XP Levels rise every ${TrainerRankProgression.xpPerLevel} XP earned. Pokémon Class Promotion & Class Achievements require consecutive verified streak days:",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        HorizontalDivider(color = BorderSubtle)

        TrainerRankProgression.tiers.forEach { tier ->
            val isCurrent = tier.tierLevel == currentTierLevel
            val isUnlocked = streakDays >= tier.requiredStreakDays
            val borderCol = when {
                isCurrent -> HunterGold
                isUnlocked -> HunterEmerald.copy(alpha = 0.65f)
                else -> BorderSubtle
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isCurrent) ObsidianCardElevated else ObsidianBg)
                    .border(if (isCurrent) 1.5.dp else 1.dp, borderCol, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianCard)
                        .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PikachuMascotBadge(
                        drawableRes = tier.pokemonDrawableRes,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lv ${tier.tierLevel} • ${tier.classTitle} (${tier.pokemonName})",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isUnlocked) TextPrimary else TextMuted,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Surface(
                            color = when {
                                isCurrent -> HunterGold.copy(alpha = 0.2f)
                                isUnlocked -> HunterEmerald.copy(alpha = 0.16f)
                                else -> ObsidianCard
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        isCurrent -> Icons.Filled.Bolt
                                        isUnlocked -> Icons.Filled.CheckCircle
                                        else -> Icons.Filled.Lock
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isCurrent -> HunterGold
                                        isUnlocked -> HunterEmerald
                                        else -> TextMuted
                                    },
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${tier.requiredStreakDays}d",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isCurrent -> HunterGold
                                        isUnlocked -> HunterEmerald
                                        else -> TextMuted
                                    },
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Text(
                        text = "${tier.streakRangeLabel} • ${tier.rewardTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnlocked) HunterCyan else TextMuted
                    )
                }
            }
        }
    }
}
