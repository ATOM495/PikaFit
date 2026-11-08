package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HunterSystemState
import com.example.ui.theme.*

@Composable
fun ModernDashboardScreen(
    state: HunterSystemState,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onAddWorkoutClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onQuickCalories: (Int, Double) -> Unit,
    onQuickWater: (Double) -> Unit
) {
    val cardBg = if (isDark) DarkCardBg else LightCardBg
    val borderColor = if (isDark) DarkBorder else LightBorder
    val textPrimary = if (isDark) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val progress = (state.playerStatus.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)

    var taskList by remember {
        mutableStateOf(
            listOf(
                "Review Q3 Project Goals" to true,
                "Write draft for newsletter" to false
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Header: Greeting + Theme Switcher + Avatar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    val dynamicGreeting = when (currentHour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        in 17..20 -> "Good evening"
                        else -> "Good night"
                    }
                    Text(
                        text = "$dynamicGreeting,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
                    )
                    Text(
                        text = "${state.biometrics.hunterName}!",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dark / Light Theme Toggle
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = if (isDark) AccentTeal else WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Avatar Thumbnail
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AccentTeal.copy(alpha = 0.2f))
                            .border(1.5.dp, AccentTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = AccentTeal,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // 1. Today's Focus Card (Radial Progress + Badges)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(26.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Focus",
                            style = MaterialTheme.typography.titleMedium,
                            color = textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isDark) Color(0xFF1E283D) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "today",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Radial Donut
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val trackColor = if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(AccentTeal, AccentPurple, AccentTeal)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary
                                )
                            }
                        }

                        // Status Chips Column
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AccentPurple.copy(alpha = if (isDark) 0.15f else 0.10f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Workouts", color = AccentPurple, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AccentTeal.copy(alpha = if (isDark) 0.15f else 0.10f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentTeal.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Checklist, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tasks", color = AccentTeal, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Fitness & Workouts Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fitness & Workouts",
                    style = MaterialTheme.typography.titleMedium,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onAddWorkoutClick) {
                    Text("+ Add Workout", color = textSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Glow / Gradient Workout Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                AccentTeal.copy(alpha = if (isDark) 0.25f else 0.15f),
                                AccentPurple.copy(alpha = if (isDark) 0.25f else 0.15f)
                            )
                        )
                    )
                    .border(1.dp, AccentTeal.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { /* Start workout action */ },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(AccentTeal, AccentTealDark)))
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Start", tint = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Cardio & Strength",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "45 mins • 350 kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }

                    // Pulse Wave Indicator
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Productivity Tasks Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Productivity Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onAddTaskClick) {
                    Text("+ Add Task", color = textSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                taskList.forEachIndexed { index, (taskTitle, isDone) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable {
                                taskList = taskList.toMutableList().also {
                                    it[index] = taskTitle to !isDone
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDone) AccentPurple else Color.Transparent)
                                .border(
                                    1.5.dp,
                                    if (isDone) AccentPurple else textSecondary,
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = taskTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (isDone) TextDecoration.LineThrough else null
                            ),
                            color = if (isDone) textSecondary else textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Clearance for bottom navigation
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddTaskClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            containerColor = if (isDark) DarkCardElevated else LightSurface,
            contentColor = AccentTeal,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Item")
        }
    }
}