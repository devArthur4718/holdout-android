package app.quitter.model

import androidx.annotation.StringRes
import app.quitter.R
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

enum class HabitKind(
  @StringRes val label: Int,
  @StringRes val defaultName: Int,
  /** Heading on the home counter, e.g. "Smoke-free for". */
  @StringRes val freeFor: Int,
) {
  Smoking(R.string.kind_smoking, R.string.kind_smoking_default_name, R.string.free_for_smoking),
  Alcohol(R.string.kind_alcohol, R.string.kind_alcohol, R.string.free_for_alcohol),
  Sugar(R.string.kind_sugar, R.string.kind_sugar, R.string.free_for_sugar),
  Vaping(R.string.kind_vaping, R.string.kind_vaping, R.string.free_for_vaping),
  SocialMedia(R.string.kind_social_media, R.string.kind_social_media, R.string.free_for_social_media),
  Gambling(R.string.kind_gambling, R.string.kind_gambling, R.string.free_for_gambling),
}

/** A habit the user has filled in but that is not saved yet. */
data class HabitDraft(
  val kind: HabitKind,
  val name: String,
  val lastDoneAt: Instant,
  val costPerDay: BigDecimal?,
  val minutesPerDay: Int?,
)

/** A saved habit. Its counter runs from [lastDoneAt]. */
data class Habit(
  val id: Long,
  val kind: HabitKind,
  val name: String,
  val lastDoneAt: Instant,
  val costPerDay: BigDecimal?,
  val minutesPerDay: Int?,
  /** Longest finished streak. The current run can beat it. */
  val bestSeconds: Long = 0,
  /** Clean time from finished streaks, so money and time kept survive a slip. */
  val bankedSeconds: Long = 0,
)

/** When tracking began. Slips restart the counter immediately, so no time is ever untracked. */
val Habit.trackingSince: Instant get() = lastDoneAt.minusSeconds(bankedSeconds)

/** A logged slip. */
data class Slip(val at: Instant, val trigger: SlipTrigger?)

/** Why the user is quitting, in their own words. Shown on the habit and during urges. */
data class Reason(val id: Long, val text: String)

/** Longest reason text accepted. */
const val REASON_MAX_LENGTH = 200

/**
 * The habit after a slip at [at]: the current run is banked, the longest streak is kept,
 * and the counter starts again from [at].
 */
fun Habit.afterSlip(at: Instant): Habit {
  val run = Duration.between(lastDoneAt, at).seconds.coerceAtLeast(0)
  return copy(
    lastDoneAt = maxOf(at, lastDoneAt),
    bestSeconds = maxOf(bestSeconds, run),
    bankedSeconds = bankedSeconds + run,
  )
}

/** What led to a slip. Optional: people can reset without saying why. */
enum class SlipTrigger(@StringRes val label: Int) {
  Stress(R.string.slip_trigger_stress),
  Social(R.string.slip_trigger_social),
  Boredom(R.string.slip_trigger_boredom),
  Alcohol(R.string.slip_trigger_alcohol),
  NoReason(R.string.slip_trigger_no_reason);

  companion object {
    /** "Alcohol" makes no sense as a trigger for an alcohol habit. */
    fun forKind(kind: HabitKind) = entries.filter { !(kind == HabitKind.Alcohol && it == Alcohol) }
  }
}
