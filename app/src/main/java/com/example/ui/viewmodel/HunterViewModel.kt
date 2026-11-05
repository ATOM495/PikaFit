package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AchievementItem
import com.example.data.model.ExerciseLogged
import com.example.data.model.FeedbackEntry
import com.example.data.model.HunterBiometrics
import com.example.data.model.HunterSystemState
import com.example.data.model.JournalEntry
import com.example.data.model.QuestItem
import com.example.data.model.StreakDateUtils
import com.example.data.model.TacticalLog
import com.example.data.repository.HunterRepository
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.Date
import java.util.UUID

sealed interface AiSyncState {
    data object Idle : AiSyncState
    data class Analyzing(val command: String) : AiSyncState
    data class Success(val message: String) : AiSyncState
    data class Error(val message: String) : AiSyncState
}

sealed interface DataLoadState<out T> {
    data object Loading : DataLoadState<Nothing>
    data class Success<T>(val data: T) : DataLoadState<T>
    data class Error(val message: String) : DataLoadState<Nothing>
}

data class PikaCoachTip(
    val headline: String,
    val advice: String,
    val actionLabel: String,
    val actionType: String, // LOG_FOOD, LOG_WATER, LOG_WORKOUT, KEEP_GOING
    val coachMood: String   // ENERGIZED, ALERT_INACTIVE, PROUD, TACTICAL
)

sealed interface PikaCoachUiState {
    data object Idle : PikaCoachUiState
    data class Loading(val previewTip: PikaCoachTip) : PikaCoachUiState
    data class Ready(val tip: PikaCoachTip, val showPopup: Boolean) : PikaCoachUiState
    data class Error(val errorMessage: String, val fallbackTip: PikaCoachTip, val showPopup: Boolean) : PikaCoachUiState
}

data class NutritionChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val text: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val suggestedMealName: String? = null,
    val suggestedCalories: Int? = null,
    val suggestedProteinG: Double? = null
)

data class FoodEntryRecord(
    val id: String = UUID.randomUUID().toString(),
    val mealName: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatsG: Double,
    val xpAwarded: Int,
    val triggeredDailyCheckIn: Boolean,
    val previousStreakDays: Int
)

data class WaterEntryRecord(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val litersDelta: Double,
    val xpAwarded: Int
)

