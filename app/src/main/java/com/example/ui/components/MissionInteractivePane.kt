package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.mission.MathMissionEngine
import com.example.domain.mission.MemoryGridEngine
import com.example.domain.mission.MemoryTapOutcome
import com.example.domain.mission.TypingMissionEngine
import com.example.domain.model.MissionConfig
import com.example.domain.model.MissionType
import com.example.ui.theme.TaktCodeTextStyle
import com.example.ui.theme.TaktError
import com.example.ui.theme.TaktSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private object NoOpTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden
    override fun hide() = Unit
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) = Unit
}

@Composable
fun MissionInteractivePane(
    mission: MissionConfig,
    onUserInteraction: () -> Unit,
    onMissionCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val normalized = remember(mission) { mission.normalized() }

    when (normalized.type) {
        MissionType.NONE -> {
            LaunchedEffect(Unit) { onMissionCompleted() }
        }
        MissionType.MATH -> MathMissionPane(
            config = normalized,
            onUserInteraction = onUserInteraction,
            onCompleted = onMissionCompleted,
            modifier = modifier
        )
        MissionType.MEMORY_GRID -> MemoryGridMissionPane(
            config = normalized,
            onUserInteraction = onUserInteraction,
            onCompleted = onMissionCompleted,
            modifier = modifier
        )
        MissionType.TYPING -> TypingMissionPane(
            config = normalized,
            onUserInteraction = onUserInteraction,
            onCompleted = onMissionCompleted,
            modifier = modifier
        )
    }
}

