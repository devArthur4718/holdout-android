package app.holdout.model

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitHistoryTest {
  private val utc = ZoneOffset.UTC
  private val now = Instant.parse("2026-11-18T12:00:00Z")
  private val november = YearMonth.of(2026, 11)

  @Test fun trackingSinceSurvivesSlips() {
    val habit = Habit(1, HabitKind.Smoking, "C", Instant.parse("2026-11-01T00:00:00Z"), null, null)
      .afterSlip(Instant.parse("2026-11-04T09:00:00Z"))
    assertEquals(Instant.parse("2026-11-01T00:00:00Z"), habit.trackingSince)
  }

  @Test fun calendarMarksEachDay() {
    val days = calendarMonth(
      november, DayOfWeek.MONDAY,
      trackingSince = Instant.parse("2026-11-03T20:00:00Z"),
      slips = listOf(Slip(Instant.parse("2026-11-10T08:00:00Z"), null)),
      now = now, zone = utc,
    )
    // 1 November 2026 is a Sunday: six blanks before it in a Monday-first week.
    assertEquals(6, days.takeWhile { it == null }.size)
    fun day(n: Int) = days.filterNotNull().first { it.date.dayOfMonth == n }
    assertEquals(DayState.Untracked, day(2).state)
    assertEquals(DayState.Clean, day(3).state) // Tracking began that evening.
    assertEquals(DayState.Slip, day(10).state)
    assertEquals(DayState.Clean, day(18).state)
    assertTrue(day(18).isToday)
    assertEquals(DayState.Future, day(19).state)
  }

  @Test fun sundayFirstWeekHasNoBlanksWhenTheMonthStartsOnSunday() {
    val days = calendarMonth(november, DayOfWeek.SUNDAY, now, emptyList(), now, utc)
    assertEquals(LocalDate.of(2026, 11, 1), days.first()?.date)
  }

  @Test fun savingsAddUpByTheEndOfEachMonth() {
    val habit = Habit(1, HabitKind.Smoking, "C", Instant.parse("2026-09-21T00:00:00Z"), BigDecimal("10"), null)
    val savings = habit.savingsByMonth(now, utc)
    assertEquals(listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 10), november), savings.map { it.month })
    assertEquals(0, BigDecimal("100").compareTo(savings[0].total)) // 10 days of September
    assertEquals(0, BigDecimal("410").compareTo(savings[1].total)) // + 31 days of October
    assertEquals(0, BigDecimal("585").compareTo(savings[2].total)) // + 17.5 days of November
  }

  @Test fun savingsShowAtMostSixMonths() {
    val habit = Habit(1, HabitKind.Smoking, "C", Instant.parse("2025-01-01T00:00:00Z"), BigDecimal("1"), null)
    val savings = habit.savingsByMonth(now, utc)
    assertEquals(YearMonth.of(2026, 6), savings.first().month)
    assertEquals(6, savings.size)
  }

  @Test fun noCostMeansNoSavings() {
    val habit = Habit(1, HabitKind.Smoking, "C", now, null, 30)
    assertTrue(habit.savingsByMonth(now, utc).isEmpty())
  }
}
