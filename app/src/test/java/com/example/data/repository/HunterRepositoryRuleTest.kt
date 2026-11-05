package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.HunterBiometrics
import com.example.data.model.HunterSystemState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class HunterRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveOrUpdateSession_authenticatedOwner_createsAndUpdatesSession() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = HunterRepository(firestore)
        val sessionId = "session_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val initialState = HunterSystemState()
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveOrUpdateSession(initialState, sessionId)
        }
        assertTrue("Create session should succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val updatedState = initialState.copy(
            tacticalBriefing = "Updated briefing after 2,000 kcal meal."
        )
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveOrUpdateSession(updatedState, sessionId)
        }
        assertTrue("Update session should succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)
    }

    @Test
    fun observeActiveSession_authenticatedOwner_emitsRealtimeSession() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = HunterRepository(firestore)
        val sessionId = "session_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveOrUpdateSession(HunterSystemState(), sessionId).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeActiveSession(sessionId).first { it != null }
        }
        assertNotNull(emitted)
        assertEquals(2000L, emitted?.caloriesLogged)
    }

    @Test
    fun getSessionById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = HunterRepository(firestore)
        val sessionId = "session_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveOrUpdateSession(HunterSystemState(), sessionId).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = HunterRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getSessionById(aliceUid, sessionId)
        }
        assertTrue("Cross-user read must fail", crossReadResult.isFailure)
        val fsEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun getSessionById_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = HunterRepository(firestore)
        val sessionId = "session_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveOrUpdateSession(HunterSystemState(), sessionId).getOrThrow()
        }

        auth.signOut()
        val unauthResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getSessionById(aliceUid, sessionId)
        }
        assertTrue("Unauthenticated read must fail", unauthResult.isFailure)
        val fsEx = unauthResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun appendAndObserveLogs_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = HunterRepository(firestore)

        val logRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.appendTacticalLog("i had 2000 kcal", "2000 kcal logged.", 80)
        }
        assertTrue("Appending log should succeed: ${logRes.exceptionOrNull()}", logRes.isSuccess)
        val logId = logRes.getOrThrow()

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLogs().first { list -> list.any { it.id == logId } }
        }
        assertTrue(logs.any { it.id == logId })
    }

    @Test
    fun appendObserveAndDeleteJournalEntry_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = HunterRepository(firestore)
        val bio = HunterBiometrics(
            hunterName = "Sung Jin-Woo",
            weightKg = 75.5,
            heightCm = 180.0,
            age = 24,
            biologicalSex = "MALE"
        )

        val entryRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.appendJournalEntry(
                entryType = "PHOTO_PROGRESS",
                title = "Physique Check-In",
                caption = "Logged 75.5 kg at 180 cm.",
                poseTag = "Front Double Bicep",
                photoBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                biometrics = bio
            )
        }
        assertTrue("Appending journal entry should succeed: ${entryRes.exceptionOrNull()}", entryRes.isSuccess)
        val entryId = entryRes.getOrThrow()

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeJournalEntries().first { list -> list.any { it.id == entryId } }
        }
        assertTrue(entries.any { it.id == entryId && it.weightKg == 75.5 })

        val delRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteJournalEntry(entryId)
        }
        assertTrue("Deleting journal entry should succeed: ${delRes.exceptionOrNull()}", delRes.isSuccess)
    }

    @Test
    fun submitObserveUpdateAndDeletePublicFeedback_succeedsForOwnerAndBlocksCrossUserMutation() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = HunterRepository(firestore)

        val submitRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.submitPublicFeedback(
                hunterName = "Shadow Vanguard",
                category = "FEATURE_REQUEST",
                rating = 5,
                message = "Please add a rest timer sound alert between progressive overload sets!"
            )
        }
        assertTrue("Submitting public feedback should succeed: ${submitRes.exceptionOrNull()}", submitRes.isSuccess)
        val fbId = submitRes.getOrThrow()

        val feedbackList = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observePublicFeedback().first { list -> list.any { it.id == fbId } }
        }
        assertTrue(feedbackList.any { it.id == fbId && it.rating == 5L })

        // Cross-user Bob can read public feedback, but cannot update or delete Alice's feedback
        signInTestUser(BOB_EMAIL)
        val bobRepo = HunterRepository(firestore)
        val bobUpdateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.updateFeedbackStatus(fbId, "IMPLEMENTED")
        }
        assertTrue("Non-owner non-creator update must fail", bobUpdateRes.isFailure)

        // Owner Alice can update status and delete her feedback
        signInTestUser(ALICE_EMAIL)
        val ownerUpdateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateFeedbackStatus(fbId, "IMPLEMENTED")
        }
        assertTrue("Owner status update should succeed: ${ownerUpdateRes.exceptionOrNull()}", ownerUpdateRes.isSuccess)

        val ownerDeleteRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.deleteFeedback(fbId)
        }
        assertTrue("Owner delete should succeed: ${ownerDeleteRes.exceptionOrNull()}", ownerDeleteRes.isSuccess)
    }

    @Test
    fun resetAllUserProgress_authenticatedOwner_resetsSessionToLevelOne() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = HunterRepository(firestore)
        val sessionId = "session_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val resetState = HunterSystemState.createResetState()
        val resetRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.resetAllUserProgress(
                resetState = resetState,
                clearJournalEntries = false,
                sessionId = sessionId
            )
        }
        assertTrue("Reset progress should succeed: ${resetRes.exceptionOrNull()}", resetRes.isSuccess)
        assertEquals(1L, resetRes.getOrThrow().level)
        assertEquals(0L, resetRes.getOrThrow().streakDays)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_hunter@test.com"
        const val BOB_EMAIL = "bob_hunter@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
