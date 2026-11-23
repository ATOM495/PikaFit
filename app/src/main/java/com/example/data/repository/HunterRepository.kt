package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AppReleaseInfo
import com.example.data.model.CloudSyncState
import com.example.data.model.CloudSyncStatus
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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

    fun observeCloudSyncStatus(): StateFlow<CloudSyncStatus> = cloudSyncStatusFlow.asStateFlow()

    fun isFirebaseAuthenticated(): Boolean = auth.currentUser != null

    private fun currentUserIdOrNull(): String? = auth.currentUser?.uid

    private fun effectiveUserId(): String =
        auth.currentUser?.uid ?: activeAccountId.ifBlank { LOCAL_DEV_USER_ID }

    private fun updateCloudSyncStatus(
        state: CloudSyncState,
        detail: String,
        syncedNow: Boolean = false
    ) {
        val email = auth.currentUser?.email ?: getSavedAccountEmail().orEmpty()
        val prev = cloudSyncStatusFlow.value
        cloudSyncStatusFlow.value = CloudSyncStatus(
            state = state,
            lastSyncedAtMillis = if (syncedNow) System.currentTimeMillis() else prev.lastSyncedAtMillis,
            accountEmail = email,
            detailMessage = detail
        )
    }

    private fun hasUnsyncedLocalChanges(uid: String): Boolean {
        if (uid.isBlank()) return false
        if (unsyncedLocalUids.contains(uid)) return true
        val prefs = context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
        val flag = prefs?.getBoolean("unsynced_local_$uid", false) == true
        if (flag) unsyncedLocalUids.add(uid)
        return flag
    }

    private fun setHasUnsyncedLocalChanges(uid: String, unsynced: Boolean) {
        if (uid.isBlank()) return
        if (unsynced) unsyncedLocalUids.add(uid) else unsyncedLocalUids.remove(uid)
        context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("unsynced_local_$uid", unsynced)
            ?.apply()
    }

    private fun getSessionFlowForUid(uid: String): MutableStateFlow<HunterSession?> {
        return localSessionsByUid.getOrPut(uid) {
            val cachedJson = getCachedStateJsonForUser(uid)
            val initialSession = if (!cachedJson.isNullOrBlank()) {
                HunterSession(
                    id = DEFAULT_SESSION_ID,
                    userId = uid,
                    stateJson = cachedJson,
                    createdAt = null,
                    updatedAt = null
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

    private fun hasLocalDivergedDataNotInRemote(
        local: HunterSystemState,
        remote: HunterSystemState
    ): Boolean {
        val localPs = local.playerStatus
        val remotePs = remote.playerStatus
        if (remotePs.lastResetAtMillis > localPs.lastResetAtMillis &&
            remotePs.lastResetAtMillis >= localPs.stateUpdatedAtMillis
        ) {
            return false
        }
        val remoteHistory = remotePs.productiveHistoryDates.toSet()
        val hasExtraProductiveDate = localPs.productiveHistoryDates.any { it.isNotBlank() && it !in remoteHistory }
        if (hasExtraProductiveDate) return true

        val hasRicherDailyLog = localPs.dailyLogsByDate.any { (date, localLog) ->
            val remoteLog = remotePs.dailyLogsByDate[date]
            val localHasActivity = localLog.calories > 0 || localLog.waterLiters > 0.0 ||
                localLog.foodItems.isNotEmpty() || localLog.tasksCompleted.isNotEmpty()
            if (!localHasActivity) {
                false
            } else if (remoteLog == null) {
                true
            } else {
                localLog.calories > remoteLog.calories ||
                    localLog.foodItems.size > remoteLog.foodItems.size ||
                    localLog.waterLiters > remoteLog.waterLiters + 0.05 ||
                    localLog.tasksCompleted.size > remoteLog.tasksCompleted.size
            }
        }
        if (hasRicherDailyLog) return true

        if (localPs.lifetimeXp > remotePs.lifetimeXp) return true
        if (localPs.restoredDates.any { it !in remotePs.restoredDates }) return true
        return false
    }

    private suspend fun reconcileRemoteSnapshot(
        firebaseUid: String,
        sessionId: String,
        remoteSnap: DocumentSnapshot?,
        localFlow: MutableStateFlow<HunterSession?>,
        isInitialOrForcedSync: Boolean
    ) {
        val localSession = localFlow.value
        if (remoteSnap == null || !remoteSnap.exists()) {
            // Remote session does not exist yet — if this device has local data, push it to cloud immediately
            if (localSession != null && localSession.stateJson.isNotBlank()) {
                val localState = localSession.toHunterSystemState()
                if (localState.biometrics.onboardingCompleted || localState.playerStatus.dailyLogsByDate.isNotEmpty()) {
                    saveOrUpdateSession(localState, sessionId)
                    return
                }
            }
            updateCloudSyncStatus(CloudSyncState.SYNCED, "Google Cloud ready", syncedNow = true)
            return
        }

        val remoteSession = remoteSnap.toObject(
            HunterSession::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: return
        if (remoteSession.stateJson.isBlank()) return

        val remoteState = remoteSession.toHunterSystemState()
        val localState = localSession?.toHunterSystemState()
        val unsyncedLocal = hasUnsyncedLocalChanges(firebaseUid)

        val shouldMergeLocalIntoRemote = localState != null && (
            unsyncedLocal ||
                (
                    isInitialOrForcedSync &&
                        (localState.playerStatus.stateUpdatedAtMillis == 0L ||
                            localState.playerStatus.stateUpdatedAtMillis > remoteState.playerStatus.stateUpdatedAtMillis) &&
                        hasLocalDivergedDataNotInRemote(localState, remoteState)
                    )
            )

        if (shouldMergeLocalIntoRemote && localState != null) {
            val mergedState = HunterSystemState.reconcileWithRemote(
                remote = remoteState,
                local = localState
            )
            val mergedJson = mergedState.toStateJson()
            val mergedSession = HunterSession.fromSystemState(
                id = sessionId,
                userId = firebaseUid,
                state = mergedState,
                existingCreatedAt = remoteSession.createdAt
            ).copy(updatedAt = Timestamp(Date()))
            localFlow.value = mergedSession
            setOnboardingCompletedForUser(firebaseUid, true, mergedJson)
            saveOrUpdateSession(mergedState, sessionId)
            return
        }

        // Standard real-time cloud adoption: if remote is newer or different, adopt cloud as source of truth
        val remoteUpdatedAt = remoteState.playerStatus.stateUpdatedAtMillis
        val localUpdatedAt = localState?.playerStatus?.stateUpdatedAtMillis ?: 0L
        val remoteResetNewer = remoteState.playerStatus.lastResetAtMillis > (localState?.playerStatus?.lastResetAtMillis ?: 0L)
        val shouldAdoptRemote = localSession == null ||
            isInitialOrForcedSync ||
            remoteResetNewer ||
            remoteUpdatedAt >= localUpdatedAt ||
            localUpdatedAt == 0L

        if (shouldAdoptRemote) {
            if (localFlow.value?.stateJson != remoteSession.stateJson) {
                localFlow.value = remoteSession
            }
            setOnboardingCompletedForUser(firebaseUid, true, remoteSession.stateJson)
            setHasUnsyncedLocalChanges(firebaseUid, false)
        }
        updateCloudSyncStatus(CloudSyncState.SYNCED, "Synced with Google Cloud", syncedNow = true)
    }

    suspend fun forceCloudSyncNow(sessionId: String = DEFAULT_SESSION_ID): Result<HunterSession?> =
        withContext(Dispatchers.IO) {
            val user = auth.currentUser
            if (user == null) {
                updateCloudSyncStatus(
                    CloudSyncState.LOCAL_ONLY,
                    "Local-only mode — sign in with Google to sync across phones"
                )
                return@withContext Result.failure(IllegalStateException("Not signed in with Google"))
            }
            val firebaseUid = user.uid
            val localFlow = getSessionFlowForUid(firebaseUid)
            updateCloudSyncStatus(CloudSyncState.SYNCING, "Syncing with Google Cloud...")
            val docRef = db.collection("users").document(firebaseUid).collection("sessions").document(sessionId)
            try {
                user.getIdToken(false).await()
                val serverSnap = try {
                    docRef.get(Source.SERVER).await()
                } catch (_: Exception) {
                    docRef.get(Source.DEFAULT).await()
                }
                reconcileRemoteSnapshot(
                    firebaseUid = firebaseUid,
                    sessionId = sessionId,
                    remoteSnap = serverSnap,
                    localFlow = localFlow,
                    isInitialOrForcedSync = true
                )
                Result.success(localFlow.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, docRef.path)
                updateCloudSyncStatus(CloudSyncState.OFFLINE_PENDING, "Offline — changes saved locally & will sync")
                Result.failure(e)
            }
        }

    fun observeActiveSession(sessionId: String = DEFAULT_SESSION_ID): Flow<HunterSession?> = callbackFlow {
        val user = auth.currentUser
        val effUid = effectiveUserId()
        val localFlow = getSessionFlowForUid(effUid)

        // Emit local cache immediately if available for instant startup, while cloud syncs in parallel
        val localJob = launch(Dispatchers.IO) {
            localFlow.collect { session ->
                trySend(session)
            }
        }

        if (user == null) {
            updateCloudSyncStatus(
                CloudSyncState.LOCAL_ONLY,
                "Local-only mode — sign in with Google to sync across phones"
            )
            awaitClose { localJob.cancel() }
            return@callbackFlow
        }

        val firebaseUid = user.uid
        val docRef = db.collection("users").document(firebaseUid).collection("sessions").document(sessionId)
        updateCloudSyncStatus(CloudSyncState.SYNCING, "Syncing with Google Cloud...")

        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        val cloudInitJob = launch(Dispatchers.IO) {
            try {
                user.getIdToken(false).await()
                // 1. Force an immediate authoritative server read & reconcile diverged/offline state
                val initialSnap = try {
                    docRef.get(Source.SERVER).await()
                } catch (_: Exception) {
                    try {
                        docRef.get(Source.DEFAULT).await()
                    } catch (_: Exception) {
                        null
                    }
                }
                if (initialSnap != null) {
                    reconcileRemoteSnapshot(
                        firebaseUid = firebaseUid,
                        sessionId = sessionId,
                        remoteSnap = initialSnap,
                        localFlow = localFlow,
                        isInitialOrForcedSync = true
                    )
                }

                // 2. Attach real-time Firestore snapshot listener so changes on Phone 1 appear live on Phone 2
                registration = docRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (auth.currentUser != null) {
                            handleFirestoreError(error, OperationType.GET, docRef.path)
                            updateCloudSyncStatus(
                                CloudSyncState.OFFLINE_PENDING,
                                "Cloud sync paused — retrying automatically"
                            )
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    // Skip optimistic local-write echoes; process confirmed remote/server snapshots
                    if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener

                    launch(Dispatchers.IO) {
                        try {
                            reconcileRemoteSnapshot(
                                firebaseUid = firebaseUid,
                                sessionId = sessionId,
                                remoteSnap = snapshot,
                                localFlow = localFlow,
                                isInitialOrForcedSync = false
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w("HunterRepository", "Snapshot reconcile error: ${e.message}")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (auth.currentUser != null) {
                    handleFirestoreError(e, OperationType.GET, docRef.path)
                    updateCloudSyncStatus(
                        CloudSyncState.OFFLINE_PENDING,
                        "Offline — showing local cache until reconnected"
                    )
                }
            }
        }

        awaitClose {
            registration?.remove()
            cloudInitJob.cancel()
            localJob.cancel()
        }
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
    ): Result<HunterSession> = withContext(Dispatchers.IO) {
        sessionWriteMutex.withLock {
            val effUid = effectiveUserId()
            val localFlow = getSessionFlowForUid(effUid)
            val nowMillis = System.currentTimeMillis()
            val stampedState = state.copy(
                playerStatus = state.playerStatus.copy(
                    stateUpdatedAtMillis = maxOf(nowMillis, state.playerStatus.stateUpdatedAtMillis)
                )
            )
            val nowTs = Timestamp(Date(nowMillis))
            val existingCreatedAt = localFlow.value?.createdAt ?: nowTs
            val localModel = HunterSession.fromSystemState(
                id = sessionId,
                userId = effUid,
                state = stampedState,
                existingCreatedAt = existingCreatedAt
            ).copy(updatedAt = nowTs)
            localFlow.value = localModel
            setOnboardingCompletedForUser(effUid, true, localModel.stateJson)

            val firebaseUid = currentUserIdOrNull()
            if (firebaseUid == null) {
                setHasUnsyncedLocalChanges(effUid, true)
                updateCloudSyncStatus(
                    CloudSyncState.LOCAL_ONLY,
                    "Saved locally — sign in with Google to sync across devices"
                )
                return@withLock Result.success(localModel)
            }

            val docRef = db.collection("users").document(firebaseUid).collection("sessions").document(sessionId)
            val updatedModel = HunterSession.fromSystemState(
                id = sessionId,
                userId = firebaseUid,
                state = stampedState,
                existingCreatedAt = existingCreatedAt
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

            try {
                // Fast single-round-trip update; preserves existing server createdAt & avoids stale cache mismatch
                try {
                    docRef.update(updateFields).await()
                    setHasUnsyncedLocalChanges(firebaseUid, false)
                    updateCloudSyncStatus(CloudSyncState.SYNCED, "Synced with Google Cloud", syncedNow = true)
                    Result.success(updatedModel)
                } catch (updateEx: FirebaseFirestoreException) {
                    if (updateEx.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                        val newModel = HunterSession.fromSystemState(
                            id = sessionId,
                            userId = firebaseUid,
                            state = stampedState,
                            existingCreatedAt = null
                        )
                        docRef.set(newModel).await()
                        setHasUnsyncedLocalChanges(firebaseUid, false)
                        updateCloudSyncStatus(CloudSyncState.SYNCED, "Synced with Google Cloud", syncedNow = true)
                        Result.success(newModel)
                    } else {
                        throw updateEx
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docRef.path)
                setHasUnsyncedLocalChanges(firebaseUid, true)
                updateCloudSyncStatus(
                    CloudSyncState.OFFLINE_PENDING,
                    "Saved locally — pending cloud sync"
                )
                Result.success(localModel)
            }
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
        val resetNow = System.currentTimeMillis()
        val stampedResetState = resetState.copy(
            playerStatus = resetState.playerStatus.copy(
                stateUpdatedAtMillis = resetNow,
                lastResetAtMillis = resetNow
            )
        )
        val sessionRes = saveOrUpdateSession(stampedResetState, sessionId)

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
        return try {
            com.example.BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            "1.1.3"
        }
    }

    fun getInstalledVersionCode(): Long {
        return try {
            com.example.BuildConfig.VERSION_CODE.toLong()
        } catch (_: Exception) {
            113L
        }
    }

    fun markVersionInstalled(versionName: String, versionCode: Long, publishedAt: String = "") {
        val prefs = context?.getSharedPreferences(PREFS_ONBOARDING_NAME, Context.MODE_PRIVATE)
        if (publishedAt.isNotBlank()) {
            prefs?.edit()?.putString(KEY_ACK_PUBLISHED_AT, publishedAt)?.apply()
        }
        val current = localReleaseInfoFlow.value
        val actualInstalledCode = getInstalledVersionCode()
        localReleaseInfoFlow.value = current.copy(
            isUpdateAvailable = current.latestVersionCode > actualInstalledCode ||
                current.minNativeVersionCode > actualInstalledCode
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
                latestVersionCode = maxOf(installedCode, patchManifest.minNativeVersionCode),
                latestVersionName = if (patchManifest.minNativeVersionCode > installedCode) {
                    patchManifest.baseAppVersion.ifBlank { installedVer }
                } else {
                    installedVer
                },
                releaseTitle = if (patchManifest.minNativeVersionCode > installedCode) {
                    patchManifest.releaseTitle.ifBlank { "PikaFit v${patchManifest.baseAppVersion}" }
                } else {
                    "PikaFit v$installedVer"
                },
                releaseNotes = patchManifest.patchNotes.ifBlank {
                    "You are running PikaFit v$installedVer with auto-patch v${patchManifest.patchVersion}."
                },
                downloadUrl = "https://github.com/ATOM495/PikaFit/releases/latest",
                apkDownloadUrl = "",
                releasePageUrl = "https://github.com/ATOM495/PikaFit/releases/latest",
                minNativeVersionCode = patchManifest.minNativeVersionCode,
                isUpdateAvailable = patchManifest.minNativeVersionCode > installedCode
            )
        }

        val effectiveLatestCode = maxOf(rawResolved.latestVersionCode, patchManifest.minNativeVersionCode)
        val effectiveLatestName = if (patchManifest.minNativeVersionCode > rawResolved.latestVersionCode &&
            patchManifest.baseAppVersion.isNotBlank()
        ) {
            patchManifest.baseAppVersion
        } else {
            rawResolved.latestVersionName
        }

        // Detect genuine native APK updates via GitHub Releases, Firestore, or patchManifest.minNativeVersionCode
        val isNativeApkUpdate = (rawResolved.isUpdateAvailable &&
            !rawResolved.latestVersionName.contains("patch", ignoreCase = true)) ||
            effectiveLatestCode > installedCode ||
            compareVersionStrings(effectiveLatestName, installedVer) > 0

        val resolved = rawResolved.copy(
            latestVersionCode = effectiveLatestCode,
            latestVersionName = effectiveLatestName,
            minNativeVersionCode = patchManifest.minNativeVersionCode,
            releasePageUrl = rawResolved.releasePageUrl.ifBlank {
                "https://github.com/ATOM495/PikaFit/releases/latest"
            },
            patchNumber = patchManifest.patchNumber,
            patchVersion = patchManifest.patchVersion,
            patchAppliedAutomatically = patchResult.appliedFromRemote,
            announcementBanner = patchManifest.announcementBanner,
            patchedFoodsCount = SmartFoodNutritionEngine.getPatchedFoodCount(),
            releaseTitle = if (rawResolved.releaseTitle.isBlank()) {
                patchManifest.releaseTitle.ifBlank { "PikaFit v$effectiveLatestName" }
            } else {
                rawResolved.releaseTitle
            },
            releaseNotes = if (!isNativeApkUpdate && patchManifest.patchNotes.isNotBlank()) {
                patchManifest.patchNotes
            } else {
                rawResolved.releaseNotes.ifBlank { patchManifest.patchNotes }
            },
            isUpdateAvailable = isNativeApkUpdate
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
                            val rawNotes = releaseObj.optString("body", "").trim()
                            val cleanNotes = rawNotes
                                .lineSequence()
                                .map { it.trim() }
                                .filterNot {
                                    it.contains("Full Changelog", ignoreCase = true) ||
                                        it.startsWith("https://github.com/", ignoreCase = true)
                                }
                                .distinct()
                                .joinToString("\n") { line ->
                                    line.replace("**", "").trim()
                                }
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
                            val updateAvailable = semCompare > 0 && !isPatchOnlyRelease
                            val computedCode = if (updateAvailable) {
                                maxOf(installedCode + 1L, deriveVersionCode(cleanTag, installedCode + 1L))
                            } else {
                                deriveVersionCode(cleanTag, installedCode)
                            }
                            val resolvedReleasePage = htmlUrl.ifBlank {
                                "https://github.com/ATOM495/PikaFit/releases/latest"
                            }

                            return AppReleaseInfo(
                                latestVersionCode = computedCode,
                                latestVersionName = cleanTag,
                                releaseTitle = title,
                                releaseNotes = cleanNotes.take(1500),
                                downloadUrl = apkAssetUrl.ifBlank { resolvedReleasePage },
                                apkDownloadUrl = apkAssetUrl,
                                releasePageUrl = resolvedReleasePage,
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
        private val unsyncedLocalUids = mutableSetOf<String>()
        private val cachedStateJsonByUid = mutableMapOf<String, String>()
        private val sessionWriteMutex = Mutex()

        private val localSessionsByUid = mutableMapOf<String, MutableStateFlow<HunterSession?>>()
        private val localLogsByUid = mutableMapOf<String, MutableStateFlow<List<TacticalLog>>>()
        private val localJournalByUid = mutableMapOf<String, MutableStateFlow<List<JournalEntry>>>()
        private val localFeedbackFlow = MutableStateFlow<List<FeedbackEntry>>(emptyList())
        private val localReleaseInfoFlow = MutableStateFlow(AppReleaseInfo())
        private val localOtaPatchFlow = MutableStateFlow(PatchUpdateManager.defaultBuiltInManifest())
        private val cloudSyncStatusFlow = MutableStateFlow(CloudSyncStatus())
    }
}
