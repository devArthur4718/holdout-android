package app.quitter.ui.detail

import org.junit.Assert.assertTrue
import app.quitter.ui.WriteError
import androidx.lifecycle.SavedStateHandle
import app.quitter.data.FakeHabitRepository
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import app.quitter.model.REASON_MAX_LENGTH
import app.quitter.model.SlipTrigger
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class HabitDetailViewModelTest {
  private val start = Instant.parse("2026-09-21T00:00:00Z")
  private val now = MutableStateFlow(Instant.parse("2026-11-18T12:00:00Z"))
  private val repository = FakeHabitRepository()

  @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
  @After fun tearDown() = Dispatchers.resetMain()

  private suspend fun vm(saved: SavedStateHandle = SavedStateHandle()): HabitDetailViewModel {
    val id = repository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", start, null, null))
    return HabitDetailViewModel(id, repository, now, saved, now = { now.value }, zone = ZoneOffset.UTC, firstDayOfWeek = DayOfWeek.MONDAY)
  }

  private val HabitDetailViewModel.ready get() = uiState.value as HabitDetailUiState.Ready

  @Test fun startsOnStatsAndTheCurrentMonth() = runTest(UnconfinedTestDispatcher()) {
    val vm = vm()
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals(DetailTab.Stats, vm.ready.tab)
    assertEquals(YearMonth.of(2026, 11), vm.ready.calendar.month)
    assertEquals(start, vm.ready.trackingSince)
  }

  @Test fun tabAndMonthSurviveTheScreenBeingRecreated() = runTest(UnconfinedTestDispatcher()) {
    val saved = SavedStateHandle()
    val vm = vm(saved)
    backgroundScope.launch { vm.uiState.collect {} }
    vm.selectTab(DetailTab.Calendar)
    vm.showPreviousMonth()

    val again = HabitDetailViewModel(1, repository, now, saved, now = { now.value }, zone = ZoneOffset.UTC)
    backgroundScope.launch { again.uiState.collect {} }
    assertEquals(DetailTab.Calendar, again.ready.tab)
    assertEquals(YearMonth.of(2026, 10), again.ready.calendar.month)
  }

  @Test fun calendarStaysBetweenTrackingStartAndNow() = runTest(UnconfinedTestDispatcher()) {
    val vm = vm()
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals(false, vm.ready.calendar.canGoForward)
    vm.showPreviousMonth()
    vm.showPreviousMonth()
    assertEquals(YearMonth.of(2026, 9), vm.ready.calendar.month)
    assertEquals(false, vm.ready.calendar.canGoBack)
    vm.showPreviousMonth()
    assertEquals(YearMonth.of(2026, 9), vm.ready.calendar.month)
  }

  @Test fun reasonsAreTrimmedCappedAndDeletedWhenCleared() = runTest(UnconfinedTestDispatcher()) {
    val vm = vm()
    backgroundScope.launch { vm.uiState.collect {} }
    vm.addReason("   ")
    vm.addReason("  My daughter asked me to stop. ")
    vm.addReason("x".repeat(500))
    assertEquals(listOf("My daughter asked me to stop.", "x".repeat(REASON_MAX_LENGTH)), vm.ready.reasons.map { it.text })

    val first = vm.ready.reasons.first().id
    vm.updateReason(first, "For my lungs")
    assertEquals("For my lungs", vm.ready.reasons.first().text)
    vm.updateReason(first, " ")
    assertEquals(1, vm.ready.reasons.size)
  }

  @Test fun loggingASlipMarksTodayAndReportsTheStreak() = runTest(UnconfinedTestDispatcher()) {
    val vm = vm()
    backgroundScope.launch { vm.uiState.collect {} }
    vm.logSlip(SlipTrigger.Boredom)
    assertEquals(HabitDetailEvent.SlipLogged(longestStreakSeconds = 58 * 86_400L + 43_200), vm.events.first())
    val today = vm.ready.calendar.days.filterNotNull().first { it.isToday }
    assertEquals(app.quitter.model.DayState.Slip, today.state)
    assertEquals(start, vm.ready.trackingSince)
  }

  @Test fun deletedHabitClosesTheScreen() = runTest(UnconfinedTestDispatcher()) {
    val vm = vm()
    backgroundScope.launch { vm.uiState.collect {} }
    repository.habits.value = emptyList()
    assertEquals(HabitDetailUiState.Gone, vm.uiState.value)
  }

  @Test fun deletingTheHabitClosesTheScreenAndRemovesItsHistory() = runTest(UnconfinedTestDispatcher()) {
    val vm = vm()
    backgroundScope.launch { vm.uiState.collect {} }
    vm.addReason("For my lungs")
    vm.deleteHabit()
    assertEquals(HabitDetailUiState.Gone, vm.uiState.value)
    assertEquals(emptyList<Any>(), repository.reasons.value)
  }

  @Test fun failedWritesShowCouldntSave() = runTest(UnconfinedTestDispatcher()) {
    val vm = vm()
    backgroundScope.launch { vm.uiState.collect {} }
    vm.addReason("For my lungs")
    val reasonId = repository.reasons.value.single().second.id
    repository.failWrites = true

    vm.addReason("Another")
    assertEquals(WriteError.Save, vm.writeError.value)
    vm.dismissWriteError()
    vm.updateReason(reasonId, "Changed")
    assertEquals(WriteError.Save, vm.writeError.value)
    vm.dismissWriteError()
    vm.deleteReason(reasonId)
    assertEquals(WriteError.Save, vm.writeError.value)
    vm.dismissWriteError()
    vm.logSlip(null)
    assertEquals(WriteError.Save, vm.writeError.value)
    vm.dismissWriteError()
    vm.deleteHabit()
    assertEquals(WriteError.Save, vm.writeError.value)

    assertEquals(listOf("For my lungs"), repository.reasons.value.map { it.second.text })
    assertTrue(vm.uiState.value is HabitDetailUiState.Ready)
  }
}
