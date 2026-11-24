package app.quitter.ui

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.quitter.R
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

/** A database write that failed, as the user is told about it. Same wording as the iOS app. */
enum class WriteError(@StringRes val title: Int, @StringRes val message: Int) {
  SaveHabit(R.string.error_save_habit, R.string.error_try_again),
  LogSlip(R.string.error_log_slip, R.string.error_log_slip_body),
  Save(R.string.error_save, R.string.error_try_again),
  Delete(R.string.error_delete, R.string.error_try_again),
}

/**
 * Runs a database write. If it throws, the app doesn't crash: [error] is shown instead and null
 * is returned. Cancellation (the screen closing) still cancels.
 */
internal suspend fun <T> MutableStateFlow<WriteError?>.attempt(error: WriteError, write: suspend () -> T): T? =
  try {
    write()
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    value = error
    null
  }

/** The alert for a failed write, with an OK button. Shows nothing when [error] is null. */
@Composable
fun WriteErrorDialog(error: WriteError?, onDismiss: () -> Unit) {
  if (error == null) return
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(error.title)) },
    text = { Text(stringResource(error.message)) },
    confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
  )
}
