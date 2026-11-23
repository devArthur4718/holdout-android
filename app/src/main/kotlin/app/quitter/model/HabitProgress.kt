package app.quitter.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

/** Trophy milestones in days. After a year, a trophy lands every year. */
val MILESTONE_DAYS = listOf(1, 3, 7, 14, 30, 90, 180, 365)

private const val SECONDS_PER_DAY = 86_400L

/** Where a habit stands at a given moment. */
data class HabitProgress(
  val elapsedSeconds: Long,
  /** The longest streak so far, counting the current one. */
  val bestSeconds: Long,
  /** True while the current streak is the longest. */
  val isPersonalBest: Boolean,
  /** Money not spent so far, across all streaks, or null when the habit has no daily cost. */
  val moneyKept: BigDecimal?,
  /** Whole hours not spent so far, across all streaks, or null when the habit has no daily time. */
  val hoursBack: Long?,
  val nextMilestoneDays: Int,
  val secondsToNextMilestone: Long,
  /** 0 to 1: how far from the previous milestone to the next one. */
  val milestoneFraction: Float,
)

fun Habit.progressAt(now: Instant): HabitProgress {
  val elapsed = Duration.between(lastDoneAt, now).seconds.coerceAtLeast(0)
  val previous = previousMilestoneDays(elapsed)
  val next = nextMilestoneDays(elapsed)
  val span = (next - previous) * SECONDS_PER_DAY
  val clean = bankedSeconds + elapsed
  return HabitProgress(
    elapsedSeconds = elapsed,
    bestSeconds = maxOf(bestSeconds, elapsed),
    isPersonalBest = elapsed >= bestSeconds,
    moneyKept = costPerDay?.multiply(BigDecimal(clean))?.divide(BigDecimal(SECONDS_PER_DAY), 2, RoundingMode.HALF_UP),
    hoursBack = minutesPerDay?.let { it.toLong() * clean / SECONDS_PER_DAY / 60 },
    nextMilestoneDays = next,
    secondsToNextMilestone = next * SECONDS_PER_DAY - elapsed,
    milestoneFraction = ((elapsed - previous * SECONDS_PER_DAY).toFloat() / span).coerceIn(0f, 1f),
  )
}

internal fun nextMilestoneDays(elapsedSeconds: Long): Int {
  val days = elapsedSeconds / SECONDS_PER_DAY
  return MILESTONE_DAYS.firstOrNull { it > days } ?: ((days / 365 + 1) * 365).toInt()
}

private fun previousMilestoneDays(elapsedSeconds: Long): Int {
  val days = elapsedSeconds / SECONDS_PER_DAY
  return if (days < 365) MILESTONE_DAYS.lastOrNull { it <= days } ?: 0 else (days / 365 * 365).toInt()
}
