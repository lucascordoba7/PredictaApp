# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```shell
# Build debug APK (Windows: .\gradlew  · Linux/macOS: ./gradlew)
./gradlew assembleDebug

# Unit tests (JVM, no device needed)
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.lucas.predictaapp.ExampleUnitTest"

# Run a single unit test method
./gradlew test --tests "com.lucas.predictaapp.ExampleUnitTest.addition_isCorrect"

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## Key Versions

- AGP 8.7.3, Kotlin 2.1.0, KSP 2.1.0-1.0.29, Compose BOM 2025.02.00
- compileSdk = 35, targetSdk = 35, minSdk = 26
- Java 11 bytecode target
- Room 2.7.0, Navigation Compose 2.8.8, DataStore 1.1.2
- Retrofit 2.11.0, OkHttp 4.12.0, Ktor 3.0.3 (transport para Supabase)
- Supabase 3.0.2 (postgrest-kt), kotlinx.serialization 1.7.3

## Architecture

Single-module project (`:app`), Kotlin-only, no Java. **No DI framework, no ViewModels** — todo el estado vive en `mutableStateOf` dentro de los composables (deuda explícita, ver `PLAN.md` PTA-018).

**Entry point:** `MainActivity` → `PredictaApp()` composable (en `PredictaApp.kt`) envuelve un `Scaffold` con `BottomNavigationBar` + `PredictaNavGraph`.

**Navigation:** `ui/navigation/` — `Screen` sealed class define rutas. `bottomNavScreens` decide qué destinos muestran la bottom bar.
- Root tabs (con bottom bar): `Dashboard`, `Transactions` ("Actividad"), `Chat`, `Profile`.
- Detail screens (push): `Notifications`, `Subscriptions`, `Categories`, `FixedExpenses`.
- Transiciones del NavGraph: fade entre tabs raíz, slide horizontal + fade para detail screens (`NavGraph.kt`).

**Feature screens** (todas implementadas, no stubs): `features/{dashboard,transactions,chat,profile,onboarding,fixedexpenses,notifications,subscriptions,quickactions}/`. La feature Permito fue eliminada. Cada feature agrupa sus composables y, cuando aplica, una subcarpeta `components/` con las cards reutilizables (ver `features/dashboard/components/`).

**Entry points de registro (además del chat):** `LaunchActions` (package root) traduce los Intents del widget (`widget/PredictaWidgetProvider` + `res/layout/widget_predicta.xml`), los app shortcuts (`res/xml/shortcuts.xml`) y el share target (`ACTION_SEND` texto/imagen en `MainActivity`) a acciones que consume la UI: el scaffold abre la carga manual (`features/quickactions/ManualExpenseEntry`) y el chat resuelve voz (`features/chat/VoiceInput`), escaneo de ticket (`TicketImage` + `ChatRepository.extractFromTicket`, Claude Haiku con visión) y contenido compartido. `ExpensesRepository.findSameDayDuplicate` es la heurística anti doble carga (mismo comercio+monto en el día → confirmación).

**Data layer:**
- `data/model/` — `@Serializable` data classes: `Expense`, `Subscription`, `Notification`, `FixedExpense`, `Category`, `ExpenseCategory`, `User`. `Fixtures` ya fue removido (PTA-007) — los repos hidratan desde Room directamente con empty states reales.
- `data/local/` — **Room** (`AppDatabase`) con DAOs: `CategoryDao`, `ExpenseDao`, `FixedExpenseDao`, `NotificationDao`, `SubscriptionDao`. `Converters` para tipos custom. `UserPreferencesRepository` usa **DataStore Preferences** para perfil (nombre/email/ingreso) y flags de onboarding.
- `data/repository/` — repos por dominio: `CategoryRepository`, `ChatRepository`, `ExpensesRepository`, `FixedExpensesRepository`, `NotificationsRepository`, `PersonalityRepository`, `SubscriptionsRepository`. Exponen `StateFlow`/`Flow`; suspend functions para mutaciones.
- `data/remote/` — `ApiProvider` (singleton) construye lazy: `anthropicApi` (Claude), `openAiApi`, `GroqApi`. `SupabaseProvider` para el cliente Supabase (postgrest + Ktor android). Claves vía `BuildConfig`.

**UI shared components:** `ui/components/`
- `AnimatedAmount` — `AnimatedContent` con slide vertical + fade para montos que cambian.
- `PredictaPullRefresh` — wrapper `PullToRefreshBox` themed (acento sobre card). Acepta `onRefresh` opcional; si es null igual da feedback ~700ms.
- `TransactionRow` + `DeleteExpenseDialog` — fila de transacción y confirmación de borrado compartidas entre la home y la pantalla Transacciones (`contentPadding` configurable).

