package app.quitter.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

  @Test fun historyIsNewestFirstWithTheBestMarked() {
    val history = streakHistory(habit.streaks(slips, now))
    assertEquals(listOf(7 * day, 2 * day, 10 * day), history.map { it.streak.seconds })
    assertEquals(listOf(false, false, true), history.map { it.isBest })
    assertTrue(history.first().streak.isCurrent)
  }

  @Test fun onATieTheMostRecentStreakIsBest() {
    val h = Habit(1, HabitKind.Smoking, "x", at("2026-01-11"), null, null, bestSeconds = 5 * day, bankedSeconds = 10 * day)
    val history = streakHistory(h.streaks(listOf(at("2026-01-06"), at("2026-01-11")), at("2026-01-12")))
    // Streaks: 5 days, 5 days, current 1 day. The newer 5-day one is best.
    assertEquals(listOf(false, true, false), history.map { it.isBest })
  }

  @Test fun pastStreaksUnderAMinuteAreLeftOut() {
    val t = at("2026-01-01")
    fun streak(from: Long, to: Long?, now: Long = 0) =
      Streak(t.plusSeconds(from), to?.let { t.plusSeconds(it) }, (to ?: now) - from)

    // 10 days, then 59 s between two slips (left out), then the current streak at 0 minutes (kept).
    val history = streakHistory(listOf(streak(0, 10 * day), streak(10 * day, 10 * day + 59), streak(10 * day + 59, null, 10 * day + 59)))
    assertEquals(listOf(0L, 10 * day), history.map { it.streak.seconds })
    assertEquals(listOf(false, true), history.map { it.isBest })

    // Only a sub-minute past streak: the current one alone, not marked best, so the "show up here" line shows.
    val only = streakHistory(listOf(streak(0, 30), streak(30, null, 30)))
    assertEquals(1, only.size)
    assertTrue(only.single().streak.isCurrent)
    assertFalse(only.single().isBest)

    // Exactly a minute stays.
    assertEquals(2, streakHistory(listOf(streak(0, 60), streak(60, null, 65))).size)
  }

  @Test fun aSingleStreakIsNotMarkedBest() {
    val fresh = Habit(1, HabitKind.Smoking, "x", at("2026-01-01"), null, null)
    assertEquals(listOf(false), streakHistory(fresh.streaks(emptyList(), now)).map { it.isBest })
  }
}
