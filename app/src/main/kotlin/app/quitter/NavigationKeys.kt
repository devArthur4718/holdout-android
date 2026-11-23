package app.quitter

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Welcome : NavKey

@Serializable data object NewHabit : NavKey

@Serializable data class CounterRunning(val habitId: Long) : NavKey

@Serializable data object Home : NavKey

/** The habit form, filled in with a saved habit. */
@Serializable data class EditHabit(val habitId: Long) : NavKey

@Serializable data object Settings : NavKey

/** One habit's stats, calendar and reasons. */
@Serializable data class HabitDetail(val habitId: Long) : NavKey

/** The 5-minute urge timer for one habit. */
@Serializable data class Urge(val habitId: Long) : NavKey

/** A screen that isn't built yet. */
@Serializable data class ComingSoon(@StringRes val title: Int) : NavKey
