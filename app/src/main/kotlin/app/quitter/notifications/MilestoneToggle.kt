package app.quitter.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.quitter.QuitterApplication

/** The trophy-notification switch, shared by the counter screen and Settings. */
@Stable
class MilestoneToggle internal constructor(
  private val context: Context,
  private val milestones: MilestoneNotifications,
  private val requestPermission: () -> Unit,
) {
  internal var preference by mutableStateOf(milestones.enabled.value)
  internal var systemAllows by mutableStateOf(milestones.systemAllows())

  /** True once the user asked for them and the system shows them. */
  val isOn: Boolean get() = preference && systemAllows

  /** True when the user wants them but Quitter's notifications are off in system settings. */
  var blocked by mutableStateOf(false)
    internal set

  fun set(on: Boolean) {
    blocked = false
    when {
      !on -> milestones.setEnabled(false)
      Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
        requestPermission()
      !milestones.systemAllows() -> blocked = true
      else -> milestones.setEnabled(true)
    }
  }

  internal fun onPermissionResult(granted: Boolean) {
    systemAllows = milestones.systemAllows()
    if (granted) milestones.setEnabled(true) else blocked = true
  }

  /** Quitter's page in system settings, where its notifications can be turned back on. */
  fun openSystemSettings() {
    context.startActivity(
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
  }
}

@Composable
fun rememberMilestoneToggle(): MilestoneToggle {
  val context = LocalContext.current
  val milestones = (context.applicationContext as QuitterApplication).milestones
  var toggle: MilestoneToggle? = null
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    toggle?.onPermissionResult(granted)
  }
  toggle = remember {
    MilestoneToggle(context, milestones) {
      if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
  val preference by milestones.enabled.collectAsState()
  toggle.preference = preference
  // The user may have changed it in system settings while away.
  LifecycleResumeEffect(toggle) {
    toggle.systemAllows = milestones.systemAllows()
    if (toggle.isOn) toggle.blocked = false
    onPauseOrDispose {}
  }
  return toggle
}
