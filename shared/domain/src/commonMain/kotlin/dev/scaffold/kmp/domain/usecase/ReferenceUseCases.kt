package dev.scaffold.kmp.domain.usecase

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.domain.model.ReferenceItem
import dev.scaffold.kmp.domain.repository.ReferenceRepository
import kotlinx.coroutines.flow.Flow

class ObserveReferenceItemsUseCase(
    private val repository: ReferenceRepository,
) {
    operator fun invoke(): Flow<List<ReferenceItem>> = repository.observeItems()
}

class RefreshReferenceItemsUseCase(
    private val repository: ReferenceRepository,
) {
    suspend operator fun invoke(): Outcome<Unit> = repository.refreshItems()
}

class CreateReferenceItemUseCase(
    private val repository: ReferenceRepository,
) {
    suspend operator fun invoke(
        title: String,
        summary: String,
        id: UuidV7 = UuidV7.generate(),
        idempotencyKey: IdempotencyKey = IdempotencyKey.generate(),
    ): Outcome<ReferenceItem> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            return Outcome.failure(
                DomainError.Validation(
                    field = "title",
                    message = "Title must not be blank",
                ),
            )
        }
        val item = ReferenceItem(
            id = id,
            title = cleanTitle,
            summary = summary.trim(),
            isCompleted = false,
        )
        return repository.saveItem(item = item, idempotencyKey = idempotencyKey)
    }
}
