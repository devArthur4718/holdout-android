package app.holdout.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import app.holdout.HoldoutApplication
import app.holdout.R
import app.holdout.ui.home.HomeRoute
import app.holdout.ui.trophies.TrophiesRoute

enum class MainTab(
  @StringRes val label: Int,
  @StringRes val title: Int,
  val icon: ImageVector,
  val selectedIcon: ImageVector,
) {
  Today(R.string.home_title_today, R.string.home_title_today, Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
  Trophies(R.string.tab_trophies, R.string.tab_trophies, Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
}

/** Screens the home links to. Each is a separate destination in the app's navigation. */
interface MainActions {
  fun addHabit()
  fun openDetail(habitId: Long)
  fun openSettings()
  fun open(@StringRes comingSoonTitle: Int)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabs(actions: MainActions) {
  var tab by rememberSaveable { mutableStateOf(MainTab.Today) }
  val colors = MaterialTheme.colorScheme
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val resources = LocalResources.current
  Scaffold(
    containerColor = colors.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text(stringResource(tab.title)) },
        actions = {
          if (tab == MainTab.Today) {
            IconButton(onClick = actions::openSettings) {
              Icon(Icons.Outlined.Settings, stringResource(R.string.title_settings), tint = colors.onSurfaceVariant)
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
      )
    },
    bottomBar = {
      NavigationBar(containerColor = colors.surfaceContainer) {
        MainTab.entries.forEach { item ->
          NavigationBarItem(
            selected = item == tab,
            onClick = { tab = item },
            icon = { Icon(if (item == tab) item.selectedIcon else item.icon, contentDescription = null) },
            label = { Text(stringResource(item.label)) },
          )
        }
      }
    },
  ) { padding ->
    when (tab) {
      MainTab.Today -> HomeRoute(
        onAddHabit = actions::addHabit,
        onOpenDetail = actions::openDetail,
        onCheckIn = { actions.open(R.string.home_checkin_title) },
        snackbarHostState = snackbarHostState,
        contentPadding = padding,
      )
      MainTab.Trophies -> TrophiesRoute(onAddHabit = actions::addHabit, contentPadding = padding)
    }
  }
}
