package com.example

import com.example.data.model.HunterSystemState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun dailyStreak_loginAloneDoesNotCheckInOrIncrementStreak() {
        val today = com.example.data.model.StreakDateUtils.todayDateString()
        val yesterday = com.example.data.model.StreakDateUtils.daysAgoDateString(1)
        val initial = HunterSystemState().evaluateStreakOnLogin(
            todayDate = today,
            yesterdayDate = yesterday
        )
        assertFalse("Login alone must not mark user as checked in", initial.playerStatus.checkedInToday)
        assertEquals("Streak days must remain unchanged on login alone", 0, initial.playerStatus.streakDays)
        assertEquals(0, initial.playerStatus.classLevel)
        assertEquals(0, initial.playerStatus.todayFoodInputs)
        assertEquals(0, initial.playerStatus.todayExerciseInputs)
    }

    @Test
    fun dailyStreak_foodOrExerciseInputChecksInAndIncrementsOncePerDay() {
        val today = com.example.data.model.StreakDateUtils.todayDateString()
        val yesterday = com.example.data.model.StreakDateUtils.daysAgoDateString(1)
        val afterLogin = HunterSystemState().evaluateStreakOnLogin(
            todayDate = today,
            yesterdayDate = yesterday
        )

        // 1. User inputs food data -> checks in and increments streak 0 -> 1 (Promotes to Level 1 Beginner • Pikachu)
        val (afterFood, newlyCheckedIn1) = afterLogin.applyProductiveInput(
            isFoodInput = true,
            inputSummary = "Chicken & Rice (+600 kcal)",
            todayDate = today
        )
        assertTrue(newlyCheckedIn1)
        assertTrue(afterFood.playerStatus.checkedInToday)
        assertEquals(1, afterFood.playerStatus.streakDays)
        assertEquals(1, afterFood.playerStatus.classLevel)
        assertEquals("Beginner", afterFood.playerStatus.currentTier.classTitle)
        assertEquals(1, afterFood.playerStatus.todayFoodInputs)

        // 2. User inputs exercise data on the same day -> increments exercise counter, keeps streak at 1
        val (afterExercise, newlyCheckedIn2) = afterFood.applyProductiveInput(
            isFoodInput = false,
            inputSummary = "Bench Press 3x10",
            todayDate = today
        )
        assertFalse(newlyCheckedIn2)
        assertTrue(afterExercise.playerStatus.checkedInToday)
        assertEquals(1, afterExercise.playerStatus.streakDays)
        assertEquals(1, afterExercise.playerStatus.todayFoodInputs)
        assertEquals(1, afterExercise.playerStatus.todayExerciseInputs)
    }

    @Test
    fun resetProgress_resetsHunterStateToLevelOneAndZeroStreak() {
        val reset = HunterSystemState.createResetState(
            caloriesTarget = 2700,
            waterTargetLiters = 4.5
        )
        assertEquals(1, reset.playerStatus.level)
        assertEquals(0, reset.playerStatus.classLevel)
        assertEquals(0, reset.playerStatus.currentXp)
        assertEquals(0, reset.playerStatus.streakDays)
        assertFalse(reset.playerStatus.checkedInToday)
        assertEquals(0, reset.statBreakdown.vitality.caloriesLogged)
        assertEquals(2700, reset.statBreakdown.vitality.caloriesTarget)
        assertEquals(0.0, reset.statBreakdown.stamina.waterLiters, 0.01)
        assertEquals(4.5, reset.statBreakdown.stamina.targetLiters, 0.01)
    }
}
