# Prompt — Feature Pipeline (Shared KMP → SwiftUI → Compose, same screens)

Use this per feature. iOS leads; Android matches the same screen from the same shared contract.
Keep `token-efficiency-rules.md` loaded as the standing rules.

---

## Context
- App: Kotlin Multiplatform. `shared` module holds ALL domain, data and presentation logic.
- iOS UI: SwiftUI (lead platform). Android UI: Jetpack Compose (parity platform).
- Swift interop via SKIE: Kotlin `StateFlow` and sealed classes must be consumed as native Swift async sequences and exhaustive enums.
- Golden references (copy these patterns exactly):
  - Shared presenter: `shared/src/commonMain/.../feature/reference/`
  - SwiftUI screen: `iosApp/Features/Reference/`
  - Compose screen: `androidApp/src/main/.../feature/reference/`

## "Same screen" means
- Same `UiState` variants rendered, same content, same actions, same edge-case handling.
- NOT pixel-identical. Each platform follows its own conventions (HIG vs Material: navigation, back behaviour, typography, controls).
- Any intentional difference is written in `docs/parity/<feature>.md` under "Platform differences".

## Feature spec (I fill this in)
- Feature: <name>
- Purpose: <one line>
- UiState variants: <e.g., Loading, Content(items), Empty, Error(message, retryable)>
- Actions: <e.g., Refresh, ItemTapped(id), Retry>
- Edge cases: <network failure, empty data, long text, slow response>
- Out of scope: <...>

## Stage 1 — Shared contract (commonMain)
1. Define `UiState` (sealed), `Action` (sealed) and a presenter exposing `state: StateFlow<UiState>` and `onAction(Action)`.
2. Implement domain + repository behind existing ports. No platform types in public APIs.
3. Write `commonTest` tests covering every UiState transition and every edge case from the spec.
4. Gates: `./gradlew :shared:allTests` and the Android target compile pass.
5. STOP. Report the final `UiState` and `Action` definitions. The contract is frozen after I approve; any later change restarts Stage 2 and 3 for both platforms.

## Stage 2 — SwiftUI screen (lead)
1. Render every `UiState` variant with an exhaustive `switch` (no `default:` branch).
2. Views render state and send actions only. No business logic, networking or persistence in Swift.
3. Snapshot test for every UiState variant (light + dark).
4. Gates: KMP framework build, `xcodebuild test` on simulator, snapshot tests pass.
5. Save snapshots to `docs/parity/<feature>/ios/<State>.png`.

## Stage 3 — Compose screen (parity)
1. Render the same `UiState` variants with an exhaustive `when`.
2. Use the iOS snapshots in `docs/parity/<feature>/ios/` as the content and layout reference, adapted to Material conventions.
3. Same rule: render and send actions only.
4. Screenshot test for every UiState variant (light + dark), saved to `docs/parity/<feature>/android/<State>.png`.
5. Gates: Android build, unit tests, screenshot tests pass.

## Stage 4 — Parity check
1. Confirm a one-to-one match between iOS and Android snapshot sets (same state names, none missing).
2. Write `docs/parity/<feature>.md`: state table (state | iOS | Android | notes) and "Platform differences".
3. Update `docs/parity/MATRIX.md` (feature × state × platform status).

## Rules
- Never change the shared contract during Stage 2 or 3. If a UI needs something missing, STOP and report what and why.
- Never edit or weaken tests to pass, unless I say a test is wrong.
- Iterate on logic with JVM `commonTest` only; use Xcode builds for UI verification, not logic debugging.
- After each stage, report: changed files, gates run + result, open issues. Then wait.

## Telemetry (for the harness)
Tag every run with: `feature`, `stage` (shared | ios | android | parity), tokens (input/cached/output), wall time, iterations, gate results, and whether I rewrote output before merge.
