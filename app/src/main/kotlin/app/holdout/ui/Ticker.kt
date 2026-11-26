package app.holdout.ui

import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits the current time several times a second, so a seconds counter never visibly skips. */
fun clockTicker(clock: Clock = Clock.systemDefaultZone()): Flow<Instant> = flow {
  while (true) {
    emit(clock.instant())
    delay(250)
  }
}
