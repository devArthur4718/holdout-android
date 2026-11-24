package app.quitter.ui.urge

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quitter.QuitterApplication
import app.quitter.R
import app.quitter.model.UrgePhase
import app.quitter.ui.WriteErrorDialog
import app.quitter.ui.clockTicker

/**
 * The urge screen is dark in both themes. These come from the design's palette, with the
 * lighter tones used for text so it stays readable on the deep purple.
 */
private object UrgeColors {
  val background = Color(0xFF21005D)
  val onBackground = Color.White
  val accent = Color(0xFFEADDFF)
  val ring = Color(0xFFD0BCFF)
  val track = Color.White.copy(alpha = 0.16f)
}

@Composable
fun UrgeRoute(habitId: Long, onClose: () -> Unit) {
  val app = LocalContext.current.applicationContext as QuitterApplication
  val viewModel: UrgeViewModel = viewModel {
    UrgeViewModel(habitId, app.habitRepository, app.userMessages, clockTicker(), createSavedStateHandle())
  }
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val writeError by viewModel.writeError.collectAsStateWithLifecycle()
  WriteErrorDialog(writeError, onDismiss = viewModel::dismissWriteError)
  UrgeScreen(state, onClose = onClose, onGotThroughIt = { viewModel.gotThroughIt(onClose) })
}

@Composable
fun UrgeScreen(state: UrgeUiState, onClose: () -> Unit, onGotThroughIt: () -> Unit) {
  KeepScreenOnWithLightStatusBarIcons()
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(UrgeColors.background)
      .safeDrawingPadding()
      .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
  ) {
    IconButton(onClick = onClose) {
      Icon(Icons.Rounded.Close, stringResource(R.string.urge_close), tint = UrgeColors.accent)
    }
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(start = 12.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
      Text(
        stringResource(R.string.urge_label).uppercase(),
        color = UrgeColors.ring,
        fontSize = 13.sp,
        letterSpacing = 0.6.sp,
      )
      TimerRing(state)
      Text(
        text = stringResource(
          when (state.phase) {
            UrgePhase.Breathe -> R.string.urge_coach_breathe
            UrgePhase.Peak -> R.string.urge_coach_peak
            UrgePhase.Fading -> R.string.urge_coach_fading
            UrgePhase.Done -> R.string.urge_coach_done
          },
        ),
        color = UrgeColors.onBackground,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        textAlign = TextAlign.Center,
        // Read out when the phase changes, without the user moving focus.
        modifier = Modifier
          .widthIn(max = 280.dp)
          .semantics { liveRegion = LiveRegionMode.Polite },
      )
      state.reason?.let { ReasonCard(it) }
    }
    Button(
      onClick = onGotThroughIt,
      modifier = Modifier
        .padding(start = 12.dp)
        .fillMaxWidth()
        .heightIn(min = 48.dp),
      colors = ButtonDefaults.buttonColors(containerColor = UrgeColors.accent, contentColor = UrgeColors.background),
    ) {
      Text(stringResource(R.string.urge_got_through), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
  }
}

@Composable
private fun ReasonCard(reason: String) {
  Row(
    modifier = Modifier
      .padding(top = 18.dp)
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
      .padding(13.dp)
      .semantics(mergeDescendants = true) {},
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = UrgeColors.accent, modifier = Modifier.size(18.dp))
    Column(Modifier.weight(1f)) {
      Text("“$reason”", color = UrgeColors.accent, fontSize = 14.sp, lineHeight = 20.sp)
      Text(stringResource(R.string.urge_your_reason), color = UrgeColors.accent.copy(alpha = 0.8f), fontSize = 12.sp)
    }
  }
}

@Composable
private fun TimerRing(state: UrgeUiState) {
  // The ring grows with the font size, so the clock always fits inside it.
  val size = 182.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.8f)
  val minutes = state.remainingSeconds / 60
  val seconds = state.remainingSeconds % 60
  val spoken = stringResource(
    R.string.urge_time_left,
    pluralStringResource(R.plurals.minutes, minutes, minutes),
    pluralStringResource(R.plurals.seconds, seconds, seconds),
  )
  Box(
    modifier = Modifier
      .padding(vertical = 18.dp)
      .size(size)
      .clearAndSetSemantics {
        contentDescription = spoken
        progressBarRangeInfo = ProgressBarRangeInfo(state.fraction, 0f..1f)
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val stroke = 12.dp.toPx()
      val inset = stroke / 2
      val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
      drawArc(UrgeColors.track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
      drawArc(
        UrgeColors.ring, -90f, 360f * state.fraction, false, Offset(inset, inset), arcSize,
        style = Stroke(stroke, cap = StrokeCap.Round),
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        "%d:%02d".format(minutes, seconds),
        color = UrgeColors.onBackground,
        fontSize = 44.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
      )
      Text(stringResource(R.string.urge_of_total), color = UrgeColors.accent, fontSize = 12.sp)
    }
  }
}

/** The timer should stay visible, and the dark background needs light status bar icons. */
@Composable
private fun KeepScreenOnWithLightStatusBarIcons() {
  val view = LocalView.current
  DisposableEffect(view) {
    val window = (view.context as? Activity)?.window
    val controller = window?.let { WindowCompat.getInsetsController(it, view) }
    val wasLight = controller?.isAppearanceLightStatusBars
    val wasLightNav = controller?.isAppearanceLightNavigationBars
    controller?.isAppearanceLightStatusBars = false
    controller?.isAppearanceLightNavigationBars = false
    view.keepScreenOn = true
    onDispose {
      view.keepScreenOn = false
      wasLight?.let { controller.isAppearanceLightStatusBars = it }
      wasLightNav?.let { controller.isAppearanceLightNavigationBars = it }
    }
  }
}

@Preview(name = "Start", widthDp = 360, heightDp = 760)
@Preview(name = "Large font", widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
private fun UrgeScreenPreview() {
  UrgeScreen(UrgeUiState(remainingSeconds = 252, reason = "My daughter asked me to stop."), {}, {})
}

@Preview(name = "Done", widthDp = 360, heightDp = 760)
@Composable
private fun UrgeScreenDonePreview() {
  UrgeScreen(UrgeUiState(remainingSeconds = 0), {}, {})
}
