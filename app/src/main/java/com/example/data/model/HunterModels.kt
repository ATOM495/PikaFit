package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R
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

data class TrainerRankTier(
    val tierLevel: Int,
    val classTitle: String,
    val minXp: Int,
    val maxXp: Int?, // null for 10,000+ (Champion)
    val xpRangeLabel: String,
    val pokemonName: String,
    val pokemonSubtitle: String,
    @DrawableRes val pokemonDrawableRes: Int,
    val xpMultiplier: Double,
    val dailyCompletionXp: Int,
    val xpPaceLabel: String,
    val achievementTitle: String,
    val rewardTitle: String,
    val rewardDescription: String,
    val rarity: String
)

object TrainerRankProgression {
    val tiers: List<TrainerRankTier> = listOf(
        TrainerRankTier(
            tierLevel = 1,
            classTitle = "Beginner",
            minXp = 0,
            maxXp = 1500,
            xpRangeLabel = "0 – 1,500 XP",
            pokemonName = "Pikachu",
            pokemonSubtitle = "Thunder Spark Partner",
            pokemonDrawableRes = R.drawable.rank_1_beginner_pikachu,
            xpMultiplier = 1.60,
            dailyCompletionXp = 160,
            xpPaceLabel = "1.6x Early-Stage Boost (Fast XP Gain)",
            achievementTitle = "Class Achieved: Beginner (Pikachu)",
            rewardTitle = "Pikachu Spark Badge & +60% Rookie XP Surge",
            rewardDescription = "Unlocked at 0–1,500 XP. Grants Pikachu Guardian Emblem & 1.6x accelerated XP gain for daily completions.",
            rarity = "COMMON"
        ),
        TrainerRankTier(
            tierLevel = 2,
            classTitle = "Challenger",
            minXp = 1500,
            maxXp = 3000,
            xpRangeLabel = "1,500 – 3,000 XP",
            pokemonName = "Charizard",
            pokemonSubtitle = "Sky Flame Vanguard",
            pokemonDrawableRes = R.drawable.rank_2_challenger_charizard,
            xpMultiplier = 1.30,
            dailyCompletionXp = 125,
            xpPaceLabel = "1.3x Challenger Momentum Boost",
            achievementTitle = "Class Achieved: Challenger (Charizard)",
            rewardTitle = "Charizard Flame Crest & Challenger Armor",
            rewardDescription = "Unlocked at 1,500 XP. Unlocks Charizard Guardian Emblem, Flame Wing Buff, and Official Challenger Class Title.",
            rarity = "RARE"
        ),
        TrainerRankTier(
            tierLevel = 3,
            classTitle = "Leader",
            minXp = 3000,
            maxXp = 5000,
            xpRangeLabel = "3,000 – 5,000 XP",
            pokemonName = "Greninja",
            pokemonSubtitle = "Water Shuriken Tactician",
            pokemonDrawableRes = R.drawable.rank_3_leader_greninja,
            xpMultiplier = 1.00,
            dailyCompletionXp = 95,
            xpPaceLabel = "1.0x Standard Gym Leader Pace",
            achievementTitle = "Class Achieved: Leader (Greninja)",
            rewardTitle = "Greninja Shuriken Seal & Gym Leader Crown",
            rewardDescription = "Unlocked at 3,000 XP. Unlocks Greninja Guardian Emblem, Shinobi Focus Buff, and Official Leader Class Title.",
            rarity = "RARE"
        ),
        TrainerRankTier(
            tierLevel = 4,
            classTitle = "Elite I",
            minXp = 5000,
            maxXp = 7500,
            xpRangeLabel = "5,000 – 7,500 XP",
            pokemonName = "Mega Lucario",
            pokemonSubtitle = "Aura Sphere Master",
            pokemonDrawableRes = R.drawable.rank_4_elite1_lucario,
            xpMultiplier = 0.80,
            dailyCompletionXp = 75,
            xpPaceLabel = "0.80x Elite I Tempering Pace (Slowed XP)",
            achievementTitle = "Class Achieved: Elite I (Mega Lucario)",
            rewardTitle = "Mega Lucario Aura Seal & Steel Will Relic",
            rewardDescription = "Unlocked at 5,000 XP. Unlocks Mega Lucario Guardian Emblem, Aura Mastery Buff, and Elite I Class Title.",
            rarity = "EPIC"
        ),
        TrainerRankTier(
            tierLevel = 5,
            classTitle = "Elite II",
            minXp = 7500,
            maxXp = 9000,
            xpRangeLabel = "7,500 – 9,000 XP",
            pokemonName = "Shadow Mewtwo",
            pokemonSubtitle = "Obsidian Psystrike Sovereign",
            pokemonDrawableRes = R.drawable.rank_5_elite2_mewtwo,
            xpMultiplier = 0.65,
            dailyCompletionXp = 60,
            xpPaceLabel = "0.65x Elite II High-Gravity Pace (Slowed XP)",
            achievementTitle = "Class Achieved: Elite II (Shadow Mewtwo)",
            rewardTitle = "Shadow Mewtwo Core & Psystrike Mantle",
            rewardDescription = "Unlocked at 7,500 XP. Unlocks Shadow Mewtwo Guardian Emblem, Obsidian Aura Frame, and Elite II Class Title.",
            rarity = "EPIC"
        ),
        TrainerRankTier(
            tierLevel = 6,
            classTitle = "Elite III",
            minXp = 9000,
            maxXp = 10000,
            xpRangeLabel = "9,000 – 10,000 XP",
            pokemonName = "Yveltal",
            pokemonSubtitle = "Crimson Oblivion Apex",
            pokemonDrawableRes = R.drawable.rank_6_elite3_yveltal,
            xpMultiplier = 0.50,
            dailyCompletionXp = 45,
            xpPaceLabel = "0.50x Elite III Crucible Pace (Slowed XP)",
            achievementTitle = "Class Achieved: Elite III (Yveltal)",
            rewardTitle = "Yveltal Oblivion Wings & Apex Predator Seal",
            rewardDescription = "Unlocked at 9,000 XP. Unlocks Yveltal Guardian Emblem, Crimson Wing Buff, and Elite III Class Title.",
            rarity = "LEGENDARY"
        ),
        TrainerRankTier(
            tierLevel = 7,
            classTitle = "Champion",
            minXp = 10000,
            maxXp = null,
            xpRangeLabel = "10,000+ XP",
            pokemonName = "Arceus",
            pokemonSubtitle = "Mythic Alpha Creator",
            pokemonDrawableRes = R.drawable.rank_7_champion_arceus,
            xpMultiplier = 0.40,
            dailyCompletionXp = 35,
            xpPaceLabel = "0.40x Mythic Champion Mastery Pace",
            achievementTitle = "Class Achieved: Champion (Arceus)",
            rewardTitle = "Arceus Alpha Halo, Sovereign Trophy & Champion Title",
            rewardDescription = "Unlocked at 10,000+ XP. Unlocks Mythic Arceus Guardian Emblem, Alpha Plates Halo, and Immortal Champion Title.",
            rarity = "LEGENDARY"
        )
    )

