# AGENTS.md

## Build & Test Commands

```shell
# Build debug APK
.\gradlew assembleDebug

# Unit tests (JVM, no device needed)
.\gradlew test

# Instrumented tests (requires connected device/emulator)
.\gradlew connectedAndroidTest

# Run a single unit test class
.\gradlew test --tests "com.lucas.predictaapp.ExampleUnitTest"

# Run a single unit test method
.\gradlew test --tests "com.lucas.predictaapp.ExampleUnitTest.addition_isCorrect"
```

## Architecture

- Single-module project (`:app`) — no feature modules or libraries.
- `com.android.application` + `kotlin.android` + `kotlin.compose` + `kotlin.serialization` plugins.
- Kotlin-only source (no Java). Bytecode targets Java 11.
- Jetpack Compose with Material 3. `ComponentActivity` + `setContent {}` with `enableEdgeToEdge()`.
- Navigation: Compose Navigation (`NavHost`) with 4 screens: Dashboard (start), Permito, Chat, Profile. Bottom nav bar shown only on bottom-nav screens.
- Data layer: `data/remote/` (Retrofit APIs for Groq, OpenAI, Anthropic), `data/repository/` (PermitoRepository, ExpensesRepository), `data/model/` (data classes).
- API keys loaded from `local.properties` at build time → injected as `BuildConfig` fields. **Never commit API keys.**
- Persistence: DataStore Preferences declared.
- Image loading: Coil 3 with OkHttp network layer.
- Video: Media3 ExoPlayer declared.
- Permissions: Accompanist Permissions declared.
- No DI framework. No ViewModels yet (state managed in composables).

## Version Catalog

All dependency coordinates are in `gradle/libs.versions.toml`. Always reference
dependencies via the catalog aliases (e.g. `libs.androidx.core.ktx`), never hardcode
group/artifact/version strings in build scripts.

## Key Versions

- compileSdk = 35, targetSdk = 35, minSdk = 26
- AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2025.02.00

## Source Layout

```
app/src/main/java/com/lucas/predictaapp/
  MainActivity.kt          — entry point, PredictaAppScaffold with nav + bottom bar
  PredictaApp.kt           — Application class (empty)
  features/                — feature screens (dashboard, permito, chat, profile)
  data/                    — remote APIs, repositories, model classes
  ui/
    navigation/            — NavGraph, BottomNav, Routes
    theme/                 — Theme, Color, Typography, Dimensions, Type
    utils/                 — NumberUtils, DateUtils
```

## Testing

- **Unit tests** in `app/src/test/` use JUnit 4 (no Mockito, no coroutine test libs).
- **Instrumented tests** in `app/src/androidTest/` use `AndroidJUnit4` runner.
  Compose UI test deps are declared but not yet used.
- The `test` task runs JVM unit tests (fast, no device). `connectedAndroidTest`
  requires a device/emulator and runs instrumented tests.

## Conventions

- Kotlin code style: `official` (set in `gradle.properties`).
- Package: `com.lucas.predictaapp`.
- Gradle daemon JVM: `-Xmx2048m -Dfile.encoding=UTF-8`.
- No CI/CD pipeline configured.
- No README exists.
