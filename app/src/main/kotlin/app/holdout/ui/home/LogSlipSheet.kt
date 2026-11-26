package app.holdout.ui.home

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.holdout.Features
import app.holdout.R
import app.holdout.model.HabitKind
import app.holdout.model.SlipTrigger
import app.holdout.ui.theme.HoldoutTheme
import kotlinx.coroutines.launch

/** Confirms a slip. Nothing happens until "Reset counter". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSlipSheet(kind: HabitKind, onDismiss: () -> Unit, onConfirm: (SlipTrigger?) -> Unit) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  fun close(then: () -> Unit) {
    scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
  }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    LogSlipContent(
      kind = kind,
      onCancel = { close(onDismiss) },
      onConfirm = { trigger ->
        close {
          onConfirm(trigger)
          onDismiss()
        }
      },
    )
  }
}

@Composable
private fun LogSlipContent(kind: HabitKind, onCancel: () -> Unit, onConfirm: (SlipTrigger?) -> Unit) {
  val colors = MaterialTheme.colorScheme
  var trigger by rememberSaveable { mutableStateOf<SlipTrigger?>(null) }
  Column(
    modifier = Modifier
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp)
      .padding(bottom = 20.dp)
      .navigationBarsPadding(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
      stringResource(R.string.slip_title),
      style = MaterialTheme.typography.headlineSmall,
      color = colors.onSurface,
      modifier = Modifier.semantics { heading() },
    )
    Text(stringResource(R.string.slip_body), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    Text(stringResource(R.string.slip_trigger_label), style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SlipTrigger.forKind(kind).forEach { item ->
        FilterChip(
          selected = item == trigger,
          // Tapping the picked one again clears it: the trigger is optional.
          onClick = { trigger = if (item == trigger) null else item },
          label = { Text(stringResource(item.label)) },
        )
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp)) {
      OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
        Text(stringResource(R.string.slip_cancel))
      }
      Button(
        onClick = { onConfirm(trigger) },
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
      ) {
        Text(stringResource(R.string.slip_reset))
      }
    }
    if (Features.ADS) {
      Text(
        stringResource(R.string.slip_no_ads),
        style = MaterialTheme.typography.labelSmall,
        color = colors.outline,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

/** A streak under a minute is too short to call a record. */
fun Long.isWorthMentioning() = this >= 60

/** "18 days", "5 hours" or "12 minutes": the largest whole unit. */
fun Resources.streakLength(seconds: Long): String = when {
  seconds >= 86_400 -> (seconds / 86_400).toInt().let { getQuantityString(R.plurals.days, it, it) }
  seconds >= 3_600 -> (seconds / 3_600).toInt().let { getQuantityString(R.plurals.hours, it, it) }
  else -> (seconds / 60).toInt().let { getQuantityString(R.plurals.minutes, it, it) }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun LogSlipContentPreview() {
  HoldoutTheme { Surface { LogSlipContent(HabitKind.Smoking, {}, {}) } }
}
