#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "ERROR: scripts/run-ios.sh can only be run on macOS." >&2
  exit 1
fi

PINNED_NAME="iPhone 17 Pro"
PINNED_OS="26.4"

BUNDLE_ID="$(grep -E '^bundleId:' scaffold.yaml | sed -E 's/bundleId:[[:space:]]*"?([^"]+)"?/\1/' | tr -d '[:space:]')"
APP_BUNDLE_ID="${BUNDLE_ID}.ios"

echo "==> Linking SharedUmbrella framework for iOS Simulator..."
./gradlew :shared:umbrella:linkDebugFrameworkIosSimulatorArm64

echo "==> Generating Xcode project..."
bash "${SCRIPT_DIR}/ios-project.sh"

SIM_UDID="$(xcrun simctl list devices available -j | python3 -c "
import json, sys
target_name = '${PINNED_NAME}'
target_os = '${PINNED_OS}'
runtime_suffix = 'iOS-' + target_os.replace('.', '-')
data = json.load(sys.stdin).get('devices', {})
matches = [
    d['udid']
    for runtime, devices in data.items()
    if runtime.endswith(runtime_suffix) or f'iOS {target_os}' in runtime
    for d in devices
    if d.get('isAvailable') and d.get('name') == target_name
]
print(matches[0] if matches else '')
")"

if [[ -z "${SIM_UDID}" ]]; then
  echo "WARNING: Pinned simulator '${PINNED_NAME}' (iOS ${PINNED_OS}) not found; falling back to first available iPhone simulator." >&2
  SIM_UDID="$(xcrun simctl list devices available -j | python3 -c "
import json, sys
data = json.load(sys.stdin).get('devices', {})
sims = [
    d['udid']
    for r, ds in data.items()
    if 'iOS' in r
    for d in ds
    if d.get('isAvailable') and 'iPhone' in d.get('name', '')
]
print(sims[0] if sims else '')
")"
fi

if [[ -z "${SIM_UDID}" ]]; then
  echo "ERROR: No available iPhone simulator found. Install an iOS Simulator runtime in Xcode." >&2
  exit 1
fi

echo "==> Booting simulator ${SIM_UDID}..."
xcrun simctl boot "${SIM_UDID}" >/dev/null 2>&1 || true
open -a Simulator || true

DERIVED_DATA="${REPO_ROOT}/iosApp/build/DerivedData"
echo "==> Building iosApp for simulator ${SIM_UDID}..."
xcodebuild build \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination "platform=iOS Simulator,id=${SIM_UDID}" \
  -derivedDataPath "${DERIVED_DATA}" \
  CODE_SIGNING_ALLOWED=NO

APP_PATH="${DERIVED_DATA}/Build/Products/Debug-iphonesimulator/iosApp.app"
if [[ ! -d "${APP_PATH}" ]]; then
  echo "ERROR: Built app bundle not found at ${APP_PATH}" >&2
  exit 1
fi

echo "==> Installing and launching ${APP_BUNDLE_ID} on simulator ${SIM_UDID}..."
xcrun simctl install "${SIM_UDID}" "${APP_PATH}"
xcrun simctl launch "${SIM_UDID}" "${APP_BUNDLE_ID}"
