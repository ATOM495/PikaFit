package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.MainHunterScreen
import com.example.ui.theme.HunterSystemTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(applicationContext, intent)

        // Read immediately without blocking the Choreographer frame loop
        val initialUserId = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: applicationContext
            .getSharedPreferences("pikafit_auth_session_prefs", MODE_PRIVATE)
            .getString("active_account_id", null)
            ?.takeIf { it.isNotBlank() && !it.contains("@") }

        setContent {
            val scope = rememberCoroutineScope()
            var isDarkMode by rememberSaveable { mutableStateOf(true) }
            var activeAccountId by rememberSaveable { mutableStateOf(initialUserId) }

            // Observe Firebase Auth state updates
            LaunchedEffect(Unit) {
                Firebase.auth.authStateFlow().collect { user ->
                    val uid = user?.uid
                    if (!uid.isNullOrBlank()) {
                        try {
                            user.getIdToken(false).await()
                        } catch (_: Exception) {}
                        activeAccountId = uid
                        withContext(Dispatchers.IO) {
                            applicationContext
                                .getSharedPreferences("pikafit_auth_session_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("active_account_id", uid)
                                .apply()
                        }
                    }
                }
            }

            // Clean legacy/stale SharedPreferences in the background without holding the main thread frame
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
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
                }
            }

            HunterSystemTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (activeAccountId.isNullOrBlank()) {
                        AuthScreen(
                            onAuthSuccess = { signedInAccountId ->
                                val resolvedId = try {
                                    Firebase.auth.currentUser?.uid
                                } catch (_: Exception) {
                                    null
                                } ?: signedInAccountId.ifBlank { "google_trainer_default" }
                                activeAccountId = resolvedId
                                scope.launch(Dispatchers.IO) {
                                    applicationContext
                                        .getSharedPreferences("pikafit_auth_session_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("active_account_id", resolvedId)
                                        .apply()
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