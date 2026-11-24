package app.quitter.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/** Parts of the day, by local hour. Night wraps past midnight. */
enum class TimeOfDay(val fromHour: Int) {
  Morning(5),
  Afternoon(12),
  Evening(17),
  Night(22);

  companion object {
    fun of(hour: Int): TimeOfDay = when (hour) {
      in 5..11 -> Morning
      in 12..16 -> Afternoon
      in 17..21 -> Evening
      else -> Night
    }
  }
}

/** How many slips and urges fell in one bar. */
data class EventCount(val slips: Int, val urges: Int) {
  val total: Int get() = slips + urges
}

/** When slips and urges happen, in the phone's local time. */
data class HardestTimes(
  val byTimeOfDay: Map<TimeOfDay, EventCount>,
  /** In the week's order, starting on the locale's first day. */
  val byWeekday: List<Pair<DayOfWeek, EventCount>>,
  /** The busiest part of the day, or null with fewer than [MIN_EVENTS_FOR_TAKEAWAY] events. */
  val hardest: TimeOfDay?,
)

const val MIN_EVENTS_FOR_TAKEAWAY = 3

/**
 * Counts [slips] and [urges] (their start times) by part of the day and weekday, in [zone].
 * The hardest time is the part of the day with the most events; a tie goes to the earlier one.
 */
fun hardestTimes(slips: List<Instant>, urges: List<Instant>, zone: ZoneId, firstDayOfWeek: DayOfWeek): HardestTimes {
  fun local(at: Instant) = at.atZone(zone)
  val slipTimes = slips.map(::local)
  val urgeTimes = urges.map(::local)
  val byTime = TimeOfDay.entries.associateWith { part ->
    EventCount(slipTimes.count { TimeOfDay.of(it.hour) == part }, urgeTimes.count { TimeOfDay.of(it.hour) == part })
  }
  val week = (0L until 7L).map { firstDayOfWeek.plus(it) }
  val byDay = week.map { day ->
    day to EventCount(slipTimes.count { it.dayOfWeek == day }, urgeTimes.count { it.dayOfWeek == day })
  }
  val hardest = if (slips.size + urges.size < MIN_EVENTS_FOR_TAKEAWAY) null
  else TimeOfDay.entries.maxBy { byTime.getValue(it).total } // maxBy keeps the first on a tie
  return HardestTimes(byTime, byDay, hardest)
}
