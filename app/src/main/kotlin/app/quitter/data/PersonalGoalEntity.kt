package app.quitter.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A trophy the user set for one habit. Deleting a habit deletes its goals. */
@Entity(
  tableName = "personal_goals",
  foreignKeys = [
    ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE),
  ],
  indices = [Index("habitId")],
)
data class PersonalGoalEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val habitId: Long,
  val days: Int,
  val name: String?,
  val createdAtMillis: Long,
)
