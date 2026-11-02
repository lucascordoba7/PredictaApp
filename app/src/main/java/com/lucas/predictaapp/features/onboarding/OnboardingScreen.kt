package com.lucas.predictaapp.features.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.local.UserSetup
import com.lucas.predictaapp.data.repository.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.PieChart
import com.lucas.predictaapp.ui.components.DiceMark
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.ui.components.HeaderIconButton
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.PrimaryButton
import com.lucas.predictaapp.ui.components.pageWash
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.ThousandsVisualTransformation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Local type scale — sizes taken directly from onboarding.html CSS
private val typoHeroTitle   get() = PredictaTypography.titlePage.copy(fontSize = 28.sp, letterSpacing = (-0.4).sp)
private val tyroBrand       get() = PredictaTypography.titlePage.copy(fontSize = 22.sp, letterSpacing = (-0.3).sp)
private val typoScreenTitle get() = PredictaTypography.titlePage.copy(fontSize = 24.sp, letterSpacing = (-0.3).sp)
private val typoSubtitle    get() = PredictaTypography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)
private val typoCardTitle   get() = PredictaTypography.bodyTight.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
private val typoCardSub     get() = PredictaTypography.small.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium)
private val typoBtnLabel    get() = PredictaTypography.bodyTight.copy(fontSize = 16.sp)
private val typoFieldLabel  get() = PredictaTypography.small.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium)

private sealed class Step {
    object Welcome : Step()
    object Register : Step()
    object Setup : Step()
    object SignIn : Step()
}

