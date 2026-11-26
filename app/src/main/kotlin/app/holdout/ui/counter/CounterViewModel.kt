package app.holdout.ui.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.holdout.data.HabitRepository
import app.holdout.model.GoalProgress
import app.holdout.model.goalProgress
import app.holdout.model.isStartedJustNow
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface CounterUiState {
  data object Loading : CounterUiState

  data class Running(
    val habitName: String,
    val goal: GoalProgress,
    /** True when the habit starts now; false when its quit moment is in the past. */
    val startedJustNow: Boolean,
  ) : CounterUiState {
    val elapsedSeconds: Long get() = goal.elapsedSeconds
  }
}

class CounterViewModel(
  habitId: Long,
  repository: HabitRepository,
  ticker: Flow<Instant>,
) : ViewModel() {

  /** Decided when the habit is first seen, so the message doesn't change after a minute on screen. */
  private var startedJustNow: Boolean? = null

  val uiState: StateFlow<CounterUiState> =
    combine(repository.observe(habitId), ticker) { habit, now ->
      if (habit == null) {
        CounterUiState.Loading
      } else {
        // A clock that moved backwards shouldn't show a negative time.
        val elapsed = Duration.between(habit.lastDoneAt, now).seconds.coerceAtLeast(0)
        val justNow = startedJustNow ?: isStartedJustNow(elapsed).also { startedJustNow = it }
        CounterUiState.Running(habit.name, goalProgress(elapsed), justNow)
      }
    }.stateIn(
      viewModelScope,
      // Stops ticking shortly after the screen leaves; survives rotation.
      SharingStarted.WhileSubscribed(5_000),
      CounterUiState.Loading,
    )
}
