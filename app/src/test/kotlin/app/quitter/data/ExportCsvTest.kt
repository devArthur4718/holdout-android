package app.quitter.data

import app.quitter.model.Habit
import app.quitter.model.HabitKind
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportCsvTest {
  @Test fun timesAreUtcWithWholeSecondsNeverMilliseconds() {
    assertEquals("2026-11-23T22:05:31Z", csvTime(Instant.parse("2026-11-23T22:05:31.987Z")))
    assertEquals("2026-11-23T22:05:00Z", csvTime(Instant.parse("2026-11-23T22:05:00Z")))
    val habit = Habit(1, HabitKind.Smoking, "Cigarettes", Instant.parse("2026-11-01T08:30:15.250Z"), null, null)
    val millis = Instant.parse("2026-11-04T09:00:07.999Z").toEpochMilli()
    val csv = buildExportCsv(
      habits = listOf(habit),
      slips = listOf(SlipEntity(1, 1, millis, null)),
      urges = listOf(UrgeEntity(1, 1, millis, millis + 300_500)),
      reasons = listOf(ReasonEntity(1, 1, "Lungs", millis)),
      goals = listOf(PersonalGoalEntity(1, 1, 45, null, millis)),
    )
    assertEquals(
      listOf(
        "type,habit,kind,start,end,detail",
        "habit,Cigarettes,Smoking,2026-11-01T08:30:15Z,,",
        "slip,Cigarettes,,2026-11-04T09:00:07Z,,",
        "urge,Cigarettes,,2026-11-04T09:00:07Z,2026-11-04T09:05:08Z,",
        "reason,Cigarettes,,2026-11-04T09:00:07Z,,Lungs",
        "goal,Cigarettes,,2026-11-04T09:00:07Z,,days: 45",
        "",
      ),
      csv.split("\r\n"),
    )
  }

  @Test fun writesOneRowPerItemAndQuotesWhereNeeded() {
    val habit = Habit(1, HabitKind.Smoking, "Cigs, rolled", Instant.parse("2026-11-01T00:00:00Z"), BigDecimal("11.90"), 90)
    val csv = buildExportCsv(
      habits = listOf(habit),
      slips = listOf(SlipEntity(1, 1, Instant.parse("2026-11-04T09:00:00Z").toEpochMilli(), "Stress")),
      urges = listOf(UrgeEntity(1, 1, Instant.parse("2026-11-05T21:30:00Z").toEpochMilli(), Instant.parse("2026-11-05T21:35:00Z").toEpochMilli())),
      reasons = listOf(ReasonEntity(1, 1, "She said \"stop\"", Instant.parse("2026-11-02T00:00:00Z").toEpochMilli())),
      goals = listOf(
        PersonalGoalEntity(1, 1, 45, "Beach, at last", Instant.parse("2026-11-03T00:00:00Z").toEpochMilli()),
        PersonalGoalEntity(2, 1, 100, null, Instant.parse("2026-11-03T00:00:00Z").toEpochMilli()),
      ),
    )
    assertEquals(
      listOf(
        "type,habit,kind,start,end,detail",
        "habit,\"Cigs, rolled\",Smoking,2026-11-01T00:00:00Z,,cost per day: 11.90; minutes per day: 90",
        "slip,\"Cigs, rolled\",,2026-11-04T09:00:00Z,,Stress",
        "urge,\"Cigs, rolled\",,2026-11-05T21:30:00Z,2026-11-05T21:35:00Z,",
        "reason,\"Cigs, rolled\",,2026-11-02T00:00:00Z,,\"She said \"\"stop\"\"\"",
        "goal,\"Cigs, rolled\",,2026-11-03T00:00:00Z,,\"days: 45; name: Beach, at last\"",
        "goal,\"Cigs, rolled\",,2026-11-03T00:00:00Z,,days: 100",
        "",
      ),
      csv.split("\r\n"),
    )
  }
}
