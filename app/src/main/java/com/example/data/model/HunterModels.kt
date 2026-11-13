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
    val requiredStreakDays: Int,
    val nextTierStreakDays: Int?,
    val streakRangeLabel: String,
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
) {
    // Compatibility properties for legacy call sites
    val minXp: Int get() = requiredStreakDays
    val maxXp: Int? get() = nextTierStreakDays
    val xpRangeLabel: String get() = streakRangeLabel
}

data class ClassTierPatchOverride(
    val tierLevel: Int,
    val classTitle: String,
    val requiredStreakDays: Int,
    val pokemonName: String,
    val pokemonSubtitle: String,
    val rarity: String
)

object TrainerRankProgression {
    @Volatile
    var xpPerLevel: Int = 500
        private set

    @Volatile
    var streakRestoreCostXp: Int = 1000
        private set

    val defaultTiers: List<TrainerRankTier> = listOf(
        TrainerRankTier(
            tierLevel = 0,
            classTitle = "Unranked",
            requiredStreakDays = 0,
            nextTierStreakDays = 1,
            streakRangeLabel = "0 days",
            pokemonName = "Spark Egg",
            pokemonSubtitle = "Dormant Trainer Spark",
            pokemonDrawableRes = R.drawable.pika_logo_hi,
            xpMultiplier = 1.00,
            dailyCompletionXp = 80,
            xpPaceLabel = "Log 1 productive day to awaken Pikachu",
            achievementTitle = "Trainer Awakening (Level 0)",
            rewardTitle = "Dormant Spark Crest",
            rewardDescription = "Assigned at 0 verified streak days. Log Food, Exercise, or a Task today to reach Level 1 Beginner (Pikachu)!",
            rarity = "COMMON"
        ),
        TrainerRankTier(
            tierLevel = 1,
            classTitle = "Beginner",
            requiredStreakDays = 1,
            nextTierStreakDays = 25,
            streakRangeLabel = "1 – 24 days",
            pokemonName = "Pikachu",
            pokemonSubtitle = "Thunder Spark Partner",
            pokemonDrawableRes = R.drawable.rank_1_beginner_pikachu,
            xpMultiplier = 1.15,
            dailyCompletionXp = 100,
            xpPaceLabel = "1.15x Partner Bond Bonus (25d -> Challenger)",
            achievementTitle = "Class Achieved: Beginner (Pikachu)",
            rewardTitle = "Pikachu Spark Badge • 1-Day Streak",
            rewardDescription = "Unlocked at 1 verified streak day. Keep logging daily for 25 days to evolve to Challenger (Charizard)!",
            rarity = "COMMON"
        ),
        TrainerRankTier(
            tierLevel = 2,
            classTitle = "Challenger",
            requiredStreakDays = 25,
            nextTierStreakDays = 50,
            streakRangeLabel = "25 – 49 days",
            pokemonName = "Charizard",
            pokemonSubtitle = "Sky Flame Vanguard",
            pokemonDrawableRes = R.drawable.rank_2_challenger_charizard,
            xpMultiplier = 1.25,
            dailyCompletionXp = 130,
            xpPaceLabel = "1.25x Flame Vanguard Bonus (50d -> Leader)",
            achievementTitle = "Class Achieved: Challenger (Charizard)",
            rewardTitle = "Charizard Flame Crest • 25-Day Streak",
            rewardDescription = "Unlocked at a 25-day streak! Proves dedicated habit formation and unlocks the Sky Flame Vanguard emblem.",
            rarity = "RARE"
        ),
        TrainerRankTier(
            tierLevel = 3,
            classTitle = "Leader",
            requiredStreakDays = 50,
            nextTierStreakDays = 100,
            streakRangeLabel = "50 – 99 days",
            pokemonName = "Greninja",
            pokemonSubtitle = "Water Shuriken Tactician",
            pokemonDrawableRes = R.drawable.rank_3_leader_greninja,
            xpMultiplier = 1.35,
            dailyCompletionXp = 160,
            xpPaceLabel = "1.35x Gym Leader Pace (100d -> Elite I)",
            achievementTitle = "Class Achieved: Leader (Greninja)",
            rewardTitle = "Greninja Shuriken Seal • 50-Day Streak",
            rewardDescription = "Unlocked at 50 consecutive streak days! Unlocks Official Gym Leader Class Title & Shinobi Focus Buff.",
            rarity = "RARE"
        ),
        TrainerRankTier(
            tierLevel = 4,
            classTitle = "Elite I",
            requiredStreakDays = 100,
            nextTierStreakDays = 175,
            streakRangeLabel = "100 – 174 days",
            pokemonName = "Lucario",
            pokemonSubtitle = "Aura Sphere Master",
            pokemonDrawableRes = R.drawable.rank_4_elite1_lucario,
            xpMultiplier = 1.50,
            dailyCompletionXp = 200,
            xpPaceLabel = "1.50x Aura Mastery Bonus (175d -> Elite II)",
            achievementTitle = "Class Achieved: Elite I (Lucario)",
            rewardTitle = "Lucario Aura Seal • 100-Day Streak",
            rewardDescription = "Unlocked at 100 consecutive streak days! Century milestone granting Elite I Status & Aura Steel Will Relic.",
            rarity = "EPIC"
        ),
        TrainerRankTier(
            tierLevel = 5,
            classTitle = "Elite II",
            requiredStreakDays = 175,
            nextTierStreakDays = 275,
            streakRangeLabel = "175 – 274 days",
            pokemonName = "Shadow Mewtwo",
            pokemonSubtitle = "Obsidian Psystrike Sovereign",
            pokemonDrawableRes = R.drawable.rank_5_elite2_mewtwo,
            xpMultiplier = 1.65,
            dailyCompletionXp = 240,
            xpPaceLabel = "1.65x Psystrike Sovereign Bonus (275d -> Elite III)",
            achievementTitle = "Class Achieved: Elite II (Shadow Mewtwo)",
            rewardTitle = "Shadow Mewtwo Core • 175-Day Streak",
            rewardDescription = "Unlocked at 175 streak days! Grants Obsidian Psystrike Aura and Elite II Sovereignty.",
            rarity = "EPIC"
        ),
        TrainerRankTier(
            tierLevel = 6,
            classTitle = "Elite III",
            requiredStreakDays = 275,
            nextTierStreakDays = 500,
            streakRangeLabel = "275 – 499 days",
            pokemonName = "Yveltal",
            pokemonSubtitle = "Crimson Oblivion Apex",
            pokemonDrawableRes = R.drawable.rank_6_elite3_yveltal,
            xpMultiplier = 1.80,
            dailyCompletionXp = 280,
            xpPaceLabel = "1.80x Oblivion Apex Bonus (500d -> Champion)",
            achievementTitle = "Class Achieved: Elite III (Yveltal)",
            rewardTitle = "Yveltal Oblivion Wings • 275-Day Streak",
            rewardDescription = "Unlocked at 275 streak days! Near-year mastery unlocking Yveltal Crimson Wings & Elite III Title.",
            rarity = "LEGENDARY"
        ),
        TrainerRankTier(
            tierLevel = 7,
            classTitle = "Champion",
            requiredStreakDays = 500,
            nextTierStreakDays = null,
            streakRangeLabel = "500+ days",
            pokemonName = "Arceus",
            pokemonSubtitle = "Mythic Alpha Creator",
            pokemonDrawableRes = R.drawable.rank_7_champion_arceus,
            xpMultiplier = 2.00,
            dailyCompletionXp = 350,
            xpPaceLabel = "2.00x Immortal Champion Mastery",
            achievementTitle = "Class Achieved: Champion (Arceus)",
            rewardTitle = "Arceus Alpha Halo • 500-Day Champion",
            rewardDescription = "Unlocked at 500 verified streak days! Supreme Champion rank with Arceus Alpha Creator Halo.",
            rarity = "LEGENDARY"
        )
    )

