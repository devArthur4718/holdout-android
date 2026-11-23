package app.quitter.data

import androidx.room.withTransaction
import app.quitter.model.Habit
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import java.math.BigDecimal
import app.quitter.model.Reason
import app.quitter.model.Slip
import app.quitter.model.SlipTrigger
import app.quitter.model.afterSlip
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The one place screens read and write habits. ViewModels depend on this, not on Room. */
interface HabitRepository {
  /** Saves the draft and returns the new habit's id. */
  suspend fun add(draft: HabitDraft): Long

  /** Emits the habit, then again whenever it changes. Emits null if it doesn't exist. */
  fun observe(id: Long): Flow<Habit?>

  /** All habits, oldest first, updated whenever they change. */
  fun observeAll(): Flow<List<Habit>>

  suspend fun hasHabits(): Boolean

  /**
   * Records a slip and restarts the habit's counter from [at].
   * Returns the habit as it is now, or null if it doesn't exist.
   */
  suspend fun logSlip(habitId: Long, at: Instant, trigger: SlipTrigger?): Habit?

  /** Records an urge the user rode out. */
  suspend fun logUrge(habitId: Long, startedAt: Instant, endedAt: Instant)

  /** How many urges the user rode out for this habit since [since]. */
  suspend fun countUrges(habitId: Long, since: Instant): Int

  /** The habit's slips, oldest first, updated whenever they change. */
  fun observeSlips(habitId: Long): Flow<List<Slip>>

  /** The habit's reasons, oldest first, updated whenever they change. */
  fun observeReasons(habitId: Long): Flow<List<Reason>>

  suspend fun reasons(habitId: Long): List<Reason>

  suspend fun addReason(habitId: Long, text: String, at: Instant)

  suspend fun updateReason(id: Long, text: String)

  suspend fun deleteReason(id: Long)

  /** Changes what can be edited after the start. When the user last did it can't change. */
  suspend fun updateDetails(id: Long, kind: HabitKind, name: String, costPerDay: BigDecimal?, minutesPerDay: Int?)

  /** Deletes the habit with its slips, urges and reasons. */
  suspend fun delete(id: Long)

  /** Deletes everything the app has stored. */
  suspend fun deleteAll()

  /** Everything the app has stored, as CSV text. */
  suspend fun exportCsv(): String
}

class RoomHabitRepository(private val database: QuitterDatabase) : HabitRepository {
  private val dao = database.habitDao()

  override suspend fun add(draft: HabitDraft): Long = dao.insert(draft.toEntity())
  override fun observe(id: Long): Flow<Habit?> = dao.observe(id).map { it?.toHabit() }
  override fun observeAll(): Flow<List<Habit>> = dao.observeAll().map { list -> list.map { it.toHabit() } }
  override suspend fun hasHabits(): Boolean = dao.count() > 0

  override suspend fun logSlip(habitId: Long, at: Instant, trigger: SlipTrigger?): Habit? =
    database.withTransaction {
      val habit = dao.get(habitId)?.toHabit() ?: return@withTransaction null
      val updated = habit.afterSlip(at)
      dao.update(updated.toEntity())
      dao.insert(SlipEntity(habitId = habitId, atMillis = at.toEpochMilli(), trigger = trigger?.name))
      updated
    }

  override suspend fun logUrge(habitId: Long, startedAt: Instant, endedAt: Instant) {
    dao.insert(UrgeEntity(habitId = habitId, startedAtMillis = startedAt.toEpochMilli(), endedAtMillis = endedAt.toEpochMilli()))
  }

  override suspend fun countUrges(habitId: Long, since: Instant): Int = dao.countUrges(habitId, since.toEpochMilli())

  override fun observeSlips(habitId: Long): Flow<List<Slip>> = dao.observeSlips(habitId).map { list ->
    list.map { Slip(Instant.ofEpochMilli(it.atMillis), SlipTrigger.entries.firstOrNull { t -> t.name == it.trigger }) }
  }

  override fun observeReasons(habitId: Long): Flow<List<Reason>> =
    dao.observeReasons(habitId).map { list -> list.map { Reason(it.id, it.text) } }

  override suspend fun reasons(habitId: Long): List<Reason> = dao.reasons(habitId).map { Reason(it.id, it.text) }

  override suspend fun addReason(habitId: Long, text: String, at: Instant) {
    dao.insert(ReasonEntity(habitId = habitId, text = text, createdAtMillis = at.toEpochMilli()))
  }

  override suspend fun updateReason(id: Long, text: String) = dao.updateReason(id, text)

  override suspend fun deleteReason(id: Long) = dao.deleteReason(id)

  override suspend fun updateDetails(id: Long, kind: HabitKind, name: String, costPerDay: BigDecimal?, minutesPerDay: Int?) =
    dao.updateDetails(id, kind.name, name, costPerDay?.toPlainString(), minutesPerDay)

  override suspend fun delete(id: Long) = dao.delete(id)

  // clearAllTables blocks, and Room refuses to run it on the main thread.
  override suspend fun deleteAll() = withContext(Dispatchers.IO) { database.clearAllTables() }

  override suspend fun exportCsv(): String = database.withTransaction {
    buildExportCsv(dao.allHabits().map { it.toHabit() }, dao.allSlips(), dao.allUrges(), dao.allReasons())
  }
}
