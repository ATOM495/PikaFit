package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ReminderCategory(
    val key: String,
    val title: String,
    val defaultMessages: List<String>
) {
    WATER(
        key = "WATER",
        title = "💧 PikaFit Hydration Reminder",
        defaultMessages = listOf(
            "Time to drink 250ml–500ml of water! Stay hydrated and log your water intake in PikaFit.",
            "Hydration check! Drink a glass of water now to keep your Trainer stamina at 100%.",
            "Don't let dehydration slow your recovery — drink water and tap +0.25L or +0.5L in PikaFit!"
        )
    ),
    FOOD(
        key = "FOOD",
        title = "🍱 PikaFit Smart Food Log Reminder",
        defaultMessages = listOf(
            "Have you logged your recent meal or snack? Open Smart Food Logger to auto-calculate your calories & protein!",
            "Fuel check! Log your food intake today so your Daily Productive Streak and macro targets stay on track.",
            "Don't forget to record what you ate today — even a quick meal or shake counts toward your Trainer XP!"
        )
    ),
    EXERCISE(
        key = "EXERCISE",
        title = "💪 PikaFit Workout & Exercise Reminder",
        defaultMessages = listOf(
            "Did you complete any exercises or sets today? Log your workout in PikaFit to verify your streak!",
            "Time to move! Input your completed exercises, sets, and reps to level up your Pokémon class.",
            "Strength check-in: Log your workout session or quick bodyweight sets today for bonus XP!"
        )
    ),
    TASKS(
        key = "TASKS",
        title = "✅ PikaFit Daily Tasks & Productivity Reminder",
        defaultMessages = listOf(
            "What productive tasks or work did you complete today? Log your completed tasks in your Journal!",
            "Productivity check-in! Record the work, study, or personal tasks you finished today to earn +35 XP.",
            "Been productive today? Add your completed tasks to your Daily Productivity Log in the Journal page!"
        )
    ),
    STREAK_RESTORE(
        key = "STREAK_RESTORE",
        title = "🛡️ PikaFit • Streak Shield Alert",
        defaultMessages = listOf(
            "You missed yesterday! Open Streaks tab to restore your lost streak using 1,000 XP!",
            "1-Day Streak Recovery Available! Spend 1,000 XP in PikaFit → Streaks to save your Pokémon class rank."
        )
    )
}

data class ReminderSettingsState(
    val enabled: Boolean = true,
    val waterEnabled: Boolean = true,
    val foodEnabled: Boolean = true,
    val exerciseEnabled: Boolean = true,
    val tasksEnabled: Boolean = true,
    val minMinutes: Int = 15,
    val maxMinutes: Int = 55,
    val nextScheduledTimeMillis: Long = 0L,
    val nextScheduledCategory: String = "WATER"
)

object PikaFitNotificationScheduler {
    const val CHANNEL_ID = "pikafit_smart_reminders"
    private const val PREFS_NAME = "pikafit_reminder_prefs"
    private const val KEY_NEXT_TIME = "reminders_next_time_millis"
    private const val KEY_NEXT_CATEGORY = "reminders_next_category"
    private const val KEY_LAST_SENT_TIME = "reminders_last_sent_millis"
    private const val KEY_LAST_CATEGORY = "reminders_last_category"
    private const val KEY_LAST_MISSED_STREAK_NOTIFIED_DATE = "last_missed_streak_notified_date"

    private const val ALARM_REQUEST_CODE = 4950

    @Volatile
    private var otaExtraMessages: Map<String, List<String>> = emptyMap()

    private val autoReminderScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    private var autoLoopJob: Job? = null

    fun applyOtaExtraMessages(extra: Map<String, List<String>>) {
        otaExtraMessages = extra.mapKeys { it.key.trim().uppercase() }
            .mapValues { (_, list) -> list.map { it.trim() }.filter { it.isNotBlank() } }
            .filterValues { it.isNotEmpty() }
    }

    fun getMessagesForCategory(category: ReminderCategory): List<String> {
        val extras = otaExtraMessages[category.key].orEmpty()
        return if (extras.isEmpty()) category.defaultMessages else (category.defaultMessages + extras).distinct()
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PikaFit Automatic Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Automatic random reminders for water, food, workouts, tasks, and 1-day missed streak recovery."
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getSettings(context: Context): ReminderSettingsState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderSettingsState(
            enabled = true,
            waterEnabled = true,
            foodEnabled = true,
            exerciseEnabled = true,
            tasksEnabled = true,
            minMinutes = 15,
            maxMinutes = 55,
            nextScheduledTimeMillis = prefs.getLong(KEY_NEXT_TIME, 0L),
            nextScheduledCategory = prefs.getString(KEY_NEXT_CATEGORY, "WATER") ?: "WATER"
        )
    }

