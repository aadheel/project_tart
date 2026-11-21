package com.example.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.example.R
import com.example.alarm.AlarmContract
import com.example.alarm.AlarmForegroundService
import com.example.alarm.AlarmPermissionHelper
import com.example.alarm.AlarmPermissionStatus
import com.example.domain.model.Alarm
import com.example.ui.active.AlarmActivity
import com.example.ui.components.TaktSwitch
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ringingAlarm by AlarmForegroundService.activeRuntime.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.updatePermissions(AlarmPermissionHelper.checkStatus(context))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updatePermissions(AlarmPermissionHelper.checkStatus(context))
    }

    // Without notification permission the ringing alarm's full-screen screen can't appear,
    // so ask once on launch instead of waiting for the user to find the banner.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !AlarmPermissionHelper.checkStatus(context).hasNotificationPermission
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val deletedLabel = stringResource(R.string.home_alarm_deleted)
    val undoLabel = stringResource(R.string.home_undo)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlarm,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                ),
                modifier = Modifier
                    .size(60.dp)
                    .testTag("add_alarm_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.home_add_alarm_cd),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 20.dp)
            ) {
                HomeHeader(
                    nextAlarmCountdown = state.nextAlarmCountdown,
                    isDarkMode = state.isDarkMode,
                    showHeaderDemoAction = state.alarms.isNotEmpty(),
                    onToggleTheme = viewModel::toggleTheme,
                    onTriggerDemo = viewModel::triggerDemoAlarm
                )

                // Fallback for when the alarm screen couldn't open on its own (e.g. notifications
                // were denied): without this the ringing alarm has no way to be stopped.
                AnimatedVisibility(
                    visible = ringingAlarm != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    RingingAlarmBanner(
                        onOpen = {
                            val runtime = AlarmForegroundService.activeRuntime.value
                            if (runtime != null) {
                                context.startActivity(
                                    Intent(context, AlarmActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        AlarmContract.putAlarmExtras(
                                            this,
                                            runtime.alarm,
                                            isDemo = runtime.isDemo
                                        )
                                    }
                                )
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = state.permissions.hasMissingPermission,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PermissionBanner(
                        status = state.permissions,
                        onRequestNotifications = {
                            // The launch-time prompt already asked; if the user said no, Android
                            // won't show the dialog again, so go straight to the settings page.
                            context.startActivity(
                                AlarmPermissionHelper.notificationSettingsIntent(context)
                            )
                        },
                        onOpenExactAlarmSettings = {
                            context.startActivity(AlarmPermissionHelper.exactAlarmSettingsIntent(context))
                        },
                        onOpenFullScreenSettings = {
                            context.startActivity(AlarmPermissionHelper.fullScreenIntentSettingsIntent(context))
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (state.alarms.isEmpty()) {
                    HomeEmptyState(
                        onRunDemo = viewModel::triggerDemoAlarm,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.alarms,
                            key = { it.id }
                        ) { alarm ->
                            SwipeableAlarmCard(
                                alarm = alarm,
                                onClick = { onEditAlarm(alarm.id) },
                                onToggle = { enabled -> viewModel.toggleAlarm(alarm, enabled) },
                                onDelete = {
                                    viewModel.deleteAlarm(alarm)
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedLabel,
                                            actionLabel = undoLabel,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete()
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
}

@Composable
private fun HomeHeader(
    nextAlarmCountdown: String?,
    isDarkMode: Boolean,
    showHeaderDemoAction: Boolean,
    onToggleTheme: () -> Unit,
    onTriggerDemo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (nextAlarmCountdown != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showHeaderDemoAction) {
                    IconButton(
                        onClick = onTriggerDemo,
                        modifier = Modifier.testTag("header_demo_alarm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = stringResource(R.string.home_demo_alarm_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = stringResource(R.string.home_toggle_theme_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = nextAlarmCountdown ?: stringResource(R.string.home_no_active_alarms),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal
            ),
            color = if (nextAlarmCountdown != null) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.testTag("next_alarm_countdown")
        )
    }
}

@Composable
private fun PermissionBanner(
    status: AlarmPermissionStatus,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Show every missing permission at once, each with its own button, so a user who fixes
    // one isn't left unaware that alarms still won't work because of another.
    val missing = buildList {
        if (!status.hasNotificationPermission) {
            add(R.string.perm_notifications_desc to onRequestNotifications)
        }
        if (!status.canScheduleExactAlarms) {
            add(R.string.perm_exact_alarm_desc to onOpenExactAlarmSettings)
        }
        if (!status.canUseFullScreenIntent) {
            add(R.string.perm_full_screen_desc to onOpenFullScreenSettings)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission_banner"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.perm_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                missing.forEach { (descriptionRes, onAction) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onAction,
                            modifier = Modifier.testTag("permission_enable_button")
                        ) {
                            Text(
                                text = stringResource(R.string.perm_action_enable),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RingingAlarmBanner(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ringing_alarm_banner"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_ringing_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = stringResource(R.string.home_ringing_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onOpen,
                modifier = Modifier.testTag("ringing_alarm_open_button")
            ) {
                Text(
                    text = stringResource(R.string.home_ringing_action),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableAlarmCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.home_delete_cd),
                    tint = Color.White
                )
            }
        }
    ) {
        AlarmCard(
            alarm = alarm,
            onClick = onClick,
            onToggle = onToggle
        )
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "alarm_card_border"
    )
    val contentAlpha = if (alarm.isEnabled) 1f else 0.45f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("alarm_card_${alarm.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.alpha(contentAlpha)
                ) {
                    Text(
                        text = alarm.formattedTime12,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alarm.amPmLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 1.sp
                        ),
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                TaktSwitch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    contentDescriptionText = "Toggle ${alarm.formattedTime12} ${alarm.amPmLabel} alarm",
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }

            if (alarm.label.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(contentAlpha)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeekdayIndicatorsRow(
                    repeatDays = alarm.repeatDays,
                    isAlarmEnabled = alarm.isEnabled
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = alarm.mission.badgeLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayIndicatorsRow(
    repeatDays: Set<java.time.DayOfWeek>,
    isAlarmEnabled: Boolean
) {
    if (repeatDays.isEmpty()) {
        Text(
            text = stringResource(R.string.home_once).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Alarm.ORDERED_DAYS.forEach { day ->
            val active = day in repeatDays
            Text(
                text = Alarm.dayInitial(day),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    active && isAlarmEnabled -> MaterialTheme.colorScheme.primary
                    active -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                }
            )
        }
    }
}

@Composable
private fun HomeEmptyState(
    onRunDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.padding(bottom = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Minimalist Braun-inspired clock dial canvas graphic
        Canvas(modifier = Modifier.size(112.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = outlineColor,
                radius = radius - 2.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            for (i in 0 until 12) {
                val angleRad = (i * 30.0 - 90.0) * PI / 180.0
                val isQuarter = i % 3 == 0
                val inner = radius - (if (isQuarter) 16.dp.toPx() else 10.dp.toPx())
                val outer = radius - 6.dp.toPx()
                drawLine(
                    color = if (isQuarter) textColor else outlineColor,
                    start = Offset(
                        center.x + (cos(angleRad) * inner).toFloat(),
                        center.y + (sin(angleRad) * inner).toFloat()
                    ),
                    end = Offset(
                        center.x + (cos(angleRad) * outer).toFloat(),
                        center.y + (sin(angleRad) * outer).toFloat()
                    ),
                    strokeWidth = if (isQuarter) 2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Signature Braun vermilion alarm hand pointing to 10:10
            val handAngle = (-145.0) * PI / 180.0
            drawLine(
                color = accentColor,
                start = center,
                end = Offset(
                    center.x + (cos(handAngle) * (radius * 0.52f)).toFloat(),
                    center.y + (sin(handAngle) * (radius * 0.52f)).toFloat()
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 310.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedButton(
            onClick = onRunDemo,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier.testTag("empty_demo_alarm_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_demo_alarm_button),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
