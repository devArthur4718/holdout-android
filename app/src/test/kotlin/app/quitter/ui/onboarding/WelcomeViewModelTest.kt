package app.quitter.ui.onboarding

import app.quitter.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WelcomeViewModelTest {

  @Test
  fun uiState_listsTheBenefitsInOrder() {
    val state = WelcomeViewModel().uiState.value
    assertEquals(
      listOf(R.string.welcome_benefit_trophies, R.string.welcome_benefit_free),
      state.benefits.map { it.text },
    )
  }
}
