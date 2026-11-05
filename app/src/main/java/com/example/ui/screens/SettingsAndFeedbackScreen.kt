package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FeedbackEntry
import com.example.data.model.HunterSystemState
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.HunterCrimson
import com.example.ui.theme.HunterCyan
import com.example.ui.theme.HunterEmerald
import com.example.ui.theme.HunterGold
import com.example.ui.theme.HunterViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndFeedbackScreen(
    state: HunterSystemState,
    feedbackState: DataLoadState<List<FeedbackEntry>>,
    currentUserId: String,
    initialFocusSection: Int = 0, // 0 = All, 1 = Edit Profile, 2 = Public Feedback, 3 = Reset Progress
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
    onResetProgress: (clearJournalHistory: Boolean) -> Unit,
    onSubmitFeedback: (category: String, rating: Int, message: String) -> Unit,
    onUpdateFeedbackStatus: (feedbackId: String, newStatus: String) -> Unit,
    onDeleteFeedback: (feedbackId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserEmail = remember {
        FirebaseAuth.getInstance().currentUser?.email?.lowercase(Locale.US) ?: ""
    }
    val isAppCreator = currentUserEmail == CREATOR_EMAIL

    var activeSectionFilter by rememberSaveable { mutableIntStateOf(initialFocusSection) }
    LaunchedEffect(initialFocusSection) {
        activeSectionFilter = initialFocusSection
    }

    // Profile & Daily Targets State
    val bio = state.biometrics
    val vit = state.statBreakdown.vitality
    val stam = state.statBreakdown.stamina

    var nameInput by rememberSaveable { mutableStateOf(bio.hunterName) }
    var titleInput by rememberSaveable { mutableStateOf(bio.hunterTitle) }
    var weightInput by rememberSaveable { mutableStateOf(bio.weightKg.toString()) }
    var heightInput by rememberSaveable { mutableStateOf(bio.heightCm.toInt().toString()) }
    var ageInput by rememberSaveable { mutableStateOf(bio.age.toString()) }
    var sexInput by rememberSaveable { mutableStateOf(bio.biologicalSex) }
    var customBfInput by rememberSaveable { mutableStateOf(bio.customBodyFatPercent?.toString() ?: "") }
    var customMuscleInput by rememberSaveable { mutableStateOf(bio.customMuscleMassPercent?.toString() ?: "") }
    var calTargetInput by rememberSaveable { mutableStateOf(vit.caloriesTarget.toString()) }
    var waterTargetInput by rememberSaveable { mutableStateOf(stam.targetLiters.toString()) }
    var avatarBase64State by remember { mutableStateOf(bio.avatarBase64) }

    LaunchedEffect(bio, vit.caloriesTarget, stam.targetLiters) {
        nameInput = bio.hunterName
        titleInput = bio.hunterTitle
        weightInput = bio.weightKg.toString()
        heightInput = bio.heightCm.toInt().toString()
        ageInput = bio.age.toString()
        sexInput = bio.biologicalSex
        customBfInput = bio.customBodyFatPercent?.toString() ?: ""
        customMuscleInput = bio.customMuscleMassPercent?.toString() ?: ""
        calTargetInput = vit.caloriesTarget.toString()
        waterTargetInput = stam.targetLiters.toString()
        if (bio.avatarBase64.isNotBlank()) {
            avatarBase64State = bio.avatarBase64
        }
    }

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
    var clearJournalOnReset by rememberSaveable { mutableStateOf(false) }
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
                        text = "Are you sure you want to reset your Hunter progression back to Level 1?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Level & Rank reset to Level 1 (E-Rank Novice)\n" +
                            "• XP reset to 0 / 1,000 XP\n" +
                            "• Daily Productive Streak reset to 0 Days (Unverified)\n" +
                            "• Today's logged calories, water, and exercises cleared\n" +
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_and_feedback_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Section Filter Pills
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ObsidianCardElevated, ObsidianCard)
                        )
                    )
                    .border(1.5.dp, HunterCyan.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianBg)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Status HUD",
                                tint = HunterCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SYSTEM SETTINGS & PUBLIC FEEDBACK",
                                style = MaterialTheme.typography.titleMedium,
                                color = HunterCyan
                            )
                            Text(
                                text = "Edit Profile • Reset Progress • Send Public Feedback to Creator",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Section Filter Pills
                val sections = listOf(
                    0 to "ALL SECTIONS",
                    1 to "EDIT PROFILE",
                    2 to "PUBLIC FEEDBACK",
                    3 to "RESET PROGRESS"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sections.forEach { (idx, label) ->
                        val selected = activeSectionFilter == idx
                        val accent = when (idx) {
                            1 -> HunterGold
                            2 -> HunterEmerald
                            3 -> HunterCrimson
                            else -> HunterCyan
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { activeSectionFilter = idx },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.2f),
                                selectedLabelColor = accent,
                                containerColor = ObsidianBg,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = BorderSubtle,
                                selectedBorderColor = accent
                            ),
                            modifier = Modifier.testTag("settings_section_chip_$idx")
                        )
                    }
                }
            }
        }

        // 1. EDIT HUNTER PROFILE & DAILY TARGETS CARD
        if (activeSectionFilter == 0 || activeSectionFilter == 1) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.dp, HunterGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                                    text = "EDIT HUNTER PROFILE & DAILY TARGETS",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = HunterGold
                                )
                                Text(
                                    text = "Customize identity, body metrics, and daily calorie/water goals",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
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
                                .size(72.dp)
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
                                    modifier = Modifier.size(34.dp)
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
                                text = "${state.playerStatus.rank} • ${bio.hunterTitle}",
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Codename & Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Hunter Codename") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_name_input"),
                            colors = settingsTextFieldColors(HunterGold)
                        )
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Hunter Title / Class") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_title_input"),
                            colors = settingsTextFieldColors(HunterGold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weight, Height, Age
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_weight_input"),
                            colors = settingsTextFieldColors(HunterCyan)
                        )
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text("Height (cm)") },
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
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("settings_age_input"),
                            colors = settingsTextFieldColors(HunterCyan)
                        )
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
                            label = { Text("Body Fat % (Optional)") },
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
                            label = { Text("Muscle Mass % (Optional)") },
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
                            label = { Text("Daily Calorie Target (kcal)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_cal_target_input"),
                            colors = settingsTextFieldColors(HunterEmerald)
                        )
                        OutlinedTextField(
                            value = waterTargetInput,
                            onValueChange = { waterTargetInput = it },
                            label = { Text("Daily Water Target (L)") },
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
                            text = "BIOLOGICAL SEX (BMR & BF% FORMULA):",
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
                            val w = weightInput.toDoubleOrNull() ?: bio.weightKg
                            val h = heightInput.toDoubleOrNull() ?: bio.heightCm
                            val a = ageInput.toIntOrNull() ?: bio.age
                            val bf = customBfInput.toDoubleOrNull()
                            val mm = customMuscleInput.toDoubleOrNull()
                            val calTarget = calTargetInput.toIntOrNull() ?: vit.caloriesTarget
                            val waterTarget = waterTargetInput.toDoubleOrNull() ?: stam.targetLiters
                            onSaveProfileAndTargets(
                                nameInput,
                                titleInput,
                                w,
                                h,
                                a,
                                sexInput,
                                bf,
                                mm,
                                calTarget,
                                waterTarget,
                                avatarBase64State.ifBlank { null }
                            )
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
                            text = "SAVE PROFILE & DAILY TARGETS (+50 XP)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. PUBLIC APP FEEDBACK & CREATOR BOARD
        if (activeSectionFilter == 0 || activeSectionFilter == 2) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.5.dp, HunterEmerald.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
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
                                    text = "Submit public feedback directly to the creator so changes can be made accordingly",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
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
                                text = "CREATOR INBOX ACTIVE ($CREATOR_EMAIL) — You receive all public feedback in real time and can mark items In Review or Implemented.",
                                style = MaterialTheme.typography.labelMedium,
                                color = HunterEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Rating Selector (1-5 Stars)
                    Text(
                        text = "RATE YOUR HUNTER SYSTEM EXPERIENCE:",
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (filled) HunterGold.copy(alpha = 0.16f) else ObsidianBg
                                    )
                                    .border(
                                        1.dp,
                                        if (filled) HunterGold else BorderSubtle,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .testTag("feedback_star_$star")
                            ) {
                                Icon(
                                    imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Rate $star stars",
                                    tint = if (filled) HunterGold else TextSecondary,
                                    modifier = Modifier.size(24.dp)
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

                    // Category Selector
                    Text(
                        text = "FEEDBACK CATEGORY:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
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

                    // Quick Feedback Idea Chips
                    val quickIdeas = listOf(
                        "Add custom rest timer between sets",
                        "Add barcode scanner for food logging",
                        "Add weekly macro & workout summary export"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickIdeas.forEachIndexed { idx, idea ->
                            Surface(
                                color = ObsidianBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .clickable { feedbackMessage = idea }
                                    .testTag("quick_feedback_idea_$idx")
                            ) {
                                Text(
                                    text = "+ $idea",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HunterCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { feedbackMessage = it },
                        label = { Text("Your Public Feedback / Feature Suggestion") },
                        placeholder = {
                            Text("Share what features, improvements, or fixes you want the creator to build next...")
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
                            text = "SUBMIT PUBLIC FEEDBACK TO CREATOR (+35 XP)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Public Feedback & Creator Inbox Feed Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LIVE PUBLIC FEEDBACK INBOX & ROADMAP",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Real-time Firestore feed received by creator & community",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter by Status
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
                                    text = "Syncing public feedback from Firestore...",
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
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No public feedback entries in this filter yet. Be the first Hunter to submit feedback above!",
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

        // 3. RESET PROGRESS (DANGER ZONE) CARD
        if (activeSectionFilter == 0 || activeSectionFilter == 3) {
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
                                text = "RESET HUNTER PROGRESS (DANGER ZONE)",
                                style = MaterialTheme.typography.titleMedium,
                                color = HunterCrimson
                            )
                            Text(
                                text = "Reset Level, XP, Daily Streak, Quests, and Logs back to Level 1",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Use this option if you want a fresh start from Level 1 (0 XP, 0-Day Streak, and 0 daily calories/water/exercises logged). Your Hunter Codename, Height, Weight, and Daily Targets are kept intact.",
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
                                    "All historical biometric chart points & photos will be deleted"
                                } else {
                                    "Keep Recharts biometric history & progress photos"
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
                            text = "RESET HUNTER PROGRESS TO LEVEL 1",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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
