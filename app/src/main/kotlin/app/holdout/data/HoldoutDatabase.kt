package app.holdout.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(
  entities = [HabitEntity::class, SlipEntity::class, ReasonEntity::class, PersonalGoalEntity::class],
  version = 6,
  autoMigrations = [
    // 2: streak columns on habits and the slips table.
    AutoMigration(from = 1, to = 2),
    // 3: the urges table.
    AutoMigration(from = 2, to = 3),
    // 4: the reasons table.
    AutoMigration(from = 3, to = 4),
    // 5: the personal_goals table.
    AutoMigration(from = 4, to = 5),
    // 6: the urge timer is gone, and its table with it.
    AutoMigration(from = 5, to = 6, spec = HoldoutDatabase.DropUrges::class),
  ],
)
abstract class HoldoutDatabase : RoomDatabase() {
  abstract fun habitDao(): HabitDao

  @DeleteTable(tableName = "urges")
  class DropUrges : AutoMigrationSpec

  companion object {
    fun create(context: Context): HoldoutDatabase =
      Room.databaseBuilder(context, HoldoutDatabase::class.java, "holdout.db").build()
  }
}
