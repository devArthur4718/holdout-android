package app.holdout.ui.trophies

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.holdout.HoldoutApplication
import app.holdout.R
import app.holdout.model.Habit
import app.holdout.model.HabitKind
import app.holdout.model.PERSONAL_GOAL_MAX_DAYS
import app.holdout.model.PERSONAL_GOAL_NAME_MAX_LENGTH
import app.holdout.model.PersonalGoal
import app.holdout.model.PersonalGoalError
import app.holdout.model.Trophy
import app.holdout.model.goalProgress
import app.holdout.model.streaks
import app.holdout.model.trophies
import app.holdout.ui.ProgressArc
import app.holdout.ui.ProgressArcSize
import app.holdout.ui.WriteErrorDialog
import app.holdout.ui.clockTicker
import app.holdout.ui.goalLabel
import app.holdout.ui.goalName
import app.holdout.ui.home.EmptyTab
import app.holdout.ui.home.HabitChip
import app.holdout.ui.home.HabitChipRow
import app.holdout.ui.home.timeToGo
import app.holdout.ui.theme.HoldoutTheme
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TrophiesRoute(onAddHabit: () -> Unit, contentPadding: PaddingValues) {
  val app = LocalContext.current.applicationContext as HoldoutApplication
  val viewModel: TrophiesViewModel = viewModel { TrophiesViewModel(app.habitRepository, clockTicker(), app.habitSelection) }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val editor by viewModel.editor.collectAsStateWithLifecycle()
  val writeError by viewModel.writeError.collectAsStateWithLifecycle()

  when (val state = uiState) {
    TrophiesUiState.Loading -> Box(Modifier.fillMaxSize())
    TrophiesUiState.Empty -> EmptyTab(null, stringResource(R.string.trophies_empty), onAddHabit, contentPadding)
    is TrophiesUiState.Ready -> TrophiesScreen(
      state = state,
      onSelect = viewModel::select,
      onAddGoal = viewModel::newGoal,
      onEditGoal = viewModel::editGoal,
      contentPadding = contentPadding,
    )
  }
  editor?.let { form ->
    GoalEditorDialog(
      editor = form,
      onDaysChange = viewModel::updateDays,
      onNameChange = viewModel::updateName,
      onSave = viewModel::save,
      onDelete = viewModel::askToDelete,
      onDismiss = viewModel::dismissEditor,
    )
    if (form.confirmingDelete) {
      AlertDialog(
        onDismissRequest = viewModel::cancelDelete,
        title = { Text(stringResource(R.string.goal_delete_title)) },
        confirmButton = {
          TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.goal_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.action_cancel)) } },
      )
    }
  }
  WriteErrorDialog(writeError, onDismiss = viewModel::dismissWriteError)
}

@Composable
fun TrophiesScreen(
  state: TrophiesUiState.Ready,
  onSelect: (Long) -> Unit,
  onAddGoal: () -> Unit,
  onEditGoal: (PersonalGoal) -> Unit,
  contentPadding: PaddingValues = PaddingValues(),
) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(contentPadding)
      .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (state.chips.size >= 2) HabitChipRow(state.chips, state.selected.id, onSelect)

    // The next trophy for the current streak, as on Today.
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(colors.surfaceContainer, MaterialTheme.shapes.medium)
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      ProgressArc(goal = state.next, size = ProgressArcSize.Compact, secondaryColor = colors.onSurfaceVariant)
      Text(
        stringResource(R.string.home_next_trophy, LocalResources.current.goalName(state.next.goalDays, state.next.personal)),
        style = MaterialTheme.typography.titleSmall,
        color = colors.onSurface,
        textAlign = TextAlign.Center,
      )
      Text(timeToGo(state.next.secondsToGoal), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }

    Text(
      stringResource(R.string.trophies_all),
      style = MaterialTheme.typography.titleMedium,
      color = colors.onSurface,
      modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
    state.trophies.chunked(3).forEach { row ->
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { trophy ->
          TrophyBadge(
            trophy = trophy,
            onClick = trophy.personal?.let { goal -> { onEditGoal(goal) } },
            modifier = Modifier.weight(1f),
          )
        }
        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
      }
    }

    OutlinedButton(onClick = onAddGoal, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
      Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      Text(stringResource(R.string.goal_add), Modifier.padding(start = 8.dp))
    }
  }
}

