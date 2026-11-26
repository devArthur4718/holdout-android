package app.holdout.ui

import android.content.res.Configuration
import android.content.res.Resources
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.holdout.R
import app.holdout.model.GoalProgress
import app.holdout.model.goalProgress
import app.holdout.ui.theme.HoldoutTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** How big a [ProgressArc] is drawn: large on the counter and detail screens, compact on a home card. */
enum class ProgressArcSize(
  internal val maxWidth: Dp,
  internal val stroke: Dp,
  internal val bigMax: TextUnit,
  internal val bigMin: TextUnit,
  internal val showEnds: Boolean,
) {
  Large(maxWidth = 280.dp, stroke = 14.dp, bigMax = 60.sp, bigMin = 18.sp, showEnds = true),
  Compact(maxWidth = 210.dp, stroke = 10.dp, bigMax = 44.sp, bigMin = 14.sp, showEnds = false),
}

/** The arc opens at the bottom: it starts 20° below the left side and ends 20° below the right. */
private const val START_ANGLE = 160f
private const val SWEEP = 220f
/** Changes smaller than this are drawn at once instead of animated. */
private const val SNAP_BELOW = 0.005f
private const val BELOW_CENTRE = 0.342f // sin(20°)

/**
 * A habit's running time, inside a semicircle that fills up to the next goal (the next trophy).
 * Under a day it shows the clock; after that, the days large and the hours and minutes below.
 * Read aloud as one sentence: "755 days, 13 hours and 36 minutes. 69% of the way to 3 years."
 */
@Composable
fun ProgressArc(
  goal: GoalProgress,
  modifier: Modifier = Modifier,
  size: ProgressArcSize = ProgressArcSize.Large,
  progressColor: Color = MaterialTheme.colorScheme.primary,
  trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
  contentColor: Color = MaterialTheme.colorScheme.primary,
  secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  val resources = LocalResources.current
  val goalName = resources.goalName(goal.goalDays, goal.personal)
  val progressLine = resources.getString(
    R.string.arc_progress,
    NumberFormat.getPercentInstance().format(goal.percent / 100.0),
    goalName,
  )
  val description = resources.getString(R.string.arc_description, resources.spokenDuration(goal.elapsedSeconds), progressLine)

  // Each tick moves the arc by a hair: those changes snap, so it isn't animating all the time.
  // Bigger jumps (a new goal, a slip) are animated, unless the system's animations are off.
  val reduceMotion = rememberReduceMotion()
  val arc = remember { Animatable(goal.fraction) }
  LaunchedEffect(goal.fraction, reduceMotion) {
    val target = goal.fraction
    if (reduceMotion || abs(target - arc.value) < SNAP_BELOW) arc.snapTo(target) else arc.animateTo(target, tween(durationMillis = 700))
  }

  BoxWithConstraints(modifier.clearAndSetSemantics { contentDescription = description }, contentAlignment = Alignment.TopCenter) {
    val width = minOf(maxWidth, size.maxWidth)
    val stroke = size.stroke
    val radius = (width - stroke) / 2
    val arcHeight = stroke + radius * (1 + BELOW_CENTRE)
    // The text sits inside the arc and is kept narrower than its inside, so it can't touch the stroke.
    val top = stroke + radius * 0.35f
    val textWidth = width * 0.6f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(Modifier.width(width), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.size(width, arcHeight)) {
          val s = stroke.toPx()
          val r = radius.toPx()
          val topLeft = Offset(s / 2, s / 2)
          val box = Size(2 * r, 2 * r)
          drawArc(trackColor, START_ANGLE, SWEEP, useCenter = false, topLeft = topLeft, size = box, style = Stroke(s, cap = StrokeCap.Round))
          // A dot stays visible right after the quit moment, so the arc never looks broken.
          val sweep = (SWEEP * arc.value.coerceIn(0f, 1f)).coerceAtLeast(0.5f)
          drawArc(progressColor, START_ANGLE, sweep, useCenter = false, topLeft = topLeft, size = box, style = Stroke(s, cap = StrokeCap.Round))
        }
        // If large text needs more room than the arc has, the column grows below the arc's open
        // bottom instead of overlapping anything.
        Column(
          modifier = Modifier
            .padding(top = top)
            .width(textWidth)
            .heightIn(min = arcHeight - top),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          ArcCentre(goal.elapsedSeconds, size, contentColor, secondaryColor)
        }
      }
      if (size.showEnds) {
        Row(Modifier.width(width).padding(top = 2.dp), verticalAlignment = Alignment.Top) {
          Text("0", style = MaterialTheme.typography.labelMedium, color = secondaryColor)
          Spacer(Modifier.weight(1f))
          Text(goalName, style = MaterialTheme.typography.labelMedium, color = secondaryColor, textAlign = TextAlign.End)
        }
      }
      Text(
        text = progressLine,
        style = if (size == ProgressArcSize.Large) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
        color = secondaryColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width).padding(top = if (size.showEnds) 4.dp else 6.dp),
      )
    }
  }
}

