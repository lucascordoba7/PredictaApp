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
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
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
            .background(PredictaColors.charcoal)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // Floating hex particles in background
        HexParticles()

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
                    name = regName,
                    onBack = {
                        goingForward = false
                        step = Step.Register
                    },
                    onComplete = { income, paydayDay, fixedMonthly ->
                        onComplete()
                        // UserPreferencesRepository.completeOnboarding is called inside SetupContent
                    },
                    regName = regName,
                    regEmail = regEmail,
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
            PredictaLogoDice(size = 56.dp)
            Column {
                Text(
                    text = "FINANZAS CON CONCIENCIA",
                    style = PredictaTypography.monoCap.copy(fontSize = 11.sp),
                    color = PredictaColors.amber,
                    letterSpacing = 1.5.sp,
                )
                Row {
                    Text(
                        text = "predicta",
                        style = tyroBrand,
                        color = PredictaColors.cream,
                    )
                    Text(
                        text = ".",
                        style = tyroBrand,
                        color = PredictaColors.amber,
                    )
                }
            }
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))

        Text(
            text = buildAnnotatedString {
                append("Tus finanzas,\nmás ")
                withStyle(SpanStyle(color = PredictaColors.amber, fontWeight = FontWeight.Bold)) {
                    append("claras")
                }
                append(" que nunca")
            },
            style = typoHeroTitle,
            color = PredictaColors.cream,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.sm))
        Text(
            text = "Registrá tus gastos al toque, descubrí patrones\nescondidos y decidí gastar sin culpa.",
            style = typoSubtitle,
            color = PredictaColors.cream60,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        // Feature cards
        Column(
            verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FeatureCard(
                dotCount = 1,
                title = "Registrá al instante",
                subtitle = "Anotá gastos en segundos. Predicta aprende de tus movimientos sin esfuerzo.",
            )
            FeatureCard(
                dotCount = 2,
                title = "Sabé si te lo permitís",
                subtitle = "Describí lo que querés comprar y recibí el veredicto al instante.",
            )
            FeatureCard(
                dotCount = 3,
                title = "Predicciones con IA",
                subtitle = "Antes de que sea un problema, te avisamos cómo va a cerrar tu mes.",
            )
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AmberButton(label = "Registrar mis gastos", onClick = onGetStarted)

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text(
            text = "Ya tengo cuenta",
            style = PredictaTypography.monoCap,
            color = PredictaColors.cream35,
            modifier = Modifier
                .clickable(onClick = onSignIn)
                .padding(PredictaDimensions.Spacing.sm),
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        // Step dots
        StepDots(totalSteps = 2, activeStep = 0)

        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
    }
}

@Composable
private fun FeatureCard(dotCount: Int, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.card))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.md),
    ) {
        HexBadge(dotCount = dotCount, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = typoCardTitle, color = PredictaColors.cream)
            Text(subtitle, style = typoCardSub, color = PredictaColors.cream60)
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
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }

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
            PredictaLogoDice(size = 36.dp)
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text("Creá tu cuenta", style = typoScreenTitle, color = PredictaColors.cream)
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text(
            "Ingresá tus datos para empezar a tomar\ncontrol de tus finanzas.",
            style = typoSubtitle,
            color = PredictaColors.cream60,
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
            onValueChange = { email = it; emailError = false },
            placeholder = "tu@email.com",
            isError = emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
        )
        if (emailError) FieldError("Ingresá un email válido")
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AmberButton(label = "Continuar") {
            if (validate()) onContinue(name.trim(), email.trim())
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))

        Text(
            text = "Al continuar aceptás los Términos y Condiciones y la Política de Privacidad.",
            style = PredictaTypography.caption,
            color = PredictaColors.cream35,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
        StepDots(totalSteps = 2, activeStep = 1)
        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))
    }
}