@Composable
private fun TrophyBadge(trophy: Trophy, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  val resources = LocalResources.current
  val label = resources.goalLabel(trophy.days)
  val name = trophy.personal?.name
  val earnedOn = trophy.earnedAt?.let { formatDate(it, FormatStyle.MEDIUM) }
  val description = resources.trophyDescription(trophy)
  Column(
    modifier = modifier
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .clearAndSetSemantics {
        contentDescription = description
        if (onClick != null) role = Role.Button
      }
      .padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    // An earned trophy that is also next gets the Next ring around its filled badge.
    val ring = if (trophy.isEarned && trophy.isNext) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier
    Box(Modifier.size(64.dp).then(ring), contentAlignment = Alignment.Center) {
      val circle = Modifier.size(56.dp)
      val badge = when {
        trophy.isEarned -> circle.background(colors.primary, CircleShape)
        trophy.isNext -> circle.border(2.dp, colors.primary, CircleShape)
        else -> circle.border(1.dp, colors.outlineVariant, CircleShape)
      }
      Box(badge, contentAlignment = Alignment.Center) {
        Icon(
          Icons.Rounded.EmojiEvents,
          contentDescription = null,
          tint = when {
            trophy.isEarned -> colors.onPrimary
            trophy.isNext -> colors.primary
            else -> colors.onSurfaceVariant.copy(alpha = 0.6f)
          },
          modifier = Modifier.size(28.dp),
        )
      }
      if (trophy.personal != null) {
        // The personal marker: a small star in the top-right corner.
        Box(
          Modifier
            .align(Alignment.TopEnd)
            .offset(x = 2.dp, y = (-2).dp)
            .size(22.dp)
            .background(colors.surface, CircleShape)
            .border(1.dp, colors.outlineVariant, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Rounded.Star, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
        }
      }
    }
    val textColor = if (trophy.isEarned || trophy.isNext) colors.onSurface else colors.onSurfaceVariant
    Text(
      name ?: label,
      style = MaterialTheme.typography.labelLarge,
      color = textColor,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    if (name != null) Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    if (earnedOn != null) Text(earnedOn, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    if (trophy.isNext) Text(stringResource(R.string.trophy_next_tag), style = MaterialTheme.typography.labelSmall, color = colors.primary)
  }
}

/** "30 days, earned 12 March 2026", "90 days, not earned yet", "Personal goal: Beach trip, 45 days, not earned yet". */
internal fun Resources.trophyDescription(trophy: Trophy): String {
  val label = goalLabel(trophy.days)
  val status = trophy.earnedAt?.let { getString(R.string.trophy_a11y_earned, label, formatDate(it, FormatStyle.LONG)) }
    ?: getString(R.string.trophy_a11y_not_earned, label)
  val personal = trophy.personal
  val text = if (personal == null) status
  else getString(R.string.trophy_a11y_personal, personal.name?.let { "$it, $status" } ?: status)
  return if (trophy.isNext) "$text. ${getString(R.string.trophy_next_tag)}" else text
}

@Composable
private fun GoalEditorDialog(
  editor: GoalEditor,
  onDaysChange: (String) -> Unit,
  onNameChange: (String) -> Unit,
  onSave: () -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  val resources = LocalResources.current
  val error = when (val e = editor.error) {
    null -> null
    PersonalGoalError.InvalidDays -> stringResource(R.string.goal_error_days, NumberFormat.getIntegerInstance().format(PERSONAL_GOAL_MAX_DAYS))
    is PersonalGoalError.Duplicate -> stringResource(R.string.goal_error_duplicate, resources.goalLabel(e.days))
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(if (editor.editingId == null) R.string.goal_new_title else R.string.goal_edit_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = editor.daysText,
          onValueChange = onDaysChange,
          label = { Text(stringResource(R.string.goal_days_label)) },
          placeholder = { Text(stringResource(R.string.goal_days_hint)) },
          singleLine = true,
          isError = error != null,
          supportingText = error?.let { { Text(it) } },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = editor.name,
          onValueChange = onNameChange,
          label = { Text(stringResource(R.string.goal_name_label)) },
          placeholder = { Text(stringResource(R.string.goal_name_hint)) },
          singleLine = true,
          supportingText = { Text("${editor.name.length} / $PERSONAL_GOAL_NAME_MAX_LENGTH") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
    dismissButton = {
      // Wraps at large font sizes instead of pushing a button off the dialog.
      FlowRow(horizontalArrangement = Arrangement.End) {
        if (editor.editingId != null) {
          TextButton(onClick = onDelete) { Text(stringResource(R.string.goal_delete), color = MaterialTheme.colorScheme.error) }
        }
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
      }
    },
  )
}

private fun formatDate(instant: Instant, style: FormatStyle): String =
  instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(style))

private val previewNow = Instant.parse("2026-11-23T12:00:00Z")
private val previewHabit = Habit(
  id = 1, kind = HabitKind.Smoking, name = "Cigarettes",
  lastDoneAt = Instant.parse("2026-10-20T09:00:00Z"), costPerDay = null, minutesPerDay = null,
  bestSeconds = 40 * 86_400L, bankedSeconds = 45 * 86_400L,
)
private val previewGoals = listOf(PersonalGoal(1, 1, 45, "Beach trip", previewNow))

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 1000)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
private fun TrophiesPreview() {
  val streaks = previewHabit.streaks(listOf(Instant.parse("2026-09-15T09:00:00Z"), previewHabit.lastDoneAt), previewNow)
  HoldoutTheme {
    Surface {
      TrophiesScreen(
        TrophiesUiState.Ready(
          chips = listOf(HabitChip(1, "Cigarettes"), HabitChip(2, "Alcohol")),
          selected = previewHabit,
          next = goalProgress(Duration.between(previewHabit.lastDoneAt, previewNow).seconds, previewGoals),
          trophies = trophies(streaks, previewGoals),
          personalGoals = previewGoals,
        ),
        onSelect = {}, onAddGoal = {}, onEditGoal = {},
      )
    }
  }
}
