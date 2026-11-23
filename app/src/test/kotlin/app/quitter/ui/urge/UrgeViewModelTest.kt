package app.quitter.ui.urge

import androidx.lifecycle.SavedStateHandle
import app.quitter.data.FakeHabitRepository
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import app.quitter.model.UrgePhase
import app.quitter.ui.UserMessage
import app.quitter.ui.UserMessages
import java.time.Instant
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
class UrgeViewModelTest {
  private val start = Instant.parse("2026-11-18T21:30:00Z")
  private val now = MutableStateFlow(start)
  private val repository = FakeHabitRepository()
  private val messages = UserMessages()

  @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
  @After fun tearDown() = Dispatchers.resetMain()

  private suspend fun habit() = repository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", start, null, null))

  private fun vm(id: Long, saved: SavedStateHandle = SavedStateHandle()) =
    UrgeViewModel(id, repository, messages, now, saved, now = { now.value }, zone = ZoneOffset.UTC)

  @Test fun countsDownFromFiveMinutesAndStopsAtZero() = runTest(UnconfinedTestDispatcher()) {
    val vm = vm(habit())
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals(300, vm.uiState.value.remainingSeconds)
    assertEquals(UrgePhase.Breathe, vm.uiState.value.phase)

    now.value = start.plusSeconds(150)
    assertEquals(150, vm.uiState.value.remainingSeconds)
    assertEquals(UrgePhase.Peak, vm.uiState.value.phase)

    now.value = start.plusSeconds(400)
    assertEquals(0, vm.uiState.value.remainingSeconds)
    assertEquals(UrgePhase.Done, vm.uiState.value.phase)
  }

  @Test fun timerSurvivesTheScreenBeingRecreated() = runTest(UnconfinedTestDispatcher()) {
    val id = habit()
    val saved = SavedStateHandle()
    vm(id, saved)
    now.value = start.plusSeconds(60)
    val recreated = vm(id, saved)
    backgroundScope.launch { recreated.uiState.collect {} }
    assertEquals(240, recreated.uiState.value.remainingSeconds)
  }

  @Test fun gettingThroughItSavesTheUrgeAndCountsThisMonth() = runTest(UnconfinedTestDispatcher()) {
    val id = habit()
    repository.logUrge(id, Instant.parse("2026-10-31T10:00:00Z"), Instant.parse("2026-10-31T10:05:00Z")) // last month
    repository.logUrge(id, Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-02T10:05:00Z"))
    val vm = vm(id)
    now.value = start.plusSeconds(200)

    var done = 0
    vm.gotThroughIt { done++ }
    vm.gotThroughIt { done++ } // A double tap saves once.

    assertEquals(1, done)
    assertEquals(Triple(id, start, start.plusSeconds(200)), repository.urges.last())
    assertEquals(3, repository.urges.size)
    assertEquals(UserMessage.UrgeOutlasted(countThisMonth = 2), messages.messages.first())
  }

  @Test fun showsOneOfTheUsersReasons() = runTest(UnconfinedTestDispatcher()) {
    val id = habit()
    repository.addReason(id, "My daughter asked me to stop.", start)
    val vm = vm(id)
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals("My daughter asked me to stop.", vm.uiState.value.reason)
  }

  @Test fun noReasonsMeansNoCard() = runTest(UnconfinedTestDispatcher()) {
    val vm = vm(habit())
    backgroundScope.launch { vm.uiState.collect {} }
    assertEquals(null, vm.uiState.value.reason)
  }
}
