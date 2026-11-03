package dev.scaffold.kmp.feature.reference

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.domain.model.ReferenceItem

sealed interface ReferenceUiState {
    data object Loading : ReferenceUiState

    data class Content(
        val items: List<ReferenceItem>,
        val selectedItemId: UuidV7? = null,
    ) : ReferenceUiState

    data object Empty : ReferenceUiState

    data class Error(
        val error: DomainError,
        val retryable: Boolean,
    ) : ReferenceUiState
}

sealed interface ReferenceAction {
    data object Refresh : ReferenceAction
    data object Retry : ReferenceAction
    data class ItemTapped(val id: UuidV7) : ReferenceAction
    data class Create(val title: String) : ReferenceAction
}

sealed interface ReferenceEffect {
    data class NavigateToDetail(val id: UuidV7) : ReferenceEffect
}
