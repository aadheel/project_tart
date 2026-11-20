package com.example.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun TactileWheelTimePicker(
    hour24: Int,
    minute: Int,
    onTimeChange: (hour24: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialHour12Index = ((hour24 % 12).let { if (it == 0) 12 else it }) - 1 // 0..11 -> "01".."12"
    val initialAmPmIndex = if (hour24 >= 12) 1 else 0

    var selectedHour12Index by remember(hour24) { mutableIntStateOf(initialHour12Index) }
    var selectedMinuteIndex by remember(minute) { mutableIntStateOf(minute.coerceIn(0, 59)) }
    var selectedAmPmIndex by remember(hour24) { mutableIntStateOf(initialAmPmIndex) }

    val hours = remember { (1..12).map { "%02d".format(it) } }
    val minutes = remember { (0..59).map { "%02d".format(it) } }
    val periods = remember { listOf("AM", "PM") }

    fun emitUpdatedTime(h12Idx: Int, minIdx: Int, periodIdx: Int) {
        val h12 = h12Idx + 1
        val h24 = when {
            periodIdx == 0 && h12 == 12 -> 0
            periodIdx == 0 -> h12
            periodIdx == 1 && h12 == 12 -> 12
            else -> h12 + 12
        }
        onTimeChange(h24, minIdx)
    }

    val itemHeight = 56.dp
    val totalHeight = itemHeight * 3

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.Center
    ) {
        // Precision Swiss hairline framing the active row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
        ) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.TopCenter),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrecisionWheelColumn(
                items = hours,
                initialIndex = initialHour12Index,
                itemHeight = itemHeight,
                circular = true,
                onSelectedIndexChanged = { idx ->
                    if (idx != selectedHour12Index) {
                        selectedHour12Index = idx
                        emitUpdatedTime(idx, selectedMinuteIndex, selectedAmPmIndex)
                    }
                },
                modifier = Modifier
                    .width(96.dp)
                    .testTag("wheel_hour")
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp)
            )

            PrecisionWheelColumn(
                items = minutes,
                initialIndex = minute.coerceIn(0, 59),
                itemHeight = itemHeight,
                circular = true,
                onSelectedIndexChanged = { idx ->
                    if (idx != selectedMinuteIndex) {
                        selectedMinuteIndex = idx
                        emitUpdatedTime(selectedHour12Index, idx, selectedAmPmIndex)
                    }
                },
                modifier = Modifier
                    .width(96.dp)
                    .testTag("wheel_minute")
            )

            PrecisionWheelColumn(
                items = periods,
                initialIndex = initialAmPmIndex,
                itemHeight = itemHeight,
                circular = false,
                isPeriodColumn = true,
                onSelectedIndexChanged = { idx ->
                    if (idx != selectedAmPmIndex) {
                        selectedAmPmIndex = idx
                        emitUpdatedTime(selectedHour12Index, selectedMinuteIndex, idx)
                    }
                },
                modifier = Modifier
                    .width(72.dp)
                    .testTag("wheel_ampm")
            )
        }
    }
}

@Composable
private fun PrecisionWheelColumn(
    items: List<String>,
    initialIndex: Int,
    itemHeight: Dp,
    circular: Boolean,
    onSelectedIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isPeriodColumn: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val count = items.size
    val totalCount = if (circular) count * 200 else count
    val startIndex = if (circular) {
        val midBase = (totalCount / 2) - ((totalCount / 2) % count)
        midBase + initialIndex.coerceIn(0, count - 1)
    } else {
        initialIndex.coerceIn(0, count - 1)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
                kotlin.math.abs((itemInfo.offset + itemInfo.size / 2) - viewportCenter)
            }?.index ?: listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(listState) {
        var lastHapticLogicalIndex = initialIndex
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect { rawIdx ->
                val logicalIdx = ((rawIdx % count) + count) % count
                if (logicalIdx != lastHapticLogicalIndex) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticLogicalIndex = logicalIdx
                }
                onSelectedIndexChanged(logicalIdx)
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(itemHeight * 3)
    ) {
        items(totalCount) { rawIdx ->
            val logicalIdx = ((rawIdx % count) + count) % count
            val isCentered = rawIdx == centerIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[logicalIdx],
                    style = if (isPeriodColumn) {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = if (isCentered) 22.sp else 16.sp,
                            fontWeight = if (isCentered) FontWeight.SemiBold else FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    } else {
                        MaterialTheme.typography.displayMedium.copy(
                            fontSize = if (isCentered) 44.sp else 28.sp,
                            fontWeight = if (isCentered) FontWeight.Medium else FontWeight.Normal
                        )
                    },
                    color = if (isCentered) {
                        if (isPeriodColumn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.alpha(if (isCentered) 1f else 0.35f)
                )
            }
        }
    }
}
