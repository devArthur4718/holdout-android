package app.holdout.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class DayState {
  /** Before tracking began. */
  Untracked,
  Clean,
  Slip,
  Future,
}

data class CalendarDay(val date: LocalDate, val state: DayState, val isToday: Boolean)

/**
 * The days of [month] for a habit's calendar, with nulls first so day 1 lands under its weekday.
 * A day with any slip is a slip day; a tracked day without one is clean.
 */
fun calendarMonth(
  month: YearMonth,
  firstDayOfWeek: DayOfWeek,
  trackingSince: Instant,
  slips: List<Slip>,
  now: Instant,
  zone: ZoneId,
): List<CalendarDay?> {
  val today = now.atZone(zone).toLocalDate()
  val firstTracked = trackingSince.atZone(zone).toLocalDate()
  val slipDays = slips.mapTo(HashSet()) { it.at.atZone(zone).toLocalDate() }
  val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
  val days = (1..month.lengthOfMonth()).map { day ->
    val date = month.atDay(day)
    val state = when {
      date.isAfter(today) -> DayState.Future
      date.isBefore(firstTracked) -> DayState.Untracked
      date in slipDays -> DayState.Slip
      else -> DayState.Clean
    }
    CalendarDay(date, state, isToday = date == today)
  }
  return List(leading) { null } + days
}

data class MonthSavings(val month: YearMonth, val total: BigDecimal)

/**
 * Money kept in total by the end of each of the last [count] months (the current month up to
 * [now]), oldest first. Months before tracking began are left out. Empty without a daily cost.
 */
fun Habit.savingsByMonth(now: Instant, zone: ZoneId, count: Int = 6): List<MonthSavings> {
  val cost = costPerDay ?: return emptyList()
  val start = trackingSince
  val current = YearMonth.from(now.atZone(zone))
  val first = maxOf(YearMonth.from(start.atZone(zone)), current.minusMonths(count - 1L))
  return generateSequence(first) { it.plusMonths(1) }
    .takeWhile { it <= current }
    .map { month ->
      val end = minOf(month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant(), now)
      val seconds = Duration.between(start, end).seconds.coerceAtLeast(0)
      MonthSavings(month, cost.multiply(BigDecimal(seconds)).divide(BigDecimal(86_400), 2, RoundingMode.HALF_UP))
    }
    .toList()
}