@Composable
private fun MathMissionPane(
    config: MissionConfig,
    onUserInteraction: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { MathMissionEngine() }
    val problems = remember(config) { engine.generateBatch(config.mathDifficulty, config.rounds) }
    var currentIndex by remember(config) { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val currentProblem = problems[currentIndex.coerceIn(0, problems.lastIndex)]

    fun triggerErrorShake() {
        isError = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.snapTo(0f)
            listOf(-18f, 18f, -12f, 12f, -6f, 0f).forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(42))
            }
            delay(250)
            isError = false
            input = ""
        }
    }

    fun submitAnswer() {
        onUserInteraction()
        if (engine.validate(currentProblem, input)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isError = false
            input = ""
            if (currentIndex + 1 >= problems.size) {
                onCompleted()
            } else {
                currentIndex += 1
            }
        } else {
            triggerErrorShake()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PROBLEM ${currentIndex + 1} OF ${problems.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("mission_progress_label")
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = currentProblem.expression,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 44.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("math_expression_text")
        )

        Spacer(modifier = Modifier.height(20.dp))

        val answerBorderColor by animateColorAsState(
            targetValue = when {
                isError -> TaktError
                input.isNotEmpty() -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            },
            label = "math_answer_border"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, answerBorderColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = input.ifEmpty { "—" },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (input.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                } else if (isError) {
                    TaktError
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                modifier = Modifier.testTag("math_input_display")
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val keypadRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("DEL", "0", "OK")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keypadRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        val isSubmit = key == "OK"
                        val isBackspace = key == "DEL"
                        Surface(
                            onClick = {
                                onUserInteraction()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                when {
                                    isSubmit -> submitAnswer()
                                    isBackspace -> {
                                        isError = false
                                        if (input.isNotEmpty()) input = input.dropLast(1)
                                    }
                                    else -> {
                                        isError = false
                                        if (input.length < 6) input += key
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isSubmit -> MaterialTheme.colorScheme.primary
                                isBackspace -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = if (isSubmit) null else BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .testTag("math_key_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    isBackspace -> Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete digit",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    isSubmit -> Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Submit answer",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    else -> Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryGridMissionPane(
    config: MissionConfig,
    onUserInteraction: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { MemoryGridEngine() }
    var roundIndex by remember(config) { mutableIntStateOf(0) }
    var challenge by remember(config, roundIndex) {
        mutableStateOf(engine.generate(config.memoryGridSize, roundIndex))
    }
    var isMemorizing by remember(config, roundIndex) { mutableStateOf(true) }
    var selectedIndices by remember(config, roundIndex) { mutableStateOf(emptySet<Int>()) }
    var wrongIndex by remember { mutableStateOf<Int?>(null) }
    var replayToken by remember { mutableIntStateOf(0) }

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(roundIndex, replayToken) {
        isMemorizing = true
        selectedIndices = emptySet()
        wrongIndex = null
        delay(2000L)
        isMemorizing = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ROUND ${roundIndex + 1} OF ${config.rounds}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("mission_progress_label")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isMemorizing) {
                stringResource(R.string.active_memory_watch)
            } else {
                stringResource(R.string.active_memory_recall)
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (isMemorizing) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        val gridSize = challenge.gridSize
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in 0 until gridSize) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0 until gridSize) {
                        val index = row * gridSize + col
                        val isLit = (isMemorizing && index in challenge.targetIndices) ||
                            (!isMemorizing && index in selectedIndices)
                        val isWrong = wrongIndex == index

                        val tileColor by animateColorAsState(
                            targetValue = when {
                                isWrong -> TaktError
                                isLit -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surface
                            },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "memory_tile_$index"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = when {
                                isWrong -> TaktError
                                isLit -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            label = "memory_tile_border_$index"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tileColor)
                                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                .minimumInteractiveComponentSize()
                                .testTag("memory_tile_$index")
                                .clickable(enabled = !isMemorizing && wrongIndex == null) {
                                    onUserInteraction()
                                    when (engine.evaluateTap(challenge, selectedIndices, index)) {
                                        MemoryTapOutcome.ALREADY_SELECTED -> Unit
                                        MemoryTapOutcome.CORRECT_PROGRESS -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedIndices = selectedIndices + index
                                        }
                                        MemoryTapOutcome.ROUND_COMPLETE -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedIndices = selectedIndices + index
                                            scope.launch {
                                                delay(260L)
                                                if (roundIndex + 1 >= config.rounds) {
                                                    onCompleted()
                                                } else {
                                                    roundIndex += 1
                                                    challenge = engine.generate(
                                                        config.memoryGridSize,
                                                        roundIndex
                                                    )
                                                }
                                            }
                                        }
                                        MemoryTapOutcome.WRONG_TILE -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            wrongIndex = index
                                            scope.launch {
                                                delay(480L)
                                                wrongIndex = null
                                                replayToken += 1
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingMissionPane(
    config: MissionConfig,
    onUserInteraction: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { TypingMissionEngine() }
    var roundIndex by remember(config) { mutableIntStateOf(0) }
    var challenge by remember(config, roundIndex) { mutableStateOf(engine.generate(roundIndex)) }
    var typedValue by remember { mutableStateOf("") }
    var pasteWarning by remember { mutableStateOf(false) }
    var errorMismatch by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    fun submitTyped() {
        onUserInteraction()
        if (engine.validate(challenge, typedValue)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            typedValue = ""
            errorMismatch = false
            pasteWarning = false
            if (roundIndex + 1 >= config.rounds) {
                onCompleted()
            } else {
                roundIndex += 1
                challenge = engine.generate(roundIndex)
            }
        } else {
            errorMismatch = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                shakeOffset.snapTo(0f)
                listOf(-16f, 16f, -10f, 10f, 0f).forEach { target ->
                    shakeOffset.animateTo(target, animationSpec = tween(45))
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SEQUENCE ${roundIndex + 1} OF ${config.rounds}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("mission_progress_label")
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = challenge.prompt,
                style = TaktCodeTextStyle,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .testTag("typing_target_prompt")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        val isExactMatch = engine.validate(challenge, typedValue)

        CompositionLocalProvider(LocalTextToolbar provides NoOpTextToolbar) {
            OutlinedTextField(
                value = typedValue,
                onValueChange = { candidate ->
                    onUserInteraction()
                    val upper = candidate.uppercase()
                    if (engine.isPasteAttempt(typedValue, upper)) {
                        pasteWarning = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        pasteWarning = false
                        errorMismatch = false
                        typedValue = upper
                    }
                },
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.active_typing_hint),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                textStyle = TaktCodeTextStyle.copy(
                    fontSize = 18.sp,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                isError = errorMismatch || pasteWarning,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitTyped() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isExactMatch) TaktSuccess else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    errorBorderColor = TaktError,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .testTag("typing_input_field")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                pasteWarning -> stringResource(R.string.active_typing_paste_blocked)
                errorMismatch -> "Exact characters required — check spacing and symbols"
                else -> "Autocorrect & paste disabled"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (pasteWarning || errorMismatch) TaktError else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { submitTyped() },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("typing_submit_button")
        ) {
            Text(
                text = stringResource(R.string.active_submit),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
