package app.quitter.data

import app.quitter.model.Habit
import app.quitter.model.HabitKind
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportCsvTest {
  @Test fun writesOneRowPerItemAndQuotesWhereNeeded() {
    val habit = Habit(1, HabitKind.Smoking, "Cigs, rolled", Instant.parse("2026-11-01T00:00:00Z"), BigDecimal("11.90"), 90)
    val csv = buildExportCsv(
      habits = listOf(habit),
      slips = listOf(SlipEntity(1, 1, Instant.parse("2026-11-04T09:00:00Z").toEpochMilli(), "Stress")),
      urges = listOf(UrgeEntity(1, 1, Instant.parse("2026-11-05T21:30:00Z").toEpochMilli(), Instant.parse("2026-11-05T21:35:00Z").toEpochMilli())),
      reasons = listOf(ReasonEntity(1, 1, "She said \"stop\"", Instant.parse("2026-11-02T00:00:00Z").toEpochMilli())),
    )
    assertEquals(
      listOf(
        "type,habit,kind,start,end,detail",
        "habit,\"Cigs, rolled\",Smoking,2026-11-01T00:00:00Z,,cost per day: 11.90; minutes per day: 90",
        "slip,\"Cigs, rolled\",,2026-11-04T09:00:00Z,,Stress",
        "urge,\"Cigs, rolled\",,2026-11-05T21:30:00Z,2026-11-05T21:35:00Z,",
        "reason,\"Cigs, rolled\",,2026-11-02T00:00:00Z,,\"She said \"\"stop\"\"\"",
        "",
      ),
      csv.split("\r\n"),
    )
  }
}
