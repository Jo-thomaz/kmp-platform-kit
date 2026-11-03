# Feature Parity — `reference`

Golden reference feature demonstrating the shared KMP contract (`ReferenceUiState`, `ReferenceAction`, `ReferenceEffect`, `ReferencePresenter`) rendered natively in SwiftUI (iOS lead) and Jetpack Compose (Android parity).

## State & Snapshot Matrix

| `ReferenceUiState` Variant | Theme Mode | iOS Snapshot (`docs/parity/reference/ios/`) | Android Snapshot (`docs/parity/reference/android/`) | Notes |
|---|---|---|---|---|
| `Loading` | Light | `Loading_iPhone17Pro_iOS26.4.light.png` | `Loading_light.png` | Renders `ScaffoldSurfaceCard` with indeterminate progress indicator and loading copy. |
| `Loading` | Dark | `Loading_iPhone17Pro_iOS26.4.dark.png` | `Loading_dark.png` | Dark semantic token palette (`DarkSemanticColors` / `.dark`). |
| `Content` | Light | `Content_iPhone17Pro_iOS26.4.light.png` | `Content_light.png` | Renders item list (`title` + `summary`) and `New Item` primary action button. |
| `Content` | Dark | `Content_iPhone17Pro_iOS26.4.dark.png` | `Content_dark.png` | Dark semantic token palette (`DarkSemanticColors` / `.dark`). |
| `Empty` | Light | `Empty_iPhone17Pro_iOS26.4.light.png` | `Empty_light.png` | Renders empty-state card and `Create Sample Item` primary action button. |
| `Empty` | Dark | `Empty_iPhone17Pro_iOS26.4.dark.png` | `Empty_dark.png` | Dark semantic token palette (`DarkSemanticColors` / `.dark`). |
| `Error` | Light | `Error_iPhone17Pro_iOS26.4.light.png` | `Error_light.png` | Renders `statusError` title, `DomainError.message`, and conditional `Retry` button when `retryable == true`. |
| `Error` | Dark | `Error_iPhone17Pro_iOS26.4.dark.png` | `Error_dark.png` | Dark semantic token palette (`DarkSemanticColors` / `.dark`). |

## Navigation & Lifecycle Contract

- **One-shot navigation effect (`ReferenceEffect.NavigateToDetail`):**
  - Emitted via buffered `Channel<ReferenceEffect>` (`ReferencePresenter.effects: Flow<ReferenceEffect>`) when `ReferenceAction.ItemTapped(id)` is dispatched.
  - **Android:** `ReferenceNavHost` collects `viewModel.effects` in `LaunchedEffect` and navigates via `NavHostController.navigate("reference/detail/{itemId}")`.
  - **iOS:** `ReferenceViewModel` (`@StateObject` in `ReferenceNavigationContainer`) collects `presenter.state` and `presenter.effects` concurrently inside `func run() async` (`withTaskGroup`) bound to `.task { await viewModel.run() }`, pausing on disappear and resuming on reappear.
- **Presenter cleanup (`ReferencePresenter.close()`):**
  - Closes the `Channel<ReferenceEffect>` and cancels the presenter `CoroutineScope`.
  - **Android:** Invoked in `ReferenceViewModel.onCleared()`.
  - **iOS:** Invoked in `ReferenceViewModel.deinit` via `nonisolated func close()` on `ReferencePresenting` (compatible with `iosDeploymentTarget: "18.0"`).

## Platform Differences

- **Navigation container:** iOS uses SwiftUI `NavigationStack` with value-based path binding (`[String]`); Android uses Jetpack Navigation Compose `NavHost` with string route arguments.
- **Progress indicator:** iOS uses native `ProgressView()` tinted with `tokens.colors.actionPrimary`; Android uses Material 3 `CircularProgressIndicator` tinted with `tokens.colors.actionPrimary`.
- **Scrolling list:** iOS uses SwiftUI `ScrollView` + `VStack` with `.buttonStyle(.plain)`; Android uses Compose `LazyColumn` with `Modifier.clickable`.
