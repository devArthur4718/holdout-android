package app.quitter.ui.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quitter.R
import app.quitter.ui.theme.QuitterTheme

/** Connects the ViewModel to the screen. Navigation stays outside the ViewModel. */
@Composable
fun WelcomeRoute(
  onAddHabit: () -> Unit,
  viewModel: WelcomeViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  WelcomeScreen(uiState = uiState, onAddHabit = onAddHabit)
}

@Composable
fun WelcomeScreen(
  uiState: WelcomeUiState,
  onAddHabit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .safeDrawingPadding()
      .verticalScroll(rememberScrollState())
      .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .background(colors.primaryContainer, RoundedCornerShape(20.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Timer,
        contentDescription = null,
        tint = colors.onPrimaryContainer,
        modifier = Modifier.size(34.dp),
      )
    }

    Text(
      text = stringResource(R.string.welcome_title),
      style = MaterialTheme.typography.displaySmall,
      color = colors.onBackground,
    )

    Text(
      text = stringResource(R.string.welcome_body),
      style = MaterialTheme.typography.bodyLarge,
      color = colors.onSurfaceVariant,
    )

    Column(
      modifier = Modifier.padding(top = 4.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      uiState.benefits.forEach { benefit ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(
            imageVector = benefit.icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = stringResource(benefit.text),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onBackground,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Pushes the button to the bottom when there is room; scrolls when there isn't.
    Spacer(Modifier.weight(1f))

    Button(
      onClick = onAddHabit,
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .padding(top = 16.dp),
    ) {
      Text(stringResource(R.string.welcome_add_habit), style = MaterialTheme.typography.labelLarge)
    }
  }
}

private val previewState = WelcomeViewModel().uiState.value

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 760)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun WelcomeScreenPreview() {
  QuitterTheme {
    Surface { WelcomeScreen(uiState = previewState, onAddHabit = {}) }
  }
}
