package app.holdout.ui.home

import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import app.holdout.ui.WriteError
import app.holdout.data.FakeHabitRepository
import app.holdout.model.HabitDraft
import app.holdout.model.HabitKind
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import app.holdout.model.SlipTrigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  private val start = Instant.parse("2026-11-01T00:00:00Z")
  private val now = MutableStateFlow(start.plusSeconds(3_600))
  private val repository = FakeHabitRepository()

  @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
  @After fun tearDown() = Dispatchers.resetMain()

  private suspend fun add(name: String) = repository.add(HabitDraft(HabitKind.Smoking, name, start, null, null))

  @Test fun showsNewestHabitByDefault() = runTest(UnconfinedTestDispatcher()) {
    add("Cigarettes")
    add("Alcohol")
    val vm = HomeViewModel(repository, now)
    backgroundScope.launch { vm.uiState.collect {} }

    val state = vm.uiState.value as HomeUiState.Ready
    assertEquals(listOf("Cigarettes", "Alcohol"), state.chips.map { it.name })
    assertEquals("Alcohol", state.selected.name)
  }

  @Test fun selectingAChipSwitchesHabit() = runTest(UnconfinedTestDispatcher()) {
    val first = add("Cigarettes")
    add("Alcohol")
    val vm = HomeViewModel(repository, now)
    backgroundScope.launch { vm.uiState.collect {} }

    vm.select(first)
    assertEquals("Cigarettes", (vm.uiState.value as HomeUiState.Ready).selected.name)
  }

  @Test fun counterTicks() = runTest(UnconfinedTestDispatcher()) {
    add("Cigarettes")
    val vm = HomeViewModel(repository, now)
    backgroundScope.launch { vm.uiState.collect {} }

    assertEquals(3_600L, (vm.uiState.value as HomeUiState.Ready).progress.elapsedSeconds)
    now.value = now.value.plusSeconds(5)
    assertEquals(3_605L, (vm.uiState.value as HomeUiState.Ready).progress.elapsedSeconds)
  }

  @Test fun loggingASlipRestartsTheCounterAndReportsTheStreak() = runTest(UnconfinedTestDispatcher()) {
    val id = add("Cigarettes")
    val vm = HomeViewModel(repository, now, now = { now.value })
    backgroundScope.launch { vm.uiState.collect {} }

    vm.logSlip(id, SlipTrigger.Stress)

    assertEquals(HomeEvent.SlipLogged(longestStreakSeconds = 3_600), vm.events.first())
    assertEquals(listOf(Triple(id, now.value, SlipTrigger.Stress)), repository.slips.value)
    val state = vm.uiState.value as HomeUiState.Ready
    assertEquals(0L, state.progress.elapsedSeconds)
    assertEquals(false, state.progress.isPersonalBest)
  }

  @Test fun noHabitsLeftShowsTheEmptyHome() = runTest(UnconfinedTestDispatcher()) {
    val id = add("Cigarettes")
    val vm = HomeViewModel(repository, now)
    backgroundScope.launch { vm.uiState.collect {} }
    repository.delete(id)
    assertEquals(HomeUiState.Empty, vm.uiState.value)
  }

  @Test fun aFailedSlipShowsAnErrorAndKeepsTheCounter() = runTest(UnconfinedTestDispatcher()) {
    val id = add("Cigarettes")
    val vm = HomeViewModel(repository, now, now = { now.value })
    backgroundScope.launch { vm.uiState.collect {} }
    repository.failWrites = true

    vm.logSlip(id, SlipTrigger.Stress)

    assertEquals(WriteError.LogSlip, vm.writeError.value)
    assertEquals(3_600L, (vm.uiState.value as HomeUiState.Ready).progress.elapsedSeconds)
    assertTrue(repository.slips.value.isEmpty())
    vm.dismissWriteError()
    assertNull(vm.writeError.value)
  }
}
