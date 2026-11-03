package dev.scaffold.kmp.umbrella

import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.data.local.InMemoryReferenceLocalDataSource
import dev.scaffold.kmp.data.remote.ReferenceRemoteDataSource
import dev.scaffold.kmp.data.repository.DefaultReferenceRepository
import dev.scaffold.kmp.domain.model.ReferenceItem
import dev.scaffold.kmp.domain.usecase.CreateReferenceItemUseCase
import dev.scaffold.kmp.domain.usecase.ObserveReferenceItemsUseCase
import dev.scaffold.kmp.domain.usecase.RefreshReferenceItemsUseCase
import dev.scaffold.kmp.feature.reference.ReferencePresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object SharedUmbrellaFactory {
    fun createSeededReferencePresenter(): ReferencePresenter =
        createSeededReferencePresenter(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        )

    fun createSeededReferencePresenter(
        scope: CoroutineScope,
    ): ReferencePresenter {
        val seedItems = listOf(
            ReferenceItem(
                id = UuidV7.generate(timestampMillis = 1_762_000_000_001L),
                title = "Offline-first architecture",
                summary = "Shared presenter state rendered natively in Compose and SwiftUI",
            ),
        )
        val local = InMemoryReferenceLocalDataSource(initialItems = seedItems)
        val remote = object : ReferenceRemoteDataSource {
            override suspend fun fetchItems(): Outcome<List<ReferenceItem>> =
                Outcome.success(local.getAll())

            override suspend fun postItem(
                item: ReferenceItem,
                idempotencyKey: IdempotencyKey,
            ): Outcome<ReferenceItem> = Outcome.success(item)
        }
        val repository = DefaultReferenceRepository(
            remoteDataSource = remote,
            localDataSource = local,
        )
        return ReferencePresenter(
            observeItemsUseCase = ObserveReferenceItemsUseCase(repository),
            refreshItemsUseCase = RefreshReferenceItemsUseCase(repository),
            createItemUseCase = CreateReferenceItemUseCase(repository),
            scope = scope,
        )
    }
}
