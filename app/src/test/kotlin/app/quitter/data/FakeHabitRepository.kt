package app.quitter.data

import app.quitter.model.Habit
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import java.math.BigDecimal
import app.quitter.model.PersonalGoal
import app.quitter.model.Reason
import app.quitter.model.Slip
import app.quitter.model.SlipTrigger
import app.quitter.model.afterSlip
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeHabitRepository : HabitRepository {
  val habits = MutableStateFlow<List<Habit>>(emptyList())
  /** habitId, at, trigger */
  val slips = MutableStateFlow<List<Triple<Long, Instant, SlipTrigger?>>>(emptyList())
  /** habitId, startedAt, endedAt */
  val urges = mutableListOf<Triple<Long, Instant, Instant>>()
  /** habitId to reason */
  val reasons = MutableStateFlow<List<Pair<Long, Reason>>>(emptyList())
  private var nextReasonId = 1L
  val goals = MutableStateFlow<List<PersonalGoal>>(emptyList())
  private var nextGoalId = 1L
  private val urgeStarts = MutableStateFlow<List<Pair<Long, Instant>>>(emptyList())
  /** When true, every write throws, as a full disk or a broken database would. */
  var failWrites = false

  private fun write() {
    if (failWrites) throw IOException("disk full")
  }

  override suspend fun add(draft: HabitDraft): Long {
    write()
    val id = habits.value.size + 1L
    habits.value += Habit(id, draft.kind, draft.name, draft.lastDoneAt, draft.costPerDay, draft.minutesPerDay)
    return id
  }

  override fun observe(id: Long): Flow<Habit?> = habits.map { list -> list.firstOrNull { it.id == id } }

  override fun observeAll(): Flow<List<Habit>> = habits

  override suspend fun hasHabits(): Boolean = habits.value.isNotEmpty()

  override suspend fun logSlip(habitId: Long, at: Instant, trigger: SlipTrigger?): Habit? {
    write()
    val habit = habits.value.firstOrNull { it.id == habitId } ?: return null
    val updated = habit.afterSlip(at)
    habits.value = habits.value.map { if (it.id == habitId) updated else it }
    slips.value += Triple(habitId, at, trigger)
    return updated
  }

  override suspend fun logUrge(habitId: Long, startedAt: Instant, endedAt: Instant) {
    write()
    urges += Triple(habitId, startedAt, endedAt)
    urgeStarts.value += habitId to startedAt
  }

  override suspend fun countUrges(habitId: Long, since: Instant): Int =
    urges.count { it.first == habitId && it.third >= since }

  override fun observeSlips(habitId: Long): Flow<List<Slip>> =
    slips.map { list -> list.filter { it.first == habitId }.map { Slip(it.second, it.third) } }

  override fun observeReasons(habitId: Long): Flow<List<Reason>> =
    reasons.map { list -> list.filter { it.first == habitId }.map { it.second } }

  override suspend fun reasons(habitId: Long): List<Reason> =
    reasons.value.filter { it.first == habitId }.map { it.second }

  override suspend fun addReason(habitId: Long, text: String, at: Instant) {
    write()
    reasons.value += habitId to Reason(nextReasonId++, text)
  }

  override suspend fun updateReason(id: Long, text: String) {
    write()
    reasons.value = reasons.value.map { if (it.second.id == id) it.first to Reason(id, text) else it }
  }

  override suspend fun deleteReason(id: Long) {
    write()
    reasons.value = reasons.value.filterNot { it.second.id == id }
  }

  override fun observeUrges(habitId: Long): Flow<List<Instant>> =
    urgeStarts.map { list -> list.filter { it.first == habitId }.map { it.second }.sorted() }

  override fun observeAllPersonalGoals(): Flow<List<PersonalGoal>> = goals.map { list -> list.sortedBy { it.days } }

  override suspend fun addPersonalGoal(habitId: Long, days: Int, name: String?, at: Instant) {
    write()
    goals.value += PersonalGoal(nextGoalId++, habitId, days, name, at)
  }

  override suspend fun updatePersonalGoal(id: Long, days: Int, name: String?) {
    write()
    goals.value = goals.value.map { if (it.id == id) it.copy(days = days, name = name) else it }
  }

  override suspend fun deletePersonalGoal(id: Long) {
    write()
    goals.value = goals.value.filterNot { it.id == id }
  }

  override suspend fun updateDetails(id: Long, kind: HabitKind, name: String, costPerDay: BigDecimal?, minutesPerDay: Int?) {
    write()
    habits.value = habits.value.map {
      if (it.id == id) it.copy(kind = kind, name = name, costPerDay = costPerDay, minutesPerDay = minutesPerDay) else it
    }
  }

  override suspend fun delete(id: Long) {
    write()
    habits.value = habits.value.filterNot { it.id == id }
    slips.value = slips.value.filterNot { it.first == id }
    reasons.value = reasons.value.filterNot { it.first == id }
    urges.removeAll { it.first == id }
    urgeStarts.value = urgeStarts.value.filterNot { it.first == id }
    goals.value = goals.value.filterNot { it.habitId == id }
  }

  override suspend fun deleteAll() {
    write()
    habits.value = emptyList()
    slips.value = emptyList()
    reasons.value = emptyList()
    urges.clear()
    urgeStarts.value = emptyList()
    goals.value = emptyList()
  }

  override suspend fun exportCsv(): String = "type,habit\r\n"
}
