package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class HunterBiometrics(
    val hunterName: String = "New Trainer",
    val hunterTitle: String = "PikaFit Rookie",
    val avatarBase64: String = "",
    val weightKg: Double = 70.0,
    val heightCm: Double = 172.0,
    val age: Int = 22,
    val biologicalSex: String = "MALE", // MALE, FEMALE
    val fitnessGoal: String = "WEIGHT_GAIN", // WEIGHT_GAIN, WEIGHT_LOSS, MAINTENANCE
    val experienceLevel: String = "BEGINNER", // BEGINNER, INTERMEDIATE, PROFESSIONAL
    val onboardingCompleted: Boolean = false,
    val customBodyFatPercent: Double? = null,
    val customMuscleMassPercent: Double? = null
) {
    private fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0

    val bmi: Double
        get() {
            val hMeters = (heightCm / 100.0).coerceAtLeast(0.8)
            return round1((weightKg / (hMeters * hMeters)).coerceIn(5.0, 120.0))
        }

    val bmiCategory: String
        get() = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Optimal / Normal"
            bmi < 30.0 -> "Athletic / Overweight"
            else -> "High Mass / Obese"
        }

    val bodyFatPercent: Double
        get() {
            if (customBodyFatPercent != null && customBodyFatPercent > 0.0) {
                return round1(customBodyFatPercent.coerceIn(3.0, 65.0))
            }
            val isMale = biologicalSex.uppercase() != "FEMALE"
            val sexFactor = if (isMale) 1 else 0
            // Deurenberg body fat estimation formula adjusted for athletic training
            val rawBf = (1.20 * bmi) + (0.23 * age) - (10.8 * sexFactor) - 5.4
            return round1(rawBf.coerceIn(5.0, 65.0))
        }

    val fatMassKg: Double
        get() = round1(weightKg * (bodyFatPercent / 100.0))

    val leanBodyMassKg: Double
        get() = round1((weightKg - fatMassKg).coerceAtLeast(10.0))

    val leanMassPercent: Double
        get() = round1((100.0 - bodyFatPercent).coerceIn(35.0, 95.0))

    val boneMassKg: Double
        get() {
            val isMale = biologicalSex.uppercase() != "FEMALE"
            val ratio = if (isMale) 0.042 else 0.038
            return round1((weightKg * ratio).coerceIn(1.5, 8.0))
        }

    val boneMassPercent: Double
        get() = round1(((boneMassKg / weightKg.coerceAtLeast(20.0)) * 100.0).coerceIn(2.0, 10.0))

    val waterMassKg: Double
        get() {
            // Total Body Water (TBW) is ~73% of Fat-Free / Lean Body Mass
            return round1((leanBodyMassKg * 0.73).coerceIn(10.0, 150.0))
        }

    val waterMassPercent: Double
        get() = round1(((waterMassKg / weightKg.coerceAtLeast(20.0)) * 100.0).coerceIn(35.0, 80.0))

    val muscleMassPercent: Double
        get() {
            if (customMuscleMassPercent != null && customMuscleMassPercent > 0.0) {
                return round1(customMuscleMassPercent.coerceIn(20.0, 90.0))
            }
            val derivedKg = (leanBodyMassKg - boneMassKg).coerceAtLeast(10.0)
            return round1(((derivedKg / weightKg.coerceAtLeast(20.0)) * 100.0).coerceIn(20.0, 90.0))
        }

    val muscleMassKg: Double
        get() {
            if (customMuscleMassPercent != null && customMuscleMassPercent > 0.0) {
                return round1((weightKg * (muscleMassPercent / 100.0)).coerceAtLeast(10.0))
            }
            return round1((leanBodyMassKg - boneMassKg).coerceAtLeast(10.0))
        }

    val bmrKcal: Int
        get() {
            val isMale = biologicalSex.uppercase() != "FEMALE"
            val s = if (isMale) 5.0 else -161.0
            return ((10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + s).roundToInt().coerceIn(800, 6000)
        }

    val recommendedCaloriesTarget: Int
        get() {
            val activityMult = when (experienceLevel.uppercase()) {
                "PROFESSIONAL" -> 1.65
                "INTERMEDIATE" -> 1.50
                else -> 1.375
            }
            val tdee = (bmrKcal * activityMult).roundToInt()
            return when (fitnessGoal.uppercase()) {
                "WEIGHT_GAIN" -> (tdee + 400).coerceIn(1400, 6000)
                "WEIGHT_LOSS" -> (tdee - 400).coerceIn(1200, 5000)
                else -> tdee.coerceIn(1300, 5500)
            }
        }

    val recommendedProteinG: Int
        get() {
            val mult = if (experienceLevel.uppercase() == "PROFESSIONAL") 2.0 else 1.7
            return (weightKg * mult).roundToInt().coerceIn(70, 300)
        }

    val recommendedWaterLiters: Double
        get() {
            val bonus = if (experienceLevel.uppercase() == "PROFESSIONAL") 0.5 else 0.0
            return round1(((weightKg * 0.045) + bonus).coerceIn(2.0, 8.0))
        }

    val goalDisplayLabel: String
        get() = when (fitnessGoal.uppercase()) {
            "WEIGHT_GAIN" -> "Weight Gain (Muscle Bulk)"
            "WEIGHT_LOSS" -> "Weight Loss (Fat Shred)"
            else -> "Lean Recomp & Maintain"
        }

    val experienceDisplayLabel: String
        get() = when (experienceLevel.uppercase()) {
            "PROFESSIONAL" -> "Professional Athlete"
            "INTERMEDIATE" -> "Intermediate Challenger"
            else -> "Beginner Trainer"
        }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("hunter_name", hunterName)
        put("hunter_title", hunterTitle)
        put("avatar_base64", avatarBase64)
        put("weight_kg", weightKg)
        put("height_cm", heightCm)
        put("age", age)
        put("biological_sex", biologicalSex)
        put("fitness_goal", fitnessGoal)
        put("experience_level", experienceLevel)
        put("onboarding_completed", onboardingCompleted)
        put("bmi", bmi)
        put("body_fat_percent", bodyFatPercent)
        put("muscle_mass_percent", muscleMassPercent)
        put("bone_mass_kg", boneMassKg)
        put("water_mass_kg", waterMassKg)
        if (customBodyFatPercent != null) put("custom_body_fat_percent", customBodyFatPercent)
        if (customMuscleMassPercent != null) put("custom_muscle_mass_percent", customMuscleMassPercent)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject?): HunterBiometrics {
            if (obj == null) return HunterBiometrics()
            val customBf = if (obj.has("custom_body_fat_percent") && !obj.isNull("custom_body_fat_percent")) {
                obj.optDouble("custom_body_fat_percent").takeIf { !it.isNaN() && it > 0.0 }
            } else null
            val customMm = if (obj.has("custom_muscle_mass_percent") && !obj.isNull("custom_muscle_mass_percent")) {
                obj.optDouble("custom_muscle_mass_percent").takeIf { !it.isNaN() && it > 0.0 }
            } else null

            return HunterBiometrics(
                hunterName = obj.optString("hunter_name", "New Trainer").take(60).ifBlank { "New Trainer" },
                hunterTitle = obj.optString("hunter_title", "PikaFit Rookie").take(60).ifBlank { "PikaFit Rookie" },
                avatarBase64 = obj.optString("avatar_base64", "").take(18000),
                weightKg = obj.optDouble("weight_kg", 70.0).coerceIn(20.0, 400.0),
                heightCm = obj.optDouble("height_cm", 172.0).coerceIn(80.0, 260.0),
                age = obj.optInt("age", 22).coerceIn(10, 120),
                biologicalSex = obj.optString("biological_sex", "MALE").uppercase().let {
                    if (it == "FEMALE") "FEMALE" else "MALE"
                },
                fitnessGoal = obj.optString("fitness_goal", "WEIGHT_GAIN").uppercase().let {
                    if (it in setOf("WEIGHT_GAIN", "WEIGHT_LOSS", "MAINTENANCE")) it else "WEIGHT_GAIN"
                },
                experienceLevel = obj.optString("experience_level", "BEGINNER").uppercase().let {
                    if (it in setOf("BEGINNER", "INTERMEDIATE", "PROFESSIONAL")) it else "BEGINNER"
                },
                onboardingCompleted = obj.optBoolean("onboarding_completed", false),
                customBodyFatPercent = customBf,
                customMuscleMassPercent = customMm
            )
        }
    }
}