private val SPEC_FWD = tween<androidx.compose.ui.unit.IntOffset>(420)
private val SPEC_BACK = tween<androidx.compose.ui.unit.IntOffset>(380)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableStateOf<Step>(Step.Welcome) }
    var goingForward by remember { mutableStateOf(true) }

    // Accumulated registration data
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pageWash()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val spec = if (goingForward) SPEC_FWD else SPEC_BACK
                val dir = if (goingForward) 1 else -1
                (slideInHorizontally(spec) { it * dir } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(spec) { -it * dir } + fadeOut(tween(220)))
            },
            label = "onboarding_step",
        ) { current ->
            when (current) {
                is Step.Welcome -> WelcomeContent(
                    onGetStarted = {
                        goingForward = true
                        step = Step.Register
                    },
                    onSignIn = {
                        goingForward = true
                        step = Step.SignIn
                    },
                )

                is Step.Register -> RegisterContent(
                    onBack = {
                        goingForward = false
                        step = Step.Welcome
                    },
                    onContinue = { name, email ->
                        regName = name
                        regEmail = email
                        goingForward = true
                        step = Step.Setup
                    },
                )

                is Step.Setup -> SetupContent(
                    regName = regName,
                    regEmail = regEmail,
                    onBack = {
                        goingForward = false
                        step = Step.Register
                    },
                    onFinish = onComplete,
                )

                is Step.SignIn -> SignInContent(
                    onBack = {
                        goingForward = false
                        step = Step.Welcome
                    },
                    onCreateAccount = {
                        goingForward = false
                        step = Step.Register
                    },
                    onSignedIn = onComplete,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// STEP 1 — Welcome
// ─────────────────────────────────────────────────────────
@Composable
private fun WelcomeContent(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(PredictaDimensions.Spacing.xxl))
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        // Hero — logo + brand
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            DiceMark(size = 56.dp)
            Column {
                Text(
                    text = "TUS GASTOS, CLAROS",
                    style = PredictaTypography.kicker,
                    color = PredictaColors.kicker,
                )
                Row {
                    Text(
                        text = "predicta",
                        style = tyroBrand,
                        color = PredictaColors.ink,
                    )
                    Text(
                        text = ".",
                        style = tyroBrand,
                        color = PredictaColors.accent,
                    )
                }
            }
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))

        Text(
            text = buildAnnotatedString {
                append("Tus finanzas,\nmás ")
                withStyle(SpanStyle(color = PredictaColors.accent, fontWeight = FontWeight.Bold)) {
                    append("claras")
                }
                append(" que nunca")
            },
            style = typoHeroTitle,
            color = PredictaColors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.sm))
        Text(
            text = "Registrá tus gastos al toque, descubrí patrones\nescondidos y decidí gastar sin culpa.",
            style = typoSubtitle,
            color = PredictaColors.inkSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        // Feature cards
        Column(
            verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FeatureCard(
                icon = Icons.Outlined.Bolt,
                title = "Cargá en segundos",
                subtitle = "Escribilo, dictalo o sacale una foto al ticket.",
            )
            FeatureCard(
                icon = Icons.Outlined.PieChart,
                title = "Mirá en qué se va",
                subtitle = "Cuánto gastaste en el mes y en qué, de un vistazo.",
            )
            FeatureCard(
                icon = Icons.Outlined.EventRepeat,
                title = "Nada se te pasa",
                subtitle = "Gastos fijos y suscripciones con su día de vencimiento.",
            )
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AmberButton(label = "Registrar mis gastos", onClick = onGetStarted)

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text(
            text = "Ya tengo cuenta",
            style = PredictaTypography.body,
            color = PredictaColors.accent,
            modifier = Modifier
                .clickable(onClick = onSignIn)
                .padding(horizontal = PredictaDimensions.Spacing.base, vertical = PredictaDimensions.Spacing.md),
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        // Step dots
        StepDots(totalSteps = 3, activeStep = 0)

        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
    }
}

@Composable
private fun FeatureCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    PredictaCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlyphBadge(size = 40.dp) {
                Icon(icon, contentDescription = null, tint = PredictaColors.accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = PredictaTypography.rowTitle.copy(fontSize = 15.sp), color = PredictaColors.ink)
                Text(subtitle, style = PredictaTypography.rowSubtitle, color = PredictaColors.inkSecondary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// STEP 2 — Register
// ─────────────────────────────────────────────────────────
@Composable
private fun RegisterContent(
    onBack: () -> Unit,
    onContinue: (name: String, email: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var takenError by remember { mutableStateOf(false) }
    var offlineError by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        nameError = name.isBlank()
        emailError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        return !nameError && !emailError
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
    ) {
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
        BackButton(onClick = onBack)
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            DiceMark(size = 36.dp)
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text("Creá tu cuenta", style = typoScreenTitle, color = PredictaColors.ink)
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text(
            "Ingresá tus datos para empezar a tomar\ncontrol de tus finanzas.",
            style = typoSubtitle,
            color = PredictaColors.inkSecondary,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        FieldLabel("Tu nombre")
        OnboardingTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            placeholder = "Ej: Vale",
            isError = nameError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )
        if (nameError) FieldError("Ingresá tu nombre")
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        FieldLabel("Email")
        OnboardingTextField(
            value = email,
            onValueChange = { email = it; emailError = false; takenError = false; offlineError = false },
            placeholder = "tu@email.com",
            isError = emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
        )
        when {
            emailError -> FieldError("Ingresá un email válido")
            takenError -> FieldError("Ya existe una cuenta con ese email. Entrá en vez de registrarte.")
            offlineError -> FieldError("Sin conexión: no podemos crear la cuenta ahora")
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AmberButton(label = if (checking) "Verificando…" else "Continuar", enabled = !checking) {
            if (!validate()) return@AmberButton
            checking = true
            takenError = false
            offlineError = false
            scope.launch {
                // Cortamos acá si el email ya tiene cuenta: registrarse nunca debe pisar
                // los datos de una cuenta existente.
                when (UserPreferencesRepository.emailAvailable(email.trim())) {
                    UserPreferencesRepository.AuthResult.Ok -> onContinue(name.trim(), email.trim())
                    UserPreferencesRepository.AuthResult.AlreadyExists -> {
                        checking = false
                        takenError = true
                    }
                    else -> {
                        checking = false
                        offlineError = true
                    }
                }
            }
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))

        Text(
            text = "Al continuar aceptás los Términos y Condiciones y la Política de Privacidad.",
            style = PredictaTypography.caption,
            color = PredictaColors.inkTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
        StepDots(totalSteps = 3, activeStep = 1)
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
    }
}

// ─────────────────────────────────────────────────────────
// STEP 3 — Financial Setup
// ─────────────────────────────────────────────────────────
@Composable
private fun SetupContent(
    regName: String,
    regEmail: String,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var incomeText by remember { mutableStateOf("") }
    var paydayText by remember { mutableStateOf("") }
    var incomeError by remember { mutableStateOf(false) }
    var paydayError by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var registerError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        incomeError = incomeText.toIntOrNull() == null || incomeText.toInt() <= 0
        val day = paydayText.toIntOrNull()
        paydayError = day == null || day !in 1..31
        return !incomeError && !paydayError
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
    ) {
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
        BackButton(onClick = onBack)
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text(
            text = "Hola, ${regName.split(" ").first()} 👋",
            style = PredictaTypography.label,
            color = PredictaColors.accent,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text("Configurá tu plata", style = typoScreenTitle, color = PredictaColors.ink)
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text(
            "Estos datos ayudan a Predicta a darte respuestas precisas.",
            style = typoSubtitle,
            color = PredictaColors.inkSecondary,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        FieldLabel("¿Cuánto cobrás por mes? (ARS)")
        OnboardingTextField(
            value = incomeText,
            onValueChange = { incomeText = it.filter { c -> c.isDigit() }; incomeError = false },
            placeholder = "900.000",
            isError = incomeError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            visualTransformation = ThousandsVisualTransformation,
            prefix = "$",
        )
        if (incomeError) FieldError("Ingresá un monto válido")
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        FieldLabel("¿Qué día del mes cobrás?")
        OnboardingTextField(
            value = paydayText,
            onValueChange = { v ->
                if (v.length <= 2 && v.all { it.isDigit() }) {
                    paydayText = v; paydayError = false
                }
            },
            placeholder = "25",
            isError = paydayError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        )
        if (paydayError) FieldError("Ingresá un día del 1 al 31")
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AmberButton(label = if (creating) "Creando tu cuenta…" else "¡Listo! Empezar", enabled = !creating) {
            if (!validate()) return@AmberButton
            val income = incomeText.toIntOrNull() ?: 0
            val payday = paydayText.toIntOrNull() ?: 1
            creating = true
            registerError = false
            scope.launch {
                val setup = UserSetup(
                    name = regName,
                    email = regEmail,
                    income = income,
                    paydayDay = payday,
                    fixedMonthly = 0,
                )
                when (UserPreferencesRepository.register(context, setup)) {
                    UserPreferencesRepository.AuthResult.Ok -> {
                        // Cuenta nueva: no hay nada que bajar, pero sí hay que sembrar
                        // y subir sus categorías iniciales.
                        withContext(Dispatchers.IO) {
                            SyncManager.hydrateForCurrentAccount(context)
                        }
                        onFinish()
                    }
                    else -> {
                        creating = false
                        registerError = true
                    }
                }
            }
        }
        if (registerError) {
            FieldError("No pudimos crear la cuenta. Revisá la conexión y probá de nuevo.")
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
        StepDots(totalSteps = 3, activeStep = 2)
        Spacer(Modifier.height(PredictaDimensions.Spacing.xxl))
    }
}

// ─────────────────────────────────────────────────────────
// STEP — Sign In
// ─────────────────────────────────────────────────────────
@Composable
private fun SignInContent(
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignedIn: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var notFoundError by remember { mutableStateOf(false) }
    var offlineError by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
    ) {
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
        BackButton(onClick = onBack)
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            DiceMark(size = 36.dp)
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text("Bienvenido de nuevo", style = typoScreenTitle, color = PredictaColors.ink)
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text(
            "Ingresá el email con el que te registraste en este dispositivo.",
            style = typoSubtitle,
            color = PredictaColors.inkSecondary,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        FieldLabel("Email")
        OnboardingTextField(
            value = email,
            onValueChange = { email = it; emailError = false; notFoundError = false; offlineError = false },
            placeholder = "tu@email.com",
            isError = emailError || notFoundError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
        )
        when {
            emailError -> FieldError("Ingresá un email válido")
            notFoundError -> FieldError("No encontramos ninguna cuenta con ese email")
            offlineError -> FieldError("Sin conexión: no podemos verificar tu cuenta ahora")
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AmberButton(
            label = if (loading) "Verificando…" else "Entrar",
            enabled = !loading,
        ) {
            emailError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            if (emailError) return@AmberButton
            loading = true
            offlineError = false
            notFoundError = false
            scope.launch {
                // Contra Supabase, no contra el DataStore local: así entrar desde un
                // dispositivo nuevo encuentra la cuenta y recupera sus datos.
                when (UserPreferencesRepository.signIn(context, email)) {
                    UserPreferencesRepository.AuthResult.Ok -> {
                        withContext(Dispatchers.IO) {
                            SyncManager.hydrateForCurrentAccount(context)
                        }
                        onSignedIn()
                    }
                    UserPreferencesRepository.AuthResult.NoConnection -> {
                        loading = false
                        offlineError = true
                    }
                    else -> {
                        loading = false
                        notFoundError = true
                    }
                }
            }
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.xxl))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PredictaDimensions.Spacing.xl),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "¿No tenés cuenta?",
                style = PredictaTypography.small,
                color = PredictaColors.inkTertiary,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Registrate",
                style = PredictaTypography.bodyTight,
                color = PredictaColors.accent,
                modifier = Modifier.clickable(onClick = onCreateAccount),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Shared components
// ─────────────────────────────────────────────────────────

@Composable
private fun AmberButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    PrimaryButton(text = label, onClick = onClick, enabled = enabled)
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    HeaderIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onClick)
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = typoFieldLabel,
        color = PredictaColors.inkSecondary,
        modifier = Modifier.padding(bottom = PredictaDimensions.Spacing.xs),
    )
}

@Composable
private fun FieldError(text: String) {
    Text(
        text = text,
        style = PredictaTypography.caption,
        color = PredictaColors.danger,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    prefix: String? = null,
) {
    val borderColor = when {
        isError -> PredictaColors.danger
        else -> PredictaColors.border
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PredictaColors.card)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        textStyle = PredictaTypography.body.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = PredictaColors.ink),
        cursorBrush = SolidColor(PredictaColors.accent),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (prefix != null) {
                    Text(
                        text = prefix,
                        style = PredictaTypography.body,
                        color = PredictaColors.inkSecondary,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = PredictaTypography.body,
                            color = PredictaColors.inkTertiary,
                        )
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
private fun StepDots(totalSteps: Int, activeStep: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { i ->
            val active = i == activeStep
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .width(if (active) 18.dp else 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active) PredictaColors.accent else PredictaColors.border),
            )
        }
    }
}

