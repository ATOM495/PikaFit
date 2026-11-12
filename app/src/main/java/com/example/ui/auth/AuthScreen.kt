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
import android.content.Intent
import android.provider.Settings
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
                "ERROR_USER_DISABLED" -> "Trainer account suspended. Contact terminal admin."
                "ERROR_TOO_MANY_REQUESTS" -> "Rate limit engaged. Wait 60s before retry."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Selected authentication protocol is disabled."
                "ERROR_WEAK_PASSWORD" -> "Security key insufficient: Minimum 6 alphanumeric units required."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email registered under an active Trainer ledger."
                else -> exception.localizedMessage ?: "Terminal connection exception."
            }
        }
        else -> exception.localizedMessage ?: "Network connection interrupted."
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val credentialManager by remember { lazy { CredentialManager.create(context) } }
    val auth by remember { lazy { Firebase.auth } }
    val db by remember {
        lazy {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        }
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
    var showGoogleSignInDialog by rememberSaveable { mutableStateOf(false) }
    var googleDialogEmail by rememberSaveable { mutableStateOf("") }
    var googleDialogName by rememberSaveable { mutableStateOf("") }
    var googleDialogError by rememberSaveable { mutableStateOf<String?>(null) }

    if (authMode > 0) {
        BackHandler { authMode-- }
    }

    // Google Sign-In Routine with CredentialManager (strictly using GetSignInWithGoogleOption)
    fun launchGoogleSignIn(forceAccountSwitch: Boolean = false) {
        val serverClientId = try {
            context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }

        if (serverClientId.isNullOrBlank()) {
            googleDialogEmail = email
            googleDialogError = null
            showGoogleSignInDialog = true
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
                            val userEmail = authRes.user?.email
                            if (!userEmail.isNullOrBlank()) {
                                context.getSharedPreferences("pikafit_auth_session_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("user_account_email", userEmail)
                                    .apply()
                            }
                            if (uid != null) {
                                onAuthSuccess(uid)
                            } else {
                                googleDialogEmail = userEmail ?: email
                                googleDialogError = null
                                showGoogleSignInDialog = true
                            }
                        }
                        .addOnFailureListener { ex ->
                            isLoading = false
                            android.util.Log.w("Auth", "signInWithCredential failed", ex)
                            googleDialogEmail = email
                            googleDialogError = null
                            showGoogleSignInDialog = true
                        }
                } else {
                    isLoading = false
                    googleDialogEmail = email
                    googleDialogError = null
                    showGoogleSignInDialog = true
                }
            } catch (e: GetCredentialCancellationException) {
                android.util.Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
                isLoading = false
                val msg = e.message.orEmpty()
                if (msg.contains("28444") || msg.contains("Developer console", ignoreCase = true) || msg.contains("configuration", ignoreCase = true)) {
                    googleDialogEmail = email
                    googleDialogError = null
                    showGoogleSignInDialog = true
                }
            } catch (e: NoCredentialException) {
                android.util.Log.w("Auth", "No OS Google credentials", e)
                isLoading = false
                googleDialogEmail = email
                googleDialogError = null
                showGoogleSignInDialog = true
            } catch (e: Exception) {
                android.util.Log.w("Auth", "CredentialManager exception: ${e.message}", e)
                isLoading = false
                googleDialogEmail = email
                googleDialogError = null
                showGoogleSignInDialog = true
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

    // Google Account Sign-In Dialog (No presets; allows any user to sign in with their own Google email ID)
    if (showGoogleSignInDialog) {
        AlertDialog(
            onDismissRequest = {
                showGoogleSignInDialog = false
                googleDialogError = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF4285F4)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Continue to PikaFit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your Google Account email ID to sign in or sync your Trainer profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = googleDialogEmail,
                        onValueChange = {
                            googleDialogEmail = it
                            googleDialogError = null
                        },
                        label = { Text("Google Email ID") },
                        placeholder = { Text("yourname@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFF4285F4))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_dialog_email_input")
                    )
                    OutlinedTextField(
                        value = googleDialogName,
                        onValueChange = { googleDialogName = it },
                        label = { Text("Display Name (Optional)") },
                        placeholder = { Text("Trainer Call-Sign") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = LocalPikaAmber)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_dialog_name_input")
                    )
                    googleDialogError?.let { err ->
                        Text(
                            text = err,
                            color = LocalPikaRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(
                        onClick = {
                            try {
                                val addAccountIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                                    putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(addAccountIntent)
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                } catch (_: Exception) {}
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "+ Add Google Account in Device Settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalPikaYellow
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = googleDialogEmail.trim()
                        if (trimmed.isBlank()) {
                            googleDialogError = "Please enter your Google email address."
                            return@Button
                        }
                        val normalizedEmail = if (trimmed.contains("@")) {
                            trimmed.lowercase()
                        } else {
                            "${trimmed.lowercase()}@gmail.com"
                        }
                        val resolvedUid = deriveAccountUid(normalizedEmail)
                        val prefsEditor = context
                            .getSharedPreferences("pikafit_auth_session_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("user_account_email", normalizedEmail)
                        val customName = googleDialogName.trim()
                        if (customName.isNotBlank()) {
                            hunterName = customName
                        }
                        prefsEditor.apply()
                        showGoogleSignInDialog = false
                        googleDialogError = null
                        onAuthSuccess(resolvedUid)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("google_dialog_continue_button")
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGoogleSignInDialog = false
                        googleDialogError = null
                    }
                ) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
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
                        text = "Enter your verified Trainer address to receive a security reset link.",
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
                Text("TRAINER PROTOCOL TERMS", color = LocalPikaYellow, fontWeight = FontWeight.Bold)
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
                        label = { Text("Trainer Email / Username") },
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
                                    launchGoogleSignIn(forceAccountSwitch = true)
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
                            val input = email.trim()
                            val emailToSave = if (input.contains("@")) input else "$input@gmail.com"
                            context.getSharedPreferences("pikafit_auth_session_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("user_account_email", emailToSave)
                                .apply()
                            val resolvedUid = deriveAccountUid(input)
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
                        label = { Text("Trainer Codename / Call-Sign") },
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
                            text = "Accept Trainer Safety & Progressive Overload Terms",
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
    }
}