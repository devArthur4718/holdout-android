package app.quitter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quitter.data.HabitRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: HabitRepository) : ViewModel() {

  /** Everything stored, as CSV, for the user to save where they like. */
  suspend fun exportCsv(): String = repository.exportCsv()

  /** Deletes every habit and its history, then calls [onDone]. */
  fun deleteAll(onDone: () -> Unit) {
    viewModelScope.launch {
      repository.deleteAll()
      onDone()
    }
  }
}
