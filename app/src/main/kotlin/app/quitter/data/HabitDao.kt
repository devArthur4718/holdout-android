package app.quitter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
  @Insert suspend fun insert(habit: HabitEntity): Long

  @Update suspend fun update(habit: HabitEntity)

  @Insert suspend fun insert(slip: SlipEntity): Long

  @Query("SELECT * FROM habits WHERE id = :id")
  suspend fun get(id: Long): HabitEntity?

  @Query("SELECT * FROM habits WHERE id = :id")
  fun observe(id: Long): Flow<HabitEntity?>

  @Query("SELECT * FROM habits ORDER BY id")
  fun observeAll(): Flow<List<HabitEntity>>

  @Query("SELECT COUNT(*) FROM habits")
  suspend fun count(): Int

  @Query("UPDATE habits SET kind = :kind, name = :name, costPerDay = :costPerDay, minutesPerDay = :minutesPerDay WHERE id = :id")
  suspend fun updateDetails(id: Long, kind: String, name: String, costPerDay: String?, minutesPerDay: Int?)

  /** Its slips, reasons and personal goals go with it (foreign keys cascade). */
  @Query("DELETE FROM habits WHERE id = :id")
  suspend fun delete(id: Long)

  @Query("SELECT * FROM habits ORDER BY id")
  suspend fun allHabits(): List<HabitEntity>

  @Query("SELECT * FROM slips ORDER BY atMillis")
  suspend fun allSlips(): List<SlipEntity>

  @Query("SELECT * FROM reasons ORDER BY createdAtMillis, id")
  suspend fun allReasons(): List<ReasonEntity>

  @Query("SELECT * FROM slips WHERE habitId = :habitId ORDER BY atMillis")
  suspend fun slips(habitId: Long): List<SlipEntity>

  @Query("SELECT * FROM slips WHERE habitId = :habitId ORDER BY atMillis")
  fun observeSlips(habitId: Long): Flow<List<SlipEntity>>

  @Insert suspend fun insert(reason: ReasonEntity): Long

  @Query("UPDATE reasons SET text = :text WHERE id = :id")
  suspend fun updateReason(id: Long, text: String)

  @Query("DELETE FROM reasons WHERE id = :id")
  suspend fun deleteReason(id: Long)

  @Query("SELECT * FROM reasons WHERE habitId = :habitId ORDER BY createdAtMillis, id")
  fun observeReasons(habitId: Long): Flow<List<ReasonEntity>>

  @Query("SELECT * FROM reasons WHERE habitId = :habitId ORDER BY createdAtMillis, id")
  suspend fun reasons(habitId: Long): List<ReasonEntity>

  @Insert suspend fun insert(goal: PersonalGoalEntity): Long

  @Query("UPDATE personal_goals SET days = :days, name = :name WHERE id = :id")
  suspend fun updatePersonalGoal(id: Long, days: Int, name: String?)

  @Query("DELETE FROM personal_goals WHERE id = :id")
  suspend fun deletePersonalGoal(id: Long)

  @Query("SELECT * FROM personal_goals ORDER BY days")
  fun observeAllPersonalGoals(): Flow<List<PersonalGoalEntity>>

  @Query("SELECT * FROM personal_goals ORDER BY createdAtMillis, id")
  suspend fun allPersonalGoals(): List<PersonalGoalEntity>
}
