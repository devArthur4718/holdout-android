package app.holdout.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A reason for quitting. Deleting a habit deletes its reasons. */
@Entity(
  tableName = "reasons",
  foreignKeys = [
    ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE),
  ],
  indices = [Index("habitId")],
)
data class ReasonEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val habitId: Long,
  val text: String,
  val createdAtMillis: Long,
)
