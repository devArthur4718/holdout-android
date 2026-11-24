package app.quitter.ui.trophies

import app.quitter.data.FakeHabitRepository
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import app.quitter.model.PersonalGoalError
import app.quitter.ui.HabitSelection
import app.quitter.ui.WriteError
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrophiesViewModelTest {
  private val start = Instant.parse("2026-01-01T00:00:00Z")
  private val now = MutableStateFlow(Instant.parse("2026-02-01T00:00:00Z")) // 31 days
  private val repository = FakeHabitRepository()
  private val selection = HabitSelection()

  @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
  @After fun tearDown() = Dispatchers.resetMain()

  private suspend fun add(name: String) = repository.add(HabitDraft(HabitKind.Smoking, name, start, null, null))

  private fun TestScope.vm(): TrophiesViewModel {
    val vm = TrophiesViewModel(repository, now, selection, now = { now.value })
    backgroundScope.launch { vm.uiState.collect {} }
    return vm
  }

  private val TrophiesViewModel.ready get() = uiState.value as TrophiesUiState.Ready

  @Test fun noHabitsIsEmpty() = runTest(UnconfinedTestDispatcher()) {
    assertEquals(TrophiesUiState.Empty, vm().uiState.value)
  }

  @Test fun showsTheNewestHabitAndRemembersThePickAcrossScreens() = runTest(UnconfinedTestDispatcher()) {
    val first = add("Cigarettes")
    add("Alcohol")
    val vm = vm()
    assertEquals("Alcohol", vm.ready.selected.name)
    vm.select(first)
    assertEquals("Cigarettes", vm.ready.selected.name)
    // Another screen with the same selection (e.g. Insights) starts on the same habit.
    assertEquals(first, selection.selectedId.value)
  }

  @Test fun earnedAndNextTrophies() = runTest(UnconfinedTestDispatcher()) {
    add("Cigarettes")
    val vm = vm()
    val trophies = vm.ready.trophies
    assertEquals(listOf(1, 7, 30), trophies.filter { it.isEarned }.map { it.days })
    assertEquals(listOf(60), trophies.filter { it.isNext }.map { it.days })
    assertEquals(60, vm.ready.next.goalDays)
  }

  @Test fun addingAPersonalGoalPutsItInTheGridAndTheArc() = runTest(UnconfinedTestDispatcher()) {
    add("Cigarettes")
    val vm = vm()
    vm.newGoal()
    vm.updateDays("45")
    vm.updateName("  Beach trip  ")
    vm.save()

    assertNull(vm.editor.value)
    val goal = repository.goals.value.single()
    assertEquals(45, goal.days)
    assertEquals("Beach trip", goal.name)
    assertEquals(45, vm.ready.next.goalDays)
    assertEquals("Beach trip", vm.ready.next.personal?.name)
    assertTrue(vm.ready.trophies.single { it.days == 45 }.isNext)
  }

  @Test fun invalidOrDuplicateDaysAreRejectedInline() = runTest(UnconfinedTestDispatcher()) {
    add("Cigarettes")
    val vm = vm()
    vm.newGoal()
    vm.save()
    assertEquals(PersonalGoalError.InvalidDays, vm.editor.value?.error)
    vm.updateDays("30")
    assertNull(vm.editor.value?.error) // typing clears it
    vm.save()
    assertEquals(PersonalGoalError.Duplicate(30), vm.editor.value?.error)
    vm.updateDays("4000")
    vm.save()
    assertEquals(PersonalGoalError.InvalidDays, vm.editor.value?.error)
    vm.updateDays("12a")
    assertEquals("4000", vm.editor.value?.daysText) // letters aren't accepted
    assertTrue(repository.goals.value.isEmpty())
  }

  @Test fun editingAndDeletingAGoal() = runTest(UnconfinedTestDispatcher()) {
    add("Cigarettes")
    val vm = vm()
    vm.newGoal()
    vm.updateDays("45")
    vm.save()
    val goal = repository.goals.value.single()

    vm.editGoal(goal)
    assertEquals("45", vm.editor.value?.daysText)
    vm.updateDays("50")
    vm.updateName("x".repeat(60))
    vm.save()
    assertEquals(50, repository.goals.value.single().days)
    assertEquals(40, repository.goals.value.single().name?.length)

    vm.editGoal(repository.goals.value.single())
    vm.askToDelete()
    assertTrue(vm.editor.value!!.confirmingDelete)
    vm.confirmDelete()
    assertNull(vm.editor.value)
    assertTrue(repository.goals.value.isEmpty())
  }

  @Test fun aDoubleTapOnSaveAddsTheGoalOnce() = runTest {
    // Writes wait for the test to let them run, as a real database write would take a moment.
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    add("Cigarettes")
    val vm = vm()
    runCurrent()
    vm.newGoal()
    vm.updateDays("45")
    vm.save()
    vm.save()
    advanceUntilIdle()
    assertEquals(1, repository.goals.value.size)
    assertNull(vm.editor.value)
    // Once the first save is done, Save works again.
    vm.newGoal()
    vm.updateDays("50")
    vm.save()
    advanceUntilIdle()
    assertEquals(2, repository.goals.value.size)
  }

  @Test fun aFailedSaveShowsCouldntSaveAndKeepsTheForm() = runTest(UnconfinedTestDispatcher()) {
    add("Cigarettes")
    val vm = vm()
    vm.newGoal()
    vm.updateDays("45")
    repository.failWrites = true
    vm.save()
    assertEquals(WriteError.Save, vm.writeError.value)
    assertEquals("45", vm.editor.value?.daysText)
    vm.dismissWriteError()
    assertNull(vm.writeError.value)
  }

  @Test fun aFailedDeleteShowsCouldntSave() = runTest(UnconfinedTestDispatcher()) {
    add("Cigarettes")
    val vm = vm()
    vm.newGoal()
    vm.updateDays("45")
    vm.save()
    vm.editGoal(repository.goals.value.single())
    vm.askToDelete()
    repository.failWrites = true
    vm.confirmDelete()
    assertEquals(WriteError.Save, vm.writeError.value)
    assertEquals(1, repository.goals.value.size)
  }
}
