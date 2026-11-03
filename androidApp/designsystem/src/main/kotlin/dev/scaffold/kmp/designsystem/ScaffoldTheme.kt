package dev.scaffold.kmp.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import dev.scaffold.kmp.designsystem.tokens.DarkSemanticColors
import dev.scaffold.kmp.designsystem.tokens.LightSemanticColors
import dev.scaffold.kmp.designsystem.tokens.LocalScaffoldTokens
import dev.scaffold.kmp.designsystem.tokens.ScaffoldTokens
import dev.scaffold.kmp.designsystem.tokens.SemanticColorTokens

object ScaffoldTheme {
    val tokens: ScaffoldTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalScaffoldTokens.current
}

@Composable
fun ScaffoldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors
    val scaffoldTokens = ScaffoldTokens(colors = semanticColors)
    val colorScheme = semanticColors.toMaterialColorScheme(darkTheme = darkTheme)
    val typography = Typography(
        headlineLarge = scaffoldTokens.typography.screenTitle,
        titleMedium = scaffoldTokens.typography.cardTitle,
        bodyMedium = scaffoldTokens.typography.body,
        labelSmall = scaffoldTokens.typography.caption,
    )

    CompositionLocalProvider(LocalScaffoldTokens provides scaffoldTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

private fun SemanticColorTokens.toMaterialColorScheme(darkTheme: Boolean) =
    if (darkTheme) {
        darkColorScheme(
            primary = actionPrimary,
            onPrimary = actionOnPrimary,
            background = backgroundCanvas,
            onBackground = textPrimary,
            surface = backgroundSurface,
            onSurface = textPrimary,
            surfaceVariant = backgroundElevated,
            onSurfaceVariant = textSecondary,
            error = statusError,
            outline = borderSubtle,
        )
    } else {
        lightColorScheme(
            primary = actionPrimary,
            onPrimary = actionOnPrimary,
            background = backgroundCanvas,
            onBackground = textPrimary,
            surface = backgroundSurface,
            onSurface = textPrimary,
            surfaceVariant = backgroundElevated,
            onSurfaceVariant = textSecondary,
            error = statusError,
            outline = borderSubtle,
        )
    }
