package dev.scaffold.kmp.umbrella

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.domain.model.ReferenceItem
import dev.scaffold.kmp.feature.reference.ReferenceAction
import dev.scaffold.kmp.feature.reference.ReferenceEffect
import dev.scaffold.kmp.feature.reference.ReferenceUiState

val ReferenceItem.idValue: String
    get() = id.value

val ReferenceEffect.NavigateToDetail.itemId: String
    get() = id.value

object ReferenceIosInterop {
    fun createItem(
        id: String,
        title: String,
        summary: String,
        isCompleted: Boolean = false,
    ): ReferenceItem = ReferenceItem(
        id = UuidV7.parse(id).getOrNull() ?: UuidV7.generate(),
        title = title,
        summary = summary,
        isCompleted = isCompleted,
    )

    fun itemId(item: ReferenceItem): String = item.id.value

    fun effectItemId(effect: ReferenceEffect.NavigateToDetail): String = effect.id.value

    fun contentState(items: List<ReferenceItem>): ReferenceUiState.Content =
        ReferenceUiState.Content(items = items, selectedItemId = null)

    fun itemTappedAction(item: ReferenceItem): ReferenceAction =
        ReferenceAction.ItemTapped(item.id)

    fun networkError(
        message: String,
        statusCode: Int,
    ): DomainError.Network = DomainError.Network(
        message = message,
        statusCode = statusCode,
    )
}
