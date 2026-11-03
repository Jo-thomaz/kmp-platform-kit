# Cross-Platform Feature Parity Matrix

Tracks UI state rendering, navigation, and snapshot test parity across **Shared KMP**, **iOS (SwiftUI)**, and **Android (Jetpack Compose)**.

| Feature | State / Flow | Shared (`commonTest`) | Android (Compose + Roborazzi) | iOS (SwiftUI + XCTest Snapshot) | Spec & Notes |
|---|---|---|---|---|---|
| `reference` | `Loading` (Light & Dark) | Verified | Verified (`Loading_light.png`, `Loading_dark.png`) | Implemented (CI-verified on `macos-26`) | [docs/parity/reference.md](reference.md) |
| `reference` | `Content` (Light & Dark) | Verified | Verified (`Content_light.png`, `Content_dark.png`) | Implemented (CI-verified on `macos-26`) | [docs/parity/reference.md](reference.md) |
| `reference` | `Empty` (Light & Dark) | Verified | Verified (`Empty_light.png`, `Empty_dark.png`) | Implemented (CI-verified on `macos-26`) | [docs/parity/reference.md](reference.md) |
| `reference` | `Error` (Light & Dark) | Verified | Verified (`Error_light.png`, `Error_dark.png`) | Implemented (CI-verified on `macos-26`) | [docs/parity/reference.md](reference.md) |
| `reference` | `NavigateToDetail` (`Channel` effect) | Verified | Verified (`ReferenceNavHost`) | Implemented (`NavigationStack`) | [docs/parity/reference.md](reference.md) |
| `reference` | `close()` lifecycle cleanup | Verified | Verified (`ViewModel.onCleared()`) | Implemented (`ReferenceViewModel.deinit`) | [docs/parity/reference.md](reference.md) |
