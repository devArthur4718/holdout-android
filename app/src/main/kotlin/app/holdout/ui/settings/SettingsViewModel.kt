package app.holdout.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.holdout.data.HabitRepository
import app.holdout.ui.WriteError
import app.holdout.ui.attempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: HabitRepository) : ViewModel() {

  private val _writeError = MutableStateFlow<WriteError?>(null)
  /** Deleting failed: "Couldn't delete". */
  val writeError: StateFlow<WriteError?> = _writeError.asStateFlow()

  fun dismissWriteError() {
    _writeError.value = null
  }

  /** Everything stored, as CSV, for the user to save where they like. */
  suspend fun exportCsv(): String = repository.exportCsv()

  /** Deletes every habit and its history, then calls [onDone]. If that fails, nothing moves and the error shows. */
  fun deleteAll(onDone: () -> Unit) {
    viewModelScope.launch {
      if (_writeError.attempt(WriteError.Delete) { repository.deleteAll() } != null) onDone()
    }
  }
}
