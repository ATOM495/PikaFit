package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.*
import com.example.data.repository.HunterRepository
import com.example.ui.auth.signOutHunter
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiSyncState
import com.example.ui.viewmodel.DataLoadState
import com.example.ui.viewmodel.FoodEntryRecord
import com.example.ui.viewmodel.HunterViewModel
import com.example.ui.viewmodel.PikaCoachTip
import com.example.ui.viewmodel.PikaCoachUiState
import com.example.ui.viewmodel.WaterEntryRecord
import com.example.util.PikaFitNotificationScheduler
import com.example.util.ReminderCategory
import com.example.util.ReminderSettingsState
import com.example.util.SmartFoodNutritionEngine
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.round
import kotlin.math.roundToInt

private val PikaVoltYellow = Color(0xFFFFDE00)
private val PikaVoltAmber = Color(0xFFD97706)
private val PikaCheekCrimson = Color(0xFFEF233C)
private val PikaCrimsonDeep = Color(0xFFD90429)
private val PikaSlateBlue = Color(0xFF1E6076)
private val PikaDeepIndigo = Color(0xFF4F46E5)
private val HunterDoneGreen = Color(0xFF10B981)

data class TutorialStep(
    val title: String,
    val subtitle: String,
    val badge: String,
    @DrawableRes val drawableRes: Int,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun PikachuMascotBadge(
    @DrawableRes drawableRes: Int = R.drawable.pika_logo_hi,
    modifier: Modifier = Modifier
) {
    val bmp = rememberAsyncDrawableBitmap(drawableRes)
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = "Pikachu Mascot",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xFF6792A4))
        )
    }
}

@Composable
fun PikachuTutorialDialog(
    trainerName: String = "Trainer",
    onReopenOnboarding: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val isDark = AppThemeIsDark
    val steps = listOf(
        TutorialStep(
            title = "Pika-Pika! Hi, $trainerName!",
            subtitle = "Welcome to PikaFit! Pikachu is your personal strength, nutrition, and progressive overload companion.",
            badge = "STEP 1 • PIKAFIT",
            drawableRes = R.drawable.pika_logo_hi,
            icon = Icons.Filled.Bolt,
            accentColor = if (isDark) PikaVoltYellow else PikaCheekCrimson
        ),
        TutorialStep(
            title = "Daily Productive Overload",
            subtitle = "Opening the app alone won't keep your streak. Log your workout sets or caloric meals to verify your day with 100,000 Volts of discipline!",
            badge = "STEP 2 • WORKOUT & DIET",
            drawableRes = R.drawable.pika_orange_wave,
            icon = Icons.Filled.FitnessCenter,
            accentColor = PikaCheekCrimson
        ),
        TutorialStep(
            title = "Telemetry & Biometric Journal",
            subtitle = "Track lean mass, BMI, body fat percentage, and progress photos over time across Pikachu-themed charts and archives.",
            badge = "STEP 3 • CHARTS & LOG",
            drawableRes = R.drawable.pika_pokeball_bg,
            icon = Icons.Filled.MenuBook,
            accentColor = PikaVoltAmber
        )
    )

    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps[currentStep]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable(onClick = onDismiss)
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(2.5.dp, step.accentColor, RoundedCornerShape(28.dp))
                .clickable(enabled = false) {},
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f, fill = false),
                        color = step.accentColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.5.dp, step.accentColor)
                    ) {
                        Text(
                            text = step.badge,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            ),
                            color = step.accentColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.wrapContentWidth(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "SKIP",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .clip(CircleShape)
                        .background(step.accentColor.copy(alpha = 0.15f))
                        .border(3.dp, step.accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    PikachuMascotBadge(
                        drawableRes = step.drawableRes,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .height(9.dp)
                                .width(if (index == currentStep) 30.dp else 10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (index == currentStep) step.accentColor else BorderSubtle
                                )
                                .clickable { currentStep = index }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .widthIn(min = 82.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "BACK",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < steps.size - 1) {
                                currentStep++
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = step.accentColor,
                            contentColor = if (step.accentColor == PikaVoltYellow) Color(0xFF0F172A) else Color.White
                        )
                    ) {
                        Text(
                            text = if (currentStep == steps.size - 1) "START TRAINING" else "NEXT (${currentStep + 1}/${steps.size})",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = {
                    onDismiss()
                    onReopenOnboarding()
                }) {
                    Text(
                        text = "Redo Trainer Setup Questions →",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                    )
                }
            }
        }
    }
}

