package app.holdout.ui.settings

import app.holdout.data.FakeHabitRepository
import app.holdout.model.HabitDraft
import app.holdout.model.HabitKind
import app.holdout.ui.WriteError
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
  private val repository = FakeHabitRepository()

  @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
  @After fun tearDown() = Dispatchers.resetMain()

  private suspend fun addHabit() = repository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", Instant.parse("2026-11-01T00:00:00Z"), null, null))

  @Test fun deleteAllEmptiesTheAppAndMovesOn() = runTest(UnconfinedTestDispatcher()) {
    addHabit()
    val vm = SettingsViewModel(repository)
    var done = 0
    vm.deleteAll { done++ }
    assertEquals(1, done)
    assertEquals(0, repository.habits.value.size)
    assertNull(vm.writeError.value)
  }

  @Test fun aFailedDeleteShowsCouldntDeleteAndStays() = runTest(UnconfinedTestDispatcher()) {
    addHabit()
    val vm = SettingsViewModel(repository)
    repository.failWrites = true
    var done = 0
    vm.deleteAll { done++ }
    assertEquals(0, done)
    assertEquals(WriteError.Delete, vm.writeError.value)
    assertEquals(1, repository.habits.value.size)
    vm.dismissWriteError()
    assertNull(vm.writeError.value)
  }
}
