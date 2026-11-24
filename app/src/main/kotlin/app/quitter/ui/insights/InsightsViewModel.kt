package app.quitter.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quitter.data.HabitRepository
import app.quitter.model.Habit
import app.quitter.model.HardestTimes
import app.quitter.model.StreakEntry
import app.quitter.model.hardestTimes
import app.quitter.model.streakHistory
import app.quitter.model.streaks
import app.quitter.ui.HabitSelection
import app.quitter.ui.home.HabitChip
import app.quitter.ui.pick
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

sealed interface InsightsUiState {
  data object Loading : InsightsUiState

  /** No habits yet. */
  data object Empty : InsightsUiState

  data class Ready(
    val chips: List<HabitChip>,
    val selected: Habit,
    val hardest: HardestTimes,
    /** Newest first; the current streak is the first entry. */
    val history: List<StreakEntry>,
  ) : InsightsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
  repository: HabitRepository,
  ticker: Flow<Instant>,
  private val selection: HabitSelection,
  private val zone: ZoneId = ZoneId.systemDefault(),
  private val firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
) : ViewModel() {

  val uiState: StateFlow<InsightsUiState> =
    combine(repository.observeAll(), selection.selectedId) { habits, id -> habits to habits.pick(id) }
      .flatMapLatest { (habits, selected) ->
        if (selected == null) {
          flowOf(InsightsUiState.Empty)
        } else {
          combine(repository.observeSlips(selected.id), repository.observeUrges(selected.id), ticker) { slips, urges, time ->
            val slipTimes = slips.map { it.at }
            InsightsUiState.Ready(
              chips = habits.map { HabitChip(it.id, it.name) },
              selected = selected,
              hardest = hardestTimes(slipTimes, urges, zone, firstDayOfWeek),
              history = streakHistory(selected.streaks(slipTimes, time)),
            )
          }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState.Loading)

  fun select(id: Long) = selection.select(id)
}
