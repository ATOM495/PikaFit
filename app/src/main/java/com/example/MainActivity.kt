package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.MainHunterScreen
import com.example.ui.theme.HunterSystemTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("pikafit_auth_session_prefs", MODE_PRIVATE)
        val onboardingPrefs = getSharedPreferences("pikafit_account_onboarding_prefs", MODE_PRIVATE)

        // Purge any previously cached session or onboarding keys tied to a real email ID
        val existingCachedAccount = prefs.getString("active_account_id", null)
        if (existingCachedAccount != null && existingCachedAccount.contains("tmcrainbow", ignoreCase = true)) {
            prefs.edit().remove("active_account_id").apply()
        }
        val staleKeys = onboardingPrefs.all.keys.filter { it.contains("tmcrainbow", ignoreCase = true) }
        if (staleKeys.isNotEmpty()) {
            val editor = onboardingPrefs.edit()
            staleKeys.forEach { editor.remove(it) }
            editor.apply()
        }

        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(true) }
            val firebaseUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
            var activeAccountId by rememberSaveable {
                mutableStateOf(
                    Firebase.auth.currentUser?.uid ?: prefs.getString("active_account_id", null)
                )
            }

            LaunchedEffect(firebaseUser) {
                if (firebaseUser != null) {
                    activeAccountId = firebaseUser!!.uid
                    prefs.edit().putString("active_account_id", firebaseUser!!.uid).apply()
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
                                val resolvedId = Firebase.auth.currentUser?.uid
                                    ?: signedInAccountId.ifBlank { "google_trainer_default" }
                                activeAccountId = resolvedId
                                prefs.edit().putString("active_account_id", resolvedId).apply()
                            }
                        )
                    } else {
                        MainHunterScreen(
                            currentUserId = activeAccountId!!,
                            isDark = isDarkMode,
                            onToggleTheme = { isDarkMode = !isDarkMode },
                            onSignedOut = {
                                activeAccountId = null
                                prefs.edit().remove("active_account_id").apply()
                            }
                        )
                    }
                }
            }
        }
    }
}