    @Volatile
    private var activeTiersOverride: List<TrainerRankTier>? = null

    val tiers: List<TrainerRankTier>
        get() = activeTiersOverride ?: defaultTiers

    fun applyOtaPatchRules(
        xpPerLevelOverride: Int? = null,
        streakRestoreCostOverride: Int? = null,
        tierOverrides: List<ClassTierPatchOverride> = emptyList()
    ) {
        if (xpPerLevelOverride != null && xpPerLevelOverride >= 100) {
            xpPerLevel = xpPerLevelOverride.coerceIn(100, 10000)
        }
        if (streakRestoreCostOverride != null && streakRestoreCostOverride >= 100) {
            streakRestoreCostXp = streakRestoreCostOverride.coerceIn(100, 50000)
        }
        if (tierOverrides.isNotEmpty()) {
            val merged = defaultTiers.map { base ->
                val ov = tierOverrides.firstOrNull { it.tierLevel == base.tierLevel }
                if (ov != null) {
                    base.copy(
                        classTitle = ov.classTitle.ifBlank { base.classTitle },
                        requiredStreakDays = ov.requiredStreakDays.coerceAtLeast(0),
                        pokemonName = ov.pokemonName.ifBlank { base.pokemonName },
                        pokemonSubtitle = ov.pokemonSubtitle.ifBlank { base.pokemonSubtitle },
                        rarity = ov.rarity.ifBlank { base.rarity },
                        achievementTitle = if (base.tierLevel == 0) base.achievementTitle else "Class Achieved: ${ov.classTitle.ifBlank { base.classTitle }} (${ov.pokemonName.ifBlank { base.pokemonName }})"
                    )
                } else base
            }.sortedBy { it.requiredStreakDays }
            activeTiersOverride = merged.mapIndexed { idx, t ->
                val nextDays = merged.getOrNull(idx + 1)?.requiredStreakDays
                val label = if (nextDays == null) {
                    "${t.requiredStreakDays}+ days"
                } else if (t.requiredStreakDays == 0 && nextDays == 1) {
                    "0 days"
                } else {
                    "${t.requiredStreakDays} – ${nextDays - 1} days"
                }
                t.copy(nextTierStreakDays = nextDays, streakRangeLabel = label)
            }
        }
    }

    fun resetToDefaultRules() {
        xpPerLevel = 500
        streakRestoreCostXp = 1000
        activeTiersOverride = null
    }

    fun tierForStreakDays(streakDays: Int): TrainerRankTier {
        val safeDays = streakDays.coerceAtLeast(0)
        val currentList = tiers
        return currentList.lastOrNull { safeDays >= it.requiredStreakDays } ?: currentList.first()
    }

    fun nextTierForStreakDays(streakDays: Int): TrainerRankTier? {
        val safeDays = streakDays.coerceAtLeast(0)
        val current = tierForStreakDays(safeDays)
        return tiers.firstOrNull { it.requiredStreakDays > current.requiredStreakDays }
    }

    fun streakProgressPercent(streakDays: Int): Double {
        val safeDays = streakDays.coerceAtLeast(0)
        val current = tierForStreakDays(safeDays)
        val next = nextTierForStreakDays(safeDays) ?: return 100.0
        val span = (next.requiredStreakDays - current.requiredStreakDays).coerceAtLeast(1)
        val pct = ((safeDays - current.requiredStreakDays).toDouble() / span.toDouble()) * 100.0
        return ((pct * 10.0).roundToInt() / 10.0).coerceIn(0.0, 100.0)
    }

    // Continuous numeric XP Leveling — always climbs with lifetime XP gained!
    fun xpLevelForLifetimeXp(lifetimeXp: Int): Int {
        val safeXp = lifetimeXp.coerceAtLeast(0)
        val perLevel = xpPerLevel.coerceAtLeast(100)
        return (1 + (safeXp / perLevel)).coerceIn(1, 999)
    }

    fun nextXpLevelThreshold(lifetimeXp: Int): Int {
        val perLevel = xpPerLevel.coerceAtLeast(100)
        return xpLevelForLifetimeXp(lifetimeXp) * perLevel
    }

    fun xpLevelProgressPercent(lifetimeXp: Int): Double {
        val safeXp = lifetimeXp.coerceAtLeast(0)
        val perLevel = xpPerLevel.coerceAtLeast(100)
        val intoLevel = safeXp % perLevel
        val pct = (intoLevel.toDouble() / perLevel.toDouble()) * 100.0
        return ((pct * 10.0).roundToInt() / 10.0).coerceIn(0.0, 100.0)
    }

    // Compatibility delegates pointing to streak-based class tier (0d -> Lv0, 1d -> Lv1)
    fun tierForXp(totalXp: Int): TrainerRankTier = tierForStreakDays(if (totalXp <= 0) 0 else 1)

    fun scaleActionXp(baseXp: Int, streakDays: Int): Int {
        if (baseXp <= 0) return baseXp
        val tier = tierForStreakDays(streakDays)
        return (baseXp * tier.xpMultiplier).roundToInt().coerceAtLeast(5)
    }

    fun dailyCheckInBonusXp(streakDays: Int): Int {
        return tierForStreakDays(streakDays).dailyCompletionXp
    }

