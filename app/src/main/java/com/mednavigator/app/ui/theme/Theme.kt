package com.mednavigator.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CozyMedicalColorScheme = lightColorScheme(
    primary                 = CozyPrimary,
    onPrimary               = CozyOnPrimary,
    primaryContainer        = CozyPrimaryContainer,
    onPrimaryContainer      = CozyOnPrimaryContainer,
    inversePrimary          = CozyInversePrimary,
    secondary               = CozySecondary,
    onSecondary             = CozyOnSecondary,
    secondaryContainer      = CozySecondaryContainer,
    onSecondaryContainer    = CozyOnSecondaryContainer,
    tertiary                = CozyTertiary,
    onTertiary              = CozyOnTertiary,
    tertiaryContainer       = CozyTertiaryContainer,
    onTertiaryContainer     = CozyOnTertiaryContainer,
    error                   = CozyError,
    onError                 = CozyOnError,
    errorContainer          = CozyErrorContainer,
    onErrorContainer        = CozyOnErrorContainer,
    background              = CozyBackground,
    onBackground            = CozyOnBackground,
    surface                 = CozySurface,
    onSurface               = CozyOnSurface,
    surfaceVariant          = CozySurfaceVariant,
    onSurfaceVariant        = CozyOnSurfaceVariant,
    surfaceTint             = CozySurfaceTint,
    inverseSurface          = CozyInverseSurface,
    inverseOnSurface        = CozyInverseOnSurface,
    outline                 = CozyOutline,
    outlineVariant          = CozyOutlineVariant,
    surfaceBright           = CozySurfaceBright,
    surfaceDim              = CozySurfaceDim,
    surfaceContainerLowest  = CozySurfaceContainerLowest,
    surfaceContainerLow     = CozySurfaceContainerLow,
    surfaceContainer        = CozySurfaceContainer,
    surfaceContainerHigh    = CozySurfaceContainerHigh,
    surfaceContainerHighest = CozySurfaceContainerHighest,
)

private val CozyShapes = Shapes(
    extraSmall  = RoundedCornerShape(8.dp),
    small       = RoundedCornerShape(16.dp),
    medium      = RoundedCornerShape(24.dp),
    large       = RoundedCornerShape(32.dp),
    extraLarge  = RoundedCornerShape(48.dp),
)

@Composable
fun MedNavigatorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CozyMedicalColorScheme,
        typography  = Typography,
        shapes      = CozyShapes,
        content     = content
    )
}