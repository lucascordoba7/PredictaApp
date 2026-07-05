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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.lucas.predictaapp.features.onboarding.PredictaLogoDice
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import java.time.LocalDate
import java.util.UUID
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun hexToColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrElse { PredictaColors.amber }

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

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
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
    val colorFor: (String) -> Color = remember(allCategories) {
        val map = allCategories.associate { it.name to it.color }
        val fn: (String) -> Color = { name -> hexToColor(map[name] ?: ExpenseCategories.colorHexFor(name)) }
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
            listState.animateScrollToItem(messages.lastIndex)
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
            listState.animateScrollToItem(messages.lastIndex)
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = PredictaDimensions.Spacing.base,
                    vertical = PredictaDimensions.Spacing.base,
                ),
                verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                items(messages.size) { i ->
                    when (val msg = messages[i]) {
                        is ChatMsg.User -> UserBubble(msg.text, msg.time)
                        is ChatMsg.Bot -> BotBubble(
                            msg = msg,
                            emojiFor = emojiFor,
                            colorFor = colorFor,
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
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PredictaColors.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                AttachOption(
                    icon = Icons.Filled.CameraAlt,
                    label = "Sacar foto del ticket",
                    onClick = {
                        showAttachSheet = false
                        scanTicket()
                    },
                )
                AttachOption(
                    icon = Icons.Filled.PhotoLibrary,
                    label = "Elegir de la galería",
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
        AlertDialog(
            onDismissRequest = { duplicateWarning = null },
            containerColor = PredictaColors.surface,
            title = {
                Text("¿Gasto duplicado?", style = PredictaTypography.cardTitle, color = PredictaColors.cream)
            },
            text = {
                Text(
                    text = "Ya registraste ${expense.merchant} por $${expense.amount.fmtArs()} hoy. ¿Lo registro igual?",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    reallyConfirm(index, expense)
                    duplicateWarning = null
                }) { Text("Registrar igual", color = PredictaColors.amber) }
            },
            dismissButton = {
                TextButton(onClick = { duplicateWarning = null }) {
                    Text("Cancelar", color = PredictaColors.cream60)
                }
            },
        )
    }
}

@Composable
private fun AttachOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.base),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surfaceHigh)
            .clickable(onClick = onClick)
            .padding(PredictaDimensions.Spacing.base),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PredictaColors.amber,
            modifier = Modifier.size(22.dp),
        )
        Text(text = label, style = PredictaTypography.bodyTight, color = PredictaColors.cream)
    }
}

@Composable
private fun TopBar() {
    val infinite = rememberInfiniteTransition(label = "status")
    val dotAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "statusDot",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PredictaColors.charcoal),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PredictaDimensions.Spacing.screenPadding,
                    vertical = PredictaDimensions.Spacing.base,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PredictaColors.amberSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PredictaColors.surfaceHigh, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PredictaLogoDice(size = 26.dp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Predicta", style = PredictaTypography.bodyTight, color = PredictaColors.cream)
                Text(
                    "Registrá gastos con texto natural",
                    style = PredictaTypography.caption,
                    color = PredictaColors.cream35,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(PredictaColors.green.copy(alpha = dotAlpha), CircleShape),
                    )
                    Text("En línea", style = PredictaTypography.caption, color = PredictaColors.green)
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = PredictaDimensions.Spacing.screenPadding),
            color = PredictaColors.lineStrong,
            thickness = 0.5.dp,
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
            .padding(PredictaDimensions.Spacing.screenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(PredictaDimensions.Radius.lg))
                .background(PredictaColors.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text("💬", style = PredictaTypography.section)
        }
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))
        Text(
            text = "Contame un gasto o ingreso",
            style = PredictaTypography.cardTitle,
            color = PredictaColors.cream,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text(
            text = "Lo registro automáticamente para que\ntengas el control de tus finanzas.",
            style = PredictaTypography.body,
            color = PredictaColors.cream60,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
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
        Text(
            text = text,
            style = PredictaTypography.small,
            color = PredictaColors.cream60,
            modifier = Modifier
                .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.pill))
                .clickable(onClick = onClick)
                .padding(
                    horizontal = PredictaDimensions.Spacing.md,
                    vertical = PredictaDimensions.Spacing.sm,
                ),
        )
    }
}

