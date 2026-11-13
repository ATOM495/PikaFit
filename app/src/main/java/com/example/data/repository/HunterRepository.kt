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
import com.example.data.model.OtaPatchManifest
import com.example.data.model.TacticalLog
import com.example.util.PatchApplyResult
import com.example.util.PatchUpdateManager
import com.example.util.SmartFoodNutritionEngine
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.UUID

class HunterRepository(
    dbInstance: FirebaseFirestore? = null,
    private val context: Context? = null,
    private val activeAccountId: String = LOCAL_DEV_USER_ID
) {

    constructor(context: Context) : this(
        dbInstance = null,
        context = context.applicationContext,
        activeAccountId = LOCAL_DEV_USER_ID
    )

    constructor(context: Context, activeAccountId: String) : this(
        dbInstance = null,
        context = context.applicationContext,
        activeAccountId = activeAccountId
    )

    private val db: FirebaseFirestore by lazy {
        dbInstance ?: FirebaseFirestore.getInstance(
            requireNotNull(context).applicationContext.getString(R.string.firestore_database_id)
        )
    }

    private val auth by lazy { Firebase.auth }

    fun appContextOrNull(): Context? = context

    fun loadCachedOtaPatch(): OtaPatchManifest {
        val loaded = PatchUpdateManager.loadAndApplyCachedPatch(context)
        localOtaPatchFlow.value = loaded
        return loaded
    }

    fun observeOtaPatchManifest(): Flow<OtaPatchManifest> = localOtaPatchFlow

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
            val firebaseUid = user.uid
            if (localFlow.value == null) {
                val docRef = db.collection("users").document(firebaseUid).collection("sessions").document(sessionId)
                val snap = docRef.get().await()
                if (snap.exists()) {
                    snap.toObject(HunterSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?.also { session ->
                            if (session.stateJson.isNotBlank()) {
                                setOnboardingCompletedForUser(firebaseUid, true, session.stateJson)
                                localFlow.value = session
                            }
                        }
                }
            }
        } catch (_: Exception) {
            // Fall through to localFlow
        }
        emitAll(localFlow)
    }.flowOn(Dispatchers.IO)

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
    }.flowOn(Dispatchers.IO)

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
    }.flowOn(Dispatchers.IO)

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
    }.flowOn(Dispatchers.IO)

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
        setOnboardingCompletedForUser(effUid, true, localModel.stateJson)

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
                val updateFields = mapOf(
                    "level" to updatedModel.level,
                    "rank" to updatedModel.rank,
                    "currentXp" to updatedModel.currentXp,
                    "nextLevelXp" to updatedModel.nextLevelXp,
                    "progressPercent" to updatedModel.progressPercent,
                    "hpPercent" to updatedModel.hpPercent,
                    "streakDays" to updatedModel.streakDays,
                    "caloriesLogged" to updatedModel.caloriesLogged,
                    "caloriesTarget" to updatedModel.caloriesTarget,
                    "proteinG" to updatedModel.proteinG,
                    "carbsG" to updatedModel.carbsG,
                    "fatsG" to updatedModel.fatsG,
                    "fiberG" to updatedModel.fiberG,
                    "omega3Mg" to updatedModel.omega3Mg,
                    "waterLiters" to updatedModel.waterLiters,
                    "targetLiters" to updatedModel.targetLiters,
                    "workoutCompleted" to updatedModel.workoutCompleted,
                    "tacticalBriefing" to updatedModel.tacticalBriefing,
                    "stateJson" to updatedModel.stateJson,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                docRef.update(updateFields).await()
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
            title = title.trim().take(160).ifBlank { "Trainer Progress Entry" },
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
            hunterName = hunterName.trim().take(80).ifBlank { "Anonymous Trainer" },
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

    fun getInstalledVersionName(): String {
        val prefs = context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
        val savedVer = prefs?.getString(KEY_INSTALLED_VERSION_NAME, null)?.trim().orEmpty()
        if (savedVer.isNotBlank() && savedVer != "1.1") {
            return savedVer
        }
        val rawBuildVer = try { com.example.BuildConfig.VERSION_NAME } catch (_: Exception) { "1.0.2" }
        return if (rawBuildVer == "1.1" || rawBuildVer == "1.0") "1.0.2" else rawBuildVer
    }

    fun getInstalledVersionCode(): Long {
        val buildCode = try { com.example.BuildConfig.VERSION_CODE.toLong() } catch (_: Exception) { 2L }
        val prefs = context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs?.getLong(KEY_INSTALLED_VERSION_CODE, 0L) ?: 0L
        return if (savedCode > 0L) savedCode else buildCode
    }

    fun markVersionInstalled(versionName: String, versionCode: Long, publishedAt: String = "") {
        val cleanVer = versionName.trim().removePrefix("v").removePrefix("V").ifBlank { getInstalledVersionName() }
        val cleanCode = deriveVersionCode(cleanVer, maxOf(versionCode, getInstalledVersionCode()))
        val prefs = context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.apply {
            putString(KEY_INSTALLED_VERSION_NAME, cleanVer)
            putLong(KEY_INSTALLED_VERSION_CODE, cleanCode)
            if (publishedAt.isNotBlank()) {
                putString(KEY_ACK_PUBLISHED_AT, publishedAt)
            }
            apply()
        }
        val current = localReleaseInfoFlow.value
        localReleaseInfoFlow.value = current.copy(
            latestVersionName = cleanVer,
            latestVersionCode = cleanCode,
            isUpdateAvailable = false
        )
    }

    suspend fun syncAndApplyOtaPatch(): PatchApplyResult = withContext(Dispatchers.IO) {
        val firestorePatchJson = try {
            val snap = db.collection("system").document("app_patch").get().await()
            if (snap.exists()) {
                snap.getString("rawJson")?.takeIf { it.isNotBlank() }
                    ?: snap.getString("manifestJson")?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
        val result = PatchUpdateManager.fetchAndApplyPatchFromGitHub(
            context = context,
            firestorePatchJson = firestorePatchJson
        )
        localOtaPatchFlow.value = result.manifest
        result
    }

    suspend fun publishOtaPatchToFirestore(rawJson: String): Boolean = withContext(Dispatchers.IO) {
        val applied = PatchUpdateManager.saveAndApplyRawJson(
            context = context,
            rawJson = rawJson,
            sourceLabel = "Cloud Patch Broadcast"
        ) ?: return@withContext false
        localOtaPatchFlow.value = applied.manifest
        localReleaseInfoFlow.value = localReleaseInfoFlow.value.copy(
            patchNumber = applied.manifest.patchNumber,
            patchVersion = applied.manifest.patchVersion,
            patchAppliedAutomatically = true,
            patchedFoodsCount = SmartFoodNutritionEngine.getPatchedFoodCount(),
            announcementBanner = applied.manifest.announcementBanner
        )
        try {
            db.collection("system").document("app_patch")
                .set(
                    mapOf(
                        "rawJson" to rawJson.trim(),
                        "patchNumber" to applied.manifest.patchNumber,
                        "patchVersion" to applied.manifest.patchVersion,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            Log.w("HunterRepository", "Cloud app_patch publish failed (applied locally): ${e.message}")
            true
        }
    }

    suspend fun checkGitHubAndFirestoreForUpdates(): AppReleaseInfo = withContext(Dispatchers.IO) {
        val installedVer = getInstalledVersionName()
        val installedCode = getInstalledVersionCode()
        val prefs = context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
        val lastAckPublishedAt = prefs?.getString(KEY_ACK_PUBLISHED_AT, "") ?: ""

        // 1. Automatically fetch & hot-apply GitHub OTA patch (patch_manifest.json + release patch JSON assets)
        val patchResult = syncAndApplyOtaPatch()
        val patchManifest = patchResult.manifest

        // 2. Query GitHub Releases API (https://api.github.com/repos/ATOM495/PikaFit/releases)
        val githubInfo = fetchLatestGitHubRelease(installedVer, installedCode, lastAckPublishedAt)

        // 3. Query Firestore system/app_version if available
        val firestoreInfo = try {
            val snap = db.collection("system").document("app_version").get().await()
            if (snap.exists()) snap.toObject(AppReleaseInfo::class.java) else null
        } catch (_: Exception) {
            null
        }

        val rawResolved = when {
            githubInfo != null && firestoreInfo != null -> {
                if (githubInfo.isUpdateAvailable) {
                    githubInfo
                } else {
                    val fsVerClean = firestoreInfo.latestVersionName.trim().removePrefix("v").removePrefix("V")
                    val fsNewer = firestoreInfo.latestVersionCode > installedCode ||
                        compareVersionStrings(fsVerClean, installedVer) > 0
                    firestoreInfo.copy(
                        latestVersionName = fsVerClean.ifBlank { installedVer },
                        isUpdateAvailable = fsNewer
                    )
                }
            }
            githubInfo != null -> githubInfo
            firestoreInfo != null -> {
                val fsVerClean = firestoreInfo.latestVersionName.trim().removePrefix("v").removePrefix("V")
                val fsNewer = firestoreInfo.latestVersionCode > installedCode ||
                    compareVersionStrings(fsVerClean, installedVer) > 0
                firestoreInfo.copy(
                    latestVersionName = fsVerClean.ifBlank { installedVer },
                    isUpdateAvailable = fsNewer
                )
            }
            else -> AppReleaseInfo(
                latestVersionCode = installedCode,
                latestVersionName = installedVer,
                releaseTitle = "PikaFit v$installedVer",
                releaseNotes = patchManifest.patchNotes.ifBlank {
                    "You are running PikaFit v$installedVer with auto-patch v${patchManifest.patchVersion}."
                },
                downloadUrl = "https://raw.githubusercontent.com/ATOM495/PikaFit/main/patch_manifest.json",
                isUpdateAvailable = false
            )
        }

        // If the remote release is an OTA-only patch (tag contains "patch" or no native bump),
        // keep isUpdateAvailable = false and let the silent OTA patch engine handle it!
        val isNativeMajorBump = rawResolved.isUpdateAvailable &&
            !rawResolved.latestVersionName.contains("patch", ignoreCase = true) &&
            rawResolved.latestVersionCode > installedCode
        val resolved = rawResolved.copy(
            patchNumber = patchManifest.patchNumber,
            patchVersion = patchManifest.patchVersion,
            patchAppliedAutomatically = patchResult.appliedFromRemote,
            announcementBanner = patchManifest.announcementBanner,
            patchedFoodsCount = SmartFoodNutritionEngine.getPatchedFoodCount(),
            releaseTitle = if (patchResult.appliedFromRemote && rawResolved.releaseTitle.isBlank()) {
                patchManifest.releaseTitle
            } else {
                rawResolved.releaseTitle
            },
            releaseNotes = if (patchResult.newlyApplied && patchManifest.patchNotes.isNotBlank()) {
                patchManifest.patchNotes
            } else {
                rawResolved.releaseNotes
            },
            isUpdateAvailable = isNativeMajorBump
        )

        localReleaseInfoFlow.value = resolved
        resolved
    }

    private fun fetchLatestGitHubRelease(
        installedVer: String,
        installedCode: Long,
        lastAckPublishedAt: String
    ): AppReleaseInfo? {
        val endpoints = listOf(
            "https://api.github.com/repos/ATOM495/PikaFit/releases/latest",
            "https://api.github.com/repos/ATOM495/PikaFit/releases?per_page=5"
        )
        for (endpoint in endpoints) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "PikafitPro-Android-Updater")
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    val bodyText = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                    val releaseObj: JSONObject? = if (bodyText.startsWith("[")) {
                        val arr = JSONArray(bodyText)
                        if (arr.length() > 0) arr.optJSONObject(0) else null
                    } else if (bodyText.startsWith("{")) {
                        JSONObject(bodyText)
                    } else null

                    if (releaseObj != null) {
                        val rawTag = releaseObj.optString("tag_name", "").trim()
                        val cleanTag = rawTag.removePrefix("v").removePrefix("V").trim()
                        if (cleanTag.isNotBlank()) {
                            val title = releaseObj.optString("name", "").trim().ifBlank { "PikaFit Release $rawTag" }
                            val notes = releaseObj.optString("body", "").trim()
                                .ifBlank { "GitHub OTA patch $rawTag is synced automatically." }
                            val htmlUrl = releaseObj.optString("html_url", "https://github.com/ATOM495/PikaFit/releases")
                            val publishedAt = releaseObj.optString("published_at", "")

                            var apkAssetUrl = ""
                            var patchJsonAssetUrl = ""
                            val assetsArr = releaseObj.optJSONArray("assets")
                            if (assetsArr != null) {
                                for (i in 0 until assetsArr.length()) {
                                    val asset = assetsArr.optJSONObject(i) ?: continue
                                    val assetName = asset.optString("name", "").lowercase()
                                    val dlUrl = asset.optString("browser_download_url", "")
                                    if (dlUrl.isNotBlank()) {
                                        if (assetName.endsWith(".apk") && apkAssetUrl.isBlank()) {
                                            apkAssetUrl = dlUrl
                                        } else if (assetName.endsWith(".json") && patchJsonAssetUrl.isBlank()) {
                                            patchJsonAssetUrl = dlUrl
                                        }
                                    }
                                }
                            }

                            val semCompare = compareVersionStrings(cleanTag, installedVer)
                            val isPatchOnlyRelease = cleanTag.contains("patch", ignoreCase = true) ||
                                (patchJsonAssetUrl.isNotBlank() && apkAssetUrl.isBlank())
                            val updateAvailable = semCompare > 0 &&
                                !isPatchOnlyRelease &&
                                (lastAckPublishedAt.isBlank() || publishedAt != lastAckPublishedAt)
                            val computedCode = if (updateAvailable) {
                                maxOf(installedCode + 1L, deriveVersionCode(cleanTag, installedCode + 1L))
                            } else {
                                deriveVersionCode(cleanTag, installedCode)
                            }

                            return AppReleaseInfo(
                                latestVersionCode = computedCode,
                                latestVersionName = cleanTag,
                                releaseTitle = title,
                                releaseNotes = notes.take(1500),
                                downloadUrl = apkAssetUrl.ifBlank {
                                    patchJsonAssetUrl.ifBlank {
                                        htmlUrl.ifBlank { "https://github.com/ATOM495/PikaFit/releases" }
                                    }
                                },
                                isMandatory = false,
                                publishedAt = publishedAt,
                                isUpdateAvailable = updateAvailable
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("HunterRepository", "GitHub release check notice for $endpoint: ${e.message}")
            }
        }
        return null
    }

    private fun compareVersionStrings(v1: String, v2: String): Int {
        val parts1 = v1.trim().removePrefix("v").removePrefix("V")
            .split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        val parts2 = v2.trim().removePrefix("v").removePrefix("V")
            .split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val a = parts1.getOrElse(i) { 0 }
            val b = parts2.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    private fun deriveVersionCode(ver: String, fallback: Long): Long {
        val parts = ver.trim().removePrefix("v").removePrefix("V")
            .split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toLongOrNull() }
        if (parts.isEmpty()) return fallback
        val major = parts.getOrElse(0) { 1L }
        val minor = parts.getOrElse(1) { 0L }
        val patch = parts.getOrElse(2) { 0L }
        return (major * 100L + minor * 10L + patch).coerceAtLeast(fallback)
    }

    fun observeAppReleaseInfo(): Flow<AppReleaseInfo> = callbackFlow {
        trySend(localReleaseInfoFlow.value)

        // Defer network/Firestore update check until after initial UI frames have settled
        launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(900)
            val checked = checkGitHubAndFirestoreForUpdates()
            trySend(checked)
        }

        val flowJob = launch(Dispatchers.IO) {
            localReleaseInfoFlow.collect { info ->
                trySend(info)
            }
        }

        val listener = db.collection("system").document("app_version")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    launch(Dispatchers.IO) {
                        val info = snapshot.toObject(AppReleaseInfo::class.java) ?: return@launch
                        val installedVer = getInstalledVersionName()
                        val installedCode = getInstalledVersionCode()
                        val cleanVer = info.latestVersionName.trim().removePrefix("v").removePrefix("V").ifBlank { installedVer }
                        val isNewer = info.latestVersionCode > installedCode || compareVersionStrings(cleanVer, installedVer) > 0
                        val merged = info.copy(
                            latestVersionName = cleanVer,
                            isUpdateAvailable = isNewer || localReleaseInfoFlow.value.isUpdateAvailable
                        )
                        localReleaseInfoFlow.value = merged
                        trySend(merged)
                    }
                }
            }
        awaitClose {
            flowJob.cancel()
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun setAppReleaseInfo(releaseInfo: AppReleaseInfo): Boolean = withContext(Dispatchers.IO) {
        val cleanVer = releaseInfo.latestVersionName.trim().removePrefix("v").removePrefix("V").ifBlank { "1.2" }
        val updatedInfo = releaseInfo.copy(
            latestVersionName = cleanVer,
            isUpdateAvailable = true
        )
        localReleaseInfoFlow.value = updatedInfo
        try {
            db.collection("system").document("app_version")
                .set(updatedInfo)
                .await()
            true
        } catch (e: Exception) {
            Log.e("HunterRepository", "Failed to update system release metadata", e)
            true
        }
    }

    companion object {
        const val DEFAULT_SESSION_ID = "active_hunter_session"
        private const val LOCAL_DEV_USER_ID = "local_dev_hunter"
        private const val PREFS_ONBOARDING_NAME = "pikafit_account_onboarding_prefs"
        private const val KEY_INSTALLED_VERSION_NAME = "installed_version_name"
        private const val KEY_INSTALLED_VERSION_CODE = "installed_version_code"
        private const val KEY_ACK_PUBLISHED_AT = "ack_github_release_published_at"

        private val onboardedUidsMemoryCache = mutableSetOf<String>()
        private val cachedStateJsonByUid = mutableMapOf<String, String>()

        private val localSessionsByUid = mutableMapOf<String, MutableStateFlow<HunterSession?>>()
        private val localLogsByUid = mutableMapOf<String, MutableStateFlow<List<TacticalLog>>>()
        private val localJournalByUid = mutableMapOf<String, MutableStateFlow<List<JournalEntry>>>()
        private val localFeedbackFlow = MutableStateFlow<List<FeedbackEntry>>(emptyList())
        private val localReleaseInfoFlow = MutableStateFlow(AppReleaseInfo())
        private val localOtaPatchFlow = MutableStateFlow(PatchUpdateManager.defaultBuiltInManifest())
    }
}
