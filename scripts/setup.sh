#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

echo "==> Checking prerequisites..."

JAVA_BIN=""
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA_BIN="java"
else
  echo "ERROR: Java 17+ is required to launch Gradle, but neither JAVA_HOME/bin/java nor 'java' on PATH was found." >&2
  echo "See CONTRIBUTING.md for setup instructions." >&2
  exit 1
fi

JAVA_VERSION_RAW="$("${JAVA_BIN}" -version 2>&1 | head -n 1)"
JAVA_MAJOR="$(echo "${JAVA_VERSION_RAW}" | sed -E 's/.*"([0-9]+)(\.[0-9]+)*".*/\1/')"
if [[ "${JAVA_MAJOR}" == "1" ]]; then
  JAVA_MAJOR="$(echo "${JAVA_VERSION_RAW}" | sed -E 's/.*"1\.([0-9]+)\..*/\1/')"
fi
if [[ -z "${JAVA_MAJOR}" ]] || ! [[ "${JAVA_MAJOR}" =~ ^[0-9]+$ ]] || (( JAVA_MAJOR < 17 )); then
  echo "ERROR: Java 17+ is required (found: ${JAVA_VERSION_RAW})." >&2
  echo "See CONTRIBUTING.md for setup instructions." >&2
  exit 1
fi
echo "  [OK] Java ${JAVA_MAJOR} (${JAVA_VERSION_RAW})"

ANDROID_SDK_DIR=""
if [[ -n "${ANDROID_HOME:-}" && -d "${ANDROID_HOME}" ]]; then
  ANDROID_SDK_DIR="${ANDROID_HOME}"
elif [[ -f "${REPO_ROOT}/local.properties" ]]; then
  SDK_FROM_PROPS="$(grep -E '^sdk\.dir=' "${REPO_ROOT}/local.properties" | cut -d'=' -f2- | sed 's/\\:/:/g; s/\\\\/\\/g' | tr -d '\r' || true)"
  if [[ -n "${SDK_FROM_PROPS}" && -d "${SDK_FROM_PROPS}" ]]; then
    ANDROID_SDK_DIR="${SDK_FROM_PROPS}"
  fi
fi

if [[ -z "${ANDROID_SDK_DIR}" ]]; then
  echo "ERROR: Android SDK not found. Set ANDROID_HOME in your environment or create local.properties with sdk.dir." >&2
  echo "See CONTRIBUTING.md for setup instructions." >&2
  exit 1
fi
echo "  [OK] Android SDK (${ANDROID_SDK_DIR})"

OS_NAME="$(uname -s)"
if [[ "${OS_NAME}" == "Darwin" ]]; then
  if ! command -v xcodebuild >/dev/null 2>&1; then
    echo "ERROR: 'xcodebuild' not found. Install Xcode 26.4+ and select it with xcode-select." >&2
    exit 1
  fi
  echo "  [OK] $(xcodebuild -version | tr '\n' ' ')"

  if ! command -v xcodegen >/dev/null 2>&1; then
    echo "ERROR: 'xcodegen' not found. Install it with 'brew install xcodegen'." >&2
    exit 1
  fi
  echo "  [OK] xcodegen ($(xcodegen --version))"
fi

echo "==> Marking Gradle wrapper and scripts executable..."
chmod +x "${REPO_ROOT}/gradlew" "${SCRIPT_DIR}"/*.sh

echo "==> Running './gradlew verifyAll'..."
./gradlew verifyAll
echo "  [OK] './gradlew verifyAll' passed (design tokens, Swift & machine path guards, detekt, Konsist, KMP allTests, Android assembleDebug, Android lint, and Roborazzi screenshot verification)."

if [[ "${OS_NAME}" == "Darwin" ]]; then
  echo "==> Generating iOS Xcode project..."
  bash "${SCRIPT_DIR}/ios-project.sh"
fi

echo ""
echo "=========================================="
echo "Setup complete!"
echo "Next steps to run the apps:"
echo "  - Android (with a running emulator/device): ./scripts/run-android.sh"
if [[ "${OS_NAME}" == "Darwin" ]]; then
  echo "  - iOS (macOS Simulator):                    ./scripts/run-ios.sh"
fi
echo "=========================================="
