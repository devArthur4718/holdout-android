package app.quitter.ui.counter

import app.quitter.notifications.rememberMilestoneToggle
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.foundation.layout.Row
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quitter.QuitterApplication
import app.quitter.Features
import app.quitter.R
import app.quitter.ui.clockTicker
import app.quitter.ui.theme.QuitterTheme

@Composable
fun CounterRoute(habitId: Long, onGoToHabits: () -> Unit) {
  val app = LocalContext.current.applicationContext as QuitterApplication
  val viewModel: CounterViewModel = viewModel {
    CounterViewModel(habitId, app.habitRepository, clockTicker())
  }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val notifications = rememberMilestoneToggle()
  CounterScreen(
    uiState = uiState,
    onGoToHabits = onGoToHabits,
    notificationsOn = notifications.isOn,
    notificationsBlocked = notifications.blocked,
    onNotify = { notifications.set(true) },
    onOpenSystemSettings = notifications::openSystemSettings,
  )
}

@Composable
fun CounterScreen(
  uiState: CounterUiState,
  onGoToHabits: () -> Unit,
  modifier: Modifier = Modifier,
  notificationsOn: Boolean = false,
  notificationsBlocked: Boolean = false,
  onNotify: () -> Unit = {},
  onOpenSystemSettings: () -> Unit = {},
) {
  val colors = MaterialTheme.colorScheme
  val running = uiState as? CounterUiState.Running
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .safeDrawingPadding()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Centres the content when it fits, scrolls when it doesn't (large fonts).
    Spacer(Modifier.weight(1f))

    Box(
      modifier = Modifier
        .padding(bottom = 8.dp)
        .size(112.dp)
        .background(colors.primaryContainer, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Timer,
        contentDescription = null,
        tint = colors.onPrimaryContainer,
        modifier = Modifier.size(52.dp),
      )
    }

    Text(
      text = stringResource(R.string.counter_running_title),
      style = MaterialTheme.typography.headlineMedium,
      color = colors.onBackground,
      textAlign = TextAlign.Center,
      modifier = Modifier.semantics { heading() },
    )
    Text(
      text = stringResource(R.string.counter_running_body),
      style = MaterialTheme.typography.bodyLarge,
      color = colors.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 280.dp),
    )

    Text(
      text = formatElapsed(running?.elapsedSeconds ?: 0),
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium,
      fontSize = 44.sp,
      color = colors.primary,
      modifier = Modifier.padding(top = 18.dp),
    )
    Text(
      text = running?.habitName.orEmpty(),
      style = MaterialTheme.typography.bodyMedium,
      color = colors.onSurfaceVariant,
    )

    // The best moment to offer trophy notifications: the first trophy was just mentioned.
    when {
      notificationsOn -> Row(
        modifier = Modifier.padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.counter_notify_on), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
      }
      notificationsBlocked -> Column(
        modifier = Modifier.padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(stringResource(R.string.notifications_blocked), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        TextButton(onClick = onOpenSystemSettings) { Text(stringResource(R.string.notifications_open_settings)) }
      }
      else -> OutlinedButton(
        onClick = onNotify,
        modifier = Modifier.padding(top = 16.dp).heightIn(min = 48.dp),
      ) {
        Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.counter_notify), Modifier.padding(start = 8.dp))
      }
    }

    Spacer(Modifier.weight(1f))

    Button(
      onClick = onGoToHabits,
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .padding(top = 28.dp)
        .fillMaxWidth()
        .heightIn(min = 48.dp),
    ) {
      Text(stringResource(R.string.counter_go_to_habits), style = MaterialTheme.typography.labelLarge)
    }
    if (Features.ADS) {
      Text(
        text = stringResource(R.string.counter_ads_note),
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline,
        textAlign = TextAlign.Center,
      )
    }
    Spacer(Modifier.height(4.dp))
  }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 760)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun CounterScreenPreview() {
  QuitterTheme {
    Surface { CounterScreen(CounterUiState.Running("Cigarettes", 42), onGoToHabits = {}) }
  }
}
