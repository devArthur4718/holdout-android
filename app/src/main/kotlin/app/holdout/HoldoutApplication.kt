package app.holdout

import android.app.Application
import app.holdout.data.HabitRepository
import app.holdout.data.HoldoutDatabase
import app.holdout.data.RoomHabitRepository
import app.holdout.notifications.MilestoneNotifications
import app.holdout.ui.HabitSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds app-wide dependencies. Small enough that a DI framework isn't worth it yet. */
class HoldoutApplication : Application() {
  val database: HoldoutDatabase by lazy { HoldoutDatabase.create(this) }
  val habitRepository: HabitRepository by lazy { RoomHabitRepository(database) }
  val habitSelection = HabitSelection()
  val milestones: MilestoneNotifications by lazy { MilestoneNotifications(this, habitRepository) }

  override fun onCreate() {
    super.onCreate()
    milestones.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
  }
}
