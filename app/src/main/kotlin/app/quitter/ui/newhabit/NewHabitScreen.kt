package app.quitter.ui.newhabit

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quitter.QuitterApplication
import app.quitter.R
import app.quitter.model.HabitKind
import app.quitter.ui.theme.QuitterTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

@Composable
fun NewHabitRoute(
  onBack: () -> Unit,
  onStarted: (habitId: Long) -> Unit,
  /** The habit to edit, or null to create one. */
  editingId: Long? = null,
) {
  val app = LocalContext.current.applicationContext as QuitterApplication
  val viewModel: NewHabitViewModel = viewModel {
    NewHabitViewModel(
      repository = app.habitRepository,
      defaultName = { app.resources.getString(it.defaultName) },
      editingId = editingId,
    )
  }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  NewHabitScreen(
    uiState = uiState,
    onBack = onBack,
    onSelectKind = viewModel::selectKind,
    onNameChange = viewModel::updateName,
    onCostChange = viewModel::updateCost,
    onMinutesChange = viewModel::updateMinutes,
    onLastDonePicked = viewModel::setLastDone,
    onStart = { viewModel.start(onSaved = onStarted) },
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewHabitScreen(
  uiState: NewHabitUiState,
  onBack: () -> Unit,
  onSelectKind: (HabitKind) -> Unit,
  onNameChange: (String) -> Unit,
  onCostChange: (String) -> Unit,
  onMinutesChange: (String) -> Unit,
  onLastDonePicked: (LocalDate, LocalTime) -> Unit,
  onStart: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  var showDatePicker by rememberSaveable { mutableStateOf(false) }
  var pickedDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }

  Scaffold(
    modifier = modifier,
    containerColor = colors.background,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(if (uiState.isEditing) R.string.edit_habit_title else R.string.new_habit_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(R.string.new_habit_kind_question),
        style = MaterialTheme.typography.titleSmall,
        color = colors.onSurfaceVariant,
      )

      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HabitKind.entries.forEach { kind ->
          val selected = kind == uiState.kind
          FilterChip(
            selected = selected,
            onClick = { onSelectKind(kind) },
            label = { Text(stringResource(kind.label)) },
            leadingIcon = if (selected) {
              { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null,
          )
        }
      }

      OutlinedTextField(
        value = uiState.name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.new_habit_name)) },
        singleLine = true,
        isError = uiState.name.isBlank(),
        supportingText = if (uiState.name.isBlank()) {
          { Text(stringResource(R.string.new_habit_name_required)) }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
      )

      // Changing this later would rewrite the streak history; a slip is the way to restart.
      if (!uiState.isEditing) {
        LastDoneField(lastDoneAt = uiState.lastDoneAt, onClick = { showDatePicker = true })
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = uiState.costText,
          onValueChange = onCostChange,
          label = { Text(stringResource(R.string.new_habit_cost_per_day)) },
          prefix = { Text(currencySymbol()) },
          placeholder = { Text("0.00") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = uiState.minutesText,
          onValueChange = onMinutesChange,
          label = { Text(stringResource(R.string.new_habit_time_per_day)) },
          suffix = { Text(stringResource(R.string.unit_minutes)) },
          placeholder = { Text("0") },
          singleLine = true,
          isError = uiState.minutesTooHigh,
          supportingText = if (uiState.minutesTooHigh) {
            { Text(stringResource(R.string.new_habit_minutes_too_high)) }
          } else null,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
          modifier = Modifier.weight(1f),
        )
      }

      projectionText(uiState.yearlyCost, uiState.yearlyHours)?.let { text ->
        Surface(color = colors.surfaceContainer, shape = RoundedCornerShape(12.dp)) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
          }
        }
      }

      Button(
        onClick = onStart,
        enabled = uiState.canStart,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .padding(top = 4.dp),
      ) {
        Text(
          stringResource(if (uiState.isEditing) R.string.action_save else R.string.new_habit_start),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }

  if (showDatePicker) {
    LastDoneDatePicker(
      onDismiss = { showDatePicker = false },
      onDatePicked = { date ->
        showDatePicker = false
        pickedDate = date
      },
    )
  }

  pickedDate?.let { date ->
    LastDoneTimePicker(
      onDismiss = { pickedDate = null },
      onTimePicked = { time ->
        pickedDate = null
        onLastDonePicked(date, time)
      },
    )
  }
}

