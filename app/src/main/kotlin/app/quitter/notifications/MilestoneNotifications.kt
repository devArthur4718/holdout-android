package app.quitter.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import app.quitter.MainActivity
import app.quitter.R
import app.quitter.data.HabitRepository
import app.quitter.model.Habit
import app.quitter.model.isWholeYears
import app.quitter.model.milestoneAlerts
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A notification at each trophy. Each habit has one alarm, at its next milestone; when it goes off,
 * the notification is posted and the following one is set. The user opts in; it's off by default.
 */
class MilestoneNotifications(
  private val context: Context,
  private val repository: HabitRepository,
  private val clock: Clock = Clock.systemUTC(),
) {
  private val prefs = context.getSharedPreferences("milestones", Context.MODE_PRIVATE)
  private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
  val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
  private val lock = Mutex()

  fun setEnabled(on: Boolean) {
    prefs.edit { putBoolean(KEY_ENABLED, on) }
    _enabled.value = on
  }

  /** Whether the system lets the app post notifications at all. */
  fun systemAllows(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

  /** Keeps the alarms in step with the habits: any add, slip or delete moves them. */
  fun start(scope: CoroutineScope) {
    scope.launch {
      combine(repository.observeAll(), enabled) { habits, on -> if (on) habits else emptyList() }
        .collectLatest { schedule(it) }
    }
  }

  suspend fun reschedule() {
    schedule(if (enabled.value) repository.observeAll().first() else emptyList())
  }

  /** An alarm went off. Posts unless the streak was reset since the alarm was set. */
  suspend fun onAlarm(habitId: Long, days: Int, lastDoneAtMillis: Long) {
    val habit = repository.observe(habitId).first()
    if (enabled.value && habit != null && habit.lastDoneAt.toEpochMilli() == lastDoneAtMillis) post(habit, days)
    reschedule()
  }

  private suspend fun schedule(habits: List<Habit>) = lock.withLock {
    val alarms = context.getSystemService(AlarmManager::class.java)
    prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().forEach { id ->
      alarms.cancel(alarmIntent(id.toLong(), days = 0, lastDoneAtMillis = 0))
    }
    val now = clock.instant()
    val zone = ZoneId.systemDefault()
    habits.forEach { habit ->
      val next = habit.milestoneAlerts(now, zone, count = 1).first()
      // Not exact: Android may hold it a few minutes to save battery, which is fine for this.
      alarms.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        next.at.toEpochMilli(),
        alarmIntent(habit.id, next.days, habit.lastDoneAt.toEpochMilli()),
      )
    }
    prefs.edit { putStringSet(KEY_SCHEDULED, habits.map { it.id.toString() }.toSet()) }
  }

  /** One alarm per habit: the data URI makes each habit's intent distinct; extras don't count. */
  private fun alarmIntent(habitId: Long, days: Int, lastDoneAtMillis: Long): PendingIntent {
    val intent = Intent(context, MilestoneReceiver::class.java)
      .setAction(MilestoneReceiver.ACTION_MILESTONE)
      .setData("quitter://milestone/$habitId".toUri())
      .putExtra(MilestoneReceiver.EXTRA_HABIT_ID, habitId)
      .putExtra(MilestoneReceiver.EXTRA_DAYS, days)
      .putExtra(MilestoneReceiver.EXTRA_LAST_DONE_AT, lastDoneAtMillis)
    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun post(habit: Habit, days: Int) {
    if (Build.VERSION.SDK_INT >= 33 &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    createChannel()
    val res = context.resources
    val length = if (isWholeYears(days)) res.getQuantityString(R.plurals.years, days / 365, days / 365)
    else res.getQuantityString(R.plurals.days, days, days)
    val open = PendingIntent.getActivity(
      context, 0,
      Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
      PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_trophy)
      .setContentTitle("${context.getString(habit.kind.freeFor)} $length")
      .setContentText(context.getString(R.string.milestone_body, habit.name))
      .setContentIntent(open)
      .setAutoCancel(true)
      .build()
    NotificationManagerCompat.from(context).notify(habit.id.toInt(), notification)
  }

  private fun createChannel() {
    val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.milestone_channel), NotificationManager.IMPORTANCE_DEFAULT)
      .apply { description = context.getString(R.string.milestone_channel_detail) }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  private companion object {
    const val CHANNEL_ID = "milestones"
    const val KEY_ENABLED = "enabled"
    const val KEY_SCHEDULED = "scheduled"
  }
}
