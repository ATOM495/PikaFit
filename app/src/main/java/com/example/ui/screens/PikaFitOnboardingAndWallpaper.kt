package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.HunterBiometrics
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * Full-screen Pikachu wallpaper wrapper used across all pages of PikaFit.
 * Renders the Pikachu wallpaper clearly and vibrantly in both Dark and Light mode
 * while keeping foreground cards glassmorphic and high-contrast.
 */
@Composable
fun PikachuWallpaperBackground(
    @DrawableRes wallpaperRes: Int = R.drawable.pika_logo_hi,
    isDark: Boolean = true,
    wallpaperAlpha: Float = if (isDark) 0.50f else 0.55f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgBase = if (isDark) Color(0xFF12141A) else Color(0xFFF8FAFC)
    val wallpaperPainter = painterResource(id = wallpaperRes)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
    ) {
        // 1. Full-bleed Pikachu background wallpaper — clearly visible across the entire screen
        Image(
            painter = wallpaperPainter,
            contentDescription = "Pikachu Background Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(wallpaperAlpha)
        )

        // 2. Crisp floating Pikachu character medallion in top-right & bottom-right so Pikachu is always visible
        Image(
            painter = wallpaperPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 18.dp, y = 18.dp)
                .size(220.dp)
                .clip(CircleShape)
                .alpha(if (isDark) 0.42f else 0.45f)
        )

        // 3. Light, balanced glass scrim so text is 100% readable while keeping the Pikachu wallpaper vivid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF12141A).copy(alpha = 0.35f),
                                Color(0xFF12141A).copy(alpha = 0.48f),
                                Color(0xFF12141A).copy(alpha = 0.58f)
                            )
                        } else {
                            listOf(
                                Color(0xFFFFFFFF).copy(alpha = 0.32f),
                                Color(0xFFF8FAFC).copy(alpha = 0.45f),
                                Color(0xFFF8FAFC).copy(alpha = 0.55f)
                            )
                        }
                    )
                )
        )

        content()
    }
}

/**
 * Step-by-step interactive first-launch Trainer Onboarding screen.
 * Asks the user one by one:
 * 0. Welcome Hero ("Hey there, Are you ready? Let's start ->" matching reference image 1)
 * 1. Trainer Name ("Pika, welcome trainer! What is your name?")
 * 2. Age & Sex ("How old are you?")
 * 3. Height & Weight ("What is your height and weight?")
 * 4. Fitness Goal ("Weight gain or weight loss?")
 * 5. Experience Level ("Are you a professional or beginner?")
 */
