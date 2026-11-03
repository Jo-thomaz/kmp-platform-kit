# Contributing & Local Environment Setup

## 1. JDK Setup (Java 21 LTS)

- A Java runtime is required to launch the Gradle Wrapper (`./gradlew` on macOS/Linux, `./gradlew.bat` on Windows).
- This repository uses the Gradle Foojay Toolchain Resolver (`org.gradle.toolchains.foojay-resolver-convention`), which automatically downloads and provisions **JDK 21** (matching `javaVersion: 21` in `scaffold.yaml`) if it is not already installed on your machine.

---

## 2. Android SDK Setup (Windows, macOS & Linux)

Gradle requires the Android SDK location via **either** `local.properties` (git-ignored) at the repository root **or** the `ANDROID_HOME` user environment variable.

### Option A: Automatic via Android Studio (All Platforms)
Opening the repository in **Android Studio** automatically generates `local.properties` in the project root with your local `sdk.dir` path.

### Option B: Set `ANDROID_HOME` Once in Your User Environment
If you build from a terminal or headless environment without opening Android Studio, configure `ANDROID_HOME` once at the OS user level using the default Android SDK installation path for your platform:

- **Windows:**
  - Default SDK location: `%LOCALAPPDATA%\Android\Sdk`
  - Set permanently for your user account in PowerShell:
    ```powershell
    [Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
    ```
  - Or create `local.properties` at the repository root (`sdk.dir` with escaped backslashes).

- **macOS:**
  - Default SDK location: `$HOME/Library/Android/sdk`
  - Add to `~/.zshrc` (or `~/.bash_profile`):
    ```sh
    export ANDROID_HOME="$HOME/Library/Android/sdk"
    ```

- **Linux:**
  - Default SDK location: `$HOME/Android/Sdk`
  - Add to `~/.bashrc` (or `~/.profile`):
    ```sh
    export ANDROID_HOME="$HOME/Android/Sdk"
    ```

---

## 3. iOS Toolchain (macOS Only)

- Building and running the iOS app (`iosApp/`) and iOS simulator tests requires macOS with **Xcode 26.4+** and **XcodeGen** (`brew install xcodegen`).
- On Windows and Linux, shared Kotlin Multiplatform tests, Android builds, Roborazzi screenshot tests, `checkDesignTokens`, `checkSwiftSources`, `:tools:architecture-test:test`, and `detekt` run locally; iOS framework linking and `xcodebuild test` run on the `macos-26` GitHub Actions runner.
