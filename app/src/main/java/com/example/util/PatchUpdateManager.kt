package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.ClassTierPatchOverride
import com.example.data.model.OtaPatchManifest
import com.example.data.model.TrainerRankProgression
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ParsedPatchPayload(
    val manifest: OtaPatchManifest,
    val foodPatches: List<FoodPer100gProfile>,
    val extraReminderMessages: Map<String, List<String>>,
    val rawJson: String
)

data class PatchApplyResult(
    val manifest: OtaPatchManifest,
    val newlyApplied: Boolean,
    val appliedFromRemote: Boolean
)

/**
 * Policy-compliant Automatic GitHub OTA Patch Engine.
 * Fetches declarative JSON patch manifests (`patch_manifest.json` on GitHub main branch,
 * GitHub Releases `patch*.json` asset, and Firestore `system/app_patch`) and applies
 * rules, nutrition profiles, streak restore costs, and reminder messages in-app
 * without downloading or executing any remote code / APKs.
 */
object PatchUpdateManager {
    private const val TAG = "PatchUpdateManager"
    private const val PREFS_NAME = "pikafit_ota_patch_prefs"
    private const val KEY_CACHED_PATCH_JSON = "cached_patch_json"
    private const val KEY_CACHED_PATCH_SOURCE = "cached_patch_source"
    private const val KEY_CACHED_PATCH_APPLIED_AT = "cached_patch_applied_at"

    private const val GITHUB_RAW_MANIFEST_URL =
        "https://raw.githubusercontent.com/ATOM495/PikaFit/main/patch_manifest.json"
    private const val GITHUB_CONTENTS_MANIFEST_URL =
        "https://api.github.com/repos/ATOM495/PikaFit/contents/patch_manifest.json"
    private const val GITHUB_RELEASES_LATEST_URL =
        "https://api.github.com/repos/ATOM495/PikaFit/releases/latest"

    fun parsePatchPayload(
        rawJson: String,
        sourceLabel: String = "GitHub OTA",
        appliedAtMillis: Long = System.currentTimeMillis()
    ): ParsedPatchPayload? {
        val trimmed = rawJson.trim()
        if (!trimmed.startsWith("{")) return null
        return try {
            val root = JSONObject(trimmed)
            val patchNum = root.optLong("patch_number", 10301L).coerceAtLeast(1L)
            val patchVer = root.optString("patch_version", "1.0.3-patch.1").trim().ifBlank { "1.0.3-patch.1" }
            val baseAppVer = root.optString("base_app_version", "1.0.2").trim().ifBlank { "1.0.2" }
            val minNativeCode = root.optLong("min_native_version_code", 2L).coerceAtLeast(1L)
            val releaseTitle = root.optString("release_title", "PikaFit OTA Patch").trim()
            val patchNotes = root.optString("patch_notes", "").trim()
            val announcement = root.optString("announcement_banner", "").trim()
            val publishedAt = root.optString("published_at", "").trim()
            val xpPerLevel = root.optInt("xp_per_level", 500).coerceIn(100, 10000)
            val restoreCost = root.optInt("streak_restore_cost_xp", 1000).coerceIn(100, 50000)

            val tierOverrides = mutableListOf<ClassTierPatchOverride>()
            val tiersArr = root.optJSONArray("class_tiers")
            if (tiersArr != null) {
                for (i in 0 until tiersArr.length()) {
                    val obj = tiersArr.optJSONObject(i) ?: continue
                    val lvl = obj.optInt("tier_level", -1)
                    if (lvl in 0..7) {
                        tierOverrides.add(
                            ClassTierPatchOverride(
                                tierLevel = lvl,
                                classTitle = obj.optString("class_title", "").trim(),
                                requiredStreakDays = obj.optInt("required_streak_days", 0).coerceAtLeast(0),
                                pokemonName = obj.optString("pokemon_name", "").trim(),
                                pokemonSubtitle = obj.optString("pokemon_subtitle", "").trim(),
                                rarity = obj.optString("rarity", "").trim()
                            )
                        )
                    }
                }
            }

            val foods = mutableListOf<FoodPer100gProfile>()
            val foodsArr = root.optJSONArray("food_database_patches")
            if (foodsArr != null) {
                for (i in 0 until foodsArr.length()) {
                    val f = foodsArr.optJSONObject(i) ?: continue
                    val disp = f.optString("display_name", "").trim()
                    if (disp.isBlank()) continue
                    val kwArr = f.optJSONArray("keywords")
                    val kws = if (kwArr != null) {
                        (0 until kwArr.length()).mapNotNull { idx ->
                            kwArr.optString(idx, "").trim().takeIf { it.isNotBlank() }
                        }
                    } else {
                        listOf(disp.lowercase())
                    }
                    foods.add(
                        FoodPer100gProfile(
                            keywords = kws.ifEmpty { listOf(disp.lowercase()) },
                            displayName = disp,
                            description = f.optString("description", disp).trim(),
                            caloriesPer100g = f.optInt("calories_per_100g", 100).coerceIn(0, 1200),
                            carbsPer100g = f.optDouble("carbs_per_100g", 10.0).coerceIn(0.0, 100.0),
                            fatsPer100g = f.optDouble("fats_per_100g", 2.0).coerceIn(0.0, 100.0),
                            proteinPer100g = f.optDouble("protein_per_100g", 3.0).coerceIn(0.0, 100.0),
                            fiberPer100g = f.optDouble("fiber_per_100g", 1.0).coerceIn(0.0, 100.0),
                            micronutrients = f.optString("micronutrients", "Vitamins & Minerals").trim(),
                            pairingTip = f.optString("pairing_tip", "").trim(),
                            gramsPerUnit = f.optInt("grams_per_unit", 100).coerceIn(1, 2000),
                            unitNameSingular = f.optString("unit_name_singular", "serving").trim().ifBlank { "serving" },
                            unitNamePlural = f.optString("unit_name_plural", "servings").trim().ifBlank { "servings" },
                            isLiquidMl = f.optBoolean("is_liquid_ml", false)
                        )
                    )
                }
            }

            val extraMsgs = mutableMapOf<String, List<String>>()
            val msgObj = root.optJSONObject("extra_reminder_messages")
            if (msgObj != null) {
                val keys = msgObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = msgObj.optJSONArray(key) ?: continue
                    val list = (0 until arr.length()).mapNotNull { idx ->
                        arr.optString(idx, "").trim().takeIf { it.isNotBlank() }
                    }
                    if (list.isNotEmpty()) {
                        extraMsgs[key.trim().uppercase()] = list
                    }
                }
            }

            val manifest = OtaPatchManifest(
                patchNumber = patchNum,
                patchVersion = patchVer,
                baseAppVersion = baseAppVer,
                minNativeVersionCode = minNativeCode,
                releaseTitle = releaseTitle.ifBlank { "PikaFit OTA Patch v$patchVer" },
                patchNotes = patchNotes,
                announcementBanner = announcement,
                publishedAt = publishedAt,
                xpPerLevel = xpPerLevel,
                streakRestoreCostXp = restoreCost,
                classTiers = tierOverrides,
                patchedFoodCount = foods.size,
                sourceLabel = sourceLabel,
                appliedAtMillis = appliedAtMillis
            )
            ParsedPatchPayload(
                manifest = manifest,
                foodPatches = foods,
                extraReminderMessages = extraMsgs,
                rawJson = trimmed
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing OTA patch manifest: ${e.message}")
            null
        }
    }

