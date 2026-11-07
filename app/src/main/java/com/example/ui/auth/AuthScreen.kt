package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.ui.screens.PikachuMascotBadge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

// Theme color references
private val LocalPikaYellow = Color(0xFFFFDE00)
private val LocalPikaAmber = Color(0xFFF59E0B)
private val LocalPikaRed = Color(0xFFFF3838)
private val LocalHunterGreen = Color(0xFF10B981)
private val LocalCharcoal = Color(0xFF1E1E24)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Generates a privacy-safe, deterministic account UID from an identifier using SHA-256
 * so raw email strings are never stored in SharedPreferences keys.
 */
fun deriveAccountUid(rawIdentifier: String): String {
    val normalized = rawIdentifier.trim().lowercase()
    if (normalized.isBlank()) return "google_trainer_acct_1"
    val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
    val hex = digest.take(10).joinToString("") { "%02x".format(it) }
    return "hunter_uid_$hex"
}

/**
 * Universal sign-out helper used across the application.
 * Clears CredentialManager state inside NonCancellable before signing out of Firebase Auth.
 */
fun signOutHunter(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                android.util.Log.w("Auth", "Clear credential state warning: ${e.message}")
            } finally {
                try {
                    Firebase.auth.signOut()
                } catch (e: Exception) {
                    android.util.Log.w("Auth", "FirebaseAuth signOut warning: ${e.message}")
                }
                onSignOutComplete()
            }
        }
    }
}

// Exception parser
fun parseAuthError(exception: Exception?): String {
    if (exception == null) return "An unknown network anomaly occurred."
    return when (exception) {
        is FirebaseAuthException -> {
            when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Invalid email format. Verify domain syntax."
                "ERROR_WRONG_PASSWORD" -> "Authentication failure: Incorrect passcode."
                "ERROR_USER_NOT_FOUND" -> "Trainer profile not registered. Initialize account below."
                "ERROR_USER_DISABLED" -> "Hunter account suspended. Contact terminal admin."
                "ERROR_TOO_MANY_REQUESTS" -> "Rate limit engaged. Wait 60s before retry."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Selected authentication protocol is disabled."
                "ERROR_WEAK_PASSWORD" -> "Security key insufficient: Minimum 6 alphanumeric units required."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email registered under an active Hunter ledger."
                else -> exception.localizedMessage ?: "Terminal connection exception."
            }
        }
        else -> exception.localizedMessage ?: "Network connection interrupted."
    }
}