@Composable
private fun ArcCentre(totalSeconds: Long, size: ProgressArcSize, contentColor: Color, secondaryColor: Color) {
  val days = totalSeconds / 86_400
  val h = (totalSeconds / 3_600 % 24).toInt()
  val m = (totalSeconds / 60 % 60).toInt()
  val s = (totalSeconds % 60).toInt()
  if (days == 0L) {
    BigNumber("%02d:%02d:%02d".format(h, m, s), size, contentColor)
  } else {
    BigNumber(formatDayCount(days), size, contentColor)
    val res = LocalResources.current
    Text(
      res.getQuantityString(R.plurals.arc_days_label, days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
      style = if (size == ProgressArcSize.Large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
      color = contentColor,
      textAlign = TextAlign.Center,
    )
    // One line that shrinks rather than breaking "13 h 36 min" in two.
    BasicText(
      text = res.getString(R.string.arc_hours_minutes, h, m),
      style = (if (size == ProgressArcSize.Large) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall)
        .copy(color = secondaryColor, textAlign = TextAlign.Center),
      maxLines = 1,
      softWrap = false,
      autoSize = TextAutoSize.StepBased(
        minFontSize = 9.sp,
        maxFontSize = (if (size == ProgressArcSize.Large) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall).fontSize,
        stepSize = 0.5.sp,
      ),
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

/** True while the system's animations are off (Remove animations, or animator scale 0). Follows changes live. */
@Composable
private fun rememberReduceMotion(): Boolean {
  val resolver = LocalContext.current.contentResolver
  fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
  var reduce by remember(resolver) { mutableStateOf(read()) }
  DisposableEffect(resolver) {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) {
        reduce = read()
      }
    }
    resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
    reduce = read()
    onDispose { resolver.unregisterContentObserver(observer) }
  }
  return reduce
}

/** One line that shrinks to fit the width, never wraps. */
@Composable
private fun BigNumber(text: String, size: ProgressArcSize, color: Color) {
  BasicText(
    text = text,
    style = MaterialTheme.typography.displayLarge.copy(
      color = color,
      fontWeight = FontWeight.Medium,
      fontFeatureSettings = "tnum",
      letterSpacing = 0.sp,
      textAlign = TextAlign.Center,
      lineHeight = TextUnit.Unspecified,
    ),
    maxLines = 1,
    softWrap = false,
    autoSize = TextAutoSize.StepBased(minFontSize = size.bigMin, maxFontSize = size.bigMax, stepSize = 1.sp),
    modifier = Modifier.fillMaxWidth(),
  )
}

/** The big day count, grouped the way the language writes it: "9,999" in English, "9.999" in Portuguese and Spanish. */
fun formatDayCount(days: Long, locale: Locale = Locale.getDefault()): String = NumberFormat.getIntegerInstance(locale).format(days)

/** "755 days, 13 hours and 36 minutes", "2 hours and 5 minutes" or "4 minutes and 10 seconds". */
fun Resources.spokenDuration(totalSeconds: Long): String {
  val d = (totalSeconds / 86_400).toInt()
  val h = (totalSeconds / 3_600 % 24).toInt()
  val m = (totalSeconds / 60 % 60).toInt()
  val s = (totalSeconds % 60).toInt()
  val days = getQuantityString(R.plurals.days, d, d)
  val hours = getQuantityString(R.plurals.hours, h, h)
  val minutes = getQuantityString(R.plurals.minutes, m, m)
  return when {
    d > 0 -> getString(R.string.arc_join_three, days, hours, minutes)
    h > 0 -> getString(R.string.arc_join_two, hours, minutes)
    else -> getString(R.string.arc_join_two, minutes, getQuantityString(R.plurals.seconds, s, s))
  }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun ProgressArcPreview() {
  HoldoutTheme {
    Surface {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProgressArc(goalProgress(755 * 86_400L + 13 * 3_600 + 36 * 60 + 42))
        ProgressArc(goalProgress(9_999 * 86_400L + 23 * 3_600), size = ProgressArcSize.Compact)
        ProgressArc(goalProgress(5 * 3_600L + 12), size = ProgressArcSize.Compact)
      }
    }
  }
}
