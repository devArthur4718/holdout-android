package app.holdout.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

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
  /** The current streak against its next goal (the next trophy). */
  val goal: GoalProgress,
)

/** [personal] are the habit's own goals, which join the standard ladder. */
fun Habit.progressAt(now: Instant, personal: List<PersonalGoal> = emptyList()): HabitProgress {
  val elapsed = Duration.between(lastDoneAt, now).seconds.coerceAtLeast(0)
  val clean = bankedSeconds + elapsed
  return HabitProgress(
    elapsedSeconds = elapsed,
    bestSeconds = maxOf(bestSeconds, elapsed),
    isPersonalBest = elapsed >= bestSeconds,
    moneyKept = costPerDay?.multiply(BigDecimal(clean))?.divide(BigDecimal(SECONDS_PER_DAY), 2, RoundingMode.HALF_UP),
    hoursBack = minutesPerDay?.let { it.toLong() * clean / SECONDS_PER_DAY / 60 },
    goal = goalProgress(elapsed, personal),
  )
}
