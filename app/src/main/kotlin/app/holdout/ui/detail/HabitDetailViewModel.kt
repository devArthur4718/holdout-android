package app.holdout.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.holdout.data.HabitRepository
import app.holdout.model.CalendarDay
import app.holdout.model.Habit
import app.holdout.model.HabitProgress
import app.holdout.model.MonthSavings
import app.holdout.model.REASON_MAX_LENGTH
import app.holdout.model.Reason
import app.holdout.model.SlipTrigger
import app.holdout.model.calendarMonth
import app.holdout.model.progressAt
import app.holdout.model.savingsByMonth
import app.holdout.model.trackingSince
import app.holdout.ui.WriteError
import app.holdout.ui.attempt
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DetailTab { Stats, Calendar, Reasons }

data class CalendarState(
  val month: YearMonth,
  val firstDayOfWeek: DayOfWeek,
  val days: List<CalendarDay?>,
  val canGoBack: Boolean,
  val canGoForward: Boolean,
)

sealed interface HabitDetailUiState {
  data object Loading : HabitDetailUiState

  /** The habit was deleted while this screen was open. */
  data object Gone : HabitDetailUiState

  data class Ready(
    val habit: Habit,
    val trackingSince: Instant,
    val progress: HabitProgress,
    val tab: DetailTab,
    val savings: List<MonthSavings>,
    val calendar: CalendarState,
    val reasons: List<Reason>,
  ) : HabitDetailUiState
}

sealed interface HabitDetailEvent {
  data class SlipLogged(val longestStreakSeconds: Long) : HabitDetailEvent
}

class HabitDetailViewModel(
  private val habitId: Long,
  private val repository: HabitRepository,
  ticker: Flow<Instant>,
  private val savedState: SavedStateHandle = SavedStateHandle(),
  private val now: () -> Instant = Instant::now,
  private val zone: ZoneId = ZoneId.systemDefault(),
  private val firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
) : ViewModel() {

  private val tab = savedState.getStateFlow(TAB, DetailTab.Stats.name)
  /** The calendar month shown, as "2026-11"; null means the current month. */
  private val month = savedState.getStateFlow<String?>(MONTH, null)

  private val _events = Channel<HabitDetailEvent>(Channel.BUFFERED)
  val events: Flow<HabitDetailEvent> = _events.receiveAsFlow()

  private val _writeError = MutableStateFlow<WriteError?>(null)
  /** A write failed: "Couldn't save". Nothing changed, so the user can try again. */
  val writeError: StateFlow<WriteError?> = _writeError.asStateFlow()

  fun dismissWriteError() {
    _writeError.value = null
  }

  val uiState: StateFlow<HabitDetailUiState> =
    combine(
      repository.observe(habitId),
      repository.observeSlips(habitId),
      repository.observeReasons(habitId),
      combine(tab, month, repository.observeAllPersonalGoals(), ::Triple),
      ticker,
    ) { habit, slips, reasons, (tabName, monthText, goals), time ->
      if (habit == null) return@combine HabitDetailUiState.Gone
      val since = habit.trackingSince
      val first = YearMonth.from(since.atZone(zone))
      val current = YearMonth.from(time.atZone(zone))
      val shown = (monthText?.let(YearMonth::parse) ?: current).coerceIn(first, current)
      HabitDetailUiState.Ready(
        habit = habit,
        trackingSince = since,
        progress = habit.progressAt(time, goals.filter { it.habitId == habit.id }),
        tab = DetailTab.valueOf(tabName),
        savings = habit.savingsByMonth(time, zone),
        calendar = CalendarState(
          month = shown,
          firstDayOfWeek = firstDayOfWeek,
          days = calendarMonth(shown, firstDayOfWeek, since, slips, time, zone),
          canGoBack = shown > first,
          canGoForward = shown < current,
        ),
        reasons = reasons,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState.Loading)

  fun selectTab(tab: DetailTab) {
    savedState[TAB] = tab.name
  }

  fun showPreviousMonth() = moveMonth(-1)

  fun showNextMonth() = moveMonth(1)

  private fun moveMonth(by: Long) {
    val state = uiState.value as? HabitDetailUiState.Ready ?: return
    savedState[MONTH] = state.calendar.month.plusMonths(by).toString()
  }

  /** Adds a reason; blank text is ignored and long text is cut to [REASON_MAX_LENGTH]. */
  fun addReason(text: String) {
    val clean = text.cleanReason() ?: return
    save { repository.addReason(habitId, clean, now()) }
  }

  /** Saves the new text, or deletes the reason if the text was cleared. */
  fun updateReason(id: Long, text: String) {
    val clean = text.cleanReason()
    save { if (clean == null) repository.deleteReason(id) else repository.updateReason(id, clean) }
  }

  fun deleteReason(id: Long) {
    save { repository.deleteReason(id) }
  }

  fun logSlip(trigger: SlipTrigger?) {
    viewModelScope.launch {
      val habit = _writeError.attempt(WriteError.Save) { repository.logSlip(habitId, now(), trigger) } ?: return@launch
      _events.send(HabitDetailEvent.SlipLogged(habit.bestSeconds))
    }
  }

  /** Deletes the habit with its history. The screen then closes by itself (state becomes Gone). */
  fun deleteHabit() {
    save { repository.delete(habitId) }
  }

  /** Every write here shows "Couldn't save" if it fails, as on iOS. */
  private fun save(write: suspend () -> Unit) {
    viewModelScope.launch { _writeError.attempt(WriteError.Save, write) }
  }

  private fun String.cleanReason(): String? = trim().take(REASON_MAX_LENGTH).ifEmpty { null }

  private companion object {
    const val TAB = "tab"
    const val MONTH = "month"
  }
}
