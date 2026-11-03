#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

ADB_BIN=""
if command -v adb >/dev/null 2>&1; then
  ADB_BIN="adb"
elif [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/platform-tools/adb" ]]; then
  ADB_BIN="${ANDROID_HOME}/platform-tools/adb"
elif [[ -f "${REPO_ROOT}/local.properties" ]]; then
  SDK_FROM_PROPS="$(grep -E '^sdk\.dir=' "${REPO_ROOT}/local.properties" | cut -d'=' -f2- | sed 's/\\:/:/g; s/\\\\/\\/g' | tr -d '\r' || true)"
  if [[ -n "${SDK_FROM_PROPS}" && -x "${SDK_FROM_PROPS}/platform-tools/adb" ]]; then
    ADB_BIN="${SDK_FROM_PROPS}/platform-tools/adb"
  fi
fi

if [[ -z "${ADB_BIN}" ]]; then
  echo "ERROR: 'adb' not found on PATH or under ANDROID_HOME/platform-tools. See CONTRIBUTING.md." >&2
  exit 1
fi

CONNECTED_DEVICES="$("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" {print $1}')"
if [[ -z "${CONNECTED_DEVICES}" ]]; then
  echo "ERROR: No connected Android emulator or device detected ('adb devices' returned no online devices)." >&2
  echo "Start an Android emulator or connect a device with USB debugging enabled, then re-run ./scripts/run-android.sh." >&2
  exit 1
fi

PACKAGE_ID="$(grep -E '^packageId:' scaffold.yaml | sed -E 's/packageId:[[:space:]]*"?([^"]+)"?/\1/' | tr -d '[:space:]')"

echo "==> Connected Android device(s): $(echo "${CONNECTED_DEVICES}" | tr '\n' ' ')"
echo "==> Installing debug APK for applicationId '${PACKAGE_ID}' via './gradlew :androidApp:app:installDebug'..."
./gradlew :androidApp:app:installDebug

RESOLVED_COMPONENT="$("${ADB_BIN}" shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "${PACKAGE_ID}" | tr -d '\r' | tail -n 1)"
if [[ -z "${RESOLVED_COMPONENT}" || "${RESOLVED_COMPONENT}" == "No activity found" || "${RESOLVED_COMPONENT}" != *"/"* ]]; then
  echo "ERROR: Could not resolve launcher activity for applicationId '${PACKAGE_ID}' (got: '${RESOLVED_COMPONENT}')." >&2
  exit 1
fi

echo "==> Launching resolved activity ${RESOLVED_COMPONENT}..."
"${ADB_BIN}" shell am start -n "${RESOLVED_COMPONENT}"
