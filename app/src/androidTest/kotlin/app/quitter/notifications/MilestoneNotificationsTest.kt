package app.quitter.notifications

import android.Manifest
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import app.quitter.AppLanguageRule
import app.quitter.QuitterApplication
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MilestoneNotificationsTest {
  @get:Rule val language = AppLanguageRule()

  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val app = instrumentation.targetContext.applicationContext as QuitterApplication
  private val manager = app.getSystemService(NotificationManager::class.java)

  @Before fun setUp() = runBlocking {
    instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
    app.habitRepository.deleteAll()
    manager.cancelAll()
    app.milestones.setEnabled(true)
  }

  @After fun tearDown() = runBlocking {
    app.milestones.setEnabled(false)
    app.habitRepository.deleteAll()
    manager.cancelAll()
  }

  private fun addCigarettes(): Long = runBlocking {
    app.habitRepository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", Instant.now().minus(Duration.ofDays(7)), null, null))
    app.habitRepository.observeAll().first().single().id
  }

  @Test fun anAlarmPostsTheTrophyNotification() = runBlocking {
    val id = addCigarettes()
    val habit = app.habitRepository.observe(id).first()!!
    app.milestones.onAlarm(id, days = 7, lastDoneAtMillis = habit.lastDoneAt.toEpochMilli())

    val posted = manager.activeNotifications.single()
    assertEquals("Smoke-free for 1 week", posted.notification.extras.getString("android.title"))
    assertEquals("Cigarettes: you made it to 1 week. Another trophy — keep going.", posted.notification.extras.getString("android.text"))
  }

  @Test fun aPersonalGoalIsNamedInTheNotification() = runBlocking {
    val id = addCigarettes()
    val habit = app.habitRepository.observe(id).first()!!
    app.habitRepository.addPersonalGoal(id, 5, "Beach trip", Instant.now())
    app.milestones.onAlarm(id, days = 5, lastDoneAtMillis = habit.lastDoneAt.toEpochMilli())

    val posted = manager.activeNotifications.single()
    assertEquals("Smoke-free for Beach trip", posted.notification.extras.getString("android.title"))
    assertEquals("Cigarettes: you made it to Beach trip. Another trophy — keep going.", posted.notification.extras.getString("android.text"))
  }

  @Test fun anAlarmFromBeforeASlipPostsNothing() = runBlocking {
    val id = addCigarettes()
    val before = app.habitRepository.observe(id).first()!!.lastDoneAt.toEpochMilli()
    app.habitRepository.logSlip(id, Instant.now(), trigger = null)

    app.milestones.onAlarm(id, days = 7, lastDoneAtMillis = before)
    assertTrue(manager.activeNotifications.isEmpty())
  }

  @Test fun turnedOffPostsNothing() = runBlocking {
    val id = addCigarettes()
    val habit = app.habitRepository.observe(id).first()!!
    app.milestones.setEnabled(false)
    app.milestones.onAlarm(id, days = 7, lastDoneAtMillis = habit.lastDoneAt.toEpochMilli())
    assertTrue(manager.activeNotifications.isEmpty())
  }
}