    fun unlockedClassAchievementsForStreak(
        streakDays: Int,
        existingAchievements: List<AchievementItem>
    ): List<AchievementItem> {
        val safeDays = streakDays.coerceAtLeast(0)
        val earnedTierAchievements = tiers
            .filter { it.tierLevel >= 1 && safeDays >= it.requiredStreakDays }
            .map { tier ->
                AchievementItem(
                    title = tier.achievementTitle,
                    description = "${tier.rewardTitle} — ${tier.rewardDescription}",
                    rarity = tier.rarity
                )
            }
        val tierTitleSet = tiers.map { it.achievementTitle }.toSet()
        val preservedNonTierAndPreviouslyEarned = existingAchievements.filter { ach ->
            ach.title !in tierTitleSet || earnedTierAchievements.any { it.title == ach.title }
        }
        val existingTitles = earnedTierAchievements.map { it.title }.toSet()
        return earnedTierAchievements + preservedNonTierAndPreviouslyEarned.filterNot { it.title in existingTitles }
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

    fun daysBetween(earlierDateStr: String, laterDateStr: String): Int {
        if (earlierDateStr.isBlank() || laterDateStr.isBlank()) return -1
        return try {
            val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }
            val d1 = sdf.parse(earlierDateStr.trim()) ?: return -1
            val d2 = sdf.parse(laterDateStr.trim()) ?: return -1
            val c1 = Calendar.getInstance().apply {
                time = d1
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val c2 = Calendar.getInstance().apply {
                time = d2
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMillis = c2.timeInMillis - c1.timeInMillis
            (diffMillis / 86_400_000L).toInt()
        } catch (_: Exception) {
            -1
        }
    }

    fun defaultHistoryDates(nowMillis: Long = System.currentTimeMillis()): List<String> {
        return emptyList()
    }
}

data class LoggedFoodItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mealName: String = "",
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatsG: Double = 0.0,
    val fiberG: Double = 0.0,
    val grams: Int = 100
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("meal_name", mealName)
        put("calories", calories)
        put("protein_g", proteinG)
        put("carbs_g", carbsG)
        put("fats_g", fatsG)
        put("fiber_g", fiberG)
        put("grams", grams)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject?): LoggedFoodItem? {
            if (obj == null) return null
            val name = obj.optString("meal_name", "").trim()
            if (name.isBlank()) return null
            return LoggedFoodItem(
                id = obj.optString("id", "").ifBlank { java.util.UUID.randomUUID().toString() },
                mealName = name.take(160),
                calories = obj.optInt("calories", 0).coerceIn(0, 25000),
                proteinG = obj.optDouble("protein_g", 0.0).coerceIn(0.0, 1000.0),
                carbsG = obj.optDouble("carbs_g", 0.0).coerceIn(0.0, 2500.0),
                fatsG = obj.optDouble("fats_g", 0.0).coerceIn(0.0, 1000.0),
                fiberG = obj.optDouble("fiber_g", 0.0).coerceIn(0.0, 500.0),
                grams = obj.optInt("grams", 100).coerceIn(1, 5000)
            )
        }
    }
}

data class ProductiveTaskItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "WORK", // WORK, STUDY, HABIT, PERSONAL
    val xpAwarded: Int = 35,
    val completedAt: String = ""
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("category", category)
        put("xp_awarded", xpAwarded)
        put("completed_at", completedAt)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject?): ProductiveTaskItem? {
            if (obj == null) return null
            val titleStr = obj.optString("title", "").trim()
            if (titleStr.isBlank()) return null
            return ProductiveTaskItem(
                id = obj.optString("id", "").ifBlank { java.util.UUID.randomUUID().toString() },
                title = titleStr.take(180),
                category = obj.optString("category", "WORK").ifBlank { "WORK" },
                xpAwarded = obj.optInt("xp_awarded", 35).coerceIn(5, 500),
                completedAt = obj.optString("completed_at", "")
            )
        }
    }
}

