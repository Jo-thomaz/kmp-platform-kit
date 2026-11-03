# Playbook: Add a Design Token (W3C DTCG -> Compose & SwiftUI)

## Purpose
Add or update a primitive and/or semantic design token in the single source of truth (`design-tokens/tokens.json`), regenerate the strongly-typed Jetpack Compose (`GeneratedTokens.kt`) and SwiftUI (`GeneratedTokens.swift`) token files, and verify zero token drift.

**Prerequisites:** Requires a configured Android SDK and JDK; see `CONTRIBUTING.md`.

---

## Inputs I Provide (Spec Template)

```yaml
primitiveTokens:
  color:
    brandTeal500: "#0D9488"
    brandTeal300: "#5EEAD4"
  spacing:
    space10: "40dp"
semanticTokens:
  color:
    badgeAccent:
      light: "{primitive.color.brandTeal500}"
      dark: "{primitive.color.brandTeal300}"
  spacing:
    badgeGap: "{primitive.spacing.space10}"
```

---

## Placeholders & Exact File Paths

Read `packageId` from `scaffold.yaml` (e.g., `dev.scaffold.kmp`) and derive `{packagePath}` (`dev/scaffold/kmp`).

| Artifact | Exact File Path |
|---|---|
| Source of Truth (W3C DTCG JSON) | `design-tokens/tokens.json` |
| Token Parser & Model (Gradle Plugin) | `build-logic/src/main/kotlin/dev/scaffold/buildlogic/DesignTokensModel.kt` |
| Compose & SwiftUI Renderers | `build-logic/src/main/kotlin/dev/scaffold/buildlogic/DesignTokensRenderer.kt` |
| Gradle Tasks (`generateDesignTokens` / `checkDesignTokens`) | `build-logic/src/main/kotlin/dev/scaffold/buildlogic/DesignTokensPlugin.kt` |
| Generated Android Compose Tokens | `androidApp/designsystem/src/main/kotlin/{packagePath}/designsystem/tokens/GeneratedTokens.kt` |
| Generated iOS SwiftUI Tokens | `iosApp/DesignSystem/Generated/GeneratedTokens.swift` |
| Android Theme Wrapper | `androidApp/designsystem/src/main/kotlin/{packagePath}/designsystem/ScaffoldTheme.kt` |
| iOS Theme Wrapper | `iosApp/DesignSystem/ScaffoldTheme.swift` |

---

## Reference Files to Copy Patterns From

- **DTCG Token Definitions:** `design-tokens/tokens.json`
- **Generator Pipeline:** `build-logic/src/main/kotlin/dev/scaffold/buildlogic/DesignTokensModel.kt`, `build-logic/src/main/kotlin/dev/scaffold/buildlogic/DesignTokensRenderer.kt`, `build-logic/src/main/kotlin/dev/scaffold/buildlogic/DesignTokensPlugin.kt`
- **Generated Outputs:** `androidApp/designsystem/src/main/kotlin/{packagePath}/designsystem/tokens/GeneratedTokens.kt`, `iosApp/DesignSystem/Generated/GeneratedTokens.swift`

---

## Rules That Apply

1. **Never Edit Generated Files by Hand:** `GeneratedTokens.kt` and `GeneratedTokens.swift` are overwritten by `./gradlew.bat generateDesignTokens` and verified byte-for-byte by `./gradlew.bat checkDesignTokens`.
2. **Two-Layer Token Contract (Primitive -> Semantic):**
   - `primitive` tokens define raw values (`#RRGGBB` hex colors, `Ndp` dimensions, `Nms` durations, or `{ fontSize, lineHeight, fontWeight }` typography objects).
   - `semantic` tokens reference primitive tokens via `{primitive.<category>.<name>}` syntax.
   - Semantic colors **must** specify both `"light"` and `"dark"` primitive references.
3. **Feature UI Uses Semantic Tokens Only:** Feature screens in `:androidApp:feature:{feature}` and `iosApp/Features/{Feature}/` must only access `ScaffoldTheme.tokens.<category>.<semanticName>` (Compose) or `tokens.<category>.<semanticName>` (SwiftUI). `PrimitiveTokens` is `internal` and never referenced directly by features.
4. **Other Standing Kit Rules:** Keep `Outcome` (never `kotlin.Result`), avoid `NSObject` colliding names (`description`, `hash`, `isEqual`, `debugDescription`), use `@SerialName` on DTOs, keep business logic out of UI, maintain presenter `close()` lifecycle on Android and iOS, and keep every source file **<= 350 lines**.

---

## Step-by-Step Procedure

1. **Edit `design-tokens/tokens.json`:**
   - Add any new primitive token under `"primitive" -> "<category>"` with `"$type"` and `"$value"`.
   - Add the semantic token under `"semantic" -> "<category>"` referencing `{primitive.<category>.<name>}` (with both `"light"` and `"dark"` keys for `"color"` tokens).
2. **Run the Generator Task:**
   - Execute `./gradlew.bat generateDesignTokens` to update `GeneratedTokens.kt` and `GeneratedTokens.swift`.
3. **Verify Drift Guard & Compilation:**
   - Execute `./gradlew.bat checkDesignTokens :androidApp:designsystem:assembleDebug checkSwiftSources`.
4. **Update Screenshots if Existing Visual Tokens Changed:**
   - If an existing semantic token value changed, re-record Android screenshots (`./gradlew.bat :androidApp:feature:reference:recordRoborazziDebug`) and iOS snapshots on CI.

---

## Gate Commands to Run

```powershell
./gradlew.bat generateDesignTokens checkDesignTokens :tools:architecture-test:test detekt check :androidApp:designsystem:assembleDebug :androidApp:feature:reference:verifyRoborazziDebug
```

---

## Definition of Done

- [ ] `design-tokens/tokens.json` contains the new primitive and semantic token definitions (including both `light` and `dark` for colors).
- [ ] `GeneratedTokens.kt` and `GeneratedTokens.swift` are regenerated via `./gradlew.bat generateDesignTokens`.
- [ ] `./gradlew.bat checkDesignTokens`, `:androidApp:designsystem:assembleDebug`, `checkSwiftSources`, `:tools:architecture-test:test`, and `detekt` pass.
