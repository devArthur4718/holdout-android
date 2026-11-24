package app.quitter.ui.insights

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quitter.QuitterApplication
import app.quitter.R
import app.quitter.model.EventCount
import app.quitter.model.Habit
import app.quitter.model.HabitKind
import app.quitter.model.StreakEntry
import app.quitter.model.TimeOfDay
import app.quitter.model.hardestTimes
import app.quitter.model.streakHistory
import app.quitter.model.streaks
import app.quitter.ui.clockTicker
import app.quitter.ui.home.EmptyTab
import app.quitter.ui.home.HabitChip
import app.quitter.ui.home.HabitChipRow
import app.quitter.ui.home.streakLength
import app.quitter.ui.theme.QuitterTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsRoute(onAddHabit: () -> Unit, contentPadding: PaddingValues) {
  val app = LocalContext.current.applicationContext as QuitterApplication
  val viewModel: InsightsViewModel = viewModel { InsightsViewModel(app.habitRepository, clockTicker(), app.habitSelection) }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  when (val state = uiState) {
    InsightsUiState.Loading -> Box(Modifier.fillMaxSize())
    InsightsUiState.Empty -> EmptyTab(null, stringResource(R.string.insights_empty), onAddHabit, contentPadding)
    is InsightsUiState.Ready -> InsightsScreen(state, onSelect = viewModel::select, contentPadding = contentPadding)
  }
}

@Composable
fun InsightsScreen(state: InsightsUiState.Ready, onSelect: (Long) -> Unit, contentPadding: PaddingValues = PaddingValues()) {
  val colors = MaterialTheme.colorScheme
  val hardest = state.hardest
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(contentPadding)
      .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (state.chips.size >= 2) HabitChipRow(state.chips, state.selected.id, onSelect)

    SectionTitle(R.string.insights_hardest_title)
    Card {
      Text(
        if (hardest.hardest != null) stringResource(R.string.insights_hardest_time, stringResource(hardest.hardest.label))
        else stringResource(R.string.insights_not_enough),
        style = if (hardest.hardest != null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
        color = if (hardest.hardest != null) colors.onSurface else colors.onSurfaceVariant,
      )
      Legend()

      SubTitle(R.string.insights_time_of_day)
      val partMax = hardest.byTimeOfDay.values.maxOf { maxOf(it.slips, it.urges) }
      TimeOfDay.entries.forEach { part -> BarRow(stringResource(part.label), hardest.byTimeOfDay.getValue(part), partMax) }

      SubTitle(R.string.insights_weekday)
      val dayMax = hardest.byWeekday.maxOf { (_, count) -> maxOf(count.slips, count.urges) }
      hardest.byWeekday.forEach { (day, count) ->
        BarRow(day.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()), count, dayMax)
      }
    }

    SectionTitle(R.string.insights_streaks_title)
    Card {
      state.history.forEachIndexed { i, entry ->
        StreakRow(entry)
        if (i < state.history.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
      }
      if (state.history.size == 1) {
        Text(stringResource(R.string.streak_only_current), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
      }
    }
  }
}

private val TimeOfDay.label: Int
  @StringRes get() = when (this) {
    TimeOfDay.Morning -> R.string.time_morning
    TimeOfDay.Afternoon -> R.string.time_afternoon
    TimeOfDay.Evening -> R.string.time_evening
    TimeOfDay.Night -> R.string.time_night
  }

@Composable
private fun SectionTitle(@StringRes text: Int) {
  Text(
    stringResource(text),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.padding(top = 8.dp).semantics { heading() },
  )
}

@Composable
private fun SubTitle(@StringRes text: Int) {
  Text(
    stringResource(text),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 8.dp).semantics { heading() },
  )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) { content() }
}

private val slipColor @Composable get() = MaterialTheme.colorScheme.primary
private val urgeColor @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

@Composable
private fun Legend() {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.clearAndSetSemantics {}) {
    LegendItem(slipColor, stringResource(R.string.insights_slips))
    LegendItem(urgeColor, stringResource(R.string.insights_urges))
  }
}

@Composable
private fun LegendItem(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(Modifier.size(10.dp).background(color, CircleShape))
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

/** One part of the day or weekday: a slips bar and an urges bar, read aloud as "Evening: 5 slips, 2 urges". */
@Composable
private fun BarRow(label: String, count: EventCount, max: Int) {
  val spoken = stringResource(
    R.string.insights_bar_a11y,
    label,
    pluralStringResource(R.plurals.slips_count, count.slips, count.slips),
    pluralStringResource(R.plurals.urges_count, count.urges, count.urges),
  )
  Row(
    modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(0.38f))
    Column(Modifier.weight(0.62f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Bar(count.slips, max, slipColor)
      Bar(count.urges, max, urgeColor)
    }
  }
}

@Composable
private fun Bar(value: Int, max: Int, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(Modifier.weight(1f).height(8.dp)) {
      if (value > 0 && max > 0) {
        Box(Modifier.fillMaxWidth(value.toFloat() / max).height(8.dp).background(color, RoundedCornerShape(4.dp)))
      }
    }
    Text(
      "$value",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.End,
      modifier = Modifier.widthIn(min = 20.dp),
    )
  }
}

@Composable
private fun StreakRow(entry: StreakEntry) {
  val colors = MaterialTheme.colorScheme
  val streak = entry.streak
  val resources = LocalResources.current
  Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(resources.streakLength(streak.seconds), style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
      if (streak.isCurrent) Tag(stringResource(R.string.streak_current), colors.primary, colors.onPrimary)
      if (entry.isBest) Tag(stringResource(R.string.streak_best), colors.primaryContainer, colors.onPrimaryContainer)
    }
    Text(
      if (streak.end == null) stringResource(R.string.streak_since, formatDate(streak.start))
      else stringResource(R.string.streak_range, formatDate(streak.start), formatDate(streak.end)),
      style = MaterialTheme.typography.bodySmall,
      color = colors.onSurfaceVariant,
    )
  }
}

@Composable
private fun Tag(text: String, background: Color, content: Color) {
  Text(
    text,
    style = MaterialTheme.typography.labelSmall,
    color = content,
    modifier = Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
  )
}

private fun formatDate(instant: Instant): String =
  instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private val previewNow = Instant.parse("2026-11-23T12:00:00Z")
private val previewHabit = Habit(
  id = 1, kind = HabitKind.Smoking, name = "Cigarettes",
  lastDoneAt = Instant.parse("2026-10-20T19:00:00Z"), costPerDay = null, minutesPerDay = null,
  bestSeconds = 40 * 86_400L, bankedSeconds = 45 * 86_400L,
)
private val previewSlips = listOf(Instant.parse("2026-09-15T20:30:00Z"), previewHabit.lastDoneAt)

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 1300)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 2000, fontScale = 2f)
@Composable
private fun InsightsPreview() {
  QuitterTheme {
    Surface {
      InsightsScreen(
        InsightsUiState.Ready(
          chips = listOf(HabitChip(1, "Cigarettes")),
          selected = previewHabit,
          hardest = hardestTimes(previewSlips, listOf(Instant.parse("2026-11-01T21:00:00Z")), ZoneOffset.UTC, DayOfWeek.SUNDAY),
          history = streakHistory(previewHabit.streaks(previewSlips, previewNow)),
        ),
        onSelect = {},
      )
    }
  }
}
