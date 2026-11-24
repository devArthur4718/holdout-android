package app.quitter.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Insights
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
import app.quitter.QuitterApplication
import app.quitter.R
import app.quitter.ui.UserMessage
import app.quitter.ui.home.HomeRoute
import app.quitter.ui.insights.InsightsRoute
import app.quitter.ui.trophies.TrophiesRoute

enum class MainTab(
  @StringRes val label: Int,
  @StringRes val title: Int,
  val icon: ImageVector,
  val selectedIcon: ImageVector,
) {
  Today(R.string.home_title_today, R.string.home_title_today, Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
  Trophies(R.string.tab_trophies, R.string.tab_trophies, Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
  Insights(R.string.tab_insights, R.string.tab_insights, Icons.Outlined.Insights, Icons.Filled.Insights),
}

/** Screens the home links to. Each is a separate destination in the app's navigation. */
interface MainActions {
  fun addHabit()
  fun urge(habitId: Long)
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
  // Messages from screens that have closed, e.g. the urge timer.
  LaunchedEffect(Unit) {
    (context.applicationContext as QuitterApplication).userMessages.messages.collect { message ->
      val text = when (message) {
        is UserMessage.UrgeOutlasted ->
          resources.getQuantityString(R.plurals.urge_outlasted, message.countThisMonth, message.countThisMonth)
      }
      snackbarHostState.showSnackbar(text)
    }
  }
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
        onUrge = actions::urge,
        onOpenDetail = actions::openDetail,
        onCheckIn = { actions.open(R.string.home_checkin_title) },
        snackbarHostState = snackbarHostState,
        contentPadding = padding,
      )
      MainTab.Trophies -> TrophiesRoute(onAddHabit = actions::addHabit, contentPadding = padding)
      MainTab.Insights -> InsightsRoute(onAddHabit = actions::addHabit, contentPadding = padding)
    }
  }
}