@IgnoreExtraProperties
data class JournalEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val entryType: String = "PHOTO_PROGRESS", // PHOTO_PROGRESS, BIOMETRIC_UPDATE
    val title: String = "",
    val caption: String = "",
    val poseTag: String = "Front Relaxed",
    val photoBase64: String = "",
    val weightKg: Double = 74.0,
    val heightCm: Double = 178.0,
    val age: Long = 24L,
    val bmi: Double = 23.4,
    val bodyFatPercent: Double = 15.2,
    val muscleMassPercent: Double = 80.6,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val muscleMassKg: Double
        get() = ((weightKg * (muscleMassPercent / 100.0)) * 10.0).roundToInt() / 10.0

    @get:Exclude
    val fatMassKg: Double
        get() = ((weightKg * (bodyFatPercent / 100.0)) * 10.0).roundToInt() / 10.0
}

object StreakDateUtils {
    private const val DATE_PATTERN = "yyyy-MM-dd"

    fun todayDateString(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)
        return sdf.format(Date(nowMillis))
    }

    fun daysAgoDateString(daysAgo: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)
        return sdf.format(cal.time)
    }

    fun defaultHistoryDates(nowMillis: Long = System.currentTimeMillis()): List<String> {
        // Past 4 consecutive days leading up to yesterday (today is NOT checked in until user inputs food or exercise)
        return (4 downTo 1).map { daysAgoDateString(it, nowMillis) }
    }
}

data class PlayerStatus(
    val level: Int = 2,
    val rank: String = "E-Rank Hunter",
    val currentXp: Int = 620,
    val nextLevelXp: Int = 1000,
    val progressPercent: Double = 62.0,
    val hpPercent: Double = 100.0,
    val streakDays: Int = 4,
    val checkedInToday: Boolean = false,
    val lastProductiveDate: String = StreakDateUtils.daysAgoDateString(1),
    val todayFoodInputs: Int = 0,
    val todayExerciseInputs: Int = 0,
    val lastCheckInTrigger: String = "Unverified today — log any food or exercise data to check in",
    val productiveHistoryDates: List<String> = StreakDateUtils.defaultHistoryDates(),
    val activeBuffs: List<String> = listOf("Well-Fed (+50 XP)", "Hydrated", "Hypertrophy Pump"),
    val activeDebuffs: List<String> = emptyList()
)

