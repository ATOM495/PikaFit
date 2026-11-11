package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.HunterSystemState
import com.example.ui.theme.AppThemeIsDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.HunterCyan
import com.example.ui.theme.HunterEmerald
import com.example.ui.theme.HunterGold
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NutritionChatMessage

@Composable
fun PikaBotScreen(
    state: HunterSystemState,
    messages: List<NutritionChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onLogRecommendedFood: (mealName: String, calories: Int, proteinG: Double) -> Unit,
    onApplyDietPlan: (
        messageId: String,
        goalKey: String,
        planTitle: String,
        caloriesTarget: Int,
        proteinTargetG: Int,
        waterTargetLiters: Double
    ) -> Unit = { _, _, _, _, _, _ -> },
    onLogRecommendedExercise: (
        messageId: String,
        exerciseName: String,
        sets: Int,
        reps: String
    ) -> Unit = { _, _, _, _ -> }
) {
    val isDark = AppThemeIsDark
    val bio = state.biometrics
    val vit = state.statBreakdown.vitality
    val stam = state.statBreakdown.stamina
    val currentTier = state.playerStatus.currentTier

    val accentYellow = Color(0xFFFFDE00)
    val accentCrimson = Color(0xFFEF233C)
    val primaryAccent = if (isDark) accentYellow else accentCrimson

    var queryInput by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "💪 Workout Plan" to "Give me a workout plan for ${bio.goalDisplayLabel}",
        "🏋️ Chest Exercises" to "Best chest exercises",
        "💪 Bicep Exercises" to "Bicep exercises for peak and thickness",
        "🔥 Back & Lats" to "Back and lat exercises",
        "🦵 Leg Workout" to "Complete leg exercises",
        "⚡ Abs & Core" to "Six pack abs exercises",
        "🥙 Bun Shawarma" to "1 bun shawarma",
        "🍟 Chicken Loaded Fries" to "1 chicken loaded fries",
        "🍛 South Indian Diet" to "Switch to South Indian ${bio.goalDisplayLabel} diet plan",
        "🫓 North Indian Diet" to "Switch to North Indian ${bio.goalDisplayLabel} diet plan",
        "🍗 Chicken Gravy" to "180g chicken gravy",
        "🥞 3 Dosa 4 Idli" to "3 dosa 4 idli",
        "🍚 Chicken Biryani" to "1 plate chicken biryani"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("pikabot_screen")
    ) {
        // 1. PikaBot Aesthetic Hero Header Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ObsidianCardElevated,
                            ObsidianCard
                        )
                    )
                )
                .border(1.5.dp, primaryAccent.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
                .padding(16.dp)
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
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ObsidianBg)
                            .border(2.dp, accentYellow, CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PikachuMascotBadge(
                            drawableRes = R.drawable.pika_logo_hi,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "PIKABOT • DIET & WORKOUT AI",
                                style = MaterialTheme.typography.titleLarge,
                                color = primaryAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "South/North Indian & Fast Foods + Muscle Exercises + Workout & Diet Plans",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ObsidianBg)
                        .border(1.dp, BorderSubtle, CircleShape)
                        .testTag("pikabot_clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Clear PikaBot Chat",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Nutrition & Class Status Pills
            val calLeft = (vit.caloriesTarget - vit.caloriesLogged).coerceAtLeast(0)
            val proLeft = (bio.recommendedProteinG - vit.proteinG.toInt()).coerceAtLeast(0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PikaBotStatusChip(
                    label = "GOAL: ${bio.goalDisplayLabel.uppercase()}",
                    color = HunterGold
                )
                PikaBotStatusChip(
                    label = "KCAL LEFT: $calLeft / ${vit.caloriesTarget}",
                    color = HunterEmerald
                )
                PikaBotStatusChip(
                    label = "PROTEIN LEFT: ${proLeft}g",
                    color = HunterCyan
                )
                PikaBotStatusChip(
                    label = "WATER: ${stam.waterLiters}L / ${stam.targetLiters}L",
                    color = HunterCyan
                )
                PikaBotStatusChip(
                    label = "CLASS: ${currentTier.classTitle.uppercase()}",
                    color = HunterGold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Food, Exercise & Plan Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEach { (chipLabel, promptText) ->
                    Surface(
                        modifier = Modifier.clickable(enabled = !isLoading) {
                            onSendMessage(promptText)
                        },
                        shape = RoundedCornerShape(50),
                        color = primaryAccent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = chipLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Full-Height Chat Timeline
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ObsidianBg.copy(alpha = 0.86f))
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(12.dp)
                .testTag("pikabot_messages_list"),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ObsidianCardElevated)
                                .border(1.5.dp, accentYellow, CircleShape)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PikachuMascotBadge(
                                drawableRes = R.drawable.pika_logo_hi,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = if (isUser) 18.dp else 6.dp,
                            topEnd = if (isUser) 6.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        ),
                        color = if (isUser) {
                            accentCrimson
                        } else if (msg.isError) {
                            Color(0xFFEF4444).copy(alpha = 0.18f)
                        } else {
                            ObsidianCardElevated
                        },
                        border = if (isUser) null else BorderStroke(1.dp, primaryAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.widthIn(max = 330.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isUser) "Trainer ${bio.hunterName}" else "⚡ PikaBot • Food, Diet & Workout AI",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isUser) Color.White.copy(alpha = 0.9f) else primaryAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 21.sp
                                ),
                                color = if (isUser) Color.White else TextPrimary
                            )

                            // Interactive Apply Diet Plan Button when PikaBot generates a custom Diet Plan
                            if (!isUser && !msg.isStreaming && msg.suggestedPlanGoalKey != null && msg.suggestedPlanCaloriesTarget != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onApplyDietPlan(
                                                msg.id,
                                                msg.suggestedPlanGoalKey,
                                                msg.suggestedPlanTitle ?: "Custom Diet Plan",
                                                msg.suggestedPlanCaloriesTarget,
                                                msg.suggestedPlanProteinG ?: 115,
                                                msg.suggestedPlanWaterLiters ?: 3.5
                                            )
                                        }
                                        .testTag("pikabot_apply_diet_plan_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (msg.planApplied) HunterEmerald.copy(alpha = 0.22f) else accentYellow.copy(alpha = 0.20f),
                                    border = BorderStroke(1.5.dp, if (msg.planApplied) HunterEmerald else accentYellow)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = null,
                                            tint = if (msg.planApplied) HunterEmerald else accentYellow,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (msg.planApplied) {
                                                "✓ Applied (${msg.suggestedPlanCaloriesTarget} kcal • ${msg.suggestedPlanProteinG ?: 115}g P)"
                                            } else {
                                                "⚡ Apply Plan (${msg.suggestedPlanCaloriesTarget} kcal • ${msg.suggestedPlanProteinG ?: 115}g P)"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (msg.planApplied) HunterEmerald else accentYellow,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            // Interactive Log Meal Button when PikaBot analyzes a Food
                            if (!isUser && !msg.isStreaming && msg.suggestedMealName != null && msg.suggestedCalories != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onLogRecommendedFood(
                                                msg.suggestedMealName,
                                                msg.suggestedCalories,
                                                msg.suggestedProteinG ?: 25.0
                                            )
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = HunterEmerald.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, HunterEmerald)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Restaurant,
                                            contentDescription = null,
                                            tint = HunterEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Log ${msg.suggestedMealName} (+${msg.suggestedCalories} kcal • ${msg.suggestedProteinG?.toInt() ?: 25}g P)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = HunterEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Interactive Log Exercise Button when PikaBot provides an Exercise / Muscle Guide / Workout Plan
                            if (!isUser && !msg.isStreaming && msg.suggestedExerciseName != null) {
                                val exSets = msg.suggestedExerciseSets ?: 4
                                val exReps = msg.suggestedExerciseReps ?: "10"
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onLogRecommendedExercise(
                                                msg.id,
                                                msg.suggestedExerciseName,
                                                exSets,
                                                exReps
                                            )
                                        }
                                        .testTag("pikabot_log_exercise_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (msg.exerciseLogged) HunterEmerald.copy(alpha = 0.22f) else HunterCyan.copy(alpha = 0.18f),
                                    border = BorderStroke(1.2.dp, if (msg.exerciseLogged) HunterEmerald else HunterCyan)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FitnessCenter,
                                            contentDescription = null,
                                            tint = if (msg.exerciseLogged) HunterEmerald else HunterCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (msg.exerciseLogged) {
                                                "✓ Logged ${msg.suggestedExerciseName} ($exSets × $exReps)"
                                            } else {
                                                "💪 Log Exercise: ${msg.suggestedExerciseName} ($exSets × $exReps)"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (msg.exerciseLogged) HunterEmerald else HunterCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. PikaBot Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = queryInput,
                onValueChange = { queryInput = it },
                placeholder = {
                    Text(
                        text = "Ask anything to PikaBot",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                enabled = !isLoading,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pikabot_chat_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryAccent,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard
                )
            )

            Button(
                onClick = {
                    if (queryInput.isNotBlank()) {
                        onSendMessage(queryInput)
                        queryInput = ""
                    }
                },
                enabled = !isLoading && queryInput.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentCrimson,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("pikabot_send_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send to PikaBot"
                    )
                }
            }
        }
    }
}

@Composable
private fun PikaBotStatusChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