    fun tierForXp(totalXp: Int): TrainerRankTier {
        val safeXp = totalXp.coerceAtLeast(0)
        return when {
            safeXp >= 10000 -> tiers[6]
            safeXp >= 9000 -> tiers[5]
            safeXp >= 7500 -> tiers[4]
            safeXp >= 5000 -> tiers[3]
            safeXp >= 3000 -> tiers[2]
            safeXp >= 1500 -> tiers[1]
            else -> tiers[0]
        }
    }

    fun nextTierForXp(totalXp: Int): TrainerRankTier? {
        val current = tierForXp(totalXp)
        return tiers.getOrNull(current.tierLevel)
    }

    fun nextThresholdForXp(totalXp: Int): Int {
        val safeXp = totalXp.coerceAtLeast(0)
        val tier = tierForXp(safeXp)
        return tier.maxXp ?: (10000 + (((safeXp - 10000) / 2500 + 1) * 2500))
    }

    fun progressPercentForXp(totalXp: Int): Double {
        val safeXp = totalXp.coerceAtLeast(0)
        val tier = tierForXp(safeXp)
        val max = tier.maxXp
        val raw = if (max != null) {
            ((safeXp - tier.minXp).toDouble() / (max - tier.minXp).coerceAtLeast(1).toDouble()) * 100.0
        } else {
            (((safeXp - 10000) % 2500).toDouble() / 2500.0) * 100.0
        }
        return ((raw * 10.0).roundToInt() / 10.0).coerceIn(0.0, 100.0)
    }

    /**
     * Scales raw XP gain based on the user's current progression tier:
     * Higher XP in the beginning stage (1.6x in Beginner, 1.3x in Challenger),
     * slowing down gradually at higher ranks (0.80x -> 0.65x -> 0.50x -> 0.40x)
     * so users keep using the app consistently to earn higher ranks.
     */
    fun scaleActionXp(baseXp: Int, currentTotalXp: Int): Int {
        if (baseXp <= 0) return baseXp
        val tier = tierForXp(currentTotalXp)
        return (baseXp * tier.xpMultiplier).roundToInt().coerceAtLeast(5)
    }

    fun dailyCheckInBonusXp(currentTotalXp: Int): Int {
        return tierForXp(currentTotalXp).dailyCompletionXp
    }

    fun unlockedClassAchievementsForXp(
        totalXp: Int,
        existingAchievements: List<AchievementItem>
    ): List<AchievementItem> {
        val safeXp = totalXp.coerceAtLeast(0)
        val earnedTierAchievements = tiers
            .filter { safeXp >= it.minXp }
            .map { tier ->
                AchievementItem(
                    title = tier.achievementTitle,
                    description = "${tier.rewardTitle} — ${tier.rewardDescription}",
                    rarity = tier.rarity
                )
            }
        val existingTitles = earnedTierAchievements.map { it.title }.toSet()
        val preservedCustom = existingAchievements.filterNot { it.title in existingTitles }
        return earnedTierAchievements + preservedCustom
    }
}

