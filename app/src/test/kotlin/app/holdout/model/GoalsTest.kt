package app.holdout.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalsTest {
  private fun days(d: Long) = d * 86_400

  @Test fun theNextGoalIsTheFirstStepStrictlyAfterTheWholeDays() {
    assertEquals(1, nextGoalDays(0))
    assertEquals(7, nextGoalDays(1))
    assertEquals(7, nextGoalDays(6))
    assertEquals(30, nextGoalDays(7))
    assertEquals(60, nextGoalDays(30))
    assertEquals(180, nextGoalDays(90))
    assertEquals(365, nextGoalDays(180))
    assertEquals(545, nextGoalDays(365))
    assertEquals(545, nextGoalDays(544))
    assertEquals(730, nextGoalDays(545))
    assertEquals(730, nextGoalDays(546))
    assertEquals(730, nextGoalDays(729))
    assertEquals(1095, nextGoalDays(730))
    assertEquals(1095, nextGoalDays(755))
    assertEquals(1460, nextGoalDays(1095))
    assertEquals(10_220, nextGoalDays(9_999))
  }

  @Test fun thePreviousGoalIsTheLastStepReached() {
    assertEquals(0, previousGoalDays(0))
    assertEquals(1, previousGoalDays(1))
    assertEquals(1, previousGoalDays(6))
    assertEquals(7, previousGoalDays(7))
    assertEquals(7, previousGoalDays(29))
    assertEquals(545, previousGoalDays(729))
    assertEquals(730, previousGoalDays(730))
    assertEquals(730, previousGoalDays(755))
    assertEquals(1095, previousGoalDays(1095))
  }

  @Test fun theArcFillsFromTheQuitMomentToTheGoal() {
    val p = goalProgress(days(755) + 13 * 3_600 + 36 * 60)
    assertEquals(1095, p.goalDays)
    assertEquals(730, p.previousGoalDays)
    assertEquals(0.6900f, p.fraction, 0.001f) // 755.57 / 1095
    assertEquals(69, p.percent)
  }

  @Test fun theFractionStartsAtZeroAndNeverPassesOne() {
    assertEquals(0f, goalProgress(0).fraction, 0f)
    assertEquals(0f, goalProgress(-30).fraction, 0f)
    assertEquals(0, goalProgress(-30).elapsedSeconds)
    // One second before a week: just under 100%, never rounded up.
    val almost = goalProgress(days(7) - 1)
    assertEquals(7, almost.goalDays)
    assertEquals(99, almost.percent)
    assertTrue(almost.fraction <= 1f)
    // Reaching the goal starts on the next one, still counted from zero.
    val reached = goalProgress(days(7))
    assertEquals(30, reached.goalDays)
    assertEquals(7f / 30f, reached.fraction, 0.0001f)
  }

  @Test fun secondsToGoal() {
    assertEquals(days(1) - 3_600, goalProgress(3_600).secondsToGoal)
    assertEquals(days(7) - days(2), goalProgress(days(2)).secondsToGoal)
  }

  @Test fun goalLabels() {
    assertEquals(GoalLabel.Days(1), goalLabel(1))
    assertEquals(GoalLabel.Weeks(1), goalLabel(7))
    assertEquals(GoalLabel.Days(30), goalLabel(30))
    assertEquals(GoalLabel.Days(60), goalLabel(60))
    assertEquals(GoalLabel.Days(90), goalLabel(90))
    assertEquals(GoalLabel.Months(6), goalLabel(180))
    assertEquals(GoalLabel.Years(1), goalLabel(365))
    assertEquals(GoalLabel.YearAndAHalf, goalLabel(545))
    assertEquals(GoalLabel.Years(2), goalLabel(730))
    assertEquals(GoalLabel.Years(3), goalLabel(1095))
    assertEquals(GoalLabel.Years(28), goalLabel(10_220))
  }

  @Test fun startedJustNowIsUnderAMinute() {
    assertTrue(isStartedJustNow(0))
    assertTrue(isStartedJustNow(59))
    assertFalse(isStartedJustNow(60))
    assertFalse(isStartedJustNow(days(755)))
  }

  private fun goal(id: Long, days: Int, name: String? = null) = PersonalGoal(id, 1, days, name, java.time.Instant.EPOCH)

  @Test fun aPersonalGoalJoinsTheLadderWhenItComesFirst() {
    val beach = goal(1, 45, "Beach trip")
    // Day 31: the standard ladder says 60, the personal goal 45 comes first.
    val p = goalProgress(days(31), listOf(beach))
    assertEquals(45, p.goalDays)
    assertEquals(beach, p.personal)
    assertEquals(30, p.previousGoalDays)
    assertEquals(31f / 45f, p.fraction, 0.0001f)
  }

  @Test fun aPersonalGoalAfterTheNextStandardOneWaits() {
    val p = goalProgress(days(10), listOf(goal(1, 45)))
    assertEquals(30, p.goalDays)
    assertEquals(null, p.personal)
  }

  @Test fun aPassedPersonalGoalIsThePreviousGoal() {
    val beach = goal(1, 45)
    assertEquals(Goal(45, beach), previousGoal(50, listOf(beach)))
    assertEquals(Goal(60), nextGoal(50, listOf(beach)))
    assertEquals(Goal(30), previousGoal(44, listOf(beach)))
  }

  @Test fun personalGoalDaysAreChecked() {
    val existing = listOf(goal(1, 45))
    assertEquals(PersonalGoalError.InvalidDays, validatePersonalGoal("", existing, null))
    assertEquals(PersonalGoalError.InvalidDays, validatePersonalGoal("0", existing, null))
    assertEquals(PersonalGoalError.InvalidDays, validatePersonalGoal("3651", existing, null))
    assertEquals(PersonalGoalError.InvalidDays, validatePersonalGoal("4.5", existing, null))
    // Same days as a standard goal, before, at and after the yearly part of the ladder.
    assertEquals(PersonalGoalError.Duplicate(1), validatePersonalGoal("1", existing, null))
    assertEquals(PersonalGoalError.Duplicate(30), validatePersonalGoal("30", existing, null))
    assertEquals(PersonalGoalError.Duplicate(545), validatePersonalGoal("545", existing, null))
    assertEquals(PersonalGoalError.Duplicate(1095), validatePersonalGoal("1095", existing, null))
    // Same days as another personal goal, but a goal may keep its own days when edited.
    assertEquals(PersonalGoalError.Duplicate(45), validatePersonalGoal("45", existing, null))
    assertEquals(null, validatePersonalGoal("45", existing, editingId = 1))
    assertEquals(null, validatePersonalGoal(" 46 ", existing, null))
    assertEquals(null, validatePersonalGoal("3649", existing, null))
    assertEquals(PersonalGoalError.Duplicate(3650), validatePersonalGoal("3650", existing, null)) // 10 years
    assertEquals(null, validatePersonalGoal("1000", existing, null))
    assertEquals(46, parseGoalDays(" 46 "))
  }

  @Test fun goalNamesAreTrimmedAndCut() {
    assertEquals(null, cleanGoalName("   "))
    assertEquals("Beach trip", cleanGoalName("  Beach trip "))
    assertEquals(40, cleanGoalName("x".repeat(60))!!.length)
  }

  @Test fun standardGoals() {
    assertTrue(isStandardGoal(1))
    assertTrue(isStandardGoal(730))
    assertTrue(isStandardGoal(1460))
    assertFalse(isStandardGoal(45))
    assertFalse(isStandardGoal(1000))
  }
}
