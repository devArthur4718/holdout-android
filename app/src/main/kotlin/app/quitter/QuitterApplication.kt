package app.quitter

import android.app.Application
import app.quitter.data.HabitRepository
import app.quitter.data.QuitterDatabase
import app.quitter.data.RoomHabitRepository
import app.quitter.notifications.MilestoneNotifications
import app.quitter.ui.UserMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds app-wide dependencies. Small enough that a DI framework isn't worth it yet. */
class QuitterApplication : Application() {
  val database: QuitterDatabase by lazy { QuitterDatabase.create(this) }
  val habitRepository: HabitRepository by lazy { RoomHabitRepository(database) }
  val userMessages = UserMessages()
  val milestones: MilestoneNotifications by lazy { MilestoneNotifications(this, habitRepository) }

  override fun onCreate() {
    super.onCreate()
    milestones.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
  }
}
