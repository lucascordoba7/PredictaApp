package com.lucas.predictaapp.features.chat

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lucas.predictaapp.LaunchAction
import com.lucas.predictaapp.LaunchActions
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.ExpenseExtraction
import com.lucas.predictaapp.data.model.ExpenseSuggestion
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.remote.ChatMessage
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ChatRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import java.time.LocalDate
import java.util.UUID
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.lucas.predictaapp.ui.components.ChoiceChip
import com.lucas.predictaapp.ui.components.DiceMark
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.ui.components.HeroCard
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PickerRow
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.PredictaDialog
import com.lucas.predictaapp.ui.components.PredictaSheet
import com.lucas.predictaapp.ui.components.RowDivider
import com.lucas.predictaapp.ui.components.SheetHeader
import com.lucas.predictaapp.ui.components.pageWash
import com.lucas.predictaapp.ui.components.softShadow
import com.lucas.predictaapp.ui.theme.LightPalette
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun currentTime(): String {
    val now = java.time.LocalTime.now()
    return "%02d:%02d".format(now.hour, now.minute)
}

private sealed class ChatMsg {
    data class User(val text: String, val time: String = currentTime()) : ChatMsg()
    data class Bot(
        val text: String,
        val extraction: ExpenseExtraction? = null,
        val confirmed: Boolean = false,
        val dismissed: Boolean = false,
        val time: String = currentTime(),
    ) : ChatMsg()
    data object Thinking : ChatMsg()
}

