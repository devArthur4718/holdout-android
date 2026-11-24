package app.quitter.ui.home

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.createSavedStateHandle
import app.quitter.Features
import app.quitter.QuitterApplication
import app.quitter.R
import app.quitter.model.Habit
import app.quitter.model.HabitKind
import app.quitter.model.HabitProgress
import app.quitter.model.progressAt
import app.quitter.ui.ProgressArc
import app.quitter.ui.ProgressArcSize
import app.quitter.ui.WriteErrorDialog
import app.quitter.ui.clockTicker
import app.quitter.ui.goalName
import app.quitter.ui.theme.QuitterTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant

@Composable
fun HomeRoute(
  onAddHabit: () -> Unit,
  onUrge: (habitId: Long) -> Unit,
  onOpenDetail: (habitId: Long) -> Unit,
  onCheckIn: () -> Unit,
  snackbarHostState: SnackbarHostState,
  contentPadding: PaddingValues,
) {
  val context = LocalContext.current
  val resources = LocalResources.current
  val app = context.applicationContext as QuitterApplication
  val viewModel: HomeViewModel = viewModel {
    HomeViewModel(app.habitRepository, clockTicker(), createSavedStateHandle())
  }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val writeError by viewModel.writeError.collectAsStateWithLifecycle()
  WriteErrorDialog(writeError, onDismiss = viewModel::dismissWriteError)
  /** The habit whose slip sheet is open, if any. */
  var slipFor by rememberSaveable { mutableStateOf<Long?>(null) }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is HomeEvent.SlipLogged -> snackbarHostState.showSnackbar(
          if (event.longestStreakSeconds.isWorthMentioning()) {
            resources.getString(R.string.slip_logged, resources.streakLength(event.longestStreakSeconds))
          } else {
            resources.getString(R.string.slip_logged_short)
          },
        )
      }
    }
  }

  when (val state = uiState) {
    HomeUiState.Loading -> Box(Modifier.fillMaxSize())
    HomeUiState.Empty -> EmptyTab(
      title = stringResource(R.string.home_empty_title),
      body = stringResource(R.string.home_empty_body),
      onAddHabit = onAddHabit,
      contentPadding = contentPadding,
    )
    is HomeUiState.Ready -> {
      HomeScreen(
        state = state,
        onSelect = viewModel::select,
        onAddHabit = onAddHabit,
        onUrge = { onUrge(state.selected.id) },
        onLogSlip = { slipFor = state.selected.id },
        onOpenDetail = { onOpenDetail(state.selected.id) },
        onCheckIn = onCheckIn,
        contentPadding = contentPadding,
      )
      // The sheet is modal, so the selection can't change while it's open.
      val habit = state.selected
      if (slipFor == habit.id) {
        LogSlipSheet(
          kind = habit.kind,
          onDismiss = { slipFor = null },
          onConfirm = { trigger -> viewModel.logSlip(habit.id, trigger) },
        )
      }
    }
  }
}

@Composable
fun HomeScreen(
  state: HomeUiState.Ready,
  onSelect: (Long) -> Unit,
  onAddHabit: () -> Unit,
  onUrge: () -> Unit,
  onLogSlip: () -> Unit,
  onOpenDetail: () -> Unit,
  onCheckIn: () -> Unit,
  contentPadding: PaddingValues = PaddingValues(),
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(contentPadding)
      .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    HabitChipRow(state.chips, state.selected.id, onSelect, onAddHabit)
    CounterCard(state.selected, state.progress, onUrge, onLogSlip)
    StatTiles(state.progress)
    NextTrophyCard(state.progress, onClick = onOpenDetail)
    if (Features.CHECK_IN) CheckInRow(onClick = onCheckIn)
  }
}

/** A tab with no habit to show: a line or two and the "Add habit" button. */
@Composable
fun EmptyTab(title: String?, body: String, onAddHabit: () -> Unit, contentPadding: PaddingValues) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(contentPadding)
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
  ) {
    if (title != null) Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
      body,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Button(onClick = onAddHabit, modifier = Modifier.heightIn(min = 48.dp)) {
      Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      Text(stringResource(R.string.home_add_habit), Modifier.padding(start = 8.dp))
    }
  }
}

