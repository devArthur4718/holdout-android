package app.quitter.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import app.quitter.Features
import app.quitter.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Benefit(val icon: ImageVector, @StringRes val text: Int)

data class WelcomeUiState(
  val benefits: List<Benefit> = emptyList(),
)

class WelcomeViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(
    WelcomeUiState(
      benefits = listOf(
        Benefit(Icons.Rounded.EmojiEvents, R.string.welcome_benefit_trophies),
        if (Features.ADS) {
          Benefit(Icons.Rounded.Campaign, R.string.welcome_benefit_ads)
        } else {
          Benefit(Icons.Rounded.VolunteerActivism, R.string.welcome_benefit_free)
        },
      ),
    ),
  )
  val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()
}
