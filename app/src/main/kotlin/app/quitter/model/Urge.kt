package app.quitter.model

/** How long the urge timer runs. Most cravings peak and fade within a few minutes. */
const val URGE_SECONDS = 300

/** What the coach says, by how much of the timer is left. */
enum class UrgePhase {
  Breathe, Peak, Fading, Done;

  companion object {
    fun forRemaining(seconds: Int) = when {
      seconds > 180 -> Breathe
      seconds > 60 -> Peak
      seconds > 0 -> Fading
      else -> Done
    }
  }
}
