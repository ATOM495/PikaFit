package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

private val appCheckScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@Volatile
private var isAppCheckConfigured = false

fun configureAppCheck(context: Context, intent: Intent?) {
    if (isAppCheckConfigured) return
    isAppCheckConfigured = true
    val appContext = context.applicationContext
    val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

    appCheckScope.launch {
        try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }

            if (BuildConfig.DEBUG) {
                if (!debugToken.isNullOrEmpty()) {
                    val persistenceKey = FirebaseApp.getInstance().persistenceKey
                    val prefs = appContext.getSharedPreferences(
                        "$DEBUG_STORE_PREFIX$persistenceKey",
                        Context.MODE_PRIVATE
                    )
                    prefs.edit()
                        .putString(DEBUG_SECRET_KEY, debugToken)
                        .apply()
                    Log.d(TAG, "App Check debug secret configured successfully.")
                } else {
                    Log.w(TAG, "No debug token found in Intent extra.")
                }

                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed.")
            } else {
                val resId = appContext.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", appContext.packageName)
                if (resId != 0) {
                    val siteKey = appContext.getString(resId)
                    if (siteKey.isNotBlank()) {
                        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                            RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                        )
                        Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                    } else {
                        Log.e(TAG, "recaptcha_site_key resource string is blank.")
                    }
                } else {
                    Log.e(TAG, "recaptcha_site_key string resource not found in package.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase App Check", e)
        }
    }
}
