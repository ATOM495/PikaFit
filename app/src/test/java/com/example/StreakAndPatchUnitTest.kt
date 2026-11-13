package com.example

import com.example.data.model.HunterSystemState
import com.example.data.model.PlayerStatus
import com.example.data.model.StreakDateUtils
import com.example.data.model.TrainerRankProgression
import com.example.util.PatchUpdateManager
import com.example.util.SmartFoodNutritionEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StreakAndPatchUnitTest {

    @After
    fun tearDown() {
        TrainerRankProgression.resetToDefaultRules()
    }

    @Test
    fun classHierarchy_promotesStrictlyByConsecutiveStreakDays() {
        val expectations = listOf(
            0 to Pair(0, "Unranked"),
            1 to Pair(1, "Beginner"),
            24 to Pair(1, "Beginner"),
            25 to Pair(2, "Challenger"),
            49 to Pair(2, "Challenger"),
            50 to Pair(3, "Leader"),
            99 to Pair(3, "Leader"),
            100 to Pair(4, "Elite I"),
            174 to Pair(4, "Elite I"),
            175 to Pair(5, "Elite II"),
            274 to Pair(5, "Elite II"),
            275 to Pair(6, "Elite III"),
            499 to Pair(6, "Elite III"),
            500 to Pair(7, "Champion")
        )

        expectations.forEach { (days, expected) ->
            val tier = TrainerRankProgression.tierForStreakDays(days)
            assertEquals("Days=$days classLevel", expected.first, tier.tierLevel)
            assertEquals("Days=$days classTitle", expected.second, tier.classTitle)
        }

        assertEquals("Spark Egg", TrainerRankProgression.tierForStreakDays(0).pokemonName)
        assertEquals("Pikachu", TrainerRankProgression.tierForStreakDays(1).pokemonName)
        assertEquals("Charizard", TrainerRankProgression.tierForStreakDays(25).pokemonName)
        assertEquals("Greninja", TrainerRankProgression.tierForStreakDays(50).pokemonName)
        assertEquals("Lucario", TrainerRankProgression.tierForStreakDays(100).pokemonName)
        assertEquals("Shadow Mewtwo", TrainerRankProgression.tierForStreakDays(175).pokemonName)
        assertEquals("Yveltal", TrainerRankProgression.tierForStreakDays(275).pokemonName)
        assertEquals("Arceus", TrainerRankProgression.tierForStreakDays(500).pokemonName)
    }

    @Test
    fun continuousXpLeveling_risesWithXpWhileClassStaysGovernedByStreakDays() {
        val base = HunterSystemState.createResetState().copy(
            playerStatus = PlayerStatus(
                level = 1,
                classLevel = 1,
                currentXp = 0,
                lifetimeXp = 0,
                streakDays = 4,
                bestStreakDays = 4,
                checkedInToday = true,
                lastProductiveDate = StreakDateUtils.todayDateString(),
                lastDailyResetDate = StreakDateUtils.todayDateString()
            )
        )

        // Earn 2,000 XP while remaining on Day 4 streak -> XP Level climbs to 5, Class stays Lv 1 Beginner (Pikachu)
        val (leveled, _) = base.applyScaledXpProgress(rawBaseXpDelta = 2000, isRawAlreadyScaled = true)
        assertEquals(5, leveled.playerStatus.level)
        assertEquals(2000, leveled.playerStatus.lifetimeXp)
        assertEquals(2000, leveled.playerStatus.currentXp)
        assertEquals(1, leveled.playerStatus.classLevel)
        assertEquals("Beginner", leveled.playerStatus.currentTier.classTitle)
    }

    @Test
    fun missedOneDay_flagsRestorableStreakAndAllows1000XpRestore() {
        val today = "2026-11-13"
        val yesterday = "2026-11-12"
        val twoDaysAgo = "2026-11-11"

        val preGapState = HunterSystemState.createResetState().copy(
            playerStatus = PlayerStatus(
                level = 4,
                classLevel = 2,
                rank = "Challenger",
                currentXp = 1500,
                lifetimeXp = 1500,
                streakDays = 25,
                bestStreakDays = 25,
                checkedInToday = false,
                lastProductiveDate = twoDaysAgo,
                lastDailyResetDate = twoDaysAgo,
                productiveHistoryDates = listOf(twoDaysAgo)
            )
        )

        // Missing 1 day (last logged 2026-11-11, logging in on 2026-11-13)
        val afterGap = preGapState.evaluateStreakOnLogin(
            todayDate = today,
            yesterdayDate = yesterday
        )

        assertEquals(0, afterGap.playerStatus.streakDays)
        assertEquals(0, afterGap.playerStatus.classLevel)
        assertEquals(25, afterGap.playerStatus.restorableStreakDays)
        assertEquals(yesterday, afterGap.playerStatus.missedStreakDate)
        assertTrue(afterGap.playerStatus.canRestoreMissedStreak)

        // Restore using 1,000 XP -> Streak returns to 25 (Challenger • Charizard), Lifetime XP preserved
        val restoreResult = afterGap.restoreLostStreak(todayDate = today)
        assertTrue(restoreResult.isSuccess)
        val restored = restoreResult.getOrThrow()

        assertEquals(25, restored.playerStatus.streakDays)
        assertEquals(2, restored.playerStatus.classLevel)
        assertEquals("Challenger", restored.playerStatus.rank)
        assertEquals(500, restored.playerStatus.currentXp)
        assertEquals(1500, restored.playerStatus.lifetimeXp)
        assertEquals(4, restored.playerStatus.level)
        assertEquals(0, restored.playerStatus.restorableStreakDays)
        assertTrue(restored.playerStatus.restoredDates.contains(yesterday))
    }

    @Test
    fun missedOneDayRestore_failsGracefullyWhenXpInsufficient() {
        val state = HunterSystemState.createResetState().copy(
            playerStatus = PlayerStatus(
                level = 2,
                currentXp = 650,
                lifetimeXp = 650,
                restorableStreakDays = 14,
                missedStreakDate = "2026-11-12"
            )
        )
        val result = state.restoreLostStreak(todayDate = "2026-11-13")
        assertTrue(result.isFailure)
        assertEquals(650, state.playerStatus.currentXp)
    }

    @Test
    fun otaPatchManifest_parsesAndAppliesRulesAndFoodPatchesWithoutApk() {
        val sampleJson = """
            {
              "patch_number": 10309,
              "patch_version": "1.0.3-patch.9",
              "xp_per_level": 600,
              "streak_restore_cost_xp": 900,
              "food_database_patches": [
                {
                  "keywords": ["ragi malt drink", "ragi malt"],
                  "display_name": "Ragi Malt Drink",
                  "description": "Warm finger millet drink",
                  "calories_per_100g": 78,
                  "carbs_per_100g": 15.0,
                  "fats_per_100g": 1.1,
                  "protein_per_100g": 2.4,
                  "fiber_per_100g": 1.8,
                  "micronutrients": "Calcium, Iron",
                  "grams_per_unit": 250,
                  "unit_name_singular": "glass (250ml)",
                  "unit_name_plural": "glasses",
                  "is_liquid_ml": true
                }
              ]
            }
        """.trimIndent()

        val parsed = PatchUpdateManager.parsePatchPayload(sampleJson, "UnitTest OTA")
        assertNotNull(parsed)
        PatchUpdateManager.applyParsedPayload(parsed!!)

        assertEquals(600, TrainerRankProgression.xpPerLevel)
        assertEquals(900, TrainerRankProgression.streakRestoreCostXp)
        assertEquals(2, TrainerRankProgression.xpLevelForLifetimeXp(650))

        val ragiProfile = SmartFoodNutritionEngine.findProfile("ragi malt drink")
        assertEquals("Ragi Malt Drink", ragiProfile.displayName)
        assertTrue(ragiProfile.isLiquidMl)
    }
}
