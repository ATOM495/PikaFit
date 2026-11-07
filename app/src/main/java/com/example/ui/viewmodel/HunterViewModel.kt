package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AchievementItem
import com.example.data.model.AppReleaseInfo
import com.example.data.model.ExerciseLogged
import com.example.data.model.FeedbackEntry
import com.example.data.model.HunterBiometrics
import com.example.data.model.HunterSystemState
import com.example.data.model.JournalEntry
import com.example.data.model.QuestItem
import com.example.data.model.StreakDateUtils
import com.example.data.model.TacticalLog
import com.example.data.model.TrainerRankProgression
import com.example.data.repository.HunterRepository
import com.example.util.SmartFoodNutritionEngine
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.Date
import java.util.UUID
import kotlin.math.round

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
    val fiberG: Double = 0.0,
    val grams: Int = 100,
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

    val appReleaseInfo: StateFlow<AppReleaseInfo> = repository.observeAppReleaseInfo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppReleaseInfo()
        )

    private val _dismissedUpdateVersion = MutableStateFlow<Long?>(null)
    val dismissedUpdateVersion: StateFlow<Long?> = _dismissedUpdateVersion.asStateFlow()

    fun dismissUpdatePrompt(versionCode: Long) {
        _dismissedUpdateVersion.value = versionCode
    }

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
            val (wasAlreadyOnboardedLocally, restoredBio) = withContext(Dispatchers.IO) {
                val onboarded = repository.isOnboardingCompletedForUser(currentUserId)
                val bio = if (onboarded) {
                    val cachedStateJson = repository.getCachedStateJsonForUser(currentUserId)
                    if (!cachedStateJson.isNullOrBlank()) {
                        try {
                            val root = JSONObject(cachedStateJson)
                            HunterBiometrics.fromJsonObject(root.optJSONObject("biometrics")).copy(onboardingCompleted = true)
                        } catch (e: Exception) {
                            HunterBiometrics(onboardingCompleted = true)
                        }
                    } else {
                        HunterBiometrics(onboardingCompleted = true)
                    }
                } else null
                Pair(onboarded, bio)
            }
            if (restoredBio != null) {
                _systemState.value = _systemState.value.copy(biometrics = restoredBio)
            }

            repository.observeActiveSession()
                .catch { e ->
                    Log.w(TAG, "Error observing active session", e)
                    _isSessionLoaded.value = true
                }
                .collect { session ->
                    if (session != null) {
                        val evaluated = withContext(Dispatchers.IO) {
                            val loaded = session.toHunterSystemState()
                            val effectiveBiometrics = if (loaded.biometrics.onboardingCompleted || session.stateJson.isNotBlank()) {
                                loaded.biometrics.copy(onboardingCompleted = true)
                            } else {
                                loaded.biometrics
                            }
                            val eval = loaded.copy(biometrics = effectiveBiometrics).evaluateStreakOnLogin()
                            if (effectiveBiometrics.onboardingCompleted) {
                                repository.setOnboardingCompletedForUser(currentUserId, true, eval.toStateJson())
                            }
                            eval
                        }
                        _systemState.value = evaluated
                        hasSeededInitialSession = true
                        _isSessionLoaded.value = true
                        seedWelcomeNutritionChatIfEmpty(evaluated)
                        if (evaluated.biometrics.onboardingCompleted && !hasAutoTriggeredCoachThisSession) {
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
                text = "Pika-Pika! I'm Pika-Dietitian, your Smart Nutrition & Diet Plan Bot! ⚡\n\n" +
                    "Type ANY food name (e.g., \"tea\", \"150g chicken breast\", \"200g rice\", \"idli\", \"paneer\", \"oats\", \"banana\") or ask for a \"diet plan\" and I will give you:\n" +
                    "• Food overview & fitness benefits\n" +
                    "• Nutritional Breakdown :- (Calories, Carbs, Fats, Protein, Fiber)\n" +
                    "• Diet Plan Ideas :- tailored to your ${bio.goalDisplayLabel} target (${vit.caloriesTarget} kcal & ${bio.recommendedProteinG}g protein/day)!",
                suggestedMealName = "High-Protein Greek Yogurt & Berry Bowl (200g)",
                suggestedCalories = 260,
                suggestedProteinG = 20.0
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

                MANDATORY OUTPUT FORMAT FOR ANY FOOD OR DIET QUERY ("$cleanQuery"):
                Always format your response strictly using this clean, structured layout:

                <Food Name> - <1-2 sentence description of the food, its energy profile, and fitness benefits>

                Nutritional Breakdown (per serving / 100g) :-
                • Calories - <kcal>
                • Carbs - <grams>g
                • Fats - <grams>g
                • Protein - <grams>g
                • Fiber - <grams>g
                • Micronutrients - <key vitamins, minerals, antioxidants>

                Diet Plan Ideas (${bio.goalDisplayLabel}) :-
                • <Morning / Pre-Workout Idea with portion in grams>
                • <Lunch / Post-Workout Idea with portion in grams>
                • <Dinner / Daily Balance Tip tailored to ${vit.caloriesTarget} kcal & ${bio.recommendedProteinG}g protein>
            """.trimIndent()

            val recentContext = previousHistory
                .filter { it.text.isNotBlank() && !it.isStreaming && !it.isError }
                .takeLast(4)
                .joinToString("\n") { msg ->
                    val sender = if (msg.role == "user") "Trainer" else "Pika-Dietitian"
                    "$sender: ${msg.text.take(200)}"
                }

            val fullPrompt = buildString {
                appendLine(systemContextPrompt)
                if (recentContext.isNotBlank()) {
                    appendLine()
                    appendLine("Recent Conversation:")
                    appendLine(recentContext)
                }
                appendLine()
                appendLine("User Food / Diet Input: \"$cleanQuery\"")
                appendLine("Respond immediately following the exact '<Food> - ... \\nNutritional Breakdown :-\\n• Calories - ...\\n• Carbs - ...\\n• Fats - ...\\n• Protein - ...\\nDiet Plan Ideas :-' structure.")
            }

            val accumulated = StringBuilder()
            try {
                withTimeoutOrNull(20_000L) {
                    nutritionChatModel.generateContentStream(fullPrompt).collect { chunk ->
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Nutrition chat Gemini generation notice: ${e.message}", e)
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
                val resolvedMeal = if (accumulated.isNotBlank()) {
                    extractSuggestedMealFromQuery(cleanQuery, bio)
                } else {
                    Triple(sugMeal, sugCal, sugPro)
                }

                _nutritionChatMessages.value = _nutritionChatMessages.value.map { msg ->
                    if (msg.id == placeholderId) {
                        msg.copy(
                            text = finalText,
                            isStreaming = false,
                            isError = false,
                            suggestedMealName = resolvedMeal.first,
                            suggestedCalories = resolvedMeal.second,
                            suggestedProteinG = resolvedMeal.third
                        )
                    } else msg
                }
                _isNutritionChatLoading.value = false
            }
        }
    }

    private fun extractSuggestedMealFromQuery(query: String, bio: HunterBiometrics): Triple<String, Int, Double> {
        val (foodPart, grams) = SmartFoodNutritionEngine.extractGramsFromQuery(query)
        val calc = SmartFoodNutritionEngine.calculateForGrams(foodPart, grams)
        return Triple("${calc.foodName} (${calc.grams}g)", calc.calories, calc.proteinG)
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
        val calc = SmartFoodNutritionEngine.formatChatbotResponseForQuery(
            query = query,
            bio = bio,
            caloriesTarget = caloriesTarget,
            caloriesLogged = caloriesLogged,
            proteinLogged = proteinLogged,
            waterLogged = waterLogged,
            waterTarget = waterTarget
        )
        val formattedText = SmartFoodNutritionEngine.buildFormattedChatReplyText(
            query = query,
            calc = calc,
            bio = bio,
            caloriesTarget = caloriesTarget,
            caloriesLogged = caloriesLogged,
            proteinLogged = proteinLogged
        )
        return NutritionReplyResult(
            replyText = formattedText,
            mealName = "${calc.foodName} (${calc.grams}g)",
            calories = calc.calories,
            proteinG = calc.proteinG
        )
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
            val starterTier = TrainerRankProgression.tierForXp(0)
            val calibratedBio = HunterBiometrics(
                hunterName = cleanName,
                hunterTitle = "${starterTier.classTitle} • ${starterTier.pokemonName}",
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
                "Pika-Pika! Welcome Trainer $cleanName (${starterTier.classTitle})! Target: ${freshState.statBreakdown.vitality.caloriesTarget} kcal & ${freshState.statBreakdown.stamina.targetLiters}L water."
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

            val (xpUpdatedState, scaledXpGain) = current.copy(biometrics = updatedBio).applyScaledXpProgress(50)

            val cleanNote = note.trim().ifBlank {
                "Weight ${updatedBio.weightKg} kg • Body Fat ${updatedBio.bodyFatPercent}% • Muscle Mass ${updatedBio.muscleMassPercent}% (${updatedBio.muscleMassKg} kg)"
            }
            val briefing = "New biometric point plotted to Firestore: ${updatedBio.weightKg} kg, ${updatedBio.bodyFatPercent}% Body Fat, ${updatedBio.muscleMassPercent}% Muscle Mass (${updatedBio.muscleMassKg} kg). Class: ${xpUpdatedState.playerStatus.rank} (+$scaledXpGain XP)."

            val updatedState = xpUpdatedState.copy(
                tacticalBriefing = briefing
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)

            repository.appendJournalEntry(
                entryType = "BIOMETRIC_UPDATE",
                title = "Telemetry Point: ${updatedBio.weightKg}kg • ${updatedBio.bodyFatPercent}% BF • ${updatedBio.muscleMassPercent}% Muscle",
                caption = cleanNote,
                poseTag = "Recharts Sync",
                photoBase64 = "",
                biometrics = updatedState.biometrics
            )

            repository.appendTacticalLog(
                commandText = "Plotted Biometrics: ${updatedBio.weightKg}kg, BF ${updatedBio.bodyFatPercent}%, Muscle ${updatedBio.muscleMassPercent}%",
                briefingResponse = briefing,
                xpDelta = scaledXpGain
            )

            _aiSyncState.value = AiSyncState.Success("Biometric point saved to Firestore & plotted (+$scaledXpGain XP • ${updatedState.playerStatus.rank})")
        }
    }

    fun updateProfileAndBiometrics(
        hunterName: String,
        hunterTitle: String,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        biologicalSex: String,
        avatarBase64: String? = null,
        targetWeightKg: Double? = null,
        targetPhysique: String? = null
    ) {
        viewModelScope.launch {
            val current = _systemState.value
            val prevBio = current.biometrics
            val updatedBio = prevBio.copy(
                hunterName = hunterName.trim().take(60).ifBlank { prevBio.hunterName },
                avatarBase64 = avatarBase64?.take(18000) ?: prevBio.avatarBase64,
                weightKg = weightKg.coerceIn(25.0, 350.0),
                heightCm = heightCm.coerceIn(100.0, 250.0),
                age = age.coerceIn(10, 120),
                biologicalSex = if (biologicalSex.uppercase() == "FEMALE") "FEMALE" else "MALE",
                targetWeightKg = (targetWeightKg ?: prevBio.targetWeightKg).coerceIn(25.0, 350.0),
                targetPhysique = targetPhysique?.trim()?.take(60)?.ifBlank { prevBio.targetPhysique } ?: prevBio.targetPhysique,
                onboardingCompleted = true
            )

            val (xpUpdatedState, scaledXpGain) = current.copy(biometrics = updatedBio).applyScaledXpProgress(50)
            val calibratedBio = xpUpdatedState.biometrics

            val briefing = "Biometric telemetry calibrated for ${calibratedBio.hunterName} (${xpUpdatedState.playerStatus.rank}): ${calibratedBio.weightKg}kg -> Target ${calibratedBio.targetWeightKg}kg (${calibratedBio.targetPhysique}). BMI ${calibratedBio.bmi} • Body Fat ${calibratedBio.bodyFatPercent}% • Muscle ${calibratedBio.muscleMassPercent}%. Archived to Personal Journal (+$scaledXpGain XP)."

            val updatedState = xpUpdatedState.copy(
                tacticalBriefing = briefing
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)

            val summaryCaption = buildString {
                append("Weight: ${calibratedBio.weightKg} kg -> Target: ${calibratedBio.targetWeightKg} kg (${calibratedBio.targetPhysique})\n")
                append("Height: ${calibratedBio.heightCm.toInt()} cm • Age: ${calibratedBio.age} (${calibratedBio.biologicalSex}) • Class: ${updatedState.playerStatus.rank} (${updatedState.playerStatus.currentXp} XP)\n")
                append("BMI: ${calibratedBio.bmi} (${calibratedBio.bmiCategory}) • Body Fat: ${calibratedBio.bodyFatPercent}% (${calibratedBio.fatMassKg} kg) • Muscle: ${calibratedBio.muscleMassPercent}% (${calibratedBio.muscleMassKg} kg)")
            }

            repository.appendJournalEntry(
                entryType = "BIOMETRIC_UPDATE",
                title = "Biometric Profile Sync (${calibratedBio.weightKg} kg • BMI ${calibratedBio.bmi})",
                caption = summaryCaption,
                poseTag = "Biometric Sync",
                photoBase64 = avatarBase64 ?: "",
                biometrics = calibratedBio
            )

            repository.appendTacticalLog(
                commandText = "Updated Biometrics: ${calibratedBio.weightKg}kg, ${calibratedBio.heightCm.toInt()}cm, Age ${calibratedBio.age}",
                briefingResponse = briefing,
                xpDelta = scaledXpGain
            )

            _aiSyncState.value = AiSyncState.Success("Biometrics saved (+$scaledXpGain XP • Class: ${updatedState.playerStatus.rank})")
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

            val (xpUpdatedState, scaledXpGain) = current.copy(biometrics = updatedBio).applyScaledXpProgress(85)
            val calibratedBio = xpUpdatedState.biometrics

            val cleanTitle = title.trim().ifBlank { "Physique Check-In ($poseTag)" }
            val cleanCaption = caption.trim().ifBlank {
                "Documented physique progress at ${calibratedBio.weightKg} kg (BMI ${calibratedBio.bmi}, BF ${calibratedBio.bodyFatPercent}%, Muscle ${calibratedBio.muscleMassPercent}%)."
            }

            val hasPhotoAchievement = xpUpdatedState.unlockedAchievements.any { it.title == "Visual Chronicler" }
            val updatedAchievements = if (!hasPhotoAchievement) {
                xpUpdatedState.unlockedAchievements + AchievementItem(
                    title = "Visual Chronicler",
                    description = "Uploaded and archived a physique progress photo to your Personal Hunter Journal.",
                    rarity = "EPIC"
                )
            } else {
                xpUpdatedState.unlockedAchievements
            }

            val briefing = "Progress photo \"$cleanTitle\" [$poseTag] archived in Personal Journal with full biometric telemetry (${calibratedBio.weightKg}kg, BF ${calibratedBio.bodyFatPercent}%, Muscle ${calibratedBio.muscleMassPercent}%). Awarded +$scaledXpGain XP (Class: ${xpUpdatedState.playerStatus.rank})."

            val updatedState = xpUpdatedState.copy(
                unlockedAchievements = updatedAchievements,
                tacticalBriefing = briefing
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)

            val res = repository.appendJournalEntry(
                entryType = "PHOTO_PROGRESS",
                title = cleanTitle,
                caption = cleanCaption,
                poseTag = poseTag,
                photoBase64 = photoBase64,
                biometrics = calibratedBio
            )

            repository.appendTacticalLog(
                commandText = "Journal Photo Logged: $cleanTitle [$poseTag]",
                briefingResponse = briefing,
                xpDelta = scaledXpGain
            )

            if (res.isSuccess) {
                _aiSyncState.value = AiSyncState.Success("Progress photo added to Personal Journal (+$scaledXpGain XP)")
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
        fatsG: Double = (calories * 0.03),
        fiberG: Double = 0.0,
        grams: Int = 100
    ) {
        if (calories <= 0 && mealName.isBlank()) return
        viewModelScope.launch {
            val cleanMeal = mealName.trim().ifBlank { "Tactical Ration" }
            val safeCals = calories.coerceIn(1, 10000)
            val safePro = round(proteinG.coerceIn(0.0, 500.0) * 10.0) / 10.0
            val safeCarbs = round(carbsG.coerceIn(0.0, 1000.0) * 10.0) / 10.0
            val safeFats = round(fatsG.coerceIn(0.0, 500.0) * 10.0) / 10.0
            val safeFiber = round(fiberG.coerceIn(0.0, 200.0) * 10.0) / 10.0
            val safeGrams = grams.coerceIn(1, 5000)

            val current = _systemState.value
            val vit = current.statBreakdown.vitality
            val newKcal = (vit.caloriesLogged + safeCals).coerceIn(0, 50000)
            val newProtein = round((vit.proteinG + safePro).coerceIn(0.0, 2000.0) * 10.0) / 10.0
            val newCarbs = round((vit.carbsG + safeCarbs).coerceIn(0.0, 5000.0) * 10.0) / 10.0
            val newFats = round((vit.fatsG + safeFats).coerceIn(0.0, 2000.0) * 10.0) / 10.0
            val newFiber = round((vit.fiberG + safeFiber).coerceIn(0.0, 500.0) * 10.0) / 10.0
            val targetMet = newKcal >= vit.caloriesTarget && newProtein >= 110.0

            // Apply productive input to check the user in for their Daily Streak (awards tier-scaled daily completion bonus)
            val (streakUpdatedState, newlyCheckedIn) = current.applyProductiveInput(
                isFoodInput = true,
                inputSummary = "$cleanMeal (+$safeCals kcal, ${safePro}g P, ${safeCarbs}g C, ${safeFats}g F)"
            )

            val baseXpGain = 45
            val (xpUpdatedState, scaledActionXp) = streakUpdatedState.applyScaledXpProgress(baseXpGain)
            val ps = xpUpdatedState.playerStatus
            val dailyBonusXp = if (newlyCheckedIn) TrainerRankProgression.tierForXp(current.playerStatus.currentXp).dailyCompletionXp else 0
            val totalXpGain = scaledActionXp + dailyBonusXp

            val updatedQuests = xpUpdatedState.questLog.map { q ->
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
                "Daily Productive Streak Checked In (${ps.streakDays} Days • +$dailyBonusXp Daily XP)!"
            } else {
                "Productive Streak active (${ps.streakDays} Days • Class: ${ps.rank})."
            }

            val updatedBriefing = "Logged food \"$cleanMeal\" (+$safeCals kcal | P: ${safePro}g • C: ${safeCarbs}g • F: ${safeFats}g). Total intake: $newKcal / ${vit.caloriesTarget} kcal. $streakNote"

            val finalState = xpUpdatedState.copy(
                statBreakdown = xpUpdatedState.statBreakdown.copy(
                    vitality = vit.copy(
                        caloriesLogged = newKcal,
                        proteinG = newProtein,
                        carbsG = newCarbs,
                        fatsG = newFats,
                        fiberG = newFiber,
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
                fiberG = safeFiber,
                grams = safeGrams,
                xpAwarded = totalXpGain,
                triggeredDailyCheckIn = newlyCheckedIn,
                previousStreakDays = current.playerStatus.streakDays
            )

            _systemState.value = finalState
            repository.saveOrUpdateSession(finalState)
            repository.appendTacticalLog(
                commandText = "Food Input: $cleanMeal (+$safeCals kcal, ${safePro}g P, ${safeCarbs}g C, ${safeFats}g F)",
                briefingResponse = updatedBriefing,
                xpDelta = totalXpGain
            )

            _aiSyncState.value = AiSyncState.Success(
                if (newlyCheckedIn) {
                    "Daily Streak Checked In (${ps.streakDays}D) via $cleanMeal! (+$totalXpGain XP • ${ps.rank})"
                } else {
                    "Logged $cleanMeal: +$safeCals kcal (+$totalXpGain XP • ${ps.rank})"
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
            val (xpUpdatedState, scaledActionXp) = streakState.applyScaledXpProgress(baseXpGain)
            val ps = xpUpdatedState.playerStatus
            val dailyBonusXp = if (newlyCheckedIn) TrainerRankProgression.tierForXp(current.playerStatus.currentXp).dailyCompletionXp else 0
            val totalXpGain = scaledActionXp + dailyBonusXp

            val updatedQuests = xpUpdatedState.questLog.map { q ->
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

            val updated = xpUpdatedState.copy(
                statBreakdown = xpUpdatedState.statBreakdown.copy(
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
                    xpAwarded = totalXpGain,
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
                _aiSyncState.value = AiSyncState.Success("Daily Streak Checked In (${ps.streakDays}D) via Food Input! (+$totalXpGain XP • ${ps.rank})")
            } else if (deltaKcal > 0) {
                _aiSyncState.value = AiSyncState.Success("Logged +$deltaKcal kcal (+${deltaProteinG.toInt()}g protein • +$totalXpGain XP)")
            }
        }
    }

    fun quickAddWater(litersDelta: Double, customLabel: String? = null) {
        if (litersDelta == 0.0) return
        viewModelScope.launch {
            val safeDelta = round(litersDelta.coerceIn(-10.0, 15.0) * 100.0) / 100.0
            val current = _systemState.value
            val stam = current.statBreakdown.stamina
            val newWater = round((stam.waterLiters + safeDelta) * 100.0) / 100.0
            val clampedWater = newWater.coerceIn(0.0, 50.0)
            val targetMet = clampedWater >= stam.targetLiters

            val baseXpGain = if (safeDelta > 0) 20 else 0
            val (xpUpdatedState, scaledXpGain) = current.applyScaledXpProgress(baseXpGain)

            val mlEquivalent = (safeDelta * 1000).toInt()
            val entryLabel = customLabel?.takeIf { it.isNotBlank() }
                ?: "+${safeDelta}L (${mlEquivalent}ml) Water"

            val briefing = "Hydration reserves at ${clampedWater}L / ${stam.targetLiters}L ($entryLabel). Cellular nutrient transport and joint lubrication optimized."
            val updated = xpUpdatedState.copy(
                statBreakdown = xpUpdatedState.statBreakdown.copy(
                    stamina = stam.copy(
                        waterLiters = clampedWater,
                        targetMet = targetMet
                    )
                ),
                tacticalBriefing = briefing
            )
            if (safeDelta > 0) {
                _waterHistory.value = _waterHistory.value + WaterEntryRecord(
                    label = entryLabel,
                    litersDelta = safeDelta,
                    xpAwarded = scaledXpGain
                )
            }
            _systemState.value = updated
            repository.saveOrUpdateSession(updated)
            repository.appendTacticalLog(
                commandText = "Hydration $entryLabel (${clampedWater}L total)",
                briefingResponse = briefing,
                xpDelta = scaledXpGain
            )
            if (safeDelta > 0) {
                _aiSyncState.value = AiSyncState.Success("Logged $entryLabel (${clampedWater}L / ${stam.targetLiters}L • +$scaledXpGain XP)")
            }
        }
    }

    /**
     * Undoes the most recent Food/Calorie entry (or removes a specific FoodEntryRecord by id),
     * reverting calories, protein, carbs, fats, fiber, XP, and daily food input counters if mistakenly entered.
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
            val removeFiber = targetRecord?.fiberG ?: 0.0
            val removeLabel = targetRecord?.mealName ?: "${removeCals} kcal entry"
            val removeXp = targetRecord?.xpAwarded ?: 40

            if (targetRecord != null) {
                _foodHistory.value = history.filterNot { it.id == targetRecord.id }
            }

            val newKcal = (vit.caloriesLogged - removeCals).coerceAtLeast(0)
            val newProtein = round((vit.proteinG - removePro).coerceAtLeast(0.0) * 10.0) / 10.0
            val newCarbs = round((vit.carbsG - removeCarbs).coerceAtLeast(0.0) * 10.0) / 10.0
            val newFats = round((vit.fatsG - removeFats).coerceAtLeast(0.0) * 10.0) / 10.0
            val newFiber = round((vit.fiberG - removeFiber).coerceAtLeast(0.0) * 10.0) / 10.0
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

            val (xpRevertedState, _) = current.applyScaledXpProgress(-removeXp, isRawAlreadyScaled = true)

            val updatedQuests = xpRevertedState.questLog.map { q ->
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

            val updatedBriefing = "Undid food entry \"$removeLabel\" (-$removeCals kcal, -${removePro}g protein). Updated total: $newKcal / ${vit.caloriesTarget} kcal."

            val updatedState = xpRevertedState.copy(
                playerStatus = xpRevertedState.playerStatus.copy(
                    todayFoodInputs = newFoodInputs,
                    checkedInToday = newCheckedIn,
                    streakDays = newStreakDays
                ),
                statBreakdown = xpRevertedState.statBreakdown.copy(
                    vitality = vit.copy(
                        caloriesLogged = newKcal,
                        proteinG = newProtein,
                        carbsG = newCarbs,
                        fatsG = newFats,
                        fiberG = newFiber,
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
            _aiSyncState.value = AiSyncState.Success("Undid food entry: $removeLabel (-$removeCals kcal, -${removePro}g P)")
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

            val newWater = (round((stam.waterLiters - removeLiters) * 100.0) / 100.0).coerceAtLeast(0.0)
            val targetMet = newWater >= stam.targetLiters

            val (xpRevertedState, _) = current.applyScaledXpProgress(-removeXp, isRawAlreadyScaled = true)

            val updatedBriefing = "Undid water entry (-${removeLiters}L). Hydration reserves updated to ${newWater}L / ${stam.targetLiters}L."

            val updatedState = xpRevertedState.copy(
                statBreakdown = xpRevertedState.statBreakdown.copy(
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
            val (xpUpdatedState, scaledActionXp) = streakState.applyScaledXpProgress(baseXpGain)
            val ps = xpUpdatedState.playerStatus
            val dailyBonusXp = if (newlyCheckedIn) TrainerRankProgression.tierForXp(current.playerStatus.currentXp).dailyCompletionXp else 0
            val totalXpGain = scaledActionXp + dailyBonusXp

            val streakSuffix = if (newlyCheckedIn) " Daily Productive Streak Checked In (${ps.streakDays}D)!" else ""
            val briefing = "Logged ${newEx.exerciseName} (${newEx.setsCompleted}x${newEx.repsPerSet}). Progressive overload ${if (overload) "CONFIRMED" else "maintained"} — mechanical tension stimulus recorded.$streakSuffix"
            val updated = xpUpdatedState.copy(
                statBreakdown = xpUpdatedState.statBreakdown.copy(
                    strength = xpUpdatedState.statBreakdown.strength.copy(
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
                    "Daily Streak Checked In (${ps.streakDays}D) via Exercise Input! (+$totalXpGain XP • ${ps.rank})"
                } else {
                    "Exercise logged: ${newEx.exerciseName} (+$totalXpGain XP • ${ps.rank})"
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
            val (xpUpdatedState, _) = current.applyScaledXpProgress(xpDelta)

            val updated = xpUpdatedState.copy(
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
        avatarBase64: String? = null,
        targetWeightKg: Double? = null,
        targetPhysique: String? = null
    ) {
        viewModelScope.launch {
            val current = _systemState.value
            val prevBio = current.biometrics
            val updatedBio = prevBio.copy(
                hunterName = hunterName.trim().take(60).ifBlank { prevBio.hunterName },
                avatarBase64 = avatarBase64?.take(18000) ?: prevBio.avatarBase64,
                weightKg = weightKg.coerceIn(25.0, 350.0),
                heightCm = heightCm.coerceIn(100.0, 250.0),
                age = age.coerceIn(10, 120),
                biologicalSex = if (biologicalSex.uppercase() == "FEMALE") "FEMALE" else "MALE",
                targetWeightKg = (targetWeightKg ?: prevBio.targetWeightKg).coerceIn(25.0, 350.0),
                targetPhysique = targetPhysique?.trim()?.take(60)?.ifBlank { prevBio.targetPhysique } ?: prevBio.targetPhysique,
                customBodyFatPercent = customBodyFatPercent?.coerceIn(3.0, 65.0),
                customMuscleMassPercent = customMuscleMassPercent?.coerceIn(20.0, 90.0),
                onboardingCompleted = true
            )

            val safeCalTarget = caloriesTarget.coerceIn(800, 15000)
            val safeWaterTarget = ((waterTargetLiters.coerceIn(0.5, 20.0) * 10.0).toInt() / 10.0)
            val vit = current.statBreakdown.vitality
            val stam = current.statBreakdown.stamina

            val (xpUpdatedState, scaledXpGain) = current.copy(biometrics = updatedBio).applyScaledXpProgress(50)
            val calibratedBio = xpUpdatedState.biometrics

            val briefing = "Profile & Daily Targets updated in Settings for ${calibratedBio.hunterName} (${xpUpdatedState.playerStatus.rank}): ${calibratedBio.weightKg}kg -> Target ${calibratedBio.targetWeightKg}kg (${calibratedBio.targetPhysique}), Daily Targets ${safeCalTarget} kcal & ${safeWaterTarget}L water."

            val updatedQuests = xpUpdatedState.questLog.map { q ->
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

            val updatedState = xpUpdatedState.copy(
                statBreakdown = xpUpdatedState.statBreakdown.copy(
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
                tacticalBriefing = briefing
            )

            _systemState.value = updatedState
            repository.saveOrUpdateSession(updatedState)

            repository.appendJournalEntry(
                entryType = "BIOMETRIC_UPDATE",
                title = "Settings Profile Sync (${calibratedBio.weightKg} kg -> ${calibratedBio.targetWeightKg} kg)",
                caption = "Updated profile & targets: ${calibratedBio.hunterName} (${updatedState.playerStatus.rank}) • Weight ${calibratedBio.weightKg} kg • Target ${calibratedBio.targetWeightKg} kg (${calibratedBio.targetPhysique}) • Targets: ${safeCalTarget} kcal / ${safeWaterTarget}L.",
                poseTag = "Settings Sync",
                photoBase64 = avatarBase64 ?: "",
                biometrics = calibratedBio
            )

            repository.appendTacticalLog(
                commandText = "Settings Profile Updated: ${calibratedBio.hunterName} (${safeCalTarget} kcal, ${safeWaterTarget}L)",
                briefingResponse = briefing,
                xpDelta = scaledXpGain
            )

            _aiSyncState.value = AiSyncState.Success("Profile & Daily Targets saved (+$scaledXpGain XP • ${updatedState.playerStatus.rank})")
        }
    }

    fun resetHunterProgress(clearJournalHistory: Boolean) {
        viewModelScope.launch {
            val current = _systemState.value
            _foodHistory.value = emptyList()
            _waterHistory.value = emptyList()
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
                        "All progress & journal history reset to Beginner (0 XP, 0D Streak)."
                    } else {
                        "Hunter progress reset to Beginner (0 XP, 0D Streak). Biometrics preserved."
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
                val (xpUpdatedState, scaledXpGain) = current.applyScaledXpProgress(35)
                val updated = xpUpdatedState.copy(
                    tacticalBriefing = "Public feedback ($category • ${rating}★) transmitted to the Creator Feedback Board. Thank you for helping evolve the Hunter System!"
                )
                _systemState.value = updated
                repository.saveOrUpdateSession(updated)
                repository.appendTacticalLog(
                    commandText = "Public Feedback Submitted [$category • ${rating}★]",
                    briefingResponse = updated.tacticalBriefing,
                    xpDelta = scaledXpGain
                )
                _aiSyncState.value = AiSyncState.Success("Public feedback received by Creator Board (+$scaledXpGain XP)")
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
