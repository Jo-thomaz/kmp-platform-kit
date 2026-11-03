package dev.scaffold.kmp.data.repository

import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.data.local.TagLocalDataSource
import dev.scaffold.kmp.data.remote.TagRemoteDataSource
import dev.scaffold.kmp.domain.model.Tag
import dev.scaffold.kmp.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class DefaultTagRepository(
    private val remoteDataSource: TagRemoteDataSource,
    private val localDataSource: TagLocalDataSource,
) : TagRepository {
    override fun observeTags(): Flow<List<Tag>> =
        localDataSource.observeAll()

    override suspend fun refreshTags(): Outcome<Unit> =
        when (val remoteOutcome = remoteDataSource.fetchTags()) {
            is Outcome.Success -> {
                localDataSource.replaceAll(remoteOutcome.value)
                Outcome.success(Unit)
            }
            is Outcome.Failure -> remoteOutcome
        }

    override suspend fun saveTag(
        tag: Tag,
        idempotencyKey: IdempotencyKey,
    ): Outcome<Tag> =
        when (val outcome = remoteDataSource.postTag(tag = tag, idempotencyKey = idempotencyKey)) {
            is Outcome.Success -> {
                localDataSource.upsert(outcome.value)
                outcome
            }
            is Outcome.Failure -> outcome
        }
}
