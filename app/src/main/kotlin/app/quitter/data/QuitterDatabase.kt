package app.quitter.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [HabitEntity::class, SlipEntity::class, UrgeEntity::class, ReasonEntity::class, PersonalGoalEntity::class],
  version = 5,
  autoMigrations = [
    // 2: streak columns on habits and the slips table.
    AutoMigration(from = 1, to = 2),
    // 3: the urges table.
    AutoMigration(from = 2, to = 3),
    // 4: the reasons table.
    AutoMigration(from = 3, to = 4),
    // 5: the personal_goals table.
    AutoMigration(from = 4, to = 5),
  ],
)
abstract class QuitterDatabase : RoomDatabase() {
  abstract fun habitDao(): HabitDao

  companion object {
    fun create(context: Context): QuitterDatabase =
      Room.databaseBuilder(context, QuitterDatabase::class.java, "quitter.db").build()
  }
}
