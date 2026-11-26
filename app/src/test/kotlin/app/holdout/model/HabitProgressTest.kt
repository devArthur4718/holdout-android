package app.holdout.model

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitProgressTest {
  private val start = Instant.parse("2026-11-01T00:00:00Z")
  private fun habit(cost: String? = "11.90", minutes: Int? = 90) =
    Habit(1, HabitKind.Smoking, "Cigarettes", start, cost?.let(::BigDecimal), minutes)
  private fun days(d: Double) = start.plusSeconds((d * 86_400).toLong())

  @Test fun moneyAndTimeGrowWithElapsedTime() {
    val p = habit().progressAt(days(18.0))
    assertEquals(0, BigDecimal("214.20").compareTo(p.moneyKept))
    assertEquals(27L, p.hoursBack) // 90 min × 18 days = 27 h
  }

  @Test fun noCostOrTimeGivesNull() {
    val p = habit(cost = null, minutes = null).progressAt(days(2.0))
    assertNull(p.moneyKept)
    assertNull(p.hoursBack)
  }

  @Test fun firstGoalIsOneDayAndTheArcFillsFromTheQuitMoment() {
    val p = habit().progressAt(days(0.5))
    assertEquals(1, p.goal.goalDays)
    assertEquals(0, p.goal.previousGoalDays)
    assertEquals(43_200L, p.goal.secondsToGoal)
    assertEquals(0.5f, p.goal.fraction, 0.001f)

    val week = habit().progressAt(days(3.5))
    assertEquals(7, week.goal.goalDays)
    assertEquals(1, week.goal.previousGoalDays)
    assertEquals(0.5f, week.goal.fraction, 0.001f)
  }

  @Test fun personalGoalsShapeTheArc() {
    val goal = PersonalGoal(1, 1, 45, "Beach trip", start)
    val p = habit().progressAt(days(31.0), listOf(goal))
    assertEquals(45, p.goal.goalDays)
    assertEquals("Beach trip", p.goal.personal?.name)
  }

  @Test fun progressCountsFromZeroNotFromThePreviousGoal() {
    val p = habit().progressAt(days(15.0)) // past 7, towards 30
    assertEquals(30, p.goal.goalDays)
    assertEquals(7, p.goal.previousGoalDays)
    assertEquals(0.5f, p.goal.fraction, 0.001f)
    assertEquals(50, p.goal.percent)
  }

  @Test fun afterTwoYearsAGoalLandsEveryYear() {
    assertEquals(1095, habit().progressAt(days(755.0)).goal.goalDays)
  }

  @Test fun futureLastDoneCountsAsZero() {
    val p = habit().progressAt(start.minusSeconds(60))
    assertEquals(0L, p.elapsedSeconds)
  }

  @Test fun slipBanksTheRunAndKeepsTheLongestStreak() {
    val slipped = habit().afterSlip(days(18.0))
    assertEquals(days(18.0), slipped.lastDoneAt)
    assertEquals(18 * 86_400L, slipped.bestSeconds)
    assertEquals(18 * 86_400L, slipped.bankedSeconds)

    // A shorter second run keeps the old best.
    val again = slipped.afterSlip(days(20.0))
    assertEquals(18 * 86_400L, again.bestSeconds)
    assertEquals(20 * 86_400L, again.bankedSeconds)
  }

  @Test fun moneyKeptSurvivesASlip() {
    val p = habit().afterSlip(days(18.0)).progressAt(days(19.0))
    assertEquals(86_400L, p.elapsedSeconds)
    assertEquals(0, BigDecimal("226.10").compareTo(p.moneyKept)) // 19 clean days × 11.90
    assertEquals(28L, p.hoursBack) // 90 min × 19 days = 28.5 h
  }

  @Test fun personalBestUntilTheCurrentRunOvertakesIt() {
    val slipped = habit().afterSlip(days(5.0))
    val behind = slipped.progressAt(days(8.0))
    assertEquals(false, behind.isPersonalBest)
    assertEquals(5 * 86_400L, behind.bestSeconds)

    val ahead = slipped.progressAt(days(11.0))
    assertEquals(true, ahead.isPersonalBest)
    assertEquals(6 * 86_400L, ahead.bestSeconds)
  }

  @Test fun noAlcoholTriggerForAnAlcoholHabit() {
    assertEquals(false, SlipTrigger.Alcohol in SlipTrigger.forKind(HabitKind.Alcohol))
    assertEquals(true, SlipTrigger.Alcohol in SlipTrigger.forKind(HabitKind.Smoking))
  }
}