data class VitalityStats(
    val caloriesLogged: Int = 2000,
    val caloriesTarget: Int = 2588,
    val proteinG: Double = 112.0,
    val carbsG: Double = 240.0,
    val fatsG: Double = 48.0,
    val fiberG: Double = 22.0,
    val omega3Mg: Double = 1900.0,
    val targetMet: Boolean = false
)

data class StaminaStats(
    val waterLiters: Double = 4.0,
    val targetLiters: Double = 4.0,
    val targetMet: Boolean = true
)

data class ExerciseLogged(
    val exerciseName: String = "",
    val setsCompleted: Int = 3,
    val repsPerSet: String = "10",
    val progressiveOverloadHit: Boolean = true
)

data class StrengthStats(
    val workoutCompleted: Boolean = true,
    val exercisesLogged: List<ExerciseLogged> = listOf(
        ExerciseLogged("Lateral Raise", 3, "15", true),
        ExerciseLogged("Declined Pushups", 3, "10", true),
        ExerciseLogged("Table Rows", 3, "10", true)
    )
)

data class StatBreakdown(
    val vitality: VitalityStats = VitalityStats(),
    val stamina: StaminaStats = StaminaStats(),
    val strength: StrengthStats = StrengthStats()
)

data class QuestItem(
    val questName: String = "",
    val status: String = "IN_PROGRESS", // COMPLETED, IN_PROGRESS, FAILED
    val xpAwarded: Int = 100
)

data class AchievementItem(
    val title: String = "",
    val description: String = "",
    val rarity: String = "RARE" // COMMON, RARE, EPIC, LEGENDARY
)

