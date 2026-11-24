package app.quitter.data

import app.quitter.model.Habit
import app.quitter.model.trackingSince
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * One CSV with a row per habit, slip, urge, reason and personal goal, so it opens in any spreadsheet.
 * Times are UTC in ISO 8601, e.g. 2026-11-18T21:30:00Z.
 */
internal fun buildExportCsv(
  habits: List<Habit>,
  slips: List<SlipEntity>,
  urges: List<UrgeEntity>,
  reasons: List<ReasonEntity>,
  goals: List<PersonalGoalEntity> = emptyList(),
): String {
  val names = habits.associate { it.id to it.name }
  val rows = mutableListOf(listOf("type", "habit", "kind", "start", "end", "detail"))
  habits.forEach {
    val detail = listOfNotNull(
      it.costPerDay?.let { cost -> "cost per day: ${cost.toPlainString()}" },
      it.minutesPerDay?.let { minutes -> "minutes per day: $minutes" },
    ).joinToString("; ")
    rows += listOf("habit", it.name, it.kind.name, csvTime(it.trackingSince), "", detail)
  }
  slips.forEach { rows += listOf("slip", names[it.habitId].orEmpty(), "", csvTime(it.atMillis), "", it.trigger.orEmpty()) }
  urges.forEach {
    rows += listOf(
      "urge", names[it.habitId].orEmpty(), "",
      csvTime(it.startedAtMillis), csvTime(it.endedAtMillis), "",
    )
  }
  reasons.forEach { rows += listOf("reason", names[it.habitId].orEmpty(), "", csvTime(it.createdAtMillis), "", it.text) }
  goals.forEach {
    val detail = listOfNotNull("days: ${it.days}", it.name?.let { name -> "name: $name" }).joinToString("; ")
    rows += listOf("goal", names[it.habitId].orEmpty(), "", csvTime(it.createdAtMillis), "", detail)
  }
  return rows.joinToString("\r\n", postfix = "\r\n") { row -> row.joinToString(",") { it.csvField() } }
}

/** ISO-8601 in UTC with whole seconds, never milliseconds: 2026-11-23T22:05:31Z. */
internal fun csvTime(at: Instant): String = at.truncatedTo(ChronoUnit.SECONDS).toString()

private fun csvTime(epochMillis: Long): String = csvTime(Instant.ofEpochMilli(epochMillis))

/** Quotes a field when it holds a comma, quote or line break, doubling any quotes inside. */
private fun String.csvField(): String =
  if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + replace("\"", "\"\"") + "\"" else this
