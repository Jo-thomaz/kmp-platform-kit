package dev.scaffold.kmp.android.feature.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.scaffold.kmp.designsystem.ScaffoldPrimaryButton
import dev.scaffold.kmp.designsystem.ScaffoldSurfaceCard
import dev.scaffold.kmp.designsystem.ScaffoldTheme
import dev.scaffold.kmp.domain.model.ReferenceItem
import dev.scaffold.kmp.feature.reference.ReferenceAction
import dev.scaffold.kmp.feature.reference.ReferenceUiState

@Composable
fun ReferenceScreen(
    state: ReferenceUiState,
    onAction: (ReferenceAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = ScaffoldTheme.tokens
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.backgroundCanvas)
            .padding(tokens.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Reference Items",
                style = tokens.typography.screenTitle,
                color = tokens.colors.textPrimary,
            )
            ScaffoldPrimaryButton(
                text = "Refresh",
                onClick = { onAction(ReferenceAction.Refresh) },
            )
        }

        when (state) {
            ReferenceUiState.Loading -> ReferenceLoadingContent()
            is ReferenceUiState.Content -> ReferenceListContent(
                items = state.items,
                onAction = onAction,
            )
            ReferenceUiState.Empty -> ReferenceEmptyContent(onAction = onAction)
            is ReferenceUiState.Error -> ReferenceErrorContent(
                state = state,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun ReferenceLoadingContent() {
    val tokens = ScaffoldTheme.tokens
    ScaffoldSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reference_loading"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.stackGap),
        ) {
            CircularProgressIndicator(
                color = tokens.colors.actionPrimary,
            )
            Text(
                text = "Loading reference items...",
                style = tokens.typography.body,
                color = tokens.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ReferenceListContent(
    items: List<ReferenceItem>,
    onAction: (ReferenceAction) -> Unit,
) {
    val tokens = ScaffoldTheme.tokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reference_content"),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
    ) {
        ScaffoldPrimaryButton(
            text = "New Item",
            onClick = { onAction(ReferenceAction.Create("New Reference Item")) },
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.stackGap),
        ) {
            items(items = items, key = { it.id.value }) { item ->
                ScaffoldSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(ReferenceAction.ItemTapped(item.id)) },
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.stackGap),
                    ) {
                        Text(
                            text = item.title,
                            style = tokens.typography.cardTitle,
                            color = tokens.colors.textPrimary,
                        )
                        if (item.summary.isNotEmpty()) {
                            Text(
                                text = item.summary,
                                style = tokens.typography.body,
                                color = tokens.colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceEmptyContent(
    onAction: (ReferenceAction) -> Unit,
) {
    val tokens = ScaffoldTheme.tokens
    ScaffoldSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reference_empty"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.stackGap),
        ) {
            Text(
                text = "No reference items yet",
                style = tokens.typography.cardTitle,
                color = tokens.colors.textPrimary,
            )
            Text(
                text = "Create the first item or refresh from the remote source.",
                style = tokens.typography.body,
                color = tokens.colors.textSecondary,
            )
            ScaffoldPrimaryButton(
                text = "Create Sample Item",
                onClick = { onAction(ReferenceAction.Create("Sample Item")) },
            )
        }
    }
}

@Composable
private fun ReferenceErrorContent(
    state: ReferenceUiState.Error,
    onAction: (ReferenceAction) -> Unit,
) {
    val tokens = ScaffoldTheme.tokens
    ScaffoldSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reference_error"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.stackGap),
        ) {
            Text(
                text = "Unable to load items",
                style = tokens.typography.cardTitle,
                color = tokens.colors.statusError,
            )
            Text(
                text = state.error.message,
                style = tokens.typography.body,
                color = tokens.colors.textSecondary,
            )
            if (state.retryable) {
                ScaffoldPrimaryButton(
                    text = "Retry",
                    onClick = { onAction(ReferenceAction.Retry) },
                )
            }
        }
    }
}

@Composable
fun ReferenceDetailScreen(
    itemId: String,
    state: ReferenceUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = ScaffoldTheme.tokens
    val selectedItem = (state as? ReferenceUiState.Content)
        ?.items
        ?.firstOrNull { it.id.value == itemId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.backgroundCanvas)
            .padding(tokens.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
    ) {
        Text(
            text = selectedItem?.title ?: "Item Detail",
            style = tokens.typography.screenTitle,
            color = tokens.colors.textPrimary,
        )
        ScaffoldSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.stackGap),
            ) {
                Text(
                    text = selectedItem?.summary?.ifEmpty { "No summary provided." }
                        ?: "Item ID: $itemId",
                    style = tokens.typography.body,
                    color = tokens.colors.textPrimary,
                )
                Text(
                    text = "ID: $itemId",
                    style = tokens.typography.caption,
                    color = tokens.colors.textSecondary,
                )
            }
        }
        ScaffoldPrimaryButton(
            text = "Back",
            onClick = onBack,
        )
    }
}