data class HunterSystemState(
    val playerStatus: PlayerStatus = PlayerStatus(),
    val statBreakdown: StatBreakdown = StatBreakdown(),
    val questLog: List<QuestItem> = listOf(
        QuestItem("Hit 110g Protein", "COMPLETED", 100),
        QuestItem("Upper Body Progression Routine", "COMPLETED", 250),
        QuestItem("Drink 3.5L+ Water", "COMPLETED", 70),
        QuestItem("Clear 2,588 kcal Surplus Target (2,000 / 2,588 logged)", "IN_PROGRESS", 120),
        QuestItem("Daily Productive Streak Check-In (Input Food or Exercise)", "IN_PROGRESS", 50)
    ),
    val unlockedAchievements: List<AchievementItem> = listOf(
        AchievementItem(
            title = "Iron Foundation",
            description = "Completed your first week of progressive overload without skipping a set.",
            rarity = "RARE"
        ),
        AchievementItem(
            title = "Caloric Vanguard",
            description = "Logged 2,000 kcal in a single tactical cycle to fuel myofibrillar synthesis.",
            rarity = "EPIC"
        )
    ),
    val tacticalBriefing: String = "System online. Logging in alone does not count as a productive day — input any food or exercise data to verify today's check-in and extend your 4-day streak to 5 days!",
    val suggestedActions: List<String> = listOf(
        "LOG FINAL +588 KCAL MEAL",
        "LOG TOMORROW'S MEAL PREP",
        "REST & RECOVER",
        "VIEW SKILL TREE"
    ),
    val biometrics: HunterBiometrics = HunterBiometrics()
) {
    /**
     * Evaluates the daily streak when the user logs in or opens the app.
     * CRITICAL RULE: Logging into the app NEVER counts as a productive day and NEVER marks checkedInToday = true.
     * A user is ONLY checked in when they actively input food or exercise data via applyProductiveInput().
     */
    fun evaluateStreakOnLogin(
        todayDate: String = StreakDateUtils.todayDateString(),
        yesterdayDate: String = StreakDateUtils.daysAgoDateString(1)
    ): HunterSystemState {
        val ps = playerStatus
        return if (ps.lastProductiveDate == todayDate && ps.checkedInToday) {
            // Already verified via food or exercise input earlier today
            this
        } else if (ps.lastProductiveDate == yesterdayDate || ps.lastProductiveDate.isBlank()) {
            // Yesterday was productive, so streak is preserved but today is UNVERIFIED until food/exercise is logged
            copy(
                playerStatus = ps.copy(
                    checkedInToday = false,
                    todayFoodInputs = 0,
                    todayExerciseInputs = 0,
                    lastCheckInTrigger = "Unverified today — log any food or exercise data to check in"
                )
            )
        } else {
            // Missed a full calendar day without logging food or exercise -> streak resets to 0
            val debuffLabel = "Streak Reset (Missed Productive Day)"
            val updatedDebuffs = if (ps.activeDebuffs.contains(debuffLabel)) {
                ps.activeDebuffs
            } else {
                ps.activeDebuffs + debuffLabel
            }
            copy(
                playerStatus = ps.copy(
                    streakDays = 0,
                    checkedInToday = false,
                    todayFoodInputs = 0,
                    todayExerciseInputs = 0,
                    lastCheckInTrigger = "Streak reset — input food or exercise data to start Day 1",
                    activeDebuffs = updatedDebuffs
                )
            )
        }
    }

    /**
     * Called ONLY when the user inputs actual food/calorie data or exercise/workout data.
     * Returns Pair(updatedHunterSystemState, newlyCheckedInToday).
     */
    fun applyProductiveInput(
        isFoodInput: Boolean,
        inputSummary: String,
        todayDate: String = StreakDateUtils.todayDateString()
    ): Pair<HunterSystemState, Boolean> {
        val ps = playerStatus
        val newFoodCount = if (isFoodInput) ps.todayFoodInputs + 1 else ps.todayFoodInputs
        val newExerciseCount = if (!isFoodInput) ps.todayExerciseInputs + 1 else ps.todayExerciseInputs
        val categoryLabel = if (isFoodInput) "Food Input" else "Exercise Input"

        val alreadyCheckedInToday = ps.checkedInToday && ps.lastProductiveDate == todayDate
        val newStreakDays = if (alreadyCheckedInToday) ps.streakDays else ps.streakDays + 1
        val updatedHistory = (ps.productiveHistoryDates + todayDate).distinct().takeLast(14)

        val streakBuffPrefix = "Productive Streak"
        val cleanedBuffs = ps.activeBuffs.filterNot { it.startsWith(streakBuffPrefix) }
        val updatedBuffs = cleanedBuffs + "$streakBuffPrefix (${newStreakDays}D Checked In)"
        val cleanedDebuffs = ps.activeDebuffs.filterNot { it.contains("Streak Reset", ignoreCase = true) }

        val updatedQuests = questLog.map { q ->
            if (q.questName.contains("Streak Check-In", ignoreCase = true)) {
                q.copy(
                    questName = "Daily Productive Streak Check-In (Verified: $categoryLabel)",
                    status = "COMPLETED"
                )
            } else {
                q
            }
        }.let { list ->
            if (list.none { it.questName.contains("Streak Check-In", ignoreCase = true) }) {
                list + QuestItem(
                    questName = "Daily Productive Streak Check-In (Verified: $categoryLabel)",
                    status = "COMPLETED",
                    xpAwarded = 50
                )
            } else {
                list
            }
        }

        val hasStreakAchievement = unlockedAchievements.any { it.title == "Productive Discipline" }
        val updatedAchievements = if (!alreadyCheckedInToday && !hasStreakAchievement) {
            unlockedAchievements + AchievementItem(
                title = "Productive Discipline",
                description = "Checked in for your daily streak by logging real food or exercise data rather than just opening the app.",
                rarity = "EPIC"
            )
        } else {
            unlockedAchievements
        }

        val bonusXp = if (!alreadyCheckedInToday) 50 else 0
        val rawXp = ps.currentXp + bonusXp
        val leveledUp = rawXp >= ps.nextLevelXp
        val newLevel = if (leveledUp) ps.level + 1 else ps.level
        val finalXp = if (leveledUp) rawXp - ps.nextLevelXp else rawXp
        val progress = ((finalXp.toDouble() / ps.nextLevelXp.toDouble()) * 100.0).coerceIn(0.0, 100.0)

        val updatedState = copy(
            playerStatus = ps.copy(
                level = newLevel,
                rank = if (newLevel >= 3) "D-Rank Vanguard" else ps.rank,
                currentXp = finalXp,
                progressPercent = progress,
                streakDays = newStreakDays,
                checkedInToday = true,
                lastProductiveDate = todayDate,
                todayFoodInputs = newFoodCount,
                todayExerciseInputs = newExerciseCount,
                lastCheckInTrigger = "Checked in via $categoryLabel: $inputSummary",
                productiveHistoryDates = updatedHistory,
                activeBuffs = updatedBuffs,
                activeDebuffs = cleanedDebuffs
            ),
            questLog = updatedQuests,
            unlockedAchievements = updatedAchievements
        )

        return Pair(updatedState, !alreadyCheckedInToday)
    }

    fun toStateJson(): String {
        val root = JSONObject()
        root.put("active_buffs", JSONArray(playerStatus.activeBuffs))
        root.put("active_debuffs", JSONArray(playerStatus.activeDebuffs))
        root.put("checked_in_today", playerStatus.checkedInToday)
        root.put("last_productive_date", playerStatus.lastProductiveDate)
        root.put("today_food_inputs", playerStatus.todayFoodInputs)
        root.put("today_exercise_inputs", playerStatus.todayExerciseInputs)
        root.put("last_check_in_trigger", playerStatus.lastCheckInTrigger)
        root.put("productive_history_dates", JSONArray(playerStatus.productiveHistoryDates))

        val exercisesArr = JSONArray()
        statBreakdown.strength.exercisesLogged.forEach { ex ->
            exercisesArr.put(
                JSONObject().apply {
                    put("exercise_name", ex.exerciseName)
                    put("sets_completed", ex.setsCompleted)
                    put("reps_per_set", ex.repsPerSet)
                    put("progressive_overload_hit", ex.progressiveOverloadHit)
                }
            )
        }
        root.put("exercises_logged", exercisesArr)

        val questsArr = JSONArray()
        questLog.forEach { q ->
            questsArr.put(
                JSONObject().apply {
                    put("quest_name", q.questName)
                    put("status", q.status)
                    put("xp_awarded", q.xpAwarded)
                }
            )
        }
        root.put("quest_log", questsArr)

        val achArr = JSONArray()
        unlockedAchievements.forEach { a ->
            achArr.put(
                JSONObject().apply {
                    put("title", a.title)
                    put("description", a.description)
                    put("rarity", a.rarity)
                }
            )
        }
        root.put("unlocked_achievements", achArr)
        root.put("suggested_actions", JSONArray(suggestedActions))
        root.put("biometrics", biometrics.toJsonObject())
        return root.toString()
    }

    fun toRawSystemJson(): String {
        val root = JSONObject()
        root.put(
            "player_status",
            JSONObject().apply {
                put("level", playerStatus.level)
                put("rank", playerStatus.rank)
                put("current_xp", playerStatus.currentXp)
                put("next_level_xp", playerStatus.nextLevelXp)
                put("progress_percent", playerStatus.progressPercent)
                put("hp_percent", playerStatus.hpPercent)
                put("streak_days", playerStatus.streakDays)
                put("checked_in_today", playerStatus.checkedInToday)
                put("last_productive_date", playerStatus.lastProductiveDate)
                put("today_food_inputs", playerStatus.todayFoodInputs)
                put("today_exercise_inputs", playerStatus.todayExerciseInputs)
                put("last_check_in_trigger", playerStatus.lastCheckInTrigger)
                put("productive_history_dates", JSONArray(playerStatus.productiveHistoryDates))
                put("active_buffs", JSONArray(playerStatus.activeBuffs))
                put("active_debuffs", JSONArray(playerStatus.activeDebuffs))
            }
        )
        root.put("hunter_biometrics", biometrics.toJsonObject())
        root.put(
            "stat_breakdown",
            JSONObject().apply {
                put(
                    "vitality",
                    JSONObject().apply {
                        put("calories_logged", statBreakdown.vitality.caloriesLogged)
                        put("calories_target", statBreakdown.vitality.caloriesTarget)
                        put("protein_g", statBreakdown.vitality.proteinG)
                        put("carbs_g", statBreakdown.vitality.carbsG)
                        put("fats_g", statBreakdown.vitality.fatsG)
                        put("fiber_g", statBreakdown.vitality.fiberG)
                        put("omega3_mg", statBreakdown.vitality.omega3Mg)
                        put("target_met", statBreakdown.vitality.targetMet)
                    }
                )
                put(
                    "stamina",
                    JSONObject().apply {
                        put("water_liters", statBreakdown.stamina.waterLiters)
                        put("target_liters", statBreakdown.stamina.targetLiters)
                        put("target_met", statBreakdown.stamina.targetMet)
                    }
                )
                put(
                    "strength",
                    JSONObject().apply {
                        put("workout_completed", statBreakdown.strength.workoutCompleted)
                        val exArr = JSONArray()
                        statBreakdown.strength.exercisesLogged.forEach { ex ->
                            exArr.put(
                                JSONObject().apply {
                                    put("exercise_name", ex.exerciseName)
                                    put("sets_completed", ex.setsCompleted)
                                    put("reps_per_set", ex.repsPerSet)
                                    put("progressive_overload_hit", ex.progressiveOverloadHit)
                                }
                            )
                        }
                        put("exercises_logged", exArr)
                    }
                )
            }
        )
        val qArr = JSONArray()
        questLog.forEach { q ->
            qArr.put(
                JSONObject().apply {
                    put("quest_name", q.questName)
                    put("status", q.status)
                    put("xp_awarded", q.xpAwarded)
                }
            )
        }
        root.put("quest_log", qArr)

        val aArr = JSONArray()
        unlockedAchievements.forEach { a ->
            aArr.put(
                JSONObject().apply {
                    put("title", a.title)
                    put("description", a.description)
                    put("rarity", a.rarity)
                }
            )
        }
        root.put("unlocked_achievements", aArr)
        root.put("tactical_briefing", tacticalBriefing)
        root.put("suggested_actions", JSONArray(suggestedActions))
        return root.toString(2)
    }

    companion object {
        fun fromGeminiJson(
            jsonString: String,
            existingBiometrics: HunterBiometrics = HunterBiometrics(),
            existingPlayerStatus: PlayerStatus = PlayerStatus()
        ): HunterSystemState {
            val root = JSONObject(jsonString)
            val psObj = root.getJSONObject("player_status")
            val buffsArr = psObj.optJSONArray("active_buffs") ?: JSONArray()
            val buffs = (0 until buffsArr.length()).map { buffsArr.getString(it) }
            val debuffsArr = psObj.optJSONArray("active_debuffs") ?: JSONArray()
            val debuffs = (0 until debuffsArr.length()).map { debuffsArr.getString(it) }

            val playerStatus = PlayerStatus(
                level = psObj.optInt("level", 2).coerceIn(1, 999),
                rank = psObj.optString("rank", "E-Rank Hunter").take(80).ifBlank { "E-Rank Hunter" },
                currentXp = psObj.optInt("current_xp", 620).coerceIn(0, 10000000),
                nextLevelXp = psObj.optInt("next_level_xp", 1000).coerceIn(100, 10000000),
                progressPercent = psObj.optDouble("progress_percent", 62.0).coerceIn(0.0, 100.0),
                hpPercent = psObj.optDouble("hp_percent", 100.0).coerceIn(0.0, 100.0),
                streakDays = existingPlayerStatus.streakDays,
                checkedInToday = existingPlayerStatus.checkedInToday,
                lastProductiveDate = existingPlayerStatus.lastProductiveDate,
                todayFoodInputs = existingPlayerStatus.todayFoodInputs,
                todayExerciseInputs = existingPlayerStatus.todayExerciseInputs,
                lastCheckInTrigger = existingPlayerStatus.lastCheckInTrigger,
                productiveHistoryDates = existingPlayerStatus.productiveHistoryDates,
                activeBuffs = buffs,
                activeDebuffs = debuffs
            )

            val sbObj = root.getJSONObject("stat_breakdown")
            val vitObj = sbObj.getJSONObject("vitality")
            val vitality = VitalityStats(
                caloriesLogged = vitObj.optInt("calories_logged", 2000).coerceIn(0, 50000),
                caloriesTarget = vitObj.optInt("calories_target", 2588).coerceIn(500, 50000),
                proteinG = vitObj.optDouble("protein_g", 112.0).coerceIn(0.0, 2000.0),
                carbsG = vitObj.optDouble("carbs_g", 240.0).coerceIn(0.0, 5000.0),
                fatsG = vitObj.optDouble("fats_g", 48.0).coerceIn(0.0, 2000.0),
                fiberG = vitObj.optDouble("fiber_g", 22.0).coerceIn(0.0, 500.0),
                omega3Mg = vitObj.optDouble("omega3_mg", 1900.0).coerceIn(0.0, 50000.0),
                targetMet = vitObj.optBoolean("target_met", false)
            )

            val stamObj = sbObj.getJSONObject("stamina")
            val stamina = StaminaStats(
                waterLiters = stamObj.optDouble("water_liters", 4.0).coerceIn(0.0, 50.0),
                targetLiters = stamObj.optDouble("target_liters", 4.0).coerceIn(0.5, 50.0),
                targetMet = stamObj.optBoolean("target_met", true)
            )

            val strObj = sbObj.getJSONObject("strength")
            val exArr = strObj.optJSONArray("exercises_logged") ?: JSONArray()
            val exercises = (0 until exArr.length()).map { idx ->
                val item = exArr.getJSONObject(idx)
                ExerciseLogged(
                    exerciseName = item.optString("exercise_name", "Exercise"),
                    setsCompleted = item.optInt("sets_completed", 3),
                    repsPerSet = item.optString("reps_per_set", "10"),
                    progressiveOverloadHit = item.optBoolean("progressive_overload_hit", true)
                )
            }
            val strength = StrengthStats(
                workoutCompleted = strObj.optBoolean("workout_completed", true),
                exercisesLogged = exercises
            )

            val qArr = root.optJSONArray("quest_log") ?: JSONArray()
            val quests = (0 until qArr.length()).map { idx ->
                val q = qArr.getJSONObject(idx)
                QuestItem(
                    questName = q.optString("quest_name", "Daily Quest"),
                    status = q.optString("status", "IN_PROGRESS"),
                    xpAwarded = q.optInt("xp_awarded", 50)
                )
            }

            val aArr = root.optJSONArray("unlocked_achievements") ?: JSONArray()
            val achievements = (0 until aArr.length()).map { idx ->
                val a = aArr.getJSONObject(idx)
                AchievementItem(
                    title = a.optString("title", "Achievement"),
                    description = a.optString("description", ""),
                    rarity = a.optString("rarity", "RARE")
                )
            }

            val briefing = root.optString("tactical_briefing", "Session updated.").take(1900).ifBlank { "Session updated." }
            val sugArr = root.optJSONArray("suggested_actions") ?: JSONArray()
            val actions = (0 until sugArr.length()).map { sugArr.getString(it) }

            return HunterSystemState(
                playerStatus = playerStatus,
                statBreakdown = StatBreakdown(vitality, stamina, strength),
                questLog = quests,
                unlockedAchievements = achievements,
                tacticalBriefing = briefing,
                suggestedActions = actions,
                biometrics = existingBiometrics
            )
        }

        fun createResetState(
            existingBiometrics: HunterBiometrics = HunterBiometrics(),
            caloriesTarget: Int = existingBiometrics.recommendedCaloriesTarget,
            waterTargetLiters: Double = existingBiometrics.recommendedWaterLiters
        ): HunterSystemState {
            val rankLabel = when (existingBiometrics.experienceLevel.uppercase()) {
                "PROFESSIONAL" -> "Pro Gym Leader"
                "INTERMEDIATE" -> "Thunder Challenger"
                else -> "PikaFit Rookie"
            }
            val proTarget = existingBiometrics.recommendedProteinG
            val safeCal = caloriesTarget.coerceIn(1000, 15000)
            val safeWater = waterTargetLiters.coerceIn(1.0, 20.0)
            return HunterSystemState(
                playerStatus = PlayerStatus(
                    level = 1,
                    rank = rankLabel,
                    currentXp = 0,
                    nextLevelXp = 1000,
                    progressPercent = 0.0,
                    hpPercent = 100.0,
                    streakDays = 0,
                    checkedInToday = false,
                    lastProductiveDate = "",
                    todayFoodInputs = 0,
                    todayExerciseInputs = 0,
                    lastCheckInTrigger = "Fresh calibration — log any meal or exercise set to start Day 1!",
                    productiveHistoryDates = emptyList(),
                    activeBuffs = listOf("PikaFit Charged", existingBiometrics.goalDisplayLabel),
                    activeDebuffs = emptyList()
                ),
                statBreakdown = StatBreakdown(
                    vitality = VitalityStats(
                        caloriesLogged = 0,
                        caloriesTarget = safeCal,
                        proteinG = 0.0,
                        carbsG = 0.0,
                        fatsG = 0.0,
                        fiberG = 0.0,
                        omega3Mg = 0.0,
                        targetMet = false
                    ),
                    stamina = StaminaStats(
                        waterLiters = 0.0,
                        targetLiters = safeWater,
                        targetMet = false
                    ),
                    strength = StrengthStats(
                        workoutCompleted = false,
                        exercisesLogged = emptyList()
                    )
                ),
                questLog = listOf(
                    QuestItem("Hit ${proTarget}g Protein (${existingBiometrics.goalDisplayLabel})", "IN_PROGRESS", 100),
                    QuestItem("Complete First Progressive Overload Workout", "IN_PROGRESS", 250),
                    QuestItem("Drink ${safeWater}L Water", "IN_PROGRESS", 70),
                    QuestItem("Clear $safeCal kcal Target (0 / $safeCal logged)", "IN_PROGRESS", 120),
                    QuestItem("Daily Productive Streak Check-In (Input Food or Exercise)", "IN_PROGRESS", 50)
                ),
                unlockedAchievements = emptyList(),
                tacticalBriefing = "Pika-Pika! Welcome to PikaFit, Trainer ${existingBiometrics.hunterName}! Your profile is calibrated for ${existingBiometrics.goalDisplayLabel} (${existingBiometrics.experienceDisplayLabel}) with a $safeCal kcal & ${safeWater}L daily target. Log your first meal or workout set to start Day 1 of your streak!",
                suggestedActions = listOf(
                    "LOG FIRST MEAL",
                    "LOG FIRST WORKOUT SET",
                    "DRINK 500ML WATER"
                ),
                biometrics = existingBiometrics
            )
        }
    }
}

