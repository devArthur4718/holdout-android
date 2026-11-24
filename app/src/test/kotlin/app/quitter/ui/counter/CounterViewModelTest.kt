package app.quitter.ui.counter

import app.quitter.data.FakeHabitRepository
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import app.quitter.model.goalProgress
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class CounterViewModelTest {

  private val lastDone = Instant.parse("2026-11-17T20:00:00Z")
  private val repository = FakeHabitRepository()
  private val now = MutableStateFlow(lastDone)

  @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
  @After fun tearDown() = Dispatchers.resetMain()

  @Test fun showsNameAndTicksFromLastDone() = runTest(UnconfinedTestDispatcher()) {
    val id = repository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", lastDone, null, null))
    val vm = CounterViewModel(id, repository, now)
    backgroundScope.launch { vm.uiState.collect {} }

    assertEquals(CounterUiState.Running("Cigarettes", goalProgress(0), startedJustNow = true), vm.uiState.value)
    now.value = lastDone.plusSeconds(65)
    assertEquals(CounterUiState.Running("Cigarettes", goalProgress(65), startedJustNow = true), vm.uiState.value)
  }

  @Test fun aHabitStartedJustNowKeepsItsMessageAfterAMinuteOnScreen() = runTest(UnconfinedTestDispatcher()) {
    val id = repository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", lastDone, null, null))
    now.value = lastDone.plusSeconds(5)
    val vm = CounterViewModel(id, repository, now)
    backgroundScope.launch { vm.uiState.collect {} }
    now.value = lastDone.plusSeconds(600)
    assertEquals(true, (vm.uiState.value as CounterUiState.Running).startedJustNow)
  }

  @Test fun aHabitStartedInThePastAimsAtItsNextGoal() = runTest(UnconfinedTestDispatcher()) {
    val id = repository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", lastDone.minus(Duration.ofDays(755)), null, null))
    val vm = CounterViewModel(id, repository, now)
    backgroundScope.launch { vm.uiState.collect {} }
    val running = vm.uiState.value as CounterUiState.Running
    assertEquals(false, running.startedJustNow)
    assertEquals(1095, running.goal.goalDays)
    assertEquals(755 * 86_400L, running.elapsedSeconds)
  }

  @Test fun clockBehindLastDoneShowsZero() = runTest(UnconfinedTestDispatcher()) {
    val id = repository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", lastDone, null, null))
    now.value = lastDone.minusSeconds(30)
    val vm = CounterViewModel(id, repository, now)
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals(CounterUiState.Running("Cigarettes", goalProgress(0), startedJustNow = true), vm.uiState.value)
  }

  @Test fun unknownHabitStaysLoading() = runTest(UnconfinedTestDispatcher()) {
    val vm = CounterViewModel(99, repository, now)
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals(CounterUiState.Loading, vm.uiState.value)
  }
}
