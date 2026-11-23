package app.quitter.ui.settings

import app.quitter.notifications.rememberMilestoneToggle
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarResult
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.foundation.selection.toggleable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quitter.QuitterApplication
import app.quitter.R
import app.quitter.ui.theme.QuitterTheme
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsRoute(onBack: () -> Unit, onAllDeleted: () -> Unit) {
  val context = LocalContext.current
  val resources = LocalResources.current
  val app = context.applicationContext as QuitterApplication
  val viewModel: SettingsViewModel = viewModel { SettingsViewModel(app.habitRepository) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  // The user picks where to save the file; the app never needs storage permission.
  val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch {
      val saved = runCatching {
        val csv = viewModel.exportCsv()
        withContext(Dispatchers.IO) {
          context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) } ?: error("No stream")
        }
      }.isSuccess
      snackbarHostState.showSnackbar(resources.getString(if (saved) R.string.settings_exported else R.string.settings_export_failed))
    }
  }

  val version = remember {
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
  }

  val notifications = rememberMilestoneToggle()
  val blockedMessage = stringResource(R.string.notifications_blocked)
  val openSettings = stringResource(R.string.notifications_open_settings)
  LaunchedEffect(notifications.blocked) {
    if (notifications.blocked) {
      val result = snackbarHostState.showSnackbar(blockedMessage, actionLabel = openSettings, withDismissAction = true)
      if (result == SnackbarResult.ActionPerformed) notifications.openSystemSettings()
    }
  }

  SettingsScreen(
    version = version,
    snackbarHostState = snackbarHostState,
    milestonesOn = notifications.isOn,
    onMilestonesChange = notifications::set,
    onBack = onBack,
    onExport = { exportLauncher.launch("quitter-${LocalDate.now()}.csv") },
    onDeleteAll = { viewModel.deleteAll(onAllDeleted) },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  version: String,
  snackbarHostState: SnackbarHostState,
  milestonesOn: Boolean,
  onMilestonesChange: (Boolean) -> Unit,
  onBack: () -> Unit,
  onExport: () -> Unit,
  onDeleteAll: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  var confirmDelete by rememberSaveable { mutableStateOf(false) }
  Scaffold(
    containerColor = colors.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.title_settings)) },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
      )
    },
  ) { padding ->
    Column(
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(padding)
        .padding(horizontal = 4.dp),
    ) {
      SectionHeader(R.string.settings_notifications_section)
      SettingsRow(
        Icons.Rounded.EmojiEvents, stringResource(R.string.settings_milestones), stringResource(R.string.settings_milestones_detail),
        checked = milestonesOn, onClick = { onMilestonesChange(!milestonesOn) },
      )
      HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = colors.outlineVariant)
      SectionHeader(R.string.settings_data_section)
      SettingsRow(Icons.Rounded.Download, stringResource(R.string.settings_export), stringResource(R.string.settings_export_detail), onClick = onExport)
      SettingsRow(
        Icons.Rounded.DeleteForever, stringResource(R.string.settings_delete_all), stringResource(R.string.settings_delete_all_detail),
        tint = colors.error, onClick = { confirmDelete = true },
      )
      HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = colors.outlineVariant)
      SectionHeader(R.string.settings_privacy_section)
      SettingsRow(Icons.Rounded.Lock, stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_detail))
      Text(
        stringResource(R.string.settings_version, version),
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline,
        modifier = Modifier.padding(16.dp),
      )
    }
  }
  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text(stringResource(R.string.settings_delete_all_title)) },
      text = { Text(stringResource(R.string.settings_delete_all_body)) },
      confirmButton = {
        TextButton(onClick = { confirmDelete = false; onDeleteAll() }) {
          Text(stringResource(R.string.settings_delete_all_confirm), color = colors.error)
        }
      },
      dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.slip_cancel)) } },
    )
  }
}

@Composable
private fun SectionHeader(text: Int) {
  Text(
    stringResource(text),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier
      .padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
      .semantics { heading() },
  )
}

@Composable
private fun SettingsRow(
  icon: ImageVector,
  title: String,
  detail: String,
  tint: Color = Color.Unspecified,
  /** Shows a switch; the whole row toggles it. */
  checked: Boolean? = null,
  onClick: (() -> Unit)? = null,
) {
  val colors = MaterialTheme.colorScheme
  val color = if (tint == Color.Unspecified) colors.onSurface else tint
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(
        when {
          checked != null && onClick != null -> Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = { onClick() })
          onClick != null -> Modifier.clickable(onClick = onClick)
          else -> Modifier
        },
      )
      .heightIn(min = 56.dp)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = if (tint == Color.Unspecified) colors.onSurfaceVariant else tint)
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
      Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
    if (checked != null) Switch(checked = checked, onCheckedChange = null)
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SettingsPreview() {
  QuitterTheme { Surface { SettingsScreen("1.0", SnackbarHostState(), milestonesOn = true, onMilestonesChange = {}, onBack = {}, onExport = {}, onDeleteAll = {}) } }
}
