package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AchievementItem
import com.example.data.model.HunterBiometrics
import com.example.data.model.HunterSystemState
import com.example.data.model.JournalEntry
import com.example.data.model.PlayerStatus
import com.example.data.model.ProductiveTaskItem
import com.example.data.model.StreakDateUtils
import com.example.data.model.TrainerRankProgression
import com.example.data.model.TrainerRankTier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileAndJournalTab(
    state: HunterSystemState,
    journalState: DataLoadState<List<JournalEntry>>,
    onSaveBiometrics: (
        hunterName: String,
        hunterTitle: String,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        biologicalSex: String,
        avatarBase64: String?
    ) -> Unit,
    onSaveBiometricsWithGoal: (
        hunterName: String,
        hunterTitle: String,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        biologicalSex: String,
        avatarBase64: String?,
        targetWeightKg: Double,
        targetPhysique: String
    ) -> Unit = { name, title, w, h, a, sex, avatar, _, _ ->
        onSaveBiometrics(name, title, w, h, a, sex, avatar)
    },
    onLogProgressPhoto: (
        title: String,
        caption: String,
        poseTag: String,
        photoBase64: String,
        setAsAvatar: Boolean
    ) -> Unit,
    onDeleteJournalEntry: (String) -> Unit,
    onOpenAchievementsInSettings: () -> Unit = {},
    onLogProductiveTask: (title: String, category: String, dateStr: String) -> Unit = { _, _, _ -> },
    onEditProductiveTask: (dateStr: String, taskId: String, newTitle: String, newCategory: String) -> Unit = { _, _, _, _ -> },
    onRemoveProductiveTask: (dateStr: String, taskId: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedBio = state.biometrics

    val currentTier = state.playerStatus.currentTier
    val autoAssignedTitle = "${currentTier.classTitle} • ${currentTier.pokemonName}"

    // Editable Profile & Biometrics state (empty strings by default; placeholders show current values)
    var hunterName by rememberSaveable { mutableStateOf("") }
    var weightInput by rememberSaveable { mutableStateOf("") }
    var heightInput by rememberSaveable { mutableStateOf("") }
    var ageInput by rememberSaveable { mutableStateOf("") }
    var targetWeightInput by rememberSaveable { mutableStateOf("") }
    var targetPhysiqueInput by rememberSaveable(savedBio.targetPhysique) { mutableStateOf(savedBio.targetPhysique) }
    var sexInput by rememberSaveable(savedBio.biologicalSex) { mutableStateOf(savedBio.biologicalSex) }
    var avatarBase64 by remember(savedBio.avatarBase64) { mutableStateOf(savedBio.avatarBase64) }

    // Live preview calculation based on current text fields (falls back to savedBio when input is blank)
    val liveBio = remember(
        hunterName,
        autoAssignedTitle,
        weightInput,
        heightInput,
        ageInput,
        targetWeightInput,
        targetPhysiqueInput,
        sexInput,
        avatarBase64,
        savedBio
    ) {
        HunterBiometrics(
            hunterName = hunterName.ifBlank { savedBio.hunterName },
            hunterTitle = autoAssignedTitle,
            avatarBase64 = avatarBase64,
            weightKg = weightInput.toDoubleOrNull()?.coerceIn(25.0, 350.0) ?: savedBio.weightKg,
            heightCm = heightInput.toDoubleOrNull()?.coerceIn(100.0, 250.0) ?: savedBio.heightCm,
            age = ageInput.toIntOrNull()?.coerceIn(10, 120) ?: savedBio.age,
            biologicalSex = sexInput,
            fitnessGoal = savedBio.fitnessGoal,
            experienceLevel = savedBio.experienceLevel,
            onboardingCompleted = savedBio.onboardingCompleted,
            targetWeightKg = targetWeightInput.toDoubleOrNull()?.coerceIn(25.0, 350.0) ?: savedBio.targetWeightKg,
            targetPhysique = targetPhysiqueInput.ifBlank { savedBio.targetPhysique }
        )
    }

    // Progress Photo Upload State
    var photoTitle by rememberSaveable { mutableStateOf("") }
    var photoCaption by rememberSaveable { mutableStateOf("") }
    var selectedPoseTag by rememberSaveable { mutableStateOf("Front Double Bicep") }
    var stagedPhotoBase64 by remember { mutableStateOf("") }
    var setAsProfileAvatar by rememberSaveable { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for Avatar
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val encoded = encodeUriToBase64(context, uri, maxDim = 220, quality = 70)
                if (encoded.isNotBlank()) {
                    avatarBase64 = encoded
                    onSaveBiometricsWithGoal(
                        liveBio.hunterName,
                        liveBio.hunterTitle,
                        liveBio.weightKg,
                        liveBio.heightCm,
                        liveBio.age,
                        liveBio.biologicalSex,
                        encoded,
                        liveBio.targetWeightKg,
                        liveBio.targetPhysique
                    )
                }
            }
        }
    }

    // Zero-permission Android Photo Picker for Progress Photo Journal
    val progressPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val encoded = encodeUriToBase64(context, uri, maxDim = 480, quality = 72)
                if (encoded.isNotBlank()) {
                    stagedPhotoBase64 = encoded
                }
            }
        }
    }

    // Zero-permission TakePicturePreview camera capture for instant progress check-in
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                val encoded = encodeBitmapToBase64(bitmap, maxDim = 480, quality = 75)
                if (encoded.isNotBlank()) {
                    stagedPhotoBase64 = encoded
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_journal_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Compact Hunter Profile, Target Weight & Physique Goal Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ObsidianCardElevated, ObsidianCard)
                        )
                    )
                    .border(1.2.dp, HunterCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = HunterCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.profile_biometrics_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = HunterCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = HunterGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, HunterGold.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "AUTO-JOURNALS",
                            style = MaterialTheme.typography.labelSmall,
                            color = HunterGold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compact Avatar + Codename + App-Assigned Class Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ObsidianBg)
                            .border(2.dp, HunterCyan, CircleShape)
                            .clickable {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("avatar_picker_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        val decodedAvatar = remember(avatarBase64) { decodeBase64ToBitmap(avatarBase64) }
                        if (decodedAvatar != null) {
                            Image(
                                bitmap = decodedAvatar.asImageBitmap(),
                                contentDescription = "Trainer Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Upload Avatar",
                                tint = HunterCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = hunterName,
                            onValueChange = { hunterName = it },
                            label = { Text("Trainer Codename", fontSize = 11.sp, maxLines = 1) },
                            placeholder = { Text(savedBio.hunterName, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_hunter_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HunterCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Compact App-Assigned Class Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBg)
                            .border(1.dp, HunterGold.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .clickable { onOpenAchievementsInSettings() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("auto_assigned_hunter_title"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PikachuMascotBadge(
                            drawableRes = currentTier.pokemonDrawableRes,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = currentTier.classTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = HunterGold,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Text(
                                text = "${state.playerStatus.currentXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = HunterEmerald,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4-Column Compact Row: Current Weight, Target Weight, Height, Age
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)", fontSize = 10.sp, maxLines = 1) },
                        placeholder = { Text("${savedBio.weightKg}", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_weight_kg"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = targetWeightInput,
                        onValueChange = { targetWeightInput = it },
                        label = { Text("Target (kg)", fontSize = 10.sp, maxLines = 1) },
                        placeholder = { Text("${savedBio.targetWeightKg}", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_target_weight_kg"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Height (cm)", fontSize = 10.sp, maxLines = 1) },
                        placeholder = { Text("${savedBio.heightCm.toInt()}", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_height_cm"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it },
                        label = { Text("Age", fontSize = 10.sp, maxLines = 1) },
                        placeholder = { Text("${savedBio.age}", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("input_age_years"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TARGET WEIGHT & TARGET PHYSIQUE GOAL SELECTOR IN JOURNAL
                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HunterGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_target_goal_card")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 GOAL: ${liveBio.weightKg} KG → ${liveBio.targetWeightKg} KG",
                                style = MaterialTheme.typography.labelLarge,
                                color = HunterGold,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = liveBio.targetWeightSummaryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = HunterEmerald,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "TARGET PHYSIQUE GOAL: ${liveBio.targetPhysique.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = HunterCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HunterBiometrics.PHYSIQUE_OPTIONS.forEach { physiqueOption ->
                                val selected = liveBio.targetPhysique.equals(physiqueOption, ignoreCase = true)
                                Surface(
                                    color = if (selected) HunterGold.copy(alpha = 0.22f) else ObsidianCardElevated,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, if (selected) HunterGold else BorderSubtle),
                                    modifier = Modifier
                                        .clickable { targetPhysiqueInput = physiqueOption }
                                        .testTag("physique_chip_${physiqueOption.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = physiqueOption,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) HunterGold else TextSecondary,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Compact Sex Selector + Save Button on the same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MALE", "FEMALE").forEach { option ->
                        FilterChip(
                            selected = sexInput.uppercase() == option,
                            onClick = { sexInput = option },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HunterCyan.copy(alpha = 0.22f),
                                selectedLabelColor = HunterCyan,
                                containerColor = ObsidianBg,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("sex_chip_$option")
                        )
                    }

                    Button(
                        onClick = {
                            onSaveBiometricsWithGoal(
                                liveBio.hunterName,
                                liveBio.hunterTitle,
                                liveBio.weightKg,
                                liveBio.heightCm,
                                liveBio.age,
                                liveBio.biologicalSex,
                                avatarBase64.ifBlank { null },
                                liveBio.targetWeightKg,
                                liveBio.targetPhysique
                            )
                            hunterName = ""
                            weightInput = ""
                            targetWeightInput = ""
                            heightInput = ""
                            ageInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HunterCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("save_biometrics_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SAVE GOAL & BIO (+50 XP)",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 1B. Daily Productivity & Tasks Done Today Logger (Shifted to Journal Page)
        item {
            JournalDailyProductivityTasksCard(
                state = state,
                onLogProductiveTask = onLogProductiveTask,
                onEditProductiveTask = onEditProductiveTask,
                onRemoveProductiveTask = onRemoveProductiveTask
            )
        }

        // 2. Compact Live Body Composition Telemetry Matrix
        item {
            BodyCompositionTelemetryCard(bio = liveBio)
        }

        // 3. Upload & Document Progress Photo Card (Automatically adds to Personal Journal)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianCard)
                    .border(1.dp, HunterGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            tint = HunterGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PROGRESS PHOTO LOG",
                            style = MaterialTheme.typography.titleMedium,
                            color = HunterGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = HunterEmerald.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, HunterEmerald)
                    ) {
                        Text(
                            text = "+85 XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = HunterEmerald,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Upload from gallery, capture a camera snap, or generate a tactical physique card stamped with BMI (${liveBio.bmi}), Body Fat (${liveBio.bodyFatPercent}%), and Muscle Mass (${liveBio.muscleMassPercent}%).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Photo Selection Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            progressPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HunterCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_gallery_photo_button")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GALLERY PHOTO", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { cameraPreviewLauncher.launch(null) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HunterGold),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("capture_camera_photo_button")
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = HunterGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CAMERA SNAP", color = HunterGold, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        stagedPhotoBase64 = generateTacticalPhysiqueCardBase64(liveBio, selectedPoseTag)
                        if (photoTitle.isBlank()) {
                            photoTitle = "Trainer Physique Check-In (${liveBio.weightKg} kg)"
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HunterViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_tactical_card_button")
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = HunterViolet, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GENERATE TACTICAL PHYSIQUE TELEMETRY SNAPSHOT",
                        color = HunterViolet,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Preview of Staged Photo
                if (stagedPhotoBase64.isNotBlank()) {
                    val previewBitmap = remember(stagedPhotoBase64) { decodeBase64ToBitmap(stagedPhotoBase64) }
                    if (previewBitmap != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, HunterGold, RoundedCornerShape(14.dp))
                                .testTag("staged_photo_preview")
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Staged Progress Photo Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = ObsidianBg.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${liveBio.weightKg}kg • BMI ${liveBio.bmi} • BF ${liveBio.bodyFatPercent}% • Muscle ${liveBio.muscleMassPercent}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HunterCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pose / Check-in Tag Chips
                Text(
                    text = "PHYSIQUE POSE / CATEGORY TAG",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val poseOptions = listOf(
                    "Front Double Bicep",
                    "Front Relaxed",
                    "Back Lat Spread",
                    "Side Chest",
                    "Post-Pump Check-In"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    poseOptions.forEach { pose ->
                        FilterChip(
                            selected = selectedPoseTag == pose,
                            onClick = { selectedPoseTag = pose },
                            label = { Text(pose, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HunterGold.copy(alpha = 0.22f),
                                selectedLabelColor = HunterGold,
                                containerColor = ObsidianCardElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = photoTitle,
                    onValueChange = { photoTitle = it },
                    label = { Text("Progress Entry Title (e.g., Week 4 Hypertrophy Check-In)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = photoCaption,
                    onValueChange = { photoCaption = it },
                    label = { Text("Journal Notes / Pump & Conditioning Observations") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_caption_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Also set photo as Trainer Profile Avatar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Switch(
                        checked = setAsProfileAvatar,
                        onCheckedChange = { setAsProfileAvatar = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = HunterCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val finalPhoto = stagedPhotoBase64.ifBlank {
                            generateTacticalPhysiqueCardBase64(liveBio, selectedPoseTag)
                        }
                        if (setAsProfileAvatar) {
                            avatarBase64 = finalPhoto
                        }
                        onLogProgressPhoto(
                            photoTitle.ifBlank { "Physique Check-In ($selectedPoseTag)" },
                            photoCaption.ifBlank {
                                "Weight: ${liveBio.weightKg}kg • Height: ${liveBio.heightCm.toInt()}cm • BMI: ${liveBio.bmi} • Body Fat: ${liveBio.bodyFatPercent}% • Muscle Mass: ${liveBio.muscleMassPercent}% • Bone: ${liveBio.boneMassKg}kg • Water: ${liveBio.waterMassKg}kg"
                            },
                            selectedPoseTag,
                            finalPhoto,
                            setAsProfileAvatar
                        )
                        photoTitle = ""
                        photoCaption = ""
                        stagedPhotoBase64 = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HunterGold,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_progress_photo_button")
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UPLOAD PHOTO & ADD TO PERSONAL JOURNAL (+85 XP)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // 4. Personal Progress Journal Timeline Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Book,
                        contentDescription = null,
                        tint = HunterCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.personal_journal_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = HunterCyan
                    )
                }
            }
        }

        // 5. Journal Entries Feed
        when (journalState) {
            DataLoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = HunterCyan)
                    }
                }
            }
            is DataLoadState.Error -> {
                item {
                    Text(
                        text = journalState.message,
                        color = HunterCrimson,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is DataLoadState.Success -> {
                val entries = journalState.data
                if (entries.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianCard)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Book,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Personal Trainer Journal is ready.",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Update your weight/height/age above or upload a progress photo to automatically log your first chronological entry.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(entries, key = { it.id.ifBlank { it.title + it.createdAt } }) { entry ->
                        JournalEntryCard(
                            entry = entry,
                            onDelete = { onDeleteJournalEntry(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PokemonClassHierarchyAndAchievementsCard(
    playerStatus: PlayerStatus,
    unlockedAchievements: List<AchievementItem>
) {
    val currentTier = playerStatus.currentTier
    val nextTier = playerStatus.nextTier
    val totalXp = playerStatus.currentXp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ObsidianCardElevated, ObsidianCard)
                )
            )
            .border(1.5.dp, HunterGold.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("pokemon_class_hierarchy_card")
    ) {
        // Section Header
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
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = HunterGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "POKÉMON CLASS HIERARCHY & REWARDS",
                        style = MaterialTheme.typography.titleMedium,
                        color = HunterGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Auto-assigned by XP • Boosted early XP, slower elite XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = HunterCyan.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, HunterCyan.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "TIER ${currentTier.tierLevel}/7",
                    style = MaterialTheme.typography.labelMedium,
                    color = HunterCyan,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Current Active Guardian Showcase Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianBg)
                .border(1.5.dp, HunterCyan.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianCardElevated)
                    .border(1.5.dp, HunterGold, RoundedCornerShape(14.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                PikachuMascotBadge(
                    drawableRes = currentTier.pokemonDrawableRes,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = HunterGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = currentTier.classTitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = HunterGold,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "• ${currentTier.pokemonName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = HunterCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total XP: $totalXp XP (${currentTier.xpRangeLabel})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = HunterEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currentTier.xpPaceLabel} • Daily Check-In: +${currentTier.dailyCompletionXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = HunterEmerald
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Active Reward: ${currentTier.rewardTitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HunterGold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress to Next Class Bar
        val progressFraction = (playerStatus.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (nextTier != null) {
                    "NEXT CLASS: ${nextTier.classTitle.uppercase()} (${nextTier.pokemonName})"
                } else {
                    "APEX CLASS REACHED: CHAMPION (ARCEUS)"
                },
                style = MaterialTheme.typography.labelMedium,
                color = HunterCyan,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (nextTier != null) {
                    "${(nextTier.minXp - totalXp).coerceAtLeast(0)} XP TO PROMOTION"
                } else {
                    "$totalXp / 10,000+ XP"
                },
                style = MaterialTheme.typography.labelMedium,
                color = HunterGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = HunterGold,
            trackColor = ObsidianBg
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Dynamic XP Curve: Beginner (1.6x) & Challenger (1.3x) award boosted XP for daily completions to build momentum, then gradually slow down in Elite I–III & Champion (0.80x → 0.40x) to test true consistency!",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = BorderSubtle)
        Spacer(modifier = Modifier.height(12.dp))

        // All 7 Pokémon Class Tiers Roadmap
        Text(
            text = "7-TIER POKÉMON CLASS PROGRESSION ROADMAP",
            style = MaterialTheme.typography.labelLarge,
            color = HunterCyan,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrainerRankProgression.tiers.forEach { tier ->
                val isUnlocked = totalXp >= tier.minXp
                val isCurrent = tier.tierLevel == currentTier.tierLevel
                PokemonTierRoadmapRow(
                    tier = tier,
                    isUnlocked = isUnlocked,
                    isCurrent = isCurrent
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = BorderSubtle)
        Spacer(modifier = Modifier.height(12.dp))

        // Unlocked Achievements & Class Rewards Vault
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACHIEVEMENTS & CLASS REWARDS VAULT",
                style = MaterialTheme.typography.labelLarge,
                color = HunterGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${unlockedAchievements.size} UNLOCKED",
                style = MaterialTheme.typography.labelMedium,
                color = HunterEmerald,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            unlockedAchievements.forEach { ach ->
                val rarityColor = when (ach.rarity.uppercase()) {
                    "LEGENDARY" -> HunterGold
                    "EPIC" -> HunterViolet
                    "RARE" -> HunterCyan
                    else -> HunterEmerald
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianBg)
                        .border(1.dp, rarityColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = rarityColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ach.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = rarityColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ach.rarity.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = rarityColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = ach.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonTierRoadmapRow(
    tier: TrainerRankTier,
    isUnlocked: Boolean,
    isCurrent: Boolean
) {
    val borderColor = when {
        isCurrent -> HunterGold
        isUnlocked -> HunterEmerald.copy(alpha = 0.65f)
        else -> BorderSubtle
    }
    val bgColor = when {
        isCurrent -> ObsidianCardElevated
        isUnlocked -> ObsidianBg
        else -> ObsidianBg.copy(alpha = 0.55f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isCurrent) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(10.dp)
            .testTag("class_tier_row_${tier.tierLevel}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianCard)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            PikachuMascotBadge(
                drawableRes = tier.pokemonDrawableRes,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tier.classTitle} • ${tier.pokemonName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isUnlocked) TextPrimary else TextMuted,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = when {
                        isCurrent -> HunterGold.copy(alpha = 0.2f)
                        isUnlocked -> HunterEmerald.copy(alpha = 0.16f)
                        else -> ObsidianCard
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isCurrent -> Icons.Filled.Bolt
                                isUnlocked -> Icons.Filled.CheckCircle
                                else -> Icons.Filled.Lock
                            },
                            contentDescription = null,
                            tint = when {
                                isCurrent -> HunterGold
                                isUnlocked -> HunterEmerald
                                else -> TextMuted
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isCurrent -> "CURRENT"
                                isUnlocked -> "UNLOCKED"
                                else -> "LOCKED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isCurrent -> HunterGold
                                isUnlocked -> HunterEmerald
                                else -> TextMuted
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${tier.xpRangeLabel}  •  ${tier.xpPaceLabel} (Daily +${tier.dailyCompletionXp} XP)",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUnlocked) HunterCyan else TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Reward: ${tier.rewardTitle} — ${tier.rewardDescription}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isUnlocked) TextSecondary else TextMuted
            )
        }
    }
}

@Composable
private fun BodyCompositionTelemetryCard(bio: HunterBiometrics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCard)
            .border(1.dp, HunterEmerald.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("body_composition_card")
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
                    imageVector = Icons.Filled.Science,
                    contentDescription = null,
                    tint = HunterEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.body_comp_matrix_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = HunterEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = HunterEmerald.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = bio.bmiCategory.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = HunterEmerald,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Compact 3x2 Grid for all 6 Body Composition Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BiometricMetricTile(
                label = "BMI",
                primaryValue = "${bio.bmi}",
                subValue = bio.bmiCategory,
                accentColor = HunterCyan,
                modifier = Modifier.weight(1f).testTag("metric_bmi_value")
            )
            BiometricMetricTile(
                label = "BODY FAT",
                primaryValue = "${bio.bodyFatPercent}%",
                subValue = "${bio.fatMassKg} kg",
                accentColor = HunterGold,
                modifier = Modifier.weight(1f).testTag("metric_body_fat_value")
            )
            BiometricMetricTile(
                label = "MUSCLE",
                primaryValue = "${bio.muscleMassPercent}%",
                subValue = "${bio.muscleMassKg} kg",
                accentColor = HunterEmerald,
                modifier = Modifier.weight(1f).testTag("metric_muscle_mass_value")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BiometricMetricTile(
                label = "LEAN MASS",
                primaryValue = "${bio.leanMassPercent}%",
                subValue = "${bio.leanBodyMassKg} kg",
                accentColor = HunterViolet,
                modifier = Modifier.weight(1f).testTag("metric_lean_mass_value")
            )
            BiometricMetricTile(
                label = "BONE MASS",
                primaryValue = "${bio.boneMassKg} kg",
                subValue = "${bio.boneMassPercent}%",
                accentColor = HunterCyan,
                modifier = Modifier.weight(1f).testTag("metric_bone_mass_value")
            )
            BiometricMetricTile(
                label = "WATER",
                primaryValue = "${bio.waterMassKg} kg",
                subValue = "${bio.waterMassPercent}%",
                accentColor = HunterCyan,
                modifier = Modifier.weight(1f).testTag("metric_water_mass_value")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ObsidianBg)
        ) {
            val muscleWeight = (bio.muscleMassPercent / 100.0).toFloat().coerceIn(0.1f, 0.85f)
            val fatWeight = (bio.bodyFatPercent / 100.0).toFloat().coerceIn(0.05f, 0.65f)
            val boneWeight = (bio.boneMassPercent / 100.0).toFloat().coerceIn(0.03f, 0.15f)

            Box(
                modifier = Modifier
                    .weight(muscleWeight)
                    .fillMaxSize()
                    .background(HunterEmerald)
            )
            Box(
                modifier = Modifier
                    .weight(fatWeight)
                    .fillMaxSize()
                    .background(HunterGold)
            )
            Box(
                modifier = Modifier
                    .weight(boneWeight)
                    .fillMaxSize()
                    .background(HunterCyan)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("■ Muscle ${bio.muscleMassPercent}%", style = MaterialTheme.typography.labelSmall, color = HunterEmerald)
            Text("■ Fat ${bio.bodyFatPercent}%", style = MaterialTheme.typography.labelSmall, color = HunterGold)
            Text("■ Bone ${bio.boneMassPercent}%", style = MaterialTheme.typography.labelSmall, color = HunterCyan)
            Text("BMR ${bio.bmrKcal} kcal", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun BiometricMetricTile(
    label: String,
    primaryValue: String,
    subValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianCardElevated)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            maxLines = 1
        )
        Text(
            text = primaryValue,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = subValue,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun JournalEntryCard(
    entry: JournalEntry,
    onDelete: () -> Unit
) {
    val isPhotoEntry = entry.entryType == "PHOTO_PROGRESS" && entry.photoBase64.isNotBlank()
    val accentColor = if (isPhotoEntry) HunterGold else HunterCyan
    val bitmap = remember(entry.photoBase64) {
        if (entry.photoBase64.isNotBlank()) decodeBase64ToBitmap(entry.photoBase64) else null
    }
    val formattedDate = remember(entry.createdAt) {
        val date = entry.createdAt?.toDate() ?: Date()
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(date)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianCard)
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("journal_entry_card_${entry.id}")
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
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = entry.poseTag.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
            if (entry.id.isNotBlank()) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_journal_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Journal Entry",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        if (bitmap != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = entry.caption,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Embedded Biometric Snapshot Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JournalStatPill("WEIGHT", "${entry.weightKg} kg", HunterCyan)
            JournalStatPill("HEIGHT", "${entry.heightCm.toInt()} cm", TextPrimary)
            JournalStatPill("AGE", "${entry.age} yrs", TextPrimary)
            JournalStatPill("BMI", "${entry.bmi}", HunterEmerald)
            JournalStatPill("BODY FAT", "${entry.bodyFatPercent}%", HunterGold)
            JournalStatPill("MUSCLE", "${entry.muscleMassPercent}%", HunterEmerald)
        }
    }
}

@Composable
private fun JournalStatPill(label: String, value: String, color: Color) {
    Surface(
        color = ObsidianCardElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private suspend fun encodeUriToBase64(
    context: Context,
    uri: Uri,
    maxDim: Int,
    quality: Int
): String = withContext(Dispatchers.IO) {
    try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext ""
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext ""
        encodeBitmapToBase64(decoded, maxDim, quality)
    } catch (e: Exception) {
        ""
    }
}

private suspend fun encodeBitmapToBase64(
    bitmap: Bitmap,
    maxDim: Int,
    quality: Int
): String = withContext(Dispatchers.Default) {
    try {
        val ratio = minOf(
            maxDim.toFloat() / bitmap.width.coerceAtLeast(1),
            maxDim.toFloat() / bitmap.height.coerceAtLeast(1),
            1.0f
        )
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    }
}

fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    if (base64Str.isBlank()) return null
    return try {
        val bytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

private fun generateTacticalPhysiqueCardBase64(bio: HunterBiometrics, poseTag: String): String {
    return try {
        val width = 480
        val height = 300
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                android.graphics.Color.rgb(10, 16, 28),
                android.graphics.Color.rgb(18, 34, 56),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(35, 0, 229, 255)
            strokeWidth = 1.5f
        }
        for (x in 0..width step 40) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }
        for (y in 0..height step 40) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(0, 229, 255)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(8f, 8f, (width - 8).toFloat(), (height - 8).toFloat(), borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(0, 229, 255)
            textSize = 22f
            isFakeBoldText = true
        }
        canvas.drawText("TRAINER BIOMETRIC SCAN // ${poseTag.uppercase()}", 28f, 48f, titlePaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isFakeBoldText = true
        }
        canvas.drawText("${bio.hunterName.uppercase()} (${bio.weightKg} KG)", 28f, 90f, namePaint)

        val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(0, 230, 118)
            textSize = 20f
            isFakeBoldText = true
        }
        canvas.drawText("BMI: ${bio.bmi} (${bio.bmiCategory})", 28f, 138f, metricPaint)
        canvas.drawText("BODY FAT: ${bio.bodyFatPercent}% (${bio.fatMassKg} kg)", 28f, 172f, metricPaint)
        canvas.drawText("SKELETAL MUSCLE: ${bio.muscleMassPercent}% (${bio.muscleMassKg} kg)", 28f, 206f, metricPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(255, 213, 79)
            textSize = 18f
        }
        canvas.drawText(
            "BONE MASS: ${bio.boneMassKg} kg (${bio.boneMassPercent}%)  •  WATER MASS: ${bio.waterMassKg} kg (${bio.waterMassPercent}%)",
            28f,
            246f,
            subPaint
        )

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    }
}

@Composable
internal fun JournalDailyProductivityTasksCard(
    state: HunterSystemState,
    onLogProductiveTask: (title: String, category: String, dateStr: String) -> Unit,
    onEditProductiveTask: (dateStr: String, taskId: String, newTitle: String, newCategory: String) -> Unit,
    onRemoveProductiveTask: (dateStr: String, taskId: String) -> Unit
) {
    val todayDateStr = remember { StreakDateUtils.todayDateString() }
    var taskTitleInput by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("WORK") }
    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTaskTitle by rememberSaveable { mutableStateOf("") }
    var editingTaskCategory by rememberSaveable { mutableStateOf("WORK") }

    val activeDayLog = state.playerStatus.dailyLogsByDate[todayDateStr]
    val tasksForDay: List<ProductiveTaskItem> = activeDayLog?.tasksCompleted ?: emptyList()

    val categories = listOf(
        "WORK" to "💼 Work",
        "STUDY" to "📚 Study",
        "HABIT" to "⚡ Productive",
        "PERSONAL" to "🏡 Personal"
    )
    val quickPresets = listOf(
        "Been productive & focused today" to "HABIT",
        "Completed daily work targets" to "WORK",
        "Finished study / coding session" to "STUDY",
        "Meal prep & clean room done" to "PERSONAL"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianCard)
            .border(1.2.dp, HunterEmerald.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(14.dp)
            .testTag("daily_productivity_tasks_card")
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HunterEmerald.copy(alpha = 0.16f))
                        .border(1.dp, HunterEmerald, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.TaskAlt,
                        contentDescription = null,
                        tint = HunterEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DAILY PRODUCTIVITY & TASKS LOG",
                        style = MaterialTheme.typography.titleSmall,
                        color = HunterEmerald,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Log productive work, study & habits done today (+35 XP)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = HunterEmerald.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, HunterEmerald)
            ) {
                Text(
                    text = "${tasksForDay.size} Done",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = HunterEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (catKey, catLabel) ->
                val selected = selectedCategory == catKey
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { selectedCategory = catKey },
                    shape = RoundedCornerShape(50),
                    color = if (selected) HunterGold.copy(alpha = 0.22f) else ObsidianBg,
                    border = BorderStroke(1.dp, if (selected) HunterGold else BorderSubtle)
                ) {
                    Text(
                        text = catLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) HunterGold else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input field + Log button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = taskTitleInput,
                onValueChange = { taskTitleInput = it },
                placeholder = {
                    Text(
                        text = "What tasks did you complete today? (e.g. Been productive, finished work)",
                        fontSize = 11.5.sp
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("productive_task_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HunterEmerald,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Button(
                onClick = {
                    val trimmed = taskTitleInput.trim()
                    if (trimmed.isNotEmpty()) {
                        onLogProductiveTask(trimmed, selectedCategory, todayDateStr)
                        taskTitleInput = ""
                    }
                },
                enabled = taskTitleInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HunterEmerald,
                    contentColor = ObsidianBg
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                modifier = Modifier.testTag("productive_task_add_button")
            ) {
                Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Log",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 1-Tap Quick Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPresets.forEach { (presetText, presetCat) ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            onLogProductiveTask(presetText, presetCat, todayDateStr)
                        },
                    shape = RoundedCornerShape(50),
                    color = HunterEmerald.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, HunterEmerald.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "+ $presetText",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HunterEmerald
                    )
                }
            }
        }

        if (tasksForDay.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tasksForDay.forEach { task ->
                    val isEditingThis = editingTaskId == task.id
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianBg,
                        border = BorderStroke(1.dp, if (isEditingThis) HunterGold else HunterEmerald.copy(alpha = 0.45f))
                    ) {
                        if (isEditingThis) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                OutlinedTextField(
                                    value = editingTaskTitle,
                                    onValueChange = { editingTaskTitle = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Edit Task") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HunterGold,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { editingTaskId = null }) {
                                        Text("Cancel", color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            if (editingTaskTitle.isNotBlank()) {
                                                onEditProductiveTask(
                                                    todayDateStr,
                                                    task.id,
                                                    editingTaskTitle.trim(),
                                                    editingTaskCategory
                                                )
                                                editingTaskId = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HunterEmerald,
                                            contentColor = ObsidianBg
                                        )
                                    ) {
                                        Text("Save", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = HunterEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${task.category} • +${task.xpAwarded} XP${if (task.completedAt.isNotBlank()) " • ${task.completedAt}" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HunterEmerald
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingTaskId = task.id
                                            editingTaskTitle = task.title
                                            editingTaskCategory = task.category
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit Task",
                                            tint = HunterGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onRemoveProductiveTask(todayDateStr, task.id)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove Task",
                                            tint = HunterCrimson,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

