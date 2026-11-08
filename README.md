# ⚡ PikaFit • RPG Hunter Fitness, Nutrition & Progression Companion

[![Android Build CI](https://github.com/tmcrainbow093/PikaFit/actions/workflows/main.yml/badge.svg)](https://github.com/tmcrainbow093/PikaFit/actions)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Cloud%20Sync-Firebase%20Firestore%20%26%20Auth-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Level Up Your Real-Life Fitness.** Turn your daily nutrition, hydration, and progressive overload workouts into an electrifying RPG adventure with Pokémon class ranks, AI diet advice, and biometric telemetry.

---

## 📱 Feature Highlights

### ⚡ 1. Dynamic Hunter Dashboard
* **Dynamic Time Greetings**: Context-aware greetings that adapt to your exact local time (**Good Morning**, **Good Afternoon**, **Good Evening**, and **Good Night**).
* **Live HP & XP Telemetry**: Real-time Level, Rank, XP bar to next tier, and Daily Streak tracker.
* **Buffs & Debuffs**: Automatically calculates buffs like *Well-Fed (+50 XP)*, *Hydrated*, and *Hypertrophy Pump*.

### 🍽️ 2. Smart Food & Quantity-to-Macro Converter
* **Unit or Gram Inputs**: Input foods in standard grams (`150g Chicken Breast`, `200g Cooked Rice`) **or** discrete item counts (`3 Dosa 4 Idly`, `2 Paratha`, `1 Banana`).
* **Instant Macro Extraction**: Real-time breakdown of Calories, Protein, Carbohydrates, Healthy Fats, Fiber, and Omega-3.
* **Macronutrient Tracking**: Live progress rings toward your personalized daily protein and caloric targets.

### 🏋️ 3. Workout & Progressive Overload Tracker
* **Exercise Logging**: Log sets, reps, and weights for multi-exercise routines.
* **Calorie Burn Engine**: Accurately computes active workout calories burned based on exercise intensity, volume, and sets.
* **Post-Workout Diet Directives**: Instant tactical advice telling you whether to eat at a caloric surplus (for lean muscle bulk) or deficit (for fat loss) with targeted meal suggestions.

### 🤖 4. PikaBot • AI Nutritionist & Dietitian
* **Dedicated Gemini AI Chat Page**: Chat directly with your smart pocket dietitian powered by Google Gemini.
* **Custom Diet Plans**: Ask for personalized meal plans tailored to your exact height, weight, daily calorie target, and target physique.

### 📈 5. Biometric Charts & Body Composition Telemetry
* **Visual Telemetry Graphs**: Multi-day weight trendlines with visual target-weight goal overlays.
* **Hydration Counter**: Quick-tap water logging (`+150ml`, `+250ml`, `+350ml`, or custom ml/L).
* **Target Physique & Weight Goals**: Set and monitor your target weight and desired physique (*Lean Athletic*, *Muscle Bulk*, *Shredded Cut*, or *Endurance*).

### 🏆 6. 7-Tier Pokémon Progression Roadmap
Advance through 7 prestige ranks as you earn XP through consistency:
1. **Beginner** • *Pikachu* (0 – 1,500 XP)
2. **Challenger** • *Charizard* (1,500 – 3,000 XP)
3. **Leader** • *Greninja* (3,000 – 5,000 XP)
4. **Elite I** • *Lucario* (5,000 – 7,500 XP)
5. **Elite II** • *Mewtwo* (7,500 – 10,000 XP)
6. **Elite III** • *Yveltal* (10,000 – 15,000 XP)
7. **Champion** • *Arceus* (15,000+ XP)

### ⚙️ 7. Click-to-Go Settings Hub
* **Profile & Biometrics**: One-tap navigation to edit weight, height, age, target weight, and target physique.
* **Creator & Community Feedback Board**: Interactive feedback submission board with upvotes and status badges (*Under Review*, *Planned*, *In Progress*, *Implemented*).
* **Dark & Light Mode**: Seamless M3 dark/light theme switching.

---

## 📥 How to Install the App on Mobile

### Option A: Download from GitHub Actions (Automatic CI Build)
1. Go to the **Actions** tab in this GitHub repository.
2. Click on the latest workflow run on `main`.
3. Scroll down to **Artifacts** and download **`PikaFit-Debug-APK`**.
4. Unzip the file and install `app-debug.apk` on your Android phone.

### Option B: Build from Source
```bash
# Clone the repository
git clone https://github.com/tmcrainbow093/PikaFit.git
cd PikaFit

# Build the Debug APK using Gradle
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛠️ Tech Stack & Architecture

* **Architecture**: Clean Architecture + MVVM (Model-View-ViewModel) + Single Activity
* **Language**: 100% Kotlin with Coroutines & StateFlow
* **UI Framework**: Jetpack Compose with Material Design 3 (M3)
* **Design & Typography**: 
  - Headings & Big Stats: `Integral CF`
  - Trackers & Body UI: `SF Pro Semibold`
* **Cloud Database & Auth**: Google Cloud Firestore & Firebase Auth (Google Sign-In with Credential Manager)
* **AI & LLM**: Google Gemini API via Firebase AI Logic
* **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with GitHub Actions CI/CD

---

## 🔒 Security & Data Privacy

* Encrypted, zero-trust cloud Firestore security rules ensuring trainers only read and write their own telemetry documents.
* Google Sign-In powered by Android Jetpack `CredentialManager`.
* Offline-first local storage caching ensures smooth performance even without network connectivity.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
