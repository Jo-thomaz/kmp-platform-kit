package dev.scaffold.kmp.feature.reference

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.domain.model.ReferenceItem
import dev.scaffold.kmp.domain.usecase.CreateReferenceItemUseCase
import dev.scaffold.kmp.domain.usecase.ObserveReferenceItemsUseCase
import dev.scaffold.kmp.domain.usecase.RefreshReferenceItemsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ReferencePresenter(
    private val observeItemsUseCase: ObserveReferenceItemsUseCase,
    private val refreshItemsUseCase: RefreshReferenceItemsUseCase,
    private val createItemUseCase: CreateReferenceItemUseCase,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<ReferenceUiState>(ReferenceUiState.Loading)
    val state: StateFlow<ReferenceUiState> = _state.asStateFlow()

    private val _effects = Channel<ReferenceEffect>(Channel.BUFFERED)
    val effects: Flow<ReferenceEffect> = _effects.receiveAsFlow()

    private var latestItems: List<ReferenceItem> = emptyList()
    private var selectedItemId: UuidV7? = null
    private var hasCompletedInitialLoad: Boolean = false
    private var isCreating: Boolean = false
    private var pendingCreate: PendingCreate? = null

    private data class PendingCreate(
        val title: String,
        val id: UuidV7,
        val idempotencyKey: IdempotencyKey,
    )

    init {
        scope.launch {
            observeItemsUseCase().collect { items ->
                latestItems = items
                if (hasCompletedInitialLoad && _state.value !is ReferenceUiState.Error) {
                    _state.value = mapItemsToState(items, selectedItemId)
                }
            }
        }
    }

    fun onAction(action: ReferenceAction) {
        when (action) {
            ReferenceAction.Refresh -> performRefresh()
            ReferenceAction.Retry -> performRetry()
            is ReferenceAction.ItemTapped -> handleItemTapped(action.id)
            is ReferenceAction.Create -> performCreate(action.title)
        }
    }

    fun close() {
        _effects.close()
        scope.cancel()
    }

    private fun performRefresh() {
        pendingCreate = null
        _state.value = ReferenceUiState.Loading
        scope.launch {
            when (val outcome = refreshItemsUseCase()) {
                is Outcome.Success -> {
                    hasCompletedInitialLoad = true
                    _state.value = mapItemsToState(latestItems, selectedItemId)
                }
                is Outcome.Failure -> {
                    _state.value = ReferenceUiState.Error(
                        error = outcome.error,
                        retryable = isRetryable(outcome.error),
                    )
                }
            }
        }
    }

    private fun performRetry() {
        val pending = pendingCreate
        if (pending != null) {
            executeCreate(pending)
        } else {
            performRefresh()
        }
    }

    private fun handleItemTapped(id: UuidV7) {
        selectedItemId = id
        val current = _state.value
        if (current is ReferenceUiState.Content) {
            _state.value = current.copy(selectedItemId = id)
        }
        _effects.trySend(ReferenceEffect.NavigateToDetail(id))
    }

    private fun performCreate(title: String) {
        if (isCreating) return
        val existingPending = pendingCreate
        val request = if (existingPending != null && existingPending.title == title) {
            existingPending
        } else {
            PendingCreate(
                title = title,
                id = UuidV7.generate(),
                idempotencyKey = IdempotencyKey.generate(),
            ).also { pendingCreate = it }
        }
        executeCreate(request)
    }

    private fun executeCreate(request: PendingCreate) {
        if (isCreating) return
        isCreating = true
        scope.launch {
            try {
                when (
                    val outcome = createItemUseCase(
                        title = request.title,
                        summary = "",
                        id = request.id,
                        idempotencyKey = request.idempotencyKey,
                    )
                ) {
                    is Outcome.Success -> {
                        pendingCreate = null
                        hasCompletedInitialLoad = true
                        if (latestItems.none { it.id == outcome.value.id }) {
                            latestItems = latestItems + outcome.value
                        }
                        _state.value = mapItemsToState(latestItems, selectedItemId)
                    }
                    is Outcome.Failure -> {
                        _state.value = ReferenceUiState.Error(
                            error = outcome.error,
                            retryable = isRetryable(outcome.error),
                        )
                    }
                }
            } finally {
                isCreating = false
            }
        }
    }

    private fun mapItemsToState(
        items: List<ReferenceItem>,
        selectedId: UuidV7?,
    ): ReferenceUiState =
        if (items.isEmpty()) {
            ReferenceUiState.Empty
        } else {
            ReferenceUiState.Content(items = items, selectedItemId = selectedId)
        }

    private fun isRetryable(error: DomainError): Boolean =
        when (error) {
            is DomainError.Network -> true
            is DomainError.Conflict -> false
            is DomainError.Validation -> false
            is DomainError.NotFound -> false
            is DomainError.Unauthorized -> false
            is DomainError.Unknown -> true
        }
}
