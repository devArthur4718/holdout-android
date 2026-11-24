package app.quitter.ui.urge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quitter.data.HabitRepository
import app.quitter.model.URGE_SECONDS
import app.quitter.model.UrgePhase
import app.quitter.ui.UserMessage
import app.quitter.ui.UserMessages
import app.quitter.ui.WriteError
import app.quitter.ui.attempt
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UrgeUiState(
  val remainingSeconds: Int,
  /** One of the user's reasons for quitting, to keep in mind. Null if they haven't written any. */
  val reason: String? = null,
) {
  val phase: UrgePhase get() = UrgePhase.forRemaining(remainingSeconds)
  /** 0 to 1: how much of the timer has passed. */
  val fraction: Float get() = 1f - remainingSeconds.toFloat() / URGE_SECONDS
}

class UrgeViewModel(
  private val habitId: Long,
  private val repository: HabitRepository,
  private val messages: UserMessages,
  ticker: Flow<Instant>,
  savedState: SavedStateHandle = SavedStateHandle(),
  private val now: () -> Instant = Instant::now,
  private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

  /** When the timer started. Saved, so leaving the app or rotating doesn't restart it. */
  private val startedAt: Instant =
    savedState.get<Long>(STARTED_AT)?.let(Instant::ofEpochMilli)
      ?: now().also { savedState[STARTED_AT] = it.toEpochMilli() }

  /** Picked once per urge, so it doesn't change while the user reads it. */
  private val reason = MutableStateFlow(savedState.get<String>(REASON))

  init {
    if (!savedState.contains(REASON)) {
      viewModelScope.launch {
        val picked = repository.reasons(habitId).randomOrNull()?.text
        savedState[REASON] = picked
        reason.value = picked
      }
    }
  }

  val uiState: StateFlow<UrgeUiState> = combine(ticker, reason) { time, reason -> stateAt(time).copy(reason = reason) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), stateAt(now()))

  private var saving = false

  private val _writeError = MutableStateFlow<WriteError?>(null)
  /** Saving failed: "Couldn't save the urge". The timer stays open, so the user can try again. */
  val writeError: StateFlow<WriteError?> = _writeError.asStateFlow()

  fun dismissWriteError() {
    _writeError.value = null
  }

  /** Saves the urge, queues "Urge logged and outlasted" for the home, then calls [onDone]. */
  fun gotThroughIt(onDone: () -> Unit) {
    if (saving) return
    saving = true
    viewModelScope.launch {
      val end = now()
      val countThisMonth = _writeError.attempt(WriteError.SaveUrge) {
        repository.logUrge(habitId, startedAt, end)
        val monthStart = end.atZone(zone).toLocalDate().withDayOfMonth(1).atStartOfDay(zone).toInstant()
        repository.countUrges(habitId, monthStart)
      }
      if (countThisMonth == null) {
        saving = false // Not saved: the button works again.
        return@launch
      }
      messages.post(UserMessage.UrgeOutlasted(countThisMonth))
      onDone()
    }
  }

  private fun stateAt(time: Instant): UrgeUiState {
    val elapsed = Duration.between(startedAt, time).seconds.coerceIn(0, URGE_SECONDS.toLong()).toInt()
    return UrgeUiState(URGE_SECONDS - elapsed)
  }

  private companion object {
    const val STARTED_AT = "startedAt"
    const val REASON = "reason"
  }
}
