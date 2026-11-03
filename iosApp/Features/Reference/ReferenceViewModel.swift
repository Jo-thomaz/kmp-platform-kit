import Foundation
import SwiftUI
import SharedUmbrella

@MainActor
public protocol ReferencePresenting: AnyObject {
    var currentState: ReferenceUiState { get }
    func makeStateStream() -> AsyncStream<ReferenceUiState>
    func makeEffectsStream() -> AsyncStream<ReferenceEffect>
    func onAction(_ action: ReferenceAction)
    nonisolated func close()
}

@MainActor
public final class KotlinReferencePresenterAdapter: ReferencePresenting {
    nonisolated(unsafe) private let presenter: ReferencePresenter

    public init(presenter: ReferencePresenter) {
        self.presenter = presenter
    }

    public var currentState: ReferenceUiState {
        presenter.state.value
    }

    public func makeStateStream() -> AsyncStream<ReferenceUiState> {
        let presenterRef = presenter
        return AsyncStream { continuation in
            let task = Task {
                for await newState in presenterRef.state {
                    if Task.isCancelled { break }
                    continuation.yield(newState)
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }

    public func makeEffectsStream() -> AsyncStream<ReferenceEffect> {
        let presenterRef = presenter
        return AsyncStream { continuation in
            let task = Task {
                for await effect in presenterRef.effects {
                    if Task.isCancelled { break }
                    continuation.yield(effect)
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }

    public func onAction(_ action: ReferenceAction) {
        presenter.onAction(action: action)
    }

    public nonisolated func close() {
        presenter.close()
    }
}

@MainActor
public final class ReferenceViewModel: ObservableObject {
    @Published public private(set) var state: ReferenceUiState
    @Published public var navigationPath: [String] = []

    nonisolated(unsafe) private let presenter: any ReferencePresenting
    private let loadOnInit: Bool
    private var hasLoadedInitialData = false

    public init(
        presenter: any ReferencePresenting,
        loadOnInit: Bool = true
    ) {
        self.presenter = presenter
        self.loadOnInit = loadOnInit
        self.state = presenter.currentState
    }

    public convenience init(
        presenter: ReferencePresenter = SharedUmbrellaFactory.shared.createSeededReferencePresenter(),
        loadOnInit: Bool = true
    ) {
        self.init(
            presenter: KotlinReferencePresenterAdapter(presenter: presenter),
            loadOnInit: loadOnInit
        )
    }

    public func run() async {
        let stateStream = presenter.makeStateStream()
        let effectsStream = presenter.makeEffectsStream()

        if loadOnInit && !hasLoadedInitialData {
            hasLoadedInitialData = true
            presenter.onAction(ReferenceActionRefresh.shared)
        }

        await withTaskGroup(of: Void.self) { [weak self] group in
            group.addTask { @MainActor [weak self] in
                for await newState in stateStream {
                    guard !Task.isCancelled, let self else { break }
                    self.state = newState
                }
            }

            group.addTask { @MainActor [weak self] in
                for await effect in effectsStream {
                    guard !Task.isCancelled, let self else { break }
                    self.handleEffect(effect)
                }
            }
        }
    }

    public func onAction(_ action: ReferenceAction) {
        presenter.onAction(action)
    }

    private func handleEffect(_ effect: ReferenceEffect) {
        switch onEnum(of: effect) {
        case .navigateToDetail(let navigateToDetail):
            navigationPath.append(ReferenceIosInterop.shared.effectItemId(effect: navigateToDetail))
        }
    }

    deinit {
        presenter.close()
    }
}
