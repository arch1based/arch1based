package com.arch1based.lensy.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arch1based.lensy.LensyViewModel
import com.arch1based.lensy.R
import com.arch1based.lensy.core.LensPlan
import com.arch1based.lensy.core.Mood
import com.arch1based.lensy.core.moodFor
import com.arch1based.lensy.data.Prefs
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

private val DateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val TimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private val Presets = listOf(
    14 to R.string.type_biweekly,
    30 to R.string.type_monthly,
    90 to R.string.type_quarterly,
)

@Composable
fun LensyApp(vm: LensyViewModel = viewModel()) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val p = prefs
        val plan = p?.plan
        when {
            p == null -> Unit
            plan == null -> Onboarding(onDone = vm::setPlan)
            else -> Home(p, plan, vm)
        }
    }
}

@Composable
private fun Onboarding(onDone: (LocalDate, Int) -> Unit) {
    var days by rememberSaveable { mutableIntStateOf(30) }
    var start by remember { mutableStateOf(LocalDate.now()) }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EyeMascot(Mood.FRESH, Modifier.size(170.dp))
        Text(
            stringResource(R.string.onb_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.onb_sub),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        PlanEditor(days, start, onDays = { days = it }, onStart = { start = it })
        Button(
            onClick = { onDone(start, days) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(stringResource(R.string.start_btn), style = MaterialTheme.typography.titleMedium)
        }
        Disclaimer()
    }
}

/** Lens type + "when did you put them in". Shared by onboarding and settings. */
@Composable
private fun PlanEditor(days: Int, start: LocalDate, onDays: (Int) -> Unit, onStart: (LocalDate) -> Unit) {
    var custom by rememberSaveable { mutableStateOf(Presets.none { it.first == days }) }
    var pickDate by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.lens_type_q), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((value, label) in Presets) {
                FilterChip(
                    selected = !custom && days == value,
                    onClick = {
                        custom = false
                        onDays(value)
                    },
                    label = { Text(stringResource(label)) },
                )
            }
            FilterChip(
                selected = custom,
                onClick = { custom = true },
                label = { Text(stringResource(R.string.type_custom)) },
            )
        }
        if (custom) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { onDays((days - 1).coerceAtLeast(1)) }) { Text("−") }
                Text(
                    "$days ${stringResource(R.string.days_unit)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                FilledTonalButton(onClick = { onDays((days + 1).coerceAtMost(365)) }) { Text("+") }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.when_q), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = start == today,
                onClick = { onStart(today) },
                label = { Text(stringResource(R.string.today)) },
            )
            FilterChip(
                selected = start == yesterday,
                onClick = { onStart(yesterday) },
                label = { Text(stringResource(R.string.yesterday)) },
            )
            val other = start != today && start != yesterday
            FilterChip(
                selected = other,
                onClick = { pickDate = true },
                label = { Text(if (other) start.format(DateFormatter) else stringResource(R.string.pick_date)) },
            )
        }
    }

    if (pickDate) {
        PastDateDialog(start, onPick = onStart, onDismiss = { pickDate = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastDateDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The picker works in UTC-midnight millis, so compare against today's date expressed the same way.
    val todayUtc = remember { LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtc
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state)
    }
}

@Composable
private fun Home(p: Prefs, plan: LensPlan, vm: LensyViewModel) {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose { }
    }
    val left = plan.daysLeft(today)
    val mood = moodFor(plan, today)
    val unlocked = p.unlocked(today)
    val haptics = LocalHapticFeedback.current

    var confetti by remember { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    // Ask once per app open when the trial has run out.
    var paywall by remember { mutableStateOf(!unlocked) }

    val wasPurchased = remember { p.purchased }
    LaunchedEffect(p.purchased) {
        if (p.purchased && !wasPurchased) confetti++
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { settings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                }
            }

            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                ProgressRing(1f - plan.progress(today), moodColor(mood), Modifier.fillMaxSize())
                EyeMascot(mood, Modifier.size(190.dp))
            }

            if (left == 0L) {
                Text(
                    stringResource(R.string.change_today),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = moodColor(mood),
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    abs(left).toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = moodColor(mood),
                )
                Text(
                    stringResource(if (left > 0) R.string.days_left else R.string.days_overdue),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                stringResource(
                    when (mood) {
                        Mood.FRESH -> R.string.mood_fresh
                        Mood.OK -> R.string.mood_ok
                        Mood.TIRED -> R.string.mood_tired
                        Mood.DUE -> R.string.mood_due
                        Mood.OVERDUE -> R.string.mood_overdue
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.next_change, plan.due.format(DateFormatter)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // The button pulses once it is time to change.
            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 1f,
                targetValue = if (left <= 0) 1.04f else 1f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "pulse",
            )
            Button(
                onClick = { confirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    },
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(stringResource(R.string.btn_new), style = MaterialTheme.typography.titleMedium)
            }

            NotificationCard()

            if (!p.purchased) {
                if (unlocked) {
                    AssistChip(
                        onClick = { paywall = true },
                        label = { Text(stringResource(R.string.trial_left, p.trialLeft(today).toInt())) },
                    )
                } else {
                    Card(Modifier.fillMaxWidth().clickable { paywall = true }) {
                        Text(stringResource(R.string.paused_banner), Modifier.padding(16.dp))
                    }
                }
            }
        }
        Confetti(confetti)
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.confirm_q)) },
            confirmButton = {
                Button(onClick = {
                    confirm = false
                    vm.setPlan(today, plan.intervalDays)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    confetti++
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (settings) {
        SettingsSheet(p, plan, vm, onDismiss = { settings = false })
    }
    if (paywall && !p.purchased) {
        Paywall(vm, onDismiss = { paywall = false })
    }
}

/** Android 13+ needs an explicit yes before we may notify; ask from a visible card, not out of the blue. */
@Composable
private fun NotificationCard() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    fun isGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(isGranted()) }
    LifecycleResumeEffect(Unit) {
        granted = isGranted()
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (granted) return
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.notif_permission), Modifier.weight(1f))
            Button(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text(stringResource(R.string.allow))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(p: Prefs, plan: LensPlan, vm: LensyViewModel, onDismiss: () -> Unit) {
    var pickTime by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
            PlanEditor(
                plan.intervalDays,
                plan.start,
                onDays = { vm.setPlan(plan.start, it) },
                onStart = { vm.setPlan(it, plan.intervalDays) },
            )
            Row(
                Modifier.fillMaxWidth().clickable { pickTime = true }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.reminder_time),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    p.remindAt.format(TimeFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (p.purchased) {
                Text(stringResource(R.string.pay_thanks), style = MaterialTheme.typography.bodyMedium)
            }
            Disclaimer()
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(
            initialHour = p.remindAt.hour,
            initialMinute = p.remindAt.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    vm.setRemindAt(LocalTime.of(state.hour, state.minute))
                    pickTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun Paywall(vm: LensyViewModel, onDismiss: () -> Unit) {
    val activity = LocalContext.current as? Activity
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("☕", fontSize = 40.sp) },
        title = { Text(stringResource(R.string.pay_title), textAlign = TextAlign.Center) },
        text = { Text(stringResource(R.string.pay_body)) },
        confirmButton = {
            Button(onClick = { activity?.let { vm.billing.buy(it) } }) {
                Text(stringResource(R.string.pay_btn, vm.billing.price ?: "0,99 €"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pay_later)) } },
    )
}

@Composable
private fun Disclaimer() {
    Text(
        stringResource(R.string.disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
