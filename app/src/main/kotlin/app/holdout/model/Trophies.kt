package app.holdout.model

import java.time.Duration
import java.time.Instant

/** One run without the habit: from [start] to a slip at [end], or still going ([end] is null). */
data class Streak(val start: Instant, val end: Instant?, val seconds: Long) {
  val isCurrent: Boolean get() = end == null
}

/**
 * Every streak of the habit, oldest first, the current one last. Tracking began at [trackingSince]
 * ([Habit.trackingSince]); each slip ends a streak and starts the next.
 */
fun Habit.streaks(slips: List<Instant>, now: Instant): List<Streak> {
  val past = mutableListOf<Streak>()
  var start = trackingSince
  slips.sorted().forEach { slip ->
    val end = maxOf(slip, start)
    past += Streak(start, end, Duration.between(start, end).seconds)
    start = end
  }
  // The counter runs from lastDoneAt, which is the last slip (or the start).
  val currentStart = maxOf(lastDoneAt, start)
  return past + Streak(currentStart, null, Duration.between(currentStart, now).seconds.coerceAtLeast(0))
}

/** A trophy in the grid: a standard or personal goal, and when it was earned (null: not yet). */
data class Trophy(
  val days: Int,
  val personal: PersonalGoal?,
  val earnedAt: Instant?,
  /** The current streak's next goal (the arc's 100%). Only one trophy is next; it may already be earned in an earlier streak. */
  val isNext: Boolean,
) {
  val isEarned: Boolean get() = earnedAt != null
}

/**
 * The trophy grid in ladder order. A trophy is earned when any streak, past or current, lasted at
 * least its days; its date is the start of the first such streak plus the days. Standard goals are
 * shown up to the first yearly goal past both the best streak and the next goal; every personal
 * goal is shown.
 */
fun trophies(streaks: List<Streak>, personal: List<PersonalGoal>): List<Trophy> {
  val current = streaks.lastOrNull()
  val currentDays = (current?.seconds ?: 0) / SECONDS_PER_DAY
  val next = nextGoal(currentDays, personal)
  val bestDays = (streaks.maxOfOrNull { it.seconds } ?: 0) / SECONDS_PER_DAY
  val reach = maxOf(bestDays, next.days.toLong())
  val lastShown = ((reach / DAYS_PER_YEAR) + 1) * DAYS_PER_YEAR
  val standard = (GOAL_DAYS + generateSequence(GOAL_DAYS.last() + DAYS_PER_YEAR) { it + DAYS_PER_YEAR }
    .takeWhile { it <= lastShown }).filter { it <= lastShown }

  fun earnedAt(days: Int): Instant? = streaks.firstOrNull { it.seconds >= days * SECONDS_PER_DAY }
    ?.start?.plus(Duration.ofDays(days.toLong()))

  return (standard.map { Goal(it) } + personal.map { Goal(it.days, it) })
    .sortedBy { it.days }
    .map { goal ->
      val earned = earnedAt(goal.days)
      Trophy(goal.days, goal.personal, earned, isNext = goal.days == next.days)
    }
}