    fun pickNextRandomCategory(context: Context): ReminderCategory {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastKey = prefs.getString(KEY_LAST_CATEGORY, null)
        val routinePool = ReminderCategory.entries.filter { it != ReminderCategory.STREAK_RESTORE }
        val candidates = routinePool.filter { it.key != lastKey }.ifEmpty { routinePool }
        return candidates[Random.nextInt(candidates.size)]
    }

    /**
     * Automatically notifies once per missed date when a Trainer misses 1 day so they can restore
     * their lost streak using 1,000 XP.
     */
    fun checkAndNotifyMissedStreak(
        context: Context,
        restorableDays: Int,
        missedDate: String,
        currentXp: Int,
        restoreCost: Int
    ): Boolean {
        if (restorableDays <= 0 || missedDate.isBlank()) return false
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) return false

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotifiedDate = prefs.getString(KEY_LAST_MISSED_STREAK_NOTIFIED_DATE, "") ?: ""
        if (lastNotifiedDate == missedDate) return false

        val message = if (currentXp >= restoreCost) {
            "You missed 1 day ($missedDate)! Your ${restorableDays}-day streak can be restored right now for $restoreCost XP (You have $currentXp XP). Open Streaks tab to restore!"
        } else {
            val need = (restoreCost - currentXp).coerceAtLeast(0)
            "You missed 1 day ($missedDate)! Earn $need more XP to restore your ${restorableDays}-day streak (Cost: $restoreCost XP)."
        }

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_reminder_category", ReminderCategory.STREAK_RESTORE.key)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            5199,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pikafit_notification)
            .setContentTitle("🛡️ Restore Your ${restorableDays}D Streak ($restoreCost XP)")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(appContext).notify(5299, notification)
            prefs.edit().putString(KEY_LAST_MISSED_STREAK_NOTIFIED_DATE, missedDate).apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Automatically starts and manages random notifications without any user intervention.
     * Schedules both an OS AlarmManager broadcast and an automatic in-process random timer loop.
     */
    fun startAutomaticRandomNotifications(context: Context) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val nextScheduled = prefs.getLong(KEY_NEXT_TIME, 0L)
        if (nextScheduled <= now) {
            scheduleNextRandomReminder(appContext)
        }

        if (autoLoopJob?.isActive == true) return
        autoLoopJob = autoReminderScope.launch {
            val lastSent = prefs.getLong(KEY_LAST_SENT_TIME, 0L)
            val initialDelayMs = if (now - lastSent > 15 * 60_000L) {
                Random.nextLong(60_000L, 150_000L)
            } else {
                Random.nextLong(15 * 60_000L, 45 * 60_000L)
            }
            delay(initialDelayMs)

            while (isActive) {
                if (hasNotificationPermission(appContext)) {
                    val currentLastSent = prefs.getLong(KEY_LAST_SENT_TIME, 0L)
                    if (System.currentTimeMillis() - currentLastSent >= 5 * 60_000L) {
                        val category = pickNextRandomCategory(appContext)
                        showReminderNotification(appContext, category)
                        scheduleNextRandomReminder(appContext)
                    }
                }
                val nextRandomWaitMs = Random.nextLong(18 * 60_000L, 50 * 60_000L)
                delay(nextRandomWaitMs)
            }
        }
    }

    fun scheduleNextRandomReminder(context: Context): ReminderSettingsState {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        val randomDelayMinutes = Random.nextInt(15, 56)
        val triggerAtMillis = System.currentTimeMillis() + (randomDelayMinutes * 60_000L)
        val category = pickNextRandomCategory(appContext)

        val intent = Intent(appContext, HunterReminderReceiver::class.java).apply {
            action = "com.example.ACTION_PIKAFIT_RANDOM_REMINDER"
            putExtra("reminder_category", category.key)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        try {
            alarmManager?.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: Exception) {
            try {
                alarmManager?.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }

        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putLong(KEY_NEXT_TIME, triggerAtMillis)
            putString(KEY_NEXT_CATEGORY, category.key)
            apply()
        }

        return getSettings(appContext)
    }

    fun showReminderNotification(
        context: Context,
        categoryOverride: ReminderCategory? = null
    ): Boolean {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) {
            return false
        }

        val category = categoryOverride ?: pickNextRandomCategory(appContext)
        val messages = getMessagesForCategory(category)
        val message = messages[Random.nextInt(messages.size)]

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_reminder_category", category.key)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            category.ordinal + 5100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pikafit_notification)
            .setContentTitle(category.title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(appContext).notify(
                5200 + category.ordinal,
                notification
            )
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putLong(KEY_LAST_SENT_TIME, System.currentTimeMillis())
                putString(KEY_LAST_CATEGORY, category.key)
                apply()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

class HunterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val catKey = intent?.getStringExtra("reminder_category").orEmpty()
        val category = ReminderCategory.entries.firstOrNull { it.key == catKey }
        PikaFitNotificationScheduler.showReminderNotification(context, category)
        // Automatically chain the next random notification time!
        PikaFitNotificationScheduler.scheduleNextRandomReminder(context)
    }
}
