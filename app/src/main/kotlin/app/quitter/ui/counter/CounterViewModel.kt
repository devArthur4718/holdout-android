package app.quitter.ui.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quitter.data.HabitRepository
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface CounterUiState {
  data object Loading : CounterUiState

  data class Running(val habitName: String, val elapsedSeconds: Long) : CounterUiState
}

class CounterViewModel(
  habitId: Long,
  repository: HabitRepository,
  ticker: Flow<Instant>,
) : ViewModel() {

  val uiState: StateFlow<CounterUiState> =
    combine(repository.observe(habitId), ticker) { habit, now ->
      if (habit == null) {
        CounterUiState.Loading
      } else {
        // A clock that moved backwards shouldn't show a negative time.
        val elapsed = Duration.between(habit.lastDoneAt, now).seconds.coerceAtLeast(0)
        CounterUiState.Running(habit.name, elapsed)
      }
    }.stateIn(
      viewModelScope,
      // Stops ticking shortly after the screen leaves; survives rotation.
      SharingStarted.WhileSubscribed(5_000),
      CounterUiState.Loading,
    )
}

/** "00:01:05", or "3d 04:01:05" once a full day has passed. */
fun formatElapsed(totalSeconds: Long): String {
  val days = totalSeconds / 86_400
  val h = totalSeconds / 3_600 % 24
  val m = totalSeconds / 60 % 60
  val s = totalSeconds % 60
  val clock = "%02d:%02d:%02d".format(h, m, s)
  return if (days > 0) "${days}d $clock" else clock
}
