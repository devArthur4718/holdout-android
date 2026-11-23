package app.quitter.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quitter.data.HabitRepository
import app.quitter.model.Habit
import app.quitter.model.HabitProgress
import app.quitter.model.SlipTrigger
import app.quitter.model.progressAt
import java.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitChip(val id: Long, val name: String)

sealed interface HomeUiState {
  data object Loading : HomeUiState

  /** Every habit was deleted. */
  data object Empty : HomeUiState

  data class Ready(
    val chips: List<HabitChip>,
    val selected: Habit,
    val progress: HabitProgress,
  ) : HomeUiState
}

/** Things the screen shows once, like a snackbar. */
sealed interface HomeEvent {
  /** The counter was reset; [longestStreakSeconds] is the streak that's kept. */
  data class SlipLogged(val longestStreakSeconds: Long) : HomeEvent
}

class HomeViewModel(
  private val repository: HabitRepository,
  ticker: Flow<Instant>,
  private val savedState: SavedStateHandle = SavedStateHandle(),
  private val now: () -> Instant = Instant::now,
) : ViewModel() {

  private val _events = Channel<HomeEvent>(Channel.BUFFERED)
  val events: Flow<HomeEvent> = _events.receiveAsFlow()

  /** The habit the user picked; null means "the newest one". Survives process death. */
  private val selectedId = savedState.getStateFlow<Long?>(SELECTED_ID, null)

  val uiState: StateFlow<HomeUiState> =
    combine(repository.observeAll(), selectedId, ticker) { habits, id, now ->
      val selected = habits.firstOrNull { it.id == id } ?: habits.lastOrNull()
      if (selected == null) {
        HomeUiState.Empty
      } else {
        HomeUiState.Ready(
          chips = habits.map { HabitChip(it.id, it.name) },
          selected = selected,
          progress = selected.progressAt(now),
        )
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

  fun select(id: Long) {
    savedState[SELECTED_ID] = id
  }

  /** Restarts [habitId]'s counter from now. Its longest streak and money kept stay. */
  fun logSlip(habitId: Long, trigger: SlipTrigger?) {
    viewModelScope.launch {
      val habit = repository.logSlip(habitId, now(), trigger) ?: return@launch
      _events.send(HomeEvent.SlipLogged(habit.bestSeconds))
    }
  }

  private companion object {
    const val SELECTED_ID = "selectedId"
  }
}
