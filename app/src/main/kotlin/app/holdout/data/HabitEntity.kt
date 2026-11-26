package app.holdout.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.holdout.model.Habit
import app.holdout.model.HabitDraft
import app.holdout.model.HabitKind
import java.time.Instant

@Entity(tableName = "habits")
data class HabitEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  /** [HabitKind] name. Stored as text so reordering the enum can't corrupt data. */
  val kind: String,
  val name: String,
  val lastDoneAtMillis: Long,
  /** Exact decimal as text, e.g. "11.90", to avoid floating-point rounding. */
  val costPerDay: String?,
  val minutesPerDay: Int?,
  @ColumnInfo(defaultValue = "0") val bestSeconds: Long = 0,
  @ColumnInfo(defaultValue = "0") val bankedSeconds: Long = 0,
)

fun HabitDraft.toEntity() = HabitEntity(
  kind = kind.name,
  name = name,
  lastDoneAtMillis = lastDoneAt.toEpochMilli(),
  costPerDay = costPerDay?.toPlainString(),
  minutesPerDay = minutesPerDay,
)

fun Habit.toEntity() = HabitEntity(
  id = id,
  kind = kind.name,
  name = name,
  lastDoneAtMillis = lastDoneAt.toEpochMilli(),
  costPerDay = costPerDay?.toPlainString(),
  minutesPerDay = minutesPerDay,
  bestSeconds = bestSeconds,
  bankedSeconds = bankedSeconds,
)

fun HabitEntity.toHabit() = Habit(
  id = id,
  kind = HabitKind.entries.firstOrNull { it.name == kind } ?: HabitKind.Smoking,
  name = name,
  lastDoneAt = Instant.ofEpochMilli(lastDoneAtMillis),
  costPerDay = costPerDay?.toBigDecimalOrNull(),
  minutesPerDay = minutesPerDay,
  bestSeconds = bestSeconds,
  bankedSeconds = bankedSeconds,
)
