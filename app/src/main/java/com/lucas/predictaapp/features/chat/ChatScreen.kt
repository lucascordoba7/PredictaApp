package com.lucas.predictaapp.features.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseExtraction
import com.lucas.predictaapp.data.model.ExpenseSuggestion
import com.lucas.predictaapp.data.repository.ChatRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs
import kotlinx.coroutines.launch

private sealed class ChatMsg {
    data class User(val text: String) : ChatMsg()
    data class Bot(
        val text: String,
        val extraction: ExpenseExtraction? = null,
        val confirmed: Boolean = false,
        val dismissed: Boolean = false,
    ) : ChatMsg()
    data object Thinking : ChatMsg()
}

private val starters = listOf(
    "Gasté \$38.500 en Don Julio",
    "Cafe 1.200 pesos",
    "Uber 8400",
    "Deposité el sueldo \$900k",
)

@Composable
fun ChatScreen() {
    val messages = remember { mutableStateListOf<ChatMsg>() }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun send(text: String) {
        if (text.isBlank()) return
        inputText = ""
        messages.add(ChatMsg.User(text))
        messages.add(ChatMsg.Thinking)

        scope.launch {
            val (extraction, unknownReply) = ChatRepository.extract(text)
            val thinkingIdx = messages.indexOfLast { it is ChatMsg.Thinking }
            if (thinkingIdx >= 0) messages.removeAt(thinkingIdx)

            val botMsg = when (extraction) {
                is ExpenseExtraction.Expense -> ChatMsg.Bot(
                    text = "Encontré este gasto 👇",
                    extraction = extraction,
                )
                is ExpenseExtraction.Income -> ChatMsg.Bot(
                    text = "Anotado como ingreso 👇",
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
            messages.add(botMsg)
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun confirmExtraction(index: Int, extraction: ExpenseExtraction) {
        val msg = messages[index] as? ChatMsg.Bot ?: return
        messages[index] = msg.copy(confirmed = true)
        scope.launch {
            when (extraction) {
                is ExpenseExtraction.Expense -> ExpensesRepository.add(
                    Expense(
                        merchant = extraction.merchant,
                        category = extraction.category,
                        amount = extraction.amount,
                    )
                )
                is ExpenseExtraction.Income -> ExpensesRepository.add(
                    Expense(
                        merchant = extraction.merchant,
                        category = "Ingreso",
                        amount = -extraction.amount,
                    )
                )
                else -> Unit
            }
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
                    horizontal = PredictaDimensions.Spacing.screenPadding,
                    vertical = PredictaDimensions.Spacing.base,
                ),
                verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                items(messages.size) { i ->
                    when (val msg = messages[i]) {
                        is ChatMsg.User -> UserBubble(msg.text)
                        is ChatMsg.Bot -> BotBubble(
                            msg = msg,
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
        )
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PredictaDimensions.Spacing.screenPadding,
                vertical = PredictaDimensions.Spacing.base,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(PredictaColors.amber, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("P", style = PredictaTypography.bodyTight, color = PredictaColors.onAmber)
        }
        Column {
            Text("Predicta", style = PredictaTypography.bodyTight, color = PredictaColors.cream)
            Text("Registrá gastos con texto natural", style = PredictaTypography.caption, color = PredictaColors.cream35)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(
    onStarterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(PredictaDimensions.Spacing.screenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("💬", style = PredictaTypography.scoreHero, textAlign = TextAlign.Center)
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))
        Text(
            text = "Contame un gasto o ingreso",
            style = PredictaTypography.cardTitle,
            color = PredictaColors.cream,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
        Text(
            text = "Lo registro automáticamente.",
            style = PredictaTypography.body,
            color = PredictaColors.cream60,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
        ) {
            starters.forEach { starter ->
                Text(
                    text = starter,
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                    modifier = Modifier
                        .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                        .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.pill))
                        .clickable { onStarterClick(starter) }
                        .padding(
                            horizontal = PredictaDimensions.Spacing.md,
                            vertical = PredictaDimensions.Spacing.sm,
                        ),
                )
            }
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BotBubble(
    msg: ChatMsg.Bot,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onSuggestionClick: (String) -> Unit,
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
                .clip(
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = PredictaDimensions.Radius.bubble,
                        bottomStart = PredictaDimensions.Radius.bubble,
                        bottomEnd = PredictaDimensions.Radius.bubble,
                    )
                )
                .background(PredictaColors.surface)
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
    }
}

@Composable
private fun ExtractionCard(
    extraction: ExpenseExtraction,
    confirmed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (emoji, label, amount, category) = when (extraction) {
        is ExpenseExtraction.Expense -> ExtractionInfo(
            emoji = categoryEmoji(extraction.category),
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
                Text(emoji, style = PredictaTypography.body)
                Column {
                    Text(label, style = PredictaTypography.bodyTight, color = PredictaColors.cream)
                    Text(category, style = PredictaTypography.caption, color = PredictaColors.cream35)
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

private data class ExtractionInfo(
    val emoji: String,
    val label: String,
    val amount: Int,
    val category: String,
)

private fun categoryEmoji(category: String) = when (category.lowercase()) {
    "salidas" -> "🍷"
    "delivery" -> "🛵"
    "transporte" -> "🚗"
    "salud" -> "💊"
    "comida en casa" -> "🛒"
    "tecnología" -> "💻"
    "ropa" -> "👟"
    "entretenimiento" -> "🎬"
    "suscripciones" -> "📱"
    else -> "💸"
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
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alpha",
    )
    Row(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = PredictaDimensions.Radius.bubble,
                    bottomStart = PredictaDimensions.Radius.bubble,
                    bottomEnd = PredictaDimensions.Radius.bubble,
                )
            )
            .background(PredictaColors.surface)
            .padding(horizontal = PredictaDimensions.Spacing.base, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(PredictaColors.cream35.copy(alpha = alpha), CircleShape),
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PredictaColors.surface)
            .padding(
                horizontal = PredictaDimensions.Spacing.base,
                vertical = PredictaDimensions.Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                .background(PredictaColors.surfaceHigh)
                .padding(
                    horizontal = PredictaDimensions.Spacing.base,
                    vertical = PredictaDimensions.Spacing.sm,
                ),
            textStyle = PredictaTypography.body.copy(color = PredictaColors.cream),
            cursorBrush = SolidColor(PredictaColors.amber),
            maxLines = 4,
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text("Ej: gasté 8.400 en Uber", style = PredictaTypography.body, color = PredictaColors.cream35)
                }
                inner()
            },
        )

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (text.isNotBlank()) PredictaColors.amber else PredictaColors.surfaceHigh,
                    CircleShape,
                )
                .clickable(enabled = text.isNotBlank(), onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Enviar",
                tint = if (text.isNotBlank()) PredictaColors.onAmber else PredictaColors.cream35,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