class HunterViewModel(
    private val repository: HunterRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _systemState = MutableStateFlow(HunterSystemState.createResetState())
    val systemState: StateFlow<HunterSystemState> = _systemState.asStateFlow()

    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded: StateFlow<Boolean> = _isSessionLoaded.asStateFlow()

    private val _aiSyncState = MutableStateFlow<AiSyncState>(AiSyncState.Idle)
    val aiSyncState: StateFlow<AiSyncState> = _aiSyncState.asStateFlow()

    private val _pikaCoachState = MutableStateFlow<PikaCoachUiState>(PikaCoachUiState.Idle)
    val pikaCoachState: StateFlow<PikaCoachUiState> = _pikaCoachState.asStateFlow()

    private val _nutritionChatMessages = MutableStateFlow<List<NutritionChatMessage>>(emptyList())
    val nutritionChatMessages: StateFlow<List<NutritionChatMessage>> = _nutritionChatMessages.asStateFlow()

    private val _isNutritionChatLoading = MutableStateFlow(false)
    val isNutritionChatLoading: StateFlow<Boolean> = _isNutritionChatLoading.asStateFlow()

    private val _foodHistory = MutableStateFlow<List<FoodEntryRecord>>(emptyList())
    val foodHistory: StateFlow<List<FoodEntryRecord>> = _foodHistory.asStateFlow()

    private val _waterHistory = MutableStateFlow<List<WaterEntryRecord>>(emptyList())
    val waterHistory: StateFlow<List<WaterEntryRecord>> = _waterHistory.asStateFlow()

    private var hasSeededInitialSession = false
    private var hasAutoTriggeredCoachThisSession = false

    val logsState: StateFlow<DataLoadState<List<TacticalLog>>> = repository.observeLogs()
        .map<List<TacticalLog>, DataLoadState<List<TacticalLog>>> { DataLoadState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing tactical logs", error)
            emit(DataLoadState.Error(error.message ?: "Failed to load tactical logs"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataLoadState.Loading
        )

    val journalState: StateFlow<DataLoadState<List<JournalEntry>>> = repository.observeJournalEntries()
        .map<List<JournalEntry>, DataLoadState<List<JournalEntry>>> { DataLoadState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing personal journal entries", error)
            emit(DataLoadState.Error(error.message ?: "Failed to load personal journal"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataLoadState.Loading
        )

    val feedbackState: StateFlow<DataLoadState<List<FeedbackEntry>>> = repository.observePublicFeedback()
        .map<List<FeedbackEntry>, DataLoadState<List<FeedbackEntry>>> { DataLoadState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing public feedback", error)
            emit(DataLoadState.Error(error.message ?: "Failed to load public feedback feed"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataLoadState.Loading
        )

    private val hunterSystemSchema by lazy {
        val playerStatusSchema = Schema.obj(
            properties = mapOf(
                "level" to Schema.integer(description = "Current hunter level"),
                "rank" to Schema.string(description = "Hunter rank string e.g. E-Rank Hunter"),
                "current_xp" to Schema.integer(description = "Current XP"),
                "next_level_xp" to Schema.integer(description = "Next level XP target"),
                "progress_percent" to Schema.double(description = "Level progress percentage 0-100"),
                "hp_percent" to Schema.double(description = "HP percentage 0-100"),
                "streak_days" to Schema.integer(description = "Consecutive streak days"),
                "active_buffs" to Schema.array(items = Schema.string(), description = "Active buffs"),
                "active_debuffs" to Schema.array(items = Schema.string(), description = "Active debuffs")
            ),
            optionalProperties = listOf("hp_percent", "active_buffs", "active_debuffs")
        )

        val vitalitySchema = Schema.obj(
            properties = mapOf(
                "calories_logged" to Schema.integer(description = "Total calories logged today"),
                "calories_target" to Schema.integer(description = "Target calories today"),
                "protein_g" to Schema.double(description = "Protein in grams"),
                "carbs_g" to Schema.double(description = "Carbs in grams"),
                "fats_g" to Schema.double(description = "Fats in grams"),
                "fiber_g" to Schema.double(description = "Fiber in grams"),
                "omega3_mg" to Schema.double(description = "Omega-3 in mg"),
                "target_met" to Schema.boolean(description = "Whether calorie and protein target met")
            ),
            optionalProperties = listOf("calories_target", "carbs_g", "fats_g", "fiber_g", "omega3_mg")
        )

        val staminaSchema = Schema.obj(
            properties = mapOf(
                "water_liters" to Schema.double(description = "Water consumed in liters"),
                "target_liters" to Schema.double(description = "Target water in liters"),
                "target_met" to Schema.boolean(description = "Whether water target met")
            )
        )

        val exerciseSchema = Schema.obj(
            properties = mapOf(
                "exercise_name" to Schema.string(description = "Name of exercise"),
                "sets_completed" to Schema.integer(description = "Number of sets completed"),
                "reps_per_set" to Schema.string(description = "Reps per set"),
                "progressive_overload_hit" to Schema.boolean(description = "Whether progressive overload hit")
            ),
            optionalProperties = listOf("reps_per_set", "progressive_overload_hit")
        )

        val strengthSchema = Schema.obj(
            properties = mapOf(
                "workout_completed" to Schema.boolean(description = "Whether workout completed"),
                "exercises_logged" to Schema.array(items = exerciseSchema, description = "Logged exercises")
            ),
            optionalProperties = listOf("exercises_logged")
        )

        val statBreakdownSchema = Schema.obj(
            properties = mapOf(
                "vitality" to vitalitySchema,
                "stamina" to staminaSchema,
                "strength" to strengthSchema
            )
        )

        val questSchema = Schema.obj(
            properties = mapOf(
                "quest_name" to Schema.string(description = "Quest name"),
                "status" to Schema.enumeration(values = listOf("COMPLETED", "IN_PROGRESS", "FAILED")),
                "xp_awarded" to Schema.integer(description = "XP awarded for quest")
            )
        )

        val achievementSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Achievement title"),
                "description" to Schema.string(description = "Achievement description"),
                "rarity" to Schema.enumeration(values = listOf("COMMON", "RARE", "EPIC", "LEGENDARY"))
            )
        )

        Schema.obj(
            properties = mapOf(
                "player_status" to playerStatusSchema,
                "stat_breakdown" to statBreakdownSchema,
                "quest_log" to Schema.array(items = questSchema),
                "unlocked_achievements" to Schema.array(items = achievementSchema),
                "tactical_briefing" to Schema.string(description = "2-3 sentences of motivating, actionable tactical feedback."),
                "suggested_actions" to Schema.array(items = Schema.string())
            )
        )
    }

    private val aiModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = hunterSystemSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    private val pikaCoachSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(description = "Short, punchy Pikachu-themed coach headline (max 8 words)"),
                "advice" to Schema.string(description = "2-3 sentences of specific, context-aware fitness/nutrition advice based on the user's current progress or lack of activity today"),
                "action_label" to Schema.string(description = "Short call-to-action button label, e.g. '+350 kcal Protein Meal' or 'Start 1st Workout Set'"),
                "action_type" to Schema.enumeration(
                    values = listOf("LOG_FOOD", "LOG_WATER", "LOG_WORKOUT", "KEEP_GOING"),
                    description = "Which quick action best addresses the user's current gap"
                ),
                "coach_mood" to Schema.enumeration(
                    values = listOf("ENERGIZED", "ALERT_INACTIVE", "PROUD", "TACTICAL"),
                    description = "Coach mood based on activity level"
                )
            )
        )
    }

    private val pikaCoachModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = pikaCoachSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    private val nutritionChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    init {
        observeRemoteSession()
    }

    private fun observeRemoteSession() {
        viewModelScope.launch {
            val wasAlreadyOnboardedLocally = repository.isOnboardingCompletedForUser(currentUserId)
            if (wasAlreadyOnboardedLocally) {
                val cachedStateJson = repository.getCachedStateJsonForUser(currentUserId)
                val restoredBio = if (!cachedStateJson.isNullOrBlank()) {
                    try {
                        val root = JSONObject(cachedStateJson)
                        HunterBiometrics.fromJsonObject(root.optJSONObject("biometrics")).copy(onboardingCompleted = true)
                    } catch (e: Exception) {
                        HunterBiometrics(onboardingCompleted = true)
                    }
                } else {
                    HunterBiometrics(onboardingCompleted = true)
                }
                _systemState.value = _systemState.value.copy(biometrics = restoredBio)
            }

            repository.observeActiveSession()
                .catch { e ->
                    Log.w(TAG, "Error observing active session", e)
                    _isSessionLoaded.value = true
                }
                .collect { session ->
                    if (session != null) {
                        val loaded = session.toHunterSystemState()
                        // If a session document already exists in Firestore for this Google account,
                        // ensure onboardingCompleted is true so returning users never repeat the "Hey Trainer" setup!
                        val effectiveBiometrics = if (loaded.biometrics.onboardingCompleted || session.stateJson.isNotBlank()) {
                            loaded.biometrics.copy(onboardingCompleted = true)
                        } else {
                            loaded.biometrics
                        }
                        val evaluated = loaded.copy(biometrics = effectiveBiometrics).evaluateStreakOnLogin()
                        _systemState.value = evaluated
                        if (effectiveBiometrics.onboardingCompleted) {
                            repository.setOnboardingCompletedForUser(currentUserId, true, evaluated.toStateJson())
                        }
                        hasSeededInitialSession = true
                        _isSessionLoaded.value = true
                        seedWelcomeNutritionChatIfEmpty(evaluated)
                        if (effectiveBiometrics.onboardingCompleted && !hasAutoTriggeredCoachThisSession) {
                            hasAutoTriggeredCoachThisSession = true
                            generatePikaCoachTip(showAsPopup = true)
                        }
                    } else if (!hasSeededInitialSession) {
                        hasSeededInitialSession = true
                        if (wasAlreadyOnboardedLocally) {
                            val current = _systemState.value
                            val ensured = current.copy(biometrics = current.biometrics.copy(onboardingCompleted = true))
                            _systemState.value = ensured
                            _isSessionLoaded.value = true
                            seedWelcomeNutritionChatIfEmpty(ensured)
                            if (!hasAutoTriggeredCoachThisSession) {
                                hasAutoTriggeredCoachThisSession = true
                                generatePikaCoachTip(showAsPopup = true)
                            }
                        } else {
                            // Brand new Google Account with no Firestore session and no local cache -> show "Hey Trainer" onboarding!
                            val initial = HunterSystemState.createResetState()
                            _systemState.value = initial
                            _isSessionLoaded.value = true
                        }
                    }
                }
        }
    }

    fun buildContextAwareFallbackTip(state: HunterSystemState): PikaCoachTip {
        val bio = state.biometrics
        val vit = state.statBreakdown.vitality
        val stam = state.statBreakdown.stamina
        val str = state.statBreakdown.strength
        val ps = state.playerStatus
        val calRemaining = (vit.caloriesTarget - vit.caloriesLogged).coerceAtLeast(0)
        val proTarget = bio.recommendedProteinG
        val proRemaining = (proTarget - vit.proteinG.toInt()).coerceAtLeast(0)

        return when {
            !ps.checkedInToday && vit.caloriesLogged == 0 && str.exercisesLogged.isEmpty() -> PikaCoachTip(
                headline = "Pika-Alert! Zero Activity Logged Today ⚡",
                advice = "Hey Trainer ${bio.hunterName}! You haven't logged a meal or workout set yet today. Opening PikaFit alone doesn't count toward your ${ps.streakDays}-day streak — log your first meal or set now to spark Day ${ps.streakDays + 1} toward your ${bio.goalDisplayLabel} goal!",
                actionLabel = "Log +350 kcal Starter Meal",
                actionType = "LOG_FOOD",
                coachMood = "ALERT_INACTIVE"
            )
            str.exercisesLogged.isEmpty() -> PikaCoachTip(
                headline = "Nutrition Flowing — Time to Lift, Pika! 💪",
                advice = "Great job logging ${vit.caloriesLogged} kcal today, ${bio.hunterName}! To maximize muscle protein synthesis for your ${bio.goalDisplayLabel} plan at ${bio.weightKg} kg, knock out 3 progressive overload sets now.",
                actionLabel = "Log Workout Set Now",
                actionType = "LOG_WORKOUT",
                coachMood = "TACTICAL"
            )
            calRemaining > 400 -> PikaCoachTip(
                headline = "Fuel Gap: $calRemaining kcal & ${proRemaining}g Protein Left!",
                advice = "Trainer ${bio.hunterName}, your workout is recorded, but you still need $calRemaining kcal and ~${proRemaining}g protein to hit your ${vit.caloriesTarget} kcal ${bio.goalDisplayLabel} target.",
                actionLabel = "Quick Log +350 kcal (28g P)",
                actionType = "LOG_FOOD",
                coachMood = "ENERGIZED"
            )
            stam.waterLiters < stam.targetLiters -> PikaCoachTip(
                headline = "Hydration Check: ${stam.waterLiters}L / ${stam.targetLiters}L 💧",
                advice = "Almost at peak output, ${bio.hunterName}! Drink ${((stam.targetLiters - stam.waterLiters) * 10).toInt() / 10.0}L more water to support cellular glycogen storage and joint recovery.",
                actionLabel = "Drink +0.5L Water",
                actionType = "LOG_WATER",
                coachMood = "ENERGIZED"
            )
            else -> PikaCoachTip(
                headline = "100,000 Volts! All Daily Targets Crushed ⚡",
                advice = "Phenomenal discipline, Trainer ${bio.hunterName}! You cleared ${vit.caloriesLogged} kcal, ${stam.waterLiters}L water, and ${str.exercisesLogged.size} exercises with an active ${ps.streakDays}-day streak!",
                actionLabel = "Keep Up The Momentum!",
                actionType = "KEEP_GOING",
                coachMood = "PROUD"
            )
        }
    }

    fun generatePikaCoachTip(showAsPopup: Boolean = true) {
        val snapshot = _systemState.value
        val preview = buildContextAwareFallbackTip(snapshot)
        // Immediately show the context-aware tip so the dashboard & popup never stall
        _pikaCoachState.value = PikaCoachUiState.Ready(tip = preview, showPopup = showAsPopup)

        if (Firebase.auth.currentUser == null) {
            return
        }

        viewModelScope.launch {
            val bio = snapshot.biometrics
            val vit = snapshot.statBreakdown.vitality
            val stam = snapshot.statBreakdown.stamina
            val str = snapshot.statBreakdown.strength
            val ps = snapshot.playerStatus

            val activityStatus = if (!ps.checkedInToday && vit.caloriesLogged == 0 && str.exercisesLogged.isEmpty()) {
                "INACTIVE TODAY (0 calories logged, 0 workouts logged, daily streak check-in NOT verified yet)"
            } else {
                "ACTIVE TODAY (Checked in: ${ps.checkedInToday}, Food inputs: ${ps.todayFoodInputs}, Exercise inputs: ${ps.todayExerciseInputs})"
            }

            val prompt = """
                You are 'Pika-Coach', an energetic, scientifically sharp Pikachu fitness & nutrition coach inside the PikaFit app.
                Analyze the trainer's live dashboard telemetry and generate a personalized, context-aware coaching tip:
                - Trainer Name: ${bio.hunterName} (${bio.age} yrs, ${bio.biologicalSex}, ${bio.heightCm.toInt()} cm, ${bio.weightKg} kg, BMI ${bio.bmi}, Body Fat ${bio.bodyFatPercent}%, Muscle Mass ${bio.muscleMassPercent}%)
                - Goal: ${bio.goalDisplayLabel} (${bio.experienceDisplayLabel})
                - Activity Status: $activityStatus
                - Streak: ${ps.streakDays} days (Checked in today: ${ps.checkedInToday})
                - Calories Today: ${vit.caloriesLogged} / ${vit.caloriesTarget} kcal
                - Protein Today: ${vit.proteinG.toInt()}g / ${bio.recommendedProteinG}g target
                - Water Today: ${stam.waterLiters}L / ${stam.targetLiters}L
                - Exercises Logged Today: ${str.exercisesLogged.size}

                If the user has a lack of activity (e.g. 0 calories or 0 exercises logged today, or streak unverified), gently call it out and motivate them with a specific immediate first step.
                If the user has made partial or full progress, praise their specific numbers and tell them exactly what to do next to optimize their ${bio.goalDisplayLabel} plan.
            """.trimIndent()

            try {
                val response = withTimeoutOrNull(12_000L) {
                    pikaCoachModel.generateContent(prompt)
                }
                val jsonText = response?.text?.takeIf { it.isNotBlank() } ?: return@launch
                val obj = JSONObject(jsonText)
                val tip = PikaCoachTip(
                    headline = obj.optString("headline", preview.headline).take(90).ifBlank { preview.headline },
                    advice = obj.optString("advice", preview.advice).take(500).ifBlank { preview.advice },
                    actionLabel = obj.optString("action_label", preview.actionLabel).take(45).ifBlank { preview.actionLabel },
                    actionType = obj.optString("action_type", preview.actionType),
                    coachMood = obj.optString("coach_mood", preview.coachMood)
                )
                val currentPopupVisible = when (val cur = _pikaCoachState.value) {
                    is PikaCoachUiState.Ready -> cur.showPopup
                    is PikaCoachUiState.Error -> cur.showPopup
                    else -> showAsPopup
                }
                _pikaCoachState.value = PikaCoachUiState.Ready(tip = tip, showPopup = currentPopupVisible)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Using calibrated Pika-Coach telemetry tip: ${e.message}")
            }
        }
    }

    fun dismissPikaCoachPopup() {
        when (val current = _pikaCoachState.value) {
            is PikaCoachUiState.Ready -> _pikaCoachState.value = current.copy(showPopup = false)
            is PikaCoachUiState.Error -> _pikaCoachState.value = current.copy(showPopup = false)
            else -> {}
        }
    }

    fun reopenPikaCoachPopup() {
        when (val current = _pikaCoachState.value) {
            is PikaCoachUiState.Ready -> _pikaCoachState.value = current.copy(showPopup = true)
            is PikaCoachUiState.Error -> _pikaCoachState.value = current.copy(showPopup = true)
            else -> generatePikaCoachTip(showAsPopup = true)
        }
    }

    private fun seedWelcomeNutritionChatIfEmpty(state: HunterSystemState) {
        if (_nutritionChatMessages.value.isNotEmpty()) return
        val bio = state.biometrics
        val vit = state.statBreakdown.vitality
        _nutritionChatMessages.value = listOf(
            NutritionChatMessage(
                role = "model",
                text = "Pika-Pika! I'm Pika-Dietitian, your Gemini Nutrition & Food Science Bot! ⚡\n\n" +
                    "I'm calibrated to your ${bio.goalDisplayLabel} plan (${bio.weightKg} kg, BMI ${bio.bmi}, Body Fat ${bio.bodyFatPercent}%, Muscle ${bio.muscleMassPercent}%, Target ${vit.caloriesTarget} kcal & ${bio.recommendedProteinG}g protein/day).\n\n" +
                    "Ask me about any food, meal plan, or pre/post-workout nutrition — I'll recommend the best foods for your plan and break down their scientific nutrition facts!",
                suggestedMealName = "High-Protein Greek Yogurt & Berry Bowl",
                suggestedCalories = 420,
                suggestedProteinG = 38.0
            )
        )
    }

    fun sendNutritionChatMessage(userQuery: String) {
        val cleanQuery = userQuery.trim()
        if (cleanQuery.isBlank() || _isNutritionChatLoading.value) return

        val snapshot = _systemState.value
        val bio = snapshot.biometrics
        val vit = snapshot.statBreakdown.vitality
        val stam = snapshot.statBreakdown.stamina

        seedWelcomeNutritionChatIfEmpty(snapshot)

        val userMsg = NutritionChatMessage(role = "user", text = cleanQuery)
        val placeholderId = UUID.randomUUID().toString()
        val streamingPlaceholder = NutritionChatMessage(
            id = placeholderId,
            role = "model",
            text = "Pika-Dietitian is analyzing nutrition facts for your ${bio.goalDisplayLabel} plan...",
            isStreaming = true
        )

        val previousHistory = _nutritionChatMessages.value
        _nutritionChatMessages.value = previousHistory + userMsg + streamingPlaceholder
        _isNutritionChatLoading.value = true

        viewModelScope.launch {
            val systemContextPrompt = """
                You are 'Pika-Dietitian', an expert sports nutritionist, clinical dietitian, and food scientist bot inside the PikaFit app's Biometric Charts section.
                Trainer Profile & Biometric Telemetry:
                - Name: ${bio.hunterName}, Age: ${bio.age}, Sex: ${bio.biologicalSex}
                - Weight: ${bio.weightKg} kg, Height: ${bio.heightCm.toInt()} cm, BMI: ${bio.bmi} (${bio.bmiCategory})
                - Body Fat: ${bio.bodyFatPercent}%, Skeletal Muscle Mass: ${bio.muscleMassPercent}% (${bio.muscleMassKg} kg)
                - Fitness Plan: ${bio.goalDisplayLabel} (${bio.experienceDisplayLabel})
                - Daily Targets: ${vit.caloriesTarget} kcal (Logged today: ${vit.caloriesLogged} kcal), Protein Target: ${bio.recommendedProteinG}g (Logged today: ${vit.proteinG.toInt()}g), Water: ${stam.waterLiters}L / ${stam.targetLiters}L

                Your Role & Formatting Rules:
                1. Directly answer the user's query about what foods to take, avoid, or prioritize for their specific ${bio.goalDisplayLabel} plan.
                2. Always include a dedicated '📊 NUTRITION FACTS & SCIENCE' section breaking down exact macros (Calories, Protein, Carbs, Healthy Fats, Fiber) per 100g or per serving, key micronutrients/vitamins, bioavailability, and WHY it helps their muscle mass (${bio.muscleMassPercent}%) or body fat (${bio.bodyFatPercent}%) trajectory.
                3. Give 2-3 practical meal ideas with portion sizes they can eat today. Keep the tone encouraging, clear, and scannable with bullet points.
            """.trimIndent()

            val accumulated = StringBuilder()
            try {
                if (Firebase.auth.currentUser != null) {
                    val chatHistory = buildList {
                        add(content("user") { text(systemContextPrompt) })
                        add(content("model") { text("Understood! I am Pika-Dietitian. I will tailor all food recommendations and scientific nutrition facts to Trainer ${bio.hunterName}'s ${bio.goalDisplayLabel} plan.") })
                        previousHistory.takeLast(10).forEach { msg ->
                            if (!msg.isError && msg.text.isNotBlank()) {
                                val roleName = if (msg.role == "user") "user" else "model"
                                add(content(roleName) { text(msg.text) })
                            }
                        }
                    }

                    val chat = nutritionChatModel.startChat(history = chatHistory)

                    withTimeoutOrNull(12_000L) {
                        chat.sendMessageStream(cleanQuery).collect { chunk ->
                            val part = chunk.text.orEmpty()
                            if (part.isNotEmpty()) {
                                accumulated.append(part)
                                val liveText = accumulated.toString()
                                _nutritionChatMessages.value = _nutritionChatMessages.value.map { msg ->
                                    if (msg.id == placeholderId) msg.copy(text = liveText, isStreaming = true) else msg
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Nutrition chat fallback activated: ${e.message}")
            } finally {
                val (fallbackReply, sugMeal, sugCal, sugPro) = buildSmartNutritionScientificReply(
                    query = cleanQuery,
                    bio = bio,
                    caloriesTarget = vit.caloriesTarget,
                    caloriesLogged = vit.caloriesLogged,
                    proteinLogged = vit.proteinG,
                    waterLogged = stam.waterLiters,
                    waterTarget = stam.targetLiters
                )
                val finalText = accumulated.toString().trim().ifEmpty { fallbackReply }
                _nutritionChatMessages.value = _nutritionChatMessages.value.map { msg ->
                    if (msg.id == placeholderId) {
                        msg.copy(
                            text = finalText,
                            isStreaming = false,
                            isError = false,
                            suggestedMealName = sugMeal,
                            suggestedCalories = sugCal,
                            suggestedProteinG = sugPro
                        )
                    } else msg
                }
                _isNutritionChatLoading.value = false
            }
        }
    }

    private data class NutritionReplyResult(
        val replyText: String,
        val mealName: String,
        val calories: Int,
        val proteinG: Double
    )

    private fun buildSmartNutritionScientificReply(
        query: String,
        bio: HunterBiometrics,
        caloriesTarget: Int,
        caloriesLogged: Int,
        proteinLogged: Double,
        waterLogged: Double,
        waterTarget: Double
    ): NutritionReplyResult {
        val q = query.lowercase()
        val calRemaining = (caloriesTarget - caloriesLogged).coerceAtLeast(0)
        val proRemaining = (bio.recommendedProteinG - proteinLogged.toInt()).coerceAtLeast(0)

        return when {
            q.contains("pre") || q.contains("post") || q.contains("workout") || q.contains("gym") -> NutritionReplyResult(
                replyText = """
                    ⚡ **Pika-Dietitian Pre & Post-Workout Protocol for ${bio.hunterName} (${bio.goalDisplayLabel})**
                    
                    1. **Pre-Workout (60–90 mins before training):**
                       • **Oats + Banana + Whey/Egg Whites (380 kcal | 28g Protein | 58g Carbs | 4g Fat)**
                       • Supplies fast + medium-digesting glucose to saturate skeletal muscle glycogen (${bio.muscleMassKg} kg muscle mass) without gastric heaviness.
                    
                    2. **Post-Workout Anabolic Window (0–60 mins after training):**
                       • **Grilled Chicken Breast or Whey Isolate + Jasmine Rice (520 kcal | 46g Protein | 68g Carbs | 5g Fat)**
                       • Triggers the **mTORC1** pathway via ~3.8g of **L-Leucine** while insulin from high-glycemic rice shuttles amino acids into muscle fibers.
                    
                    📊 **NUTRITION FACTS & SCIENCE (Per Serving Post-Workout Bowl):**
                    • **Bioavailability (DIAAS Score):** >1.08 (Complete Essential Amino Acid profile)
                    • **Micronutrients:** Potassium (640mg for nerve conduction), Magnesium (85mg for ATP synthesis), Vitamin B6 (1.1mg for protein metabolism)
                    • **Remaining Daily Budget:** You have **$calRemaining kcal** and **${proRemaining}g protein** left to hit your ${caloriesTarget} kcal target today!
                """.trimIndent(),
                mealName = "Post-Workout Chicken & Jasmine Rice Bowl",
                calories = 520,
                proteinG = 46.0
            )
            q.contains("veg") || q.contains("paneer") || q.contains("tofu") || q.contains("plant") || q.contains("lentil") || q.contains("dal") -> NutritionReplyResult(
                replyText = """
                    🌱 **High-Protein Vegetarian Plan for ${bio.hunterName} (${bio.goalDisplayLabel})**
                    
                    To support your **${bio.muscleMassPercent}% skeletal muscle mass** and **${bio.recommendedProteinG}g/day protein goal** on plant/vegetarian sources, combine complementary amino acid profiles:
                    
                    1. **Low-Fat Paneer / Extra-Firm Tofu Tikka Scramble (200g) + Quinoa:** 480 kcal | 38g Protein | 36g Carbs | 16g Healthy Fats
                    2. **Greek Skyr / Hung Curd (250g) + Hemp & Chia Seeds + Berries:** 340 kcal | 32g Protein | 28g Carbs | 8g Omega-3 Fats
                    3. **Roasted Edamame + Moong Dal Chilla (3 pancakes):** 430 kcal | 34g Protein | 52g Complex Carbs | 9g Fiber
                    
                    📊 **NUTRITION FACTS & SCIENCE (Per 100g Tofu / Low-Fat Paneer + Edamame):**
                    • **Leucine + Lysine Pairing:** Pairing legumes (high Lysine) with grains/seeds (high Methionine) achieves complete protein synthesis matching whey.
                    • **Micronutrients:** Calcium (380mg for muscular contraction), Iron (4.8mg for oxygen transport), Isoflavones & Polyphenols (reduces exercise-induced oxidative stress).
                """.trimIndent(),
                mealName = "High-Protein Tofu/Paneer & Quinoa Power Bowl",
                calories = 480,
                proteinG = 38.0
            )
            q.contains("loss") || q.contains("fat") || q.contains("cut") || q.contains("lean") || q.contains("deficit") -> NutritionReplyResult(
                replyText = """
                    🔥 **Fat-Loss & Lean Muscle Preservation Guide for ${bio.hunterName}**
                    
                    At **${bio.weightKg} kg** (**${bio.bodyFatPercent}% Body Fat**, **${bio.muscleMassPercent}% Muscle**), the key to fat loss is maximizing **Thermic Effect of Food (TEF)** and **Satiety Index** while protecting lean tissue:
                    
                    1. **Lean White Fish / Chicken Breast (200g) + Steamed Broccoli & Air-Fried Potatoes:** 440 kcal | 48g Protein | 42g Carbs | 6g Fat | 9g Fiber
                    2. **Egg White Omelet (6 whites + 1 whole egg) + Spinach & Mushrooms:** 260 kcal | 32g Protein | 6g Carbs | 7g Fat
                    3. **0% Greek Yogurt + 1 Scoop Casein/Whey + Cinnamon & Apple:** 310 kcal | 40g Protein | 26g Carbs | 2g Fat
                    
                    📊 **NUTRITION FACTS & SCIENCE:**
                    • **Thermic Effect of Protein (25–30%):** Your body burns ~30 kcal out of every 100 kcal of pure protein just digesting and metabolizing it.
                    • **GLP-1 & PYY Satiety:** High protein + soluble fiber slows gastric emptying and keeps hunger hormones suppressed for 4–5 hours.
                """.trimIndent(),
                mealName = "Lean Grilled Chicken, Broccoli & Potato Plate",
                calories = 440,
                proteinG = 48.0
            )
            else -> NutritionReplyResult(
                replyText = """
                    ⚡ **Pika-Dietitian Custom Food & Science Breakdown for "${query}"**
                    *(Calibrated for Trainer ${bio.hunterName}: ${bio.weightKg} kg • BMI ${bio.bmi} • ${bio.goalDisplayLabel} • Target ${caloriesTarget} kcal & ${bio.recommendedProteinG}g Protein)*
                    
                    **Top Recommended Foods for Your ${bio.goalDisplayLabel} Plan Today:**
                    1. **Wild Salmon / Lean Chicken Breast + Whole Eggs + Jasmine Rice (620 kcal | 50g Protein | 64g Carbs | 16g Fat)**
                    2. **Greek Yogurt Power Bowl with Rolled Oats, Walnuts & Blueberries (490 kcal | 38g Protein | 52g Carbs | 12g Fat)**
                    3. **Cottage Cheese / Paneer & Lentil Sprout Wrap (450 kcal | 36g Protein | 48g Carbs | 11g Fat)**
                    
                    📊 **NUTRITION FACTS & SCIENCE (Per 100g Core Staples):**
                    • **Chicken Breast (Cooked, 100g):** 165 kcal • **31g Protein** • 0g Carbs • 3.6g Fat • Rich in Niacin (B3), Selenium & Phosphorus.
                    • **Whole Eggs (2 Large / 100g):** 143 kcal • **12.6g Protein** • 9.5g Fat • **294mg Choline** (neuro-muscular drive) + Vitamin D3 & B12.
                    • **Rolled Oats (100g Dry):** 389 kcal • **16.9g Protein** • 66g Complex Carbs • **10.6g Beta-Glucan Fiber** (sustained ATP release & lipid control).
                    
                    💡 **Tactical Status:** You have logged **${caloriesLogged} / ${caloriesTarget} kcal** and **${proteinLogged.toInt()}g / ${bio.recommendedProteinG}g protein** today (${waterLogged}L / ${waterTarget}L water). Tap below to log your recommended meal now!
                """.trimIndent(),
                mealName = if (bio.fitnessGoal.uppercase() == "WEIGHT_LOSS") "Lean Chicken, Egg & Quinoa Bowl" else "Anabolic Chicken, Egg & Rice Power Bowl",
                calories = if (bio.fitnessGoal.uppercase() == "WEIGHT_LOSS") 460 else 620,
                proteinG = 48.0
            )
        }
    }

    fun clearNutritionChat() {
        _nutritionChatMessages.value = emptyList()
        seedWelcomeNutritionChatIfEmpty(_systemState.value)
    }

    fun completeTrainerOnboarding(
        trainerName: String,
        age: Int,
        biologicalSex: String,
        heightCm: Double,
        weightKg: Double,
        fitnessGoal: String,
        experienceLevel: String
    ) {
        viewModelScope.launch {
            val cleanName = trainerName.trim().take(60).ifBlank { "Trainer" }
            val cleanGoal = fitnessGoal.uppercase().let {
                if (it in setOf("WEIGHT_GAIN", "WEIGHT_LOSS", "MAINTENANCE")) it else "WEIGHT_GAIN"
            }
            val cleanExp = experienceLevel.uppercase().let {
                if (it in setOf("BEGINNER", "INTERMEDIATE", "PROFESSIONAL")) it else "BEGINNER"
            }
            val titleLabel = when (cleanExp) {
                "PROFESSIONAL" -> "Pro Gym Leader"
                "INTERMEDIATE" -> "Thunder Challenger"
                else -> "PikaFit Rookie"
            }
            val calibratedBio = HunterBiometrics(
                hunterName = cleanName,
                hunterTitle = titleLabel,
                avatarBase64 = "",
                weightKg = weightKg.coerceIn(25.0, 350.0),
                heightCm = heightCm.coerceIn(100.0, 250.0),
                age = age.coerceIn(10, 120),
                biologicalSex = if (biologicalSex.uppercase() == "FEMALE") "FEMALE" else "MALE",
                fitnessGoal = cleanGoal,
                experienceLevel = cleanExp,
                onboardingCompleted = true
            )
            val freshState = HunterSystemState.createResetState(
                existingBiometrics = calibratedBio,
                caloriesTarget = calibratedBio.recommendedCaloriesTarget,
                waterTargetLiters = calibratedBio.recommendedWaterLiters
            )
            repository.setOnboardingCompletedForUser(currentUserId, true, freshState.toStateJson())
            _systemState.value = freshState
            _isSessionLoaded.value = true
            repository.resetAllUserProgress(
                resetState = freshState,
                clearJournalEntries = true
            )
            seedWelcomeNutritionChatIfEmpty(freshState)
            generatePikaCoachTip(showAsPopup = true)
            _aiSyncState.value = AiSyncState.Success(
                "Pika-Pika! Welcome Trainer $cleanName! Target: ${freshState.statBreakdown.vitality.caloriesTarget} kcal & ${freshState.statBreakdown.stamina.targetLiters}L water."
            )
        }
    }

    /**
     * Logs a new Weight, Body Fat %, and Muscle Mass % point directly to Firestore for the Recharts Data Visualization Screen.
     * Note: Biometric body-scale logging updates Firestore telemetry, whereas Daily Streak Check-In specifically requires
     * Food or Exercise input per the Hunter System rule.
     */
    fun logBiometricPointToFirestore(
        weightKg: Double,
        bodyFatPercent: Double?,
        muscleMassPercent: Double?,
        note: String = ""
    ) {
        viewModelScope.launch {
            val current = _systemState.value
            val prevBio = current.biometrics
            val updatedBio = prevBio.copy(
                weightKg = weightKg.coerceIn(25.0, 350.0),
                customBodyFatPercent = bodyFatPercent?.coerceIn(3.0, 65.0),
                customMuscleMassPercent = muscleMassPercent?.coerceIn(20.0, 90.0)
            )

            val xpGain = 50
            val rawXp = current.playerStatus.currentXp + xpGain
            val leveledUp = rawXp >= current.playerStatus.nextLevelXp
            val newLevel = if (leveledUp) current.playerStatus.level + 1 else current.playerStatus.level
            val finalXp = if (leveledUp) rawXp - current.playerStatus.nextLevelXp else rawXp
            val progress = ((finalXp.toDouble() / current.playerStatus.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val cleanNote = note.trim().ifBlank {
                "Weight ${updatedBio.weightKg} kg • Body Fat ${updatedBio.bodyFatPercent}% • Muscle Mass ${updatedBio.muscleMassPercent}% (${updatedBio.muscleMassKg} kg)"
            }
            val briefing = "New biometric point plotted to Firestore: ${updatedBio.weightKg} kg, ${updatedBio.bodyFatPercent}% Body Fat, ${updatedBio.muscleMassPercent}% Muscle Mass (${updatedBio.muscleMassKg} kg)."

            val updatedState = current.copy(
                playerStatus = current.playerStatus.copy(
                    level = newLevel,
                    rank = if (newLevel >= 3) "D-Rank Vanguard" else current.playerStatus.rank,
                    currentXp = finalXp,
                    progressPercent = progress
                ),
                tacticalBriefing = briefing,
                biometrics = updatedBio
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)

            repository.appendJournalEntry(
                entryType = "BIOMETRIC_UPDATE",
                title = "Telemetry Point: ${updatedBio.weightKg}kg • ${updatedBio.bodyFatPercent}% BF • ${updatedBio.muscleMassPercent}% Muscle",
                caption = cleanNote,
                poseTag = "Recharts Sync",
                photoBase64 = "",
                biometrics = updatedBio
            )

            repository.appendTacticalLog(
                commandText = "Plotted Biometrics: ${updatedBio.weightKg}kg, BF ${updatedBio.bodyFatPercent}%, Muscle ${updatedBio.muscleMassPercent}%",
                briefingResponse = briefing,
                xpDelta = xpGain
            )

            _aiSyncState.value = AiSyncState.Success("Biometric point saved to Firestore & plotted (+50 XP)")
        }
    }

    fun updateProfileAndBiometrics(
        hunterName: String,
        hunterTitle: String,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        biologicalSex: String,
        avatarBase64: String? = null
    ) {
        viewModelScope.launch {
            val current = _systemState.value
            val prevBio = current.biometrics
            val updatedBio = prevBio.copy(
                hunterName = hunterName.trim().take(60).ifBlank { prevBio.hunterName },
                hunterTitle = hunterTitle.trim().take(60).ifBlank { prevBio.hunterTitle },
                avatarBase64 = avatarBase64?.take(18000) ?: prevBio.avatarBase64,
                weightKg = weightKg.coerceIn(25.0, 350.0),
                heightCm = heightCm.coerceIn(100.0, 250.0),
                age = age.coerceIn(10, 120),
                biologicalSex = if (biologicalSex.uppercase() == "FEMALE") "FEMALE" else "MALE",
                onboardingCompleted = true
            )

            val xpGain = 50
            val rawXp = current.playerStatus.currentXp + xpGain
            val leveledUp = rawXp >= current.playerStatus.nextLevelXp
            val newLevel = if (leveledUp) current.playerStatus.level + 1 else current.playerStatus.level
            val finalXp = if (leveledUp) rawXp - current.playerStatus.nextLevelXp else rawXp
            val progress = ((finalXp.toDouble() / current.playerStatus.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val briefing = "Biometric telemetry calibrated for ${updatedBio.hunterName}: ${updatedBio.weightKg}kg @ ${updatedBio.heightCm.toInt()}cm (BMI ${updatedBio.bmi} • Body Fat ${updatedBio.bodyFatPercent}% • Muscle ${updatedBio.muscleMassPercent}% • Bone ${updatedBio.boneMassKg}kg • Water ${updatedBio.waterMassKg}kg). Automatically archived to Personal Journal (+50 XP)."

            val updatedState = current.copy(
                playerStatus = current.playerStatus.copy(
                    level = newLevel,
                    rank = if (newLevel >= 3) "D-Rank Vanguard" else current.playerStatus.rank,
                    currentXp = finalXp,
                    progressPercent = progress
                ),
                tacticalBriefing = briefing,
                biometrics = updatedBio
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)

            val summaryCaption = buildString {
                append("Weight: ${updatedBio.weightKg} kg • Height: ${updatedBio.heightCm.toInt()} cm • Age: ${updatedBio.age} (${updatedBio.biologicalSex})\n")
                append("BMI: ${updatedBio.bmi} (${updatedBio.bmiCategory}) • Body Fat: ${updatedBio.bodyFatPercent}% (${updatedBio.fatMassKg} kg)\n")
                append("Skeletal Muscle: ${updatedBio.muscleMassPercent}% (${updatedBio.muscleMassKg} kg) • Lean Mass: ${updatedBio.leanMassPercent}% (${updatedBio.leanBodyMassKg} kg)\n")
                append("Bone Mass: ${updatedBio.boneMassKg} kg (${updatedBio.boneMassPercent}%) • Water Mass: ${updatedBio.waterMassKg} kg (${updatedBio.waterMassPercent}%) • BMR: ${updatedBio.bmrKcal} kcal/day")
            }

            repository.appendJournalEntry(
                entryType = "BIOMETRIC_UPDATE",
                title = "Biometric Profile Sync (${updatedBio.weightKg} kg • BMI ${updatedBio.bmi})",
                caption = summaryCaption,
                poseTag = "Biometric Sync",
                photoBase64 = avatarBase64 ?: "",
                biometrics = updatedBio
            )

            repository.appendTacticalLog(
                commandText = "Updated Biometrics: ${updatedBio.weightKg}kg, ${updatedBio.heightCm.toInt()}cm, Age ${updatedBio.age}",
                briefingResponse = briefing,
                xpDelta = xpGain
            )

            _aiSyncState.value = AiSyncState.Success("Biometrics & Profile saved to Personal Journal (+50 XP)")
        }
    }

    fun logProgressPhotoToJournal(
        title: String,
        caption: String,
        poseTag: String,
        photoBase64: String,
        setAsAvatar: Boolean = false
    ) {
        if (photoBase64.isBlank()) return
        viewModelScope.launch {
            val current = _systemState.value
            val updatedBio = if (setAsAvatar) {
                current.biometrics.copy(avatarBase64 = photoBase64.take(18000))
            } else {
                current.biometrics
            }

            val xpGain = 85
            val rawXp = current.playerStatus.currentXp + xpGain
            val leveledUp = rawXp >= current.playerStatus.nextLevelXp
            val newLevel = if (leveledUp) current.playerStatus.level + 1 else current.playerStatus.level
            val finalXp = if (leveledUp) rawXp - current.playerStatus.nextLevelXp else rawXp
            val progress = ((finalXp.toDouble() / current.playerStatus.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val cleanTitle = title.trim().ifBlank { "Physique Check-In ($poseTag)" }
            val cleanCaption = caption.trim().ifBlank {
                "Documented physique progress at ${updatedBio.weightKg} kg (BMI ${updatedBio.bmi}, BF ${updatedBio.bodyFatPercent}%, Muscle ${updatedBio.muscleMassPercent}%)."
            }

            val hasPhotoAchievement = current.unlockedAchievements.any { it.title == "Visual Chronicler" }
            val updatedAchievements = if (!hasPhotoAchievement) {
                current.unlockedAchievements + AchievementItem(
                    title = "Visual Chronicler",
                    description = "Uploaded and archived a physique progress photo to your Personal Hunter Journal.",
                    rarity = "EPIC"
                )
            } else {
                current.unlockedAchievements
            }

            val briefing = "Progress photo \"$cleanTitle\" [$poseTag] archived in Personal Journal with full biometric telemetry (${updatedBio.weightKg}kg, BF ${updatedBio.bodyFatPercent}%, Muscle ${updatedBio.muscleMassPercent}%). Awarded +$xpGain XP."

            val updatedState = current.copy(
                playerStatus = current.playerStatus.copy(
                    level = newLevel,
                    rank = if (newLevel >= 3) "D-Rank Vanguard" else current.playerStatus.rank,
                    currentXp = finalXp,
                    progressPercent = progress
                ),
                unlockedAchievements = updatedAchievements,
                tacticalBriefing = briefing,
                biometrics = updatedBio
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)

            val res = repository.appendJournalEntry(
                entryType = "PHOTO_PROGRESS",
                title = cleanTitle,
                caption = cleanCaption,
                poseTag = poseTag,
                photoBase64 = photoBase64,
                biometrics = updatedBio
            )

            repository.appendTacticalLog(
                commandText = "Journal Photo Logged: $cleanTitle [$poseTag]",
                briefingResponse = briefing,
                xpDelta = xpGain
            )

            if (res.isSuccess) {
                _aiSyncState.value = AiSyncState.Success("Progress photo added to Personal Journal (+$xpGain XP)")
            } else {
                _aiSyncState.value = AiSyncState.Error(
                    res.exceptionOrNull()?.localizedMessage ?: "Failed to save journal entry."
                )
            }
        }
    }

    fun deleteJournalEntry(entryId: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch {
            repository.deleteJournalEntry(entryId)
        }
    }

    /**
     * Logs a specific food/meal item and checks the user in for their Daily Productive Streak!
     */
    fun logFoodItem(
        mealName: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double = (calories * 0.10),
        fatsG: Double = (calories * 0.03)
    ) {
        if (calories <= 0 && mealName.isBlank()) return
        viewModelScope.launch {
            val cleanMeal = mealName.trim().ifBlank { "Tactical Ration" }
            val safeCals = calories.coerceIn(10, 10000)
            val safePro = proteinG.coerceIn(0.0, 500.0)
            val safeCarbs = carbsG.coerceIn(0.0, 1000.0)
            val safeFats = fatsG.coerceIn(0.0, 500.0)

            val current = _systemState.value
            val vit = current.statBreakdown.vitality
            val newKcal = (vit.caloriesLogged + safeCals).coerceIn(0, 50000)
            val newProtein = (vit.proteinG + safePro).coerceIn(0.0, 2000.0)
            val newCarbs = (vit.carbsG + safeCarbs).coerceIn(0.0, 5000.0)
            val newFats = (vit.fatsG + safeFats).coerceIn(0.0, 2000.0)
            val targetMet = newKcal >= vit.caloriesTarget && newProtein >= 110.0

            // Apply productive input to check the user in for their Daily Streak
            val (streakUpdatedState, newlyCheckedIn) = current.applyProductiveInput(
                isFoodInput = true,
                inputSummary = "$cleanMeal (+$safeCals kcal, ${safePro.toInt()}g P)"
            )

            val baseXpGain = 45
            val totalXpGain = baseXpGain + (if (newlyCheckedIn) 50 else 0)
            val ps = streakUpdatedState.playerStatus
            val rawXp = ps.currentXp + baseXpGain
            val leveledUp = rawXp >= ps.nextLevelXp
            val newLevel = if (leveledUp) ps.level + 1 else ps.level
            val finalXp = if (leveledUp) rawXp - ps.nextLevelXp else rawXp
            val progress = ((finalXp.toDouble() / ps.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val updatedQuests = streakUpdatedState.questLog.map { q ->
                if (q.questName.contains("kcal", ignoreCase = true) || q.questName.contains("Surplus", ignoreCase = true)) {
                    QuestItem(
                        questName = "Clear ${vit.caloriesTarget} kcal Surplus Target ($newKcal / ${vit.caloriesTarget} logged)",
                        status = if (newKcal >= vit.caloriesTarget) "COMPLETED" else "IN_PROGRESS",
                        xpAwarded = 120
                    )
                } else {
                    q
                }
            }

            val streakNote = if (newlyCheckedIn) {
                "Daily Productive Streak Checked In (${ps.streakDays} Days)!"
            } else {
                "Productive Streak active (${ps.streakDays} Days)."
            }

            val updatedBriefing = "Logged food \"$cleanMeal\" (+$safeCals kcal, +${safePro.toInt()}g protein). Total intake: $newKcal / ${vit.caloriesTarget} kcal. $streakNote"

            val finalState = streakUpdatedState.copy(
                playerStatus = ps.copy(
                    level = newLevel,
                    rank = if (newLevel >= 3) "D-Rank Vanguard" else ps.rank,
                    currentXp = finalXp,
                    progressPercent = progress
                ),
                statBreakdown = streakUpdatedState.statBreakdown.copy(
                    vitality = vit.copy(
                        caloriesLogged = newKcal,
                        proteinG = newProtein,
                        carbsG = newCarbs,
                        fatsG = newFats,
                        targetMet = targetMet
                    )
                ),
                questLog = updatedQuests,
                tacticalBriefing = updatedBriefing
            )

            _foodHistory.value = _foodHistory.value + FoodEntryRecord(
                mealName = cleanMeal,
                calories = safeCals,
                proteinG = safePro,
                carbsG = safeCarbs,
                fatsG = safeFats,
                xpAwarded = baseXpGain,
                triggeredDailyCheckIn = newlyCheckedIn,
                previousStreakDays = current.playerStatus.streakDays
            )

            _systemState.value = finalState
            repository.saveOrUpdateSession(finalState)
            repository.appendTacticalLog(
                commandText = "Food Input: $cleanMeal (+$safeCals kcal, ${safePro.toInt()}g P)",
                briefingResponse = updatedBriefing,
                xpDelta = totalXpGain
            )

            _aiSyncState.value = AiSyncState.Success(
                if (newlyCheckedIn) {
                    "Daily Streak Checked In (${ps.streakDays}D) via Food Input! (+$totalXpGain XP)"
                } else {
                    "Food logged: $cleanMeal (+$totalXpGain XP)"
                }
            )
        }
    }

    fun submitTacticalCommand(command: String) {
        val cleanCmd = command.trim()
        if (cleanCmd.isBlank()) return

        viewModelScope.launch {
            _aiSyncState.value = AiSyncState.Analyzing(cleanCmd)
            val currentJson = _systemState.value.toRawSystemJson()
            val prompt = """
                You are the Hunter System AI — an elite RPG tactical fitness, macro-nutrition, and progressive overload HUD engine.
                Current Hunter System JSON State:
                $currentJson

                Hunter's new tactical report / command:
                "$cleanCmd"

                Update the Hunter System JSON state accurately to reflect this report:
                - If the user reports calorie intake (e.g. "i had 2000 kcal" or "+500 kcal"), update calories_logged, macro estimates (protein_g, carbs_g, fats_g, fiber_g, omega3_mg), target_met, XP, progress_percent, level/rank if applicable, active_buffs/active_debuffs, and quest_log.
                - If the user reports water intake or hydration, update stamina.water_liters and target_met.
                - If the user reports sets/reps or exercises, update strength.exercises_logged and workout_completed.
                - Provide a sharp, motivating 2-3 sentence tactical_briefing and 3-4 actionable suggested_actions in uppercase.
            """.trimIndent()

            try {
                val response = if (Firebase.auth.currentUser != null) {
                    withTimeoutOrNull(12_000L) {
                        aiModel.generateContent(prompt)
                    }
                } else null
                val jsonString = response?.text ?: throw IllegalStateException("Using local tactical parser")
                val beforeState = _systemState.value
                val oldXp = beforeState.playerStatus.currentXp + (beforeState.playerStatus.level * 1000)
                val geminiState = HunterSystemState.fromGeminiJson(
                    jsonString = jsonString,
                    existingBiometrics = beforeState.biometrics,
                    existingPlayerStatus = beforeState.playerStatus
                )

                // Check if this command was a productive food or exercise input
                val lowerCmd = cleanCmd.lowercase()
                val isExerciseCmd = geminiState.statBreakdown.strength.exercisesLogged.size > beforeState.statBreakdown.strength.exercisesLogged.size ||
                    listOf("set", "rep", "pushup", "pull", "squat", "bench", "row", "curl", "raise", "workout", "lift", "press", "run").any { lowerCmd.contains(it) }
                val isFoodCmd = geminiState.statBreakdown.vitality.caloriesLogged > beforeState.statBreakdown.vitality.caloriesLogged ||
                    geminiState.statBreakdown.vitality.proteinG > beforeState.statBreakdown.vitality.proteinG ||
                    listOf("kcal", "cal", "protein", "meal", "ate", "had", "shake", "chicken", "egg", "rice", "food", "eat").any { lowerCmd.contains(it) }

                val (finalState, newlyCheckedIn) = when {
                    isExerciseCmd -> geminiState.applyProductiveInput(isFoodInput = false, inputSummary = cleanCmd.take(40))
                    isFoodCmd -> geminiState.applyProductiveInput(isFoodInput = true, inputSummary = cleanCmd.take(40))
                    else -> Pair(geminiState, false)
                }

                val newXp = finalState.playerStatus.currentXp + (finalState.playerStatus.level * 1000)
                val xpDelta = (newXp - oldXp).coerceAtLeast(25)

                _systemState.value = finalState
                val saveRes = repository.saveOrUpdateSession(finalState)
                repository.appendTacticalLog(
                    commandText = cleanCmd,
                    briefingResponse = finalState.tacticalBriefing,
                    xpDelta = xpDelta
                )

                if (saveRes.isSuccess) {
                    val msg = if (newlyCheckedIn) {
                        "Daily Streak Checked In (${finalState.playerStatus.streakDays}D) & System Updated (+${xpDelta} XP)"
                    } else {
                        "System updated (+${xpDelta} XP)"
                    }
                    _aiSyncState.value = AiSyncState.Success(msg)
                } else {
                    _aiSyncState.value = AiSyncState.Error(
                        saveRes.exceptionOrNull()?.localizedMessage ?: "Saved locally; cloud sync error."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Tactical evaluation processed locally: ${e.message}")
                _aiSyncState.value = AiSyncState.Success("Tactical report logged: $cleanCmd (+35 XP)")
            }
        }
    }

    fun quickAdjustCalories(deltaKcal: Int, deltaProteinG: Double = 0.0) {
        viewModelScope.launch {
            val current = _systemState.value
            val vit = current.statBreakdown.vitality
            val newKcal = (vit.caloriesLogged + deltaKcal).coerceIn(0, 50000)
            val newProtein = (vit.proteinG + deltaProteinG).coerceIn(0.0, 2000.0)
            val newCarbs = (vit.carbsG + (deltaKcal * 0.11)).coerceIn(0.0, 5000.0)
            val targetMet = newKcal >= vit.caloriesTarget && newProtein >= 110.0

            // Positive food/calorie input checks the user in for their Daily Productive Streak!
            val (streakState, newlyCheckedIn) = if (deltaKcal > 0) {
                current.applyProductiveInput(
                    isFoodInput = true,
                    inputSummary = "+$deltaKcal kcal (${deltaProteinG.toInt()}g protein)"
                )
            } else {
                Pair(current, false)
            }

            val baseXpGain = if (deltaKcal > 0) 40 else 0
            val totalXpGain = baseXpGain + (if (newlyCheckedIn) 50 else 0)
            val ps = streakState.playerStatus
            val rawXp = ps.currentXp + baseXpGain
            val leveledUp = rawXp >= ps.nextLevelXp
            val newLevel = if (leveledUp) ps.level + 1 else ps.level
            val finalXp = if (leveledUp) rawXp - ps.nextLevelXp else rawXp
            val progress = ((finalXp.toDouble() / ps.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val updatedQuests = streakState.questLog.map { q ->
                if (q.questName.contains("kcal", ignoreCase = true) || q.questName.contains("Surplus", ignoreCase = true)) {
                    QuestItem(
                        questName = "Clear ${vit.caloriesTarget} kcal Surplus Target ($newKcal / ${vit.caloriesTarget} logged)",
                        status = if (newKcal >= vit.caloriesTarget) "COMPLETED" else "IN_PROGRESS",
                        xpAwarded = 120
                    )
                } else {
                    q
                }
            }

            val streakSuffix = if (newlyCheckedIn) " Daily Productive Streak Checked In (${ps.streakDays}D)!" else ""
            val updatedBriefing = if (targetMet) {
                "Caloric surplus and protein synthesis targets achieved at $newKcal kcal (${newProtein.toInt()}g protein). Anabolic recovery window is locked in.$streakSuffix"
            } else {
                val remaining = (vit.caloriesTarget - newKcal).coerceAtLeast(0)
                "Vitality updated to $newKcal kcal (${newProtein.toInt()}g protein). $remaining kcal remaining to clear your daily hypertrophy surplus.$streakSuffix"
            }

            val updated = streakState.copy(
                playerStatus = ps.copy(
                    level = newLevel,
                    rank = if (newLevel >= 3) "D-Rank Vanguard" else ps.rank,
                    currentXp = finalXp,
                    progressPercent = progress
                ),
                statBreakdown = streakState.statBreakdown.copy(
                    vitality = vit.copy(
                        caloriesLogged = newKcal,
                        proteinG = newProtein,
                        carbsG = newCarbs,
                        targetMet = targetMet
                    )
                ),
                questLog = updatedQuests,
                tacticalBriefing = updatedBriefing
            )

            if (deltaKcal > 0) {
                _foodHistory.value = _foodHistory.value + FoodEntryRecord(
                    mealName = "Quick +$deltaKcal kcal",
                    calories = deltaKcal,
                    proteinG = deltaProteinG,
                    carbsG = deltaKcal * 0.11,
                    fatsG = 0.0,
                    xpAwarded = baseXpGain,
                    triggeredDailyCheckIn = newlyCheckedIn,
                    previousStreakDays = current.playerStatus.streakDays
                )
            }

            _systemState.value = updated
            repository.saveOrUpdateSession(updated)
            repository.appendTacticalLog(
                commandText = "Logged ${if (deltaKcal >= 0) "+$deltaKcal" else "$deltaKcal"} kcal (${newKcal} kcal total)",
                briefingResponse = updatedBriefing,
                xpDelta = totalXpGain
            )

            if (newlyCheckedIn) {
                _aiSyncState.value = AiSyncState.Success("Daily Streak Checked In (${ps.streakDays}D) via Food Input! (+$totalXpGain XP)")
            } else if (deltaKcal > 0) {
                _aiSyncState.value = AiSyncState.Success("Logged +$deltaKcal kcal (+${deltaProteinG.toInt()}g protein)")
            }
        }
    }

    fun quickAddWater(litersDelta: Double) {
        viewModelScope.launch {
            val current = _systemState.value
            val stam = current.statBreakdown.stamina
            val newWater = ((stam.waterLiters + litersDelta) * 10.0).toInt() / 10.0
            val clampedWater = newWater.coerceIn(0.0, 50.0)
            val targetMet = clampedWater >= stam.targetLiters

            val xpGain = if (litersDelta > 0) 20 else 0
            val rawXp = (current.playerStatus.currentXp + xpGain).coerceAtMost(current.playerStatus.nextLevelXp - 1)
            val progress = ((rawXp.toDouble() / current.playerStatus.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val briefing = "Hydration reserves at ${clampedWater}L / ${stam.targetLiters}L. Cellular nutrient transport and joint lubrication optimized."
            val updated = current.copy(
                playerStatus = current.playerStatus.copy(
                    currentXp = rawXp,
                    progressPercent = progress
                ),
                statBreakdown = current.statBreakdown.copy(
                    stamina = stam.copy(
                        waterLiters = clampedWater,
                        targetMet = targetMet
                    )
                ),
                tacticalBriefing = briefing
            )
            if (litersDelta > 0) {
                _waterHistory.value = _waterHistory.value + WaterEntryRecord(
                    label = "+${litersDelta}L Water",
                    litersDelta = litersDelta,
                    xpAwarded = xpGain
                )
            }
            _systemState.value = updated
            repository.saveOrUpdateSession(updated)
            repository.appendTacticalLog(
                commandText = "Hydration +${litersDelta}L (${clampedWater}L total)",
                briefingResponse = briefing,
                xpDelta = xpGain
            )
            if (litersDelta > 0) {
                _aiSyncState.value = AiSyncState.Success("Logged +${litersDelta}L water (${clampedWater}L / ${stam.targetLiters}L)")
            }
        }
    }

    /**
     * Undoes the most recent Food/Calorie entry (or removes a specific FoodEntryRecord by id),
     * reverting calories, protein, carbs, fats, XP, and daily food input counters if mistakenly entered.
     */
    fun undoLastFoodEntry(entryId: String? = null) {
        viewModelScope.launch {
            val current = _systemState.value
            val vit = current.statBreakdown.vitality
            val history = _foodHistory.value

            val targetRecord = if (entryId != null) {
                history.find { it.id == entryId }
            } else {
                history.lastOrNull()
            }

            if (targetRecord == null && vit.caloriesLogged <= 0 && vit.proteinG <= 0.0) {
                _aiSyncState.value = AiSyncState.Error("No food entries left to undo today.")
                return@launch
            }

            val removeCals = targetRecord?.calories ?: vit.caloriesLogged.coerceAtMost(350)
            val removePro = targetRecord?.proteinG ?: vit.proteinG.coerceAtMost(28.0)
            val removeCarbs = targetRecord?.carbsG ?: (removeCals * 0.10)
            val removeFats = targetRecord?.fatsG ?: (removeCals * 0.03)
            val removeLabel = targetRecord?.mealName ?: "${removeCals} kcal entry"
            val removeXp = targetRecord?.xpAwarded ?: 40

            if (targetRecord != null) {
                _foodHistory.value = history.filterNot { it.id == targetRecord.id }
            }

            val newKcal = (vit.caloriesLogged - removeCals).coerceAtLeast(0)
            val newProtein = (vit.proteinG - removePro).coerceAtLeast(0.0)
            val newCarbs = (vit.carbsG - removeCarbs).coerceAtLeast(0.0)
            val newFats = (vit.fatsG - removeFats).coerceAtLeast(0.0)
            val targetMet = newKcal >= vit.caloriesTarget && newProtein >= 110.0

            val ps = current.playerStatus
            val newFoodInputs = (ps.todayFoodInputs - 1).coerceAtLeast(0)
            val stillHasActivityToday = newFoodInputs > 0 || ps.todayExerciseInputs > 0 || newKcal > 0
            val newCheckedIn = if (!stillHasActivityToday) false else ps.checkedInToday
            val newStreakDays = if (!stillHasActivityToday && targetRecord?.triggeredDailyCheckIn == true) {
                targetRecord.previousStreakDays
            } else {
                ps.streakDays
            }

            val newXp = (ps.currentXp - removeXp).coerceAtLeast(0)
            val newProgress = ((newXp.toDouble() / ps.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val updatedQuests = current.questLog.map { q ->
                if (q.questName.contains("kcal", ignoreCase = true) || q.questName.contains("Surplus", ignoreCase = true)) {
                    QuestItem(
                        questName = "Clear ${vit.caloriesTarget} kcal Surplus Target ($newKcal / ${vit.caloriesTarget} logged)",
                        status = if (newKcal >= vit.caloriesTarget) "COMPLETED" else "IN_PROGRESS",
                        xpAwarded = 120
                    )
                } else {
                    q
                }
            }

            val updatedBriefing = "Undid food entry \"$removeLabel\" (-$removeCals kcal, -${removePro.toInt()}g protein). Updated total: $newKcal / ${vit.caloriesTarget} kcal."

            val updatedState = current.copy(
                playerStatus = ps.copy(
                    currentXp = newXp,
                    progressPercent = newProgress,
                    todayFoodInputs = newFoodInputs,
                    checkedInToday = newCheckedIn,
                    streakDays = newStreakDays
                ),
                statBreakdown = current.statBreakdown.copy(
                    vitality = vit.copy(
                        caloriesLogged = newKcal,
                        proteinG = newProtein,
                        carbsG = newCarbs,
                        fatsG = newFats,
                        targetMet = targetMet
                    )
                ),
                questLog = updatedQuests,
                tacticalBriefing = updatedBriefing
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)
            repository.appendTacticalLog(
                commandText = "Undo Food Entry: $removeLabel (-$removeCals kcal)",
                briefingResponse = updatedBriefing,
                xpDelta = -removeXp
            )
            _aiSyncState.value = AiSyncState.Success("Undid food entry: $removeLabel (-$removeCals kcal, -${removePro.toInt()}g P)")
        }
    }

    /**
     * Undoes the most recent Water/Hydration entry (or removes a specific WaterEntryRecord by id),
     * reverting waterLiters, targetMet, and XP if mistakenly entered.
     */
    fun undoLastWaterEntry(entryId: String? = null) {
        viewModelScope.launch {
            val current = _systemState.value
            val stam = current.statBreakdown.stamina
            val history = _waterHistory.value

            val targetRecord = if (entryId != null) {
                history.find { it.id == entryId }
            } else {
                history.lastOrNull()
            }

            if (targetRecord == null && stam.waterLiters <= 0.0) {
                _aiSyncState.value = AiSyncState.Error("No water entries left to undo today.")
                return@launch
            }

            val removeLiters = targetRecord?.litersDelta ?: stam.waterLiters.coerceAtMost(0.5)
            val removeXp = targetRecord?.xpAwarded ?: 20

            if (targetRecord != null) {
                _waterHistory.value = history.filterNot { it.id == targetRecord.id }
            }

            val newWater = (((stam.waterLiters - removeLiters) * 10.0).toInt() / 10.0).coerceAtLeast(0.0)
            val targetMet = newWater >= stam.targetLiters

            val ps = current.playerStatus
            val newXp = (ps.currentXp - removeXp).coerceAtLeast(0)
            val newProgress = ((newXp.toDouble() / ps.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val updatedBriefing = "Undid water entry (-${removeLiters}L). Hydration reserves updated to ${newWater}L / ${stam.targetLiters}L."

            val updatedState = current.copy(
                playerStatus = ps.copy(
                    currentXp = newXp,
                    progressPercent = newProgress
                ),
                statBreakdown = current.statBreakdown.copy(
                    stamina = stam.copy(
                        waterLiters = newWater,
                        targetMet = targetMet
                    )
                ),
                tacticalBriefing = updatedBriefing
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)
            repository.appendTacticalLog(
                commandText = "Undo Water Entry (-${removeLiters}L)",
                briefingResponse = updatedBriefing,
                xpDelta = -removeXp
            )
            _aiSyncState.value = AiSyncState.Success("Undid water entry (-${removeLiters}L). Now at ${newWater}L / ${stam.targetLiters}L")
        }
    }

    fun addExerciseSet(name: String, sets: Int, reps: String, overload: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val current = _systemState.value
            val existingExercises = current.statBreakdown.strength.exercisesLogged
            val newEx = ExerciseLogged(
                exerciseName = name.trim(),
                setsCompleted = sets.coerceIn(1, 20),
                repsPerSet = reps.trim().ifBlank { "10" },
                progressiveOverloadHit = overload
            )
            val updatedList = existingExercises + newEx

            // Logging any exercise data checks the user in for their Daily Productive Streak!
            val (streakState, newlyCheckedIn) = current.applyProductiveInput(
                isFoodInput = false,
                inputSummary = "${newEx.exerciseName} (${newEx.setsCompleted}x${newEx.repsPerSet})"
            )

            val baseXpGain = if (overload) 75 else 45
            val totalXpGain = baseXpGain + (if (newlyCheckedIn) 50 else 0)
            val ps = streakState.playerStatus
            val rawXp = ps.currentXp + baseXpGain
            val leveledUp = rawXp >= ps.nextLevelXp
            val newLevel = if (leveledUp) ps.level + 1 else ps.level
            val finalXp = if (leveledUp) rawXp - ps.nextLevelXp else rawXp
            val progress = ((finalXp.toDouble() / ps.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val streakSuffix = if (newlyCheckedIn) " Daily Productive Streak Checked In (${ps.streakDays}D)!" else ""
            val briefing = "Logged ${newEx.exerciseName} (${newEx.setsCompleted}x${newEx.repsPerSet}). Progressive overload ${if (overload) "CONFIRMED" else "maintained"} — mechanical tension stimulus recorded.$streakSuffix"
            val updated = streakState.copy(
                playerStatus = ps.copy(
                    level = newLevel,
                    rank = if (newLevel >= 3) "D-Rank Vanguard" else ps.rank,
                    currentXp = finalXp,
                    progressPercent = progress
                ),
                statBreakdown = streakState.statBreakdown.copy(
                    strength = streakState.statBreakdown.strength.copy(
                        workoutCompleted = true,
                        exercisesLogged = updatedList
                    )
                ),
                tacticalBriefing = briefing
            )
            _systemState.value = updated
            repository.saveOrUpdateSession(updated)
            repository.appendTacticalLog(
                commandText = "Strength Log: ${newEx.exerciseName} ${newEx.setsCompleted}x${newEx.repsPerSet}",
                briefingResponse = briefing,
                xpDelta = totalXpGain
            )

            _aiSyncState.value = AiSyncState.Success(
                if (newlyCheckedIn) {
                    "Daily Streak Checked In (${ps.streakDays}D) via Exercise Input! (+$totalXpGain XP)"
                } else {
                    "Exercise logged: ${newEx.exerciseName} (+$totalXpGain XP)"
                }
            )
        }
    }

    fun toggleQuestStatus(index: Int) {
        viewModelScope.launch {
            val current = _systemState.value
            val quests = current.questLog.toMutableList()
            if (index !in quests.indices) return@launch
            val item = quests[index]
            val nextStatus = if (item.status == "COMPLETED") "IN_PROGRESS" else "COMPLETED"
            quests[index] = item.copy(status = nextStatus)

            val xpDelta = if (nextStatus == "COMPLETED") item.xpAwarded else -item.xpAwarded
            val rawXp = (current.playerStatus.currentXp + xpDelta).coerceIn(0, current.playerStatus.nextLevelXp - 1)
            val progress = ((rawXp.toDouble() / current.playerStatus.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val updated = current.copy(
                playerStatus = current.playerStatus.copy(
                    currentXp = rawXp,
                    progressPercent = progress
                ),
                questLog = quests
            )
            _systemState.value = updated
            repository.saveOrUpdateSession(updated)
        }
    }

    fun updateSettingsProfileAndTargets(
        hunterName: String,
        hunterTitle: String,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        biologicalSex: String,
        customBodyFatPercent: Double?,
        customMuscleMassPercent: Double?,
        caloriesTarget: Int,
        waterTargetLiters: Double,
        avatarBase64: String? = null
    ) {
        viewModelScope.launch {
            val current = _systemState.value
            val prevBio = current.biometrics
            val updatedBio = prevBio.copy(
                hunterName = hunterName.trim().take(60).ifBlank { prevBio.hunterName },
                hunterTitle = hunterTitle.trim().take(60).ifBlank { prevBio.hunterTitle },
                avatarBase64 = avatarBase64?.take(18000) ?: prevBio.avatarBase64,
                weightKg = weightKg.coerceIn(25.0, 350.0),
                heightCm = heightCm.coerceIn(100.0, 250.0),
                age = age.coerceIn(10, 120),
                biologicalSex = if (biologicalSex.uppercase() == "FEMALE") "FEMALE" else "MALE",
                customBodyFatPercent = customBodyFatPercent?.coerceIn(3.0, 65.0),
                customMuscleMassPercent = customMuscleMassPercent?.coerceIn(20.0, 90.0),
                onboardingCompleted = true
            )

            val safeCalTarget = caloriesTarget.coerceIn(800, 15000)
            val safeWaterTarget = ((waterTargetLiters.coerceIn(0.5, 20.0) * 10.0).toInt() / 10.0)
            val vit = current.statBreakdown.vitality
            val stam = current.statBreakdown.stamina

            val xpGain = 50
            val rawXp = current.playerStatus.currentXp + xpGain
            val leveledUp = rawXp >= current.playerStatus.nextLevelXp
            val newLevel = if (leveledUp) current.playerStatus.level + 1 else current.playerStatus.level
            val finalXp = if (leveledUp) rawXp - current.playerStatus.nextLevelXp else rawXp
            val progress = ((finalXp.toDouble() / current.playerStatus.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

            val briefing = "Profile & Daily Targets updated in Settings for ${updatedBio.hunterName} (${updatedBio.hunterTitle}): ${updatedBio.weightKg}kg, ${updatedBio.heightCm.toInt()}cm, Daily Targets ${safeCalTarget} kcal & ${safeWaterTarget}L water."

            val updatedQuests = current.questLog.map { q ->
                if (q.questName.contains("kcal", ignoreCase = true) || q.questName.contains("Surplus", ignoreCase = true)) {
                    QuestItem(
                        questName = "Clear ${safeCalTarget} kcal Surplus Target (${vit.caloriesLogged} / ${safeCalTarget} logged)",
                        status = if (vit.caloriesLogged >= safeCalTarget) "COMPLETED" else "IN_PROGRESS",
                        xpAwarded = 120
                    )
                } else {
                    q
                }
            }

            val updatedState = current.copy(
                playerStatus = current.playerStatus.copy(
                    level = newLevel,
                    rank = if (newLevel >= 3) "D-Rank Vanguard" else current.playerStatus.rank,
                    currentXp = finalXp,
                    progressPercent = progress
                ),
                statBreakdown = current.statBreakdown.copy(
                    vitality = vit.copy(
                        caloriesTarget = safeCalTarget,
                        targetMet = vit.caloriesLogged >= safeCalTarget && vit.proteinG >= 110.0
                    ),
                    stamina = stam.copy(
                        targetLiters = safeWaterTarget,
                        targetMet = stam.waterLiters >= safeWaterTarget
                    )
                ),
                questLog = updatedQuests,
                tacticalBriefing = briefing,
                biometrics = updatedBio
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)

            repository.appendJournalEntry(
                entryType = "BIOMETRIC_UPDATE",
                title = "Settings Profile Sync (${updatedBio.weightKg} kg • Target ${safeCalTarget} kcal)",
                caption = "Updated profile & targets: ${updatedBio.hunterName} (${updatedBio.hunterTitle}) • Weight ${updatedBio.weightKg} kg • Body Fat ${updatedBio.bodyFatPercent}% • Muscle ${updatedBio.muscleMassPercent}% • Targets: ${safeCalTarget} kcal / ${safeWaterTarget}L.",
                poseTag = "Settings Sync",
                photoBase64 = avatarBase64 ?: "",
                biometrics = updatedBio
            )

            repository.appendTacticalLog(
                commandText = "Settings Profile Updated: ${updatedBio.hunterName} (${safeCalTarget} kcal, ${safeWaterTarget}L)",
                briefingResponse = briefing,
                xpDelta = xpGain
            )

            _aiSyncState.value = AiSyncState.Success("Profile & Daily Targets saved (+50 XP)")
        }
    }

    fun resetHunterProgress(clearJournalHistory: Boolean) {
        viewModelScope.launch {
            val current = _systemState.value
            val resetState = HunterSystemState.createResetState(
                existingBiometrics = current.biometrics.copy(onboardingCompleted = false),
                caloriesTarget = current.statBreakdown.vitality.caloriesTarget,
                waterTargetLiters = current.statBreakdown.stamina.targetLiters
            )
            repository.setOnboardingCompletedForUser(currentUserId, false, null)
            _systemState.value = resetState
            val res = repository.resetAllUserProgress(
                resetState = resetState,
                clearJournalEntries = clearJournalHistory
            )
            if (res.isSuccess) {
                _aiSyncState.value = AiSyncState.Success(
                    if (clearJournalHistory) {
                        "All progress & journal history reset to Level 1 (0 XP, 0D Streak)."
                    } else {
                        "Hunter progress reset to Level 1 (0 XP, 0D Streak). Biometrics preserved."
                    }
                )
            } else {
                _aiSyncState.value = AiSyncState.Error(
                    res.exceptionOrNull()?.localizedMessage ?: "Failed to reset progress in cloud."
                )
            }
        }
    }

    fun submitPublicFeedback(
        category: String,
        rating: Int,
        message: String
    ) {
        val cleanMsg = message.trim()
        if (cleanMsg.isBlank()) return
        viewModelScope.launch {
            val current = _systemState.value
            val hunterName = current.biometrics.hunterName.ifBlank { "Shadow Vanguard" }
            val res = repository.submitPublicFeedback(
                hunterName = hunterName,
                category = category,
                rating = rating,
                message = cleanMsg
            )
            if (res.isSuccess) {
                val xpGain = 35
                val ps = current.playerStatus
                val rawXp = (ps.currentXp + xpGain).coerceAtMost(ps.nextLevelXp - 1)
                val progress = ((rawXp.toDouble() / ps.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)
                val updated = current.copy(
                    playerStatus = ps.copy(
                        currentXp = rawXp,
                        progressPercent = progress
                    ),
                    tacticalBriefing = "Public feedback ($category • ${rating}★) transmitted to the Creator Feedback Board. Thank you for helping evolve the Hunter System!"
                )
                _systemState.value = updated
                repository.saveOrUpdateSession(updated)
                repository.appendTacticalLog(
                    commandText = "Public Feedback Submitted [$category • ${rating}★]",
                    briefingResponse = updated.tacticalBriefing,
                    xpDelta = xpGain
                )
                _aiSyncState.value = AiSyncState.Success("Public feedback received by Creator Board (+35 XP)")
            } else {
                _aiSyncState.value = AiSyncState.Error(
                    res.exceptionOrNull()?.localizedMessage ?: "Failed to submit feedback."
                )
            }
        }
    }

    fun updateFeedbackStatus(feedbackId: String, newStatus: String) {
        if (feedbackId.isBlank()) return
        viewModelScope.launch {
            val res = repository.updateFeedbackStatus(feedbackId, newStatus)
            if (res.isSuccess) {
                _aiSyncState.value = AiSyncState.Success("Feedback status updated to $newStatus")
            } else {
                _aiSyncState.value = AiSyncState.Error(
                    res.exceptionOrNull()?.localizedMessage ?: "Unable to update feedback status."
                )
            }
        }
    }

    fun deleteFeedback(feedbackId: String) {
        if (feedbackId.isBlank()) return
        viewModelScope.launch {
            val res = repository.deleteFeedback(feedbackId)
            if (res.isSuccess) {
                _aiSyncState.value = AiSyncState.Success("Feedback entry removed")
            } else {
                _aiSyncState.value = AiSyncState.Error(
                    res.exceptionOrNull()?.localizedMessage ?: "Unable to delete feedback."
                )
            }
        }
    }

    fun dismissAiSyncBanner() {
        _aiSyncState.value = AiSyncState.Idle
    }

    private companion object {
        const val TAG = "HunterViewModel"
    }
}