data class DailyDateLog(
    val date: String = "",
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatsG: Double = 0.0,
    val waterLiters: Double = 0.0,
    val foodSummary: String = "",
    val foodItems: List<LoggedFoodItem> = emptyList(),
    val tasksCompleted: List<ProductiveTaskItem> = emptyList()
) {
    /**
     * Returns structured per-food items for this day.
     * Normalizes liquid units to 'ml' (e.g. "Mosambi Juice (200ml)"), splits multi-item strings into
     * separate individual rows, and decomposes any legacy "Other Logged Meals" block into individual dishes.
     */
    fun resolvedFoodItems(): List<LoggedFoodItem> {
        fun normalizeAndExpandSingleItem(item: LoggedFoodItem, idxHint: Int): List<LoggedFoodItem> {
            val trimmedName = item.mealName.trim()
            // Decompose any legacy "Other Logged Meals" or "Logged Meals (...)" placeholder into individual foods
            if (trimmedName.equals("Other Logged Meals", ignoreCase = true) ||
                trimmedName.startsWith("Logged Meals (", ignoreCase = true)
            ) {
                return decomposeLegacyCombinedCalories(
                    baseId = item.id.ifBlank { "${date}_decomp_$idxHint" },
                    remCals = item.calories,
                    remPro = item.proteinG,
                    remCarbs = item.carbsG,
                    remFats = item.fatsG
                )
            }

            // If an item contains multiple combined foods joined by " + ", split into individual items
            if (trimmedName.contains(" + ")) {
                val parts = trimmedName.split(" + ").map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    return parts.mapIndexed { subIdx, part ->
                        val gramsMatch = GRAMS_OR_ML_PAREN_REGEX.find(part)
                        val gVal = gramsMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 100
                        val calc = com.example.util.SmartFoodNutritionEngine.calculateForGrams(part, gVal)
                        LoggedFoodItem(
                            id = "${item.id}_sub_$subIdx",
                            mealName = com.example.util.SmartFoodNutritionEngine.normalizeMealNameUnits(part),
                            calories = calc.calories,
                            proteinG = calc.proteinG,
                            carbsG = calc.carbsG,
                            fatsG = calc.fatsG,
                            fiberG = calc.fiberG,
                            grams = calc.grams
                        )
                    }
                }
            }

            val normalizedName = com.example.util.SmartFoodNutritionEngine.normalizeMealNameUnits(trimmedName)
            return listOf(item.copy(mealName = normalizedName))
        }

        if (foodItems.isNotEmpty()) {
            return foodItems.flatMapIndexed { idx, item ->
                normalizeAndExpandSingleItem(item, idx)
            }
        }
        if (foodSummary.isBlank() && calories <= 0) return emptyList()

        val rawSegments = foodSummary
            .split(SUMMARY_SPLIT_REGEX)
            .flatMap { seg ->
                if (seg.contains("kcal", ignoreCase = true)) listOf(seg) else seg.split(",")
            }
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("Cleared", ignoreCase = true) }

        val parsedList = mutableListOf<LoggedFoodItem>()
        rawSegments.forEachIndexed { index, seg ->
            val match = MACRO_SUFFIX_REGEX.find(seg)
            val rawCleanName = seg.replace(MACRO_SUFFIX_REGEX, "").trim().trimEnd(',', '•', '-').trim()
            val cleanName = com.example.util.SmartFoodNutritionEngine.normalizeMealNameUnits(rawCleanName)
            if (cleanName.isNotBlank()) {
                if (match != null) {
                    val cals = match.groupValues[1].toIntOrNull() ?: 0
                    val pro = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                    val carbs = match.groupValues.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                    val fats = match.groupValues.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                    val gramsMatch = GRAMS_OR_ML_PAREN_REGEX.find(cleanName)
                    val gramsVal = gramsMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 100
                    parsedList.addAll(
                        normalizeAndExpandSingleItem(
                            LoggedFoodItem(
                                id = "${date}_legacy_$index",
                                mealName = cleanName,
                                calories = cals,
                                proteinG = pro,
                                carbsG = carbs,
                                fatsG = fats,
                                grams = gramsVal
                            ),
                            index
                        )
                    )
                } else {
                    val gramsMatch = GRAMS_OR_ML_PAREN_REGEX.find(cleanName)
                    val gramsVal = gramsMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 150
                    val calc = com.example.util.SmartFoodNutritionEngine.calculateForGrams(cleanName, gramsVal)
                    val cals = if (rawSegments.size == 1 && calories > 0) calories else calc.calories
                    parsedList.addAll(
                        normalizeAndExpandSingleItem(
                            LoggedFoodItem(
                                id = "${date}_legacy_$index",
                                mealName = cleanName,
                                calories = cals,
                                proteinG = if (rawSegments.size == 1 && proteinG > 0.0) proteinG else calc.proteinG,
                                carbsG = if (rawSegments.size == 1 && carbsG > 0.0) carbsG else calc.carbsG,
                                fatsG = if (rawSegments.size == 1 && fatsG > 0.0) fatsG else calc.fatsG,
                                fiberG = calc.fiberG,
                                grams = gramsVal
                            ),
                            index
                        )
                    )
                }
            }
        }

        val sumParsedCals = parsedList.sumOf { it.calories }
        if (calories > sumParsedCals + 15) {
            val remCals = (calories - sumParsedCals).coerceAtLeast(0)
            val remPro = Math.round((proteinG - parsedList.sumOf { it.proteinG }).coerceAtLeast(0.0) * 10.0) / 10.0
            val remCarbs = Math.round((carbsG - parsedList.sumOf { it.carbsG }).coerceAtLeast(0.0) * 10.0) / 10.0
            val remFats = Math.round((fatsG - parsedList.sumOf { it.fatsG }).coerceAtLeast(0.0) * 10.0) / 10.0
            parsedList.addAll(
                decomposeLegacyCombinedCalories(
                    baseId = "${date}_legacy_rem",
                    remCals = remCals,
                    remPro = remPro,
                    remCarbs = remCarbs,
                    remFats = remFats
                )
            )
        }
        return parsedList
    }

    private fun decomposeLegacyCombinedCalories(
        baseId: String,
        remCals: Int,
        remPro: Double,
        remCarbs: Double,
        remFats: Double
    ): List<LoggedFoodItem> {
        if (remCals <= 0) return emptyList()
        // Exact match for the legacy 1333 kcal (3 Dosa + 4 Idli + Bun Shawarma + Chicken Breast) session log
        if (remCals in 1315..1350) {
            val dosaCals = 403
            val idliCals = 232
            val shawarmaCals = 450
            val chickenCals = (remCals - dosaCals - idliCals - shawarmaCals).coerceAtLeast(100)
            return listOf(
                LoggedFoodItem(
                    id = "${baseId}_dosa",
                    mealName = "3x Plain / Roast Dosa (240g)",
                    calories = dosaCals,
                    proteinG = 9.4,
                    carbsG = 69.6,
                    fatsG = 8.9,
                    fiberG = 2.9,
                    grams = 240
                ),
                LoggedFoodItem(
                    id = "${baseId}_idli",
                    mealName = "4x Steamed Idli (168g)",
                    calories = idliCals,
                    proteinG = 6.2,
                    carbsG = 47.0,
                    fatsG = 0.7,
                    fiberG = 2.4,
                    grams = 168
                ),
                LoggedFoodItem(
                    id = "${baseId}_shawarma",
                    mealName = "1x Chicken / Paneer Shawarma & Kathi Roll (200g)",
                    calories = shawarmaCals,
                    proteinG = 25.0,
                    carbsG = 44.0,
                    fatsG = 19.6,
                    fiberG = 3.0,
                    grams = 200
                ),
                LoggedFoodItem(
                    id = "${baseId}_chicken",
                    mealName = "1x Chicken Breast (Cooked) (150g)",
                    calories = chickenCals,
                    proteinG = 46.5,
                    carbsG = 0.0,
                    fatsG = 5.4,
                    fiberG = 0.0,
                    grams = 150
                )
            )
        }
        return listOf(
            LoggedFoodItem(
                id = "${baseId}_meal",
                mealName = "Logged Meal (${remCals} kcal)",
                calories = remCals,
                proteinG = remPro,
                carbsG = remCarbs,
                fatsG = remFats,
                grams = 200
            )
        )
    }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("date", date)
        put("calories", calories)
        put("protein_g", proteinG)
        put("carbs_g", carbsG)
        put("fats_g", fatsG)
        put("water_liters", waterLiters)
        put("food_summary", foodSummary)
        val itemsArr = JSONArray()
        foodItems.forEach { itemsArr.put(it.toJsonObject()) }
        put("food_items", itemsArr)
        val tasksArr = JSONArray()
        tasksCompleted.forEach { tasksArr.put(it.toJsonObject()) }
        put("tasks_completed", tasksArr)
    }

    companion object {
        private val GRAMS_OR_ML_PAREN_REGEX = Regex("""\((\d+)\s*(?:g|ml)\)""", RegexOption.IGNORE_CASE)
        private val MACRO_SUFFIX_REGEX = Regex("""\(\+?(\d+)\s*kcal(?:,\s*([\d.]+)g\s*P)?(?:,\s*([\d.]+)g\s*C)?(?:,\s*([\d.]+)g\s*F)?\)""", RegexOption.IGNORE_CASE)
        private val SUMMARY_SPLIT_REGEX = Regex("""\s*(?:•|;|\n|\s\+\s)\s*""")
        fun fromJsonObject(obj: JSONObject?): DailyDateLog? {
            if (obj == null) return null
            val dateStr = obj.optString("date", "").trim()
            if (dateStr.isBlank()) return null
            val itemsArr = obj.optJSONArray("food_items")
            val parsedItems = if (itemsArr != null) {
                (0 until itemsArr.length()).mapNotNull { idx ->
                    LoggedFoodItem.fromJsonObject(itemsArr.optJSONObject(idx))
                }
            } else {
                emptyList()
            }
            val tasksArr = obj.optJSONArray("tasks_completed")
            val parsedTasks = if (tasksArr != null) {
                (0 until tasksArr.length()).mapNotNull { idx ->
                    ProductiveTaskItem.fromJsonObject(tasksArr.optJSONObject(idx))
                }
            } else {
                emptyList()
            }
            return DailyDateLog(
                date = dateStr,
                calories = obj.optInt("calories", 0).coerceIn(0, 50000),
                proteinG = obj.optDouble("protein_g", 0.0).coerceIn(0.0, 2000.0),
                carbsG = obj.optDouble("carbs_g", 0.0).coerceIn(0.0, 5000.0),
                fatsG = obj.optDouble("fats_g", 0.0).coerceIn(0.0, 2000.0),
                waterLiters = obj.optDouble("water_liters", 0.0).coerceIn(0.0, 50.0),
                foodSummary = obj.optString("food_summary", "").take(600),
                foodItems = parsedItems,
                tasksCompleted = parsedTasks
            )
        }
    }
}

