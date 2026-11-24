package app.quitter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.quitter.ui.counter.CounterRoute
import app.quitter.ui.detail.HabitDetailRoute
import app.quitter.ui.newhabit.NewHabitRoute
import app.quitter.ui.onboarding.WelcomeRoute
import app.quitter.ui.main.MainActions
import app.quitter.ui.main.MainTabs
import app.quitter.ui.placeholder.ComingSoonScreen
import app.quitter.ui.settings.SettingsRoute

@Composable
fun MainNavigation() {
  // People who already have a habit skip onboarding. Saved so rotation doesn't re-check.
  val repository = (LocalContext.current.applicationContext as QuitterApplication).habitRepository
  var hasHabits by rememberSaveable { mutableStateOf<Boolean?>(null) }
  LaunchedEffect(Unit) { if (hasHabits == null) hasHabits = repository.hasHabits() }

  when (hasHabits) {
    null -> Unit // A few milliseconds; the window background shows meanwhile.
    true -> QuitterNavDisplay(start = Home)
    false -> QuitterNavDisplay(start = Welcome)
  }
}

@Composable
private fun QuitterNavDisplay(start: NavKey) {
  val backStack = rememberNavBackStack(start)

  /** Replaces the whole stack, so Back can't return to finished onboarding steps. */
  fun resetTo(key: NavKey) {
    backStack.clear()
    backStack.add(key)
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    // Gives each screen its own ViewModel, cleared when the screen leaves the stack.
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
      entry<Welcome> { WelcomeRoute(onAddHabit = { backStack.add(NewHabit) }) }
      entry<NewHabit> {
        NewHabitRoute(
          onBack = { backStack.removeLastOrNull() },
          // The habit is saved now; going back to the form would only make a duplicate.
          onStarted = { id -> resetTo(CounterRunning(id)) },
        )
      }
      entry<CounterRunning> { key ->
        CounterRoute(habitId = key.habitId, onGoToHabits = { resetTo(Home) })
      }
      entry<Home> {
        MainTabs(
          object : MainActions {
            override fun addHabit() { backStack.add(NewHabit) }
            override fun openDetail(habitId: Long) { backStack.add(HabitDetail(habitId)) }
            override fun openSettings() { backStack.add(Settings) }
            override fun open(comingSoonTitle: Int) { backStack.add(ComingSoon(comingSoonTitle)) }
          },
        )
      }
      entry<HabitDetail> { key ->
        HabitDetailRoute(
          habitId = key.habitId,
          onBack = { backStack.removeLastOrNull() },
          onEdit = { backStack.add(EditHabit(key.habitId)) },
        )
      }
      entry<EditHabit> { key ->
        NewHabitRoute(
          editingId = key.habitId,
          onBack = { backStack.removeLastOrNull() },
          onStarted = { backStack.removeLastOrNull() },
        )
      }
      entry<Settings> {
        SettingsRoute(
          onBack = { backStack.removeLastOrNull() },
          // Nothing is left, so start again from the welcome screen.
          onAllDeleted = { resetTo(Welcome) },
        )
      }
      entry<ComingSoon> { key ->
        ComingSoonScreen(stringResource(key.title), onBack = { backStack.removeLastOrNull() })
      }
    },
  )
}
