package dev.scaffold.kmp.android.feature.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.scaffold.kmp.feature.reference.ReferenceAction
import dev.scaffold.kmp.feature.reference.ReferenceEffect
import dev.scaffold.kmp.feature.reference.ReferencePresenter
import dev.scaffold.kmp.feature.reference.ReferenceUiState
import dev.scaffold.kmp.umbrella.SharedUmbrellaFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ReferenceViewModel(
    private val presenter: ReferencePresenter,
    loadOnInit: Boolean = true,
) : ViewModel() {
    val state: StateFlow<ReferenceUiState> = presenter.state
    val effects: Flow<ReferenceEffect> = presenter.effects

    init {
        if (loadOnInit) {
            presenter.onAction(ReferenceAction.Refresh)
        }
    }

    fun onAction(action: ReferenceAction) {
        presenter.onAction(action)
    }

    override fun onCleared() {
        presenter.close()
        super.onCleared()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = factory()

        fun factory(
            createPresenter: () -> ReferencePresenter = {
                SharedUmbrellaFactory.createSeededReferencePresenter()
            },
            loadOnInit: Boolean = true,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReferenceViewModel(
                    presenter = createPresenter(),
                    loadOnInit = loadOnInit,
                )
            }
        }
    }
}
