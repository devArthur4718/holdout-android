package app.quitter.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** An urge the user rode out. Deleting a habit deletes its urges. */
@Entity(
  tableName = "urges",
  foreignKeys = [
    ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE),
  ],
  indices = [Index("habitId")],
)
data class UrgeEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val habitId: Long,
  val startedAtMillis: Long,
  val endedAtMillis: Long,
)
