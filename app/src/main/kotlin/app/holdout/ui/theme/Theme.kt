package app.holdout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
  primary = PrimaryLight,
  onPrimary = OnPrimaryLight,
  primaryContainer = PrimaryContainerLight,
  onPrimaryContainer = OnPrimaryContainerLight,
  secondaryContainer = SecondaryContainerLight,
  background = BackgroundLight,
  onBackground = OnSurfaceLight,
  surface = BackgroundLight,
  onSurface = OnSurfaceLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  surfaceContainer = SurfaceContainerLight,
  outline = OutlineLight,
  outlineVariant = OutlineVariantLight,
  errorContainer = ErrorContainerLight,
  onErrorContainer = OnErrorContainerLight,
  inverseSurface = InverseSurfaceLight,
  inverseOnSurface = InverseOnSurfaceLight,
)

private val DarkColors = darkColorScheme(
  primary = PrimaryDark,
  onPrimary = OnPrimaryDark,
  primaryContainer = PrimaryContainerDark,
  onPrimaryContainer = OnPrimaryContainerDark,
  secondaryContainer = SecondaryContainerDark,
  background = BackgroundDark,
  onBackground = OnSurfaceDark,
  surface = BackgroundDark,
  onSurface = OnSurfaceDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  surfaceContainer = SurfaceContainerDark,
  outline = OutlineDark,
  outlineVariant = OutlineVariantDark,
  errorContainer = ErrorContainerDark,
  onErrorContainer = OnErrorContainerDark,
  inverseSurface = InverseSurfaceDark,
  inverseOnSurface = InverseOnSurfaceDark,
)

// Android corner radii from the design: buttons 24, cards 16, small icon tiles 20.
private val HoldoutShapes = Shapes(
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun HoldoutTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  // Brand colors on purpose: no dynamic (wallpaper) color.
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    typography = Typography,
    shapes = HoldoutShapes,
    content = content,
  )
}
