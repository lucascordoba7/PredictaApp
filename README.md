# PredictaApp

Android app for personal finance management with AI-powered insights. Built with Kotlin + Jetpack Compose.

## Quickstart

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 11+
- Android SDK with API 35
- A physical device or emulator running Android 8.0+ (API 26+)

### 1. Clone and open

```bash
git clone https://github.com/lucascordoba77/PredictaApp.git
cd PredictaApp
```

Open the project in Android Studio (`File → Open`).

### 2. Configure API keys

Create a `local.properties` file in the project root (next to `gradle.properties`) and add your keys:

```properties
sdk.dir=/path/to/your/Android/Sdk

ANTHROPIC_API_KEY=sk-ant-...
OPENAI_API_KEY=sk-...
GROQ_API_KEY=gsk_...
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

> `local.properties` is gitignored — never commit it.

### 3. Build and run

```bash
# Sync Gradle, then run from Android Studio (Shift+F10)
# Or from the terminal:
.\gradlew assembleDebug
```

Install on a connected device:

```bash
.\gradlew installDebug
```

### 4. Run tests

```bash
# Unit tests (no device needed)
.\gradlew test

# Instrumented tests (requires connected device/emulator)
.\gradlew connectedAndroidTest
```

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose (BOM 2025.02.00) |
| Navigation | Navigation Compose |
| Local DB | Room |
| Networking | Retrofit + OkHttp |
| Serialization | kotlinx.serialization |
| Preferences | DataStore |
| Image loading | Coil |
| Backend | Supabase |
| AI APIs | Anthropic (Claude), OpenAI, Groq |

## Project structure

```
app/src/main/java/com/lucas/predictaapp/
├── data/
│   ├── local/        # Room database, DAOs
│   ├── model/        # Data classes
│   ├── remote/       # Retrofit API clients
│   └── repository/   # Repositories
├── features/
│   ├── dashboard/
│   ├── permito/
│   ├── chat/
│   ├── profile/
│   ├── subscriptions/
│   ├── notifications/
│   ├── onboarding/
│   └── quickactions/
└── ui/
    ├── navigation/   # NavGraph, Routes, BottomNav
    └── theme/        # PredictaColors, PredictaTypography, PredictaDimensions
```

## Design system

Do not use Material3 theme tokens directly. Use the Predicta design system instead:

- **`PredictaColors`** — dark palette (charcoal background, amber accent, cream text)
- **`PredictaTypography`** — IBM Plex Sans + IBM Plex Mono with named scales (`scoreHero`, `titlePage`, `cardTitle`, `body`, etc.)
- **`PredictaDimensions`** — named spacing, radius, and height values
