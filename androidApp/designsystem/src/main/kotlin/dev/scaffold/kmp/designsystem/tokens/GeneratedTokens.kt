// AUTO-GENERATED FROM design-tokens/tokens.json — DO NOT EDIT BY HAND.
package dev.scaffold.kmp.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object PrimitiveTokens {
    object Colors {
        val brand500 = Color(0xFF2563EB)
        val brand400 = Color(0xFF60A5FA)
        val neutral50 = Color(0xFFF8FAFC)
        val neutral100 = Color(0xFFF1F5F9)
        val neutral200 = Color(0xFFE2E8F0)
        val neutral400 = Color(0xFF94A3B8)
        val neutral600 = Color(0xFF475569)
        val neutral800 = Color(0xFF1E293B)
        val neutral900 = Color(0xFF0F172A)
        val neutral950 = Color(0xFF020617)
        val red500 = Color(0xFFDC2626)
        val red400 = Color(0xFFF87171)
        val green500 = Color(0xFF16A34A)
        val green400 = Color(0xFF4ADE80)
        val white = Color(0xFFFFFFFF)
        val black = Color(0xFF000000)
    }
    object Spacing {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 16.dp
        val lg: Dp = 24.dp
        val xl: Dp = 32.dp
    }
    object Radius {
        val sm: Dp = 6.dp
        val md: Dp = 12.dp
        val lg: Dp = 20.dp
        val full: Dp = 999.dp
    }
    object Elevation {
        val none: Dp = 0.dp
        val low: Dp = 2.dp
        val medium: Dp = 6.dp
        val high: Dp = 12.dp
    }
    object Typography {
        val headingLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight(700))
        val titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight(600))
        val bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight(400))
        val labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight(500))
    }
    object Motion {
        const val fastMillis: Int = 150
        const val normalMillis: Int = 250
        const val slowMillis: Int = 400
    }
}

@Immutable
data class SemanticColorTokens(
    val backgroundCanvas: Color,
    val backgroundSurface: Color,
    val backgroundElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val actionPrimary: Color,
    val actionOnPrimary: Color,
    val statusError: Color,
    val statusSuccess: Color,
    val borderSubtle: Color,
)

val LightSemanticColors = SemanticColorTokens(
    backgroundCanvas = PrimitiveTokens.Colors.neutral50,
    backgroundSurface = PrimitiveTokens.Colors.white,
    backgroundElevated = PrimitiveTokens.Colors.neutral100,
    textPrimary = PrimitiveTokens.Colors.neutral900,
    textSecondary = PrimitiveTokens.Colors.neutral600,
    actionPrimary = PrimitiveTokens.Colors.brand500,
    actionOnPrimary = PrimitiveTokens.Colors.white,
    statusError = PrimitiveTokens.Colors.red500,
    statusSuccess = PrimitiveTokens.Colors.green500,
    borderSubtle = PrimitiveTokens.Colors.neutral200,
)

val DarkSemanticColors = SemanticColorTokens(
    backgroundCanvas = PrimitiveTokens.Colors.neutral950,
    backgroundSurface = PrimitiveTokens.Colors.neutral900,
    backgroundElevated = PrimitiveTokens.Colors.neutral800,
    textPrimary = PrimitiveTokens.Colors.neutral50,
    textSecondary = PrimitiveTokens.Colors.neutral400,
    actionPrimary = PrimitiveTokens.Colors.brand400,
    actionOnPrimary = PrimitiveTokens.Colors.neutral950,
    statusError = PrimitiveTokens.Colors.red400,
    statusSuccess = PrimitiveTokens.Colors.green400,
    borderSubtle = PrimitiveTokens.Colors.neutral800,
)

@Immutable
data class SemanticSpacingTokens(
    val screenPadding: Dp = PrimitiveTokens.Spacing.md,
    val cardPadding: Dp = PrimitiveTokens.Spacing.md,
    val stackGap: Dp = PrimitiveTokens.Spacing.sm,
    val sectionGap: Dp = PrimitiveTokens.Spacing.lg,
)

@Immutable
data class SemanticRadiusTokens(
    val card: Dp = PrimitiveTokens.Radius.md,
    val button: Dp = PrimitiveTokens.Radius.sm,
    val pill: Dp = PrimitiveTokens.Radius.full,
)

@Immutable
data class SemanticElevationTokens(
    val card: Dp = PrimitiveTokens.Elevation.low,
    val modal: Dp = PrimitiveTokens.Elevation.high,
)

@Immutable
data class SemanticTypographyTokens(
    val screenTitle: TextStyle = PrimitiveTokens.Typography.headingLarge,
    val cardTitle: TextStyle = PrimitiveTokens.Typography.titleMedium,
    val body: TextStyle = PrimitiveTokens.Typography.bodyMedium,
    val caption: TextStyle = PrimitiveTokens.Typography.labelSmall,
)

@Immutable
data class SemanticMotionTokens(
    val quickMillis: Int = PrimitiveTokens.Motion.fastMillis,
    val standardMillis: Int = PrimitiveTokens.Motion.normalMillis,
)

@Immutable
data class ScaffoldTokens(
    val colors: SemanticColorTokens,
    val spacing: SemanticSpacingTokens = SemanticSpacingTokens(),
    val radius: SemanticRadiusTokens = SemanticRadiusTokens(),
    val elevation: SemanticElevationTokens = SemanticElevationTokens(),
    val typography: SemanticTypographyTokens = SemanticTypographyTokens(),
    val motion: SemanticMotionTokens = SemanticMotionTokens(),
)

val LocalScaffoldTokens = staticCompositionLocalOf { ScaffoldTokens(colors = LightSemanticColors) }
