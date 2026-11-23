package app.quitter.ui.newhabit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quitter.data.HabitRepository
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MAX_MINUTES_PER_DAY = 24 * 60
private val DAYS_PER_YEAR = BigDecimal(365)

data class NewHabitUiState(
  val kind: HabitKind,
  val name: String,
  /** null means "just now": the moment the user taps Start. */
  val lastDoneAt: Instant? = null,
  val costText: String = "",
  val minutesText: String = "",
  /** Editing a saved habit: when it was last done can't change, and the button says Save. */
  val isEditing: Boolean = false,
) {
  val costPerDay: BigDecimal? = costText.replace(',', '.').toBigDecimalOrNull()
  val minutesPerDay: Int? = minutesText.toIntOrNull()
  val minutesTooHigh: Boolean = (minutesPerDay ?: 0) > MAX_MINUTES_PER_DAY
  val canStart: Boolean = name.isNotBlank() && !minutesTooHigh

  /** Money kept in a year at this rate, or null when no cost is entered. */
  val yearlyCost: BigDecimal? = costPerDay?.takeIf { it.signum() > 0 }?.multiply(DAYS_PER_YEAR)

  /** Whole hours back in a year, or null when no time is entered. */
  val yearlyHours: Int? = minutesPerDay?.takeIf { it > 0 && !minutesTooHigh }?.let { it * 365 / 60 }
}

class NewHabitViewModel(
  private val repository: HabitRepository,
  private val defaultName: (HabitKind) -> String,
  private val clock: Clock = Clock.systemDefaultZone(),
  /** The habit to edit, or null to create a new one. */
  private val editingId: Long? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    NewHabitUiState(kind = HabitKind.Smoking, name = defaultName(HabitKind.Smoking), isEditing = editingId != null),
  )
  val uiState: StateFlow<NewHabitUiState> = _uiState.asStateFlow()

  init {
    if (editingId != null) {
      viewModelScope.launch {
        val habit = repository.observe(editingId).first() ?: return@launch
        _uiState.update {
          it.copy(
            kind = habit.kind,
            name = habit.name,
            lastDoneAt = habit.lastDoneAt,
            costText = habit.costPerDay?.toPlainString().orEmpty(),
            minutesText = habit.minutesPerDay?.toString().orEmpty(),
          )
        }
      }
    }
  }

  fun selectKind(kind: HabitKind) = _uiState.update { s ->
    // Keep a name the user typed; replace an empty or default one.
    val keepName = s.name.isNotBlank() && s.name != defaultName(s.kind)
    s.copy(kind = kind, name = if (keepName) s.name else defaultName(kind))
  }

  fun updateName(name: String) = _uiState.update { it.copy(name = name) }

  /** Accepts digits with one decimal separator and up to two decimals. */
  fun updateCost(text: String) {
    if (text.isEmpty() || COST_INPUT.matches(text)) _uiState.update { it.copy(costText = text) }
  }

  fun updateMinutes(text: String) {
    if (text.length <= 4 && text.all(Char::isDigit)) _uiState.update { it.copy(minutesText = text) }
  }

  /** Sets when the user last did it. A time in the future is moved back to now. */
  fun setLastDone(date: LocalDate, time: LocalTime) {
    val picked = date.atTime(time).atZone(clock.zone).toInstant()
    val now = clock.instant()
    _uiState.update { it.copy(lastDoneAt = if (picked.isAfter(now)) now else picked) }
  }

  private var saving = false

  /** Saves the habit, then calls [onSaved] with its id. Does nothing if the form isn't valid. */
  fun start(onSaved: (Long) -> Unit) {
    val draft = draft() ?: return
    if (saving) return // A double tap must not save the habit twice.
    saving = true
    viewModelScope.launch {
      try {
        if (editingId == null) {
          onSaved(repository.add(draft))
        } else {
          repository.updateDetails(editingId, draft.kind, draft.name, draft.costPerDay, draft.minutesPerDay)
          onSaved(editingId)
        }
      } finally {
        saving = false
      }
    }
  }

  /** Returns the finished draft, or null when the form is not valid yet. */
  fun draft(): HabitDraft? {
    val s = _uiState.value
    if (!s.canStart) return null
    return HabitDraft(
      kind = s.kind,
      name = s.name.trim(),
      lastDoneAt = s.lastDoneAt ?: clock.instant(),
      costPerDay = s.costPerDay,
      minutesPerDay = s.minutesPerDay,
    )
  }

  private companion object {
    val COST_INPUT = Regex("""\d{0,6}([.,]\d{0,2})?""")
  }
}