data class HunterBiometrics(
    val hunterName: String = "New Trainer",
    val hunterTitle: String = "Beginner • Pikachu",
    val avatarBase64: String = "",
    val weightKg: Double = 70.0,
    val heightCm: Double = 172.0,
    val age: Int = 22,
    val biologicalSex: String = "MALE", // MALE, FEMALE
    val fitnessGoal: String = "WEIGHT_GAIN", // WEIGHT_GAIN, WEIGHT_LOSS, MAINTENANCE
    val experienceLevel: String = "BEGINNER", // BEGINNER, INTERMEDIATE, PROFESSIONAL
    val onboardingCompleted: Boolean = false,
    val targetWeightKg: Double = 75.0,
    val targetPhysique: String = "Athletic & Aesthetic",
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

    val targetWeightDeltaKg: Double
        get() = round1(targetWeightKg - weightKg)

    val targetWeightSummaryLabel: String
        get() = when {
            targetWeightKg > weightKg + 0.2 -> "Gain +${round1(targetWeightKg - weightKg)} kg to reach ${targetWeightKg} kg"
            targetWeightKg < weightKg - 0.2 -> "Lose ${round1(weightKg - targetWeightKg)} kg to reach ${targetWeightKg} kg"
            else -> "Target Weight Reached (${targetWeightKg} kg)!"
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
        put("target_weight_kg", targetWeightKg)
        put("target_physique", targetPhysique)
        put("bmi", bmi)
        put("body_fat_percent", bodyFatPercent)
        put("muscle_mass_percent", muscleMassPercent)
        put("bone_mass_kg", boneMassKg)
        put("water_mass_kg", waterMassKg)
        if (customBodyFatPercent != null) put("custom_body_fat_percent", customBodyFatPercent)
        if (customMuscleMassPercent != null) put("custom_muscle_mass_percent", customMuscleMassPercent)
    }

    companion object {
        val PHYSIQUE_OPTIONS = listOf(
            "Lean & Shredded",
            "Athletic & Aesthetic",
            "Muscular & Bulked",
            "Strength & Power",
            "Toned & Fit"
        )

        fun fromJsonObject(obj: JSONObject?): HunterBiometrics {
            if (obj == null) return HunterBiometrics()
            val customBf = if (obj.has("custom_body_fat_percent") && !obj.isNull("custom_body_fat_percent")) {
                obj.optDouble("custom_body_fat_percent").takeIf { !it.isNaN() && it > 0.0 }
            } else null
            val customMm = if (obj.has("custom_muscle_mass_percent") && !obj.isNull("custom_muscle_mass_percent")) {
                obj.optDouble("custom_muscle_mass_percent").takeIf { !it.isNaN() && it > 0.0 }
            } else null
            val parsedWeight = obj.optDouble("weight_kg", 70.0).coerceIn(20.0, 400.0)
            val parsedGoal = obj.optString("fitness_goal", "WEIGHT_GAIN").uppercase().let {
                if (it in setOf("WEIGHT_GAIN", "WEIGHT_LOSS", "MAINTENANCE")) it else "WEIGHT_GAIN"
            }
            val defaultTargetW = when (parsedGoal) {
                "WEIGHT_GAIN" -> parsedWeight + 5.0
                "WEIGHT_LOSS" -> (parsedWeight - 5.0).coerceAtLeast(40.0)
                else -> parsedWeight
            }

            return HunterBiometrics(
                hunterName = obj.optString("hunter_name", "New Trainer").take(60).ifBlank { "New Trainer" },
                hunterTitle = obj.optString("hunter_title", "Beginner • Pikachu").take(60).ifBlank { "Beginner • Pikachu" },
                avatarBase64 = obj.optString("avatar_base64", "").take(18000),
                weightKg = parsedWeight,
                heightCm = obj.optDouble("height_cm", 172.0).coerceIn(80.0, 260.0),
                age = obj.optInt("age", 22).coerceIn(10, 120),
                biologicalSex = obj.optString("biological_sex", "MALE").uppercase().let {
                    if (it == "FEMALE") "FEMALE" else "MALE"
                },
                fitnessGoal = parsedGoal,
                experienceLevel = obj.optString("experience_level", "BEGINNER").uppercase().let {
                    if (it in setOf("BEGINNER", "INTERMEDIATE", "PROFESSIONAL")) it else "BEGINNER"
                },
                onboardingCompleted = obj.optBoolean("onboarding_completed", false),
                targetWeightKg = obj.optDouble("target_weight_kg", defaultTargetW).coerceIn(25.0, 350.0),
                targetPhysique = obj.optString("target_physique", "Athletic & Aesthetic").take(60).ifBlank { "Athletic & Aesthetic" },
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
        return emptyList()
    }
}

data class DailyDateLog(
    val date: String = "",
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatsG: Double = 0.0,
    val waterLiters: Double = 0.0,
    val foodSummary: String = ""
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("date", date)
        put("calories", calories)
        put("protein_g", proteinG)
        put("carbs_g", carbsG)
        put("fats_g", fatsG)
        put("water_liters", waterLiters)
        put("food_summary", foodSummary)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject?): DailyDateLog? {
            if (obj == null) return null
            val dateStr = obj.optString("date", "").trim()
            if (dateStr.isBlank()) return null
            return DailyDateLog(
                date = dateStr,
                calories = obj.optInt("calories", 0).coerceIn(0, 50000),
                proteinG = obj.optDouble("protein_g", 0.0).coerceIn(0.0, 2000.0),
                carbsG = obj.optDouble("carbs_g", 0.0).coerceIn(0.0, 5000.0),
                fatsG = obj.optDouble("fats_g", 0.0).coerceIn(0.0, 2000.0),
                waterLiters = obj.optDouble("water_liters", 0.0).coerceIn(0.0, 50.0),
                foodSummary = obj.optString("food_summary", "").take(400)
            )
        }
    }
}

data class PlayerStatus(
    val level: Int = 1,
    val rank: String = "Beginner",
    val currentXp: Int = 0,
    val nextLevelXp: Int = 1500,
    val progressPercent: Double = 0.0,
    val hpPercent: Double = 100.0,
    val streakDays: Int = 0,
    val checkedInToday: Boolean = false,
    val lastProductiveDate: String = "",
    val lastDailyResetDate: String = "",
    val todayFoodInputs: Int = 0,
    val todayExerciseInputs: Int = 0,
    val lastCheckInTrigger: String = "No activity logged today — input food or exercise data to check in",
    val productiveHistoryDates: List<String> = emptyList(),
    val dailyLogsByDate: Map<String, DailyDateLog> = emptyMap(),
    val activeBuffs: List<String> = listOf("Pikachu (1.6x XP)"),
    val activeDebuffs: List<String> = emptyList()
) {
    val currentTier: TrainerRankTier
        get() = TrainerRankProgression.tierForXp(currentXp)

    val nextTier: TrainerRankTier?
        get() = TrainerRankProgression.nextTierForXp(currentXp)
}

