import XCTest
import SharedUmbrella
@testable import iosApp

@MainActor
private final class FakeReferencePresenter: ReferencePresenting {
    private(set) var currentState: ReferenceUiState = ReferenceUiStateLoading.shared
    private(set) var recordedActions: [ReferenceAction] = []
    nonisolated(unsafe) private(set) var closeCallCount: Int = 0

    private var stateContinuation: AsyncStream<ReferenceUiState>.Continuation?
    private var effectsContinuation: AsyncStream<ReferenceEffect>.Continuation?

    nonisolated var isClosed: Bool {
        closeCallCount > 0
    }

    func makeStateStream() -> AsyncStream<ReferenceUiState> {
        let initial = currentState
        return AsyncStream { [weak self] continuation in
            self?.stateContinuation = continuation
            continuation.yield(initial)
            continuation.onTermination = { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.stateContinuation = nil
                }
            }
        }
    }

    func makeEffectsStream() -> AsyncStream<ReferenceEffect> {
        AsyncStream { [weak self] continuation in
            self?.effectsContinuation = continuation
            continuation.onTermination = { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.effectsContinuation = nil
                }
            }
        }
    }

    func emitState(_ newState: ReferenceUiState) {
        currentState = newState
        stateContinuation?.yield(newState)
    }

    func onAction(_ action: ReferenceAction) {
        recordedActions.append(action)
        switch onEnum(of: action) {
        case .refresh, .retry:
            let seeded = ReferenceIosInterop.shared.createItem(
                id: "019a4448-dc01-7000-8000-000000000001",
                title: "Seeded Item",
                summary: "Initial summary",
                isCompleted: false
            )
            emitState(ReferenceIosInterop.shared.contentState(items: [seeded]))
        case .create(let createAction):
            var items: [ReferenceItem] = []
            if case .content(let existing) = onEnum(of: currentState) {
                items = existing.items
            }
            let created = ReferenceIosInterop.shared.createItem(
                id: "019a4448-dc02-7000-8000-000000000002",
                title: createAction.title,
                summary: "",
                isCompleted: false
            )
            items.append(created)
            emitState(ReferenceIosInterop.shared.contentState(items: items))
        case .itemTapped(let tapped):
            effectsContinuation?.yield(ReferenceEffectNavigateToDetail(id: tapped.id))
        }
    }

    nonisolated func close() {
        closeCallCount += 1
    }
}

@MainActor
final class ReferenceViewModelTests: XCTestCase {
    func testRunCancelsAndResumesStateUpdatesOnReappear() async throws {
        let fakePresenter = FakeReferencePresenter()
        let viewModel = ReferenceViewModel(
            presenter: fakePresenter,
            loadOnInit: true
        )

        let firstRunTask = Task {
            await viewModel.run()
        }

        try await waitUntilStateMatches(stateProvider: { viewModel.state }) { state in
            guard case .content(let content) = onEnum(of: state) else { return false }
            return content.items.count == 1
        }

        firstRunTask.cancel()
        await firstRunTask.value

        let secondRunTask = Task {
            await viewModel.run()
        }
        defer { secondRunTask.cancel() }

        viewModel.onAction(ReferenceActionCreate(title: "Added After Reappear"))

        try await waitUntilStateMatches(stateProvider: { viewModel.state }) { state in
            guard case .content(let content) = onEnum(of: state) else { return false }
            return content.items.contains { $0.title == "Added After Reappear" }
        }
    }

    func testCancellingRunTaskReturnsWithinOneSecondAndDeallocationClosesPresenter() async throws {
        let fakePresenter = FakeReferencePresenter()
        var strongViewModel: ReferenceViewModel? = ReferenceViewModel(
            presenter: fakePresenter,
            loadOnInit: true
        )
        weak var weakViewModel: ReferenceViewModel? = strongViewModel

        var runTask: Task<Void, Never>? = Task { [weak weakViewModel] in
            await weakViewModel?.run()
        }

        try await waitUntilStateMatches(stateProvider: { weakViewModel?.state }) { state in
            guard case .content(let content) = onEnum(of: state) else { return false }
            return content.items.count == 1
        }

        let clock = ContinuousClock()
        let cancellationDuration = await clock.measure {
            runTask?.cancel()
            await runTask?.value
        }
        runTask = nil

        XCTAssertLessThan(
            cancellationDuration,
            .seconds(1),
            "Expected run() to return within 1 second after task cancellation, took \(cancellationDuration)"
        )
        XCTAssertFalse(fakePresenter.isClosed, "Presenter must remain open while ReferenceViewModel is still retained")

        strongViewModel = nil
        await Task.yield()

        XCTAssertNil(weakViewModel, "Expected ReferenceViewModel to deallocate after dropping strong references")
        XCTAssertTrue(fakePresenter.isClosed, "Expected presenter.close() to be called when ReferenceViewModel deallocates")
        XCTAssertEqual(fakePresenter.closeCallCount, 1)
    }

    private func waitUntilStateMatches(
        stateProvider: () -> ReferenceUiState?,
        timeoutNanoseconds: UInt64 = 2_000_000_000,
        predicate: (ReferenceUiState) -> Bool
    ) async throws {
        let deadline = DispatchTime.now().uptimeNanoseconds + timeoutNanoseconds
        while DispatchTime.now().uptimeNanoseconds < deadline {
            if let currentState = stateProvider(), predicate(currentState) {
                return
            }
            try await Task.sleep(nanoseconds: 20_000_000)
        }
        XCTFail("Timed out waiting for ReferenceViewModel state update")
    }
}
