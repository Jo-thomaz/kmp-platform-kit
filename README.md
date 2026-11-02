# kmp-platform-kit

## Repository layout

- `build-logic/`: Gradle convention plugins and shared build logic.
- `gradle/`: Gradle wrapper and version catalog (`libs.versions.toml`).
- `design-tokens/`: Shared design tokens and theme definitions across platforms.
- `shared/`: Kotlin Multiplatform shared module (domain, data, and shared logic).
- `androidApp/`: Android application entry point and native UI host.
- `iosApp/`: iOS Xcode project and Swift entry point.
- `backend/`: Backend service / API implementation.
- `docs/`: Project documentation and agent rules.
- `docs/playbooks/`: Agent prompt playbooks for scaffolding and feature pipelines.
- `docs/parity/`: Cross-platform feature and UI parity tracking docs.
- `.github/workflows/`: GitHub Actions CI/CD workflow definitions.
