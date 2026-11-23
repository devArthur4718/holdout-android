package app.quitter.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.quitter.QuitterApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** A milestone alarm went off. Only the app's own alarms reach it (not exported). */
class MilestoneReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_MILESTONE) return
    val milestones = (context.applicationContext as QuitterApplication).milestones
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
      try {
        milestones.onAlarm(
          habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1),
          days = intent.getIntExtra(EXTRA_DAYS, 0),
          lastDoneAtMillis = intent.getLongExtra(EXTRA_LAST_DONE_AT, 0),
        )
      } finally {
        pending.finish()
      }
    }
  }

  companion object {
    const val ACTION_MILESTONE = "app.quitter.MILESTONE"
    const val EXTRA_HABIT_ID = "habitId"
    const val EXTRA_DAYS = "days"
    const val EXTRA_LAST_DONE_AT = "lastDoneAt"
  }
}

/** Alarms are lost on restart and app update, and quiet hours move with the time zone. */
class RescheduleReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action !in RESCHEDULE_ACTIONS) return
    val milestones = (context.applicationContext as QuitterApplication).milestones
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
      try {
        milestones.reschedule()
      } finally {
        pending.finish()
      }
    }
  }

  private companion object {
    val RESCHEDULE_ACTIONS = setOf(
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_MY_PACKAGE_REPLACED,
      Intent.ACTION_TIMEZONE_CHANGED,
    )
  }
}
