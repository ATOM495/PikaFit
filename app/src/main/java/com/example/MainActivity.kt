package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.MainHunterScreen
import com.example.ui.theme.HunterSystemTheme
import com.example.util.PikaFitNotificationScheduler
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}.flowOn(Dispatchers.IO)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check synchronously with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            var isDarkMode by rememberSaveable { mutableStateOf(true) }
            var activeAccountId by rememberSaveable { mutableStateOf<String?>(null) }
            var isStartupReady by remember { mutableStateOf(false) }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    PikaFitNotificationScheduler.startAutomaticRandomNotifications(applicationContext)
                }
            }

            // Let initial window SurfaceSyncGroup transactions commit before mounting heavy screens
            LaunchedEffect(Unit) {
                repeat(2) { withFrameNanos { } }

                val resolvedInitialAccount = withContext(Dispatchers.IO) {
                    val prefs = applicationContext.getSharedPreferences("pikafit_auth_session_prefs", MODE_PRIVATE)
                    val onboardingPrefs = applicationContext.getSharedPreferences("pikafit_account_onboarding_prefs", MODE_PRIVATE)
                    val existingCachedAccount = prefs.getString("active_account_id", null)
                    if (existingCachedAccount != null && existingCachedAccount.contains("@")) {
                        prefs.edit().remove("active_account_id").apply()
                    }
                    val staleKeys = onboardingPrefs.all.keys.filter { it.contains("@") }
                    if (staleKeys.isNotEmpty()) {
                        val editor = onboardingPrefs.edit()
                        staleKeys.forEach { editor.remove(it) }
                        editor.apply()
                    }

                    val firebaseUid = try {
                        Firebase.auth.currentUser?.uid
                    } catch (_: Exception) {
                        null
                    }
                    firebaseUid ?: prefs
                        .getString("active_account_id", null)
                        ?.takeIf { it.isNotBlank() && !it.contains("@") }
                }
                if (activeAccountId == null && !resolvedInitialAccount.isNullOrBlank()) {
                    activeAccountId = resolvedInitialAccount
                }
                isStartupReady = true

                // Automatically request POST_NOTIFICATIONS permission and start random background notifications
                PikaFitNotificationScheduler.ensureNotificationChannel(applicationContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !PikaFitNotificationScheduler.hasNotificationPermission(applicationContext)
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PikaFitNotificationScheduler.startAutomaticRandomNotifications(applicationContext)
                }

                // Observe Firebase Auth state updates on IO after first frame
                try {
                    Firebase.auth.authStateFlow().collect { user ->
                        val uid = user?.uid
                        if (!uid.isNullOrBlank()) {
                            withContext(Dispatchers.IO) {
                                try {
                                    user.getIdToken(false).await()
                                } catch (_: Exception) {}
                                applicationContext
                                    .getSharedPreferences("pikafit_auth_session_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("active_account_id", uid)
                                    .apply()
                            }
                            activeAccountId = uid
                        }
                    }
                } catch (_: Exception) {}
            }

            HunterSystemTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isStartupReady) {
                        Box(modifier = Modifier.fillMaxSize())
                    } else if (activeAccountId.isNullOrBlank()) {
                        AuthScreen(
                            onAuthSuccess = { signedInAccountId ->
                                scope.launch {
                                    val resolvedId = withContext(Dispatchers.IO) {
                                        val fbUid = try {
                                            Firebase.auth.currentUser?.uid
                                        } catch (_: Exception) {
                                            null
                                        }
                                        val finalId = fbUid ?: signedInAccountId.ifBlank { "google_trainer_default" }
                                        applicationContext
                                            .getSharedPreferences("pikafit_auth_session_prefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("active_account_id", finalId)
                                            .apply()
                                        finalId
                                    }
                                    activeAccountId = resolvedId
                                }
                            }
                        )
                    } else {
                        MainHunterScreen(
                            currentUserId = activeAccountId!!,
                            isDark = isDarkMode,
                            onToggleTheme = { isDarkMode = !isDarkMode },
                            onSignedOut = {
                                activeAccountId = null
                                scope.launch(Dispatchers.IO) {
                                    applicationContext
                                        .getSharedPreferences("pikafit_auth_session_prefs", MODE_PRIVATE)
                                        .edit()
                                        .remove("active_account_id")
                                        .apply()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}