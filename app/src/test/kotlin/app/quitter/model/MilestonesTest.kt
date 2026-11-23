package app.quitter.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestonesTest {
  private val utc = ZoneOffset.UTC
  private fun habit(lastDoneAt: String) = Habit(1, HabitKind.Smoking, "Cigarettes", Instant.parse(lastDoneAt), null, null)
  private fun Instant.plusHours(h: Long) = plus(Duration.ofHours(h))

  @Test fun daytimeMilestonesArriveOnTime() {
    val habit = habit("2026-11-18T10:00:00Z")
    assertEquals(
      listOf(
        MilestoneAlert(1, Instant.parse("2026-11-19T10:00:00Z")),
        MilestoneAlert(3, Instant.parse("2026-11-21T10:00:00Z")),
        MilestoneAlert(7, Instant.parse("2026-11-25T10:00:00Z")),
      ),
      habit.milestoneAlerts(habit.lastDoneAt, utc, count = 3),
    )
  }

  @Test fun lateEveningMilestonesWaitUntilEightTheNextMorning() {
    val habit = habit("2026-11-18T23:30:00Z")
    assertEquals(Instant.parse("2026-11-20T08:00:00Z"), habit.milestoneAlerts(habit.lastDoneAt, utc, 1).single().at)
  }

  @Test fun earlyMorningMilestonesWaitUntilEight() {
    val habit = habit("2026-11-18T03:00:00Z")
    assertEquals(Instant.parse("2026-11-19T08:00:00Z"), habit.milestoneAlerts(habit.lastDoneAt, utc, 1).single().at)
  }

  @Test fun aMilestoneReachedInTheNightIsStillDueAtEight() {
    val habit = habit("2026-11-18T03:00:00Z")
    // 05:00 the next day: the 1-day mark passed at 03:00, but its notification is due at 08:00.
    val now = Instant.parse("2026-11-19T05:00:00Z")
    assertEquals(MilestoneAlert(1, Instant.parse("2026-11-19T08:00:00Z")), habit.milestoneAlerts(now, utc, 1).single())
  }

  @Test fun onceDeliveredTheNextMilestoneFollows() {
    val habit = habit("2026-11-18T03:00:00Z")
    val delivered = Instant.parse("2026-11-19T08:00:00Z")
    assertEquals(3, habit.milestoneAlerts(delivered, utc, 1).single().days)
  }

  @Test fun milestonesAlreadyPassedInTheDayAreNotSent() {
    // Added with a start 3 days and 2 hours ago: the 3-day trophy is in the past, so the next is 7 days.
    val now = Instant.parse("2026-11-18T12:00:00Z")
    val habit = habit("2026-11-15T10:00:00Z")
    assertEquals(7, habit.milestoneAlerts(now, utc, 1).single().days)
  }

  @Test fun afterAYearATrophyLandsEveryYear() {
    val habit = habit("2025-01-01T12:00:00Z")
    val now = Instant.parse("2026-06-01T12:00:00Z")
    assertEquals(listOf(730, 1095), habit.milestoneAlerts(now, utc, 2).map { it.days })
    assertTrue(isWholeYears(730))
    assertFalse(isWholeYears(180))
  }

  @Test fun quietHoursFollowTheLocalClock() {
    val saoPaulo = ZoneId.of("America/Sao_Paulo") // UTC−3
    // 01:00 UTC is 22:00 in São Paulo: quiet, so 08:00 local (11:00 UTC) the next morning.
    val habit = habit("2026-11-18T01:00:00Z")
    assertEquals(Instant.parse("2026-11-19T11:00:00Z"), habit.milestoneAlerts(habit.lastDoneAt, saoPaulo, 1).single().at)
  }
}
