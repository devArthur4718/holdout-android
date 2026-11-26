package app.holdout.ui

import app.holdout.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The habit picked on the Trophies tab. Kept while the app runs;
 * null means "the newest habit".
 */
class HabitSelection {
  val selectedId = MutableStateFlow<Long?>(null)

  fun select(id: Long) {
    selectedId.value = id
  }
}

/** The picked habit, or the newest one when nothing (or a deleted habit) is picked. */
fun List<Habit>.pick(id: Long?): Habit? = firstOrNull { it.id == id } ?: lastOrNull()
