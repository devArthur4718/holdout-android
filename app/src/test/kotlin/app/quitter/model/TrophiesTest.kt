package app.quitter.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrophiesTest {
  private fun at(date: String) = Instant.parse("${date}T00:00:00Z")
  private val day = 86_400L

  /** Tracked since 1 Jan, slipped on 11 Jan (10 days) and 13 Jan (2 days); clean since. */
  private val habit = Habit(1, HabitKind.Smoking, "Cigarettes", at("2026-01-13"), null, null, bestSeconds = 10 * day, bankedSeconds = 12 * day)
  private val slips = listOf(at("2026-01-11"), at("2026-01-13"))
  private val now = at("2026-01-20") // 7 days into the current streak

  @Test fun streaksRunBetweenSlips() {
    val streaks = habit.streaks(slips, now)
    assertEquals(
      listOf(
        Streak(at("2026-01-01"), at("2026-01-11"), 10 * day),
        Streak(at("2026-01-11"), at("2026-01-13"), 2 * day),
        Streak(at("2026-01-13"), null, 7 * day),
      ),
      streaks,
    )
    assertTrue(streaks.last().isCurrent)
  }

  @Test fun aHabitWithoutSlipsHasOnlyTheCurrentStreak() {
    val fresh = Habit(1, HabitKind.Smoking, "Cigarettes", at("2026-01-01"), null, null)
    assertEquals(listOf(Streak(at("2026-01-01"), null, 19 * day)), fresh.streaks(emptyList(), now))
  }

  @Test fun earnedTrophiesComeFromTheFirstStreakThatReachedThem() {
    val trophies = trophies(habit.streaks(slips, now), emptyList())
    assertEquals(listOf(1, 7, 30, 60, 90, 180, 365), trophies.map { it.days })
    // 1 day and 1 week were first reached in the first streak, not in the current one.
    assertEquals(at("2026-01-02"), trophies[0].earnedAt)
    assertEquals(at("2026-01-08"), trophies[1].earnedAt)
    assertNull(trophies[2].earnedAt)
    // The current streak (7 days) is heading for 30.
    assertEquals(listOf(30), trophies.filter { it.isNext }.map { it.days })
  }

  @Test fun aTrophyEarnedInAPastStreakStaysEarnedAndCanStillBeNext() {
    // 40 clean days, a slip, then 10 days: 30 days was earned before the slip.
    val h = Habit(1, HabitKind.Smoking, "x", at("2026-02-10"), null, null, bestSeconds = 40 * day, bankedSeconds = 40 * day)
    val trophies = trophies(h.streaks(listOf(at("2026-02-10")), at("2026-02-20")), emptyList())
    val thirty = trophies.single { it.days == 30 }
    assertEquals(at("2026-01-31"), thirty.earnedAt)
    // The current streak (10 days) is heading for 30 again: earned and next.
    assertTrue(thirty.isNext)
    assertEquals(listOf(30), trophies.filter { it.isNext }.map { it.days })
  }

  @Test fun personalGoalsMergeIntoTheGridByDays() {
    val beach = PersonalGoal(1, 1, 9, "Beach trip", now)
    val far = PersonalGoal(2, 1, 2000, null, now)
    val trophies = trophies(habit.streaks(slips, now), listOf(far, beach))
    assertEquals(listOf(1, 7, 9, 30, 60, 90, 180, 365, 2000), trophies.map { it.days })
    // 9 days was reached in the first streak (10 days).
    assertEquals(at("2026-01-10"), trophies[2].earnedAt)
    assertEquals(beach, trophies[2].personal)
    // Personal goals are shown even beyond the standard cut-off.
    assertEquals(far, trophies.last().personal)
  }

  @Test fun aPersonalGoalCanBeTheNextOne() {
    val goal = PersonalGoal(1, 1, 20, null, now)
    val trophies = trophies(habit.streaks(slips, now), listOf(goal))
    assertEquals(listOf(20), trophies.filter { it.isNext }.map { it.days })
  }

  @Test fun theLadderStopsOneYearlyTrophyPastTheBestStreakAndTheNextGoal() {
    val start = at("2024-01-01")
    val long = Habit(1, HabitKind.Smoking, "x", start, null, null)
    // 755 days: next goal 1095, so the grid ends at 1460.
    val days = trophies(long.streaks(emptyList(), start.plusSeconds(755 * day)), emptyList()).map { it.days }
    assertEquals(listOf(1, 7, 30, 60, 90, 180, 365, 545, 730, 1095, 1460), days)
    // A new habit: up to 1 year.
    assertEquals(365, trophies(long.streaks(emptyList(), start), emptyList()).last().days)
  }
}
