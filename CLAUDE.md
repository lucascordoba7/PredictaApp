# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```shell
# Build debug APK
.\gradlew assembleDebug

# Unit tests (JVM, no device needed)
.\gradlew test

# Run a single unit test class
.\gradlew test --tests "com.lucas.predictaapp.ExampleUnitTest"

# Run a single unit test method
.\gradlew test --tests "com.lucas.predictaapp.ExampleUnitTest.addition_isCorrect"

# Instrumented tests (requires connected device/emulator)
.\gradlew connectedAndroidTest
```

## Key Versions

- AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2025.02.00
- compileSdk = 35, targetSdk = 35, minSdk = 26
- Java 11 bytecode target

## Architecture

Single-module project (`:app`), Kotlin-only, no Java. No DI framework, no ViewModels.

**Entry point:** `MainActivity` → `PredictaApp()` composable (in `MainActivity.kt`) wraps a `Scaffold` with a `BottomNavigationBar` and `PredictaNavGraph`.

**Navigation:** `ui/navigation/` — `Screen` sealed class defines routes. `bottomNavScreens` drives which routes show the bottom bar. Four active destinations: `Dashboard`, `Permito`, `Chat`, `Profile`. `Subscriptions` and `Notifications` are defined but not yet wired into the nav graph.

**Feature screens:** `features/{dashboard,permito,chat,profile}/` — currently all stubs. Each will grow its own composables in that package.

**Data layer:**
- `data/model/` — `@Serializable` data classes: `Expense`, `Subscription`, `Notification`, `User`, `Fixtures` (static sample data seeded into repositories).
- `data/repository/ExpensesRepository` — in-memory `object` singleton backed by `MutableStateFlow<List<Expense>>`.
- `data/remote/ApiProvider` — singleton `object` that lazily builds two Retrofit clients: `anthropicApi` (Claude) and `openAiApi`. API keys are read from `local.properties` and exposed via `BuildConfig.ANTHROPIC_API_KEY` / `BuildConfig.OPENAI_API_KEY`.

**Design system** (do not use Material3 theme tokens directly — use these instead):
- `PredictaColors` — dark palette (charcoal background, amber accent, cream text).
- `PredictaTypography` — IBM Plex Sans (regular/medium/semibold/bold) + IBM Plex Mono. Named scales: `scoreHero`, `titlePage`, `section`, `cardTitle`, `body`, `bodyTight`, `small`, `caption`, `monoCap`, `kpiInline`.
- `PredictaDimensions` — `Spacing`, `Radius`, `Heights` objects with named dp values.

## Version Catalog

All dependency coordinates live in `gradle/libs.versions.toml`. Always reference them via catalog aliases (e.g. `libs.androidx.core.ktx`). Never hardcode group/artifact/version strings in build scripts.

## API Keys

Add to `local.properties` (not committed):
```
ANTHROPIC_API_KEY=sk-ant-...
OPENAI_API_KEY=sk-...
```

## Conventions

- Kotlin code style: `official` (set in `gradle.properties`).
- Package root: `com.lucas.predictaapp`.
- No CI/CD pipeline configured.
- Unit tests in `app/src/test/` use JUnit 4. Instrumented tests in `app/src/androidTest/` use `AndroidJUnit4` runner; Compose UI test deps are declared but not yet used.