private val starters = listOf(
    "Gasté \$38.500 en Don Julio",
    "Cafe 1.200 pesos",
    "Uber 8400",
    "Deposité el sueldo \$900k",
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ChatMsg>() }
    val conversationHistory = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val voice = rememberVoiceInput { inputText = it }

    val allCategories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    val expenseCategoryNames = remember(allCategories) {
        allCategories.filter { it.type == CategoryType.EXPENSE }.map { it.name }
            .ifEmpty { ExpenseCategories.expenseNames }
    }
    val emojiFor: (String) -> String = remember(allCategories) {
        val map = allCategories.associate { it.name to it.emoji }
        val fn: (String) -> String = { name -> map[name] ?: ExpenseCategories.emojiFor(name) }
        fn
    }

    fun botMsgFor(extraction: ExpenseExtraction, unknownReply: String?): ChatMsg.Bot = when (extraction) {
        is ExpenseExtraction.Expense -> ChatMsg.Bot(
            text = "Encontré este gasto 👇",
            extraction = extraction,
        )
        is ExpenseExtraction.Income -> ChatMsg.Bot(
            text = "Anotado como ingreso 👇",
            extraction = extraction,
        )
        is ExpenseExtraction.MultiExpense -> ChatMsg.Bot(
            text = "Encontré ${extraction.expenses.size} gastos 👇",
            extraction = extraction,
        )
        is ExpenseExtraction.Subscription -> ChatMsg.Bot(
            text = "Detecté una suscripción 👇",
            extraction = extraction,
        )
        is ExpenseExtraction.Clarify -> ChatMsg.Bot(
            text = extraction.question,
            extraction = extraction,
        )
        is ExpenseExtraction.Unknown -> ChatMsg.Bot(
            text = unknownReply ?: "Solo puedo ayudarte con gastos e ingresos.",
        )
    }

    fun send(text: String) {
        if (text.isBlank()) return
        inputText = ""
        messages.add(ChatMsg.User(text))
        messages.add(ChatMsg.Thinking)

        scope.launch {
            val historySnapshot = conversationHistory.takeLast(10)
            val (extraction, unknownReply, rawResponse) = ChatRepository.extract(text, expenseCategoryNames, historySnapshot)
            conversationHistory.add(ChatMessage("user", text))
            if (rawResponse.isNotEmpty()) conversationHistory.add(ChatMessage("assistant", rawResponse))

            val thinkingIdx = messages.indexOfLast { it is ChatMsg.Thinking }
            if (thinkingIdx >= 0) messages.removeAt(thinkingIdx)

            messages.add(botMsgFor(extraction, unknownReply))
            listState.animateScrollToItem(messages.size) // +1 por la píldora del día
        }
    }

    /** Manda la foto de un ticket al modelo de visión; el resto del flujo es el mismo. */
    fun sendTicket(uri: Uri) {
        messages.add(ChatMsg.User("📷 Foto de ticket"))
        messages.add(ChatMsg.Thinking)
        scope.launch {
            val base64 = runCatching { TicketImage.toBase64Jpeg(context, uri) }.getOrNull()
            val (extraction, reply, raw) = if (base64 == null) {
                Triple(ExpenseExtraction.Unknown, "No pude leer la imagen. Probá con otra foto.", "")
            } else {
                ChatRepository.extractFromTicket(base64, expenseCategoryNames)
            }
            conversationHistory.add(ChatMessage("user", "[foto de un ticket]"))
            if (raw.isNotEmpty()) conversationHistory.add(ChatMessage("assistant", raw))

            val thinkingIdx = messages.indexOfLast { it is ChatMsg.Thinking }
            if (thinkingIdx >= 0) messages.removeAt(thinkingIdx)

            messages.add(botMsgFor(extraction, reply))
            listState.animateScrollToItem(messages.size) // +1 por la píldora del día
        }
    }

    // Cámara (FileProvider en cache) y galería (photo picker, sin permiso).
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) captureUri?.let { sendTicket(it) }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { sendTicket(it) }
    }

    fun launchCamera() {
        val uri = TicketImage.createCaptureUri(context)
        captureUri = uri
        takePicture.launch(uri)
    }

    // Con CAMERA declarado en el manifest, TakePicture exige el permiso en runtime.
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) launchCamera()
    }

    fun scanTicket() {
        if (cameraPermission.status.isGranted) launchCamera() else cameraPermission.launchPermissionRequest()
    }

    var showAttachSheet by remember { mutableStateOf(false) }

    // Acciones con las que se llegó al chat (widget, shortcut, share, QuickActions).
    LaunchedEffect(Unit) {
        LaunchActions.pending.collect { action ->
            when (action) {
                LaunchAction.VoiceChat -> {
                    LaunchActions.clear()
                    if (!voice.listening) voice.toggle()
                }
                LaunchAction.ScanTicket -> {
                    LaunchActions.clear()
                    scanTicket()
                }
                is LaunchAction.SharedText -> {
                    LaunchActions.clear()
                    send(action.text)
                }
                is LaunchAction.SharedImage -> {
                    LaunchActions.clear()
                    sendTicket(action.uri)
                }
                else -> Unit
            }
        }
    }

    var duplicateWarning by remember { mutableStateOf<Pair<Int, ExpenseExtraction.Expense>?>(null) }

    fun reallyConfirm(index: Int, extraction: ExpenseExtraction) {
        val msg = messages[index] as? ChatMsg.Bot ?: return
        messages[index] = msg.copy(confirmed = true)
        scope.launch {
            when (extraction) {
                is ExpenseExtraction.Expense -> ExpensesRepository.addByName(
                    merchant = extraction.merchant,
                    categoryName = extraction.category,
                    amount = extraction.amount,
                    dateMillis = extraction.dateMillis,
                )
                is ExpenseExtraction.Income -> ExpensesRepository.addByName(
                    merchant = extraction.merchant,
                    categoryName = "Ingreso",
                    amount = -extraction.amount,
                    dateMillis = extraction.dateMillis,
                    income = true,
                )
                is ExpenseExtraction.MultiExpense -> extraction.expenses.forEach { exp ->
                    ExpensesRepository.addByName(
                        merchant = exp.merchant,
                        categoryName = exp.category,
                        amount = exp.amount,
                        dateMillis = exp.dateMillis,
                    )
                }
                is ExpenseExtraction.Subscription -> SubscriptionsRepository.upsert(
                    Subscription(
                        id = UUID.randomUUID().toString(),
                        service = extraction.service,
                        initial = extraction.service.take(1).uppercase(),
                        monthly = extraction.monthly,
                        lastUsedDate = LocalDate.now().toString(),
                    )
                )
                else -> Unit
            }
        }
    }

    fun confirmExtraction(index: Int, extraction: ExpenseExtraction) {
        // Heurística anti doble carga: mismo comercio y monto ya registrados hoy.
        if (extraction is ExpenseExtraction.Expense) {
            scope.launch {
                val dup = ExpensesRepository.findSameDayDuplicate(
                    extraction.merchant, extraction.amount, extraction.dateMillis,
                )
                if (dup != null) duplicateWarning = index to extraction
                else reallyConfirm(index, extraction)
            }
        } else {
            reallyConfirm(index, extraction)
        }
    }

    fun dismissExtraction(index: Int) {
        val msg = messages[index] as? ChatMsg.Bot ?: return
        messages[index] = msg.copy(dismissed = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pageWash()
            .imePadding(),
    ) {
        TopBar()

        if (messages.isEmpty()) {
            EmptyState(
                onStarterClick = { send(it) },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "day") { DayPill("Hoy") }
                items(messages.size) { i ->
                    when (val msg = messages[i]) {
                        is ChatMsg.User -> UserBubble(msg.text, msg.time)
                        is ChatMsg.Bot -> BotBubble(
                            msg = msg,
                            emojiFor = emojiFor,
                            onConfirm = { confirmExtraction(i, msg.extraction!!) },
                            onDismiss = { dismissExtraction(i) },
                            onSuggestionClick = { send(it) },
                        )
                        is ChatMsg.Thinking -> ThinkingBubble()
                    }
                }
            }
        }

        ChatInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = { send(inputText) },
            voice = voice,
            onAttach = { showAttachSheet = true },
        )
    }

    if (showAttachSheet) {
        PredictaSheet(onDismiss = { showAttachSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SheetHeader(title = "Escanear ticket", subtitle = "Leo el total y el comercio", onClose = { showAttachSheet = false })
                PickerRow(
                    icon = Icons.Outlined.PhotoCamera,
                    title = "Sacar una foto",
                    subtitle = "Con la cámara, ahora",
                    trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onClick = {
                        showAttachSheet = false
                        scanTicket()
                    },
                )
                PickerRow(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = "Elegir de la galería",
                    subtitle = "Una foto que ya tenés",
                    trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onClick = {
                        showAttachSheet = false
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
            }
        }
    }

    duplicateWarning?.let { (index, expense) ->
        PredictaDialog(
            title = "¿Ya lo cargaste?",
            text = "Hoy ya hay un ${expense.merchant} por $${expense.amount.fmtArs()}. ¿Lo registro igual?",
            confirmLabel = "Registrar igual",
            onConfirm = {
                reallyConfirm(index, expense)
                duplicateWarning = null
            },
            onDismiss = { duplicateWarning = null },
        )
    }
}

@Composable
private fun TopBar() {
    val infinite = rememberInfiniteTransition(label = "status")
    val dotAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "statusDot",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DiceMark(size = 42.dp)
        Column {
            Text("Predicta", style = PredictaTypography.heading, color = PredictaColors.ink)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).background(PredictaColors.success.copy(alpha = dotAlpha), CircleShape))
                Text("En línea", style = PredictaTypography.label.copy(fontSize = 13.sp), color = PredictaColors.kicker)
            }
        }
    }
}

