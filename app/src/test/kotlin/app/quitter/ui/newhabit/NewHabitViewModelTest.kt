package app.quitter.ui.newhabit

import app.quitter.ui.WriteError
import app.quitter.data.FakeHabitRepository
import app.quitter.model.HabitKind
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class NewHabitViewModelTest {

  @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
  @After fun tearDown() = Dispatchers.resetMain()

  private val now = Instant.parse("2026-11-17T20:00:00Z")
  private val clock = Clock.fixed(now, ZoneOffset.UTC)
  private val repository = FakeHabitRepository()
  private fun vm() = NewHabitViewModel(repository = repository, defaultName = { if (it == HabitKind.Smoking) "Cigarettes" else it.name }, clock = clock)

  @Test fun startsWithSmokingAndDefaultName() {
    val s = vm().uiState.value
    assertEquals(HabitKind.Smoking, s.kind)
    assertEquals("Cigarettes", s.name)
    assertNull(s.lastDoneAt)
  }

  @Test fun changingKindReplacesDefaultName() {
    val vm = vm()
    vm.selectKind(HabitKind.Alcohol)
    assertEquals("Alcohol", vm.uiState.value.name)
  }

  @Test fun changingKindKeepsTypedName() {
    val vm = vm()
    vm.updateName("Rolling tobacco")
    vm.selectKind(HabitKind.Vaping)
    assertEquals("Rolling tobacco", vm.uiState.value.name)
  }

  @Test fun blankNameBlocksStart() {
    val vm = vm()
    vm.updateName("  ")
    assertFalse(vm.uiState.value.canStart)
    assertNull(vm.draft())
  }

  @Test fun projectionMatchesDesignExample() {
    val vm = vm()
    vm.updateCost("11.90")
    vm.updateMinutes("90")
    val s = vm.uiState.value
    assertEquals(0, BigDecimal("4343.50").compareTo(s.yearlyCost))
    assertEquals(547, s.yearlyHours)
  }

  @Test fun costAcceptsCommaAndRejectsJunk() {
    val vm = vm()
    vm.updateCost("4,2")
    vm.updateCost("4,2x")
    vm.updateCost("4,255")
    assertEquals("4,2", vm.uiState.value.costText)
    assertEquals(0, BigDecimal("4.2").compareTo(vm.uiState.value.costPerDay))
  }

  @Test fun tooManyMinutesBlocksStart() {
    val vm = vm()
    vm.updateMinutes("1441")
    assertTrue(vm.uiState.value.minutesTooHigh)
    assertFalse(vm.uiState.value.canStart)
  }

  @Test fun futureLastDoneIsClampedToNow() {
    val vm = vm()
    vm.setLastDone(LocalDate.of(2026, 11, 17), LocalTime.of(23, 0))
    assertEquals(now, vm.uiState.value.lastDoneAt)
  }

  @Test fun startUsesNowWhenLastDoneNotPicked() {
    val vm = vm()
    vm.updateCost("8")
    val draft = vm.draft()!!
    assertEquals(now, draft.lastDoneAt)
    assertEquals("Cigarettes", draft.name)
    assertEquals(0, BigDecimal(8).compareTo(draft.costPerDay))
    assertNull(draft.minutesPerDay)
  }

  @Test fun costAndTimeAreOptional() {
    val vm = vm()
    assertTrue(vm.uiState.value.canStart)
    val draft = vm.draft()!!
    assertNull(draft.costPerDay)
    assertNull(draft.minutesPerDay)
  }

  @Test fun startSavesHabitOnceAndReportsItsId() {
    val vm = vm()
    val ids = mutableListOf<Long>()
    vm.start { ids += it }
    assertEquals(listOf(1L), ids)
    assertEquals("Cigarettes", repository.habits.value.single().name)
  }

  @Test fun startDoesNothingWhenFormInvalid() {
    val vm = vm()
    vm.updateName("")
    vm.start { error("must not be called") }
    assertTrue(repository.habits.value.isEmpty())
  }

  @Test fun editingFillsTheFormAndSavesWithoutTouchingTheCounter() = kotlinx.coroutines.test.runTest(UnconfinedTestDispatcher()) {
    val start = Instant.parse("2026-10-01T00:00:00Z")
    val id = repository.add(app.quitter.model.HabitDraft(HabitKind.Alcohol, "Wine", start, BigDecimal("8"), 60))
    val vm = NewHabitViewModel(repository, defaultName = { it.name }, clock = clock, editingId = id)

    assertTrue(vm.uiState.value.isEditing)
    assertEquals("Wine", vm.uiState.value.name)
    assertEquals("8", vm.uiState.value.costText)
    assertEquals("60", vm.uiState.value.minutesText)

    vm.updateName("Red wine")
    vm.updateCost("")
    var saved: Long? = null
    vm.start { saved = it }

    assertEquals(id, saved)
    val habit = repository.habits.value.single()
    assertEquals("Red wine", habit.name)
    assertNull(habit.costPerDay)
    assertEquals(start, habit.lastDoneAt)
  }

  @Test fun aFailedSaveShowsAnErrorAndCanBeRetried() {
    val vm = vm()
    repository.failWrites = true
    vm.start { error("must not be called") }
    assertEquals(WriteError.SaveHabit, vm.writeError.value)
    assertTrue(repository.habits.value.isEmpty())

    vm.dismissWriteError()
    assertNull(vm.writeError.value)
    repository.failWrites = false
    val ids = mutableListOf<Long>()
    vm.start { ids += it }
    assertEquals(listOf(1L), ids)
  }

  @Test fun aFailedEditShowsAnError() = kotlinx.coroutines.test.runTest(UnconfinedTestDispatcher()) {
    val id = repository.add(app.quitter.model.HabitDraft(HabitKind.Alcohol, "Wine", now, null, null))
    val vm = NewHabitViewModel(repository, defaultName = { it.name }, clock = clock, editingId = id)
    vm.updateName("Red wine")
    repository.failWrites = true
    vm.start { error("must not be called") }
    assertEquals(WriteError.SaveHabit, vm.writeError.value)
    assertEquals("Wine", repository.habits.value.single().name)
  }
}