@IgnoreExtraProperties
data class HunterSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val level: Long = 2L,
    val rank: String = "E-Rank Hunter",
    val currentXp: Long = 620L,
    val nextLevelXp: Long = 1000L,
    val progressPercent: Double = 62.0,
    val hpPercent: Double = 100.0,
    val streakDays: Long = 4L,
    val caloriesLogged: Long = 2000L,
    val caloriesTarget: Long = 2588L,
    val proteinG: Double = 112.0,
    val carbsG: Double = 240.0,
    val fatsG: Double = 48.0,
    val fiberG: Double = 22.0,
    val omega3Mg: Double = 1900.0,
    val waterLiters: Double = 4.0,
    val targetLiters: Double = 4.0,
    val workoutCompleted: Boolean = true,
    val tacticalBriefing: String = "2,000 kcal intake registered! Keep pushing toward your 2,588 kcal target.",
    val stateJson: String = "{}",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toHunterSystemState(): HunterSystemState {
        val defaultState = HunterSystemState()
        val parsedJson = try {
            JSONObject(stateJson.ifBlank { "{}" })
        } catch (e: Exception) {
            JSONObject()
        }

        val buffsArr = parsedJson.optJSONArray("active_buffs")
        val buffs = if (buffsArr != null) {
            (0 until buffsArr.length()).map { buffsArr.getString(it) }
        } else {
            defaultState.playerStatus.activeBuffs
        }

        val debuffsArr = parsedJson.optJSONArray("active_debuffs")
        val debuffs = if (debuffsArr != null) {
            (0 until debuffsArr.length()).map { debuffsArr.getString(it) }
        } else {
            defaultState.playerStatus.activeDebuffs
        }

        val checkedInToday = parsedJson.optBoolean("checked_in_today", false)
        val lastProductiveDate = parsedJson.optString(
            "last_productive_date",
            defaultState.playerStatus.lastProductiveDate
        )
        val todayFoodInputs = parsedJson.optInt("today_food_inputs", 0)
        val todayExerciseInputs = parsedJson.optInt("today_exercise_inputs", 0)
        val lastCheckInTrigger = parsedJson.optString(
            "last_check_in_trigger",
            defaultState.playerStatus.lastCheckInTrigger
        )
        val historyArr = parsedJson.optJSONArray("productive_history_dates")
        val historyDates = if (historyArr != null) {
            (0 until historyArr.length()).map { historyArr.getString(it) }
        } else {
            defaultState.playerStatus.productiveHistoryDates
        }

        val exArr = parsedJson.optJSONArray("exercises_logged")
        val exercises = if (exArr != null) {
            (0 until exArr.length()).map { idx ->
                val item = exArr.getJSONObject(idx)
                ExerciseLogged(
                    exerciseName = item.optString("exercise_name", "Exercise"),
                    setsCompleted = item.optInt("sets_completed", 3),
                    repsPerSet = item.optString("reps_per_set", "10"),
                    progressiveOverloadHit = item.optBoolean("progressive_overload_hit", true)
                )
            }
        } else {
            defaultState.statBreakdown.strength.exercisesLogged
        }

        val qArr = parsedJson.optJSONArray("quest_log")
        val quests = if (qArr != null) {
            (0 until qArr.length()).map { idx ->
                val q = qArr.getJSONObject(idx)
                QuestItem(
                    questName = q.optString("quest_name", "Quest"),
                    status = q.optString("status", "IN_PROGRESS"),
                    xpAwarded = q.optInt("xp_awarded", 100)
                )
            }
        } else {
            defaultState.questLog
        }

        val aArr = parsedJson.optJSONArray("unlocked_achievements")
        val achievements = if (aArr != null) {
            (0 until aArr.length()).map { idx ->
                val a = aArr.getJSONObject(idx)
                AchievementItem(
                    title = a.optString("title", "Achievement"),
                    description = a.optString("description", ""),
                    rarity = a.optString("rarity", "RARE")
                )
            }
        } else {
            defaultState.unlockedAchievements
        }

        val sugArr = parsedJson.optJSONArray("suggested_actions")
        val actions = if (sugArr != null) {
            (0 until sugArr.length()).map { sugArr.getString(it) }
        } else {
            defaultState.suggestedActions
        }

        val biometrics = HunterBiometrics.fromJsonObject(parsedJson.optJSONObject("biometrics"))

        return HunterSystemState(
            playerStatus = PlayerStatus(
                level = level.toInt(),
                rank = rank,
                currentXp = currentXp.toInt(),
                nextLevelXp = nextLevelXp.toInt(),
                progressPercent = progressPercent,
                hpPercent = hpPercent,
                streakDays = streakDays.toInt(),
                checkedInToday = checkedInToday,
                lastProductiveDate = lastProductiveDate,
                todayFoodInputs = todayFoodInputs,
                todayExerciseInputs = todayExerciseInputs,
                lastCheckInTrigger = lastCheckInTrigger,
                productiveHistoryDates = historyDates,
                activeBuffs = buffs,
                activeDebuffs = debuffs
            ),
            statBreakdown = StatBreakdown(
                vitality = VitalityStats(
                    caloriesLogged = caloriesLogged.toInt(),
                    caloriesTarget = caloriesTarget.toInt(),
                    proteinG = proteinG,
                    carbsG = carbsG,
                    fatsG = fatsG,
                    fiberG = fiberG,
                    omega3Mg = omega3Mg,
                    targetMet = caloriesLogged >= caloriesTarget && proteinG >= 110.0
                ),
                stamina = StaminaStats(
                    waterLiters = waterLiters,
                    targetLiters = targetLiters,
                    targetMet = waterLiters >= targetLiters
                ),
                strength = StrengthStats(
                    workoutCompleted = workoutCompleted,
                    exercisesLogged = exercises
                )
            ),
            questLog = quests,
            unlockedAchievements = achievements,
            tacticalBriefing = tacticalBriefing,
            suggestedActions = actions,
            biometrics = biometrics
        )
    }

    companion object {
        fun fromSystemState(
            id: String,
            userId: String,
            state: HunterSystemState,
            existingCreatedAt: Timestamp? = null
        ): HunterSession {
            return HunterSession(
                id = id,
                userId = userId,
                level = state.playerStatus.level.toLong().coerceIn(1L, 999L),
                rank = state.playerStatus.rank.take(80).ifBlank { "E-Rank Hunter" },
                currentXp = state.playerStatus.currentXp.toLong().coerceIn(0L, 10000000L),
                nextLevelXp = state.playerStatus.nextLevelXp.toLong().coerceIn(100L, 10000000L),
                progressPercent = state.playerStatus.progressPercent.coerceIn(0.0, 100.0),
                hpPercent = state.playerStatus.hpPercent.coerceIn(0.0, 100.0),
                streakDays = state.playerStatus.streakDays.toLong().coerceIn(0L, 10000L),
                caloriesLogged = state.statBreakdown.vitality.caloriesLogged.toLong().coerceIn(0L, 50000L),
                caloriesTarget = state.statBreakdown.vitality.caloriesTarget.toLong().coerceIn(500L, 50000L),
                proteinG = state.statBreakdown.vitality.proteinG.coerceIn(0.0, 2000.0),
                carbsG = state.statBreakdown.vitality.carbsG.coerceIn(0.0, 5000.0),
                fatsG = state.statBreakdown.vitality.fatsG.coerceIn(0.0, 2000.0),
                fiberG = state.statBreakdown.vitality.fiberG.coerceIn(0.0, 500.0),
                omega3Mg = state.statBreakdown.vitality.omega3Mg.coerceIn(0.0, 50000.0),
                waterLiters = state.statBreakdown.stamina.waterLiters.coerceIn(0.0, 50.0),
                targetLiters = state.statBreakdown.stamina.targetLiters.coerceIn(0.5, 50.0),
                workoutCompleted = state.statBreakdown.strength.workoutCompleted,
                tacticalBriefing = state.tacticalBriefing.take(1900).ifBlank { "Tactical briefing ready." },
                stateJson = state.toStateJson().take(24000),
                createdAt = existingCreatedAt,
                updatedAt = null
            )
        }
    }
}

@IgnoreExtraProperties
data class TacticalLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val commandText: String = "",
    val briefingResponse: String = "",
    val xpDelta: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FeedbackEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hunterName: String = "Shadow Vanguard",
    val category: String = "FEATURE_REQUEST", // FEATURE_REQUEST, BUG_REPORT, UI_UX, GENERAL
    val rating: Long = 5L,
    val message: String = "",
    val status: String = "OPEN", // OPEN, IN_REVIEW, IMPLEMENTED
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null
)