@Composable
private fun DayPill(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = PredictaTypography.label.copy(fontSize = 12.sp),
            color = PredictaColors.kicker,
            modifier = Modifier
                .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                .background(PredictaColors.glass)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(
    onStarterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlyphBadge(size = 72.dp, modifier = Modifier.clip(RoundedCornerShape(24.dp))) {
            Text("💬", fontSize = 32.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Contame un gasto o ingreso",
            style = PredictaTypography.title,
            color = PredictaColors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Escribilo como te salga y lo anoto.\nTambién podés dictarlo o sacarle foto al ticket.",
            style = PredictaTypography.body,
            color = PredictaColors.inkSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Kicker("Probá con")
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            starters.forEachIndexed { i, starter ->
                AnimatedChip(
                    text = starter,
                    delayMs = 150 + i * 130,
                    onClick = { onStarterClick(starter) },
                )
            }
        }
    }
}

@Composable
private fun AnimatedChip(text: String, delayMs: Int, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) +
            slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it / 2 } +
            scaleIn(tween(350, easing = FastOutSlowInEasing), initialScale = 0.95f),
    ) {
        ChoiceChip(text = text, selected = false, onClick = onClick)
    }
}

@Composable
private fun EnterAnimated(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxWidth(),
        enter = fadeIn(tween(200)) +
            slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 3 } +
            scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.97f),
    ) { content() }
}