@Composable
private fun UserBubble(text: String, time: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxWidth(),
        enter = fadeIn(tween(200)) +
            slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 3 } +
            scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.97f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = text,
                style = PredictaTypography.body,
                color = PredictaColors.onAmber,
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = PredictaDimensions.Radius.bubble,
                            topEnd = 4.dp,
                            bottomStart = PredictaDimensions.Radius.bubble,
                            bottomEnd = PredictaDimensions.Radius.bubble,
                        )
                    )
                    .background(PredictaColors.amber)
                    .padding(
                        horizontal = PredictaDimensions.Spacing.base,
                        vertical = PredictaDimensions.Spacing.sm,
                    ),
            )
            Text(
                text = time,
                style = PredictaTypography.caption,
                color = PredictaColors.cream35,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BotBubble(
    msg: ChatMsg.Bot,
    emojiFor: (String) -> String,
    colorFor: (String) -> Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onSuggestionClick: (String) -> Unit,
) {
    val bubbleShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = PredictaDimensions.Radius.bubble,
        bottomStart = PredictaDimensions.Radius.bubble,
        bottomEnd = PredictaDimensions.Radius.bubble,
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxWidth(),
        enter = fadeIn(tween(200)) +
            slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 3 } +
            scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.97f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
        ) {
            Text(
                text = msg.text,
                style = PredictaTypography.body,
                color = PredictaColors.cream,
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(PredictaColors.surface)
                    .border(0.5.dp, PredictaColors.lineStrong, bubbleShape)
                    .padding(
                        horizontal = PredictaDimensions.Spacing.base,
                        vertical = PredictaDimensions.Spacing.sm,
                    ),
            )

            when (val ext = msg.extraction) {
                is ExpenseExtraction.Expense, is ExpenseExtraction.Income -> {
                    if (!msg.dismissed) {
                        ExtractionCard(
                            extraction = ext,
                            emojiFor = emojiFor,
                            colorFor = colorFor,
                            confirmed = msg.confirmed,
                            onConfirm = onConfirm,
                            onDismiss = onDismiss,
                        )
                    }
                }
                is ExpenseExtraction.MultiExpense -> {
                    if (!msg.dismissed) {
                        MultiExtractionCard(
                            extraction = ext,
                            emojiFor = emojiFor,
                            colorFor = colorFor,
                            confirmed = msg.confirmed,
                            onConfirm = onConfirm,
                            onDismiss = onDismiss,
                        )
                    }
                }
                is ExpenseExtraction.Subscription -> {
                    if (!msg.dismissed) {
                        SubscriptionExtractionCard(
                            extraction = ext,
                            confirmed = msg.confirmed,
                            onConfirm = onConfirm,
                            onDismiss = onDismiss,
                        )
                    }
                }
                is ExpenseExtraction.Clarify -> {
                    if (ext.suggestions.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
                        ) {
                            ext.suggestions.forEach { sug ->
                                SuggestionChip(sug, onClick = { onSuggestionClick(sug.rewrittenIntent) })
                            }
                        }
                    }
                }
                else -> Unit
            }

            Text(
                text = msg.time,
                style = PredictaTypography.caption,
                color = PredictaColors.cream35,
            )
        }
    }
}

@Composable
private fun MultiExtractionCard(
    extraction: ExpenseExtraction.MultiExpense,
    emojiFor: (String) -> String,
    colorFor: (String) -> Color,
    confirmed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val total = extraction.expenses.sumOf { it.amount }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(
                1.dp,
                if (confirmed) PredictaColors.greenSoft else PredictaColors.lineStrong,
                RoundedCornerShape(PredictaDimensions.Radius.card),
            )
            .padding(PredictaDimensions.Spacing.base),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        extraction.expenses.forEach { exp ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
                ) {
                    CategoryBubble(emoji = emojiFor(exp.category), color = colorFor(exp.category))
                    Column {
                        Text(exp.merchant, style = PredictaTypography.bodyTight, color = PredictaColors.cream)
                        Text(
                            text = if (exp.whenLabel != "hoy") "${exp.category} · ${exp.whenLabel}" else exp.category,
                            style = PredictaTypography.caption,
                            color = PredictaColors.cream35,
                        )
                    }
                }
                Text(
                    text = "\$${exp.amount.fmtArs()}",
                    style = PredictaTypography.bodyTight,
                    color = PredictaColors.cream,
                )
            }
        }

        HorizontalDivider(color = PredictaColors.lineStrong, thickness = 0.5.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Total", style = PredictaTypography.bodyTight, color = PredictaColors.cream60)
            Text("\$${total.fmtArs()}", style = PredictaTypography.bodyTight, color = PredictaColors.cream)
        }

        if (!confirmed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
                        .background(PredictaColors.surfaceHigh)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = PredictaDimensions.Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Close, null, tint = PredictaColors.cream60, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ignorar", style = PredictaTypography.small, color = PredictaColors.cream60)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
                        .background(PredictaColors.greenSoft)
                        .clickable(onClick = onConfirm)
                        .padding(vertical = PredictaDimensions.Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, null, tint = PredictaColors.green, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Confirmar todos", style = PredictaTypography.small, color = PredictaColors.green)
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Check, null, tint = PredictaColors.green, modifier = Modifier.size(14.dp))
                Text("Registrados", style = PredictaTypography.small, color = PredictaColors.green)
            }
        }
    }
}

