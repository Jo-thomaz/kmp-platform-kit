package dev.scaffold.kmp.feature.reference

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.domain.model.ReferenceItem
import dev.scaffold.kmp.domain.repository.ReferenceRepository
import dev.scaffold.kmp.domain.usecase.CreateReferenceItemUseCase
import dev.scaffold.kmp.domain.usecase.ObserveReferenceItemsUseCase
import dev.scaffold.kmp.domain.usecase.RefreshReferenceItemsUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReferencePresenterTest {
    private class FakeReferenceRepository : ReferenceRepository {
        val itemsFlow = MutableStateFlow<List<ReferenceItem>>(emptyList())
        var refreshOutcome: Outcome<Unit> = Outcome.success(Unit)
        var remoteItemsOnRefresh: List<ReferenceItem> = emptyList()
        var saveGate: CompletableDeferred<Outcome<ReferenceItem>>? = null
        var saveOutcomeOverride: Outcome<ReferenceItem>? = null
        val recordedSaveKeys = mutableListOf<IdempotencyKey>()
        val recordedSaveItems = mutableListOf<ReferenceItem>()

        override fun observeItems(): Flow<List<ReferenceItem>> = itemsFlow

        override suspend fun refreshItems(): Outcome<Unit> =
            when (val result = refreshOutcome) {
                is Outcome.Success -> {
                    itemsFlow.value = remoteItemsOnRefresh
                    Outcome.success(Unit)
                }
                is Outcome.Failure -> result
            }

        override suspend fun saveItem(
            item: ReferenceItem,
            idempotencyKey: IdempotencyKey,
        ): Outcome<ReferenceItem> {
            recordedSaveItems.add(item)
            recordedSaveKeys.add(idempotencyKey)
            val outcome = saveGate?.await()
                ?: saveOutcomeOverride
                ?: Outcome.success(item)
            if (outcome is Outcome.Success) {
                itemsFlow.value = itemsFlow.value + outcome.value
            }
            return outcome
        }
    }

    private fun createPresenter(
        repo: FakeReferenceRepository,
        scope: TestScope,
    ): ReferencePresenter {
        val presenterScope: CoroutineScope =
            scope.backgroundScope + UnconfinedTestDispatcher(scope.testScheduler)
        return ReferencePresenter(
            observeItemsUseCase = ObserveReferenceItemsUseCase(repo),
            refreshItemsUseCase = RefreshReferenceItemsUseCase(repo),
            createItemUseCase = CreateReferenceItemUseCase(repo),
            scope = presenterScope,
        )
    }

    @Test
    fun initialStateIsLoadingAndRefreshTransitionsToEmptyWhenNoItems() = runTest {
        val repo = FakeReferenceRepository()
        val presenter = createPresenter(repo, this)

        assertIs<ReferenceUiState.Loading>(presenter.state.value)

        presenter.onAction(ReferenceAction.Refresh)
        advanceUntilIdle()

        assertIs<ReferenceUiState.Empty>(presenter.state.value)
    }

    @Test
    fun errorToRetryToContentAndItemTappedEmitsNavigationEffect() = runTest {
        val sampleItem = ReferenceItem(
            id = UuidV7.generate(timestampMillis = 1_762_000_001_000L),
            title = "Reference Item",
            summary = "From retry",
        )
        val repo = FakeReferenceRepository().apply {
            refreshOutcome = Outcome.failure(DomainError.Network(message = "Offline", statusCode = 503))
            remoteItemsOnRefresh = listOf(sampleItem)
        }
        val presenter = createPresenter(repo, this)

        presenter.onAction(ReferenceAction.Refresh)
        advanceUntilIdle()

        val errorState = assertIs<ReferenceUiState.Error>(presenter.state.value)
        assertTrue(errorState.retryable)
        assertEquals("Offline", errorState.error.message)

        repo.refreshOutcome = Outcome.success(Unit)
        presenter.onAction(ReferenceAction.Retry)
        advanceUntilIdle()

        val contentState = assertIs<ReferenceUiState.Content>(presenter.state.value)
        assertEquals(listOf(sampleItem), contentState.items)
        assertEquals(null, contentState.selectedItemId)

        presenter.onAction(ReferenceAction.ItemTapped(sampleItem.id))
        val tappedState = assertIs<ReferenceUiState.Content>(presenter.state.value)
        assertEquals(sampleItem.id, tappedState.selectedItemId)

        val effect = presenter.effects.first()
        assertEquals(ReferenceEffect.NavigateToDetail(sampleItem.id), effect)
    }

    @Test
    fun validationErrorSetsNonRetryableErrorState() = runTest {
        val repo = FakeReferenceRepository()
        val presenter = createPresenter(repo, this)

        presenter.onAction(ReferenceAction.Create("   "))
        advanceUntilIdle()

        val errorState = assertIs<ReferenceUiState.Error>(presenter.state.value)
        assertFalse(errorState.retryable)
        assertIs<DomainError.Validation>(errorState.error)
    }

    @Test
    fun doubleCreateTappedQuicklyProducesSingleRequestWithSameIdempotencyKey() = runTest {
        val gate = CompletableDeferred<Outcome<ReferenceItem>>()
        val repo = FakeReferenceRepository().apply {
            saveGate = gate
        }
        val presenter = createPresenter(repo, this)

        presenter.onAction(ReferenceAction.Create("Fast Tap"))
        presenter.onAction(ReferenceAction.Create("Fast Tap"))
        advanceUntilIdle()

        assertEquals(1, repo.recordedSaveKeys.size, "In-flight Create must deduplicate rapid double-tap")

        val createdItem = repo.recordedSaveItems.single()
        gate.complete(Outcome.success(createdItem))
        advanceUntilIdle()

        val content = assertIs<ReferenceUiState.Content>(presenter.state.value)
        assertEquals(1, content.items.size)
        assertEquals("Fast Tap", content.items.single().title)
    }

    @Test
    fun failedCreateRetriedReusesSameIdempotencyKey() = runTest {
        val repo = FakeReferenceRepository().apply {
            saveOutcomeOverride = Outcome.failure(DomainError.Network("Timeout"))
        }
        val presenter = createPresenter(repo, this)

        presenter.onAction(ReferenceAction.Create("Idempotent Item"))
        advanceUntilIdle()

        assertIs<ReferenceUiState.Error>(presenter.state.value)
        assertEquals(1, repo.recordedSaveKeys.size)

        repo.saveOutcomeOverride = null
        presenter.onAction(ReferenceAction.Retry)
        advanceUntilIdle()

        assertEquals(2, repo.recordedSaveKeys.size)
        assertEquals(
            repo.recordedSaveKeys[0],
            repo.recordedSaveKeys[1],
            "Retry of failed Create must reuse the exact same IdempotencyKey",
        )
        val content = assertIs<ReferenceUiState.Content>(presenter.state.value)
        assertEquals("Idempotent Item", content.items.single().title)
    }

    @Test
    fun closeCancelsPresenterScopeAndClosesEffectsChannel() = runTest {
        val repo = FakeReferenceRepository()
        val presenterScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val presenter = ReferencePresenter(
            observeItemsUseCase = ObserveReferenceItemsUseCase(repo),
            refreshItemsUseCase = RefreshReferenceItemsUseCase(repo),
            createItemUseCase = CreateReferenceItemUseCase(repo),
            scope = presenterScope,
        )

        assertTrue(presenterScope.isActive)
        presenter.close()
        assertFalse(presenterScope.isActive)
        assertTrue(presenter.effects.toList().isEmpty())
    }
}
