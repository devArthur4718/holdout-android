package app.quitter.ui.trophies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quitter.data.HabitRepository
import app.quitter.model.GoalProgress
import app.quitter.model.Habit
import app.quitter.model.PERSONAL_GOAL_NAME_MAX_LENGTH
import app.quitter.model.PersonalGoal
import app.quitter.model.PersonalGoalError
import app.quitter.model.Trophy
import app.quitter.model.cleanGoalName
import app.quitter.model.goalProgress
import app.quitter.model.parseGoalDays
import app.quitter.model.streaks
import app.quitter.model.trophies
import app.quitter.model.validatePersonalGoal
import app.quitter.ui.HabitSelection
import app.quitter.ui.WriteError
import app.quitter.ui.attempt
import app.quitter.ui.home.HabitChip
import app.quitter.ui.pick
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TrophiesUiState {
  data object Loading : TrophiesUiState

  /** No habits yet. */
  data object Empty : TrophiesUiState

  data class Ready(
    val chips: List<HabitChip>,
    val selected: Habit,
    /** The current streak against its next goal, standard or personal. */
    val next: GoalProgress,
    val trophies: List<Trophy>,
    val personalGoals: List<PersonalGoal>,
  ) : TrophiesUiState
}

/** The personal-goal form. [editingId] is null for a new goal. */
data class GoalEditor(
  val habitId: Long,
  val editingId: Long?,
  val daysText: String = "",
  val name: String = "",
  /** Shown under the days field after Save is tapped. */
  val error: PersonalGoalError? = null,
  /** The "Delete this goal?" confirmation is open. */
  val confirmingDelete: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrophiesViewModel(
  private val repository: HabitRepository,
  ticker: Flow<Instant>,
  private val selection: HabitSelection,
  private val now: () -> Instant = Instant::now,
) : ViewModel() {

  val uiState: StateFlow<TrophiesUiState> =
    combine(repository.observeAll(), selection.selectedId) { habits, id -> habits to habits.pick(id) }
      .flatMapLatest { (habits, selected) ->
        if (selected == null) {
          flowOf(TrophiesUiState.Empty)
        } else {
          combine(repository.observeSlips(selected.id), repository.observeAllPersonalGoals(), ticker) { slips, allGoals, time ->
            val goals = allGoals.filter { it.habitId == selected.id }
            val streaks = selected.streaks(slips.map { it.at }, time)
            TrophiesUiState.Ready(
              chips = habits.map { HabitChip(it.id, it.name) },
              selected = selected,
              next = goalProgress(Duration.between(selected.lastDoneAt, time).seconds, goals),
              trophies = trophies(streaks, goals),
              personalGoals = goals,
            )
          }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrophiesUiState.Loading)

  private val _editor = MutableStateFlow<GoalEditor?>(null)
  val editor: StateFlow<GoalEditor?> = _editor.asStateFlow()

  private val _writeError = MutableStateFlow<WriteError?>(null)
  /** A goal couldn't be saved or deleted: "Couldn't save". */
  val writeError: StateFlow<WriteError?> = _writeError.asStateFlow()

  fun select(id: Long) = selection.select(id)

  fun dismissWriteError() {
    _writeError.value = null
  }

  fun newGoal() {
    val habit = (uiState.value as? TrophiesUiState.Ready)?.selected ?: return
    _editor.value = GoalEditor(habit.id, editingId = null)
  }

  fun editGoal(goal: PersonalGoal) {
    _editor.value = GoalEditor(goal.habitId, goal.id, daysText = goal.days.toString(), name = goal.name.orEmpty())
  }

  /** Digits only, at most 4; editing clears the error. */
  fun updateDays(text: String) {
    if (text.length <= 4 && text.all(Char::isDigit)) _editor.update { it?.copy(daysText = text, error = null) }
  }

  /** Extra typing past 40 characters is cut off, as for reasons. */
  fun updateName(text: String) = _editor.update { it?.copy(name = text.take(PERSONAL_GOAL_NAME_MAX_LENGTH)) }

  fun dismissEditor() {
    _editor.value = null
  }

  /** True while a save is being written; a second tap on Save is ignored so a goal isn't added twice. */
  private var saving = false

  /** Checks the form; when it's valid, saves the goal and closes the form. */
  fun save() {
    if (saving) return
    val form = _editor.value ?: return
    val existing = (uiState.value as? TrophiesUiState.Ready)?.personalGoals.orEmpty()
    val error = validatePersonalGoal(form.daysText, existing, form.editingId)
    if (error != null) {
      _editor.value = form.copy(error = error)
      return
    }
    val days = parseGoalDays(form.daysText) ?: return
    val name = cleanGoalName(form.name)
    saving = true
    viewModelScope.launch {
      try {
        val saved = _writeError.attempt(WriteError.Save) {
          if (form.editingId == null) repository.addPersonalGoal(form.habitId, days, name, now())
          else repository.updatePersonalGoal(form.editingId, days, name)
        }
        if (saved != null) _editor.value = null
      } finally {
        saving = false
      }
    }
  }

  fun askToDelete() = _editor.update { it?.copy(confirmingDelete = true) }

  fun cancelDelete() = _editor.update { it?.copy(confirmingDelete = false) }

  fun confirmDelete() {
    val id = _editor.value?.editingId ?: return
    viewModelScope.launch {
      val deleted = _writeError.attempt(WriteError.Save) { repository.deletePersonalGoal(id) }
      _editor.value = if (deleted != null) null else _editor.value?.copy(confirmingDelete = false)
    }
  }
}