// ─────────────────────────────────────────────────────────
// STEP 3 — Financial Setup
// ─────────────────────────────────────────────────────────
@Composable
private fun SetupContent(
    name: String,
    regName: String,
    regEmail: String,
    onBack: () -> Unit,
    onComplete: (income: Int, paydayDay: Int, fixedMonthly: Int) -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var incomeText by remember { mutableStateOf("") }
    var paydayText by remember { mutableStateOf("") }
    var fixedText by remember { mutableStateOf("") }
    var incomeError by remember { mutableStateOf(false) }
    var paydayError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        incomeError = incomeText.replace(".", "").toIntOrNull() == null || incomeText.replace(".", "").toInt() <= 0
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
            style = PredictaTypography.monoCap,
            color = PredictaColors.amber,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text("Configurá tu plata", style = typoScreenTitle, color = PredictaColors.cream)
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text(
            "Estos datos ayudan a Predicta a darte respuestas precisas.",
            style = typoSubtitle,
            color = PredictaColors.cream60,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        FieldLabel("¿Cuánto cobrás por mes? (ARS)")
        OnboardingTextField(
            value = incomeText,
            onValueChange = { incomeText = it.filter { c -> c.isDigit() || c == '.' }; incomeError = false },
            placeholder = "900.000",
            isError = incomeError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
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

        FieldLabel("Gastos fijos del mes (alquiler, servicios…)")
        OnboardingTextField(
            value = fixedText,
            onValueChange = { fixedText = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = "250.000",
            isError = false,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            prefix = "$",
        )
        Text(
            text = "Opcional — podés ajustarlo más tarde.",
            style = PredictaTypography.caption,
            color = PredictaColors.cream35,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AmberButton(label = "¡Listo! Empezar") {
            if (validate()) {
                val income = incomeText.replace(".", "").toIntOrNull() ?: 0
                val payday = paydayText.toIntOrNull() ?: 1
                val fixed = fixedText.replace(".", "").toIntOrNull() ?: 0
                scope.launch {
                    UserPreferencesRepository.completeOnboarding(
                        context,
                        UserSetup(
                            name = regName,
                            email = regEmail,
                            income = income,
                            paydayDay = payday,
                            fixedMonthly = fixed,
                        ),
                    )
                    onFinish()
                }
            }
        }
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
            PredictaLogoDice(size = 36.dp)
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text("Bienvenido de nuevo", style = typoScreenTitle, color = PredictaColors.cream)
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text(
            "Ingresá el email con el que te registraste en este dispositivo.",
            style = typoSubtitle,
            color = PredictaColors.cream60,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        FieldLabel("Email")
        OnboardingTextField(
            value = email,
            onValueChange = { email = it; emailError = false; notFoundError = false },
            placeholder = "tu@email.com",
            isError = emailError || notFoundError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
        )
        when {
            emailError -> FieldError("Ingresá un email válido")
            notFoundError -> FieldError("No encontramos una cuenta con ese email en este dispositivo")
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AmberButton(
            label = if (loading) "Verificando…" else "Entrar",
            enabled = !loading,
        ) {
            emailError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            if (emailError) return@AmberButton
            loading = true
            scope.launch {
                val setup = UserPreferencesRepository.getUserSetup(context).first()
                when {
                    setup == null -> {
                        loading = false
                        notFoundError = true
                    }
                    setup.email.equals(email.trim(), ignoreCase = true) -> onSignedIn()
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
                color = PredictaColors.cream35,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Registrate",
                style = PredictaTypography.bodyTight,
                color = PredictaColors.amber,
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(if (enabled) PredictaColors.amber else PredictaColors.surfaceHigh)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typoBtnLabel,
            color = if (enabled) PredictaColors.onAmber else PredictaColors.cream35,
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Volver",
            tint = PredictaColors.cream60,
            modifier = Modifier.size(18.dp),
        )
        Text("Volver", style = PredictaTypography.body, color = PredictaColors.cream60)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = typoFieldLabel,
        color = PredictaColors.cream60,
        modifier = Modifier.padding(bottom = PredictaDimensions.Spacing.xs),
    )
}

@Composable
private fun FieldError(text: String) {
    Text(
        text = text,
        style = PredictaTypography.caption,
        color = PredictaColors.coral,
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
        isError -> PredictaColors.coral
        value.isNotEmpty() -> PredictaColors.amberEdge
        else -> PredictaColors.lineStrong
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(1.dp, borderColor, RoundedCornerShape(PredictaDimensions.Radius.card)),
        textStyle = PredictaTypography.body.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = PredictaColors.cream),
        cursorBrush = SolidColor(PredictaColors.amber),
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
                        color = PredictaColors.cream60,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = PredictaTypography.body,
                            color = PredictaColors.cream35,
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
                    .background(if (active) PredictaColors.amber else PredictaColors.cream12),
            )
        }
    }
}

