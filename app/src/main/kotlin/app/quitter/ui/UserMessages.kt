package app.quitter.ui

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** A short confirmation shown on the main tabs, sent by a screen that has already closed. */
sealed interface UserMessage {
  data class UrgeOutlasted(val countThisMonth: Int) : UserMessage
}

/** Carries [UserMessage]s from one screen to the main tabs. Each message is shown once. */
class UserMessages {
  private val channel = Channel<UserMessage>(Channel.BUFFERED)
  val messages: Flow<UserMessage> = channel.receiveAsFlow()

  fun post(message: UserMessage) {
    channel.trySend(message)
  }
}