@Composable
private fun ExtractionCard(
    extraction: ExpenseExtraction,
    emojiFor: (String) -> String,
    colorFor: (String) -> Color,
    confirmed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (emoji, label, amount, category) = when (extraction) {
        is ExpenseExtraction.Expense -> ExtractionInfo(
            emoji = emojiFor(extraction.category),
            label = extraction.merchant,
            amount = extraction.amount,
            category = extraction.category,
        )
        is ExpenseExtraction.Income -> ExtractionInfo(
            emoji = "💰",
            label = extraction.merchant,
            amount = extraction.amount,
            category = "Ingreso",
        )
        else -> return
    }

    val dateLabel = when (extraction) {
        is ExpenseExtraction.Expense -> if (extraction.whenLabel != "hoy") "$category · ${extraction.whenLabel}" else category
        is ExpenseExtraction.Income -> if (extraction.whenLabel != "hoy") "$category · ${extraction.whenLabel}" else category
        else -> category
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(
                1.dp,
                if (confirmed) PredictaColors.greenSoft else PredictaColors.lineStrong,
                RoundedCornerShape(PredictaDimensions.Radius.card),
            )
            .padding(PredictaDimensions.Spacing.base),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                val bubbleColor = if (extraction is ExpenseExtraction.Income) PredictaColors.green else colorFor(category)
                CategoryBubble(emoji = emoji, color = bubbleColor)
                Column {
                    Text(label, style = PredictaTypography.bodyTight, color = PredictaColors.cream)
                    Text(dateLabel, style = PredictaTypography.caption, color = PredictaColors.cream35)
                }
            }
            Text(
                text = "\$${amount.fmtArs()}",
                style = PredictaTypography.bodyTight,
                color = if (extraction is ExpenseExtraction.Income) PredictaColors.green else PredictaColors.cream,
            )
        }

        if (!confirmed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
                        .background(PredictaColors.surfaceHigh)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = PredictaDimensions.Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Close, null, tint = PredictaColors.cream60, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ignorar", style = PredictaTypography.small, color = PredictaColors.cream60)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
                        .background(PredictaColors.greenSoft)
                        .clickable(onClick = onConfirm)
                        .padding(vertical = PredictaDimensions.Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, null, tint = PredictaColors.green, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Confirmar", style = PredictaTypography.small, color = PredictaColors.green)
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Check, null, tint = PredictaColors.green, modifier = Modifier.size(14.dp))
                Text("Registrado", style = PredictaTypography.small, color = PredictaColors.green)
            }
        }
    }
}

@Composable
private fun SubscriptionExtractionCard(
    extraction: ExpenseExtraction.Subscription,
    confirmed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(
                1.dp,
                if (confirmed) PredictaColors.greenSoft else PredictaColors.amberEdge,
                RoundedCornerShape(PredictaDimensions.Radius.card),
            )
            .padding(PredictaDimensions.Spacing.base),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PredictaColors.amberSoft),
                ) {
                    Text(
                        text = extraction.service.take(1).uppercase(),
                        style = PredictaTypography.bodyTight,
                        color = PredictaColors.amber,
                    )
                }
                Column {
                    Text(
                        text = extraction.service,
                        style = PredictaTypography.bodyTight,
                        color = PredictaColors.cream,
                    )
                    Text(
                        text = "Suscripción · mensual",
                        style = PredictaTypography.caption,
                        color = PredictaColors.cream35,
                    )
                }
            }
            Text(
                text = "\$${extraction.monthly.fmtArs()}/mes",
                style = PredictaTypography.bodyTight,
                color = PredictaColors.amber,
            )
        }

        if (!confirmed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
                        .background(PredictaColors.surfaceHigh)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = PredictaDimensions.Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Close, null, tint = PredictaColors.cream60, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ignorar", style = PredictaTypography.small, color = PredictaColors.cream60)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
                        .background(PredictaColors.amberSoft)
                        .clickable(onClick = onConfirm)
                        .padding(vertical = PredictaDimensions.Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, null, tint = PredictaColors.amber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar suscripción", style = PredictaTypography.small, color = PredictaColors.amber)
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Check, null, tint = PredictaColors.green, modifier = Modifier.size(14.dp))
                Text("Suscripción agregada", style = PredictaTypography.small, color = PredictaColors.green)
            }
        }
    }
}

