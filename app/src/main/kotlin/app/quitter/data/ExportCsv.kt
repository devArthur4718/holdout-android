package app.quitter.data

import app.quitter.model.Habit
import app.quitter.model.trackingSince
import java.time.Instant

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
    rows += listOf("habit", it.name, it.kind.name, it.trackingSince.toString(), "", detail)
  }
  slips.forEach { rows += listOf("slip", names[it.habitId].orEmpty(), "", Instant.ofEpochMilli(it.atMillis).toString(), "", it.trigger.orEmpty()) }
  urges.forEach {
    rows += listOf(
      "urge", names[it.habitId].orEmpty(), "",
      Instant.ofEpochMilli(it.startedAtMillis).toString(), Instant.ofEpochMilli(it.endedAtMillis).toString(), "",
    )
  }
  reasons.forEach { rows += listOf("reason", names[it.habitId].orEmpty(), "", Instant.ofEpochMilli(it.createdAtMillis).toString(), "", it.text) }
  goals.forEach {
    val detail = listOfNotNull("days: ${it.days}", it.name?.let { name -> "name: $name" }).joinToString("; ")
    rows += listOf("goal", names[it.habitId].orEmpty(), "", Instant.ofEpochMilli(it.createdAtMillis).toString(), "", detail)
  }
  return rows.joinToString("\r\n", postfix = "\r\n") { row -> row.joinToString(",") { it.csvField() } }
}

/** Quotes a field when it holds a comma, quote or line break, doubling any quotes inside. */
private fun String.csvField(): String =
  if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + replace("\"", "\"\"") + "\"" else this
