package app.holdout.model

import java.time.Instant

/**
 * The standard goal ladder, in days. Each step is a trophy, and the next step is the 100% of the
 * progress arc. After 2 years, a goal lands every year (1095, 1460, ...).
 */
val GOAL_DAYS = listOf(1, 7, 30, 60, 90, 180, 365, 545, 730)

internal const val SECONDS_PER_DAY = 86_400L
internal const val DAYS_PER_YEAR = 365

/** The first standard goal strictly after [days] whole days. */
fun nextGoalDays(days: Long): Int =
  GOAL_DAYS.firstOrNull { it > days } ?: ((days / DAYS_PER_YEAR + 1) * DAYS_PER_YEAR).toInt()

/** The last standard goal reached at [days] whole days, or 0 when none is reached yet. */
fun previousGoalDays(days: Long): Int =
  if (days < GOAL_DAYS.last()) GOAL_DAYS.lastOrNull { it <= days } ?: 0 else (days / DAYS_PER_YEAR * DAYS_PER_YEAR).toInt()

/** True when [days] is a step of the standard ladder, including the yearly ones after 2 years. */
fun isStandardGoal(days: Int): Boolean = days in GOAL_DAYS || (days > GOAL_DAYS.last() && days % DAYS_PER_YEAR == 0)

/** A goal the user set for one habit: a trophy at [days], with an optional [name]. */
data class PersonalGoal(
  val id: Long,
  val habitId: Long,
  val days: Int,
  val name: String?,
  val createdAt: Instant,
)

/** A step of the combined ladder: a standard goal ([personal] is null) or a personal one. */
data class Goal(val days: Int, val personal: PersonalGoal? = null)

/** The first goal, standard or personal, strictly after [days] whole days. */
fun nextGoal(days: Long, personal: List<PersonalGoal>): Goal {
  val standard = nextGoalDays(days)
  val mine = personal.filter { it.days > days }.minByOrNull { it.days }
  return if (mine != null && mine.days < standard) Goal(mine.days, mine) else Goal(standard)
}

/** The last goal, standard or personal, reached at [days] whole days, or 0 days when none is. */
fun previousGoal(days: Long, personal: List<PersonalGoal>): Goal {
  val standard = previousGoalDays(days)
  val mine = personal.filter { it.days <= days }.maxByOrNull { it.days }
  return if (mine != null && mine.days > standard) Goal(mine.days, mine) else Goal(standard)
}

/** How a goal is named: "1 day", "1 week", "30 days", "6 months", "1½ years", "3 years". */
sealed interface GoalLabel {
  data class Weeks(val count: Int) : GoalLabel
  data class Days(val count: Int) : GoalLabel
  data class Months(val count: Int) : GoalLabel
  data object YearAndAHalf : GoalLabel
  data class Years(val count: Int) : GoalLabel
}

fun goalLabel(days: Int): GoalLabel = when {
  days == 545 -> GoalLabel.YearAndAHalf
  days >= DAYS_PER_YEAR && days % DAYS_PER_YEAR == 0 -> GoalLabel.Years(days / DAYS_PER_YEAR)
  days == 180 -> GoalLabel.Months(6)
  days < 30 && days % 7 == 0 -> GoalLabel.Weeks(days / 7)
  else -> GoalLabel.Days(days)
}

/** The running time measured against the current goal, as the progress arc shows it. */
data class GoalProgress(
  val elapsedSeconds: Long,
  /** The goal the arc fills up to. */
  val goalDays: Int,
  /** The last goal reached, or 0. */
  val previousGoalDays: Int,
  /** 0 to 1: elapsed time over the whole goal, counted from the quit moment. */
  val fraction: Float,
  /** Set when the goal is one of the user's own. */
  val personal: PersonalGoal? = null,
) {
  val secondsToGoal: Long get() = goalDays * SECONDS_PER_DAY - elapsedSeconds

  /** Whole percent, never rounded up to 100 before the goal is reached. */
  val percent: Int get() = (elapsedSeconds * 100 / (goalDays * SECONDS_PER_DAY)).toInt().coerceIn(0, 100)
}

fun goalProgress(elapsedSeconds: Long, personal: List<PersonalGoal> = emptyList()): GoalProgress {
  val elapsed = elapsedSeconds.coerceAtLeast(0)
  val days = elapsed / SECONDS_PER_DAY
  val goal = nextGoal(days, personal)
  return GoalProgress(
    elapsedSeconds = elapsed,
    goalDays = goal.days,
    previousGoalDays = previousGoal(days, personal).days,
    fraction = (elapsed.toDouble() / (goal.days * SECONDS_PER_DAY)).toFloat().coerceIn(0f, 1f),
    personal = goal.personal,
  )
}

/** A habit whose quit moment is less than a minute old was started just now, not in the past. */
fun isStartedJustNow(elapsedSeconds: Long): Boolean = elapsedSeconds < 60

const val PERSONAL_GOAL_MIN_DAYS = 1
const val PERSONAL_GOAL_MAX_DAYS = 3650
const val PERSONAL_GOAL_NAME_MAX_LENGTH = 40

/** Why a personal goal can't be saved. */
sealed interface PersonalGoalError {
  /** Empty, not a whole number, or outside 1…3650. */
  data object InvalidDays : PersonalGoalError

  /** A standard trophy or another personal goal of the habit is already at these days. */
  data class Duplicate(val days: Int) : PersonalGoalError
}

/**
 * Checks the days typed for a personal goal. [existing] are the habit's goals; [editingId] is the
 * one being edited, which may keep its own days. Null means it can be saved as [parseGoalDays].
 */
fun validatePersonalGoal(daysText: String, existing: List<PersonalGoal>, editingId: Long?): PersonalGoalError? {
  val days = parseGoalDays(daysText)
  return when {
    days == null -> PersonalGoalError.InvalidDays
    isStandardGoal(days) || existing.any { it.days == days && it.id != editingId } -> PersonalGoalError.Duplicate(days)
    else -> null
  }
}

/** The days typed, or null when they aren't a whole number from 1 to 3650. */
fun parseGoalDays(daysText: String): Int? =
  daysText.trim().toIntOrNull()?.takeIf { it in PERSONAL_GOAL_MIN_DAYS..PERSONAL_GOAL_MAX_DAYS }

/** The name as saved: trimmed, cut to 40 characters, and null when empty. */
fun cleanGoalName(name: String): String? = name.trim().take(PERSONAL_GOAL_NAME_MAX_LENGTH).trim().ifEmpty { null }