**Design system — "Atardecer"** (rediseño 2026-11, claro + oscuro siguiendo al sistema; do not use Material3 theme tokens directly — use these instead):
- `PredictaColors` — lee la paleta activa (`LightPalette` / `DarkPalette`, data class `PredictaPalette`), que `PredictaTheme` fija según `isSystemInDarkTheme()`. Es estado de snapshot: sirve también en `drawBehind` y lambdas. Nunca hardcodear `Color.White` para fondos (usar `glass`, `card`…); blanco solo sobre la hero o el degradé. Roles: `paper` (fondo), `card`, `ink`/`inkSecondary`/`inkTertiary`, `kicker`, `accent`/`accentSoft`/`onAccent` (terracota), `heroStart→heroMid→heroEnd` (degradé de la hero card), `honey`/`onHoney` (ámbar del sol), `success`/`danger`/`warn` + `*Soft`. Brushes: `pageWash`, `sheetWash`, `heroBrush`, `primaryBrush`. Los alias viejos (`charcoal`, `cream*`, `amber*`…) ya no existen.
- `PredictaTypography` — Plus Jakarta Sans variable autoalojada (`res/font/plus_jakarta_sans.ttf`, OFL en `licenses/`). Escala: `display`, `title`, `heading`, `rowTitle`, `rowSubtitle`, `rowAmount`, `kicker`, `label`, `button`. Quedan estilos viejos (`body`, `small`, `caption`, `cardTitle`…) usados como base de `.copy()`.
- `PredictaDimensions` — `Spacing`, `Radius` (`badge` 14, `card` 24, `hero` 28), `Heights`.
- `CategoryColors` — paleta para chips/avatares de categorías.
- `ui/components/Surfaces.kt` — piezas del lenguaje visual: `Modifier.pageWash()`, `HeroCard` (único bloque grande de color), `PredictaCard` (tarjeta con `softShadow`), `GlyphBadge`, `Kicker`, `SectionHeader`, `StatusPill`, `SunMark` (la marca: sol en el horizonte sobre el degradé; mismo dibujo que el ícono de la app, el widget y `ic_stat_predicta`).
- `ui/components/Controls.kt` — controles compartidos: `ScreenTopBar` (con `onBack` es el de detalle), `HeaderIconButton`, `PrimaryButton`/`SecondaryButton`, `ChoiceChip`, `SegmentedToggle`, `SearchField`, `PredictaTextField`, `PickerRow`, `EmptyState`, `PredictaSheet` + `SheetHeader`, `PredictaDialog`, `RowDivider`. No usar `OutlinedTextField`, `AlertDialog` ni `ModalBottomSheet` de Material directo.
- Patrón de pantalla: `pageWash` de fondo, padding lateral 20dp, `ScreenTopBar`, una `HeroCard` como resumen y el contenido en `PredictaCard` agrupadas bajo un `Kicker`. Tocar una fila edita; mantener apretado borra.
- Diseño de referencia (Claude Design): https://claude.ai/artifact/VtQ9NX2msz3d1USwkmkevw — todas las pantallas migradas (tabs, detalle, sheets y onboarding).

## Version Catalog

All dependency coordinates live in `gradle/libs.versions.toml`. Always reference them via catalog aliases (e.g. `libs.androidx.core.ktx`). Never hardcode group/artifact/version strings in build scripts.

## API Keys

Add to `local.properties` (not committed):
```
ANTHROPIC_API_KEY=sk-ant-...
OPENAI_API_KEY=sk-...
GROQ_API_KEY=gsk_...
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
```

## Conventions

- Kotlin code style: `official` (set in `gradle.properties`).
- Package root: `com.lucas.predictaapp`.
- No CI/CD pipeline configured.
- No crash reporting integrated (decisión abierta — ver `PLAN.md` §5).
- `isMinifyEnabled = false` en release — bloqueante para publicar (PTA-020).
- Unit tests en `app/src/test/` con JUnit 4 (solo el `Example` por ahora). Instrumented en `app/src/androidTest/` con `AndroidJUnit4`; deps de Compose UI test declaradas pero sin uso.
- Copy en español rioplatense (target Argentina). Montos formateados con `fmtArs()` (separador de miles `.`).

## Patrones recurrentes

- **LazyColumn con listas mutables**: usar `items(list, key = { it.id })` + `Modifier.animateItem()` para inserción/eliminación suave.
- **Pantallas con listas**: envolver en `PredictaPullRefresh` para que el gesto de pull-to-refresh sea consistente.
- **Montos en UI**: cuando el valor cambia en respuesta a acciones del usuario, usar `AnimatedAmount` en lugar de `Text` plano.
- **Errores de Supabase**: actualmente hay catches silenciosos (PTA-008). Al agregar logging, mantener el patrón en todos los repos.
- **Empty states**: nunca volver a Fixtures hardcoded. El repo emite lista vacía y la UI muestra el placeholder correspondiente.