@Composable
fun PikaFitOnboardingScreen(
    initialBiometrics: HunterBiometrics = HunterBiometrics(),
    isDark: Boolean = true,
    onCompleteOnboarding: (
        trainerName: String,
        age: Int,
        biologicalSex: String,
        heightCm: Double,
        weightKg: Double,
        fitnessGoal: String,
        experienceLevel: String
    ) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 5

    var trainerName by rememberSaveable {
        mutableStateOf(
            if (initialBiometrics.hunterName == "New Trainer" || initialBiometrics.hunterName == "Shadow Vanguard") ""
            else initialBiometrics.hunterName
        )
    }
    var ageText by rememberSaveable { mutableStateOf(initialBiometrics.age.toString()) }
    var biologicalSex by rememberSaveable { mutableStateOf(initialBiometrics.biologicalSex) }
    var heightText by rememberSaveable { mutableStateOf(initialBiometrics.heightCm.roundToInt().toString()) }
    var weightText by rememberSaveable { mutableStateOf(initialBiometrics.weightKg.toString()) }
    var fitnessGoal by rememberSaveable { mutableStateOf(initialBiometrics.fitnessGoal) }
    var experienceLevel by rememberSaveable { mutableStateOf(initialBiometrics.experienceLevel) }

    val parsedAge = ageText.toIntOrNull()?.coerceIn(10, 100) ?: 22
    val parsedHeight = heightText.toDoubleOrNull()?.coerceIn(100.0, 240.0) ?: 172.0
    val parsedWeight = weightText.toDoubleOrNull()?.coerceIn(30.0, 250.0) ?: 70.0

    val previewBio = remember(trainerName, parsedAge, biologicalSex, parsedHeight, parsedWeight, fitnessGoal, experienceLevel) {
        HunterBiometrics(
            hunterName = trainerName.ifBlank { "Trainer" },
            weightKg = parsedWeight,
            heightCm = parsedHeight,
            age = parsedAge,
            biologicalSex = biologicalSex,
            fitnessGoal = fitnessGoal,
            experienceLevel = experienceLevel,
            onboardingCompleted = true
        )
    }

    // Dynamic high-contrast colors for Dark & Light mode
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val accentGold = if (isDark) PikaYellow else Color(0xFFB45309)
    val cardSurface = if (isDark) Color(0xFF1E2330).copy(alpha = 0.84f) else Color(0xFFFFFFFF).copy(alpha = 0.88f)
    val borderCol = if (isDark) Color(0xFF3B4358) else Color(0xFFCBD5E1)

    val stepWallpaper = when (step) {
        0 -> R.drawable.pika_logo_hi
        1 -> R.drawable.pika_logo_hi
        2 -> R.drawable.pika_orange_wave
        3 -> R.drawable.pika_pokeball_bg
        4 -> R.drawable.pika_orange_wave
        else -> R.drawable.pika_logo_hi
    }

    if (step == 0) {
        // Step 0: Bold Crimson Red Welcome Screen matching the left phone in the reference image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEF233C),
                            Color(0xFFE51B32),
                            Color(0xFFD90429)
                        )
                    )
                )
        ) {
            // Visible Pikachu wallpaper in the background
            Image(
                painter = painterResource(id = R.drawable.pika_orange_wave),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.32f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top PikaFit Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.5.dp, PikaYellow)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = PikaYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PIKAFIT • TRAINER INTRODUCTION",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                    }
                }

                // Center Waving Pikachu Logo + Speech Bubble
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6792A4))
                            .border(4.dp, PikaYellow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.pika_logo_hi),
                            contentDescription = "Pikachu saying hi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 10.dp
                    ) {
                        Text(
                            text = "\"Pika-Pika! Welcome, Trainer! ⚡\nTell me your name, age, height, weight & goals one by one!\"",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                lineHeight = 23.sp
                            ),
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                    }
                }

                // Bottom Hero Copy & "Let's start ->" CTA matching the left screen in the reference image
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Hey there,\nAre you ready?",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp,
                            lineHeight = 44.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Keep your vitality. A life without health is like a river without water — let's calibrate your PikaFit profile!",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                        color = Color(0xFF1E1E24).copy(alpha = 0.88f),
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 23.sp
                    )
                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("onboarding_lets_start_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PikaYellow,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Text(
                            text = "Let's start   →",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 21.sp
                            )
                        )
                    }
                }
            }
        }
        return
    }

    // Steps 1..5: One-by-one Trainer Questionnaire with vivid Pikachu Wallpapers
    PikachuWallpaperBackground(
        wallpaperRes = stepWallpaper,
        isDark = isDark,
        wallpaperAlpha = if (isDark) 0.50f else 0.55f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Progress Bar & Step Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (step > 0) step-- },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(cardSurface)
                        .border(1.dp, borderCol, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Question",
                        tint = textPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "STEP $step OF $totalSteps",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        ),
                        color = accentGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..totalSteps) {
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (i == step) 32.dp else 12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (i <= step) PikaCheekRed else borderCol
                                    )
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = PikaCheekRed,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "PikaFit",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pikachu Mascot Dialogue Header Card
            val (pikaImageRes, pikaSpeech) = when (step) {
                1 -> R.drawable.pika_logo_hi to "Pika, welcome Trainer! Let's get acquainted — what is your name?"
                2 -> R.drawable.pika_orange_wave to "Awesome to meet you, ${trainerName.ifBlank { "Trainer" }}! ⚡ What is your age?"
                3 -> R.drawable.pika_pokeball_bg to "Pika! Now tell me your height (cm) and current weight (kg)!"
                4 -> R.drawable.pika_orange_wave to "What is your main goal, ${trainerName.ifBlank { "Trainer" }} — Weight Gain or Weight Loss?"
                else -> R.drawable.pika_logo_hi to "Final question! Are you a Professional athlete or a Beginner?"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                border = BorderStroke(2.dp, PikaCheekRed)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = pikaImageRes),
                        contentDescription = "Pikachu Coach",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, PikaYellow, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PIKACHU ASKS:",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = PikaCheekRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"$pikaSpeech\"",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 24.sp
                            ),
                            color = textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated Question Card (One by One Input)
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step_content"
            ) { currentStep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = BorderStroke(1.5.dp, borderCol)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        when (currentStep) {
                            1 -> {
                                Text(
                                    text = "1. Your Name?",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Pikachu will greet you by name on your dashboard every morning.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(18.dp))

                                OutlinedTextField(
                                    value = trainerName,
                                    onValueChange = { trainerName = it },
                                    label = { Text("Trainer Name", fontSize = 16.sp) },
                                    placeholder = { Text("e.g., Arul, Ash, John...", fontSize = 16.sp) },
                                    textStyle = MaterialTheme.typography.titleLarge.copy(color = textPrimary),
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = PikaCheekRed)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_name_input")
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tap a quick name:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Arul", "Ash", "Trainer").forEach { sample ->
                                        AssistChip(
                                            onClick = { trainerName = sample },
                                            label = {
                                                Text(
                                                    text = sample,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = textPrimary
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            2 -> {
                                Text(
                                    text = "2. Your Age?",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Used to calibrate your basal metabolic rate (BMR) and daily energy target.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Age",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "$parsedAge years",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = PikaCheekRed
                                    )
                                }

                                Slider(
                                    value = parsedAge.toFloat(),
                                    onValueChange = { ageText = it.roundToInt().toString() },
                                    valueRange = 12f..80f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PikaCheekRed,
                                        activeTrackColor = PikaCheekRed
                                    )
                                )

                                OutlinedTextField(
                                    value = ageText,
                                    onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Age (years)", fontSize = 16.sp) },
                                    textStyle = MaterialTheme.typography.titleLarge.copy(color = textPrimary),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "Biological Sex",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    listOf("MALE" to "Male ♂", "FEMALE" to "Female ♀").forEach { (key, label) ->
                                        val selected = biologicalSex == key
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { biologicalSex = key },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (selected) PikaCheekRed else Color.Transparent,
                                            border = BorderStroke(
                                                width = 2.dp,
                                                color = if (selected) PikaCheekRed else borderCol
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 14.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = if (selected) Color.White else textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                Text(
                                    text = "3. Height & Weight?",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Set your starting height and weight so Pikachu can track your body composition.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Height", style = MaterialTheme.typography.titleLarge, color = textPrimary)
                                    Text(
                                        "${parsedHeight.roundToInt()} cm",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = accentGold
                                    )
                                }
                                Slider(
                                    value = parsedHeight.toFloat(),
                                    onValueChange = { heightText = it.roundToInt().toString() },
                                    valueRange = 130f..220f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PikaYellowDark,
                                        activeTrackColor = PikaYellowDark
                                    )
                                )
                                OutlinedTextField(
                                    value = heightText,
                                    onValueChange = { heightText = it },
                                    label = { Text("Height (cm)", fontSize = 16.sp) },
                                    textStyle = MaterialTheme.typography.titleLarge.copy(color = textPrimary),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Weight", style = MaterialTheme.typography.titleLarge, color = textPrimary)
                                    Text(
                                        "$parsedWeight kg",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = PikaCheekRed
                                    )
                                }
                                Slider(
                                    value = parsedWeight.toFloat(),
                                    onValueChange = {
                                        val rounded = (it * 2).roundToInt() / 2.0
                                        weightText = rounded.toString()
                                    },
                                    valueRange = 35f..160f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PikaCheekRed,
                                        activeTrackColor = PikaCheekRed
                                    )
                                )
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = { weightText = it },
                                    label = { Text("Weight (kg)", fontSize = 16.sp) },
                                    textStyle = MaterialTheme.typography.titleLarge.copy(color = textPrimary),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    color = if (isDark) Color(0xFF12141A) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, borderCol)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("BMI", style = MaterialTheme.typography.labelLarge, color = textSecondary)
                                            Text(
                                                "${previewBio.bmi}",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = accentGold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Body Fat", style = MaterialTheme.typography.labelLarge, color = textSecondary)
                                            Text(
                                                "${previewBio.bodyFatPercent}%",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = PikaCheekRed
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Muscle", style = MaterialTheme.typography.labelLarge, color = textSecondary)
                                            Text(
                                                "${previewBio.muscleMassPercent}%",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = HunterEmerald
                                            )
                                        }
                                    }
                                }
                            }

                            4 -> {
                                Text(
                                    text = "4. Weight Gain or Loss?",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Select your target direction so Pikachu can set your daily calorie & macro goals.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val goals = listOf(
                                    Triple(
                                        "WEIGHT_GAIN",
                                        "⚡ Weight Gain (Muscle Bulk)",
                                        "Caloric surplus (+400 kcal) & progressive overload for lean muscle growth."
                                    ),
                                    Triple(
                                        "WEIGHT_LOSS",
                                        "🔥 Weight Loss (Fat Cut)",
                                        "Caloric deficit (-400 kcal) & high protein to shed body fat cleanly."
                                    ),
                                    Triple(
                                        "MAINTENANCE",
                                        "⚖️ Recomp & Maintain",
                                        "Balanced maintenance calories for steady strength & body recomposition."
                                    )
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    goals.forEach { (key, title, desc) ->
                                        val selected = fitnessGoal == key
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .clickable { fitnessGoal = key },
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (selected) PikaCheekRed.copy(alpha = 0.15f) else Color.Transparent,
                                            border = BorderStroke(
                                                width = if (selected) 2.5.dp else 1.5.dp,
                                                color = if (selected) PikaCheekRed else borderCol
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = textPrimary
                                                    )
                                                    if (selected) {
                                                        Icon(
                                                            imageVector = Icons.Filled.CheckCircle,
                                                            contentDescription = null,
                                                            tint = PikaCheekRed
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                Text(
                                    text = "5. Professional or Beginner?",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Choose your experience level to unlock your starting Trainer Rank.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val levels = listOf(
                                    Triple(
                                        "BEGINNER",
                                        "🌱 Beginner Trainer",
                                        "Starting fresh or building daily consistency • Rank: PikaFit Rookie"
                                    ),
                                    Triple(
                                        "INTERMEDIATE",
                                        "⚡ Intermediate Challenger",
                                        "Consistent lifter pushing weekly overload • Rank: Thunder Challenger"
                                    ),
                                    Triple(
                                        "PROFESSIONAL",
                                        "👑 Professional Athlete",
                                        "High-intensity volume & strict macro precision • Rank: Pro Gym Leader"
                                    )
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    levels.forEach { (key, title, desc) ->
                                        val selected = experienceLevel == key
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .clickable { experienceLevel = key },
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (selected) PikaCheekRed.copy(alpha = 0.15f) else Color.Transparent,
                                            border = BorderStroke(
                                                width = if (selected) 2.5.dp else 1.5.dp,
                                                color = if (selected) PikaCheekRed else borderCol
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = textPrimary
                                                    )
                                                    if (selected) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Bolt,
                                                            contentDescription = null,
                                                            tint = PikaCheekRed
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = textSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Summary Card of Calibrated Targets
                                Surface(
                                    color = if (isDark) Color(0xFF12141A) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(2.dp, PikaCheekRed)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "⚡ YOUR CALIBRATED PIKAFIT PLAN",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                            color = PikaCheekRed
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${previewBio.hunterName} • ${previewBio.age}y • ${previewBio.heightCm.roundToInt()}cm • ${previewBio.weightKg}kg",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "${previewBio.goalDisplayLabel} • ${previewBio.experienceDisplayLabel}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${previewBio.recommendedCaloriesTarget} kcal",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = PikaCheekRed
                                            )
                                            Text(
                                                text = "${previewBio.recommendedProteinG}g Protein",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = HunterEmerald
                                            )
                                            Text(
                                                text = "${previewBio.recommendedWaterLiters}L Water",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = accentGold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (currentStep < totalSteps) {
                                    step = currentStep + 1
                                } else {
                                    onCompleteOnboarding(
                                        trainerName.ifBlank { "Trainer" },
                                        parsedAge,
                                        biologicalSex,
                                        parsedHeight,
                                        parsedWeight,
                                        fitnessGoal,
                                        experienceLevel
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .testTag("onboarding_next_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PikaCheekRed,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (currentStep == totalSteps) {
                                    "LAUNCH PIKAFIT DASHBOARD ⚡"
                                } else {
                                    "NEXT QUESTION (${currentStep}/$totalSteps) →"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }
        }
    }
}
