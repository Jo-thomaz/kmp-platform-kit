package dev.scaffold.kmp.domain

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.domain.model.ReferenceItem
import dev.scaffold.kmp.domain.repository.ReferenceRepository
import dev.scaffold.kmp.domain.usecase.CreateReferenceItemUseCase
import dev.scaffold.kmp.domain.usecase.ObserveReferenceItemsUseCase
import dev.scaffold.kmp.domain.usecase.RefreshReferenceItemsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReferenceUseCasesTest {
    private class FakeReferenceRepository : ReferenceRepository {
        private val itemsFlow = MutableStateFlow<List<ReferenceItem>>(emptyList())
        var lastIdempotencyKey: IdempotencyKey? = null
        var refreshCalls: Int = 0

        override fun observeItems(): Flow<List<ReferenceItem>> = itemsFlow

        override suspend fun refreshItems(): Outcome<Unit> {
            refreshCalls += 1
            return Outcome.success(Unit)
        }

        override suspend fun saveItem(
            item: ReferenceItem,
            idempotencyKey: IdempotencyKey,
        ): Outcome<ReferenceItem> {
            lastIdempotencyKey = idempotencyKey
            itemsFlow.value = itemsFlow.value + item
            return Outcome.success(item)
        }
    }

    @Test
    fun createReferenceItemRejectsBlankTitle() = runTest {
        val repo = FakeReferenceRepository()
        val createUseCase = CreateReferenceItemUseCase(repo)
        val observeUseCase = ObserveReferenceItemsUseCase(repo)

        val outcome = createUseCase(title = "   ", description = "Ignored")

        assertTrue(outcome.isFailure)
        val error = assertIs<DomainError.Validation>(outcome.errorOrNull())
        assertEquals("title", error.field)
        assertTrue(observeUseCase().first().isEmpty())
    }

    @Test
    fun createObserveAndRefreshReferenceItemsUseOutcomeAndFlow() = runTest {
        val repo = FakeReferenceRepository()
        val createUseCase = CreateReferenceItemUseCase(repo)
        val observeUseCase = ObserveReferenceItemsUseCase(repo)
        val refreshUseCase = RefreshReferenceItemsUseCase(repo)

        val created = createUseCase(title = "  Golden Item  ", description = "  Sample description ")
        assertTrue(created.isSuccess)
        val item = created.getOrNull()!!
        assertEquals("Golden Item", item.title)
        assertEquals("Sample description", item.description)
        assertTrue(repo.lastIdempotencyKey != null)

        assertEquals(listOf(item), observeUseCase().first())
        assertTrue(refreshUseCase().isSuccess)
        assertEquals(1, repo.refreshCalls)
    }
}
