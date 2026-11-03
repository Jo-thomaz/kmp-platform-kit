import SwiftUI
import SharedUmbrella

public struct ReferenceNavigationContainer: View {
    @StateObject private var viewModel = ReferenceViewModel()

    public init() {}

    public var body: some View {
        NavigationStack(path: $viewModel.navigationPath) {
            ReferenceScreen(
                state: viewModel.state,
                onAction: { action in viewModel.onAction(action) }
            )
            .navigationDestination(for: String.self) { itemId in
                ReferenceDetailScreen(
                    itemId: itemId,
                    state: viewModel.state,
                    onBack: {
                        if !viewModel.navigationPath.isEmpty {
                            viewModel.navigationPath.removeLast()
                        }
                    }
                )
            }
        }
        .task {
            await viewModel.run()
        }
    }
}

public struct ReferenceScreen: View {
    @Environment(\.scaffoldTokens) private var tokens
    private let state: ReferenceUiState
    private let onAction: (ReferenceAction) -> Void

    public init(
        state: ReferenceUiState,
        onAction: @escaping (ReferenceAction) -> Void
    ) {
        self.state = state
        self.onAction = onAction
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: tokens.spacing.sectionGap) {
            HStack {
                Text("Reference Items")
                    .font(tokens.typography.screenTitle)
                    .foregroundStyle(tokens.colors.textPrimary)
                Spacer()
                ScaffoldPrimaryButton(title: "Refresh") {
                    onAction(ReferenceActionRefresh.shared)
                }
            }

            switch onEnum(of: state) {
            case .loading:
                loadingView
            case .content(let content):
                listView(items: content.items)
            case .empty:
                emptyView
            case .error(let errorState):
                errorView(state: errorState)
            }

            Spacer(minLength: 0)
        }
        .padding(tokens.spacing.screenPadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(tokens.colors.backgroundCanvas)
    }

    private var loadingView: some View {
        ScaffoldSurfaceCard {
            ProgressView()
                .tint(tokens.colors.actionPrimary)
            Text("Loading reference items...")
                .font(tokens.typography.body)
                .foregroundStyle(tokens.colors.textSecondary)
        }
    }

    private func listView(items: [ReferenceItem]) -> some View {
        VStack(alignment: .leading, spacing: tokens.spacing.sectionGap) {
            ScaffoldPrimaryButton(title: "New Item") {
                onAction(ReferenceActionCreate(title: "New Reference Item"))
            }

            ScrollView {
                VStack(alignment: .leading, spacing: tokens.spacing.stackGap) {
                    ForEach(items, id: \.self) { item in
                        Button {
                            onAction(ReferenceIosInterop.shared.itemTappedAction(item: item))
                        } label: {
                            ScaffoldSurfaceCard {
                                Text(item.title)
                                    .font(tokens.typography.cardTitle)
                                    .foregroundStyle(tokens.colors.textPrimary)
                                if !item.summary.isEmpty {
                                    Text(item.summary)
                                        .font(tokens.typography.body)
                                        .foregroundStyle(tokens.colors.textSecondary)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var emptyView: some View {
        ScaffoldSurfaceCard {
            Text("No reference items yet")
                .font(tokens.typography.cardTitle)
                .foregroundStyle(tokens.colors.textPrimary)
            Text("Create the first item or refresh from the remote source.")
                .font(tokens.typography.body)
                .foregroundStyle(tokens.colors.textSecondary)
            ScaffoldPrimaryButton(title: "Create Sample Item") {
                onAction(ReferenceActionCreate(title: "Sample Item"))
            }
        }
    }

    private func errorView(state: ReferenceUiStateError) -> some View {
        ScaffoldSurfaceCard {
            Text("Unable to load items")
                .font(tokens.typography.cardTitle)
                .foregroundStyle(tokens.colors.statusError)
            Text(state.error.message)
                .font(tokens.typography.body)
                .foregroundStyle(tokens.colors.textSecondary)
            if state.retryable {
                ScaffoldPrimaryButton(title: "Retry") {
                    onAction(ReferenceActionRetry.shared)
                }
            }
        }
    }
}

public struct ReferenceDetailScreen: View {
    @Environment(\.scaffoldTokens) private var tokens
    private let itemId: String
    private let state: ReferenceUiState
    private let onBack: () -> Void

    public init(
        itemId: String,
        state: ReferenceUiState,
        onBack: @escaping () -> Void
    ) {
        self.itemId = itemId
        self.state = state
        self.onBack = onBack
    }

    private var selectedItem: ReferenceItem? {
        guard case .content(let content) = onEnum(of: state) else {
            return nil
        }
        return content.items.first { ReferenceIosInterop.shared.itemId(item: $0) == itemId }
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: tokens.spacing.sectionGap) {
            Text(selectedItem?.title ?? "Item Detail")
                .font(tokens.typography.screenTitle)
                .foregroundStyle(tokens.colors.textPrimary)

            ScaffoldSurfaceCard {
                Text(selectedItem?.summary.isEmpty == false ? (selectedItem?.summary ?? "") : "No summary provided.")
                    .font(tokens.typography.body)
                    .foregroundStyle(tokens.colors.textPrimary)
                Text("ID: \(itemId)")
                    .font(tokens.typography.caption)
                    .foregroundStyle(tokens.colors.textSecondary)
            }

            ScaffoldPrimaryButton(title: "Back", action: onBack)
            Spacer(minLength: 0)
        }
        .padding(tokens.spacing.screenPadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(tokens.colors.backgroundCanvas)
    }
}