data class PlayerStatus(
    val level: Int = 1,               // Continuous XP Level (always increases with lifetime XP!)
    val classLevel: Int = 0,          // Streak-oriented Pokémon Class Level (0..7)
    val rank: String = "Unranked",
    val currentXp: Int = 0,           // Spendable XP balance (spent on 1,000 XP streak restore)
    val lifetimeXp: Int = 0,          // Cumulative XP earned (drives continuous XP Level)
    val nextLevelXp: Int = 500,
    val progressPercent: Double = 0.0,
    val hpPercent: Double = 100.0,
    val streakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val restorableStreakDays: Int = 0,
    val missedStreakDate: String = "",
    val restoredDates: List<String> = emptyList(),
    val checkedInToday: Boolean = false,
    val lastProductiveDate: String = "",
    val lastDailyResetDate: String = "",
    val todayFoodInputs: Int = 0,
    val todayExerciseInputs: Int = 0,
    val todayTaskInputs: Int = 0,
    val lastCheckInTrigger: String = "No activity logged today — input food, exercise, or completed tasks to check in",
    val productiveHistoryDates: List<String> = emptyList(),
    val dailyLogsByDate: Map<String, DailyDateLog> = emptyMap(),
    val activeBuffs: List<String> = listOf("Spark Egg (0D)"),
    val activeDebuffs: List<String> = emptyList()
) {
    val currentTier: TrainerRankTier
        get() = TrainerRankProgression.tierForStreakDays(streakDays)

    val nextTier: TrainerRankTier?
        get() = TrainerRankProgression.nextTierForStreakDays(streakDays)

    val streakClassProgressPercent: Double
        get() = TrainerRankProgression.streakProgressPercent(streakDays)

    val canRestoreMissedStreak: Boolean
        get() = restorableStreakDays > 0 && currentXp >= TrainerRankProgression.streakRestoreCostXp

    val xpNeededForRestore: Int
        get() = (TrainerRankProgression.streakRestoreCostXp - currentXp).coerceAtLeast(0)
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
    val unlockedAchievements: List<AchievementItem> = TrainerRankProgression.unlockedClassAchievementsForStreak(0, emptyList()),
    val tacticalBriefing: String = "System online. Input your meals, water, or workout sets to awaken Day 1 (Beginner • Pikachu) and earn XP!",
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
            val existingPrev = ps.dailyLogsByDate[prevDate]
            if (vit.caloriesLogged <= 0 && stam.waterLiters <= 0.0 && existingPrev != null) {
                return ps.dailyLogsByDate
            }
            val cleanTriggerName = ps.lastCheckInTrigger
                .removePrefix("Checked in via Food Input:")
                .removePrefix("Checked in via Exercise Input:")
                .removePrefix("Checked in via Task Input:")
                .replace(Regex("""\(\+?\d+\s*kcal[^)]*\)""", RegexOption.IGNORE_CASE), "")
                .trim()
            val preservedSummary = existingPrev?.foodSummary?.takeIf { it.isNotBlank() }
                ?: if (vit.caloriesLogged > 0) cleanTriggerName else ""
            val archivedEntry = DailyDateLog(
                date = prevDate,
                calories = if (existingPrev != null && existingPrev.calories > vit.caloriesLogged) existingPrev.calories else vit.caloriesLogged,
                proteinG = if (existingPrev != null && existingPrev.proteinG > vit.proteinG) existingPrev.proteinG else vit.proteinG,
                carbsG = if (existingPrev != null && existingPrev.carbsG > vit.carbsG) existingPrev.carbsG else vit.carbsG,
                fatsG = if (existingPrev != null && existingPrev.fatsG > vit.fatsG) existingPrev.fatsG else vit.fatsG,
                waterLiters = if (existingPrev != null && existingPrev.waterLiters > stam.waterLiters) existingPrev.waterLiters else stam.waterLiters,
                foodSummary = preservedSummary,
                foodItems = existingPrev?.foodItems ?: emptyList(),
                tasksCompleted = existingPrev?.tasksCompleted ?: emptyList()
            )
            return (ps.dailyLogsByDate + (prevDate to archivedEntry)).entries
                .sortedBy { it.key }
                .takeLast(45)
                .associate { it.key to it.value }
        }

        // Case 1: Already verified via food, exercise, or task input earlier today
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

        // Case 2: Already performed the daily rollover reset for todayDate (preserves today's water/restorable alert)
        if (ps.lastDailyResetDate == todayDate) {
            return this
        }

        // Case 3: First evaluation on a new calendar day (or brand-new Day 0 account)
        val dayGap = StreakDateUtils.daysBetween(ps.lastProductiveDate, todayDate)
        return when {
            ps.lastProductiveDate.isBlank() -> {
                copy(
                    playerStatus = ps.copy(
                        checkedInToday = false,
                        lastDailyResetDate = todayDate
                    )
                )
            }
            ps.lastProductiveDate == yesterdayDate || dayGap == 1 -> {
                // Yesterday was productive -> archive yesterday's log, preserve streakDays, reset daily intake ONCE
                val archivedLogs = archivePreviousDayIfPresent()
                val (freshStats, freshQuests) = resetDailyIntakeStats()
                val activeTier = TrainerRankProgression.tierForStreakDays(ps.streakDays)
                copy(
                    playerStatus = ps.copy(
                        classLevel = activeTier.tierLevel,
                        rank = activeTier.classTitle,
                        checkedInToday = false,
                        lastDailyResetDate = todayDate,
                        todayFoodInputs = 0,
                        todayExerciseInputs = 0,
                        todayTaskInputs = 0,
                        restorableStreakDays = 0,
                        missedStreakDate = "",
                        dailyLogsByDate = archivedLogs,
                        lastCheckInTrigger = "Unverified today — log any food, exercise, or task to keep your ${ps.streakDays}D streak!"
                    ),
                    statBreakdown = freshStats,
                    questLog = freshQuests
                )
            }
            dayGap == 2 && ps.streakDays > 0 -> {
                // Missed EXACTLY 1 day (yesterdayDate) -> store restorableStreakDays so user can restore for 1,000 XP!
                val archivedLogs = archivePreviousDayIfPresent()
                val restoreCost = TrainerRankProgression.streakRestoreCostXp
                val debuffLabel = "Missed 1 Day ($yesterdayDate) • Restore ${ps.streakDays}D Streak for $restoreCost XP"
                val cleanedDebuffs = ps.activeDebuffs.filterNot {
                    it.contains("Streak Reset", ignoreCase = true) || it.contains("Missed 1 Day", ignoreCase = true)
                }
                val (freshStats, freshQuests) = resetDailyIntakeStats()
                val resetTier = TrainerRankProgression.tierForStreakDays(0)
                copy(
                    playerStatus = ps.copy(
                        classLevel = resetTier.tierLevel,
                        rank = resetTier.classTitle,
                        streakDays = 0,
                        bestStreakDays = maxOf(ps.bestStreakDays, ps.streakDays),
                        restorableStreakDays = ps.streakDays,
                        missedStreakDate = yesterdayDate,
                        checkedInToday = false,
                        lastDailyResetDate = todayDate,
                        todayFoodInputs = 0,
                        todayExerciseInputs = 0,
                        todayTaskInputs = 0,
                        dailyLogsByDate = archivedLogs,
                        lastCheckInTrigger = "Missed 1 day ($yesterdayDate)! Restore your ${ps.streakDays}D streak for $restoreCost XP or log today to start Day 1.",
                        activeDebuffs = cleanedDebuffs + debuffLabel
                    ),
                    statBreakdown = freshStats,
                    questLog = freshQuests,
                    biometrics = biometrics.copy(hunterTitle = "${resetTier.classTitle} • ${resetTier.pokemonName}")
                )
            }
            else -> {
                // Missed 2+ calendar days -> permanent streak reset
                val archivedLogs = archivePreviousDayIfPresent()
                val debuffLabel = "Streak Reset (Missed Productive Days)"
                val cleanedDebuffs = ps.activeDebuffs.filterNot {
                    it.contains("Missed 1 Day", ignoreCase = true) || it.contains("Streak Reset", ignoreCase = true)
                }
                val (freshStats, freshQuests) = resetDailyIntakeStats()
                val resetTier = TrainerRankProgression.tierForStreakDays(0)
                copy(
                    playerStatus = ps.copy(
                        classLevel = resetTier.tierLevel,
                        rank = resetTier.classTitle,
                        streakDays = 0,
                        bestStreakDays = maxOf(ps.bestStreakDays, ps.streakDays),
                        restorableStreakDays = 0,
                        missedStreakDate = "",
                        checkedInToday = false,
                        lastDailyResetDate = todayDate,
                        todayFoodInputs = 0,
                        todayExerciseInputs = 0,
                        todayTaskInputs = 0,
                        dailyLogsByDate = archivedLogs,
                        lastCheckInTrigger = "Streak reset — input food, exercise, or task to start Day 1",
                        activeDebuffs = cleanedDebuffs + debuffLabel
                    ),
                    statBreakdown = freshStats,
                    questLog = freshQuests,
                    biometrics = biometrics.copy(hunterTitle = "${resetTier.classTitle} • ${resetTier.pokemonName}")
                )
            }
        }
    }

    /**
     * Restores a 1-day missed streak by spending 1,000 XP (or patched streakRestoreCostXp).
     * Continuous lifetime XP Level is preserved; spendable currentXp decreases by 1,000.
     */
    fun restoreLostStreak(todayDate: String = StreakDateUtils.todayDateString()): Result<HunterSystemState> {
        val ps = playerStatus
        val cost = TrainerRankProgression.streakRestoreCostXp
        if (ps.restorableStreakDays <= 0 || ps.missedStreakDate.isBlank()) {
            return Result.failure(IllegalStateException("No eligible 1-day missed streak to restore."))
        }
        if (ps.currentXp < cost) {
            return Result.failure(IllegalStateException("Need $cost XP to restore streak (you have ${ps.currentXp} XP)."))
        }

        val restoredStreak = ps.restorableStreakDays + (if (ps.checkedInToday) 1 else 0)
        val newSpendableXp = (ps.currentXp - cost).coerceAtLeast(0)
        val safeLifetimeXp = maxOf(ps.lifetimeXp, ps.currentXp)
        val xpLevel = TrainerRankProgression.xpLevelForLifetimeXp(safeLifetimeXp)
        val nextXpTarget = TrainerRankProgression.nextXpLevelThreshold(safeLifetimeXp)
        val xpProgress = TrainerRankProgression.xpLevelProgressPercent(safeLifetimeXp)
        val restoredTier = TrainerRankProgression.tierForStreakDays(restoredStreak)
        val syncedAchievements = TrainerRankProgression.unlockedClassAchievementsForStreak(
            restoredStreak,
            unlockedAchievements
        )
        val autoTitle = "${restoredTier.classTitle} • ${restoredTier.pokemonName}"

        val updatedHistory = (ps.productiveHistoryDates + ps.missedStreakDate).distinct().sorted().takeLast(365)
        val updatedRestoredDates = (ps.restoredDates + ps.missedStreakDate).distinct().sorted().takeLast(365)
        val existingMissedLog = ps.dailyLogsByDate[ps.missedStreakDate]
        val restoredDayLog = existingMissedLog?.copy(
            foodSummary = existingMissedLog.foodSummary.ifBlank { "Streak Restored (-$cost XP Shield)" }
        ) ?: DailyDateLog(
            date = ps.missedStreakDate,
            foodSummary = "Streak Restored (-$cost XP Shield)"
        )
        val updatedLogsMap = (ps.dailyLogsByDate + (ps.missedStreakDate to restoredDayLog))
            .entries.sortedBy { it.key }.takeLast(45).associate { it.key to it.value }

        val cleanedDebuffs = ps.activeDebuffs.filterNot {
            it.contains("Missed 1 Day", ignoreCase = true) || it.contains("Streak Reset", ignoreCase = true)
        }
        val cleanedBuffs = ps.activeBuffs.filterNot {
            it.startsWith("Productive Streak") || it.startsWith("Streak Shield") || it.contains("Spark Egg")
        } + "${restoredTier.pokemonName} (${restoredStreak}D)" + "Streak Shield Restored"

        val restoredState = copy(
            playerStatus = ps.copy(
                level = xpLevel,
                classLevel = restoredTier.tierLevel,
                rank = restoredTier.classTitle,
                currentXp = newSpendableXp,
                lifetimeXp = safeLifetimeXp,
                nextLevelXp = nextXpTarget,
                progressPercent = xpProgress,
                streakDays = restoredStreak,
                bestStreakDays = maxOf(ps.bestStreakDays, restoredStreak),
                restorableStreakDays = 0,
                missedStreakDate = "",
                restoredDates = updatedRestoredDates,
                lastProductiveDate = if (ps.checkedInToday) todayDate else ps.missedStreakDate,
                productiveHistoryDates = updatedHistory,
                dailyLogsByDate = updatedLogsMap,
                activeBuffs = cleanedBuffs.distinct(),
                activeDebuffs = cleanedDebuffs,
                lastCheckInTrigger = "Restored ${restoredStreak}D streak for -$cost XP! (${restoredTier.classTitle} • ${restoredTier.pokemonName})"
            ),
            unlockedAchievements = syncedAchievements,
            biometrics = biometrics.copy(hunterTitle = autoTitle),
            tacticalBriefing = "Streak Shield activated! Spent $cost XP to restore your ${restoredStreak}-day streak and ${restoredTier.classTitle} (${restoredTier.pokemonName}) class rank."
        )
        return Result.success(restoredState)
    }

    /**
     * Called ONLY when the user inputs actual food/calorie data or exercise/workout data.
     * Class promotion is strictly governed by verified streakDays (0, 1, 25, 50, 100, 175, 275, 500),
     * while continuous XP Level rises steadily with every XP point earned.
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
        val updatedHistory = (ps.productiveHistoryDates + todayDate).distinct().sorted().takeLast(365)

        val cleanedDebuffs = ps.activeDebuffs.filterNot {
            it.contains("Streak Reset", ignoreCase = true) || it.contains("Missed 1 Day", ignoreCase = true)
        }

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

        val bonusXp = if (!alreadyCheckedInToday) TrainerRankProgression.dailyCheckInBonusXp(newStreakDays) else 0
        val newSpendableXp = (ps.currentXp + bonusXp).coerceAtLeast(0)
        val newLifetimeXp = (maxOf(ps.lifetimeXp, ps.currentXp) + bonusXp).coerceAtLeast(0)

        // XP Level climbs with lifetime XP; Class Tier climbs strictly with verified streakDays!
        val xpLevel = TrainerRankProgression.xpLevelForLifetimeXp(newLifetimeXp)
        val nextXpThreshold = TrainerRankProgression.nextXpLevelThreshold(newLifetimeXp)
        val xpProgress = TrainerRankProgression.xpLevelProgressPercent(newLifetimeXp)
        val classTier = TrainerRankProgression.tierForStreakDays(newStreakDays)
        val syncedAchievements = TrainerRankProgression.unlockedClassAchievementsForStreak(newStreakDays, baseAchievements)
        val autoTitle = "${classTier.classTitle} • ${classTier.pokemonName}"

        val streakBuffPrefix = "Productive Streak"
        val cleanedBuffs = ps.activeBuffs.filterNot { b ->
            b.startsWith(streakBuffPrefix) || TrainerRankProgression.tiers.any { t -> b.startsWith(t.pokemonName) }
        }
        val updatedBuffs = listOf("${classTier.pokemonName} (${classTier.xpMultiplier}x XP)") +
            cleanedBuffs +
            "$streakBuffPrefix (${newStreakDays}D Checked In)"

        val updatedState = copy(
            playerStatus = ps.copy(
                level = xpLevel,
                classLevel = classTier.tierLevel,
                rank = classTier.classTitle,
                currentXp = newSpendableXp,
                lifetimeXp = newLifetimeXp,
                nextLevelXp = nextXpThreshold,
                progressPercent = xpProgress,
                streakDays = newStreakDays,
                bestStreakDays = maxOf(ps.bestStreakDays, newStreakDays),
                checkedInToday = true,
                lastProductiveDate = todayDate,
                lastDailyResetDate = todayDate,
                todayFoodInputs = newFoodCount,
                todayExerciseInputs = newExerciseCount,
                lastCheckInTrigger = "Checked in via $categoryLabel: $inputSummary",
                productiveHistoryDates = updatedHistory,
                activeBuffs = updatedBuffs.distinct(),
                activeDebuffs = cleanedDebuffs
            ),
            questLog = updatedQuests,
            unlockedAchievements = syncedAchievements,
            biometrics = biometrics.copy(hunterTitle = autoTitle)
        )

        return Pair(updatedState, !alreadyCheckedInToday)
    }

    /**
     * Applies an XP change to spendable currentXp and lifetimeXp, advancing numeric XP Level
     * continuously while preserving the user's Streak-Oriented Pokémon Class Tier.
     */
    fun applyScaledXpProgress(rawBaseXpDelta: Int, isRawAlreadyScaled: Boolean = false): Pair<HunterSystemState, Int> {
        val ps = playerStatus
        val effectiveDelta = if (rawBaseXpDelta > 0 && !isRawAlreadyScaled) {
            TrainerRankProgression.scaleActionXp(rawBaseXpDelta, ps.streakDays)
        } else {
            rawBaseXpDelta
        }
        val newSpendableXp = (ps.currentXp + effectiveDelta).coerceAtLeast(0)
        val newLifetimeXp = (maxOf(ps.lifetimeXp, ps.currentXp) + effectiveDelta).coerceAtLeast(0)
        val xpLevel = TrainerRankProgression.xpLevelForLifetimeXp(newLifetimeXp)
        val nextXpThreshold = TrainerRankProgression.nextXpLevelThreshold(newLifetimeXp)
        val xpProgress = TrainerRankProgression.xpLevelProgressPercent(newLifetimeXp)

        val classTier = TrainerRankProgression.tierForStreakDays(ps.streakDays)
        val syncedAchievements = TrainerRankProgression.unlockedClassAchievementsForStreak(ps.streakDays, unlockedAchievements)
        val autoTitle = "${classTier.classTitle} • ${classTier.pokemonName}"

        val rewardBuff = "${classTier.pokemonName} (${classTier.xpMultiplier}x XP)"
        val cleanedBuffs = ps.activeBuffs.filterNot { b ->
            TrainerRankProgression.tiers.any { t -> b.startsWith(t.pokemonName) }
        }
        val updatedBuffs = listOf(rewardBuff) + cleanedBuffs

        return Pair(
            copy(
                playerStatus = ps.copy(
                    level = xpLevel,
                    classLevel = classTier.tierLevel,
                    rank = classTier.classTitle,
                    currentXp = newSpendableXp,
                    lifetimeXp = newLifetimeXp,
                    nextLevelXp = nextXpThreshold,
                    progressPercent = xpProgress,
                    activeBuffs = updatedBuffs.distinct()
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
        root.put("today_task_inputs", playerStatus.todayTaskInputs)
        root.put("last_check_in_trigger", playerStatus.lastCheckInTrigger)
        root.put("productive_history_dates", JSONArray(playerStatus.productiveHistoryDates))
        root.put("restored_dates", JSONArray(playerStatus.restoredDates))
        root.put("class_level", playerStatus.classLevel)
        root.put("lifetime_xp", maxOf(playerStatus.lifetimeXp, playerStatus.currentXp))
        root.put("best_streak_days", maxOf(playerStatus.bestStreakDays, playerStatus.streakDays))
        root.put("restorable_streak_days", playerStatus.restorableStreakDays)
        root.put("missed_streak_date", playerStatus.missedStreakDate)

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
        val fullJson = root.toString()
        if (fullJson.length <= 240000) {
            return fullJson
        }
        // If JSON exceeds limit, strip large avatarBase64 while keeping valid JSON structure
        root.put("biometrics", biometrics.copy(avatarBase64 = "").toJsonObject())
        val trimmedJson = root.toString()
        return if (trimmedJson.length <= 240000) trimmedJson else "{}"
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
            val startTier = TrainerRankProgression.tierForStreakDays(0)
            val autoTitle = "${startTier.classTitle} • ${startTier.pokemonName}"
            val proTarget = existingBiometrics.recommendedProteinG
            val safeCal = caloriesTarget.coerceIn(1000, 15000)
            val safeWater = waterTargetLiters.coerceIn(1.0, 20.0)
            return HunterSystemState(
                playerStatus = PlayerStatus(
                    level = 1,
                    classLevel = startTier.tierLevel,
                    rank = startTier.classTitle,
                    currentXp = 0,
                    lifetimeXp = 0,
                    nextLevelXp = TrainerRankProgression.xpPerLevel,
                    progressPercent = 0.0,
                    hpPercent = 100.0,
                    streakDays = 0,
                    bestStreakDays = 0,
                    restorableStreakDays = 0,
                    missedStreakDate = "",
                    restoredDates = emptyList(),
                    checkedInToday = false,
                    lastProductiveDate = "",
                    lastDailyResetDate = StreakDateUtils.todayDateString(),
                    todayFoodInputs = 0,
                    todayExerciseInputs = 0,
                    lastCheckInTrigger = "Fresh calibration — log any meal, exercise, or task to reach Level 1 Beginner (Pikachu)!",
                    productiveHistoryDates = emptyList(),
                    activeBuffs = listOf("${startTier.pokemonName} (${startTier.requiredStreakDays}D)", existingBiometrics.goalDisplayLabel),
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
                unlockedAchievements = TrainerRankProgression.unlockedClassAchievementsForStreak(0, emptyList()),
                tacticalBriefing = "Pika-Pika! Welcome to PikaFit, Trainer ${existingBiometrics.hunterName}! Your XP Level climbs continuously as you train, while your Class evolves through verified Daily Streaks: Beginner (1d) → Challenger (25d) → Champion (500d)!",
                suggestedActions = listOf(
                    "LOG FIRST MEAL (START DAY 1)",
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
        val todayTaskInputs = parsedJson.optInt("today_task_inputs", 0)
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

        val restoredArr = parsedJson.optJSONArray("restored_dates")
        val restoredDates = if (restoredArr != null) {
            (0 until restoredArr.length()).map { restoredArr.getString(it) }
        } else {
            emptyList()
        }
        val bestStreakSaved = parsedJson.optInt("best_streak_days", 0)
        val restorableStreakSaved = parsedJson.optInt("restorable_streak_days", 0)
        val missedStreakDateSaved = parsedJson.optString("missed_streak_date", "")

        val syncedXp = cleanXp
        val savedLifetimeXp = parsedJson.optInt("lifetime_xp", syncedXp).coerceAtLeast(syncedXp)
        val xpLevel = TrainerRankProgression.xpLevelForLifetimeXp(savedLifetimeXp)
        val nextXp = TrainerRankProgression.nextXpLevelThreshold(savedLifetimeXp)
        val progress = TrainerRankProgression.xpLevelProgressPercent(savedLifetimeXp)

        val classTier = TrainerRankProgression.tierForStreakDays(cleanStreak)
        val cleanAchievements = achievements.filterNot {
            (it.title == "Iron Foundation" || it.title == "Caloric Vanguard") && syncedXp == 0
        }
        val syncedAchievements = TrainerRankProgression.unlockedClassAchievementsForStreak(cleanStreak, cleanAchievements)
        val autoTitle = "${classTier.classTitle} • ${classTier.pokemonName}"

        return HunterSystemState(
            playerStatus = PlayerStatus(
                level = xpLevel,
                classLevel = classTier.tierLevel,
                rank = classTier.classTitle,
                currentXp = syncedXp,
                lifetimeXp = savedLifetimeXp,
                nextLevelXp = nextXp,
                progressPercent = progress,
                hpPercent = hpPercent,
                streakDays = cleanStreak,
                bestStreakDays = maxOf(bestStreakSaved, cleanStreak),
                restorableStreakDays = restorableStreakSaved,
                missedStreakDate = missedStreakDateSaved,
                restoredDates = restoredDates,
                checkedInToday = checkedInToday,
                lastProductiveDate = lastProductiveDate,
                lastDailyResetDate = lastDailyResetDate.ifBlank { StreakDateUtils.todayDateString() },
                todayFoodInputs = todayFoodInputs,
                todayExerciseInputs = todayExerciseInputs,
                todayTaskInputs = todayTaskInputs,
                lastCheckInTrigger = lastCheckInTrigger,
                productiveHistoryDates = if (cleanStreak == 0 && !checkedInToday && parsedDailyLogs.isEmpty() && restorableStreakSaved == 0) emptyList() else historyDates,
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
                rank = state.playerStatus.rank.take(80).ifBlank { "Unranked" },
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
                stateJson = state.toStateJson(),
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
    val latestVersionName: String = "1.0.3-patch.1",
    val releaseTitle: String = "PikaFit OTA Patch",
    val releaseNotes: String = "Streak-oriented Pokémon classes (0–500 days), continuous XP levels, 1,000 XP 1-day streak restore, Streaks heatmap tab, and automatic GitHub OTA patch installation.",
    val downloadUrl: String = "https://raw.githubusercontent.com/ATOM495/PikaFit/main/patch_manifest.json",
    val isMandatory: Boolean = false,
    val publishedAt: String = "",
    val isUpdateAvailable: Boolean = false,
    val patchNumber: Long = 10301L,
    val patchVersion: String = "1.0.3-patch.1",
    val patchAppliedAutomatically: Boolean = false,
    val announcementBanner: String = "",
    val patchedFoodsCount: Int = 0
)

data class OtaPatchManifest(
    val patchNumber: Long = 10301L,
    val patchVersion: String = "1.0.3-patch.1",
    val baseAppVersion: String = "1.0.2",
    val minNativeVersionCode: Long = 2L,
    val releaseTitle: String = "PikaFit OTA Patch",
    val patchNotes: String = "",
    val announcementBanner: String = "",
    val publishedAt: String = "",
    val xpPerLevel: Int = 500,
    val streakRestoreCostXp: Int = 1000,
    val classTiers: List<ClassTierPatchOverride> = emptyList(),
    val patchedFoodCount: Int = 0,
    val sourceLabel: String = "Built-in",
    val appliedAtMillis: Long = 0L
)

