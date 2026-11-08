package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedbackEntry
import com.example.data.model.HunterBiometrics
import com.example.data.model.HunterSystemState
import com.example.ui.theme.AppThemeIsDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.HunterCrimson
import com.example.ui.theme.HunterCyan
import com.example.ui.theme.HunterEmerald
import com.example.ui.theme.HunterGold
import com.example.ui.theme.HunterViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DataLoadState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CREATOR_EMAIL = "creator@pikafit.app"

/**
 * Click-to-Go Settings Menu Hub & Sub-Pages:
 * 0 = Main Settings Menu Hub (Click-to-go rows like Screenshot reference)
 * 1 = Edit Profile, Biometrics & Target Weight/Physique
 * 2 = Feedback & Public Creator Board
 * 3 = App Data & Reset Progress
 * 4 = Achievements & 7-Tier Pokémon Class Hierarchy
 * 5 = Appearance (Dark/Light Mode & Wallpaper Guide)
 * 6 = Analytics & Goal Telemetry Summary
 * 7 = FAQ & Help Articles
 * 8 = About Us
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndFeedbackScreen(
    state: HunterSystemState,
    feedbackState: DataLoadState<List<FeedbackEntry>>,
    currentUserId: String,
    initialFocusSection: Int = 0,
    releaseInfo: com.example.data.model.AppReleaseInfo = com.example.data.model.AppReleaseInfo(),
    isDark: Boolean = AppThemeIsDark,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit,
    onSaveProfileAndTargets: (
        hunterName: String,
        hunterTitle: String,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        biologicalSex: String,
        customBodyFatPercent: Double?,
        customMuscleMassPercent: Double?,
        caloriesTarget: Int,
        waterTargetLiters: Double,
        avatarBase64: String?
    ) -> Unit,
    onSaveProfileWithGoal: (
        hunterName: String,
        hunterTitle: String,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        biologicalSex: String,
        customBodyFatPercent: Double?,
        customMuscleMassPercent: Double?,
        caloriesTarget: Int,
        waterTargetLiters: Double,
        avatarBase64: String?,
        targetWeightKg: Double?,
        targetPhysique: String?
    ) -> Unit = { name, title, w, h, a, sex, bf, mm, cal, water, avatar, _, _ ->
        onSaveProfileAndTargets(name, title, w, h, a, sex, bf, mm, cal, water, avatar)
    },
    onResetProgress: (clearJournalHistory: Boolean) -> Unit,
    onSubmitFeedback: (category: String, rating: Int, message: String) -> Unit,
    onUpdateFeedbackStatus: (feedbackId: String, newStatus: String) -> Unit,
    onDeleteFeedback: (feedbackId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserEmail = remember {
        val fbEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase(Locale.US)
        if (!fbEmail.isNullOrBlank()) {
            fbEmail
        } else {
            context.getSharedPreferences("pikafit_auth_session_prefs", Context.MODE_PRIVATE)
                .getString("user_account_email", "")?.lowercase(Locale.US) ?: ""
        }
    }
    val isAppCreator = currentUserEmail == "tmcrainbow093@gmail.com" || currentUserEmail == CREATOR_EMAIL

    var activePage by rememberSaveable { mutableIntStateOf(initialFocusSection) }
    LaunchedEffect(initialFocusSection) {
        activePage = initialFocusSection
    }

    if (activePage != 0) {
        BackHandler {
            activePage = 0
        }
    }

    // Profile & Daily Targets State (Empty default strings so no pre-cached values clutter inputs)
    val bio = state.biometrics
    val vit = state.statBreakdown.vitality
    val stam = state.statBreakdown.stamina

    var nameInput by rememberSaveable { mutableStateOf("") }
    var weightInput by rememberSaveable { mutableStateOf("") }
    var targetWeightInput by rememberSaveable { mutableStateOf("") }
    var selectedPhysique by rememberSaveable(bio.targetPhysique) { mutableStateOf(bio.targetPhysique) }
    var heightInput by rememberSaveable { mutableStateOf("") }
    var ageInput by rememberSaveable { mutableStateOf("") }
    var sexInput by rememberSaveable(bio.biologicalSex) { mutableStateOf(bio.biologicalSex) }
    var customBfInput by rememberSaveable { mutableStateOf("") }
    var customMuscleInput by rememberSaveable { mutableStateOf("") }
    var calTargetInput by rememberSaveable { mutableStateOf("") }
    var waterTargetInput by rememberSaveable { mutableStateOf("") }
    var avatarBase64State by remember(bio.avatarBase64) { mutableStateOf(bio.avatarBase64) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val encoded = encodeAvatarUriToBase64(context, uri)
                if (encoded.isNotBlank()) {
                    avatarBase64State = encoded
                }
            }
        }
    }

    // Public Feedback State
    var feedbackCategory by rememberSaveable { mutableStateOf("FEATURE_REQUEST") }
    var feedbackRating by rememberSaveable { mutableIntStateOf(5) }
    var feedbackMessage by rememberSaveable { mutableStateOf("") }
    var boardStatusFilter by rememberSaveable { mutableStateOf("ALL") }

    // Reset Progress State
    var clearJournalOnReset by rememberSaveable { mutableStateOf(true) }
    var showResetConfirmDialog by rememberSaveable { mutableStateOf(false) }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = ObsidianCard,
            titleContentColor = HunterCrimson,
            textContentColor = TextPrimary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = HunterCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONFIRM PROGRESS RESET",
                        style = MaterialTheme.typography.titleLarge,
                        color = HunterCrimson
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to reset your Hunter progression and clear all cached daily logs?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Level & Class reset to Level 1 (Beginner • Pikachu)\n" +
                            "• XP reset to 0 / 1,500 XP\n" +
                            "• Daily Productive Streak reset to 0 Days\n" +
                            "• Today's logged calories, water, and exercises cleared to 0\n" +
                            if (clearJournalOnReset) {
                                "• Biometric & Physique Journal History WILL BE DELETED"
                            } else {
                                "• Biometric & Physique Journal History will be PRESERVED"
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetProgress(clearJournalOnReset)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HunterCrimson,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.testTag("confirm_reset_progress_button")
                ) {
                    Text("CONFIRM RESET TO LVL 1", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_reset_progress_button")
                ) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    val pageTitle = when (activePage) {
        1 -> "Edit Profile"
        2 -> "Feedback"
        3 -> "App Data"
        4 -> "Achievements & Classes"
        5 -> "Appearance"
        6 -> "Analytics"
        7 -> "FAQ & Help Articles"
        8 -> "About Us"
        else -> "Settings"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_and_feedback_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // =====================================================================
        // TOP BAR (Close / Back + Page Title)
        // =====================================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (activePage == 0) {
                            onBack()
                        } else {
                            activePage = 0
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    if (activePage != 0) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings",
                            tint = HunterCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = HunterCrimson
                        )
                    } else {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = HunterCrimson
                        )
                    }
                }

                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = TextPrimary
                )

                // Balance spacer so title stays centered
                Spacer(modifier = Modifier.width(64.dp))
            }
        }

        // =====================================================================
        // MAIN CLICK-TO-GO SETTINGS HUB (activePage == 0)
        // =====================================================================
        if (activePage == 0) {
            // 1. Profile Hero Click-to-Go Card (Matches Screenshot 5)
            item {
                val avatarBitmap = remember(avatarBase64State) {
                    decodeAvatarBase64(avatarBase64State)
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { activePage = 1 }
                        .testTag("settings_section_chip_1"),
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianBg)
                                    .border(2.dp, HunterGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarBitmap != null) {
                                    Image(
                                        bitmap = avatarBitmap.asImageBitmap(),
                                        contentDescription = "Hunter Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    PikachuMascotBadge(
                                        drawableRes = state.playerStatus.currentTier.pokemonDrawableRes,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bio.hunterName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUserEmail.ifBlank {
                                        "${state.playerStatus.currentTier.classTitle} • ${state.playerStatus.currentTier.pokemonName} (${state.playerStatus.currentXp} XP)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap to edit profile, target weight & daily goals",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HunterGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Open Edit Profile",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 2. Achievements & Class Hierarchy Click-to-Go Card
            item {
                SettingsMenuGroupCard {
                    SettingsClickToGoRow(
                        icon = Icons.Filled.EmojiEvents,
                        iconTint = HunterGold,
                        title = "Achievements & Class Hierarchy",
                        subtitle = "${state.playerStatus.currentTier.classTitle} (${state.playerStatus.currentTier.pokemonName}) • ${state.unlockedAchievements.size} Unlocked",
                        testTag = "settings_nav_achievements",
                        onClick = { activePage = 4 }
                    )
                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.6f), modifier = Modifier.padding(start = 52.dp))
                    SettingsClickToGoRow(
                        icon = Icons.Filled.Edit,
                        iconTint = HunterCyan,
                        title = "Edit Profile & Target Physique",
                        subtitle = "Weight: ${bio.weightKg}kg → Goal: ${bio.targetWeightKg}kg (${bio.targetPhysique})",
                        testTag = "settings_nav_edit_profile",
                        onClick = { activePage = 1 }
                    )
                }
            }

            // 3. Appearance Group
            item {
                SettingsMenuGroupCard {
                    SettingsClickToGoRow(
                        icon = Icons.Filled.Palette,
                        iconTint = HunterViolet,
                        title = "Appearance",
                        subtitle = if (isDark) "Dark Obsidian Mode" else "Light Volt Mode",
                        testTag = "settings_nav_appearance",
                        onClick = { activePage = 5 }
                    )
                }
            }

            // 4. App Data & Analytics Group
            item {
                SettingsMenuGroupCard {
                    SettingsClickToGoRow(
                        icon = Icons.Filled.Storage,
                        iconTint = HunterCrimson,
                        title = "App Data",
                        subtitle = "Clear cached logs or reset progress to Level 1",
                        testTag = "settings_section_chip_3",
                        onClick = { activePage = 3 }
                    )
                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.6f), modifier = Modifier.padding(start = 52.dp))
                    SettingsClickToGoRow(
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        iconTint = HunterCyan,
                        title = "Analytics",
                        subtitle = "BMI ${bio.bmi} • Goal ${bio.recommendedCaloriesTarget} kcal • ${bio.targetWeightSummaryLabel}",
                        testTag = "settings_nav_analytics",
                        onClick = { activePage = 6 }
                    )
                }
            }

            // 5. FAQ & Help Articles Group
            item {
                SettingsMenuGroupCard {
                    SettingsClickToGoRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconTint = HunterGold,
                        title = "FAQ & Help Articles",
                        subtitle = "How XP scaling, Smart Grams & Streaks work",
                        testTag = "settings_nav_faq",
                        onClick = { activePage = 7 }
                    )
                }
            }

            // 6. Feedback & About Us Group
            item {
                SettingsMenuGroupCard {
                    SettingsClickToGoRow(
                        icon = Icons.Filled.Feedback,
                        iconTint = HunterEmerald,
                        title = "Feedback",
                        subtitle = "Rate the app & send suggestions to Creator Board",
                        testTag = "settings_section_chip_2",
                        onClick = { activePage = 2 }
                    )
                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.6f), modifier = Modifier.padding(start = 52.dp))
                    val hasUpdate = releaseInfo.latestVersionCode > 1L
                    SettingsClickToGoRow(
                        icon = Icons.Filled.SystemUpdate,
                        iconTint = if (hasUpdate) HunterGold else HunterCyan,
                        title = "Check for Updates",
                        subtitle = if (hasUpdate) {
                            "v${releaseInfo.latestVersionName} Available • Tap to Download"
                        } else {
                            "Installed v1.0 • Up to date"
                        },
                        testTag = "settings_nav_check_updates",
                        onClick = {
                            val rawUrl = releaseInfo.downloadUrl.ifBlank { "https://github.com/" }
                            val targetUri = Uri.parse(
                                if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) "https://$rawUrl" else rawUrl
                            )
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.6f), modifier = Modifier.padding(start = 52.dp))
                    SettingsClickToGoRow(
                        icon = Icons.Filled.Info,
                        iconTint = HunterCyan,
                        title = "About Us",
                        subtitle = "PikaFit Hunter System • Pokémon Class Edition",
                        testTag = "settings_nav_about",
                        onClick = { activePage = 8 }
                    )
                }
            }
        }

        // =====================================================================
        // SUB-PAGE 1: EDIT PROFILE, BIOMETRICS & TARGET GOALS
        // =====================================================================
        if (activePage == 1) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.dp, HunterGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = HunterGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "EDIT HUNTER PROFILE & TARGET GOALS",
                                style = MaterialTheme.typography.titleMedium,
                                color = HunterGold
                            )
                            Text(
                                text = "Leave any field blank to keep your current saved value",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Avatar & Identity Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val avatarBitmap = remember(avatarBase64State) {
                            decodeAvatarBase64(avatarBase64State)
                        }
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(ObsidianBg)
                                .border(2.dp, HunterGold, CircleShape)
                                .clickable {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("settings_avatar_picker"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap.asImageBitmap(),
                                    contentDescription = "Hunter Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Choose Avatar Photo",
                                    tint = HunterGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bio.hunterName,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "${state.playerStatus.currentTier.classTitle} • ${state.playerStatus.currentTier.pokemonName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = HunterCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, HunterGold.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddAPhoto,
                                    contentDescription = null,
                                    tint = HunterGold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CHANGE AVATAR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HunterGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Codename & App-Assigned Class
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Hunter Codename") },
                            placeholder = { Text(bio.hunterName) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_name_input"),
                            colors = settingsTextFieldColors(HunterGold)
                        )
                        OutlinedTextField(
                            value = "${state.playerStatus.currentTier.classTitle} • ${state.playerStatus.currentTier.pokemonName}",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("App-Assigned Class") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_title_input"),
                            colors = settingsTextFieldColors(HunterGold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weight, Target Weight, Height, Age
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            placeholder = { Text("${bio.weightKg}") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_weight_input"),
                            colors = settingsTextFieldColors(HunterCyan)
                        )
                        OutlinedTextField(
                            value = targetWeightInput,
                            onValueChange = { targetWeightInput = it },
                            label = { Text("Target Wt (kg)") },
                            placeholder = { Text("${bio.targetWeightKg}") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_target_weight_input"),
                            colors = settingsTextFieldColors(HunterEmerald)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text("Height (cm)") },
                            placeholder = { Text("${bio.heightCm.toInt()}") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_height_input"),
                            colors = settingsTextFieldColors(HunterCyan)
                        )
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("Age (yrs)") },
                            placeholder = { Text("${bio.age}") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_age_input"),
                            colors = settingsTextFieldColors(HunterCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target Physique Goal Chips
                    Text(
                        text = "TARGET PHYSIQUE GOAL:",
                        style = MaterialTheme.typography.labelMedium,
                        color = HunterEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HunterBiometrics.PHYSIQUE_OPTIONS.forEach { option ->
                            val selected = selectedPhysique.equals(option, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { selectedPhysique = option },
                                label = { Text(option, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HunterEmerald.copy(alpha = 0.22f),
                                    selectedLabelColor = HunterEmerald,
                                    containerColor = ObsidianBg,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = BorderSubtle,
                                    selectedBorderColor = HunterEmerald
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optional Custom Body Fat % & Muscle Mass %
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customBfInput,
                            onValueChange = { customBfInput = it },
                            label = { Text("Body Fat % (Opt)") },
                            placeholder = { Text("Auto: ${bio.bodyFatPercent}%") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_bf_input"),
                            colors = settingsTextFieldColors(HunterGold)
                        )
                        OutlinedTextField(
                            value = customMuscleInput,
                            onValueChange = { customMuscleInput = it },
                            label = { Text("Muscle Mass % (Opt)") },
                            placeholder = { Text("Auto: ${bio.muscleMassPercent}%") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_muscle_input"),
                            colors = settingsTextFieldColors(HunterEmerald)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Daily Calorie Target & Daily Water Target
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = calTargetInput,
                            onValueChange = { calTargetInput = it },
                            label = { Text("Daily Calorie Goal") },
                            placeholder = { Text("${vit.caloriesTarget} kcal") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_cal_target_input"),
                            colors = settingsTextFieldColors(HunterEmerald)
                        )
                        OutlinedTextField(
                            value = waterTargetInput,
                            onValueChange = { waterTargetInput = it },
                            label = { Text("Daily Water Goal (L)") },
                            placeholder = { Text("${stam.targetLiters} L") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_water_target_input"),
                            colors = settingsTextFieldColors(HunterCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Biological Sex Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "BIOLOGICAL SEX:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MALE", "FEMALE").forEach { sexOption ->
                                val selected = sexInput.equals(sexOption, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { sexInput = sexOption },
                                    label = { Text(sexOption, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HunterGold.copy(alpha = 0.22f),
                                        selectedLabelColor = HunterGold,
                                        containerColor = ObsidianBg,
                                        labelColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("settings_sex_chip_${sexOption.lowercase()}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val finalName = nameInput.trim().ifBlank { bio.hunterName }
                            val w = weightInput.toDoubleOrNull() ?: bio.weightKg
                            val tw = targetWeightInput.toDoubleOrNull() ?: bio.targetWeightKg
                            val h = heightInput.toDoubleOrNull() ?: bio.heightCm
                            val a = ageInput.toIntOrNull() ?: bio.age
                            val bf = customBfInput.toDoubleOrNull() ?: bio.customBodyFatPercent
                            val mm = customMuscleInput.toDoubleOrNull() ?: bio.customMuscleMassPercent
                            val calTarget = calTargetInput.toIntOrNull() ?: vit.caloriesTarget
                            val waterTarget = waterTargetInput.toDoubleOrNull() ?: stam.targetLiters
                            onSaveProfileWithGoal(
                                finalName,
                                state.playerStatus.currentTier.classTitle,
                                w,
                                h,
                                a,
                                sexInput,
                                bf,
                                mm,
                                calTarget,
                                waterTarget,
                                avatarBase64State.ifBlank { null },
                                tw,
                                selectedPhysique
                            )
                            nameInput = ""
                            weightInput = ""
                            targetWeightInput = ""
                            heightInput = ""
                            ageInput = ""
                            customBfInput = ""
                            customMuscleInput = ""
                            calTargetInput = ""
                            waterTargetInput = ""
                            activePage = 0
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HunterGold,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_save_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SAVE PROFILE & DAILY TARGETS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =====================================================================
        // SUB-PAGE 2: PUBLIC APP FEEDBACK & CREATOR BOARD
        // =====================================================================
        if (activePage == 2) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.5.dp, HunterEmerald.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Feedback,
                            contentDescription = null,
                            tint = HunterEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PUBLIC APP FEEDBACK & CREATOR BOARD",
                                style = MaterialTheme.typography.titleMedium,
                                color = HunterEmerald
                            )
                            Text(
                                text = "Submit feedback directly to the creator so changes can be made",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    if (isAppCreator) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(HunterEmerald.copy(alpha = 0.14f))
                                .border(1.dp, HunterEmerald, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = HunterEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CREATOR INBOX ACTIVE ($CREATOR_EMAIL)",
                                style = MaterialTheme.typography.labelMedium,
                                color = HunterEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Rating Selector (1-5 Stars)
                    Text(
                        text = "RATE YOUR EXPERIENCE:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { star ->
                            val filled = star <= feedbackRating
                            IconButton(
                                onClick = { feedbackRating = star },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (filled) HunterGold.copy(alpha = 0.16f) else ObsidianBg)
                                    .border(1.dp, if (filled) HunterGold else BorderSubtle, RoundedCornerShape(10.dp))
                                    .testTag("feedback_star_$star")
                            ) {
                                Icon(
                                    imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Rate $star stars",
                                    tint = if (filled) HunterGold else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$feedbackRating / 5 STARS",
                            style = MaterialTheme.typography.labelLarge,
                            color = HunterGold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val categories = listOf(
                        "FEATURE_REQUEST" to "Feature Request",
                        "UI_UX" to "UI / UX",
                        "BUG_REPORT" to "Bug Report",
                        "GENERAL" to "General"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (key, label) ->
                            val selected = feedbackCategory == key
                            FilterChip(
                                selected = selected,
                                onClick = { feedbackCategory = key },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HunterEmerald.copy(alpha = 0.2f),
                                    selectedLabelColor = HunterEmerald,
                                    containerColor = ObsidianBg,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = BorderSubtle,
                                    selectedBorderColor = HunterEmerald
                                ),
                                modifier = Modifier.testTag("feedback_cat_${key.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { feedbackMessage = it },
                        label = { Text("Your Public Feedback / Feature Suggestion") },
                        placeholder = {
                            Text("Share what features, improvements, or fixes you want next...")
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_message_input"),
                        colors = settingsTextFieldColors(HunterEmerald)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (feedbackMessage.isNotBlank()) {
                                onSubmitFeedback(
                                    feedbackCategory,
                                    feedbackRating,
                                    feedbackMessage.trim()
                                )
                                feedbackMessage = ""
                            }
                        },
                        enabled = feedbackMessage.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HunterEmerald,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_feedback_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SUBMIT PUBLIC FEEDBACK",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "LIVE PUBLIC FEEDBACK INBOX",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val statusFilters = listOf("ALL", "OPEN", "IN_REVIEW", "IMPLEMENTED")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusFilters.forEach { st ->
                            val selected = boardStatusFilter == st
                            FilterChip(
                                selected = selected,
                                onClick = { boardStatusFilter = st },
                                label = {
                                    Text(
                                        text = st.replace("_", " "),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HunterCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = HunterCyan,
                                    containerColor = ObsidianBg,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("feedback_filter_${st.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (feedbackState) {
                        is DataLoadState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = HunterEmerald,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Syncing public feedback...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        is DataLoadState.Error -> {
                            Text(
                                text = feedbackState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = HunterCrimson
                            )
                        }
                        is DataLoadState.Success -> {
                            val filteredList = feedbackState.data.filter { item ->
                                boardStatusFilter == "ALL" || item.status == boardStatusFilter
                            }
                            if (filteredList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianBg)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No feedback entries yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    filteredList.forEach { entry ->
                                        PublicFeedbackItemCard(
                                            entry = entry,
                                            canManage = isAppCreator || entry.userId == currentUserId,
                                            onUpdateStatus = { newStatus ->
                                                onUpdateFeedbackStatus(entry.id, newStatus)
                                            },
                                            onDelete = {
                                                onDeleteFeedback(entry.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // SUB-PAGE 3: APP DATA & RESET PROGRESS
        // =====================================================================
        if (activePage == 3) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.5.dp, HunterCrimson.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = null,
                            tint = HunterCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "APP DATA & RESET HUNTER PROGRESS",
                                style = MaterialTheme.typography.titleMedium,
                                color = HunterCrimson
                            )
                            Text(
                                text = "Clear all cached daily logs, XP, streaks, and journal history",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Resetting progress clears all daily calories, water logs, workout exercises, XP, and streaks back to 0 so you start completely fresh with zero cached data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianBg)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Also Clear Biometric & Photo Journal History",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = if (clearJournalOnReset) {
                                    "All historical chart points & journal photos will be deleted"
                                } else {
                                    "Keep biometric history & progress photos"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (clearJournalOnReset) HunterCrimson else TextSecondary
                            )
                        }
                        Switch(
                            checked = clearJournalOnReset,
                            onCheckedChange = { clearJournalOnReset = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = HunterCrimson,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ObsidianCard
                            ),
                            modifier = Modifier.testTag("reset_clear_journal_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HunterCrimson.copy(alpha = 0.18f),
                            contentColor = HunterCrimson
                        ),
                        border = BorderStroke(1.dp, HunterCrimson),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_reset_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CLEAR DATA & RESET TO LEVEL 1",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =====================================================================
        // SUB-PAGE 4: ACHIEVEMENTS & 7-TIER POKÉMON CLASS HIERARCHY
        // =====================================================================
        if (activePage == 4) {
            item {
                PokemonClassHierarchyAndAchievementsCard(
                    playerStatus = state.playerStatus,
                    unlockedAchievements = state.unlockedAchievements
                )
            }
        }

        // =====================================================================
        // SUB-PAGE 5: APPEARANCE
        // =====================================================================
        if (activePage == 5) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.dp, HunterViolet.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.DarkMode,
                                contentDescription = null,
                                tint = HunterGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Dark Obsidian Theme",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isDark) "Active (Dark Obsidian Mode)" else "Active (Light Volt Mode)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { onToggleTheme() }
                        )
                    }

                    HorizontalDivider(color = BorderSubtle)

                    Text(
                        text = "TYPOGRAPHY & WALLPAPER",
                        style = MaterialTheme.typography.labelMedium,
                        color = HunterGold
                    )
                    Text(
                        text = "• Headings & Big Stats: Integral CF Bold\n• UI, Trackers & Active Workouts: SF Pro Semibold\n• Tip: Tap the Wallpaper icon in the top bar anytime to cycle Pikachu backgrounds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // =====================================================================
        // SUB-PAGE 6: ANALYTICS & GOAL TELEMETRY
        // =====================================================================
        if (activePage == 6) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.dp, HunterCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = HunterCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ANALYTICS & GOAL PROGRESS",
                            style = MaterialTheme.typography.titleMedium,
                            color = HunterCyan
                        )
                    }

                    Text(
                        text = "• Current Weight: ${bio.weightKg} kg → Target Weight: ${bio.targetWeightKg} kg (${bio.targetWeightSummaryLabel})\n" +
                            "• Target Physique: ${bio.targetPhysique}\n" +
                            "• BMI: ${bio.bmi} (${bio.bmiCategory}) • Body Fat: ${bio.bodyFatPercent}%\n" +
                            "• Muscle Mass: ${bio.muscleMassPercent}% • Lean Mass: ${bio.leanBodyMassKg} kg\n" +
                            "• BMR: ${bio.bmrKcal} kcal/day • Goal Intake: ${bio.recommendedCaloriesTarget} kcal/day\n" +
                            "• Current Class: ${state.playerStatus.currentTier.classTitle} (${state.playerStatus.currentTier.pokemonName}) • ${state.playerStatus.currentXp} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        // =====================================================================
        // SUB-PAGE 7: FAQ & HELP ARTICLES
        // =====================================================================
        if (activePage == 7) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.dp, HunterGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "FAQ & HELP ARTICLES",
                        style = MaterialTheme.typography.titleMedium,
                        color = HunterGold
                    )
                    Text(
                        text = "1. How do Pokémon Classes & XP Scaling work?\n" +
                            "Your class is automatically assigned by your total XP: Beginner (Pikachu, 0–1,500 XP), Challenger (Charizard, 1,500–3,000 XP), Leader (Greninja, 3,000–5,000 XP), Elite I (Mega Lucario, 5,000–7,500 XP), Elite II (Garchomp, 7,500–9,000 XP), Elite III (Rayquaza, 9,000–10,000 XP), and Champion (Arceus, 10,000+ XP). Early tiers grant a 1.6x XP boost, gradually tapering at higher ranks.\n\n" +
                            "2. How does Smart Grams & Quantity work?\n" +
                            "In the Workout & Nutrition tab, switch between Quantity mode (e.g., '3 Dosa 4 Idly') and Grams mode (e.g., '150g Chicken Breast') to automatically compute calories, protein, carbs, fats, and fiber.\n\n" +
                            "3. How do Daily Productive Streaks work?\n" +
                            "Log at least one meal or exercise set each day to verify your daily streak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // =====================================================================
        // SUB-PAGE 8: ABOUT US
        // =====================================================================
        if (activePage == 8) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.dp, HunterCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ABOUT PIKAFIT HUNTER SYSTEM",
                        style = MaterialTheme.typography.titleMedium,
                        color = HunterCyan
                    )
                    Text(
                        text = "PikaFit combines scientific biometric telemetry, smart South Indian & global nutrition tracking, post-workout expenditure analysis, PikaBot AI dietitian coaching, and a 7-tier Pokémon class progression system.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuGroupCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsClickToGoRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.5.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Open $title",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PublicFeedbackItemCard(
    entry: FeedbackEntry,
    canManage: Boolean,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (entry.status) {
        "IMPLEMENTED" -> HunterEmerald
        "IN_REVIEW" -> HunterGold
        else -> HunterCyan
    }
    val categoryLabel = when (entry.category) {
        "FEATURE_REQUEST" -> "FEATURE REQUEST"
        "BUG_REPORT" -> "BUG REPORT"
        "UI_UX" -> "UI / UX"
        else -> "GENERAL"
    }
    val dateStr = remember(entry.createdAt) {
        val date = entry.createdAt?.toDate() ?: Date()
        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(date)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianBg)
            .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("feedback_card_${entry.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.hunterName,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = HunterViolet.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, HunterViolet.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = HunterViolet,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                color = statusColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, statusColor)
            ) {
                Text(
                    text = entry.status.replace("_", " "),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val stars = "★".repeat(entry.rating.toInt().coerceIn(1, 5)) +
                "☆".repeat((5 - entry.rating.toInt()).coerceIn(0, 4))
            Text(
                text = "$stars (${entry.rating}/5)",
                style = MaterialTheme.typography.labelMedium,
                color = HunterGold
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )

        if (canManage) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    listOf(
                        "OPEN" to HunterCyan,
                        "IN_REVIEW" to HunterGold,
                        "IMPLEMENTED" to HunterEmerald
                    ).forEach { (st, color) ->
                        val active = entry.status == st
                        Surface(
                            color = if (active) color.copy(alpha = 0.22f) else ObsidianCard,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (active) color else BorderSubtle),
                            modifier = Modifier
                                .clickable { onUpdateStatus(st) }
                                .testTag("feedback_status_${entry.id}_${st.lowercase()}")
                        ) {
                            Text(
                                text = st.replace("_", " "),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) color else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("delete_feedback_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Feedback",
                        tint = HunterCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun settingsTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = BorderSubtle,
    focusedLabelColor = accent,
    unfocusedLabelColor = TextSecondary,
    cursorColor = accent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = ObsidianBg,
    unfocusedContainerColor = ObsidianBg
)

private suspend fun encodeAvatarUriToBase64(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext ""
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext ""
        val maxDim = 220
        val ratio = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height, 1f)
        val w = (decoded.width * ratio).toInt().coerceAtLeast(1)
        val h = (decoded.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(decoded, w, h, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    }
}

private fun decodeAvatarBase64(base64Str: String): Bitmap? {
    if (base64Str.isBlank()) return null
    return try {
        val bytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
