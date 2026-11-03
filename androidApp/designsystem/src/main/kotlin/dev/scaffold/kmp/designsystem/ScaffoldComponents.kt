package dev.scaffold.kmp.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ScaffoldSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = ScaffoldTheme.tokens
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radius.card),
        color = tokens.colors.backgroundSurface,
        shadowElevation = tokens.elevation.card,
        border = BorderStroke(
            width = tokens.elevation.card / 2,
            color = tokens.colors.borderSubtle,
        ),
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.cardPadding),
            content = content,
        )
    }
}

@Composable
fun ScaffoldPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = ScaffoldTheme.tokens
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radius.button),
        contentPadding = PaddingValues(
            horizontal = tokens.spacing.cardPadding,
            vertical = tokens.spacing.stackGap,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = tokens.colors.actionPrimary,
            contentColor = tokens.colors.actionOnPrimary,
        ),
    ) {
        Text(
            text = text,
            style = tokens.typography.body,
        )
    }
}