@Composable
private fun UserBubble(text: String, time: String) {
    EnterAnimated {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Text(
                text = text,
                style = PredictaTypography.body.copy(fontWeight = PredictaTypography.bodyTight.fontWeight, lineHeight = 21.sp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .wrapContentWidth(Alignment.End)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 6.dp))
                    .background(Brush.linearGradient(listOf(PredictaColors.heroStart, PredictaColors.heroMid)))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Text(time, style = PredictaTypography.caption, color = PredictaColors.inkTertiary, modifier = Modifier.padding(top = 4.dp, end = 4.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BotBubble(
    msg: ChatMsg.Bot,
    emojiFor: (String) -> String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onSuggestionClick: (String) -> Unit,
) {
    val ext = msg.extraction
    val hasCard = ext is ExpenseExtraction.Expense || ext is ExpenseExtraction.Income ||
        ext is ExpenseExtraction.MultiExpense || ext is ExpenseExtraction.Subscription
    EnterAnimated {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                // Ya confirmado: la tarjeta se colapsa en una línea de recibo.
                hasCard && msg.confirmed -> ConfirmedChip(ext!!)
                hasCard && msg.dismissed -> Text(
                    "Descartado",
                    style = PredictaTypography.label.copy(fontSize = 13.sp),
                    color = PredictaColors.inkTertiary,
                    modifier = Modifier.padding(start = 4.dp),
                )
                else -> {
                    // Con tarjeta, el texto va suelto como título; sin tarjeta, es la burbuja.
                    if (hasCard) {
                        Text(msg.text, style = PredictaTypography.body.copy(fontWeight = PredictaTypography.bodyTight.fontWeight), color = PredictaColors.ink, modifier = Modifier.padding(start = 4.dp))
                    } else {
                        val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
                        Text(
                            text = msg.text,
                            style = PredictaTypography.body.copy(lineHeight = 21.sp),
                            color = PredictaColors.ink,
                            modifier = Modifier
                                .softShadow(shape, elevation = 4.dp)
                                .clip(shape)
                                .background(PredictaColors.card)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    when (ext) {
                        is ExpenseExtraction.Expense, is ExpenseExtraction.Income ->
                            ExtractionCard(listOf(extractionRow(ext, emojiFor)), onConfirm = onConfirm, onDismiss = onDismiss)
                        is ExpenseExtraction.MultiExpense ->
                            ExtractionCard(ext.expenses.map { extractionRow(it, emojiFor) }, onConfirm = onConfirm, onDismiss = onDismiss)
                        is ExpenseExtraction.Subscription ->
                            SubscriptionCard(ext, onConfirm = onConfirm, onDismiss = onDismiss)
                        is ExpenseExtraction.Clarify -> if (ext.suggestions.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ext.suggestions.forEach { sug -> SuggestionChip(sug, onClick = { onSuggestionClick(sug.rewrittenIntent) }) }
                            }
                        }
                        else -> Unit
                    }
                }
            }
            Text(msg.time, style = PredictaTypography.caption, color = PredictaColors.inkTertiary, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** Una fila de la tarjeta de extracción. */
private data class ExtractionRow(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val amount: Int,
    val income: Boolean,
)

private fun extractionRow(ext: ExpenseExtraction, emojiFor: (String) -> String): ExtractionRow = when (ext) {
    is ExpenseExtraction.Expense -> ExtractionRow(
        emoji = emojiFor(ext.category),
        title = ext.merchant,
        subtitle = if (ext.whenLabel != "hoy") "${ext.category} · ${ext.whenLabel}" else ext.category,
        amount = ext.amount,
        income = false,
    )
    is ExpenseExtraction.Income -> ExtractionRow(
        emoji = "💰",
        title = ext.merchant,
        subtitle = if (ext.whenLabel != "hoy") "Ingreso · ${ext.whenLabel}" else "Ingreso",
        amount = ext.amount,
        income = true,
    )
    else -> error("sin fila para $ext")
}

/** Lo que entendió Predicta, para confirmar o ignorar. Con más de una fila suma el total. */
@Composable
private fun ExtractionCard(rows: List<ExtractionRow>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PredictaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)) {
        rows.forEachIndexed { i, row ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                GlyphBadge(size = 40.dp, container = if (row.income) PredictaColors.successSoft else PredictaColors.accentSoft) {
                    Text(row.emoji, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.title, style = PredictaTypography.rowTitle.copy(fontSize = 15.sp), color = PredictaColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(row.subtitle, style = PredictaTypography.rowSubtitle, color = PredictaColors.inkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = (if (row.income) "+" else "") + "$${row.amount.fmtArs()}",
                    style = PredictaTypography.rowAmount.copy(fontSize = 16.sp),
                    color = if (row.income) PredictaColors.success else PredictaColors.ink,
                )
            }
            if (i < rows.lastIndex) RowDivider()
        }
        if (rows.size > 1) {
            RowDivider()
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = PredictaTypography.bodyTight, color = PredictaColors.inkSecondary, modifier = Modifier.weight(1f))
                Text("$${rows.sumOf { it.amount }.fmtArs()}", style = PredictaTypography.rowAmount.copy(fontSize = 18.sp), color = PredictaColors.ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardButton("Ignorar", onDismiss, filled = false, modifier = Modifier.weight(1f))
            CardButton(if (rows.size > 1) "Confirmar todos" else "Confirmar", onConfirm, filled = true, modifier = Modifier.weight(2f))
        }
    }
}

@Composable
private fun CardButton(text: String, onClick: () -> Unit, filled: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .then(
                if (filled) Modifier.background(PredictaColors.primaryBrush)
                else Modifier.background(PredictaColors.card).border(1.dp, PredictaColors.border, shape),
            )
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(text, style = PredictaTypography.label.copy(fontSize = 15.sp), color = if (filled) Color.White else PredictaColors.ink)
    }
}

/** Suscripción detectada: es una sugerencia, no un gasto, así que va en la hero con degradé. */
@Composable
private fun SubscriptionCard(ext: ExpenseExtraction.Subscription, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    HeroCard(contentPadding = PaddingValues(16.dp), modifier = Modifier.clip(RoundedCornerShape(PredictaDimensions.Radius.card))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphBadge(size = 40.dp, container = PredictaColors.honey) {
                Icon(Icons.Filled.Autorenew, contentDescription = null, tint = PredictaColors.onHoney, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("${ext.service} parece una suscripción", style = PredictaTypography.label.copy(fontSize = 15.sp, fontWeight = PredictaTypography.button.fontWeight), color = Color.White)
                Text("Mensual · $${ext.monthly.fmtArs()}/mes", style = PredictaTypography.rowSubtitle, color = Color.White.copy(alpha = 0.85f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.16f)).clickable(role = Role.Button, onClick = onDismiss),
            ) { Text("Ahora no", style = PredictaTypography.label, color = Color.White) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(2f).height(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Color.White).clickable(role = Role.Button, onClick = onConfirm),
            ) { Text("Agregar suscripción", style = PredictaTypography.label.copy(fontWeight = PredictaTypography.button.fontWeight), color = LightPalette.heroMid) }
        }
    }
}

/** Recibo de una línea después de confirmar: "✓ Café · $2.500  Registrado". */
@Composable
private fun ConfirmedChip(ext: ExpenseExtraction) {
    val (label, status) = when (ext) {
        is ExpenseExtraction.Expense -> "${ext.merchant} · $${ext.amount.fmtArs()}" to "Registrado"
        is ExpenseExtraction.Income -> "${ext.merchant} · +$${ext.amount.fmtArs()}" to "Registrado"
        is ExpenseExtraction.MultiExpense -> "${ext.expenses.size} gastos · $${ext.expenses.sumOf { it.amount }.fmtArs()}" to "Registrados"
        is ExpenseExtraction.Subscription -> "${ext.service} · $${ext.monthly.fmtArs()}/mes" to "Agregada"
        else -> return
    }
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .softShadow(shape, elevation = 4.dp)
            .clip(shape)
            .background(PredictaColors.card)
            .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
    ) {
        GlyphBadge(size = 32.dp, container = PredictaColors.successSoft) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = PredictaColors.success, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = PredictaTypography.label, color = PredictaColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(8.dp))
        Text(status, style = PredictaTypography.label.copy(fontSize = 13.sp), color = PredictaColors.success)
    }
}

@Composable
private fun SuggestionChip(suggestion: ExpenseSuggestion, onClick: () -> Unit) {
    Text(
        text = suggestion.label,
        style = PredictaTypography.label,
        color = PredictaColors.accent,
        modifier = Modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .background(PredictaColors.accentSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun ThinkingBubble() {
    val infinite = rememberInfiniteTransition(label = "thinking")
    val animSpec = { offsetMs: Int ->
        infiniteRepeatable<Float>(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(offsetMs, StartOffsetType.FastForward),
        )
    }
    val offset0 by infinite.animateFloat(0f, -5f, animSpec(0), label = "d0")
    val offset1 by infinite.animateFloat(0f, -5f, animSpec(167), label = "d1")
    val offset2 by infinite.animateFloat(0f, -5f, animSpec(334), label = "d2")

    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
    Row(
        modifier = Modifier
            .softShadow(shape, elevation = 4.dp)
            .clip(shape)
            .background(PredictaColors.card)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(offset0, offset1, offset2).forEach { offset ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = offset.dp)
                    .background(PredictaColors.accent.copy(alpha = 0.6f), CircleShape),
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    voice: VoiceInputState,
    onAttach: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            voice.listening -> PredictaColors.danger
            isFocused -> PredictaColors.accent
            else -> Color.Transparent
        },
        animationSpec = tween(250),
        label = "inputBorder",
    )
    // Pulso del botón mientras el micrófono está abierto.
    val micPulse by rememberInfiniteTransition(label = "micPulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "micPulseAlpha",
    )
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to PredictaColors.paper.copy(alpha = 0f), 0.3f to PredictaColors.paper))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .softShadow(shape, elevation = 6.dp)
                .clip(shape)
                .background(PredictaColors.card)
                .clickable(role = Role.Button, onClick = onAttach),
        ) {
            Icon(Icons.Outlined.DocumentScanner, contentDescription = "Escanear ticket", tint = PredictaColors.accent, modifier = Modifier.size(22.dp))
        }

        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = PredictaTypography.body.copy(color = PredictaColors.ink),
            cursorBrush = SolidColor(PredictaColors.accent),
            maxLines = 4,
            decorationBox = { inner ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .softShadow(shape, elevation = 6.dp)
                        .clip(shape)
                        .background(PredictaColors.card)
                        .border(1.5.dp, borderColor, shape)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = if (voice.listening) "Escuchando…" else "Contame un gasto o ingreso",
                            style = PredictaTypography.body,
                            color = if (voice.listening) PredictaColors.danger else PredictaColors.inkTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )

        // Un solo botón, estilo WhatsApp: mic con campo vacío, stop mientras
        // escucha, flecha de enviar cuando hay texto.
        val showMic = voice.isAvailable && text.isBlank() && !voice.listening
        val enabled = voice.listening || text.isNotBlank() || showMic
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .then(
                    when {
                        voice.listening -> Modifier.background(PredictaColors.danger.copy(alpha = micPulse))
                        enabled -> Modifier.background(PredictaColors.primaryBrush)
                        else -> Modifier.background(PredictaColors.neutralSoft)
                    },
                )
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = { if (voice.listening || showMic) voice.toggle() else onSend() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    voice.listening -> Icons.Filled.Stop
                    showMic -> Icons.Outlined.Mic
                    else -> Icons.Filled.ArrowUpward
                },
                contentDescription = when {
                    voice.listening -> "Detener dictado"
                    showMic -> "Dictar gasto"
                    else -> "Enviar"
                },
                tint = if (enabled) Color.White else PredictaColors.inkTertiary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
