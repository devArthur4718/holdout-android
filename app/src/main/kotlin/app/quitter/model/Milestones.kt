package app.quitter.model

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** A trophy notification: the streak reaches [days] and the user hears about it [at]. */
data class MilestoneAlert(val days: Int, val at: Instant)

/** No notifications from 22:00 until 08:00. */
val QUIET_START: LocalTime = LocalTime.of(22, 0)
val QUIET_END: LocalTime = LocalTime.of(8, 0)

/** Moves a time inside quiet hours to the next 08:00. */
fun outsideQuietHours(at: Instant, zone: ZoneId): Instant {
  val local = at.atZone(zone)
  val time = local.toLocalTime()
  return when {
    time < QUIET_END -> local.with(QUIET_END).toInstant()
    time >= QUIET_START -> local.plusDays(1).with(QUIET_END).toInstant()
    else -> at
  }
}

/** True when the duration is a whole number of years, so it reads better as years. */
fun isWholeYears(days: Int) = days >= 365 && days % 365 == 0

/** The next [count] trophy notifications for the current streak, all after [now]. */
fun Habit.milestoneAlerts(now: Instant, zone: ZoneId, count: Int): List<MilestoneAlert> {
  // A milestone reached at night is still due at 08:00, so start looking far enough back to catch it.
  val quietHours = Duration.between(QUIET_START, QUIET_END).plusDays(1).seconds
  var elapsed = (Duration.between(lastDoneAt, now).seconds - quietHours).coerceAtLeast(0)
  val alerts = mutableListOf<MilestoneAlert>()
  while (alerts.size < count) {
    val days = nextMilestoneDays(elapsed)
    val at = outsideQuietHours(lastDoneAt.plus(Duration.ofDays(days.toLong())), zone)
    if (at.isAfter(now)) alerts += MilestoneAlert(days, at)
    elapsed = days * 86_400L
  }
  return alerts
}
