package com.example.ui.active

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Alarm
import com.example.domain.model.MissionType
import com.example.ui.components.MissionInteractivePane
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ActiveAlarmPhase {
    RINGING,
    MISSION,
    COMPLETED
}

private const val GRACE_DURATION_MS = 15_000L

@Composable
fun ActiveAlarmScreen(
    alarm: Alarm,
    isDemo: Boolean,
    onPauseAudio: () -> Unit,
    onResumeAudio: () -> Unit,
    onSnooze: () -> Unit,
    onMissionSolved: () -> Unit,
    onCloseFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phase by remember { mutableStateOf(ActiveAlarmPhase.RINGING) }
    var graceRemainingMs by remember { mutableLongStateOf(GRACE_DURATION_MS) }
    var audioResumedByTimeout by remember { mutableStateOf(false) }

    // Anti-cheat: strictly intercept Back gesture/button while alarm or mission is active
    BackHandler(enabled = phase != ActiveAlarmPhase.COMPLETED) {
        // Intentionally consumed to prevent backing out of an active alarm mission
    }

    // 15-second audio pause countdown while solving mission
    LaunchedEffect(phase) {
        if (phase == ActiveAlarmPhase.MISSION) {
            graceRemainingMs = GRACE_DURATION_MS
            audioResumedByTimeout = false
            onPauseAudio()
            while (phase == ActiveAlarmPhase.MISSION) {
                delay(100L)
                if (graceRemainingMs > 0L) {
                    graceRemainingMs = (graceRemainingMs - 100L).coerceAtLeast(0L)
                    if (graceRemainingMs == 0L && !audioResumedByTimeout) {
                        audioResumedByTimeout = true
                        onResumeAudio()
                    }
                }
            }
        }
    }

    // Auto-close after serene Good Morning confirmation
    LaunchedEffect(phase) {
        if (phase == ActiveAlarmPhase.COMPLETED) {
            delay(2400L)
            onCloseFinished()
        }
    }

    fun handleUserInteraction() {
        graceRemainingMs = GRACE_DURATION_MS
        if (audioResumedByTimeout) {
            audioResumedByTimeout = false
            onPauseAudio()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "active_alarm_phase"
            ) { currentPhase ->
                when (currentPhase) {
                    ActiveAlarmPhase.RINGING -> RingingPhaseContent(
                        alarm = alarm,
                        isDemo = isDemo,
                        onStartMission = {
                            if (alarm.mission.type == MissionType.NONE) {
                                onMissionSolved()
                                phase = ActiveAlarmPhase.COMPLETED
                            } else {
                                phase = ActiveAlarmPhase.MISSION
                            }
                        },
                        onSnooze = onSnooze
                    )

                    ActiveAlarmPhase.MISSION -> MissionPhaseContent(
                        alarm = alarm,
                        graceRemainingMs = graceRemainingMs,
                        audioResumedByTimeout = audioResumedByTimeout,
                        onUserInteraction = ::handleUserInteraction,
                        onCompleted = {
                            onMissionSolved()
                            phase = ActiveAlarmPhase.COMPLETED
                        }
                    )

                    ActiveAlarmPhase.COMPLETED -> GoodMorningPhaseContent(
                        label = alarm.label,
                        onDismissNow = onCloseFinished
                    )
                }
            }
        }
    }
}

@Composable
private fun RingingPhaseContent(
    alarm: Alarm,
    isDemo: Boolean,
    onStartMission: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000L)
        }
    }

    val timeText = remember(currentTime) {
        currentTime.format(DateTimeFormatter.ofPattern("hh:mm", Locale.US))
    }
    val amPmText = remember(currentTime) {
        currentTime.format(DateTimeFormatter.ofPattern("a", Locale.US)).uppercase(Locale.US)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 520.dp)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status pill
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isDemo) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = stringResource(R.string.active_demo_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .size(14.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        // Center oversized Swiss clock readout
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 84.sp,
                    lineHeight = 88.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("active_alarm_clock")
            )
            Text(
                text = amPmText,
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 3.sp),
                color = MaterialTheme.colorScheme.primary
            )

            if (alarm.label.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = alarm.mission.badgeLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // Bottom primary action + strict snooze limit
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartMission,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("start_mission_button")
            ) {
                Text(
                    text = if (alarm.mission.type == MissionType.NONE) {
                        stringResource(R.string.active_dismiss_alarm)
                    } else {
                        stringResource(R.string.active_start_mission)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            if (alarm.maxSnoozes > 0 && alarm.remainingSnoozes > 0) {
                OutlinedButton(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("snooze_button")
                ) {
                    Text(
                        text = "${stringResource(R.string.active_snooze)} · ${alarm.remainingSnoozes} left",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionPhaseContent(
    alarm: Alarm,
    graceRemainingMs: Long,
    audioResumedByTimeout: Boolean,
    onUserInteraction: () -> Unit,
    onCompleted: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = (graceRemainingMs.toFloat() / GRACE_DURATION_MS.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(100),
        label = "grace_progress"
    )
    val secondsLeft = ((graceRemainingMs + 999L) / 1000L).toInt().coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 520.dp)
    ) {
        // Thin, calm 15-second audio pause bar pinned at the top edge
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .testTag("grace_countdown_bar"),
            color = if (audioResumedByTimeout) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.outline
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (audioResumedByTimeout) {
                    stringResource(R.string.active_sound_resumed_label)
                } else {
                    "${stringResource(R.string.active_sound_paused_label)} · ${secondsLeft}s"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (audioResumedByTimeout) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag("grace_status_label")
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                MissionInteractivePane(
                    mission = alarm.mission,
                    onUserInteraction = onUserInteraction,
                    onMissionCompleted = onCompleted
                )
            }
        }
    }
}

@Composable
private fun GoodMorningPhaseContent(
    label: String,
    onDismissNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag("good_morning_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.WbSunny,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.active_good_morning),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = label.ifBlank { stringResource(R.string.active_good_morning_sub) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onDismissNow,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
