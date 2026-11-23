package app.quitter.ui.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quitter.QuitterApplication
import app.quitter.R
import app.quitter.model.CalendarDay
import app.quitter.model.DayState
import app.quitter.model.Habit
import app.quitter.model.HabitKind
import app.quitter.model.MonthSavings
import app.quitter.model.REASON_MAX_LENGTH
import app.quitter.model.Reason
import app.quitter.model.Slip
import app.quitter.model.calendarMonth
import app.quitter.model.progressAt
import app.quitter.model.savingsByMonth
import app.quitter.model.trackingSince
import app.quitter.ui.UserMessage
import app.quitter.ui.clockTicker
import app.quitter.ui.home.ElapsedText
import app.quitter.ui.home.LogSlipSheet
import app.quitter.ui.home.isWorthMentioning
import app.quitter.ui.home.streakLength
import app.quitter.ui.theme.QuitterTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitDetailRoute(habitId: Long, onBack: () -> Unit, onUrge: () -> Unit, onEdit: () -> Unit) {
  val context = LocalContext.current
  val resources = LocalResources.current
  val app = context.applicationContext as QuitterApplication
  val viewModel: HabitDetailViewModel = viewModel {
    HabitDetailViewModel(habitId, app.habitRepository, clockTicker(), createSavedStateHandle())
  }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  var slipSheetOpen by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is HabitDetailEvent.SlipLogged -> snackbarHostState.showSnackbar(
          if (event.longestStreakSeconds.isWorthMentioning()) {
            resources.getString(R.string.slip_logged, resources.streakLength(event.longestStreakSeconds))
          } else {
            resources.getString(R.string.slip_logged_short)
          },
        )
      }
    }
  }
  // The urge timer reports back here when it was opened from this screen.
  LaunchedEffect(Unit) {
    app.userMessages.messages.collect { message ->
      when (message) {
        is UserMessage.UrgeOutlasted -> snackbarHostState.showSnackbar(
          resources.getQuantityString(R.plurals.urge_outlasted, message.countThisMonth, message.countThisMonth),
        )
      }
    }
  }

  when (val state = uiState) {
    HabitDetailUiState.Loading -> Box(Modifier.fillMaxSize())
    HabitDetailUiState.Gone -> LaunchedEffect(Unit) { onBack() }
    is HabitDetailUiState.Ready -> {
      HabitDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelectTab = viewModel::selectTab,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onAddReason = viewModel::addReason,
        onUpdateReason = viewModel::updateReason,
        onDeleteReason = viewModel::deleteReason,
        onUrge = onUrge,
        onLogSlip = { slipSheetOpen = true },
        onEdit = onEdit,
        onDelete = viewModel::deleteHabit,
      )
      if (slipSheetOpen) {
        LogSlipSheet(
          kind = state.habit.kind,
          onDismiss = { slipSheetOpen = false },
          onConfirm = viewModel::logSlip,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
  state: HabitDetailUiState.Ready,
  snackbarHostState: SnackbarHostState,
  onBack: () -> Unit,
  onSelectTab: (DetailTab) -> Unit,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onAddReason: (String) -> Unit,
  onUpdateReason: (Long, String) -> Unit,
  onDeleteReason: (Long) -> Unit,
  onUrge: () -> Unit,
  onLogSlip: () -> Unit,
  onEdit: () -> Unit = {},
  onDelete: () -> Unit = {},
) {
  val colors = MaterialTheme.colorScheme
  var menuOpen by remember { mutableStateOf(false) }
  var confirmDelete by rememberSaveable { mutableStateOf(false) }
  Scaffold(
    containerColor = colors.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {},
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
          }
        },
        actions = {
          IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.detail_more))
          }
          DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.edit_habit_title)) },
              leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
              onClick = { menuOpen = false; onEdit() },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.detail_delete), color = colors.error) },
              leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error) },
              onClick = { menuOpen = false; confirmDelete = true },
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
      )
    },
    bottomBar = { ActionBar(onUrge, onLogSlip) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(padding)
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          state.habit.name,
          style = MaterialTheme.typography.headlineMedium,
          color = colors.onSurface,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          stringResource(R.string.detail_tracking_since, formatDate(state.trackingSince)),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
        )
      }
      ElapsedText(
        state.progress.elapsedSeconds,
        color = colors.primary,
        daysSize = 46.sp,
        partSize = 26.sp,
        textAlign = TextAlign.Start,
      )
      PrimaryTabRow(selectedTabIndex = state.tab.ordinal, containerColor = colors.background) {
        DetailTab.entries.forEach { tab ->
          Tab(
            selected = tab == state.tab,
            onClick = { onSelectTab(tab) },
            text = { Text(stringResource(tab.title)) },
            unselectedContentColor = colors.onSurfaceVariant,
          )
        }
      }
      if (confirmDelete) {
        AlertDialog(
          onDismissRequest = { confirmDelete = false },
          title = { Text(stringResource(R.string.detail_delete_title, state.habit.name)) },
          text = { Text(stringResource(R.string.detail_delete_body)) },
          confirmButton = {
            TextButton(onClick = { confirmDelete = false; onDelete() }) {
              Text(stringResource(R.string.action_delete), color = colors.error)
            }
          },
          dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.slip_cancel)) } },
        )
      }
      when (state.tab) {
        DetailTab.Stats -> StatsTab(state)
        DetailTab.Calendar -> CalendarCard(state.calendar, onPreviousMonth, onNextMonth)
        DetailTab.Reasons -> ReasonsTab(state.reasons, onAddReason, onUpdateReason, onDeleteReason)
      }
    }
  }
}