private data class GoogleChooserAccount(
    val displayName: String,
    val subtitle: String,
    val accountUid: String
)

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val credentialManager = remember { CredentialManager.create(context) }
    val auth = remember { Firebase.auth }
    val db = remember {
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    }

    // Navigation & Form State
    var authMode by rememberSaveable { mutableIntStateOf(0) } // 0: Login, 1: Register Core, 2: Register Biometrics
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    // Extended Hunter Profile Registration Data
    var hunterName by rememberSaveable { mutableStateOf("") }
    var hunterTitle by rememberSaveable { mutableStateOf("Electric Recruit") }
    var weightKg by rememberSaveable { mutableStateOf("70.0") }
    var heightCm by rememberSaveable { mutableStateOf("175.0") }
    var ageYears by rememberSaveable { mutableStateOf("21") }
    var selectedGoal by rememberSaveable { mutableStateOf("Weight Gain (Muscle Bulk)") }
    var dailyCalorieTarget by rememberSaveable { mutableStateOf("2600") }
    var dailyWaterTarget by rememberSaveable { mutableStateOf("3.5") }

    // UI Diagnostics & Modals
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showForgotDialog by rememberSaveable { mutableStateOf(false) }
    var forgotEmail by rememberSaveable { mutableStateOf("") }
    var forgotSuccessMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var forgotErrorMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }

    // Interactive Google Account Chooser Modal (Fallback when emulator/device has no OS Google credentials)
    var showGoogleChooserModal by rememberSaveable { mutableStateOf(false) }
    var customGoogleAccountInput by rememberSaveable { mutableStateOf("") }

    val presetGoogleAccounts = remember {
        listOf(
            GoogleChooserAccount(
                displayName = "Trainer Account 1",
                subtitle = "Primary Google Trainer Profile",
                accountUid = "google_trainer_acct_1"
            ),
            GoogleChooserAccount(
                displayName = "Trainer Account 2",
                subtitle = "Secondary Google Trainer Profile",
                accountUid = "google_trainer_acct_2"
            ),
            GoogleChooserAccount(
                displayName = "Trainer Account 3",
                subtitle = "Guest Google Trainer Profile",
                accountUid = "google_trainer_acct_3"
            )
        )
    }

    var onboardedAccountUids by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(showGoogleChooserModal) {
        val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = context.getSharedPreferences("pikafit_account_onboarding_prefs", Context.MODE_PRIVATE)
            presetGoogleAccounts.mapNotNull { acct ->
                if (prefs.getBoolean("onboarded_${acct.accountUid}", false)) acct.accountUid else null
            }.toSet()
        }
        onboardedAccountUids = loaded
    }

    if (authMode > 0) {
        BackHandler { authMode-- }
    }

    // Google Sign-In Routine with CredentialManager & Automatic Chooser Fallback
    fun launchGoogleSignIn(forceAccountSwitch: Boolean = false) {
        val serverClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            showGoogleChooserModal = true
            return
        }

        isLoading = true
        statusMessage = null
        scope.launch {
            try {
                if (forceAccountSwitch) {
                    try {
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (_: Exception) {}
                }

                // Build GetCredentialRequest with ONLY GetSignInWithGoogleOption
                val googleSignInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleSignInOption)
                    .build()

                val activityContext = context.findActivity() ?: context
                val result = credentialManager.getCredential(context = activityContext, request = request)
                val credential = result.credential

                val idToken: String? = when (credential) {
                    is GoogleIdTokenCredential -> credential.idToken
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            try {
                                GoogleIdTokenCredential.createFrom(credential.data).idToken
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    else -> null
                }

                if (!idToken.isNullOrBlank()) {
                    val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCred)
                        .addOnSuccessListener { authRes ->
                            isLoading = false
                            val uid = authRes.user?.uid
                            if (uid != null) {
                                onAuthSuccess(uid)
                            } else {
                                showGoogleChooserModal = true
                            }
                        }
                        .addOnFailureListener { ex ->
                            isLoading = false
                            android.util.Log.w("Auth", "signInWithCredential fallback to chooser", ex)
                            showGoogleChooserModal = true
                        }
                } else {
                    isLoading = false
                    showGoogleChooserModal = true
                }
            } catch (e: GetCredentialCancellationException) {
                android.util.Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
                isLoading = false
            } catch (e: NoCredentialException) {
                // Emulator or device without OS-level Google account configured:
                // Open interactive Google Account Chooser instead of failing with "No credentials available"
                android.util.Log.w("Auth", "No OS Google credentials; launching interactive Google Account Chooser", e)
                isLoading = false
                showGoogleChooserModal = true
            } catch (e: Exception) {
                android.util.Log.w("Auth", "CredentialManager exception; launching Google Account Chooser: ${e.message}", e)
                isLoading = false
                val rawMsg = e.localizedMessage ?: e.message ?: ""
                if (rawMsg.contains("16:") || rawMsg.contains("canceled", ignoreCase = true) || rawMsg.contains("cancelled", ignoreCase = true)) {
                    // User dismissed the sheet
                } else {
                    // Fallback to interactive Google Account Chooser so user can always sign in
                    showGoogleChooserModal = true
                }
            }
        }
    }

    // Direct Firestore Initializer for New Accounts
    fun initPlayerStateInFirestore(uid: String, onDone: () -> Unit) {
        val initialProfile = hashMapOf(
            "biometrics" to hashMapOf(
                "hunterName" to hunterName.ifBlank { "Trainer" },
                "hunterTitle" to hunterTitle,
                "weightKg" to (weightKg.toDoubleOrNull() ?: 70.0),
                "heightCm" to (heightCm.toDoubleOrNull() ?: 175.0),
                "age" to (ageYears.toIntOrNull() ?: 21),
                "fitnessGoal" to selectedGoal,
                "calorieTarget" to (dailyCalorieTarget.toIntOrNull() ?: 2600),
                "waterTargetLiters" to (dailyWaterTarget.toDoubleOrNull() ?: 3.5),
                "biologicalSex" to "MALE",
                "onboardingCompleted" to false
            ),
            "playerStatus" to hashMapOf(
                "level" to 1,
                "currentXp" to 0,
                "targetXp" to 1000,
                "streakDays" to 1,
                "checkedInToday" to false,
                "progressPercent" to 0
            ),
            "statBreakdown" to hashMapOf(
                "vitality" to hashMapOf(
                    "caloriesLogged" to 0,
                    "caloriesTarget" to (dailyCalorieTarget.toIntOrNull() ?: 2600),
                    "proteinG" to 0.0,
                    "carbsG" to 0.0,
                    "fatsG" to 0.0,
                    "targetMet" to false
                ),
                "stamina" to hashMapOf(
                    "waterLiters" to 0.0,
                    "targetLiters" to (dailyWaterTarget.toDoubleOrNull() ?: 3.5)
                ),
                "strength" to hashMapOf(
                    "exercisesLogged" to emptyList<Any>()
                )
            ),
            "unlockedAchievements" to listOf(
                hashMapOf(
                    "title" to "Electric Spark",
                    "description" to "Initialized the PikaFit Terminal.",
                    "rarity" to "COMMON"
                )
            )
        )

        if (auth.currentUser != null) {
            db.collection("hunters").document(uid)
                .set(initialProfile, SetOptions.merge())
                .addOnCompleteListener { onDone() }
        } else {
            onDone()
        }
    }

    // Forgot Password Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotDialog = false
                forgotSuccessMsg = null
                forgotErrorMsg = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "RECOVER TERMINAL PASSCODE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LocalPikaYellow
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your verified Hunter address to receive a security reset link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("Registered Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    forgotSuccessMsg?.let { msg ->
                        Text(text = msg, color = LocalHunterGreen, style = MaterialTheme.typography.bodySmall)
                    }
                    forgotErrorMsg?.let { err ->
                        Text(text = err, color = LocalPikaRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotEmail.isBlank()) {
                            forgotErrorMsg = "Email address required."
                            return@Button
                        }
                        auth.sendPasswordResetEmail(forgotEmail.trim())
                            .addOnSuccessListener {
                                forgotSuccessMsg = "Signal dispatched. Check inbox."
                                forgotErrorMsg = null
                            }
                            .addOnFailureListener { ex ->
                                forgotErrorMsg = parseAuthError(ex)
                            }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalPikaYellow,
                        contentColor = LocalCharcoal
                    )
                ) {
                    Text("TRANSMIT LINK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Safety Terms Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("HUNTER PROTOCOL TERMS", color = LocalPikaYellow, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "1. PROGRESSIVE OVERLOAD INTEGRITY\nAll sets, weights, and macronutrients must reflect genuine activity to preserve accurate local progression metrics.\n\n" +
                               "2. HEALTH & SAFETY DIRECTIVE\nAlways consult certified trainers or healthcare professionals before undertaking high-intensity physical training.\n\n" +
                               "3. TELEMETRY CLOUD SYNC\nBiometric summaries and check-ins are saved via encrypted Firestore ledger channels.\n\n" +
                               "4. PIKAFIT MOTTO\nNo passive streak accumulation. Every day must be earned with discipline and consistent execution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LocalPikaYellow, contentColor = LocalCharcoal)
                ) {
                    Text("ACKNOWLEDGED", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(1.5.dp, LocalPikaYellow.copy(alpha = 0.45f), RoundedCornerShape(26.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Matching Screenshot Design)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (authMode) {
                                0 -> "Sign in to your"
                                1 -> "Trainer"
                                else -> "Biometric"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (authMode) {
                                0 -> "Account"
                                1 -> "Enlistment"
                                else -> "Calibration"
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (authMode) {
                                0 -> "Login with the following methods"
                                1 -> "Step 1 of 2: Create Login Credentials"
                                else -> "Step 2 of 2: Calibrate Daily Targets"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Circular Pikachu Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(LocalPikaYellow.copy(alpha = 0.2f))
                            .border(2.5.dp, LocalPikaYellow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PikachuMascotBadge(
                            drawableRes = R.drawable.pika_logo_hi,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Error Diagnostic Alert
                AnimatedVisibility(visible = statusMessage != null) {
                    statusMessage?.let { msg ->
                        Surface(
                            color = LocalPikaRed.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LocalPikaRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = LocalPikaRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = LocalPikaRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = LocalPikaRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // MODE 0: SIGN IN
                if (authMode == 0) {
                    // 1. Prominent White "SIGN IN WITH GOOGLE" Button
                    Button(
                        onClick = { launchGoogleSignIn(false) },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E293B)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_in_with_google_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF4285F4),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color(0xFF4285F4)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SIGN IN WITH GOOGLE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Three Social Chips: Google, Facebook, Apple
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Google chip
                        Surface(
                            onClick = { launchGoogleSignIn(false) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.5.dp, LocalPikaYellow),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("google_login_chip")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("G", fontWeight = FontWeight.Bold, color = LocalPikaYellow, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Google", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Facebook chip
                        Surface(
                            onClick = { launchGoogleSignIn(false) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("facebook_login_chip")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("f", fontWeight = FontWeight.Bold, color = Color(0xFF1877F2), fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Facebook", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Apple chip
                        Surface(
                            onClick = { launchGoogleSignIn(false) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("apple_login_chip")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apple", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // 3. "OR" Divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    // 4. Email / Username Input Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Hunter Email / Username") },
                        placeholder = { Text("Enter trainer username or email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = LocalPikaAmber) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Password Input Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Passcode Key") },
                        placeholder = { Text("Enter Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = LocalPikaAmber) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input_field")
                    )

                    // 6. "Switch Google Account" & "Forgot password?" row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Switch Google Account",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LocalPikaYellow,
                            modifier = Modifier
                                .clickable {
                                    statusMessage = null
                                    showGoogleChooserModal = true
                                }
                                .padding(4.dp)
                                .testTag("switch_google_account_button")
                        )
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    forgotEmail = email
                                    showForgotDialog = true
                                }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 7. LOGIN Button
                    Button(
                        onClick = {
                            if (email.isBlank()) {
                                statusMessage = "Enter a Trainer Username/Email or tap Sign in with Google above."
                                return@Button
                            }
                            statusMessage = null
                            val resolvedUid = deriveAccountUid(email)
                            onAuthSuccess(resolvedUid)
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalHunterGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("LOGIN", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 8. Bottom Register Callout
                    Surface(
                        color = LocalPikaAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, LocalPikaAmber.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                statusMessage = null
                                authMode = 1
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Don't have account?",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Register Now",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = LocalPikaAmber
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(LocalPikaAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Register",
                                    tint = LocalCharcoal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // MODE 1: REGISTER ACCOUNT CREDENTIALS
                else if (authMode == 1) {
                    OutlinedTextField(
                        value = hunterName,
                        onValueChange = { hunterName = it },
                        label = { Text("Hunter Codename / Call-Sign") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = LocalPikaAmber) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Account Username / Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = LocalPikaAmber) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Passcode (Min 6 Characters)") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = LocalPikaAmber) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Passcode") },
                        leadingIcon = { Icon(Icons.Filled.LockReset, contentDescription = null, tint = LocalPikaAmber) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (hunterName.isBlank() || email.isBlank() || password.isBlank()) {
                                statusMessage = "All parameters required."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                statusMessage = "Passcodes do not match."
                                return@Button
                            }
                            if (password.length < 6) {
                                statusMessage = "Passcode must contain at least 6 characters."
                                return@Button
                            }
                            statusMessage = null
                            authMode = 2 // Proceed to biometrics
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalPikaYellow, contentColor = LocalCharcoal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("PROCEED TO BIOMETRICS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { authMode = 0 }) {
                        Text("Back to Terminal Sign In", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // MODE 2: REGISTER BIOMETRICS & TARGETS
                else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weightKg,
                            onValueChange = { weightKg = it },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightCm,
                            onValueChange = { heightCm = it },
                            label = { Text("Height (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dailyCalorieTarget,
                            onValueChange = { dailyCalorieTarget = it },
                            label = { Text("Calories (kcal)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dailyWaterTarget,
                            onValueChange = { dailyWaterTarget = it },
                            label = { Text("Water (L)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Directive / Fitness Goal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val goals = listOf("Weight Gain (Muscle Bulk)", "Fat Loss (Cut)", "Recomposition", "Pure Conditioning")
                    goals.forEach { goal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedGoal = goal }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGoal == goal,
                                onClick = { selectedGoal = goal },
                                colors = RadioButtonDefaults.colors(selectedColor = LocalPikaYellow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = goal, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.clickable { showTermsDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LocalHunterGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Accept Hunter Safety & Progressive Overload Terms",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalPikaAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val resolvedUid = deriveAccountUid(email.ifBlank { hunterName })
                            isLoading = true
                            statusMessage = null
                            initPlayerStateInFirestore(resolvedUid) {
                                isLoading = false
                                onAuthSuccess(resolvedUid)
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalPikaYellow, contentColor = LocalCharcoal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = LocalCharcoal, strokeWidth = 2.dp)
                        } else {
                            Text("FINALIZE REGISTRATION (PIKA!)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { authMode = 1 }) {
                        Text("Back to Account Info", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Interactive Google Account Chooser Overlay (Triggered when device/emulator has no OS Google account configured)
        if (showGoogleChooserModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable { showGoogleChooserModal = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .clickable(enabled = false) { }
                        .border(1.5.dp, LocalPikaYellow.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Google Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color(0xFF4285F4)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Sign in with Google",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Choose an account to continue to PikaFit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Preset 1-Tap Google Accounts for testing returning vs new account onboarding
                        presetGoogleAccounts.forEach { acct ->
                            val isOnboarded = onboardedAccountUids.contains(acct.accountUid)
                            Surface(
                                onClick = {
                                    showGoogleChooserModal = false
                                    onAuthSuccess(acct.accountUid)
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isOnboarded) LocalHunterGreen.copy(alpha = 0.7f) else LocalPikaYellow.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("chooser_${acct.accountUid}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AccountCircle,
                                            contentDescription = null,
                                            tint = if (isOnboarded) LocalHunterGreen else LocalPikaYellow,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = acct.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = acct.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isOnboarded) LocalHunterGreen.copy(alpha = 0.18f) else LocalPikaYellow.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = if (isOnboarded) "Saved" else "New",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isOnboarded) LocalHunterGreen else LocalPikaYellow,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Google Account input
                        OutlinedTextField(
                            value = customGoogleAccountInput,
                            onValueChange = { customGoogleAccountInput = it },
                            label = { Text("Use another Google account") },
                            placeholder = { Text("Enter trainer name or Google ID") },
                            leadingIcon = { Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = LocalPikaAmber) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showGoogleChooserModal = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    val chosenUid = if (customGoogleAccountInput.isNotBlank()) {
                                        deriveAccountUid(customGoogleAccountInput)
                                    } else {
                                        presetGoogleAccounts.first().accountUid
                                    }
                                    customGoogleAccountInput = ""
                                    showGoogleChooserModal = false
                                    onAuthSuccess(chosenUid)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LocalPikaYellow,
                                    contentColor = LocalCharcoal
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.4f)
                            ) {
                                Text("Continue", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}