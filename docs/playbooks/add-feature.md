# Playbook: Add an End-to-End Feature (`:shared:feature:{feature}`, Android Compose, iOS SwiftUI)

## Purpose
Add a complete vertical feature slice (`{feature}` / `{Feature}`) across shared domain use cases, shared MVI presenter (`:shared:feature:{feature}`), iOS umbrella export (`:shared:umbrella`), Android Compose UI + Navigation + Roborazzi screenshot tests (`:androidApp:feature:{feature}`), iOS SwiftUI UI + `NavigationStack` + XCTest unit & snapshot tests (`iosApp/Features/{Feature}/`), CI workflows, and parity documentation.

**Prerequisites:** Requires a configured Android SDK and JDK; see `CONTRIBUTING.md`.

---

## Inputs I Provide (Spec Template)

```yaml
featureName: reference         # lowercase module slug ({feature}); PascalCase = {Feature}
modelName: ReferenceItem       # Primary domain model rendered by the feature
states:
  - Loading
  - Content(items: List<ReferenceItem>, selectedItemId: UuidV7?)
  - Empty
  - Error(error: DomainError, retryable: Boolean)
actions:
  - Refresh
  - Retry
  - ItemTapped(id: UuidV7)
  - Create(title: String)
effects:
  - NavigateToDetail(id: UuidV7)
```

---

## Placeholders & Exact File Paths

Read `packageId` from `scaffold.yaml` (e.g., `dev.scaffold.kmp`) and derive `{packagePath}` (`dev/scaffold/kmp`).

| Layer | Exact File Path |
|---|---|
| Gradle Settings | `settings.gradle.kts` (add `:shared:feature:{feature}` and `:androidApp:feature:{feature}`) |
| Domain Use Cases | `shared/domain/src/commonMain/kotlin/{packagePath}/domain/usecase/{Feature}UseCases.kt` |
| Domain Use Case Tests | `shared/domain/src/commonTest/kotlin/{packagePath}/domain/{Feature}UseCasesTest.kt` |
| Shared Feature Gradle Build | `shared/feature/{feature}/build.gradle.kts` |
| Shared Feature Contract | `shared/feature/{feature}/src/commonMain/kotlin/{packagePath}/feature/{feature}/{Feature}Contract.kt` |
| Shared Feature Presenter | `shared/feature/{feature}/src/commonMain/kotlin/{packagePath}/feature/{feature}/{Feature}Presenter.kt` |
| Shared Presenter Tests | `shared/feature/{feature}/src/commonTest/kotlin/{packagePath}/feature/{feature}/{Feature}PresenterTest.kt` |
| iOS Umbrella Export | `shared/umbrella/build.gradle.kts` (add `api(projects.shared.feature.{feature})`) |
| iOS Umbrella Factory & Interop | `shared/umbrella/src/commonMain/kotlin/{packagePath}/umbrella/{Feature}IosInterop.kt` |
| Android Feature Gradle Build | `androidApp/feature/{feature}/build.gradle.kts` |
| Android ViewModel + Factory | `androidApp/feature/{feature}/src/main/kotlin/{packagePath}/android/feature/{feature}/{Feature}ViewModel.kt` |
| Android Compose Screens | `androidApp/feature/{feature}/src/main/kotlin/{packagePath}/android/feature/{feature}/{Feature}Screen.kt` |
| Android Navigation Host | `androidApp/feature/{feature}/src/main/kotlin/{packagePath}/android/feature/{feature}/{Feature}Navigation.kt` |
| Android Roborazzi Tests | `androidApp/feature/{feature}/src/test/kotlin/{packagePath}/android/feature/{feature}/{Feature}ScreenScreenshotTest.kt` |
| Android Golden PNGs | `docs/parity/{feature}/android/{State}_{light\|dark}.png` |
| iOS `@StateObject` ViewModel | `iosApp/Features/{Feature}/{Feature}ViewModel.swift` |
| iOS SwiftUI Screens | `iosApp/Features/{Feature}/{Feature}Screen.swift` |
| iOS ViewModel Lifecycle Tests | `iosApp/Tests/{Feature}ViewModelTests.swift` |
| iOS Snapshot Tests | `iosApp/Tests/{Feature}ScreenSnapshotTests.swift` |
| iOS Golden PNGs | `docs/parity/{feature}/ios/{State}_iPhone17Pro_iOS26.4.{light\|dark}.png` |
| Parity Docs | `docs/parity/{feature}.md` and `docs/parity/MATRIX.md` |

---

## Reference Files to Copy Patterns From