private val DetailTab.title: Int
  get() = when (this) {
    DetailTab.Stats -> R.string.detail_tab_stats
    DetailTab.Calendar -> R.string.detail_tab_calendar
    DetailTab.Reasons -> R.string.detail_tab_reasons
  }

@Composable
private fun ActionBar(onUrge: () -> Unit, onLogSlip: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = Modifier
      .background(colors.background)
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Button(onClick = onUrge, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
      Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(19.dp))
      Text(stringResource(R.string.detail_urge_timer), Modifier.padding(start = 8.dp))
    }
    FilledIconButton(
      onClick = onLogSlip,
      modifier = Modifier.size(48.dp),
      shape = RoundedCornerShape(24.dp),
      colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
    ) {
      Icon(Icons.Rounded.RestartAlt, stringResource(R.string.home_log_slip))
    }
  }
}

@Composable
private fun StatsTab(state: HabitDetailUiState.Ready) {
  val habit = state.habit
  val money = state.progress.moneyKept
  val hours = state.progress.hoursBack
  if (money == null && hours == null) {
    Text(
      stringResource(R.string.detail_no_stats),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return
  }
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
      if (money != null && habit.costPerDay != null) {
        StatTile(
          stringResource(R.string.detail_money_kept),
          formatMoney(money),
          stringResource(R.string.detail_cost_per_day, formatMoney(habit.costPerDay, decimals = 2)),
          Modifier.weight(1f),
        )
      }
      if (hours != null && habit.minutesPerDay != null) {
        StatTile(
          stringResource(R.string.detail_time_back),
          stringResource(R.string.home_hours_short, hours),
          stringResource(R.string.detail_minutes_per_day, habit.minutesPerDay),
          Modifier.weight(1f),
        )
      }
    }
    if (state.savings.isNotEmpty()) SavingsChart(state.savings)
  }
}

@Composable
private fun StatTile(label: String, value: String, rate: String, modifier: Modifier) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(colors.surfaceContainer, MaterialTheme.shapes.large)
      .padding(14.dp)
      .semantics(mergeDescendants = true) {},
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    Text(value, fontFamily = FontFamily.Monospace, fontSize = 21.sp, color = colors.onSurface)
    Text(rate, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
  }
}

