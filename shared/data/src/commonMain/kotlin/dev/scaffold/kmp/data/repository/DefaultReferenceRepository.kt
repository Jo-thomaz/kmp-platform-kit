package dev.scaffold.kmp.data.repository

import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.data.local.ReferenceLocalDataSource
import dev.scaffold.kmp.data.remote.ReferenceRemoteDataSource
import dev.scaffold.kmp.domain.model.ReferenceItem
import dev.scaffold.kmp.domain.repository.ReferenceRepository
import kotlinx.coroutines.flow.Flow

class DefaultReferenceRepository(
    private val remoteDataSource: ReferenceRemoteDataSource,
    private val localDataSource: ReferenceLocalDataSource,
) : ReferenceRepository {
    override fun observeItems(): Flow<List<ReferenceItem>> =
        localDataSource.observeAll()

    override suspend fun refreshItems(): Outcome<Unit> =
        when (val remoteOutcome = remoteDataSource.fetchItems()) {
            is Outcome.Success -> {
                localDataSource.replaceAll(remoteOutcome.value)
                Outcome.success(Unit)
            }
            is Outcome.Failure -> remoteOutcome
        }

    override suspend fun saveItem(
        item: ReferenceItem,
        idempotencyKey: IdempotencyKey,
    ): Outcome<ReferenceItem> =
        when (val outcome = remoteDataSource.postItem(item = item, idempotencyKey = idempotencyKey)) {
            is Outcome.Success -> {
                localDataSource.upsert(outcome.value)
                outcome
            }
            is Outcome.Failure -> outcome
        }
}