@Composable
private fun LastDoneField(lastDoneAt: Instant?, onClick: () -> Unit) {
  val label = lastDoneAt?.let {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
      .withLocale(Locale.getDefault())
      .format(it.atZone(ZoneId.systemDefault()))
  } ?: stringResource(R.string.new_habit_just_now)

  // A read-only field that opens the pickers when tapped.
  val interaction = remember { MutableInteractionSource() }
  LaunchedEffect(interaction) {
    interaction.interactions.collect { if (it is PressInteraction.Release) onClick() }
  }
  OutlinedTextField(
    value = label,
    onValueChange = {},
    readOnly = true,
    label = { Text(stringResource(R.string.new_habit_last_time)) },
    trailingIcon = { Icon(Icons.Rounded.Event, contentDescription = stringResource(R.string.new_habit_pick_date)) },
    interactionSource = interaction,
    modifier = Modifier.fillMaxWidth(),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastDoneDatePicker(onDismiss: () -> Unit, onDatePicked: (LocalDate) -> Unit) {
  val today = remember { LocalDate.now() }
  val state = rememberDatePickerState(
    initialSelectedDateMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    selectableDates = object : SelectableDates {
      override fun isSelectableDate(utcTimeMillis: Long) =
        !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

      override fun isSelectableYear(year: Int) = year <= today.year
    },
  )
  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          state.selectedDateMillis?.let {
            onDatePicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
          }
        },
        enabled = state.selectedDateMillis != null,
      ) { Text(stringResource(R.string.action_next)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
  ) {
    DatePicker(state = state)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastDoneTimePicker(onDismiss: () -> Unit, onTimePicked: (LocalTime) -> Unit) {
  val context = LocalContext.current
  val now = remember { LocalTime.now() }
  val state = rememberTimePickerState(
    initialHour = now.hour,
    initialMinute = now.minute,
    is24Hour = DateFormat.is24HourFormat(context),
  )
  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(onClick = { onTimePicked(LocalTime.of(state.hour, state.minute)) }) {
        Text(stringResource(R.string.action_ok))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    text = { TimePicker(state = state) },
  )
}

@Composable
private fun projectionText(yearlyCost: BigDecimal?, yearlyHours: Int?): AnnotatedString? {
  val money = yearlyCost?.let {
    NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }.format(it)
  }
  val hours = yearlyHours?.let { pluralStringResource(R.plurals.hours, it, it) }
  val sentence = when {
    money != null && hours != null -> stringResource(R.string.new_habit_projection_both, money, hours)
    money != null -> stringResource(R.string.new_habit_projection_money, money)
    hours != null -> stringResource(R.string.new_habit_projection_time, hours)
    else -> return null
  }
  // The money amount is bold, as in the design.
  return buildAnnotatedString {
    append(sentence)
    if (money != null) {
      val start = sentence.indexOf(money)
      if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface), start, start + money.length)
    }
  }
}

private fun currencySymbol(): String =
  runCatching { Currency.getInstance(Locale.getDefault()).symbol }.getOrDefault("")

@Preview(name = "Light", widthDp = 360, heightDp = 760)
@Preview(name = "Dark", widthDp = 360, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NewHabitScreenPreview() {
  QuitterTheme {
    NewHabitScreen(
      uiState = NewHabitUiState(kind = HabitKind.Smoking, name = "Cigarettes", costText = "11.90", minutesText = "90"),
      onBack = {}, onSelectKind = {}, onNameChange = {}, onCostChange = {},
      onMinutesChange = {}, onLastDonePicked = { _, _ -> }, onStart = {},
    )
  }
}