@Composable
private fun SavingsChart(savings: List<MonthSavings>) {
  val colors = MaterialTheme.colorScheme
  val max = savings.maxOf { it.total }.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
  val spoken = savings.joinToString(", ") { "${monthName(it.month, TextStyle.FULL)}: ${formatMoney(it.total)}" }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(colors.surfaceContainer, MaterialTheme.shapes.large)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        stringResource(R.string.detail_savings_title),
        style = MaterialTheme.typography.titleSmall,
        color = colors.onSurface,
        modifier = Modifier.weight(1f),
      )
      Text(
        LocalResources.current.getQuantityString(R.plurals.detail_months, savings.size, savings.size),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
      )
    }
    Column(Modifier.clearAndSetSemantics { contentDescription = spoken }) {
      Row(
        modifier = Modifier.fillMaxWidth().height(76.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        savings.forEachIndexed { i, month ->
          val fraction = (month.total.toFloat() / max.toFloat()).coerceIn(0.04f, 1f)
          Box(
            Modifier
              .weight(1f)
              .fillMaxHeight(fraction)
              .background(
                if (i == savings.lastIndex) colors.primary else colors.outlineVariant,
                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
              ),
          )
        }
      }
      Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        savings.forEach {
          Text(
            monthName(it.month, TextStyle.SHORT),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun CalendarCard(calendar: CalendarState, onPrevious: () -> Unit, onNext: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(colors.surfaceContainer, MaterialTheme.shapes.large)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        "${monthName(calendar.month, TextStyle.FULL_STANDALONE)} ${calendar.month.year}",
        style = MaterialTheme.typography.titleMedium,
        color = colors.onSurface,
        modifier = Modifier.weight(1f).semantics { heading() },
      )
      IconButton(onClick = onPrevious, enabled = calendar.canGoBack) {
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.detail_previous_month))
      }
      IconButton(onClick = onNext, enabled = calendar.canGoForward) {
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.detail_next_month))
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.clearAndSetSemantics {}) {
      (0 until 7).forEach { i ->
        Text(
          calendar.firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
          style = MaterialTheme.typography.labelSmall,
          color = colors.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
      }
    }
    calendar.days.chunked(7).forEach { week ->
      Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        (0 until 7).forEach { i ->
          Box(Modifier.weight(1f)) { week.getOrNull(i)?.let { DayCell(it) } }
        }
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 2.dp)) {
      LegendDot(colors.primary, stringResource(R.string.detail_clean))
      LegendDot(colors.errorContainer, stringResource(R.string.detail_slip))
    }
  }
}

@Composable
private fun DayCell(day: CalendarDay) {
  val colors = MaterialTheme.colorScheme
  val (background, content) = when (day.state) {
    DayState.Clean -> colors.primary to colors.onPrimary
    DayState.Slip -> colors.errorContainer to colors.onErrorContainer
    DayState.Untracked -> androidx.compose.ui.graphics.Color.Transparent to colors.onSurfaceVariant
    DayState.Future -> androidx.compose.ui.graphics.Color.Transparent to colors.outline
  }
  val stateText = stringResource(
    when (day.state) {
      DayState.Clean -> R.string.detail_clean
      DayState.Slip -> R.string.detail_slip
      DayState.Untracked -> R.string.detail_day_untracked
      DayState.Future -> R.string.detail_day_future
    },
  )
  val dateText = day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .fillMaxWidth()
      .background(background, CircleShape)
      .then(if (day.isToday) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
      .clearAndSetSemantics { contentDescription = "$dateText, $stateText" },
    contentAlignment = Alignment.Center,
  ) {
    Text("${day.date.dayOfMonth}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = content)
  }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(Modifier.size(10.dp).background(color, CircleShape))
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun ReasonsTab(
  reasons: List<Reason>,
  onAdd: (String) -> Unit,
  onUpdate: (Long, String) -> Unit,
  onDelete: (Long) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  /** The reason being edited; id 0 means a new one. Null when no dialog is open. */
  var editing by rememberSaveable { mutableStateOf<Long?>(null) }
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (reasons.isEmpty()) {
      Text(stringResource(R.string.detail_reasons_empty), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
    reasons.forEach { reason ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(colors.surfaceContainer, MaterialTheme.shapes.large)
          .clickable(onClickLabel = stringResource(R.string.detail_edit_reason)) { editing = reason.id }
          .padding(15.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Text(reason.text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
      }
    }
    val dash = colors.outlineVariant
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .drawBehind {
          drawRoundRect(
            dash,
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
          )
        }
        .clickable { editing = 0L }
        .heightIn(min = 48.dp)
        .padding(15.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.outline, modifier = Modifier.size(20.dp))
      Text(stringResource(R.string.detail_add_reason), style = MaterialTheme.typography.bodyLarge, color = colors.outline)
    }
  }
  editing?.let { id ->
    val existing = reasons.firstOrNull { it.id == id }
    ReasonDialog(
      initial = existing?.text.orEmpty(),
      isNew = existing == null,
      onSave = { text -> if (existing == null) onAdd(text) else onUpdate(id, text); editing = null },
      onDelete = { onDelete(id); editing = null },
      onDismiss = { editing = null },
    )
  }
}

@Composable
private fun ReasonDialog(initial: String, isNew: Boolean, onSave: (String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
  var text by rememberSaveable { mutableStateOf(initial) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(if (isNew) R.string.detail_add_reason else R.string.detail_edit_reason)) },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(REASON_MAX_LENGTH) },
        placeholder = { Text(stringResource(R.string.detail_reason_hint)) },
        supportingText = { Text("${text.length} / $REASON_MAX_LENGTH") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_save)) }
    },
    dismissButton = {
      Row {
        if (!isNew) {
          TextButton(onClick = onDelete) {
            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
          }
        }
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.slip_cancel)) }
      }
    },
  )
}

