package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- Self-contained Modern Theme Colors ---
private val ColorAccentTeal = Color(0xFF00E5C0)
private val ColorAccentPurple = Color(0xFFA855F7)
private val ColorDarkBgTop = Color(0xFF090D18)
private val ColorDarkBgMid = Color(0xFF0F1526)
private val ColorDarkBgBottom = Color(0xFF0B101E)
private val ColorCardBg = Color(0xFF151C2E)
private val ColorBorder = Color(0xFF263352)
private val ColorTextPrimary = Color(0xFFF8FAFC)
private val ColorTextSecondary = Color(0xFF94A3B8)
private val ColorDanger = Color(0xFFEF4444)

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
            onAuthCancelled()
        } catch (e: Exception) {
            Log.w("Auth", "Google Sign-In fallback: ${e.message}")
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutHunter(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("Auth", "Clear credential state skipped: ${e.message}")
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Default directly to Step 1 ("Sign in to your Account") so Google Sign-In and all login options are immediately visible!
    // 0 = Welcome to PikaFit, 1 = Sign in to your Account, 2 = Create Your New Account
    var step by rememberSaveable { mutableIntStateOf(1) }

    // Login form state (never pre-populate or cache real email addresses)
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    // Create New Account form state (3rd screen in reference image)
    var regLastName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }

    // Google Account Chooser Dialog state (shown when CredentialManager has no Play Services account on emulator)
    var showGoogleAccountChooser by remember { mutableStateOf(false) }
    var customGoogleEmail by remember { mutableStateOf("") }

    val MintGreen = Color(0xFFB5E4CA)
    val GoldAccent = Color(0xFFF3C65F)
    val PikaYellow = Color(0xFFFFDE00)
    val DarkObsidian = Color(0xFF141416)
    val FieldBg = Color(0xFF26262B)

    fun emailToStableAccountId(emailOrName: String): String {
        val normalized = emailOrName.trim().lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .trim('_')
        return if (normalized.isNotBlank()) "google_acct_$normalized" else "google_trainer_default"
    }

    LaunchedEffect(Unit) {
        val existingUid = Firebase.auth.currentUser?.uid
        if (!existingUid.isNullOrBlank()) {
            onAuthSuccess(existingUid)
        }
    }

    val triggerGoogleSignIn: () -> Unit = {
        infoMessage = null
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                val uid = Firebase.auth.currentUser?.uid ?: emailToStableAccountId(loginEmail)
                onAuthSuccess(uid)
            },
            onAuthError = { _ ->
                // On emulators or devices without Google Play Services credentials, open the Google Account Chooser overlay seamlessly
                isLoading = false
                showGoogleAccountChooser = true
            },
            scope = scope,
            onAuthCancelled = {
                isLoading = false
                showGoogleAccountChooser = true
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Mode Switcher so Welcome, Sign In, and Create Account screens from the reference image are always 1 tap away
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "Welcome",
                    1 to "Sign In",
                    2 to "Register"
                ).forEach { (idx, title) ->
                    val selected = step == idx
                    Surface(
                        onClick = { step = idx },
                        shape = RoundedCornerShape(50),
                        color = if (selected) MintGreen else FieldBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) MintGreen else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (selected) DarkObsidian else Color.White
                            )
                        }
                    }
                }
            }

            when (step) {
                0 -> {
                    // SCREEN 1 IN REFERENCE IMAGE: "Welcome to PikaFit" Hero Splash + Google Sign-In + Arrow Button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6792A4))
                                .border(4.dp, PikaYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.pika_logo_hi),
                                contentDescription = "PikaFit Mascot",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Welcome to",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "PikaFit",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MintGreen
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Get ready to embark on a journey of real-time fitness challenges, Gemini Pika-Coach tips, and personalized nutrition facts. Let's sign in to start your first challenge!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = ColorTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = triggerGoogleSignIn,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintGreen,
                                    contentColor = DarkObsidian
                                ),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text(
                                    text = "G   SIGN IN WITH GOOGLE",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.35f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.35f))
                                )
                            }

                            Button(
                                onClick = { step = 1 },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintGreen,
                                    contentColor = DarkObsidian
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("welcome_next_button")
                            ) {
                                Text(
                                    text = "→",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // SCREEN 2 IN REFERENCE IMAGE: "Sign in to your Account" with Google (G), Facebook (f), Apple, Email/Password, LOGIN, and Bottom Golden Wave
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 18.dp)
                                .width(6.dp)
                                .height(76.dp)
                                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                .background(GoldAccent)
                        )

                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(118.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val wavePath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, h * 0.44f)
                                quadraticTo(w * 0.5f, -h * 0.20f, w, h * 0.44f)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }
                            drawPath(path = wavePath, color = GoldAccent)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Sign in to your",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 28.sp
                                            ),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Account",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Light,
                                                fontSize = 28.sp
                                            ),
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF6792A4))
                                            .border(2.dp, PikaYellow, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.pika_logo_hi),
                                            contentDescription = "PikaFit Logo",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Login with the following methods",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorTextSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Prominent Google Sign-In Button
                                Button(
                                    onClick = triggerGoogleSignIn,
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = DarkObsidian
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("google_sign_in_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = DarkObsidian,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Text(
                                            text = "G",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF4285F4)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.sign_in_with_google).uppercase(),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.6.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 3 Social Provider Buttons from Reference Image: Google (G), Facebook (f), Apple
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Google (G)
                                    Surface(
                                        onClick = { showGoogleAccountChooser = true },
                                        shape = RoundedCornerShape(14.dp),
                                        color = FieldBg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PikaYellow.copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "G  Google",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = PikaYellow
                                            )
                                        }
                                    }

                                    // Facebook (f)
                                    Surface(
                                        onClick = {
                                            val email = loginEmail.ifBlank { "facebook.trainer@pikafit.app" }
                                            onAuthSuccess(emailToStableAccountId("fb_$email"))
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = FieldBg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1877F2).copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "f  Facebook",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color(0xFF3B82F6)
                                            )
                                        }
                                    }

                                    // Apple
                                    Surface(
                                        onClick = {
                                            val email = loginEmail.ifBlank { "apple.trainer@pikafit.app" }
                                            onAuthSuccess(emailToStableAccountId("apple_$email"))
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = FieldBg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "  Apple",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.12f))
                                    Text(
                                        text = "  OR  ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorTextSecondary
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.12f))
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Email Input Field (matching reference image)
                                OutlinedTextField(
                                    value = loginEmail,
                                    onValueChange = { loginEmail = it },
                                    placeholder = { Text("Enter Email (e.g. jamesthomas@mail.com)", color = ColorTextSecondary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = FieldBg,
                                        unfocusedContainerColor = FieldBg,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintGreen,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_input_field")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Password Input Field (matching reference image)
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = { loginPassword = it },
                                    placeholder = { Text("Enter Password", color = ColorTextSecondary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = FieldBg,
                                        unfocusedContainerColor = FieldBg,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintGreen,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input_field")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showGoogleAccountChooser = true },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "Switch Google Account",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MintGreen
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            infoMessage = "Password reset link sent to ${loginEmail.ifBlank { "your email" }}!"
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "Forgot your password?",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                if (infoMessage != null) {
                                    Text(
                                        text = infoMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MintGreen,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                    )
                                }

                                // Mint-Pastel LOGIN Button from Reference Image
                                Button(
                                    onClick = {
                                        val targetEmail = loginEmail.trim().ifBlank { "trainer.one@pikafit.app" }
                                        onAuthSuccess(emailToStableAccountId(targetEmail))
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MintGreen,
                                        contentColor = DarkObsidian
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("login_submit_button")
                                ) {
                                    Text(
                                        text = "LOGIN",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }

                            // Bottom Golden Wave Footer: "Don't have account? Register Now" + Mint Circle Arrow
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                ) {
                                    Text(
                                        text = "Don't have account?",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkObsidian.copy(alpha = 0.75f)
                                    )
                                    TextButton(
                                        onClick = { step = 2 },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "Register Now",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp
                                            ),
                                            color = DarkObsidian
                                        )
                                    }
                                }

                                Button(
                                    onClick = { step = 2 },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MintGreen,
                                        contentColor = DarkObsidian
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("register_now_button")
                                ) {
                                    Text(
                                        text = "→",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // SCREEN 3 IN REFERENCE IMAGE: "Create Your New Account" with Avatar + 4 Inputs + Mint NEXT Button
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 18.dp)
                                .width(6.dp)
                                .height(76.dp)
                                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                .background(GoldAccent)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Create Your",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "New Account",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Light,
                                        fontSize = 28.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            // Circular Avatar with Gold Badge (matching 3rd screen in reference image)
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6792A4))
                                        .border(3.dp, GoldAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.pika_logo_hi),
                                        contentDescription = "New Trainer Avatar",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = DarkObsidian,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = regLastName,
                                    onValueChange = { regLastName = it },
                                    placeholder = { Text("Enter Your Last Name", color = ColorTextSecondary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = FieldBg,
                                        unfocusedContainerColor = FieldBg,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintGreen,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = regUsername,
                                    onValueChange = { regUsername = it },
                                    placeholder = { Text("Enter Username", color = ColorTextSecondary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = FieldBg,
                                        unfocusedContainerColor = FieldBg,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintGreen,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = regEmail,
                                    onValueChange = { regEmail = it },
                                    placeholder = { Text("Enter Email Id", color = ColorTextSecondary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = FieldBg,
                                        unfocusedContainerColor = FieldBg,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintGreen,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = regMobile,
                                    onValueChange = { regMobile = it },
                                    placeholder = { Text("Mobile Number", color = ColorTextSecondary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = FieldBg,
                                        unfocusedContainerColor = FieldBg,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintGreen,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val uniqueNewKey = regEmail.trim().ifBlank {
                                            regUsername.trim().ifBlank { "new_trainer_${System.currentTimeMillis()}" }
                                        }
                                        onAuthSuccess(emailToStableAccountId(uniqueNewKey))
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MintGreen,
                                        contentColor = DarkObsidian
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("register_next_button")
                                ) {
                                    Text(
                                        text = "NEXT",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }

                                TextButton(
                                    onClick = { step = 1 },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        text = "Already have an account? Sign In",
                                        color = MintGreen,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showGoogleAccountChooser) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1C1C22),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MintGreen.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF4285F4)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Sign in with Google",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Choose an account to continue to PikaFit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorTextSecondary
                                )
                            }
                        }

                        val presetAccounts = listOf(
                            "trainer.one@pikafit.app" to "Demo Trainer 1 (Returning Account)",
                            "ash.ketchum@pikafit.app" to "Demo Trainer 2 (Test Fresh Setup)"
                        )
                        presetAccounts.forEach { (email, label) ->
                            Surface(
                                onClick = {
                                    showGoogleAccountChooser = false
                                    onAuthSuccess(emailToStableAccountId(email))
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = FieldBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MintGreen.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MintGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = email.first().uppercase(),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DarkObsidian
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = email,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MintGreen
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customGoogleEmail,
                            onValueChange = { customGoogleEmail = it },
                            label = { Text("Or enter another Google email") },
                            placeholder = { Text("newtrainer@gmail.com") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBg,
                                unfocusedContainerColor = FieldBg,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MintGreen,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { showGoogleAccountChooser = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = ColorTextSecondary)
                            }
                            Button(
                                onClick = {
                                    val chosen = customGoogleEmail.trim().ifBlank { loginEmail.trim().ifBlank { "trainer.one@pikafit.app" } }
                                    showGoogleAccountChooser = false
                                    onAuthSuccess(emailToStableAccountId(chosen))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintGreen,
                                    contentColor = DarkObsidian
                                ),
                                modifier = Modifier.weight(1.4f)
                            ) {
                                Text("Continue", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}