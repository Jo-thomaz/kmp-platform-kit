package dev.scaffold.kmp.domain.repository

import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.domain.model.ReferenceItem
import kotlinx.coroutines.flow.Flow

interface ReferenceRepository {
    fun observeItems(): Flow<List<ReferenceItem>>

    suspend fun refreshItems(): Outcome<Unit>

    suspend fun saveItem(
        item: ReferenceItem,
        idempotencyKey: IdempotencyKey,
    ): Outcome<ReferenceItem>
}
