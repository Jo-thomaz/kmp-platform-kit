package dev.scaffold.kmp.domain.repository

import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>

    suspend fun refreshTags(): Outcome<Unit>

    suspend fun saveTag(
        tag: Tag,
        idempotencyKey: IdempotencyKey,
    ): Outcome<Tag>
}
