package app.quitter.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One logged slip. Deleting a habit deletes its slips. */
@Entity(
  tableName = "slips",
  foreignKeys = [
    ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE),
  ],
  indices = [Index("habitId")],
)
data class SlipEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val habitId: Long,
  val atMillis: Long,
  /** [app.quitter.model.SlipTrigger] name, or null when none was picked. */
  val trigger: String?,
)