    fun applyParsedPayload(payload: ParsedPatchPayload) {
        TrainerRankProgression.applyOtaPatchRules(
            xpPerLevelOverride = payload.manifest.xpPerLevel,
            streakRestoreCostOverride = payload.manifest.streakRestoreCostXp,
            tierOverrides = payload.manifest.classTiers
        )
        if (payload.foodPatches.isNotEmpty()) {
            SmartFoodNutritionEngine.applyOtaFoodPatches(payload.foodPatches)
        }
        if (payload.extraReminderMessages.isNotEmpty()) {
            PikaFitNotificationScheduler.applyOtaExtraMessages(payload.extraReminderMessages)
        }
    }

    fun getInstalledPatch(context: Context?): OtaPatchManifest {
        if (context == null) return defaultBuiltInManifest()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_CACHED_PATCH_JSON, null)
        if (cachedJson.isNullOrBlank()) return defaultBuiltInManifest()
        val source = prefs.getString(KEY_CACHED_PATCH_SOURCE, "Cached OTA") ?: "Cached OTA"
        val appliedAt = prefs.getLong(KEY_CACHED_PATCH_APPLIED_AT, 0L)
        return parsePatchPayload(cachedJson, source, appliedAt)?.manifest ?: defaultBuiltInManifest()
    }

    fun loadAndApplyCachedPatch(context: Context?): OtaPatchManifest {
        if (context == null) return defaultBuiltInManifest()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_CACHED_PATCH_JSON, null) ?: return defaultBuiltInManifest()
        val source = prefs.getString(KEY_CACHED_PATCH_SOURCE, "Cached OTA") ?: "Cached OTA"
        val appliedAt = prefs.getLong(KEY_CACHED_PATCH_APPLIED_AT, 0L)
        val payload = parsePatchPayload(cachedJson, source, appliedAt) ?: return defaultBuiltInManifest()
        applyParsedPayload(payload)
        return payload.manifest
    }

    fun saveAndApplyRawJson(
        context: Context?,
        rawJson: String,
        sourceLabel: String
    ): PatchApplyResult? {
        val now = System.currentTimeMillis()
        val payload = parsePatchPayload(rawJson, sourceLabel, now) ?: return null
        val previous = getInstalledPatch(context)
        applyParsedPayload(payload)
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putString(KEY_CACHED_PATCH_JSON, payload.rawJson)
            putString(KEY_CACHED_PATCH_SOURCE, sourceLabel)
            putLong(KEY_CACHED_PATCH_APPLIED_AT, now)
            apply()
        }
        val prevCachedJson = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString(KEY_CACHED_PATCH_JSON, null)
        val isNewer = payload.manifest.patchNumber > previous.patchNumber ||
            payload.manifest.patchVersion != previous.patchVersion ||
            prevCachedJson.isNullOrBlank()
        return PatchApplyResult(
            manifest = payload.manifest,
            newlyApplied = isNewer,
            appliedFromRemote = true
        )
    }

    /**
     * Fetches declarative patch_manifest.json from GitHub (main branch + releases JSON asset)
     * and merges with optional Firestore system/app_patch JSON. Automatically applies newest patch!
     */
    fun fetchAndApplyPatchFromGitHub(
        context: Context?,
        firestorePatchJson: String? = null
    ): PatchApplyResult {
        val currentCachedRaw = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString(KEY_CACHED_PATCH_JSON, null)
        val currentInstalled = loadAndApplyCachedPatch(context)

        val candidates = mutableListOf<ParsedPatchPayload>()
        val now = System.currentTimeMillis()

        // 1. GitHub Raw main/patch_manifest.json (cache-busted)
        fetchHttpString("$GITHUB_RAW_MANIFEST_URL?cb=$now", acceptHeader = "application/json")?.let { raw ->
            parsePatchPayload(raw, "GitHub OTA (main)", now)?.let { candidates.add(it) }
        }

        // 2. GitHub Contents API raw fallback if CDN is stale
        if (candidates.isEmpty()) {
            fetchHttpString(GITHUB_CONTENTS_MANIFEST_URL, acceptHeader = "application/vnd.github.raw+json")?.let { raw ->
                parsePatchPayload(raw, "GitHub OTA (Contents)", now)?.let { candidates.add(it) }
            }
        }

        // 3. GitHub Releases latest JSON patch asset (if attached to a release tag)
        fetchPatchAssetFromLatestRelease(now)?.let { candidates.add(it) }

        // 4. Optional Firestore patch document JSON
        if (!firestorePatchJson.isNullOrBlank()) {
            parsePatchPayload(firestorePatchJson, "Cloud Patch Sync", now)?.let { candidates.add(it) }
        }

        val bestRemote = candidates.maxByOrNull { it.manifest.patchNumber }
        if (bestRemote != null) {
            val isUpdatedPayload = bestRemote.manifest.patchNumber > currentInstalled.patchNumber ||
                bestRemote.manifest.patchVersion != currentInstalled.patchVersion ||
                currentCachedRaw?.trim() != bestRemote.rawJson.trim()

            applyParsedPayload(bestRemote)
            context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
                putString(KEY_CACHED_PATCH_JSON, bestRemote.rawJson)
                putString(KEY_CACHED_PATCH_SOURCE, bestRemote.manifest.sourceLabel)
                putLong(KEY_CACHED_PATCH_APPLIED_AT, now)
                apply()
            }
            return PatchApplyResult(
                manifest = bestRemote.manifest,
                newlyApplied = isUpdatedPayload,
                appliedFromRemote = true
            )
        }

        return PatchApplyResult(
            manifest = currentInstalled,
            newlyApplied = false,
            appliedFromRemote = false
        )
    }

    private fun fetchPatchAssetFromLatestRelease(now: Long): ParsedPatchPayload? {
        val releaseJson = fetchHttpString(GITHUB_RELEASES_LATEST_URL, acceptHeader = "application/vnd.github+json")
            ?: return null
        return try {
            val obj = JSONObject(releaseJson)
            val assets: JSONArray = obj.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "").lowercase()
                val dlUrl = asset.optString("browser_download_url", "")
                if (name.startsWith("patch") && name.endsWith(".json") && dlUrl.isNotBlank()) {
                    val raw = fetchHttpString(dlUrl, acceptHeader = "application/json")
                    if (!raw.isNullOrBlank()) {
                        parsePatchPayload(raw, "GitHub Release Patch", now)?.let { return it }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchHttpString(urlStr: String, acceptHeader: String): String? {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6500
                readTimeout = 6500
                setRequestProperty("Accept", acceptHeader)
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "PikafitPro-OTA-Patcher")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "OTA fetch skipped for $urlStr: ${e.message}")
            null
        }
    }

    fun defaultBuiltInManifest(): OtaPatchManifest = OtaPatchManifest(
        patchNumber = 10301L,
        patchVersion = "1.0.3-patch.1",
        baseAppVersion = "1.0.2",
        minNativeVersionCode = 2L,
        releaseTitle = "PikaFit • Streak Classes & Heatmap Patch",
        patchNotes = "Streak-oriented Pokémon Classes (0–500 days), continuous XP Leveling, 1,000 XP 1-day Streak Restore, dedicated Streaks Heatmap tab, and zero-redirect GitHub OTA patching.",
        announcementBanner = "PikaFit OTA Patcher Active — Automatic GitHub JSON patch delivery enabled.",
        xpPerLevel = TrainerRankProgression.xpPerLevel,
        streakRestoreCostXp = TrainerRankProgression.streakRestoreCostXp,
        patchedFoodCount = SmartFoodNutritionEngine.getPatchedFoodCount(),
        sourceLabel = "Built-in Baseline"
    )
}