- **Shared Contract & Presenter:** `shared/feature/reference/src/commonMain/kotlin/{packagePath}/feature/reference/ReferenceContract.kt`, `shared/feature/reference/src/commonMain/kotlin/{packagePath}/feature/reference/ReferencePresenter.kt`, `shared/feature/reference/src/commonTest/kotlin/{packagePath}/feature/reference/ReferencePresenterTest.kt`
- **Umbrella Export & Interop:** `shared/umbrella/build.gradle.kts`, `shared/umbrella/src/commonMain/kotlin/{packagePath}/umbrella/ReferenceIosInterop.kt`, `shared/umbrella/src/commonMain/kotlin/{packagePath}/umbrella/SharedUmbrellaFactory.kt`
- **Android UI, ViewModel, Navigation & Roborazzi:** `androidApp/feature/reference/src/main/kotlin/{packagePath}/android/feature/reference/ReferenceViewModel.kt`, `androidApp/feature/reference/src/main/kotlin/{packagePath}/android/feature/reference/ReferenceScreen.kt`, `androidApp/feature/reference/src/main/kotlin/{packagePath}/android/feature/reference/ReferenceNavigation.kt`, `androidApp/feature/reference/src/test/kotlin/{packagePath}/android/feature/reference/ReferenceScreenScreenshotTest.kt`
- **iOS UI, ViewModel & Tests:** `iosApp/Features/Reference/ReferenceViewModel.swift`, `iosApp/Features/Reference/ReferenceScreen.swift`, `iosApp/Tests/ReferenceViewModelTests.swift`, `iosApp/Tests/ReferenceScreenSnapshotTests.swift`
- **Parity Documentation:** `docs/parity/reference.md`, `docs/parity/MATRIX.md`

---

## Rules That Apply

1. **`Outcome` not `kotlin.Result`:** All use cases and repositories must return `Outcome<T>`. Never use `kotlin.Result` or `runCatching`.
2. **`NSObject` Collision Ban:** No public property or public function in `shared/` may be named `description`, `hash`, `isEqual`, or `debugDescription`.
3. **DTO `@SerialName`:** Any wire DTO fields must use explicit `@SerialName` when JSON field names differ from Kotlin names.
4. **No Business Logic in UI:** Compose and SwiftUI screens are stateless projections of `{Feature}UiState` that forward user intents via `onAction: ({Feature}Action) -> Unit`.
5. **Semantic Design Tokens Only:** Screens may only read colors, spacing, typography, radius, elevation, and motion from `ScaffoldTheme.tokens` (Compose) or `@Environment(\.scaffoldTokens)` (SwiftUI) and use shared design system components (`ScaffoldSurfaceCard`, `ScaffoldPrimaryButton`). Never hardcode `Color(0x...)` or `.dp`/`.sp` literals in feature screens, and never leak Compose identifiers (`Modifier`, `dp`, `sp`, `Composable`, `RoundedCornerShape`, `remember`, etc.) into `.swift` files (enforced by `checkSwiftSources`).
6. **Presenter Lifecycle on Both Platforms:**
   - **Shared Presenter:** Exposes `state: StateFlow<{Feature}UiState>`, `effects: Flow<{Feature}Effect>` (backed by `Channel<{Feature}Effect>(Channel.BUFFERED)`), `onAction({Feature}Action)`, and `close()` (which closes the effect channel and cancels the presenter `CoroutineScope`).
   - **Android:** `{Feature}Presenter` must be constructed inside `{Feature}ViewModel.Factory` (`viewModelFactory { initializer { ... } }`), never in `MainActivity`, so configuration changes reuse it; `{Feature}ViewModel.onCleared()` must call `presenter.close()`.
   - **iOS:** `{Feature}ViewModel` must expose `func run() async` collecting `state` and `effects` concurrently via `withTaskGroup`, bound to `.task { await viewModel.run() }` on the container view (never `onDisappear` teardown), and call `presenter.close()` in `deinit` via `nonisolated func close()` on `{Feature}Presenting` (compatible with `iosDeploymentTarget: "18.0"`).
7. **Platform-Agnostic Shared Modules:** Keep Objective-C/Swift value-class unwrappers and factory helpers inside `:shared:umbrella`, never in `:shared:domain` or `:shared:feature:{feature}`.
8. **File Size Limit:** Every source file must be **<= 350 lines** (enforced by `:tools:architecture-test:test`).

---

## Gate Commands to Run

```powershell
./gradlew.bat :shared:feature:{feature}:allTests :androidApp:feature:{feature}:recordRoborazziDebug
./gradlew.bat checkDesignTokens :tools:architecture-test:test detekt check :androidApp:feature:{feature}:assembleDebug :androidApp:feature:{feature}:verifyRoborazziDebug
```

---

## Definition of Done

- [ ] `:shared:feature:{feature}` unit tests cover every `UiState` transition (`Loading`, `Content`, `Empty`, `Error` -> `Retry` -> `Content`), rapid duplicate action deduplication with the same `IdempotencyKey`, `effects` emission, and `close()`.
- [ ] `:shared:umbrella` exports `:shared:feature:{feature}` in both `commonMain` (`api`) and `framework { export(...) }`.
- [ ] `:androidApp:feature:{feature}` compiles, constructs its presenter inside `ViewModelProvider.Factory`, calls `presenter.close()` in `onCleared()`, and records/verifies Light + Dark Roborazzi PNGs in `docs/parity/{feature}/android/`.
- [ ] `iosApp/Features/{Feature}/` passes `checkSwiftSources`, uses `onEnum(of: state)` for exhaustive state switching, binds `.task { await viewModel.run() }`, and has XCTest unit tests (including `run()` cancellation within 1s, deallocation, and fake presenter `close()`) plus pinned-simulator (`iPhone17Pro_iOS26.4`) snapshot tests.
- [ ] `docs/parity/{feature}.md` and `docs/parity/MATRIX.md` are updated.
- [ ] All gate commands pass with 0 errors.
