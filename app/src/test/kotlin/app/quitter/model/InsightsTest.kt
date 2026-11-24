package app.quitter.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsightsTest {
  private val utc = ZoneOffset.UTC
  private fun at(s: String) = Instant.parse(s)

  @Test fun partsOfTheDayByLocalHour() {
    assertEquals(TimeOfDay.Night, TimeOfDay.of(0))
    assertEquals(TimeOfDay.Night, TimeOfDay.of(4))
    assertEquals(TimeOfDay.Morning, TimeOfDay.of(5))
    assertEquals(TimeOfDay.Morning, TimeOfDay.of(11))
    assertEquals(TimeOfDay.Afternoon, TimeOfDay.of(12))
    assertEquals(TimeOfDay.Afternoon, TimeOfDay.of(16))
    assertEquals(TimeOfDay.Evening, TimeOfDay.of(17))
    assertEquals(TimeOfDay.Evening, TimeOfDay.of(21))
    assertEquals(TimeOfDay.Night, TimeOfDay.of(22))
    assertEquals(TimeOfDay.Night, TimeOfDay.of(23))
  }

  @Test fun slipsAndUrgesAreCountedApart() {
    val times = hardestTimes(
      slips = listOf(at("2026-11-16T04:59:00Z"), at("2026-11-16T22:00:00Z"), at("2026-11-17T18:00:00Z")),
      urges = listOf(at("2026-11-17T05:00:00Z"), at("2026-11-17T21:59:00Z")),
      zone = utc,
      firstDayOfWeek = DayOfWeek.MONDAY,
    )
    assertEquals(EventCount(slips = 2, urges = 0), times.byTimeOfDay[TimeOfDay.Night])
    assertEquals(EventCount(slips = 0, urges = 1), times.byTimeOfDay[TimeOfDay.Morning])
    assertEquals(EventCount(slips = 1, urges = 1), times.byTimeOfDay[TimeOfDay.Evening])
    assertEquals(EventCount(0, 0), times.byTimeOfDay[TimeOfDay.Afternoon])
    // 16 Nov 2026 is a Monday.
    assertEquals(DayOfWeek.MONDAY to EventCount(2, 0), times.byWeekday[0])
    assertEquals(DayOfWeek.TUESDAY to EventCount(1, 2), times.byWeekday[1])
  }

  @Test fun localTimeFollowsDaylightSaving() {
    val madrid = ZoneId.of("Europe/Madrid")
    // 10:30 UTC is 11:30 in Madrid in winter, 12:30 after the clocks go forward on 29 March.
    val before = hardestTimes(listOf(at("2026-03-28T10:30:00Z")), emptyList(), madrid, DayOfWeek.MONDAY)
    val after = hardestTimes(listOf(at("2026-03-29T10:30:00Z")), emptyList(), madrid, DayOfWeek.MONDAY)
    assertEquals(1, before.byTimeOfDay.getValue(TimeOfDay.Morning).slips)
    assertEquals(1, after.byTimeOfDay.getValue(TimeOfDay.Afternoon).slips)
  }

  @Test fun theWeekStartsOnTheLocalesFirstDay() {
    val sunday = hardestTimes(emptyList(), emptyList(), utc, DayOfWeek.SUNDAY).byWeekday.map { it.first }
    assertEquals(DayOfWeek.SUNDAY, sunday.first())
    assertEquals(DayOfWeek.SATURDAY, sunday.last())
    val monday = hardestTimes(emptyList(), emptyList(), utc, DayOfWeek.MONDAY).byWeekday.map { it.first }
    assertEquals(DayOfWeek.entries, monday)
  }

  @Test fun theHardestTimeNeedsThreeEvents() {
    val two = hardestTimes(listOf(at("2026-11-16T18:00:00Z")), listOf(at("2026-11-16T19:00:00Z")), utc, DayOfWeek.MONDAY)
    assertNull(two.hardest)
    val three = hardestTimes(listOf(at("2026-11-16T18:00:00Z"), at("2026-11-17T08:00:00Z")), listOf(at("2026-11-16T19:00:00Z")), utc, DayOfWeek.MONDAY)
    assertEquals(TimeOfDay.Evening, three.hardest)
  }

  @Test fun aTieGoesToTheEarlierPartOfTheDay() {
    val times = hardestTimes(
      listOf(at("2026-11-16T23:00:00Z"), at("2026-11-16T13:00:00Z")),
      listOf(at("2026-11-17T23:30:00Z"), at("2026-11-17T14:00:00Z")),
      utc,
      DayOfWeek.MONDAY,
    )
    assertEquals(TimeOfDay.Afternoon, times.hardest)
  }
}
