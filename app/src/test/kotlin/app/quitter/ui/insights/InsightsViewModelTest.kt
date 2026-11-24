package app.quitter.ui.insights

import app.quitter.data.FakeHabitRepository
import app.quitter.model.EventCount
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import app.quitter.model.TimeOfDay
import app.quitter.ui.HabitSelection
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
  private val start = Instant.parse("2026-11-01T00:00:00Z")
  private val now = MutableStateFlow(Instant.parse("2026-11-20T12:00:00Z"))
  private val repository = FakeHabitRepository()

  @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
  @After fun tearDown() = Dispatchers.resetMain()

  @Test fun countsSlipsAndUrgesAndListsStreaks() = runTest(UnconfinedTestDispatcher()) {
    val id = repository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", start, null, null))
    val vm = InsightsViewModel(repository, now, HabitSelection(), zone = ZoneOffset.UTC, firstDayOfWeek = DayOfWeek.SUNDAY)
    backgroundScope.launch { vm.uiState.collect {} }

    var state = vm.uiState.value as InsightsUiState.Ready
    assertEquals(1, state.history.size)
    assertTrue(state.history.single().streak.isCurrent)

    repository.logSlip(id, Instant.parse("2026-11-10T19:30:00Z"), null) // Tuesday evening
    repository.logUrge(id, Instant.parse("2026-11-12T20:00:00Z"), Instant.parse("2026-11-12T20:05:00Z"))
    state = vm.uiState.value as InsightsUiState.Ready

    assertEquals(EventCount(slips = 1, urges = 1), state.hardest.byTimeOfDay[TimeOfDay.Evening])
    assertEquals(DayOfWeek.SUNDAY, state.hardest.byWeekday.first().first)
    assertEquals(EventCount(1, 0), state.hardest.byWeekday.single { it.first == DayOfWeek.TUESDAY }.second)
    assertEquals(null, state.hardest.hardest) // only 2 events
    assertEquals(2, state.history.size)
    assertTrue(state.history.first().streak.isCurrent)
    assertTrue(state.history.last().isBest) // 9.8 days before the slip beats the 9.7 days since
  }

  @Test fun noHabitsIsEmpty() = runTest(UnconfinedTestDispatcher()) {
    val vm = InsightsViewModel(repository, now, HabitSelection(), zone = ZoneOffset.UTC, firstDayOfWeek = DayOfWeek.SUNDAY)
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals(InsightsUiState.Empty, vm.uiState.value)
  }
}