@Composable
fun AppUpdateDialog(
    releaseInfo: AppReleaseInfo,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val surfaceBg = if (isDark) Color(0xFF141B2D) else Color.White
    val borderColor = PikaVoltYellow

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = onDismiss)
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clickable(enabled = false) {}
                .testTag("app_update_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = surfaceBg,
            border = BorderStroke(2.dp, borderColor),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PikaVoltYellow.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, PikaVoltYellow)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = PikaVoltYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "IN-APP OTA PATCH READY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }

                    if (!releaseInfo.isMandatory) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close update prompt",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PikaVoltYellow.copy(alpha = 0.16f))
                        .border(3.dp, PikaVoltYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    PikachuMascotBadge(
                        drawableRes = R.drawable.pika_logo_hi,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${releaseInfo.releaseTitle} (v${releaseInfo.patchVersion.ifBlank { releaseInfo.latestVersionName }})",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = releaseInfo.releaseNotes.ifBlank {
                        "A new live OTA patch is ready for PikaFit! Tap Apply Patch Now to install updates in-app with zero APK reinstall."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = (if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = HunterDoneGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Automatic In-App GitHub Patch • No APK Reinstall Required",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!releaseInfo.isMandatory) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text("Later", color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onDownload,
                        modifier = Modifier
                            .weight(1.35f)
                            .height(50.dp)
                            .testTag("download_update_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PikaVoltYellow,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apply Patch Now",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PikaCoachPopupDialog(
    tip: PikaCoachTip,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit,
    onRefreshTip: () -> Unit
) {
    val surfaceBg = if (isDark) Color(0xFF141B2D) else Color.White
    val borderColor = when (tip.coachMood) {
        "ALERT_INACTIVE" -> PikaCheekCrimson
        "PROUD" -> HunterDoneGreen
        else -> PikaVoltYellow
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable(onClick = onDismiss)
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clickable(enabled = false) {}
                .testTag("pika_coach_popup_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = surfaceBg,
            border = BorderStroke(2.5.dp, borderColor),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = borderColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = borderColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GEMINI PIKA-COACH TIP",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Pika-Coach Tip",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6792A4))
                        .border(3.dp, PikaVoltYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    PikachuMascotBadge(
                        drawableRes = if (tip.coachMood == "ALERT_INACTIVE") R.drawable.pika_orange_wave else R.drawable.pika_logo_hi,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = tip.headline,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = tip.advice,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = PikaVoltYellow.copy(alpha = if (isDark) 0.14f else 0.22f),
                    border = BorderStroke(1.dp, PikaVoltYellow.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = if (isDark) PikaVoltYellow else PikaVoltAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NEXT MICRO-ACTION",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isDark) PikaVoltYellow else PikaVoltAmber
                            )
                            Text(
                                text = tip.actionLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRefreshTip,
                        modifier = Modifier
                            .weight(0.95f)
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        border = BorderStroke(1.5.dp, borderColor)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "New Gemini Tip",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "New Tip",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onPrimaryAction()
                        },
                        modifier = Modifier
                            .weight(1.25f)
                            .heightIn(min = 50.dp)
                            .testTag("pika_coach_action_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PikaCheekCrimson,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Let's Go, Pika!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PikaCoachDashboardCard(
    pikaCoachState: PikaCoachUiState,
    isDark: Boolean,
    onRefreshTip: () -> Unit,
    onOpenPopup: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF141B2D).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.92f)
    val accentColor = if (isDark) PikaVoltYellow else PikaCheekCrimson

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pika_coach_dashboard_card")
            .clip(RoundedCornerShape(22.dp))
            .border(2.dp, accentColor.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
            .clickable { onOpenPopup() },
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6792A4))
                    .border(2.dp, PikaVoltYellow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PikachuMascotBadge(
                    drawableRes = R.drawable.pika_logo_hi,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "PIKA-COACH AI TIP • TAP FOR POPUP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val displayedTip = when (pikaCoachState) {
                    is PikaCoachUiState.Ready -> pikaCoachState.tip
                    is PikaCoachUiState.Loading -> pikaCoachState.previewTip
                    is PikaCoachUiState.Error -> pikaCoachState.fallbackTip
                    is PikaCoachUiState.Idle -> null
                }

                if (displayedTip == null) {
                    Text(
                        text = "Pika-Coach is analyzing your daily progress & activity...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextSecondary
                    )
                } else {
                    Text(
                        text = displayedTip.headline,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayedTip.advice,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        ),
                        color = TextSecondary,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onRefreshTip,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh Gemini Pika-Coach Tip",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmbeddedDashboard(
    state: HunterSystemState,
    pikaCoachState: PikaCoachUiState = PikaCoachUiState.Idle,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onOpenTutorial: () -> Unit = {},
    onOpenOnboarding: () -> Unit = {},
    onRefreshPikaCoach: () -> Unit = {},
    onOpenPikaCoachPopup: () -> Unit = {},
    onAddWorkoutClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onQuickCalories: (Int, Double) -> Unit = { _, _ -> },
    onQuickWater: (Double) -> Unit = {},
    onUndoFood: () -> Unit = {},
    onUndoWater: () -> Unit = {},
    onLogProductiveTask: (title: String, category: String, dateStr: String) -> Unit = { _, _, _ -> },
    onEditProductiveTask: (dateStr: String, taskId: String, newTitle: String, newCategory: String) -> Unit = { _, _, _, _ -> },
    onRemoveProductiveTask: (dateStr: String, taskId: String) -> Unit = { _, _ -> },
    onOpenEditStreakFoodDialog: () -> Unit = {}
) {
    // Glassmorphic card surfaces so the Pikachu background wallpaper shines through clearly
    val cardBg = if (isDark) {
        Color(0xFF141B2D).copy(alpha = 0.80f)
    } else {
        Color.White.copy(alpha = 0.84f)
    }
    val borderColor = if (isDark) {
        Color(0xFF334155).copy(alpha = 0.85f)
    } else {
        Color(0xFFCBD5E1).copy(alpha = 0.95f)
    }
    val headingTealOrWhite = if (isDark) Color(0xFFF8FAFC) else PikaSlateBlue
    val primaryDynamic = TextPrimary
    val secondaryDynamic = TextSecondary
    val accentGold = if (isDark) PikaVoltYellow else PikaVoltAmber

    val bio = state.biometrics
    val vit = state.statBreakdown.vitality
    val stam = state.statBreakdown.stamina

    val calFraction = (vit.caloriesLogged.toFloat() / vit.caloriesTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val waterFraction = (stam.waterLiters.toFloat() / stam.targetLiters.coerceAtLeast(0.5).toFloat()).coerceIn(0f, 1f)
    val weightGoalFraction = when (bio.fitnessGoal) {
        "WEIGHT_LOSS" -> 0.68f
        "WEIGHT_GAIN" -> 0.74f
        else -> 0.80f
    }

    var selectedGoalHorizon by rememberSaveable { mutableIntStateOf(0) } // 0 = Daily, 1 = Weekly, 2 = Monthly

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // 1. REFERENCE TOP HEADER: "Good Morning,\n[Name]" + Waving Pikachu Avatar + "Today"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    val dynamicGreeting = when (currentHour) {
                        in 5..11 -> "Good Morning"
                        in 12..16 -> "Good Afternoon"
                        in 17..20 -> "Good Evening"
                        else -> "Good Night"
                    }
                    Text(
                        text = "$dynamicGreeting,\n${bio.hunterName}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 31.sp,
                            lineHeight = 37.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = headingTealOrWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${state.playerStatus.currentTier.classTitle} (${state.playerStatus.currentTier.pokemonName}) • ${state.playerStatus.currentXp} XP • ${bio.weightKg} kg",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.5.dp, PikaVoltYellow, CircleShape)
                            .clickable { onOpenTutorial() },
                        contentAlignment = Alignment.Center
                    ) {
                        PikachuMascotBadge(
                            drawableRes = state.playerStatus.currentTier.pokemonDrawableRes,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // "Today" Sub-header row with Edit Setup action (Matches middle screen of reference image)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = headingTealOrWhite
                )

                Surface(
                    modifier = Modifier.clickable { onOpenOnboarding() },
                    shape = RoundedCornerShape(50),
                    color = cardBg,
                    border = BorderStroke(1.5.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Trainer Profile",
                            tint = PikaCheekCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Trainer Info",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = primaryDynamic
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GEMINI-POWERED PIKA-COACH CONTEXT-AWARE BANNER
            PikaCoachDashboardCard(
                pikaCoachState = pikaCoachState,
                isDark = isDark,
                onRefreshTip = onRefreshPikaCoach,
                onOpenPopup = onOpenPikaCoachPopup
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. HERO CRIMSON RED ROUNDED DONUT CARD (Directly inspired by Middle Screen in Reference Image)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = PikaCheekCrimson.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF7253B),
                                    PikaCheekCrimson,
                                    PikaCrimsonDeep
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Crisp White Donut Progress Ring with large bold center numbers
                            Box(
                                modifier = Modifier.size(106.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 9.5.dp.toPx()
                                    // Subtle translucent white track
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.26f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    // Crisp solid white progress arc (only drawn when progress > 0)
                                    if (calFraction > 0.001f) {
                                        val displaySweep = (360f * calFraction.coerceIn(0f, 1f)).coerceAtLeast(8f)
                                        drawArc(
                                            color = Color.White,
                                            startAngle = -90f,
                                            sweepAngle = displaySweep,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${vit.caloriesLogged}",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Kcal",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White.copy(alpha = 0.90f),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Right: Bold White "Energy & Walk", Goal, and Sub-metrics (Water & Protein)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Energy & Walk",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Goal: ${vit.caloriesTarget} kcal",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White.copy(alpha = 0.92f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.25f)) {
                                        Text(
                                            text = "${stam.waterLiters}L / ${stam.targetLiters}L",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Water",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Column(modifier = Modifier.weight(0.75f)) {
                                        Text(
                                            text = "${vit.proteinG.toInt()}g",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Protein",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Action Pills inside the Crimson Hero Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onQuickCalories(350, 28.0) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Restaurant,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "+350 kcal",
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onQuickWater(0.5) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.WaterDrop,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "+0.5L Water",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (vit.caloriesLogged > 0 || stam.waterLiters > 0.0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dashboard_undo_food_button")
                                        .clickable(enabled = vit.caloriesLogged > 0) { onUndoFood() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = if (vit.caloriesLogged > 0) 0.26f else 0.10f),
                                    border = BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = if (vit.caloriesLogged > 0) 0.55f else 0.20f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo Food Entry",
                                            tint = Color.White.copy(alpha = if (vit.caloriesLogged > 0) 1f else 0.45f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Undo Food",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = Color.White.copy(alpha = if (vit.caloriesLogged > 0) 1f else 0.45f)
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dashboard_undo_water_button")
                                        .clickable(enabled = stam.waterLiters > 0.0) { onUndoWater() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = if (stam.waterLiters > 0.0) 0.26f else 0.10f),
                                    border = BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = if (stam.waterLiters > 0.0) 0.55f else 0.20f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo Water Entry",
                                            tint = Color.White.copy(alpha = if (stam.waterLiters > 0.0) 1f else 0.45f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Undo Water",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = Color.White.copy(alpha = if (stam.waterLiters > 0.0) 1f else 0.45f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 3. "YOUR GOALS" (Daily • Weekly • Monthly) SLIDER CARD (Matches 3rd Screen in Reference Image)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.5.dp, borderColor, RoundedCornerShape(26.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YOUR GOALS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = headingTealOrWhite
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (state.playerStatus.checkedInToday) {
                                HunterDoneGreen.copy(alpha = 0.18f)
                            } else {
                                PikaCheekCrimson.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = if (state.playerStatus.checkedInToday) "✓ Streak Active" else "Log to Check In",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = if (state.playerStatus.checkedInToday) HunterDoneGreen else PikaCheekCrimson
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily  •  Weekly  •  Monthly selector row
                    val horizons = listOf("Daily", "Weekly", "Monthly")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        horizons.forEachIndexed { idx, label ->
                            val isSelected = selectedGoalHorizon == idx
                            Text(
                                text = label,
                                modifier = Modifier.clickable { selectedGoalHorizon = idx },
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                ),
                                color = if (isSelected) headingTealOrWhite else secondaryDynamic
                            )
                            if (idx < horizons.lastIndex) {
                                Text(
                                    text = "•",
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                    color = secondaryDynamic
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val horizonMultiplier = when (selectedGoalHorizon) {
                        1 -> 7
                        2 -> 30
                        else -> 1
                    }

                    // Goal Bar 1: Weight Goal (Crimson Red Track + Dot)
                    ReferenceGoalProgressRow(
                        label = bio.goalDisplayLabel,
                        valueText = "${bio.weightKg} kg • BMI ${bio.bmi}",
                        progress = weightGoalFraction,
                        barColor = PikaCheekCrimson,
                        textColor = primaryDynamic,
                        subTextColor = secondaryDynamic
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Goal Bar 2: Calories / Steps per day (Amber Gold Track)
                    ReferenceGoalProgressRow(
                        label = if (selectedGoalHorizon == 0) "Daily Calories Target" else "Calories target (${horizons[selectedGoalHorizon]})",
                        valueText = "${vit.caloriesLogged * horizonMultiplier} / ${vit.caloriesTarget * horizonMultiplier} kcal",
                        progress = calFraction.coerceIn(0f, 1f),
                        barColor = Color(0xFFF59E0B),
                        textColor = primaryDynamic,
                        subTextColor = secondaryDynamic
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Goal Bar 3: Drinking water (Deep Indigo/Purple Track)
                    ReferenceGoalProgressRow(
                        label = "Drinking water",
                        valueText = "${(stam.waterLiters * horizonMultiplier * 10.0).roundToInt() / 10.0}L / ${(stam.targetLiters * horizonMultiplier * 10.0).roundToInt() / 10.0}L",
                        progress = waterFraction.coerceIn(0f, 1f),
                        barColor = PikaDeepIndigo,
                        textColor = primaryDynamic,
                        subTextColor = secondaryDynamic
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. BOTTOM DUAL-WAVE PROGRESS CURVE CARD (Directly inspired by Bottom of Middle Screen in Reference Image)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.5.dp, borderColor, RoundedCornerShape(26.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AVG. ${vit.caloriesTarget} KCAL GOAL",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = headingTealOrWhite
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (state.playerStatus.checkedInToday) "You did great today, Pika!" else "Log workout or meal to power up!",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = secondaryDynamic
                            )
                        }

                        Text(
                            text = "${state.playerStatus.streakDays}D : LVL ${state.playerStatus.level}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = PikaCheekCrimson
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Smooth Dual-Wave Bezier Canvas (Crimson Red Wave + Slate-Blue Wave + Peak Indicator Dot)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(138.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Secondary Slate-Blue Wave Path + Soft Area Fill
                            val blueLinePath = Path().apply {
                                moveTo(0f, h * 0.80f)
                                cubicTo(w * 0.14f, h * 0.56f, w * 0.24f, h * 0.90f, w * 0.38f, h * 0.68f)
                                cubicTo(w * 0.52f, h * 0.48f, w * 0.64f, h * 0.82f, w * 0.78f, h * 0.58f)
                                cubicTo(w * 0.88f, h * 0.44f, w * 0.95f, h * 0.64f, w, h * 0.48f)
                            }
                            val blueFillPath = Path().apply {
                                addPath(blueLinePath)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }
                            drawPath(
                                path = blueFillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF38BDF8).copy(alpha = 0.22f),
                                        Color(0xFF1E6076).copy(alpha = 0.04f)
                                    )
                                )
                            )
                            drawPath(
                                path = blueLinePath,
                                color = if (isDark) Color(0xFF38BDF8) else PikaSlateBlue,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Primary Crimson Red Wave Path Ending in Red Peak Indicator Dot
                            val peakX = w * 0.85f
                            val peakY = h * 0.18f
                            val redLinePath = Path().apply {
                                moveTo(0f, h * 0.86f)
                                cubicTo(w * 0.15f, h * 0.74f, w * 0.25f, h * 0.56f, w * 0.36f, h * 0.64f)
                                cubicTo(w * 0.46f, h * 0.70f, w * 0.52f, h * 0.28f, w * 0.63f, h * 0.36f)
                                cubicTo(w * 0.72f, h * 0.44f, w * 0.78f, h * 0.36f, peakX, peakY)
                            }
                            drawPath(
                                path = redLinePath,
                                color = PikaCheekCrimson,
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Outer glow + solid red circle at the peak (matches middle phone in reference image)
                            drawCircle(
                                color = PikaCheekCrimson.copy(alpha = 0.28f),
                                radius = 9.dp.toPx(),
                                center = Offset(peakX, peakY)
                            )
                            drawCircle(
                                color = PikaCheekCrimson,
                                radius = 5.dp.toPx(),
                                center = Offset(peakX, peakY)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 5. PIKACHU WORKOUTS & WALLPAPER SHOWCASE BANNER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PikaFit Workouts",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = headingTealOrWhite
                )
                TextButton(onClick = onAddWorkoutClick) {
                    Text(
                        text = "+ Log Set",
                        color = PikaCheekCrimson,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, PikaVoltYellow.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
                    .clickable { onAddWorkoutClick() },
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        PikachuMascotBadge(
                            drawableRes = R.drawable.pika_orange_wave,
                            modifier = Modifier
                                .size(62.dp)
                                .border(2.5.dp, PikaVoltYellow, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Thunderbolt Overload Session",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = primaryDynamic
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${state.statBreakdown.strength.exercisesLogged.size} exercises logged today • Tap to add sets",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = secondaryDynamic
                            )
                        }
                    }
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = accentGold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 6. DAILY TRAINER QUESTS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Trainer Quests",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = headingTealOrWhite
                )
                TextButton(
                    onClick = onAddTaskClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Open Log →",
                        maxLines = 1,
                        softWrap = false,
                        color = PikaCheekCrimson,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.questLog.forEach { quest ->
                    val isDone = quest.status == "COMPLETED"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBg)
                            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
                            .clickable { onAddTaskClick() }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDone) HunterDoneGreen else Color.Transparent)
                                    .border(
                                        2.dp,
                                        if (isDone) HunterDoneGreen else secondaryDynamic,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = quest.questName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                                ),
                                color = if (isDone) secondaryDynamic else primaryDynamic
                            )
                        }
                        Text(
                            text = "+${quest.xpAwarded} XP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = PikaCheekCrimson
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }

        FloatingActionButton(
            onClick = onAddWorkoutClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            containerColor = PikaCheekCrimson,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Log Meal or Workout",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ReferenceGoalProgressRow(
    label: String,
    valueText: String,
    progress: Float,
    barColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = textColor
            )
            Text(
                text = valueText,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = subTextColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { safeProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.18f),
            strokeCap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHunterScreen(
    currentUserId: String,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager by remember { lazy { CredentialManager.create(context) } }

    val viewModel: HunterViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                HunterViewModel(HunterRepository(app, currentUserId), currentUserId)
            }
        }
    )

    val state by viewModel.systemState.collectAsStateWithLifecycle()
    val isSessionLoaded by viewModel.isSessionLoaded.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var settingsFocusSection by rememberSaveable { mutableIntStateOf(0) }
    var showTutorial by rememberSaveable { mutableStateOf(false) }
    var forceShowOnboarding by rememberSaveable { mutableStateOf(false) }
    var showEditStreakFoodDialog by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            PikaFitNotificationScheduler.startAutomaticRandomNotifications(context)
        }
    }

    LaunchedEffect(isSessionLoaded, state.biometrics.onboardingCompleted) {
        if (isSessionLoaded) {
            PikaFitNotificationScheduler.ensureNotificationChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !PikaFitNotificationScheduler.hasNotificationPermission(context)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                PikaFitNotificationScheduler.startAutomaticRandomNotifications(context)
            }
        }
    }

    // Cycling Pikachu wallpaper override (0 = Page Default, 1 = Hi Pikachu, 2 = Pokéball Pikachu, 3 = Orange Wave Pikachu)
    var wallpaperMode by rememberSaveable { mutableIntStateOf(0) }

    // Wait until Firestore/per-UID onboarding status has been resolved so returning Google accounts
    // never flash or repeat the "Hey Trainer" onboarding screen.
    if (!isSessionLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PikaVoltYellow)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Syncing Trainer Profile...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextPrimary
                )
            }
        }
        return
    }

    // Show the step-by-step "Hey Trainer" Onboarding screen ONLY ONCE per Google account
    // (or if manually opened via "Edit Trainer Info" / "Redo Setup").
    if (!state.biometrics.onboardingCompleted || forceShowOnboarding) {
        PikaFitOnboardingScreen(
            initialBiometrics = state.biometrics,
            isDark = isDark,
            onCompleteOnboarding = { name, age, sex, heightCm, weightKg, goal, experience ->
                viewModel.completeTrainerOnboarding(
                    trainerName = name,
                    age = age,
                    biologicalSex = sex,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    fitnessGoal = goal,
                    experienceLevel = experience
                )
                forceShowOnboarding = false
            }
        )
        return
    }

    val pikaCoachState by viewModel.pikaCoachState.collectAsStateWithLifecycle()
    val nutritionChatMessages by viewModel.nutritionChatMessages.collectAsStateWithLifecycle()
    val isNutritionChatLoading by viewModel.isNutritionChatLoading.collectAsStateWithLifecycle()
    val foodHistory by viewModel.foodHistory.collectAsStateWithLifecycle()
    val waterHistory by viewModel.waterHistory.collectAsStateWithLifecycle()
    val aiSyncState by viewModel.aiSyncState.collectAsStateWithLifecycle()
    val logsState by viewModel.logsState.collectAsStateWithLifecycle()
    val journalState by viewModel.journalState.collectAsStateWithLifecycle()
    val feedbackState by viewModel.feedbackState.collectAsStateWithLifecycle()
    val releaseInfo by viewModel.appReleaseInfo.collectAsStateWithLifecycle()
    val otaPatch by viewModel.otaPatchManifest.collectAsStateWithLifecycle()
    val installedVersionName by viewModel.installedVersionName.collectAsStateWithLifecycle()
    val installedVersionCode by viewModel.installedVersionCode.collectAsStateWithLifecycle()
    val dismissedUpdateVersion by viewModel.dismissedUpdateVersion.collectAsStateWithLifecycle()

    // Context-aware Gemini Pika-Coach Popup state on the Dashboard
    val popupTipPair: Pair<PikaCoachTip, Boolean>? = when (val cs = pikaCoachState) {
        is PikaCoachUiState.Ready -> Pair(cs.tip, cs.showPopup)
        is PikaCoachUiState.Error -> Pair(cs.fallbackTip, cs.showPopup)
        else -> null
    }

    if (selectedTab != 0) {
        BackHandler { selectedTab = 0 }
    }

    val activeWallpaperRes = when (wallpaperMode) {
        1 -> R.drawable.pika_logo_hi
        2 -> R.drawable.pika_pokeball_bg
        3 -> R.drawable.pika_orange_wave
        else -> when (selectedTab) {
            0 -> R.drawable.pika_logo_hi
            1 -> R.drawable.pika_orange_wave
            2 -> R.drawable.pika_pokeball_bg
            3 -> R.drawable.pika_orange_wave
            4 -> R.drawable.pika_pokeball_bg
            else -> R.drawable.pika_logo_hi
        }
    }

    val topBarAccent = if (isDark) PikaVoltYellow else PikaCheekCrimson
    val navTabs = remember {
        listOf(
            Triple(0, "Home", Icons.Filled.Bolt),
            Triple(1, "Streaks", Icons.Filled.LocalFireDepartment),
            Triple(2, "Charts", Icons.AutoMirrored.Filled.ShowChart),
            Triple(3, "Workout", Icons.Filled.FitnessCenter),
            Triple(4, "Journal", Icons.Filled.Person),
            Triple(5, "PikaBot", Icons.Filled.SmartToy),
            Triple(6, "Settings", Icons.Filled.Settings)
        )
    }

    // Render the entire Scaffold INSIDE PikachuWallpaperBackground so the Pikachu wallpaper
    // is visible edge-to-edge behind the TopAppBar, content, and NavigationBar / NavigationRail!
    PikachuWallpaperBackground(
        wallpaperRes = activeWallpaperRes,
        isDark = isDark,
        wallpaperAlpha = if (isDark) 0.52f else 0.56f,
        modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            titleContentColor = TextPrimary
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6792A4))
                                        .border(2.dp, PikaVoltYellow, CircleShape)
                                        .clickable { showTutorial = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PikachuMascotBadge(
                                        drawableRes = R.drawable.pika_logo_hi,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "PikaFit • ${state.biometrics.hunterName}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = TextPrimary
                                    )
                                    val streakLabel = when {
                                        state.playerStatus.restorableStreakDays > 0 ->
                                            "⚡ Restore ${state.playerStatus.restorableStreakDays}D"
                                        else ->
                                            "🔥 ${state.playerStatus.streakDays}D"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = "XP Lv ${state.playerStatus.level} • ${state.playerStatus.currentTier.classTitle}",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            color = TextSecondary,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .clickable { selectedTab = 1 }
                                                .testTag("top_bar_streak_edit_button"),
                                            shape = RoundedCornerShape(50),
                                            color = when {
                                                state.playerStatus.restorableStreakDays > 0 ->
                                                    PikaCheekCrimson.copy(alpha = 0.2f)
                                                state.playerStatus.checkedInToday ->
                                                    HunterDoneGreen.copy(alpha = 0.16f)
                                                else -> topBarAccent.copy(alpha = 0.16f)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    state.playerStatus.restorableStreakDays > 0 -> PikaCheekCrimson
                                                    state.playerStatus.checkedInToday -> HunterDoneGreen
                                                    else -> topBarAccent
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = streakLabel,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                maxLines = 1,
                                                softWrap = false,
                                                color = when {
                                                    state.playerStatus.restorableStreakDays > 0 -> PikaCheekCrimson
                                                    state.playerStatus.checkedInToday -> HunterDoneGreen
                                                    else -> topBarAccent
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        actions = {
                            // Button to cycle Pikachu background wallpapers across all pages
                            IconButton(
                                onClick = { wallpaperMode = (wallpaperMode + 1) % 4 },
                                modifier = Modifier
                                    .padding(end = 3.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Wallpaper,
                                    contentDescription = "Cycle Pikachu Wallpaper",
                                    tint = topBarAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier
                                    .padding(end = 3.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = "Toggle Light/Dark Theme",
                                    tint = topBarAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Sign Out Button so user can switch Google accounts and test one-time onboarding
                            IconButton(
                                onClick = {
                                    signOutHunter(
                                        credentialManager = credentialManager,
                                        onSignOutComplete = onSignedOut,
                                        scope = scope
                                    )
                                },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PikaCheekCrimson.copy(alpha = 0.16f))
                                    .testTag("sign_out_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out of Google Account",
                                    tint = PikaCheekCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            contentColor = TextPrimary,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            navTabs.forEach { (idx, label, icon) ->
                                NavigationBarItem(
                                    selected = selectedTab == idx,
                                    onClick = {
                                        if (idx == 6) settingsFocusSection = 0
                                        selectedTab = idx
                                    },
                                    modifier = Modifier.testTag("nav_tab_${label.lowercase()}"),
                                    icon = { Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Visible,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                letterSpacing = (-0.3).sp,
                                                fontWeight = if (selectedTab == idx) FontWeight.ExtraBold else FontWeight.SemiBold
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = if (isDark) PikaVoltYellow else PikaCheekCrimson,
                                        selectedTextColor = if (isDark) PikaVoltYellow else PikaCheekCrimson,
                                        indicatorColor = (if (isDark) PikaVoltYellow else PikaCheekCrimson).copy(alpha = 0.18f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            contentColor = TextPrimary
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            navTabs.forEach { (idx, label, icon) ->
                                NavigationRailItem(
                                    selected = selectedTab == idx,
                                    onClick = {
                                        if (idx == 6) settingsFocusSection = 0
                                        selectedTab = idx
                                    },
                                    modifier = Modifier.testTag("nav_tab_${label.lowercase()}"),
                                    icon = { Icon(imageVector = icon, contentDescription = label) },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            softWrap = false,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (selectedTab == idx) FontWeight.ExtraBold else FontWeight.SemiBold
                                            )
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = if (isDark) PikaVoltYellow else PikaCheekCrimson,
                                        selectedTextColor = if (isDark) PikaVoltYellow else PikaCheekCrimson,
                                        indicatorColor = (if (isDark) PikaVoltYellow else PikaCheekCrimson).copy(alpha = 0.18f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(modifier = Modifier.fillMaxSize().widthIn(max = 640.dp)) {
                            AiSyncStatusBanner(aiSyncState = aiSyncState, onDismiss = viewModel::dismissAiSyncBanner)

                            when (selectedTab) {
                                0 -> EmbeddedDashboard(
                                    state = state,
                                    pikaCoachState = pikaCoachState,
                                    isDark = isDark,
                                    onToggleTheme = onToggleTheme,
                                    onOpenTutorial = { showTutorial = true },
                                    onOpenOnboarding = { forceShowOnboarding = true },
                                    onRefreshPikaCoach = { viewModel.generatePikaCoachTip(showAsPopup = true) },
                                    onOpenPikaCoachPopup = viewModel::reopenPikaCoachPopup,
                                    onAddWorkoutClick = { selectedTab = 3 },
                                    onAddTaskClick = { selectedTab = 3 },
                                    onQuickCalories = viewModel::quickAdjustCalories,
                                    onQuickWater = { liters -> viewModel.quickAddWater(liters) },
                                    onUndoFood = { viewModel.undoLastFoodEntry() },
                                    onUndoWater = { viewModel.undoLastWaterEntry() },
                                    onLogProductiveTask = viewModel::logProductiveTask,
                                    onEditProductiveTask = viewModel::editProductiveTask,
                                    onRemoveProductiveTask = viewModel::removeProductiveTask,
                                    onOpenEditStreakFoodDialog = { showEditStreakFoodDialog = true }
                                )
                                1 -> StreaksTab(
                                    state = state,
                                    isDark = isDark,
                                    onRestoreMissedStreak = viewModel::restoreMissedStreakWithXp,
                                    onLogFoodItem = { meal, cal, pro -> viewModel.logFoodItem(meal, cal, pro) },
                                    onLogExerciseSet = viewModel::addExerciseSet,
                                    onLogProductiveTask = viewModel::logProductiveTask,
                                    onRemoveProductiveTask = viewModel::removeProductiveTask,
                                    onUndoFood = { viewModel.undoLastFoodEntry() },
                                    onUndoWater = { viewModel.undoLastWaterEntry() },
                                    onUpdateHistoricalDayFoodLog = viewModel::updateHistoricalDayFoodLog
                                )
                                2 -> BiometricChartsScreen(
                                    state = state,
                                    journalState = journalState,
                                    nutritionChatMessages = nutritionChatMessages,
                                    isNutritionChatLoading = isNutritionChatLoading,
                                    onSendNutritionMessage = viewModel::sendNutritionChatMessage,
                                    onClearNutritionChat = viewModel::clearNutritionChat,
                                    onLogBiometricPoint = viewModel::logBiometricPointToFirestore,
                                    onLogFoodItem = { meal, cal, pro -> viewModel.logFoodItem(meal, cal, pro) },
                                    onLogExerciseSet = viewModel::addExerciseSet,
                                    onUndoFood = { viewModel.undoLastFoodEntry() },
                                    onUndoWater = { viewModel.undoLastWaterEntry() },
                                    onUpdateHistoricalDayFoodLog = viewModel::updateHistoricalDayFoodLog,
                                    onLogProductiveTask = viewModel::logProductiveTask,
                                    onRemoveProductiveTask = viewModel::removeProductiveTask
                                )
                                3 -> StatsBreakdownTab(
                                    state = state,
                                    foodHistory = foodHistory,
                                    waterHistory = waterHistory,
                                    onQuickCalories = viewModel::quickAdjustCalories,
                                    onLogFoodItem = { meal, cal, pro -> viewModel.logFoodItem(meal, cal, pro) },
                                    onLogSmartFoodItem = { meal, cal, pro, carbs, fats, fiber, grams ->
                                        viewModel.logFoodItem(
                                            mealName = meal,
                                            calories = cal,
                                            proteinG = pro,
                                            carbsG = carbs,
                                            fatsG = fats,
                                            fiberG = fiber,
                                            grams = grams
                                        )
                                    },
                                    onQuickWater = { liters -> viewModel.quickAddWater(liters) },
                                    onLogCustomWater = { liters, label -> viewModel.quickAddWater(liters, label) },
                                    onUndoFood = viewModel::undoLastFoodEntry,
                                    onUndoWater = viewModel::undoLastWaterEntry,
                                    onAddExercise = viewModel::addExerciseSet,
                                    onToggleQuest = viewModel::toggleQuestStatus
                                )
                                4 -> ProfileAndJournalTab(
                                    state = state,
                                    journalState = journalState,
                                    onSaveBiometrics = viewModel::updateProfileAndBiometrics,
                                    onSaveBiometricsWithGoal = viewModel::updateProfileAndBiometrics,
                                    onLogProgressPhoto = viewModel::logProgressPhotoToJournal,
                                    onDeleteJournalEntry = viewModel::deleteJournalEntry,
                                    onOpenAchievementsInSettings = {
                                        settingsFocusSection = 4
                                        selectedTab = 6
                                    },
                                    onLogProductiveTask = viewModel::logProductiveTask,
                                    onEditProductiveTask = viewModel::editProductiveTask,
                                    onRemoveProductiveTask = viewModel::removeProductiveTask
                                )
                                5 -> PikaBotScreen(
                                    state = state,
                                    messages = nutritionChatMessages,
                                    isLoading = isNutritionChatLoading,
                                    onSendMessage = viewModel::sendNutritionChatMessage,
                                    onClearChat = viewModel::clearNutritionChat,
                                    onLogRecommendedFood = { meal, cal, pro -> viewModel.logFoodItem(meal, cal, pro) },
                                    onApplyDietPlan = { msgId, goalKey, planTitle, calTarget, proTarget, waterTarget ->
                                        viewModel.applyDietPlanFromPikaBot(
                                            messageId = msgId,
                                            goalKey = goalKey,
                                            planTitle = planTitle,
                                            caloriesTarget = calTarget,
                                            proteinTargetG = proTarget,
                                            waterTargetLiters = waterTarget
                                        )
                                    },
                                    onLogRecommendedExercise = { msgId, exName, exSets, exReps ->
                                        viewModel.logExerciseFromPikaBot(
                                            messageId = msgId,
                                            exerciseName = exName,
                                            sets = exSets,
                                            reps = exReps
                                        )
                                    }
                                )
                                6 -> SettingsAndFeedbackScreen(
                                    state = state,
                                    feedbackState = feedbackState,
                                    currentUserId = currentUserId,
                                    initialFocusSection = settingsFocusSection,
                                    releaseInfo = releaseInfo,
                                    otaPatchManifest = otaPatch,
                                    installedVersionName = installedVersionName,
                                    installedVersionCode = installedVersionCode,
                                    isDark = isDark,
                                    onToggleTheme = onToggleTheme,
                                    onBack = { selectedTab = 0 },
                                    onCheckForUpdates = { viewModel.checkForAppUpdates(manualUserTrigger = true) },
                                    onMarkUpdateInstalled = { viewModel.markLatestUpdateInstalled(releaseInfo) },
                                    onPublishOtaPatch = { pVer, pNum, banner, notes ->
                                        viewModel.publishOtaPatchBroadcast(pVer, pNum, banner, notes)
                                    },
                                    onPublishRelease = { _, versionName, releaseNotes, downloadUrl ->
                                        viewModel.publishNewAppRelease(
                                            versionName = versionName,
                                            releaseTitle = "PikaFit v$versionName Update",
                                            releaseNotes = releaseNotes,
                                            downloadUrl = downloadUrl
                                        )
                                    },
                                    onSaveProfileAndTargets = viewModel::updateSettingsProfileAndTargets,
                                    onSaveProfileWithGoal = viewModel::updateSettingsProfileAndTargets,
                                    onResetProgress = { clearJournal ->
                                        viewModel.resetHunterProgress(clearJournal)
                                        forceShowOnboarding = true
                                    },
                                    onSubmitFeedback = viewModel::submitPublicFeedback,
                                    onUpdateFeedbackStatus = viewModel::updateFeedbackStatus,
                                    onDeleteFeedback = viewModel::deleteFeedback
                                )
                                else -> TacticalAiConsoleTab(
                                    state = state,
                                    aiSyncState = aiSyncState,
                                    logsState = logsState,
                                    onTransmitCommand = viewModel::submitTacticalCommand
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showEditStreakFoodDialog) {
            val todayStr = com.example.data.model.StreakDateUtils.todayDateString()
            val initialDate = if (state.playerStatus.checkedInToday || state.statBreakdown.vitality.caloriesLogged > 0 || state.playerStatus.dailyLogsByDate.containsKey(todayStr)) {
                todayStr
            } else {
                com.example.data.model.StreakDateUtils.daysAgoDateString(1)
            }
            EditHistoricalDayFoodLogDialog(
                initialDateStr = initialDate,
                state = state,
                onDismiss = { showEditStreakFoodDialog = false },
                onSaveDayLog = { dateStr, cals, pro, carbs, fats, water, summary, items ->
                    viewModel.updateHistoricalDayFoodLog(dateStr, cals, pro, carbs, fats, water, summary, items)
                    showEditStreakFoodDialog = false
                }
            )
        }

        val isNewVersionAvailable = (
            releaseInfo.isUpdateAvailable ||
                releaseInfo.latestVersionCode > installedVersionCode ||
                releaseInfo.patchNumber > otaPatch.patchNumber
            ) &&
            dismissedUpdateVersion != releaseInfo.latestVersionCode &&
            dismissedUpdateVersion != releaseInfo.patchNumber

        if (isNewVersionAvailable) {
            AppUpdateDialog(
                releaseInfo = releaseInfo,
                isDark = isDark,
                onDismiss = {
                    val dismissKey = maxOf(releaseInfo.latestVersionCode, releaseInfo.patchNumber)
                    viewModel.dismissUpdatePrompt(dismissKey)
                },
                onDownload = {
                    // Apply OTA patch silently in-app without any GitHub browser redirect or APK reinstall
                    viewModel.markLatestUpdateInstalled(releaseInfo)
                    viewModel.checkForAppUpdates(manualUserTrigger = false)
                }
            )
        }

        if (popupTipPair != null && popupTipPair.second && selectedTab == 0 && !isNewVersionAvailable) {
            val activeTip = popupTipPair.first
            PikaCoachPopupDialog(
                tip = activeTip,
                isDark = isDark,
                onDismiss = viewModel::dismissPikaCoachPopup,
                onPrimaryAction = {
                    when (activeTip.actionType) {
                        "LOG_FOOD" -> viewModel.quickAdjustCalories(350, 28.0)
                        "LOG_WATER" -> viewModel.quickAddWater(0.5)
                        "LOG_WORKOUT" -> selectedTab = 3
                        else -> {}
                    }
                },
                onRefreshTip = {
                    viewModel.generatePikaCoachTip(showAsPopup = true)
                }
            )
        }

        if (showTutorial) {
            PikachuTutorialDialog(
                trainerName = state.biometrics.hunterName,
                onReopenOnboarding = { forceShowOnboarding = true },
                onDismiss = { showTutorial = false }
            )
        }
    }
}

@Composable
private fun AiSyncStatusBanner(aiSyncState: AiSyncState, onDismiss: () -> Unit) {
    val isDark = AppThemeIsDark
    val chargingAccent = if (isDark) PikaVoltYellow else PikaVoltAmber
    AnimatedVisibility(visible = aiSyncState !is AiSyncState.Idle) {
        val (bgColor, borderColor, text) = when (aiSyncState) {
            is AiSyncState.Analyzing -> Triple(chargingAccent.copy(alpha = 0.16f), chargingAccent, "PIKAFIT AI CHARGING: \"${aiSyncState.command}\"...")
            is AiSyncState.Success -> Triple(HunterDoneGreen.copy(alpha = 0.18f), HunterDoneGreen, aiSyncState.message)
            is AiSyncState.Error -> Triple(PikaCheekCrimson.copy(alpha = 0.18f), PikaCheekCrimson, aiSyncState.message)
            AiSyncState.Idle -> Triple(Color.Transparent, Color.Transparent, "")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                .clickable { onDismiss() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (aiSyncState is AiSyncState.Analyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = chargingAccent, strokeWidth = 2.5.dp)
                } else {
                    Icon(imageVector = Icons.Filled.Bolt, contentDescription = null, tint = borderColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextPrimary
                )
            }
            Text(
                text = "DISMISS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 1,
                softWrap = false,
                color = borderColor,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Composable
fun StatsBreakdownTab(
    state: HunterSystemState,
    foodHistory: List<FoodEntryRecord> = emptyList(),
    waterHistory: List<WaterEntryRecord> = emptyList(),
    onQuickCalories: (Int, Double) -> Unit,
    onLogFoodItem: (String, Int, Double) -> Unit,
    onLogSmartFoodItem: (mealName: String, calories: Int, proteinG: Double, carbsG: Double, fatsG: Double, fiberG: Double, grams: Int) -> Unit =
        { meal, cal, pro, _, _, _, _ -> onLogFoodItem(meal, cal, pro) },
    onQuickWater: (Double) -> Unit,
    onLogCustomWater: (Double, String?) -> Unit = { liters, _ -> onQuickWater(liters) },
    onUndoFood: (String?) -> Unit = {},
    onUndoWater: (String?) -> Unit = {},
    onAddExercise: (String, Int, String, Boolean) -> Unit,
    onToggleQuest: (Int) -> Unit
) {
    val vit = state.statBreakdown.vitality
    val stam = state.statBreakdown.stamina
    val str = state.statBreakdown.strength
    val isDark = AppThemeIsDark
    val cardBg = if (isDark) Color(0xFF141B2D).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.90f)

    // Smart Serving Unit Mode: "QUANTITY", "GRAMS", or "MILLILITERS" (auto-predicted from food name)
    var selectedUnitModeName by rememberSaveable { mutableStateOf("QUANTITY") }
    val selectedUnitMode = when (selectedUnitModeName) {
        "MILLILITERS" -> com.example.util.FoodServingUnitMode.MILLILITERS
        "GRAMS" -> com.example.util.FoodServingUnitMode.GRAMS
        else -> com.example.util.FoodServingUnitMode.QUANTITY
    }
    val isQuantityMode = selectedUnitMode == com.example.util.FoodServingUnitMode.QUANTITY
    var smartFoodQuery by rememberSaveable { mutableStateOf("") }
    var smartFoodAmountInput by rememberSaveable { mutableStateOf("") }

    val predictedUnit = remember(smartFoodQuery) {
        SmartFoodNutritionEngine.predictServingUnit(smartFoodQuery)
    }

    // Automatically predict and switch serving unit (ml vs g vs quantity) as user types food name
    LaunchedEffect(predictedUnit.matchedFoodName, predictedUnit.recommendedMode, predictedUnit.isLiquidMl) {
        if (smartFoodQuery.isNotBlank()) {
            selectedUnitModeName = predictedUnit.recommendedMode.name
            val currentVal = smartFoodAmountInput.toIntOrNull() ?: 0
            when (predictedUnit.recommendedMode) {
                com.example.util.FoodServingUnitMode.MILLILITERS -> {
                    if (currentVal <= 20) smartFoodAmountInput = predictedUnit.defaultAmountForRecommended.toString()
                }
                com.example.util.FoodServingUnitMode.GRAMS -> {
                    if (currentVal <= 20) smartFoodAmountInput = predictedUnit.defaultAmountForRecommended.toString()
                }
                com.example.util.FoodServingUnitMode.QUANTITY -> {
                    if (currentVal > 20) smartFoodAmountInput = "1"
                }
            }
        }
    }

    val defaultFallbackAmt = when (selectedUnitMode) {
        com.example.util.FoodServingUnitMode.QUANTITY -> 1
        com.example.util.FoodServingUnitMode.MILLILITERS -> 200
        com.example.util.FoodServingUnitMode.GRAMS -> 100
    }
    val parsedAmount = smartFoodAmountInput.toIntOrNull()?.coerceIn(1, 5000) ?: defaultFallbackAmt
    val smartCalc = remember(smartFoodQuery, parsedAmount, selectedUnitMode) {
        if (smartFoodQuery.isBlank()) {
            null
        } else {
            SmartFoodNutritionEngine.calculateForGramsOrQuantity(
                foodInput = smartFoodQuery,
                amountValue = parsedAmount,
                isQuantityMode = isQuantityMode
            )
        }
    }

    // Merge foodHistory with today's resolved DailyDateLog items so every individual food logged today is visible
    val todayResolvedFoodEntries = remember(foodHistory, state.playerStatus.dailyLogsByDate) {
        val todayStr = com.example.data.model.StreakDateUtils.todayDateString()
        val fromDailyLog = state.playerStatus.dailyLogsByDate[todayStr]?.resolvedFoodItems().orEmpty()
        if (fromDailyLog.isNotEmpty()) {
            fromDailyLog.map { item ->
                FoodEntryRecord(
                    id = item.id,
                    mealName = item.mealName,
                    calories = item.calories,
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatsG = item.fatsG,
                    fiberG = item.fiberG,
                    grams = item.grams,
                    xpAwarded = 45
                )
            }
        } else {
            foodHistory
        }
    }

    // Manual Food Entry state (empty defaults)
    var showManualFoodFields by rememberSaveable { mutableStateOf(false) }
    var foodName by rememberSaveable { mutableStateOf("") }
    var foodKcal by rememberSaveable { mutableStateOf("") }
    var foodProtein by rememberSaveable { mutableStateOf("") }

    // Separate Custom Water Hydration state (empty defaults)
    var waterUnitIsMl by rememberSaveable { mutableStateOf(true) } // true = ml, false = Liters
    var customWaterAmountInput by rememberSaveable { mutableStateOf("") }
    var customWaterNote by rememberSaveable { mutableStateOf("") }

    // Exercise state (empty defaults)
    var exName by rememberSaveable { mutableStateOf("") }
    var exSets by rememberSaveable { mutableStateOf("") }
    var exReps by rememberSaveable { mutableStateOf("") }
    var overloadHit by rememberSaveable { mutableStateOf(true) }

    val canUndoFood = vit.caloriesLogged > 0 || vit.proteinG > 0.0 || todayResolvedFoodEntries.isNotEmpty()
    val canUndoWater = stam.waterLiters > 0.0 || waterHistory.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // =========================================================================
        // 1. SMART FOOD (QUANTITY / GRAMS / ML) CONVERTER & DAILY MEAL LOG CARD
        // =========================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.5.dp, PikaCheekCrimson.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            PikachuMascotBadge(
                                drawableRes = R.drawable.pika_pokeball_bg,
                                modifier = Modifier
                                    .size(46.dp)
                                    .border(2.dp, PikaVoltYellow, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Smart Food & Drink Logger",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Auto-predicts Quantity, Grams (g) or Milliliters (ml)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.2.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily Calorie & Macro Progress Summary Bar
                    Text(
                        text = "Calories: ${vit.caloriesLogged} / ${vit.caloriesTarget} kcal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Protein: ${vit.proteinG}g • Carbs: ${vit.carbsG.toInt()}g • Fats: ${vit.fatsG.toInt()}g • Fiber: ${vit.fiberG.toInt()}g",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (vit.caloriesLogged.toFloat() / vit.caloriesTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = PikaCheekCrimson
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // SMART QUANTITY / GRAMS / MILLILITERS CONVERTER PANEL
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("smart_grams_to_calories_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        border = BorderStroke(1.5.dp, PikaVoltYellow.copy(alpha = 0.65f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "⚡ SMART QUANTITY / GRAMS / ML -> CALORIES & MACRO CONVERTER",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // INPUT MODE TOGGLE: Quantity vs Grams (g) vs Milliliters (ml)
                            val qtyUnitLabel = if (smartFoodQuery.isNotBlank() && predictedUnit.quantityUnitSingular.length <= 7) {
                                "Qty (${predictedUnit.quantityUnitSingular})"
                            } else {
                                "Quantity"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val isQtySelected = selectedUnitMode == com.example.util.FoodServingUnitMode.QUANTITY
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedUnitModeName = "QUANTITY"
                                            if ((smartFoodAmountInput.toIntOrNull() ?: 1) > 20) {
                                                smartFoodAmountInput = "1"
                                            }
                                        }
                                        .testTag("mode_quantity_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isQtySelected) PikaVoltYellow else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isQtySelected) PikaVoltYellow else BorderSubtle
                                    )
                                ) {
                                    Text(
                                        text = qtyUnitLabel,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = if (isQtySelected) Color(0xFF0F172A) else TextPrimary
                                    )
                                }

                                val isGramsSelected = selectedUnitMode == com.example.util.FoodServingUnitMode.GRAMS
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedUnitModeName = "GRAMS"
                                            if ((smartFoodAmountInput.toIntOrNull() ?: 100) <= 20) {
                                                smartFoodAmountInput = "100"
                                            }
                                        }
                                        .testTag("mode_grams_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isGramsSelected) PikaVoltYellow else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isGramsSelected) PikaVoltYellow else BorderSubtle
                                    )
                                ) {
                                    Text(
                                        text = "Grams (g)",
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = if (isGramsSelected) Color(0xFF0F172A) else TextPrimary
                                    )
                                }

                                val isMlSelected = selectedUnitMode == com.example.util.FoodServingUnitMode.MILLILITERS
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedUnitModeName = "MILLILITERS"
                                            if ((smartFoodAmountInput.toIntOrNull() ?: 200) <= 20) {
                                                smartFoodAmountInput = "200"
                                            }
                                        }
                                        .testTag("mode_ml_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isMlSelected) PikaVoltYellow else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isMlSelected) PikaVoltYellow else BorderSubtle
                                    )
                                ) {
                                    Text(
                                        text = "Liquid (ml)",
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = if (isMlSelected) Color(0xFF0F172A) else TextPrimary
                                    )
                                }
                            }

                            if (smartFoodQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = predictedUnit.predictedBadgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (predictedUnit.isLiquidMl) HunterDoneGreen else PikaVoltYellow
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Food Name & Quantity/Grams/ml Input Row
                            val amountFieldLabel = when (selectedUnitMode) {
                                com.example.util.FoodServingUnitMode.QUANTITY ->
                                    if (smartFoodQuery.isNotBlank()) "Qty (${predictedUnit.quantityUnitSingular})" else "Qty"
                                com.example.util.FoodServingUnitMode.MILLILITERS -> "Volume (ml)"
                                com.example.util.FoodServingUnitMode.GRAMS -> "Grams (g)"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = smartFoodQuery,
                                    onValueChange = { smartFoodQuery = it },
                                    label = {
                                        Text(
                                            text = "Enter Food, Dish or Juice Name",
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1.65f)
                                        .testTag("smart_food_name_input")
                                )
                                OutlinedTextField(
                                    value = smartFoodAmountInput,
                                    onValueChange = { input ->
                                        smartFoodAmountInput = input.filter { it.isDigit() }.take(4)
                                    },
                                    label = {
                                        Text(
                                            text = amountFieldLabel,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .testTag("smart_food_grams_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Presets Row (1x..6x in Quantity mode, 100ml..500ml in Milliliters mode, or 50g..300g in Grams mode)
                            val quickNumbers = predictedUnit.quickPresetsForMode(selectedUnitMode)
                            val quickChipSuffix = when (selectedUnitMode) {
                                com.example.util.FoodServingUnitMode.QUANTITY -> "x"
                                com.example.util.FoodServingUnitMode.MILLILITERS -> "ml"
                                com.example.util.FoodServingUnitMode.GRAMS -> "g"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickNumbers.forEach { num ->
                                    val selectedN = parsedAmount == num
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { smartFoodAmountInput = num.toString() },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selectedN) PikaVoltYellow else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selectedN) PikaVoltYellow else BorderSubtle
                                        )
                                    ) {
                                        Text(
                                            text = "$num$quickChipSuffix",
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = if (selectedN) Color(0xFF0F172A) else TextPrimary
                                        )
                                    }
                                }
                            }

                            if (smartCalc != null) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // Live Nutritional Breakdown Preview Card (Shows Quantity + Equivalent Grams/ml + Macros)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "${smartCalc.foodName} [${smartCalc.servingLabel}] - ${smartCalc.description}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Nutritional Breakdown :-",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "• Calories: ${smartCalc.calories} kcal\n• Carbs: ${smartCalc.carbsG}g\n• Fats: ${smartCalc.fatsG}g",
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = 18.sp
                                                ),
                                                color = TextPrimary
                                            )
                                            val measureLabel = if (smartCalc.isLiquidMl) "Volume" else "Weight"
                                            Text(
                                                text = "• Protein: ${smartCalc.proteinG}g\n• Fiber: ${smartCalc.fiberG}g\n• $measureLabel: ${smartCalc.grams}${smartCalc.unitSuffix}",
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = 18.sp
                                                ),
                                                color = HunterDoneGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "• Key Nutrients - ${smartCalc.micronutrients}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 1-Tap Log Converted Food to Daily Meal Button
                                Button(
                                    onClick = {
                                        val suffix = smartCalc.unitSuffix
                                        val label = if (smartCalc.foodName.contains("(${smartCalc.grams}$suffix)", ignoreCase = true) ||
                                            smartCalc.foodName.contains("(${smartCalc.grams}g)", ignoreCase = true) ||
                                            smartCalc.foodName.contains("(${smartCalc.grams}ml)", ignoreCase = true)
                                        ) {
                                            SmartFoodNutritionEngine.normalizeMealNameUnits(smartCalc.foodName)
                                        } else if (isQuantityMode) {
                                            SmartFoodNutritionEngine.normalizeMealNameUnits("${parsedAmount}x ${smartCalc.foodName} (${smartCalc.grams}$suffix)")
                                        } else {
                                            SmartFoodNutritionEngine.normalizeMealNameUnits("${smartCalc.foodName} (${smartCalc.grams}$suffix)")
                                        }
                                        onLogSmartFoodItem(
                                            label,
                                            smartCalc.calories,
                                            smartCalc.proteinG,
                                            smartCalc.carbsG,
                                            smartCalc.fatsG,
                                            smartCalc.fiberG,
                                            smartCalc.grams
                                        )
                                        smartFoodQuery = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PikaVoltYellow,
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("smart_convert_and_log_food_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Restaurant,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Log ${smartCalc.foodName} (+${smartCalc.calories} kcal) ⚡",
                                        maxLines = 1,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Type any dish, meal, or juice above — Smart Logger predicts the serving unit (ml, g, or qty) and calculates macros!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Calorie Presets & Undo Food Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onQuickCalories(300, 25.0) },
                            modifier = Modifier.weight(1f).testTag("quick_add_300_kcal")
                        ) {
                            Text("+300 kcal", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = { onQuickCalories(500, 35.0) },
                            modifier = Modifier.weight(1f).testTag("quick_add_500_kcal")
                        ) {
                            Text("+500 kcal", fontWeight = FontWeight.Bold, color = PikaCheekCrimson)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // UNDO FOOD BUTTON & TOGGLE MANUAL ENTRY
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val lastFoodCals = todayResolvedFoodEntries.lastOrNull()?.calories ?: vit.caloriesLogged.coerceAtMost(350)
                        OutlinedButton(
                            onClick = { onUndoFood(null) },
                            enabled = canUndoFood,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("undo_food_entry_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (canUndoFood) PikaCheekCrimson else BorderSubtle
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (canUndoFood) PikaCheekCrimson.copy(alpha = 0.12f) else Color.Transparent,
                                contentColor = if (canUndoFood) PikaCheekCrimson else TextSecondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Food Entry",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (canUndoFood) "Undo Food (-${lastFoodCals} kcal)" else "Undo Food",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = { showManualFoodFields = !showManualFoodFields },
                            modifier = Modifier
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = if (showManualFoodFields) "Hide Manual" else "Manual Kcal",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )
                        }
                    }

                    // Optional Manual Calorie/Protein Input
                    AnimatedVisibility(visible = showManualFoodFields) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = foodName,
                                onValueChange = { foodName = it },
                                label = { Text("Custom Meal Name", fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = foodKcal,
                                    onValueChange = { foodKcal = it },
                                    label = { Text("Calories (kcal)", fontSize = 14.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = foodProtein,
                                    onValueChange = { foodProtein = it },
                                    label = { Text("Protein (g)", fontSize = 14.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val cals = foodKcal.toIntOrNull()?.coerceIn(10, 10000) ?: 500
                                    val pro = foodProtein.toDoubleOrNull()?.coerceIn(0.0, 500.0) ?: 35.0
                                    onLogFoodItem(foodName.ifBlank { "Custom Meal" }, cals, pro)
                                    foodName = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PikaCheekCrimson,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = "Log Manual Meal & Update Streak ⚡",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                        }
                    }

                    // Individual Logged Food items for Today (never combined!) with per-entry Undo/Remove button
                    if (todayResolvedFoodEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TODAY'S LOGGED FOODS (${todayResolvedFoodEntries.size})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.6.sp
                                        ),
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${todayResolvedFoodEntries.sumOf { it.calories }} kcal total",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = HunterDoneGreen
                                    )
                                }

                                todayResolvedFoodEntries.reversed().forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PikaCheekCrimson.copy(alpha = 0.10f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "🍽 ${entry.mealName}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp
                                                ),
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "+${entry.calories} kcal • P: ${entry.proteinG}g • C: ${entry.carbsG.toInt()}g • F: ${entry.fatsG.toInt()}g",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = TextSecondary
                                            )
                                        }
                                        TextButton(
                                            onClick = { onUndoFood(entry.id) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                                contentDescription = "Remove ${entry.mealName}",
                                                tint = PikaCheekCrimson,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Undo",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = PikaCheekCrimson
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

        // =========================================================================
        // 2. SEPARATE DEDICATED WATER HYDRATION LOG CARD (CUSTOM ML / LITERS)
        // =========================================================================
        item {
            val waterAccent = if (isDark) Color(0xFF38BDF8) else PikaDeepIndigo
            val parsedWaterRaw = customWaterAmountInput.toDoubleOrNull() ?: 0.0
            val calculatedLiters = if (waterUnitIsMl) {
                round((parsedWaterRaw / 1000.0) * 100.0) / 100.0
            } else {
                round(parsedWaterRaw * 100.0) / 100.0
            }.coerceIn(0.05, 10.0)
            val calculatedMl = (calculatedLiters * 1000).roundToInt()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dedicated_water_hydration_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.5.dp, waterAccent.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(waterAccent.copy(alpha = 0.16f))
                                    .border(2.dp, waterAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WaterDrop,
                                    contentDescription = null,
                                    tint = waterAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Water Hydration Log 💧",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Log any custom water amount in ml or Liters",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val remainingLiters = (round((stam.targetLiters - stam.waterLiters) * 100.0) / 100.0).coerceAtLeast(0.0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Logged: ${stam.waterLiters}L / ${stam.targetLiters}L (${(stam.waterLiters * 1000).roundToInt()} ml)",
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = if (remainingLiters <= 0.0) "Goal Met! ✓" else "${remainingLiters}L left",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (remainingLiters <= 0.0) HunterDoneGreen else waterAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (stam.waterLiters.toFloat() / stam.targetLiters.coerceAtLeast(0.5).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = waterAccent
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Custom Water Size Buttons (150ml, 250ml, 350ml, 500ml, 750ml, 1L)
                    Text(
                        text = "QUICK HYDRATION SIZES (TAP TO LOG INSTANTLY)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val waterPresets = listOf(
                        Triple(0.15, "+150ml", "Small Cup"),
                        Triple(0.25, "+250ml", "1 Glass"),
                        Triple(0.35, "+350ml", "Mug / Can"),
                        Triple(0.50, "+500ml", "0.5L Bottle"),
                        Triple(0.75, "+750ml", "Shaker"),
                        Triple(1.00, "+1.0L", "1L Jug")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        waterPresets.forEach { (litersVal, amountTitle, subLabel) ->
                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        onLogCustomWater(litersVal, "$amountTitle ($subLabel)")
                                    }
                                    .testTag("quick_add_water_${(litersVal * 1000).roundToInt()}ml"),
                                shape = RoundedCornerShape(14.dp),
                                color = waterAccent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, waterAccent.copy(alpha = 0.65f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$amountTitle 💧",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = subLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Custom Water Input Section (Enter any exact ml or Liters)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CUSTOM WATER",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = waterAccent
                                )

                                // Unit Switcher: ml vs Liters
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        modifier = Modifier.clickable {
                                            if (!waterUnitIsMl) {
                                                val curL = customWaterAmountInput.toDoubleOrNull() ?: 0.25
                                                customWaterAmountInput = (curL * 1000).roundToInt().toString()
                                                waterUnitIsMl = true
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (waterUnitIsMl) waterAccent else Color.Transparent,
                                        border = BorderStroke(1.dp, waterAccent)
                                    ) {
                                        Text(
                                            text = "ml",
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = if (waterUnitIsMl) Color.White else TextPrimary
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier.clickable {
                                            if (waterUnitIsMl) {
                                                val curMl = customWaterAmountInput.toDoubleOrNull() ?: 250.0
                                                customWaterAmountInput = (round((curMl / 1000.0) * 100.0) / 100.0).toString()
                                                waterUnitIsMl = false
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!waterUnitIsMl) waterAccent else Color.Transparent,
                                        border = BorderStroke(1.dp, waterAccent)
                                    ) {
                                        Text(
                                            text = "Liters",
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = if (!waterUnitIsMl) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = customWaterAmountInput,
                                    onValueChange = { input ->
                                        customWaterAmountInput = input.filter { it.isDigit() || it == '.' }.take(6)
                                    },
                                    label = {
                                        Text(
                                            text = if (waterUnitIsMl) "Water (ml) e.g. 300" else "Water (L) e.g. 0.35",
                                            fontSize = 13.sp
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("custom_water_amount_input")
                                )

                                OutlinedTextField(
                                    value = customWaterNote,
                                    onValueChange = { customWaterNote = it },
                                    label = { Text("Label (Optional)", fontSize = 13.sp) },
                                    placeholder = { Text("e.g. Post-Run", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("custom_water_note_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val cleanNote = customWaterNote.trim()
                                    val label = if (cleanNote.isNotEmpty()) {
                                        "$cleanNote (+${calculatedLiters}L / ${calculatedMl}ml)"
                                    } else {
                                        "+${calculatedLiters}L (${calculatedMl}ml) Water"
                                    }
                                    onLogCustomWater(calculatedLiters, label)
                                    customWaterNote = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = waterAccent,
                                    contentColor = if (isDark) Color(0xFF0F172A) else Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("log_custom_water_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WaterDrop,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Log Custom Water (+${calculatedLiters}L / ${calculatedMl}ml) 💧",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Undo Water Button
                    val lastWaterLiters = waterHistory.lastOrNull()?.litersDelta ?: 0.25
                    OutlinedButton(
                        onClick = { onUndoWater(null) },
                        enabled = canUndoWater,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("undo_water_entry_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (canUndoWater) waterAccent else BorderSubtle
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (canUndoWater) waterAccent.copy(alpha = 0.12f) else Color.Transparent,
                            contentColor = if (canUndoWater) waterAccent else TextSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo Water Entry",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (canUndoWater) "Undo Last Water Entry (-${lastWaterLiters}L)" else "Undo Water Entry",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }

                    // Recent Water Entries List with per-entry Undo
                    if (waterHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "RECENT WATER LOGS (TAP UNDO TO REMOVE)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.7.sp
                                    ),
                                    color = TextSecondary
                                )

                                waterHistory.takeLast(5).reversed().forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(waterAccent.copy(alpha = 0.12f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "💧 ${entry.label}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp
                                                ),
                                                color = TextPrimary
                                            )
                                        }
                                        TextButton(
                                            onClick = { onUndoWater(entry.id) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                                contentDescription = "Remove ${entry.label}",
                                                tint = waterAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Undo",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = waterAccent
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

        // =========================================================================
        // 3. WORKOUT OVERLOAD + CALORIES BURNED & GOAL DIET / SURPLUS-DEFICIT CARD
        // =========================================================================
        item {
            val workoutDietSummary = remember(
                str.exercisesLogged,
                state.biometrics,
                vit.caloriesLogged,
                vit.caloriesTarget,
                vit.proteinG
            ) {
                SmartFoodNutritionEngine.calculateWorkoutBurnAndDietPlan(
                    exercises = str.exercisesLogged,
                    bio = state.biometrics,
                    caloriesLogged = vit.caloriesLogged,
                    caloriesTarget = vit.caloriesTarget,
                    proteinLogged = vit.proteinG
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.5.dp, PikaCheekCrimson.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            PikachuMascotBadge(
                                drawableRes = R.drawable.pika_orange_wave,
                                modifier = Modifier
                                    .size(46.dp)
                                    .border(2.dp, PikaCheekCrimson, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Workout & Burn Analysis",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Calories Burned • Post-Workout Meals • Surplus/Deficit",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PikaCheekCrimson.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, PikaCheekCrimson.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "🔥 ${workoutDietSummary.totalWorkoutBurnKcal} kcal",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Logged Exercises with Per-Exercise Calories Burned
                    if (workoutDietSummary.exerciseBreakdown.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "LOGGED SETS & EXPENDITURE (${workoutDietSummary.totalSetsCompleted} SETS TOTAL)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                                )
                                workoutDietSummary.exerciseBreakdown.forEach { burnItem ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${burnItem.exerciseName}: ${burnItem.sets} sets × ${burnItem.reps} reps",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "-${burnItem.caloriesBurned} kcal",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = HunterDoneGreen
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Muscle Group & Specific Exercise Quick-Picker
                    var selectedMuscleFilter by rememberSaveable { mutableStateOf("Chest") }
                    val muscleExercisesMap = remember {
                        mapOf(
                            "Chest" to listOf(
                                Triple("Incline Dumbbell Press", "4", "10"),
                                Triple("Flat Barbell Bench Press", "4", "8"),
                                Triple("Declined Pushups", "4", "15"),
                                Triple("Cable Crossover Fly", "3", "15"),
                                Triple("Weighted Chest Dips", "3", "12")
                            ),
                            "Back" to listOf(
                                Triple("Wide-Grip Lat Pulldown", "4", "12"),
                                Triple("Pull-Ups / Chin-Ups", "4", "8"),
                                Triple("Barbell Bent-Over Row", "4", "10"),
                                Triple("One-Arm Dumbbell Row", "3", "12"),
                                Triple("Seated Cable / Table Row", "3", "12")
                            ),
                            "Shoulders" to listOf(
                                Triple("Dumbbell Lateral Raise", "4", "15"),
                                Triple("Overhead Shoulder Press", "4", "10"),
                                Triple("Face Pulls (Rear Delts)", "4", "15"),
                                Triple("Reverse Pec Deck Fly", "3", "15"),
                                Triple("Dumbbell Shrugs", "4", "12")
                            ),
                            "Biceps" to listOf(
                                Triple("Incline Dumbbell Curl", "4", "12"),
                                Triple("EZ-Bar Bicep Curl", "4", "10"),
                                Triple("Dumbbell Hammer Curl", "3", "12"),
                                Triple("Preacher Curl", "3", "12"),
                                Triple("Concentration Curl", "3", "12")
                            ),
                            "Triceps" to listOf(
                                Triple("Tricep Rope Pushdown", "4", "15"),
                                Triple("Overhead Tricep Extension", "4", "12"),
                                Triple("EZ-Bar Skull Crushers", "3", "10"),
                                Triple("Close-Grip Bench Press", "3", "10"),
                                Triple("Diamond Pushups", "3", "15")
                            ),
                            "Legs" to listOf(
                                Triple("Barbell Back Squat", "4", "8"),
                                Triple("Leg Press", "4", "12"),
                                Triple("Romanian Deadlift (RDL)", "4", "10"),
                                Triple("Bulgarian Split Squat", "3", "10"),
                                Triple("Standing Calf Raises", "4", "15")
                            ),
                            "Abs" to listOf(
                                Triple("Hanging Leg Raises", "4", "15"),
                                Triple("Kneeling Cable Crunch", "4", "15"),
                                Triple("Plank Hold", "3", "60s"),
                                Triple("Bicycle Crunches", "3", "20"),
                                Triple("Ab Wheel Rollout", "3", "12")
                            )
                        )
                    }

                    Text(
                        text = "SELECT MUSCLE GROUP & TAP AN EXERCISE TO AUTOFILL:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        muscleExercisesMap.keys.forEach { muscle ->
                            val isSelected = selectedMuscleFilter == muscle
                            Surface(
                                modifier = Modifier.clickable { selectedMuscleFilter = muscle },
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) PikaVoltYellow else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) PikaVoltYellow else BorderSubtle)
                            ) {
                                Text(
                                    text = "💪 $muscle",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (isSelected) Color(0xFF0F172A) else TextPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        muscleExercisesMap[selectedMuscleFilter].orEmpty().forEach { (presetEx, presetSets, presetReps) ->
                            val isChosen = exName.equals(presetEx, ignoreCase = true)
                            Surface(
                                modifier = Modifier.clickable {
                                    exName = presetEx
                                    exSets = presetSets
                                    exReps = presetReps
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChosen) PikaCheekCrimson else PikaCheekCrimson.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, PikaCheekCrimson.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = "$presetEx ($presetSets×$presetReps)",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isChosen) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Add / Update Exercise Set Inputs
                    OutlinedTextField(
                        value = exName,
                        onValueChange = { exName = it },
                        label = { Text("Exercise Name (e.g. Bench Press, Squats, Pushups)", fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workout_exercise_name_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = exSets,
                            onValueChange = { exSets = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Sets", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = exReps,
                            onValueChange = { exReps = it },
                            label = { Text("Reps", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (exName.isNotBlank()) {
                                onAddExercise(exName, exSets.toIntOrNull() ?: 3, exReps.ifBlank { "12" }, overloadHit)
                                exName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PikaCheekCrimson,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("log_exercise_set_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Log Exercise Set & Recalculate Burn ⚡",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // =========================================================================
                    // POST-WORKOUT CALORIES BURNED, SURPLUS/DEFICIT & DAILY DIET GUIDANCE PANEL
                    // =========================================================================
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workout_burn_and_diet_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, PikaVoltYellow.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "🔥 WORKOUT EXPENDITURE & GOAL INTAKE PROTOCOL",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                            )

                            // 3 Stat Pills: Burned | Burn-Adjusted Target | Remaining Intake
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = PikaCheekCrimson.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, PikaCheekCrimson.copy(alpha = 0.45f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "BURNED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${workoutDietSummary.totalWorkoutBurnKcal} kcal",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = PikaCheekCrimson
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = PikaVoltYellow.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, PikaVoltYellow.copy(alpha = 0.45f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "NEW TARGET",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${workoutDietSummary.burnAdjustedTargetKcal} kcal",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = if (isDark) PikaVoltYellow else TextPrimary
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = HunterDoneGreen.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, HunterDoneGreen.copy(alpha = 0.45f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "TO INTAKE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${workoutDietSummary.remainingCaloriesNeeded} kcal",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = HunterDoneGreen
                                        )
                                    }
                                }
                            }

                            // Goal Surplus (Weight Gain) or Reduction/Deficit (Weight Loss) Breakdown Box
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = workoutDietSummary.surplusOrDeficitHeadline,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = workoutDietSummary.surplusOrDeficitAdvice,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp
                                        ),
                                        color = TextPrimary
                                    )
                                }
                            }

                            // Recommended Meals to Take Today (Post-Workout & Recovery) with 1-Tap Log
                            Text(
                                text = "🍽 MEALS TO TAKE FOR THIS WORKOUT (TAP TO LOG):",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = HunterDoneGreen
                            )

                            workoutDietSummary.recommendedMeals.forEach { meal ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${meal.timingTitle}: ${meal.mealName}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${meal.portionDescription} • ${meal.calories} kcal • P:${meal.proteinG.toInt()}g C:${meal.carbsG.toInt()}g F:${meal.fatsG.toInt()}g",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            onLogSmartFoodItem(
                                                meal.mealName,
                                                meal.calories,
                                                meal.proteinG,
                                                meal.carbsG,
                                                meal.fatsG,
                                                4.0,
                                                250
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PikaVoltYellow,
                                            contentColor = Color(0xFF0F172A)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(
                                            text = "+Log",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        )
                                    }
                                }
                            }

                            // Full-Day Diet Protocol Summary
                            Text(
                                text = "📋 DAILY DIET PROTOCOL FOR TODAY:",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = if (isDark) PikaVoltYellow else PikaCheekCrimson
                            )
                            workoutDietSummary.dailyDietProtocolBullets.forEach { bullet ->
                                Text(
                                    text = "• $bullet",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalAiConsoleTab(
    state: HunterSystemState,
    aiSyncState: AiSyncState,
    logsState: DataLoadState<List<TacticalLog>>,
    onTransmitCommand: (String) -> Unit
) {
    var commandInput by rememberSaveable { mutableStateOf("") }
    val isBusy = aiSyncState is AiSyncState.Analyzing

    Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        Text(
            text = "PikaFit AI Assistant",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("e.g. \"Ate 2000 kcal and did 3x10 squats\"") },
                enabled = !isBusy,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        onTransmitCommand(commandInput)
                        commandInput = ""
                    }
                },
                enabled = !isBusy && commandInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PikaVoltYellow, contentColor = Color(0xFF0F172A))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}