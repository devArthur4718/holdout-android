package app.quitter.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MigrationTest {
  @get:Rule
  val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), QuitterDatabase::class.java)

  @Test fun version1HabitsSurviveWithNoStreakYet() {
    helper.createDatabase(DB, 1).use { db ->
      db.execSQL(
        "INSERT INTO habits (id, kind, name, lastDoneAtMillis, costPerDay, minutesPerDay) " +
          "VALUES (1, 'Smoking', 'Cigarettes', 1700000000000, '11.90', 90)",
      )
    }

    helper.runMigrationsAndValidate(DB, 2, true).use { db ->
      db.query("SELECT name, costPerDay, bestSeconds, bankedSeconds FROM habits").use { c ->
        c.moveToFirst()
        assertEquals("Cigarettes", c.getString(0))
        assertEquals("11.90", c.getString(1))
        assertEquals(0L, c.getLong(2))
        assertEquals(0L, c.getLong(3))
      }
      db.query("SELECT COUNT(*) FROM slips").use { c ->
        c.moveToFirst()
        assertEquals(0, c.getInt(0))
      }
    }
  }

  @Test fun version2DataSurvivesAndUrgesStartEmpty() {
    helper.createDatabase(DB, 2).use { db ->
      db.execSQL(
        "INSERT INTO habits (id, kind, name, lastDoneAtMillis, costPerDay, minutesPerDay, bestSeconds, bankedSeconds) " +
          "VALUES (1, 'Alcohol', 'Wine', 1700000000000, NULL, NULL, 86400, 172800)",
      )
      db.execSQL("INSERT INTO slips (habitId, atMillis, `trigger`) VALUES (1, 1700000000000, 'Stress')")
    }

    helper.runMigrationsAndValidate(DB, 3, true).use { db ->
      db.query("SELECT bestSeconds FROM habits").use { c ->
        c.moveToFirst()
        assertEquals(86_400L, c.getLong(0))
      }
      db.query("SELECT COUNT(*) FROM slips").use { c ->
        c.moveToFirst()
        assertEquals(1, c.getInt(0))
      }
      db.query("SELECT COUNT(*) FROM urges").use { c ->
        c.moveToFirst()
        assertEquals(0, c.getInt(0))
      }
    }
  }

  @Test fun version3DataSurvivesAndReasonsStartEmpty() {
    helper.createDatabase(DB, 3).use { db ->
      db.execSQL(
        "INSERT INTO habits (id, kind, name, lastDoneAtMillis, costPerDay, minutesPerDay, bestSeconds, bankedSeconds) " +
          "VALUES (1, 'Sugar', 'Sweets', 1700000000000, '4.20', 20, 0, 0)",
      )
      db.execSQL("INSERT INTO urges (habitId, startedAtMillis, endedAtMillis) VALUES (1, 1700000000000, 1700000300000)")
    }

    helper.runMigrationsAndValidate(DB, 4, true).use { db ->
      db.query("SELECT COUNT(*) FROM urges").use { c ->
        c.moveToFirst()
        assertEquals(1, c.getInt(0))
      }
      db.query("SELECT COUNT(*) FROM reasons").use { c ->
        c.moveToFirst()
        assertEquals(0, c.getInt(0))
      }
    }
  }

  private companion object {
    const val DB = "migration-test"
  }
}