private fun monthName(month: YearMonth, style: TextStyle) = month.month.getDisplayName(style, Locale.getDefault())

private fun formatDate(instant: Instant): String =
  instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private fun formatMoney(amount: BigDecimal, decimals: Int = 0): String =
  NumberFormat.getCurrencyInstance().apply {
    minimumFractionDigits = decimals
    maximumFractionDigits = decimals
  }.format(amount)

private val previewNow = Instant.parse("2026-11-18T19:42:10Z")
private val previewHabit = Habit(
  id = 1, kind = HabitKind.Smoking, name = "Cigarettes",
  lastDoneAt = Instant.parse("2026-11-04T09:00:00Z"),
  costPerDay = BigDecimal("11.90"), minutesPerDay = 90,
  bestSeconds = 40 * 86_400L, bankedSeconds = 60 * 86_400L,
)
private val previewSlips = listOf(Slip(Instant.parse("2026-11-04T09:00:00Z"), null))

private fun previewState(tab: DetailTab) = HabitDetailUiState.Ready(
  habit = previewHabit,
  trackingSince = previewHabit.trackingSince,
  progress = previewHabit.progressAt(previewNow),
  tab = tab,
  savings = previewHabit.savingsByMonth(previewNow, ZoneId.of("UTC")),
  calendar = CalendarState(
    month = YearMonth.of(2026, 11),
    firstDayOfWeek = DayOfWeek.MONDAY,
    days = calendarMonth(YearMonth.of(2026, 11), DayOfWeek.MONDAY, previewHabit.trackingSince, previewSlips, previewNow, ZoneId.of("UTC")),
    canGoBack = true,
    canGoForward = false,
  ),
  reasons = listOf(Reason(1, "I want to climb four flights without stopping to breathe."), Reason(2, "My daughter asked me to stop.")),
)

@Composable
private fun Preview(tab: DetailTab) {
  QuitterTheme {
    Surface {
      HabitDetailScreen(previewState(tab), SnackbarHostState(), {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {})
    }
  }
}

@Preview(name = "Stats", showBackground = true, widthDp = 360, heightDp = 760)
@Preview(name = "Stats dark", showBackground = true, widthDp = 360, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatsPreview() = Preview(DetailTab.Stats)

@Preview(name = "Calendar", showBackground = true, widthDp = 360, heightDp = 760)
@Preview(name = "Calendar large font", showBackground = true, widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun CalendarPreview() = Preview(DetailTab.Calendar)

@Preview(name = "Reasons", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun ReasonsPreview() = Preview(DetailTab.Reasons)
