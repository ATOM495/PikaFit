package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AppReleaseInfo
import com.example.data.model.FeedbackEntry
import com.example.data.model.HunterBiometrics
import com.example.data.model.HunterSession
import com.example.data.model.HunterSystemState
import com.example.data.model.JournalEntry
import com.example.data.model.TacticalLog
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class HunterRepository(
    private val db: FirebaseFirestore,
    private val context: Context? = null,
    private val activeAccountId: String = LOCAL_DEV_USER_ID
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext,
        LOCAL_DEV_USER_ID
    )

    private val auth = Firebase.auth

    private fun currentUserIdOrNull(): String? = auth.currentUser?.uid

    private fun effectiveUserId(): String =
        auth.currentUser?.uid ?: activeAccountId.ifBlank { LOCAL_DEV_USER_ID }

    private fun getSessionFlowForUid(uid: String): MutableStateFlow<HunterSession?> {
        return localSessionsByUid.getOrPut(uid) {
            val cachedJson = getCachedStateJsonForUser(uid)
            val initialSession = if (!cachedJson.isNullOrBlank()) {
                HunterSession(
                    id = DEFAULT_SESSION_ID,
                    userId = uid,
                    stateJson = cachedJson,
                    createdAt = Timestamp(Date()),
                    updatedAt = Timestamp(Date())
                )
            } else {
                null
            }
            MutableStateFlow(initialSession)
        }
    }

    private fun getLogsFlowForUid(uid: String): MutableStateFlow<List<TacticalLog>> =
        localLogsByUid.getOrPut(uid) { MutableStateFlow(emptyList()) }

    private fun getJournalFlowForUid(uid: String): MutableStateFlow<List<JournalEntry>> =
        localJournalByUid.getOrPut(uid) { MutableStateFlow(emptyList()) }

    fun isOnboardingCompletedForUser(uid: String): Boolean {
        if (uid.isBlank()) return false
        if (onboardedUidsMemoryCache.contains(uid)) return true
        val prefs = context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
        val completed = prefs?.getBoolean("onboarded_$uid", false) == true
        if (completed) {
            onboardedUidsMemoryCache.add(uid)
        }
        return completed
    }

    fun setOnboardingCompletedForUser(
        uid: String,
        completed: Boolean,
        stateJsonSnapshot: String? = null
    ) {
        if (uid.isBlank()) return
        if (completed) {
            onboardedUidsMemoryCache.add(uid)
        } else {
            onboardedUidsMemoryCache.remove(uid)
        }
        if (!stateJsonSnapshot.isNullOrBlank()) {
            cachedStateJsonByUid[uid] = stateJsonSnapshot
        } else if (!completed) {
            cachedStateJsonByUid.remove(uid)
        }
        context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.apply {
                putBoolean("onboarded_$uid", completed)
                if (!stateJsonSnapshot.isNullOrBlank()) {
                    putString("state_json_$uid", stateJsonSnapshot)
                } else if (!completed) {
                    remove("state_json_$uid")
                }
            }
            ?.apply()
    }

    fun getCachedStateJsonForUser(uid: String): String? {
        if (uid.isBlank()) return null
        cachedStateJsonByUid[uid]?.let { return it }
        val prefs = context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
        return prefs?.getString("state_json_$uid", null)?.also {
            cachedStateJsonByUid[uid] = it
        }
    }

    fun getSavedAccountEmail(): String? {
        return context?.getSharedPreferences("pikafit_auth_session_prefs", Context.MODE_PRIVATE)
            ?.getString("user_account_email", null)
    }

    fun observeActiveSession(sessionId: String = DEFAULT_SESSION_ID): Flow<HunterSession?> = flow {
        val user = auth.currentUser
        val effUid = effectiveUserId()
        val localFlow = getSessionFlowForUid(effUid)
        if (user == null) {
            emitAll(localFlow)
            return@flow
        }
        try {
            user.getIdToken(false).await()
        } catch (_: Exception) {
            emitAll(localFlow)
            return@flow
        }
        val firebaseUid = user.uid
        val path = "users/$firebaseUid/sessions/$sessionId"
        val docRef = db.collection("users").document(firebaseUid).collection("sessions").document(sessionId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(HunterSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.also { session ->
                                if (session.stateJson.isNotBlank()) {
                                    setOnboardingCompletedForUser(firebaseUid, true, session.stateJson)
                                    localFlow.value = session
                                }
                            }
                    } else {
                        localFlow.value
                    }
                }
                .catch { error ->
                    val isUnauth = auth.currentUser == null
                    if (!isUnauth && error is Exception) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    emitAll(localFlow)
                }
        )
    }

    fun observeLogs(): Flow<List<TacticalLog>> = flow {
        val user = auth.currentUser
        val effUid = effectiveUserId()
        val localFlow = getLogsFlowForUid(effUid)
        if (user == null) {
            emitAll(localFlow)
            return@flow
        }
        try {
            user.getIdToken(false).await()
        } catch (_: Exception) {
            emitAll(localFlow)
            return@flow
        }
        val firebaseUid = user.uid
        val path = "users/$firebaseUid/logs"
        val colRef = db.collection("users").document(firebaseUid).collection("logs")
        emitAll(
            colRef.whereEqualTo("userId", firebaseUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TacticalLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val isUnauth = auth.currentUser == null
                    if (!isUnauth && error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    emitAll(localFlow)
                }
        )
    }

    fun observeJournalEntries(): Flow<List<JournalEntry>> = flow {
        val user = auth.currentUser
        val effUid = effectiveUserId()
        val localFlow = getJournalFlowForUid(effUid)
        if (user == null) {
            emitAll(localFlow)
            return@flow
        }
        try {
            user.getIdToken(false).await()
        } catch (_: Exception) {
            emitAll(localFlow)
            return@flow
        }
        val firebaseUid = user.uid
        val path = "users/$firebaseUid/journal"
        val colRef = db.collection("users").document(firebaseUid).collection("journal")
        emitAll(
            colRef.whereEqualTo("userId", firebaseUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(JournalEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val isUnauth = auth.currentUser == null
                    if (!isUnauth && error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    emitAll(localFlow)
                }
        )
    }

    fun observePublicFeedback(): Flow<List<FeedbackEntry>> = flow {
        val user = auth.currentUser
        if (user == null) {
            emitAll(localFeedbackFlow)
            return@flow
        }
        try {
            user.getIdToken(false).await()
        } catch (_: Exception) {
            emitAll(localFeedbackFlow)
            return@flow
        }
        val path = "feedback"
        val colRef = db.collection("feedback")
        emitAll(
            colRef.whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FeedbackEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val isUnauth = auth.currentUser == null
                    if (!isUnauth && error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    emitAll(localFeedbackFlow)
                }
        )
    }

    suspend fun saveOrUpdateSession(
        state: HunterSystemState,
        sessionId: String = DEFAULT_SESSION_ID
    ): Result<HunterSession> {
        val effUid = effectiveUserId()
        val localFlow = getSessionFlowForUid(effUid)
        val nowTs = Timestamp(Date())
        val existingCreatedAt = localFlow.value?.createdAt ?: nowTs
        val localModel = HunterSession.fromSystemState(
            id = sessionId,
            userId = effUid,
            state = state,
            existingCreatedAt = existingCreatedAt
        ).copy(updatedAt = nowTs)
        localFlow.value = localModel
        if (state.biometrics.onboardingCompleted) {
            setOnboardingCompletedForUser(effUid, true, localModel.stateJson)
        }

        val firebaseUid = currentUserIdOrNull() ?: return Result.success(localModel)
        val docRef = db.collection("users").document(firebaseUid).collection("sessions").document(sessionId)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val remoteCreatedAt = existingSnap.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val updatedModel = HunterSession.fromSystemState(
                    id = sessionId,
                    userId = firebaseUid,
                    state = state,
                    existingCreatedAt = remoteCreatedAt
                )
                docRef.set(updatedModel.copy(updatedAt = null)).await()
                Result.success(updatedModel)
            } else {
                val newModel = HunterSession.fromSystemState(
                    id = sessionId,
                    userId = firebaseUid,
                    state = state,
                    existingCreatedAt = null
                )
                docRef.set(newModel).await()
                Result.success(newModel)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.success(localModel)
        }
    }

    suspend fun getSessionById(targetUserId: String, sessionId: String = DEFAULT_SESSION_ID): Result<HunterSession?> {
        val docRef = db.collection("users").document(targetUserId).collection("sessions").document(sessionId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                if (snap.exists()) {
                    snap.toObject(HunterSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun appendTacticalLog(
        commandText: String,
        briefingResponse: String,
        xpDelta: Int
    ): Result<String> {
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val effUid = effectiveUserId()
        val localFlow = getLogsFlowForUid(effUid)
        val localLog = TacticalLog(
            id = logId,
            userId = effUid,
            commandText = commandText.take(500).ifBlank { "System sync" },
            briefingResponse = briefingResponse.take(1900).ifBlank { "Briefing logged." },
            xpDelta = xpDelta.toLong().coerceIn(-10000L, 100000L),
            createdAt = Timestamp(Date())
        )
        localFlow.value = listOf(localLog) + localFlow.value

        val firebaseUid = currentUserIdOrNull() ?: return Result.success(logId)
        val docRef = db.collection("users").document(firebaseUid).collection("logs").document(logId)
        return try {
            val log = localLog.copy(userId = firebaseUid, createdAt = null)
            docRef.set(log).await()
            Result.success(logId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.success(logId)
        }
    }

    suspend fun appendJournalEntry(
        entryType: String,
        title: String,
        caption: String,
        poseTag: String,
        photoBase64: String,
        biometrics: HunterBiometrics,
        createdAtOverride: Timestamp? = null
    ): Result<String> {
        val entryId = "jrn_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val cleanType = if (entryType == "BIOMETRIC_UPDATE") "BIOMETRIC_UPDATE" else "PHOTO_PROGRESS"
        val effUid = effectiveUserId()
        val localFlow = getJournalFlowForUid(effUid)
        val localEntry = JournalEntry(
            id = entryId,
            userId = effUid,
            entryType = cleanType,
            title = title.trim().take(160).ifBlank { "Hunter Progress Entry" },
            caption = caption.trim().take(2000).ifBlank { "Biometric & physique progress recorded." },
            poseTag = poseTag.trim().take(80).ifBlank { "Biometric Sync" },
            photoBase64 = photoBase64.take(240000),
            weightKg = biometrics.weightKg.coerceIn(20.0, 400.0),
            heightCm = biometrics.heightCm.coerceIn(80.0, 260.0),
            age = biometrics.age.toLong().coerceIn(10L, 120L),
            bmi = biometrics.bmi.coerceIn(5.0, 120.0),
            bodyFatPercent = biometrics.bodyFatPercent.coerceIn(1.0, 80.0),
            muscleMassPercent = biometrics.muscleMassPercent.coerceIn(10.0, 95.0),
            createdAt = createdAtOverride ?: Timestamp(Date())
        )
        localFlow.value = (listOf(localEntry) + localFlow.value)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }

        val firebaseUid = currentUserIdOrNull() ?: return Result.success(entryId)
        val docRef = db.collection("users").document(firebaseUid).collection("journal").document(entryId)
        return try {
            val remoteEntry = localEntry.copy(userId = firebaseUid, createdAt = createdAtOverride)
            docRef.set(remoteEntry).await()
            Result.success(entryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.success(entryId)
        }
    }

    suspend fun deleteJournalEntry(entryId: String): Result<Unit> {
        val effUid = effectiveUserId()
        val localFlow = getJournalFlowForUid(effUid)
        localFlow.value = localFlow.value.filterNot { it.id == entryId }

        val firebaseUid = currentUserIdOrNull() ?: return Result.success(Unit)
        val docRef = db.collection("users").document(firebaseUid).collection("journal").document(entryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.success(Unit)
        }
    }

    suspend fun submitPublicFeedback(
        hunterName: String,
        category: String,
        rating: Int,
        message: String
    ): Result<String> {
        val fbId = "fb_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val allowedCategories = setOf("FEATURE_REQUEST", "BUG_REPORT", "UI_UX", "GENERAL")
        val cleanCategory = if (category in allowedCategories) category else "GENERAL"
        val effUid = effectiveUserId()
        val localEntry = FeedbackEntry(
            id = fbId,
            userId = effUid,
            hunterName = hunterName.trim().take(80).ifBlank { "Anonymous Hunter" },
            category = cleanCategory,
            rating = rating.toLong().coerceIn(1L, 5L),
            message = message.trim().take(1500).ifBlank { "Great app experience!" },
            status = "OPEN",
            visibility = "PUBLIC",
            createdAt = Timestamp(Date())
        )
        localFeedbackFlow.value = listOf(localEntry) + localFeedbackFlow.value

        val firebaseUid = currentUserIdOrNull() ?: return Result.success(fbId)
        val docRef = db.collection("feedback").document(fbId)
        return try {
            val remoteEntry = localEntry.copy(userId = firebaseUid, createdAt = null)
            docRef.set(remoteEntry).await()
            Result.success(fbId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.success(fbId)
        }
    }

    suspend fun updateFeedbackStatus(feedbackId: String, newStatus: String): Result<Unit> {
        val allowedStatuses = setOf("OPEN", "IN_REVIEW", "IMPLEMENTED")
        val cleanStatus = if (newStatus in allowedStatuses) newStatus else "IN_REVIEW"
        localFeedbackFlow.value = localFeedbackFlow.value.map {
            if (it.id == feedbackId) it.copy(status = cleanStatus) else it
        }
        val firebaseUid = currentUserIdOrNull() ?: return Result.success(Unit)
        val docRef = db.collection("feedback").document(feedbackId)
        return try {
            docRef.update("status", cleanStatus).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.success(Unit)
        }
    }

    suspend fun deleteFeedback(feedbackId: String): Result<Unit> {
        localFeedbackFlow.value = localFeedbackFlow.value.filterNot { it.id == feedbackId }
        val firebaseUid = currentUserIdOrNull() ?: return Result.success(Unit)
        val docRef = db.collection("feedback").document(feedbackId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.success(Unit)
        }
    }

    suspend fun resetAllUserProgress(
        resetState: HunterSystemState,
        clearJournalEntries: Boolean = true,
        sessionId: String = DEFAULT_SESSION_ID
    ): Result<HunterSession> {
        val effUid = effectiveUserId()
        getLogsFlowForUid(effUid).value = emptyList()
        if (clearJournalEntries) {
            getJournalFlowForUid(effUid).value = emptyList()
        }
        val sessionRes = saveOrUpdateSession(resetState, sessionId)

        val firebaseUid = currentUserIdOrNull() ?: return sessionRes
        return try {
            val logsCol = db.collection("users").document(firebaseUid).collection("logs")
            val logsSnap = logsCol.whereEqualTo("userId", firebaseUid).get().await()
            for (doc in logsSnap.documents) {
                doc.reference.delete().await()
            }

            if (clearJournalEntries) {
                val journalCol = db.collection("users").document(firebaseUid).collection("journal")
                val journalSnap = journalCol.whereEqualTo("userId", firebaseUid).get().await()
                for (doc in journalSnap.documents) {
                    doc.reference.delete().await()
                }
            }
            sessionRes
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$firebaseUid/sessions/$sessionId")
            sessionRes
        }
    }

    fun observeAppReleaseInfo(): Flow<AppReleaseInfo> = callbackFlow {
        val listener = db.collection("system").document("app_version")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppReleaseInfo())
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val info = snapshot.toObject(AppReleaseInfo::class.java) ?: AppReleaseInfo()
                    trySend(info)
                } else {
                    trySend(AppReleaseInfo())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun setAppReleaseInfo(releaseInfo: AppReleaseInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("system").document("app_version")
                .set(releaseInfo)
                .await()
            true
        } catch (e: Exception) {
            Log.e("HunterRepository", "Failed to update system release metadata", e)
            false
        }
    }

    companion object {
        const val DEFAULT_SESSION_ID = "active_hunter_session"
        private const val LOCAL_DEV_USER_ID = "local_dev_hunter"
        private const val PREFS_ONBOARDING_NAME = "pikafit_account_onboarding_prefs"

        private val onboardedUidsMemoryCache = mutableSetOf<String>()
        private val cachedStateJsonByUid = mutableMapOf<String, String>()

        private val localSessionsByUid = mutableMapOf<String, MutableStateFlow<HunterSession?>>()
        private val localLogsByUid = mutableMapOf<String, MutableStateFlow<List<TacticalLog>>>()
        private val localJournalByUid = mutableMapOf<String, MutableStateFlow<List<JournalEntry>>>()
        private val localFeedbackFlow = MutableStateFlow<List<FeedbackEntry>>(emptyList())
    }
}