data class VitalityStats(
    val caloriesLogged: Int = 0,
    val caloriesTarget: Int = 2588,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatsG: Double = 0.0,
    val fiberG: Double = 0.0,
    val omega3Mg: Double = 0.0,
    val targetMet: Boolean = false
)

data class StaminaStats(
    val waterLiters: Double = 0.0,
    val targetLiters: Double = 3.5,
    val targetMet: Boolean = false
)

data class ExerciseLogged(
    val exerciseName: String = "",
    val setsCompleted: Int = 3,
    val repsPerSet: String = "10",
    val progressiveOverloadHit: Boolean = true
)

data class StrengthStats(
    val workoutCompleted: Boolean = false,
    val exercisesLogged: List<ExerciseLogged> = emptyList()
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
        QuestItem("Hit Daily Protein Target", "IN_PROGRESS", 100),
        QuestItem("Complete Progressive Overload Workout", "IN_PROGRESS", 250),
        QuestItem("Drink 3.5L+ Water", "IN_PROGRESS", 70),
        QuestItem("Clear Daily Calorie Target", "IN_PROGRESS", 120),
        QuestItem("Daily Productive Streak Check-In (Input Food or Exercise)", "IN_PROGRESS", 50)
    ),
    val unlockedAchievements: List<AchievementItem> = TrainerRankProgression.unlockedClassAchievementsForXp(0, emptyList()),
    val tacticalBriefing: String = "System online. Input your meals, water, or workout sets to start tracking your daily progress and earn XP!",
    val suggestedActions: List<String> = listOf(
        "LOG FIRST MEAL",
        "LOG WORKOUT SET",
        "LOG WATER INTAKE"
    ),
    val biometrics: HunterBiometrics = HunterBiometrics()
) {
    /**
     * Evaluates the daily streak and resets daily nutrition/hydration/workout intake ONCE when a new calendar day starts.
     * CRITICAL RULE:
     * 1. Logging into the app NEVER counts as a productive day and NEVER marks checkedInToday = true.
     * 2. Once today's reset has been recorded (lastDailyResetDate == todayDate), subsequent state updates on the same day
     *    (such as logging water before logging food/workout) MUST NEVER reset waterLiters or daily stats back to 0!
     */
    fun evaluateStreakOnLogin(
        todayDate: String = StreakDateUtils.todayDateString(),
        yesterdayDate: String = StreakDateUtils.daysAgoDateString(1)
    ): HunterSystemState {
        val ps = playerStatus

        // Helper to reset daily food, water, workout, and quest trackers for a fresh calendar day
        fun resetDailyIntakeStats(): Pair<StatBreakdown, List<QuestItem>> {
            val calTarget = statBreakdown.vitality.caloriesTarget
            val waterTarget = statBreakdown.stamina.targetLiters
            val proTarget = biometrics.recommendedProteinG
            val freshStatBreakdown = statBreakdown.copy(
                vitality = statBreakdown.vitality.copy(
                    caloriesLogged = 0,
                    proteinG = 0.0,
                    carbsG = 0.0,
                    fatsG = 0.0,
                    fiberG = 0.0,
                    targetMet = false
                ),
                stamina = statBreakdown.stamina.copy(
                    waterLiters = 0.0,
                    targetMet = false
                ),
                strength = statBreakdown.strength.copy(
                    workoutCompleted = false,
                    exercisesLogged = emptyList()
                )
            )
            val freshQuests = questLog.map { q ->
                when {
                    q.questName.contains("Streak Check-In", ignoreCase = true) ->
                        q.copy(
                            questName = "Daily Productive Streak Check-In (Input Food or Exercise)",
                            status = "IN_PROGRESS"
                        )
                    q.questName.contains("kcal", ignoreCase = true) || q.questName.contains("Calorie", ignoreCase = true) || q.questName.contains("Surplus", ignoreCase = true) ->
                        q.copy(
                            questName = "Clear $calTarget kcal Target (0 / $calTarget logged)",
                            status = "IN_PROGRESS"
                        )
                    q.questName.contains("Water", ignoreCase = true) ->
                        q.copy(
                            questName = "Drink ${waterTarget}L Water (0.0L / ${waterTarget}L)",
                            status = "IN_PROGRESS"
                        )
                    q.questName.contains("Protein", ignoreCase = true) ->
                        q.copy(
                            questName = "Hit ${proTarget}g Protein (${biometrics.goalDisplayLabel})",
                            status = "IN_PROGRESS"
                        )
                    else -> q.copy(status = "IN_PROGRESS")
                }
            }
            return Pair(freshStatBreakdown, freshQuests)
        }

        // Archive previous day's logged calories/macros into dailyLogsByDate before rolling over
        fun archivePreviousDayIfPresent(): Map<String, DailyDateLog> {
            val prevDate = ps.lastDailyResetDate.ifBlank { ps.lastProductiveDate }.ifBlank { yesterdayDate }
            val vit = statBreakdown.vitality
            val stam = statBreakdown.stamina
            if (prevDate.isBlank() || prevDate == todayDate) return ps.dailyLogsByDate
            if (vit.caloriesLogged <= 0 && stam.waterLiters <= 0.0 && ps.dailyLogsByDate.containsKey(prevDate)) {
                return ps.dailyLogsByDate
            }
            val summaryFromTrigger = ps.lastCheckInTrigger
                .removePrefix("Checked in via Food Input:")
                .removePrefix("Checked in via Exercise Input:")
                .trim()
            val archivedEntry = DailyDateLog(
                date = prevDate,
                calories = vit.caloriesLogged,
                proteinG = vit.proteinG,
                carbsG = vit.carbsG,
                fatsG = vit.fatsG,
                waterLiters = stam.waterLiters,
                foodSummary = if (vit.caloriesLogged > 0) summaryFromTrigger else ""
            )
            return (ps.dailyLogsByDate + (prevDate to archivedEntry)).entries
                .sortedBy { it.key }
                .takeLast(14)
                .associate { it.key to it.value }
        }

        // Case 1: Already verified via food or exercise input earlier today
        if (ps.lastProductiveDate == todayDate && ps.checkedInToday) {
            val cleanedTrigger = ps.lastCheckInTrigger
                .replace(Regex("""\(\d+g\)\s*(\(\d+g\))"""), "$1")
                .replace("Poha / Upma / Pongal (Cooked)", "Poha (Flattened Rice)")
            val triggerFoodRegex = Regex("""\(\+(\d+)\s*kcal,\s*([\d.]+)g\s*P(?:,\s*([\d.]+)g\s*C,\s*([\d.]+)g\s*F)?\)""")
            val match = triggerFoodRegex.find(cleanedTrigger)
            val curVit = statBreakdown.vitality

            if (ps.todayFoodInputs == 1 && match != null && ps.lastDailyResetDate != todayDate) {
                val triggerKcal = match.groupValues[1].toIntOrNull() ?: curVit.caloriesLogged
                val triggerPro = match.groupValues[2].toDoubleOrNull() ?: curVit.proteinG
                val triggerCarbs = match.groupValues.getOrNull(3)?.toDoubleOrNull()
                val triggerFats = match.groupValues.getOrNull(4)?.toDoubleOrNull()

                if (curVit.caloriesLogged > triggerKcal) {
                    val reconciledVit = curVit.copy(
                        caloriesLogged = triggerKcal,
                        proteinG = triggerPro,
                        carbsG = triggerCarbs ?: curVit.carbsG.coerceAtMost(triggerKcal / 4.0),
                        fatsG = triggerFats ?: curVit.fatsG.coerceAtMost(triggerKcal / 9.0),
                        targetMet = triggerKcal >= (curVit.caloriesTarget * 0.90).toInt()
                    )
                    return copy(
                        playerStatus = ps.copy(
                            lastCheckInTrigger = cleanedTrigger,
                            lastDailyResetDate = todayDate
                        ),
                        statBreakdown = statBreakdown.copy(vitality = reconciledVit)
                    )
                }
            }

            return if (cleanedTrigger != ps.lastCheckInTrigger || ps.lastDailyResetDate != todayDate) {
                copy(
                    playerStatus = ps.copy(
                        lastCheckInTrigger = cleanedTrigger,
                        lastDailyResetDate = todayDate
                    )
                )
            } else {
                this
            }
        }

        // Case 2: Already performed the daily rollover reset for todayDate (e.g. user logged water today before logging food/workout)
        if (ps.lastDailyResetDate == todayDate) {
            return this
        }

        // Case 3: First evaluation on a new calendar day (or brand-new Day 1 account)
        return if (ps.lastProductiveDate.isBlank()) {
            // Brand-new Day 1 profile — do NOT wipe any water/stats logged today; just stamp lastDailyResetDate = todayDate
            copy(
                playerStatus = ps.copy(
                    checkedInToday = false,
                    lastDailyResetDate = todayDate
                )
            )
        } else if (ps.lastProductiveDate == yesterdayDate) {
            // Yesterday was productive -> archive yesterday's food log, preserve streakDays, reset daily intake stats ONCE for todayDate
            val archivedLogs = archivePreviousDayIfPresent()
            val (freshStats, freshQuests) = resetDailyIntakeStats()
            copy(
                playerStatus = ps.copy(
                    checkedInToday = false,
                    lastDailyResetDate = todayDate,
                    todayFoodInputs = 0,
                    todayExerciseInputs = 0,
                    dailyLogsByDate = archivedLogs,
                    lastCheckInTrigger = "Unverified today — log any food or exercise data to check in"
                ),
                statBreakdown = freshStats,
                questLog = freshQuests
            )
        } else {
            // Missed a full calendar day without logging food or exercise -> archive previous day, streak resets to 0 and daily intake resets ONCE for todayDate
            val archivedLogs = archivePreviousDayIfPresent()
            val debuffLabel = "Streak Reset (Missed Productive Day)"
            val updatedDebuffs = if (ps.activeDebuffs.contains(debuffLabel)) {
                ps.activeDebuffs
            } else {
                ps.activeDebuffs + debuffLabel
            }
            val (freshStats, freshQuests) = resetDailyIntakeStats()
            copy(
                playerStatus = ps.copy(
                    streakDays = 0,
                    checkedInToday = false,
                    lastDailyResetDate = todayDate,
                    todayFoodInputs = 0,
                    todayExerciseInputs = 0,
                    dailyLogsByDate = archivedLogs,
                    lastCheckInTrigger = "Streak reset — input food or exercise data to start Day 1",
                    activeDebuffs = updatedDebuffs
                ),
                statBreakdown = freshStats,
                questLog = freshQuests
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
        val baseAchievements = if (!alreadyCheckedInToday && !hasStreakAchievement) {
            unlockedAchievements + AchievementItem(
                title = "Productive Discipline",
                description = "Checked in for your daily streak by logging real food or exercise data rather than just opening the app.",
                rarity = "EPIC"
            )
        } else {
            unlockedAchievements
        }

        // Dynamic XP scaling: higher daily check-in XP in the beginning stage, slowing down as rank increases
        val bonusXp = if (!alreadyCheckedInToday) TrainerRankProgression.dailyCheckInBonusXp(ps.currentXp) else 0
        val newTotalXp = (ps.currentXp + bonusXp).coerceAtLeast(0)
        val tier = TrainerRankProgression.tierForXp(newTotalXp)
        val nextXpThreshold = TrainerRankProgression.nextThresholdForXp(newTotalXp)
        val progress = TrainerRankProgression.progressPercentForXp(newTotalXp)
        val syncedAchievements = TrainerRankProgression.unlockedClassAchievementsForXp(newTotalXp, baseAchievements)
        val autoTitle = "${tier.classTitle} • ${tier.pokemonName}"

        val updatedState = copy(
            playerStatus = ps.copy(
                level = tier.tierLevel,
                rank = tier.classTitle,
                currentXp = newTotalXp,
                nextLevelXp = nextXpThreshold,
                progressPercent = progress,
                streakDays = newStreakDays,
                checkedInToday = true,
                lastProductiveDate = todayDate,
                lastDailyResetDate = todayDate,
                todayFoodInputs = newFoodCount,
                todayExerciseInputs = newExerciseCount,
                lastCheckInTrigger = "Checked in via $categoryLabel: $inputSummary",
                productiveHistoryDates = updatedHistory,
                activeBuffs = updatedBuffs,
                activeDebuffs = cleanedDebuffs
            ),
            questLog = updatedQuests,
            unlockedAchievements = syncedAchievements,
            biometrics = biometrics.copy(hunterTitle = autoTitle)
        )

        return Pair(updatedState, !alreadyCheckedInToday)
    }

    /**
     * Applies a raw XP change using the dynamic progression curve (higher in early ranks, slower in higher ranks)
     * and automatically synchronizes the user's Class/Title, Level, Next Threshold, and Class Achievements.
     */
    fun applyScaledXpProgress(rawBaseXpDelta: Int, isRawAlreadyScaled: Boolean = false): Pair<HunterSystemState, Int> {
        val ps = playerStatus
        val effectiveDelta = if (rawBaseXpDelta > 0 && !isRawAlreadyScaled) {
            TrainerRankProgression.scaleActionXp(rawBaseXpDelta, ps.currentXp)
        } else {
            rawBaseXpDelta
        }
        val newTotalXp = (ps.currentXp + effectiveDelta).coerceAtLeast(0)
        val tier = TrainerRankProgression.tierForXp(newTotalXp)
        val nextXpThreshold = TrainerRankProgression.nextThresholdForXp(newTotalXp)
        val progress = TrainerRankProgression.progressPercentForXp(newTotalXp)
        val syncedAchievements = TrainerRankProgression.unlockedClassAchievementsForXp(newTotalXp, unlockedAchievements)
        val autoTitle = "${tier.classTitle} • ${tier.pokemonName}"

        val rewardBuff = "${tier.pokemonName} (${tier.xpMultiplier}x XP)"
        val cleanedBuffs = ps.activeBuffs.filterNot { b ->
            TrainerRankProgression.tiers.any { t -> b.startsWith(t.pokemonName) }
        }
        val updatedBuffs = listOf(rewardBuff) + cleanedBuffs

        return Pair(
            copy(
                playerStatus = ps.copy(
                    level = tier.tierLevel,
                    rank = tier.classTitle,
                    currentXp = newTotalXp,
                    nextLevelXp = nextXpThreshold,
                    progressPercent = progress,
                    activeBuffs = updatedBuffs
                ),
                unlockedAchievements = syncedAchievements,
                biometrics = biometrics.copy(hunterTitle = autoTitle)
            ),
            effectiveDelta
        )
    }

    fun toStateJson(): String {
        val root = JSONObject()
        root.put("active_buffs", JSONArray(playerStatus.activeBuffs))
        root.put("active_debuffs", JSONArray(playerStatus.activeDebuffs))
        root.put("checked_in_today", playerStatus.checkedInToday)
        root.put("last_productive_date", playerStatus.lastProductiveDate)
        root.put("last_daily_reset_date", playerStatus.lastDailyResetDate)
        root.put("today_food_inputs", playerStatus.todayFoodInputs)
        root.put("today_exercise_inputs", playerStatus.todayExerciseInputs)
        root.put("last_check_in_trigger", playerStatus.lastCheckInTrigger)
        root.put("productive_history_dates", JSONArray(playerStatus.productiveHistoryDates))
        val dailyLogsArr = JSONArray()
        playerStatus.dailyLogsByDate.values.forEach { dl ->
            dailyLogsArr.put(dl.toJsonObject())
        }
        root.put("daily_logs_by_date", dailyLogsArr)

        // Persist core stats inside stateJson as well so local sessions restored from cachedJson retain 100% of water, calories, macros, and XP
        root.put("current_xp", playerStatus.currentXp)
        root.put("streak_days", playerStatus.streakDays)
        root.put("calories_logged", statBreakdown.vitality.caloriesLogged)
        root.put("calories_target", statBreakdown.vitality.caloriesTarget)
        root.put("protein_g", statBreakdown.vitality.proteinG)
        root.put("carbs_g", statBreakdown.vitality.carbsG)
        root.put("fats_g", statBreakdown.vitality.fatsG)
        root.put("fiber_g", statBreakdown.vitality.fiberG)
        root.put("omega3_mg", statBreakdown.vitality.omega3Mg)
        root.put("water_liters", statBreakdown.stamina.waterLiters)
        root.put("target_liters", statBreakdown.stamina.targetLiters)
        root.put("workout_completed", statBreakdown.strength.workoutCompleted)
        root.put("tactical_briefing", tacticalBriefing)

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
                lastDailyResetDate = existingPlayerStatus.lastDailyResetDate,
                todayFoodInputs = existingPlayerStatus.todayFoodInputs,
                todayExerciseInputs = existingPlayerStatus.todayExerciseInputs,
                lastCheckInTrigger = existingPlayerStatus.lastCheckInTrigger,
                productiveHistoryDates = existingPlayerStatus.productiveHistoryDates,
                dailyLogsByDate = existingPlayerStatus.dailyLogsByDate,
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
            val startTier = TrainerRankProgression.tierForXp(0)
            val autoTitle = "${startTier.classTitle} • ${startTier.pokemonName}"
            val proTarget = existingBiometrics.recommendedProteinG
            val safeCal = caloriesTarget.coerceIn(1000, 15000)
            val safeWater = waterTargetLiters.coerceIn(1.0, 20.0)
            return HunterSystemState(
                playerStatus = PlayerStatus(
                    level = startTier.tierLevel,
                    rank = startTier.classTitle,
                    currentXp = 0,
                    nextLevelXp = startTier.maxXp ?: 1500,
                    progressPercent = 0.0,
                    hpPercent = 100.0,
                    streakDays = 0,
                    checkedInToday = false,
                    lastProductiveDate = "",
                    lastDailyResetDate = StreakDateUtils.todayDateString(),
                    todayFoodInputs = 0,
                    todayExerciseInputs = 0,
                    lastCheckInTrigger = "Fresh calibration — log any meal or exercise set to start Day 1!",
                    productiveHistoryDates = emptyList(),
                    activeBuffs = listOf("${startTier.pokemonName} (${startTier.xpMultiplier}x XP)", existingBiometrics.goalDisplayLabel),
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
                unlockedAchievements = TrainerRankProgression.unlockedClassAchievementsForXp(0, emptyList()),
                tacticalBriefing = "Pika-Pika! Welcome to PikaFit, Trainer ${existingBiometrics.hunterName}! You start in Class ${startTier.classTitle} (0–1,500 XP) with ${startTier.pokemonName} and a ${startTier.xpMultiplier}x early-stage XP boost! Log your first meal or workout set to earn boosted XP toward Challenger (Charizard)!",
                suggestedActions = listOf(
                    "LOG FIRST MEAL (+BOOSTED XP)",
                    "LOG FIRST WORKOUT SET",
                    "DRINK 500ML WATER"
                ),
                biometrics = existingBiometrics.copy(hunterTitle = autoTitle)
            )
        }
    }
}

@IgnoreExtraProperties
data class HunterSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val level: Long = 1L,
    val rank: String = "Beginner",
    val currentXp: Long = 0L,
    val nextLevelXp: Long = 1500L,
    val progressPercent: Double = 0.0,
    val hpPercent: Double = 100.0,
    val streakDays: Long = 0L,
    val caloriesLogged: Long = 0L,
    val caloriesTarget: Long = 2588L,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatsG: Double = 0.0,
    val fiberG: Double = 0.0,
    val omega3Mg: Double = 0.0,
    val waterLiters: Double = 0.0,
    val targetLiters: Double = 3.5,
    val workoutCompleted: Boolean = false,
    val tacticalBriefing: String = "System ready. Log your first meal, water intake, or workout set to start tracking.",
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
        val lastDailyResetDate = parsedJson.optString(
            "last_daily_reset_date",
            defaultState.playerStatus.lastDailyResetDate
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
        val dailyLogsArr = parsedJson.optJSONArray("daily_logs_by_date")
        val parsedDailyLogs = if (dailyLogsArr != null) {
            (0 until dailyLogsArr.length()).mapNotNull { idx ->
                DailyDateLog.fromJsonObject(dailyLogsArr.optJSONObject(idx))
            }.associateBy { it.date }
        } else {
            emptyMap()
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

        val resolvedCaloriesLogged = if (caloriesLogged != 0L) caloriesLogged else parsedJson.optLong("calories_logged", 0L)
        val resolvedCaloriesTarget = if (caloriesTarget != 2588L || !parsedJson.has("calories_target")) {
            if (caloriesTarget != 2588L) caloriesTarget else biometrics.recommendedCaloriesTarget.toLong()
        } else {
            parsedJson.optLong("calories_target", biometrics.recommendedCaloriesTarget.toLong())
        }
        val resolvedProteinG = if (proteinG != 0.0) proteinG else parsedJson.optDouble("protein_g", 0.0)
        val resolvedCarbsG = if (carbsG != 0.0) carbsG else parsedJson.optDouble("carbs_g", 0.0)
        val resolvedFatsG = if (fatsG != 0.0) fatsG else parsedJson.optDouble("fats_g", 0.0)
        val resolvedFiberG = if (fiberG != 0.0) fiberG else parsedJson.optDouble("fiber_g", 0.0)
        val resolvedOmega3 = if (omega3Mg != 0.0) omega3Mg else parsedJson.optDouble("omega3_mg", 0.0)
        val resolvedWaterLiters = if (waterLiters != 0.0) waterLiters else parsedJson.optDouble("water_liters", 0.0)
        val resolvedTargetLiters = if (targetLiters != 3.5 || !parsedJson.has("target_liters")) {
            if (targetLiters != 3.5) targetLiters else biometrics.recommendedWaterLiters
        } else {
            parsedJson.optDouble("target_liters", biometrics.recommendedWaterLiters)
        }
        val resolvedXp = if (currentXp != 0L) currentXp else parsedJson.optLong("current_xp", 0L)
        val resolvedStreak = if (streakDays != 0L) streakDays else parsedJson.optLong("streak_days", 0L)
        val resolvedWorkoutCompleted = workoutCompleted || parsedJson.optBoolean("workout_completed", false)
        val resolvedBriefing = parsedJson.optString("tactical_briefing", "").ifBlank { tacticalBriefing }

        val isLegacyMockExercises = todayExerciseInputs == 0 &&
            exercises.size == 3 &&
            exercises[0].exerciseName == "Lateral Raise" &&
            exercises[1].exerciseName == "Declined Pushups" &&
            exercises[2].exerciseName == "Table Rows"
        val cleanExercises = if (isLegacyMockExercises) emptyList() else exercises

        val isLegacyMockNutrition = !checkedInToday &&
            todayFoodInputs == 0 &&
            resolvedCaloriesLogged == 2000L &&
            resolvedProteinG == 112.0
        val cleanCaloriesLogged = if (isLegacyMockNutrition) 0 else resolvedCaloriesLogged.toInt()
        val cleanProteinG = if (isLegacyMockNutrition) 0.0 else resolvedProteinG
        val cleanCarbsG = if (isLegacyMockNutrition) 0.0 else resolvedCarbsG
        val cleanFatsG = if (isLegacyMockNutrition) 0.0 else resolvedFatsG
        val cleanFiberG = if (isLegacyMockNutrition) 0.0 else resolvedFiberG
        val cleanOmega3 = if (isLegacyMockNutrition) 0.0 else resolvedOmega3
        val cleanWaterLiters = if (isLegacyMockNutrition && resolvedWaterLiters == 4.0) 0.0 else resolvedWaterLiters
        val cleanXp = if (isLegacyMockNutrition && isLegacyMockExercises && resolvedXp == 620L) 0 else resolvedXp.toInt().coerceAtLeast(0)
        val cleanStreak = if (isLegacyMockNutrition && isLegacyMockExercises && resolvedStreak == 4L) 0 else resolvedStreak.toInt()

        val syncedXp = cleanXp
        val tier = TrainerRankProgression.tierForXp(syncedXp)
        val nextXp = TrainerRankProgression.nextThresholdForXp(syncedXp)
        val progress = TrainerRankProgression.progressPercentForXp(syncedXp)
        val cleanAchievements = achievements.filterNot {
            (it.title == "Iron Foundation" || it.title == "Caloric Vanguard") && syncedXp == 0
        }
        val syncedAchievements = TrainerRankProgression.unlockedClassAchievementsForXp(syncedXp, cleanAchievements)
        val autoTitle = "${tier.classTitle} • ${tier.pokemonName}"

        return HunterSystemState(
            playerStatus = PlayerStatus(
                level = tier.tierLevel,
                rank = tier.classTitle,
                currentXp = syncedXp,
                nextLevelXp = nextXp,
                progressPercent = progress,
                hpPercent = hpPercent,
                streakDays = cleanStreak,
                checkedInToday = checkedInToday,
                lastProductiveDate = lastProductiveDate,
                lastDailyResetDate = lastDailyResetDate.ifBlank { StreakDateUtils.todayDateString() },
                todayFoodInputs = todayFoodInputs,
                todayExerciseInputs = todayExerciseInputs,
                lastCheckInTrigger = lastCheckInTrigger,
                productiveHistoryDates = if (cleanStreak == 0 && !checkedInToday && parsedDailyLogs.isEmpty()) emptyList() else historyDates,
                dailyLogsByDate = parsedDailyLogs,
                activeBuffs = buffs,
                activeDebuffs = debuffs
            ),
            statBreakdown = StatBreakdown(
                vitality = VitalityStats(
                    caloriesLogged = cleanCaloriesLogged,
                    caloriesTarget = resolvedCaloriesTarget.toInt().coerceIn(500, 50000),
                    proteinG = cleanProteinG,
                    carbsG = cleanCarbsG,
                    fatsG = cleanFatsG,
                    fiberG = cleanFiberG,
                    omega3Mg = cleanOmega3,
                    targetMet = cleanCaloriesLogged >= resolvedCaloriesTarget && cleanProteinG >= 110.0
                ),
                stamina = StaminaStats(
                    waterLiters = cleanWaterLiters,
                    targetLiters = resolvedTargetLiters.coerceIn(0.5, 50.0),
                    targetMet = cleanWaterLiters >= resolvedTargetLiters
                ),
                strength = StrengthStats(
                    workoutCompleted = cleanExercises.isNotEmpty() && resolvedWorkoutCompleted,
                    exercisesLogged = cleanExercises
                )
            ),
            questLog = quests,
            unlockedAchievements = syncedAchievements,
            tacticalBriefing = resolvedBriefing,
            suggestedActions = actions,
            biometrics = biometrics.copy(hunterTitle = autoTitle)
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

@IgnoreExtraProperties
data class AppReleaseInfo(
    val latestVersionCode: Long = 2L,
    val latestVersionName: String = "1.0.2",
    val releaseTitle: String = "PikaFit Update",
    val releaseNotes: String = "South/North Indian & Fast Food databases, PikaBot muscle exercise & workout plans, editable streak food logs, and automatic GitHub release notifications.",
    val downloadUrl: String = "https://github.com/ATOM495/PikaFit/releases",
    val isMandatory: Boolean = false,
    val publishedAt: String = "",
    val isUpdateAvailable: Boolean = false
)