/** One chip per habit; [onAddHabit] adds the "+" button at the end (only on Today). */
@Composable
fun HabitChipRow(chips: List<HabitChip>, selectedId: Long, onSelect: (Long) -> Unit, onAddHabit: (() -> Unit)? = null) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    chips.forEach { chip ->
      FilterChip(
        selected = chip.id == selectedId,
        onClick = { onSelect(chip.id) },
        label = { Text(chip.name, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)) },
        colors = FilterChipDefaults.filterChipColors(
          labelColor = colors.onSurfaceVariant,
          selectedContainerColor = colors.primary,
          selectedLabelColor = colors.onPrimary,
        ),
      )
    }
    if (onAddHabit == null) return@Row
    // A small square to look at, a 48dp target to tap.
    val addHabit = stringResource(R.string.home_add_habit)
    Box(
      modifier = Modifier
        .size(48.dp)
        .clickable(role = Role.Button, onClick = onAddHabit)
        .semantics { contentDescription = addHabit },
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .border(1.dp, colors.outline, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun CounterCard(habit: Habit, progress: HabitProgress, onUrge: () -> Unit, onLogSlip: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(colors.primaryContainer, MaterialTheme.shapes.extraLarge)
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text = stringResource(habit.kind.freeFor).uppercase(),
      style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp),
      color = colors.onPrimaryContainer,
      modifier = Modifier.semantics { heading() },
    )
    ProgressArc(
      goal = progress.goal,
      size = ProgressArcSize.Compact,
      modifier = Modifier.padding(vertical = 6.dp),
      trackColor = colors.onPrimaryContainer.copy(alpha = 0.12f),
      contentColor = colors.onPrimaryContainer,
      secondaryColor = colors.onPrimaryContainer.copy(alpha = 0.85f),
    )
    Text(
      text = if (progress.isPersonalBest || !progress.bestSeconds.isWorthMentioning()) {
        stringResource(R.string.home_personal_best_current)
      } else {
        stringResource(R.string.home_personal_best, LocalResources.current.streakLength(progress.bestSeconds))
      },
      style = MaterialTheme.typography.bodySmall,
      color = colors.onPrimaryContainer.copy(alpha = 0.85f),
      textAlign = TextAlign.Center,
    )
    FlowRow(
      modifier = Modifier.padding(top = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = onUrge, contentPadding = PaddingValues(horizontal = 18.dp)) {
        Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.home_urge), Modifier.padding(start = 8.dp))
      }
      Button(
        onClick = onLogSlip,
        contentPadding = PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.background, contentColor = colors.primary),
      ) {
        Text(stringResource(R.string.home_log_slip))
      }
    }
  }
}

@Composable
private fun StatTiles(progress: HabitProgress) {
  val money = progress.moneyKept
  val hours = progress.hoursBack
  if (money == null && hours == null) return
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
    if (money != null) {
      StatTile(Icons.Rounded.Savings, formatMoney(money), R.string.home_money_kept, Modifier.weight(1f))
    }
    if (hours != null) {
      StatTile(Icons.Rounded.Schedule, stringResource(R.string.home_hours_short, hours), R.string.home_time_back, Modifier.weight(1f))
    }
  }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, @StringRes label: Int, modifier: Modifier) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(colors.surfaceContainer, MaterialTheme.shapes.medium)
      .padding(14.dp)
      .semantics(mergeDescendants = true) {},
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
    Text(value, fontFamily = FontFamily.Monospace, fontSize = 22.sp, lineHeight = 28.sp, color = colors.onSurface)
    Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
  }
}

@Composable
private fun NextTrophyCard(progress: HabitProgress, onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  val goal = progress.goal
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(colors.surfaceContainer, MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .padding(16.dp),
  ) {
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalArrangement = Arrangement.spacedBy(2.dp),
      itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        stringResource(R.string.home_next_trophy, LocalResources.current.goalName(goal.goalDays, goal.personal)),
        style = MaterialTheme.typography.titleSmall,
        color = colors.onSurface,
      )
      Text(timeToGo(goal.secondsToGoal), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
  }
}

@Composable
internal fun timeToGo(seconds: Long): String = when {
  seconds >= 86_400 -> ((seconds + 86_399) / 86_400).toInt().let { pluralStringResource(R.plurals.days_to_go, it, it) }
  seconds >= 3_600 -> ((seconds + 3_599) / 3_600).toInt().let { pluralStringResource(R.plurals.hours_to_go, it, it) }
  else -> ((seconds + 59) / 60).toInt().coerceAtLeast(1).let { pluralStringResource(R.plurals.minutes_to_go, it, it) }
}

@Composable
private fun CheckInRow(onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(colors.surfaceContainer, MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .heightIn(min = 48.dp)
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier
        .size(38.dp)
        .background(colors.primaryContainer, RoundedCornerShape(12.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(Icons.Rounded.Psychology, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(20.dp))
    }
    Column(Modifier.weight(1f)) {
      Text(stringResource(R.string.home_checkin_title), style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
      Text(stringResource(R.string.home_checkin_subtitle), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
  }
}

private fun formatMoney(amount: BigDecimal): String =
  NumberFormat.getCurrencyInstance().apply { maximumFractionDigits = 0 }.format(amount)

private val previewHabit = Habit(
  id = 1,
  kind = HabitKind.Smoking,
  name = "Cigarettes",
  lastDoneAt = Instant.parse("2026-10-30T13:00:00Z"),
  costPerDay = BigDecimal("11.90"),
  minutesPerDay = 90,
)
private val previewState = HomeUiState.Ready(
  chips = listOf(HabitChip(1, "Cigarettes"), HabitChip(2, "Alcohol"), HabitChip(3, "Refined sugar")),
  selected = previewHabit,
  progress = previewHabit.progressAt(Instant.parse("2026-11-17T19:42:10Z")),
)

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 760)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun HomeScreenPreview() {
  QuitterTheme {
    Surface { HomeScreen(previewState, {}, {}, {}, {}, {}, {}) }
  }
}
