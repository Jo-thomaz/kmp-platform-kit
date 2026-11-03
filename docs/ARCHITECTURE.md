# Architecture & Toolchain Guide

## Toolchain

- **JDK Toolchain (`javaVersion`):** `21` (LTS), configured in `scaffold.yaml` and applied via `jvmToolchain(21)` across Kotlin Multiplatform and Android modules, with automatic provisioning enabled via `org.gradle.toolchains.foojay-resolver-convention` (`1.0.0`).
- **CI Consistency:** All GitHub Actions workflows (`shared-and-android.yml` and `ios.yml`) **must** configure `actions/setup-java` with `java-version: '21'` to match `scaffold.yaml`.
- **Bytecode Target (`jvmTarget` / `compileOptions`):** Kept at **Java 17** (`JvmTarget.JVM_17` and `JavaVersion.VERSION_17`) across `scaffold.kmp.library`, `scaffold.android.compose`, and `scaffold.android.app` for broad JVM/Android bytecode compatibility.

## Layering & Contracts

- **Layers:** `feature` → `domain` → `core`; `data` implements `domain` ports and depends on `domain` + `core`.
- **Typed Error & Outcome Contract:** Shared APIs must return `dev.scaffold.kmp.core.Outcome<T>` (with `DomainError`) and reactive `Flow<T>`. Using `kotlin.Result` or `runCatching` in `shared/` modules is forbidden and enforced by Detekt and Konsist (`:tools:architecture-test`).
- **Design Tokens:** `design-tokens/tokens.json` (W3C DTCG format) is the single source of truth for primitive and semantic tokens (`light` and `dark` modes). Feature UI code may only reference semantic tokens.
- **Platform-Agnostic Domain & iOS Interop Boundary:** `:shared:domain` (as well as `:shared:core` and `:shared:feature:*`) must stay strictly platform-agnostic and use pure Kotlin domain types (including `@JvmInline value class` identifiers such as `UuidV7`). Any Objective-C / Swift interop adapters, unmangled value-class unwrappers, or factory helpers (such as `ReferenceIosInterop` in `shared/umbrella/src/commonMain/kotlin/dev/scaffold/kmp/umbrella/ReferenceIosInterop.kt`) must live exclusively in `:shared:umbrella`. Enforced by Konsist in `tools/architecture-test/src/test/kotlin/dev/scaffold/kmp/architecture/SharedResultArchitectureTest.kt`.