private data class ExtractionInfo(
    val emoji: String,
    val label: String,
    val amount: Int,
    val category: String,
)

@Composable
private fun CategoryBubble(emoji: String, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
    ) {
        Text(text = emoji, fontSize = 16.sp)
    }
}

@Composable
private fun SuggestionChip(suggestion: ExpenseSuggestion, onClick: () -> Unit) {
    Text(
        text = suggestion.label,
        style = PredictaTypography.small,
        color = PredictaColors.amber,
        modifier = Modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .border(1.dp, PredictaColors.amberEdge, RoundedCornerShape(PredictaDimensions.Radius.pill))
            .background(PredictaColors.amberSoft)
            .clickable(onClick = onClick)
            .padding(
                horizontal = PredictaDimensions.Spacing.md,
                vertical = PredictaDimensions.Spacing.sm,
            ),
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

    val bubbleShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = PredictaDimensions.Radius.bubble,
        bottomStart = PredictaDimensions.Radius.bubble,
        bottomEnd = PredictaDimensions.Radius.bubble,
    )

    Row(
        modifier = Modifier
            .clip(bubbleShape)
            .background(PredictaColors.surface)
            .border(0.5.dp, PredictaColors.lineStrong, bubbleShape)
            .padding(horizontal = PredictaDimensions.Spacing.base, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(offset0, offset1, offset2).forEach { offset ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = offset.dp)
                    .background(PredictaColors.cream35, CircleShape),
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
            voice.listening -> PredictaColors.coral
            isFocused -> PredictaColors.amberEdge
            else -> PredictaColors.lineStrong
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
    val inputShape = RoundedCornerShape(PredictaDimensions.Radius.lg)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, PredictaColors.charcoal),
                )
            )
            .padding(
                start = PredictaDimensions.Spacing.base,
                end = PredictaDimensions.Spacing.base,
                top = PredictaDimensions.Spacing.md,
                bottom = PredictaDimensions.Spacing.sm,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(inputShape)
                .background(PredictaColors.surfaceHigh)
                .border(1.5.dp, borderColor, inputShape)
                .padding(start = PredictaDimensions.Spacing.base, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Escanear ticket",
                tint = PredictaColors.cream35,
                modifier = Modifier
                    .padding(bottom = 9.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAttach),
            )

            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = PredictaDimensions.Spacing.sm)
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = PredictaTypography.body.copy(color = PredictaColors.cream),
                cursorBrush = SolidColor(PredictaColors.amber),
                maxLines = 4,
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            text = if (voice.listening) "Escuchando…" else "Ej: gasté 8.400 en Uber",
                            style = PredictaTypography.body,
                            color = if (voice.listening) PredictaColors.coral else PredictaColors.cream35,
                        )
                    }
                    inner()
                },
            )

            // Un solo botón, estilo WhatsApp: mic con campo vacío, stop mientras
            // escucha, flecha de enviar cuando hay texto.
            val showMic = voice.isAvailable && text.isBlank() && !voice.listening
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when {
                            voice.listening -> PredictaColors.coral.copy(alpha = micPulse)
                            text.isNotBlank() -> PredictaColors.amber
                            else -> PredictaColors.surface
                        },
                        CircleShape,
                    )
                    .clickable(
                        enabled = voice.listening || text.isNotBlank() || showMic,
                        onClick = { if (voice.listening || showMic) voice.toggle() else onSend() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        voice.listening -> Icons.Filled.Stop
                        showMic -> Icons.Filled.Mic
                        else -> Icons.Filled.ArrowUpward
                    },
                    contentDescription = when {
                        voice.listening -> "Detener dictado"
                        showMic -> "Dictar gasto"
                        else -> "Enviar"
                    },
                    tint = when {
                        voice.listening -> PredictaColors.cream
                        showMic -> PredictaColors.cream60
                        text.isNotBlank() -> PredictaColors.onAmber
                        else -> PredictaColors.cream35
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
