package app.holdout.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
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
        MilestoneAlert(7, Instant.parse("2026-11-25T10:00:00Z")),
        MilestoneAlert(30, Instant.parse("2026-12-18T10:00:00Z")),
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
    // 05:00 a week later: the 1-week mark passed at 03:00, but its notification is due at 08:00.
    val now = Instant.parse("2026-11-25T05:00:00Z")
    assertEquals(MilestoneAlert(7, Instant.parse("2026-11-25T08:00:00Z")), habit.milestoneAlerts(now, utc, 1).single())
  }

  @Test fun onceDeliveredTheNextMilestoneFollows() {
    val habit = habit("2026-11-18T03:00:00Z")
    val delivered = Instant.parse("2026-11-25T08:00:00Z")
    assertEquals(30, habit.milestoneAlerts(delivered, utc, 1).single().days)
  }

  @Test fun milestonesAlreadyPassedInTheDayAreNotSent() {
    // Added with a start 7 days and 2 hours ago: the 1-week trophy is in the past, so the next is 30 days.
    val now = Instant.parse("2026-11-18T12:00:00Z")
    val habit = habit("2026-11-11T10:00:00Z")
    assertEquals(30, habit.milestoneAlerts(now, utc, 1).single().days)
  }

  @Test fun theWholeLadderIsScheduledInOrder() {
    val habit = habit("2026-01-01T12:00:00Z")
    assertEquals(
      listOf(1, 7, 30, 60, 90, 180, 365, 545, 730, 1095, 1460),
      habit.milestoneAlerts(habit.lastDoneAt, utc, 11).map { it.days },
    )
  }

  @Test fun afterTwoYearsATrophyLandsEveryYear() {
    val habit = habit("2024-01-01T12:00:00Z")
    val now = Instant.parse("2026-01-25T12:00:00Z") // 755 days
    assertEquals(listOf(1095, 1460), habit.milestoneAlerts(now, utc, 2).map { it.days })
  }

  @Test fun aYearAndAHalfIsATrophy() {
    val habit = habit("2025-01-01T12:00:00Z")
    val now = Instant.parse("2026-06-01T12:00:00Z") // 516 days
    assertEquals(listOf(545, 730), habit.milestoneAlerts(now, utc, 2).map { it.days })
  }

  @Test fun quietHoursFollowTheLocalClock() {
    val saoPaulo = ZoneId.of("America/Sao_Paulo") // UTC−3
    // 01:00 UTC is 22:00 in São Paulo: quiet, so the 1-day trophy waits for 08:00 local (11:00 UTC) the next morning.
    val habit = habit("2026-11-18T01:00:00Z")
    assertEquals(Instant.parse("2026-11-19T11:00:00Z"), habit.milestoneAlerts(habit.lastDoneAt, saoPaulo, 1).single().at)
  }

  @Test fun personalGoalsAreNotifiedInLadderOrder() {
    val habit = habit("2026-01-01T12:00:00Z")
    val beach = PersonalGoal(1, 1, 45, "Beach trip", habit.lastDoneAt)
    val alerts = habit.milestoneAlerts(Instant.parse("2026-01-20T12:00:00Z"), utc, 3, listOf(beach))
    assertEquals(listOf(30, 45, 60), alerts.map { it.days })
    assertEquals(beach, alerts[1].personal)
    assertEquals(Instant.parse("2026-02-15T12:00:00Z"), alerts[1].at)
  }
}
