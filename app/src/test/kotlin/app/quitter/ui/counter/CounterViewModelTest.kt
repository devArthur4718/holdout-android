package app.quitter.ui.counter

import app.quitter.data.FakeHabitRepository
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
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

    assertEquals(CounterUiState.Running("Cigarettes", 0), vm.uiState.value)
    now.value = lastDone.plusSeconds(65)
    assertEquals(CounterUiState.Running("Cigarettes", 65), vm.uiState.value)
  }

  @Test fun clockBehindLastDoneShowsZero() = runTest(UnconfinedTestDispatcher()) {
    val id = repository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", lastDone, null, null))
    now.value = lastDone.minusSeconds(30)
    val vm = CounterViewModel(id, repository, now)
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals(CounterUiState.Running("Cigarettes", 0), vm.uiState.value)
  }

  @Test fun unknownHabitStaysLoading() = runTest(UnconfinedTestDispatcher()) {
    val vm = CounterViewModel(99, repository, now)
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals(CounterUiState.Loading, vm.uiState.value)
  }

  @Test fun formatsElapsedTime() {
    assertEquals("00:00:00", formatElapsed(0))
    assertEquals("01:01:05", formatElapsed(3_665))
    assertEquals("3d 04:00:01", formatElapsed(3 * 86_400 + 4 * 3_600 + 1))
  }
}
