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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.HunterBiometrics
import com.example.data.model.HunterSystemState
import com.example.data.model.JournalEntry
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
    onLogProgressPhoto: (
        title: String,
        caption: String,
        poseTag: String,
        photoBase64: String,
        setAsAvatar: Boolean
    ) -> Unit,
    onDeleteJournalEntry: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedBio = state.biometrics

    // Editable Profile & Biometrics state
    var hunterName by rememberSaveable(savedBio.hunterName) { mutableStateOf(savedBio.hunterName) }
    var hunterTitle by rememberSaveable(savedBio.hunterTitle) { mutableStateOf(savedBio.hunterTitle) }
    var weightInput by rememberSaveable(savedBio.weightKg) { mutableStateOf(savedBio.weightKg.toString()) }
    var heightInput by rememberSaveable(savedBio.heightCm) { mutableStateOf(savedBio.heightCm.toString()) }
    var ageInput by rememberSaveable(savedBio.age) { mutableStateOf(savedBio.age.toString()) }
    var sexInput by rememberSaveable(savedBio.biologicalSex) { mutableStateOf(savedBio.biologicalSex) }
    var avatarBase64 by rememberSaveable(savedBio.avatarBase64) { mutableStateOf(savedBio.avatarBase64) }

    // Live preview calculation based on current text fields
    val liveBio = remember(hunterName, hunterTitle, weightInput, heightInput, ageInput, sexInput, avatarBase64) {
        HunterBiometrics(
            hunterName = hunterName.ifBlank { savedBio.hunterName },
            hunterTitle = hunterTitle.ifBlank { savedBio.hunterTitle },
            avatarBase64 = avatarBase64,
            weightKg = weightInput.toDoubleOrNull()?.coerceIn(25.0, 350.0) ?: savedBio.weightKg,
            heightCm = heightInput.toDoubleOrNull()?.coerceIn(100.0, 250.0) ?: savedBio.heightCm,
            age = ageInput.toIntOrNull()?.coerceIn(10, 120) ?: savedBio.age,
            biologicalSex = sexInput
        )
    }

    // Progress Photo Upload State
    var photoTitle by rememberSaveable { mutableStateOf("") }
    var photoCaption by rememberSaveable { mutableStateOf("") }
    var selectedPoseTag by rememberSaveable { mutableStateOf("Front Double Bicep") }
    var stagedPhotoBase64 by rememberSaveable { mutableStateOf("") }
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
                    onSaveBiometrics(
                        liveBio.hunterName,
                        liveBio.hunterTitle,
                        liveBio.weightKg,
                        liveBio.heightCm,
                        liveBio.age,
                        liveBio.biologicalSex,
                        encoded
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
        // 1. Hunter Identity & Editable Biometrics Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ObsidianCardElevated, ObsidianCard)
                        )
                    )
                    .border(1.5.dp, HunterCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = HunterCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.profile_biometrics_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = HunterCyan
                        )
                    }
                    Surface(
                        color = HunterGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, HunterGold.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "AUTO-JOURNALS",
                            style = MaterialTheme.typography.labelMedium,
                            color = HunterGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar + Codename Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
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
                                contentDescription = "Hunter Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Upload Avatar",
                                    tint = HunterCyan,
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    text = "EDIT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HunterCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = hunterName,
                            onValueChange = { hunterName = it },
                            label = { Text("Hunter Codename") },
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
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = hunterTitle,
                            onValueChange = { hunterTitle = it },
                            label = { Text("Class / Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_hunter_title"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HunterCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Weight, Height, Age Inputs
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
                            .testTag("input_weight_kg"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Height (cm)") },
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
                        label = { Text("Age (yrs)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.85f)
                            .testTag("input_age_years"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Biological Sex Selector for accurate Body Fat / Bone / Muscle formulas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "METABOLIC FORMULA REFERENCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MALE", "FEMALE").forEach { option ->
                            FilterChip(
                                selected = sexInput.uppercase() == option,
                                onClick = { sexInput = option },
                                label = { Text(option, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HunterCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = HunterCyan,
                                    containerColor = ObsidianBg,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("sex_chip_$option")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onSaveBiometrics(
                            liveBio.hunterName,
                            liveBio.hunterTitle,
                            liveBio.weightKg,
                            liveBio.heightCm,
                            liveBio.age,
                            liveBio.biologicalSex,
                            avatarBase64.ifBlank { null }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HunterCyan,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_biometrics_button")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE BIOMETRICS & LOG TO PERSONAL JOURNAL (+50 XP)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // 2. Live Body Composition Telemetry Matrix (BMI, Bone Mass, Water Mass, Fat %, Muscle Mass %, Lean Mass %, BMR)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            tint = HunterGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.photo_upload_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = HunterGold
                        )
                    }
                    Text(
                        text = "+85 XP",
                        style = MaterialTheme.typography.labelLarge,
                        color = HunterEmerald
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select a photo from your gallery, capture with camera, or generate a tactical physique snapshot. Every upload is automatically stamped with your current BMI (${liveBio.bmi}), Body Fat (${liveBio.bodyFatPercent}%), Muscle Mass (${liveBio.muscleMassPercent}%), Bone Mass (${liveBio.boneMassKg}kg), and Water Mass (${liveBio.waterMassKg}kg) and saved to your Personal Journal.",
                    style = MaterialTheme.typography.bodyMedium,
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
                            photoTitle = "Hunter Physique Check-In (${liveBio.weightKg} kg)"
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
                        text = "Also set photo as Hunter Profile Avatar",
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
                                text = "Your Personal Hunter Journal is ready.",
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
private fun BodyCompositionTelemetryCard(bio: HunterBiometrics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianCard)
            .border(1.dp, HunterEmerald.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("body_composition_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Science,
                    contentDescription = null,
                    tint = HunterEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.body_comp_matrix_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = HunterEmerald
                )
            }
            Surface(
                color = HunterEmerald.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = bio.bmiCategory.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = HunterEmerald,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Row 1: BMI & Body Fat %
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BiometricMetricTile(
                label = "BMI INDEX",
                primaryValue = "${bio.bmi}",
                subValue = bio.bmiCategory,
                accentColor = HunterCyan,
                modifier = Modifier.weight(1f).testTag("metric_bmi_value")
            )
            BiometricMetricTile(
                label = "BODY FAT %",
                primaryValue = "${bio.bodyFatPercent}%",
                subValue = "${bio.fatMassKg} kg Fat Mass",
                accentColor = HunterGold,
                modifier = Modifier.weight(1f).testTag("metric_body_fat_value")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Skeletal Muscle Mass % & Lean Body Mass %
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BiometricMetricTile(
                label = "MUSCLE MASS",
                primaryValue = "${bio.muscleMassPercent}%",
                subValue = "${bio.muscleMassKg} kg Skeletal Muscle",
                accentColor = HunterEmerald,
                modifier = Modifier.weight(1f).testTag("metric_muscle_mass_value")
            )
            BiometricMetricTile(
                label = "LEAN MASS %",
                primaryValue = "${bio.leanMassPercent}%",
                subValue = "${bio.leanBodyMassKg} kg Fat-Free",
                accentColor = HunterViolet,
                modifier = Modifier.weight(1f).testTag("metric_lean_mass_value")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3: Bone Mass & Total Body Water Mass
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BiometricMetricTile(
                label = "BONE MASS",
                primaryValue = "${bio.boneMassKg} kg",
                subValue = "${bio.boneMassPercent}% of Total Mass",
                accentColor = HunterCyan,
                modifier = Modifier.weight(1f).testTag("metric_bone_mass_value")
            )
            BiometricMetricTile(
                label = "WATER MASS",
                primaryValue = "${bio.waterMassKg} kg",
                subValue = "${bio.waterMassPercent}% Hydration Mass",
                accentColor = HunterCyan,
                modifier = Modifier.weight(1f).testTag("metric_water_mass_value")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = BorderSubtle)
        Spacer(modifier = Modifier.height(12.dp))

        // Tissue Partition Bar
        Text(
            text = "TOTAL MASS PARTITION (${bio.weightKg} KG • BMR ${bio.bmrKcal} KCAL/DAY)",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
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

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("■ Muscle ${bio.muscleMassPercent}%", style = MaterialTheme.typography.labelMedium, color = HunterEmerald)
            Text("■ Fat ${bio.bodyFatPercent}%", style = MaterialTheme.typography.labelMedium, color = HunterGold)
            Text("■ Bone ${bio.boneMassPercent}%", style = MaterialTheme.typography.labelMedium, color = HunterCyan)
            Text("■ H2O ${bio.waterMassPercent}%", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
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
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCardElevated)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = primaryValue,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subValue,
            style = MaterialTheme.typography.labelMedium,
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
        canvas.drawText("HUNTER BIOMETRIC SCAN // ${poseTag.uppercase()}", 28f, 48f, titlePaint)

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
