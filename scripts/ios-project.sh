#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

if ! command -v xcodegen >/dev/null 2>&1; then
  if [[ "${1:-}" == "--install-missing" ]] && command -v brew >/dev/null 2>&1; then
    echo "Installing XcodeGen via Homebrew..."
    brew install xcodegen
  else
    echo "ERROR: 'xcodegen' is not installed. Install it with 'brew install xcodegen'." >&2
    exit 1
  fi
fi

export APP_NAME="$(grep -E '^appName:' scaffold.yaml | sed -E 's/appName:[[:space:]]*"?([^"]+)"?/\1/' | tr -d '[:space:]')"
export BUNDLE_ID="$(grep -E '^bundleId:' scaffold.yaml | sed -E 's/bundleId:[[:space:]]*"?([^"]+)"?/\1/' | tr -d '[:space:]')"
export IOS_DEPLOYMENT_TARGET="$(grep -E '^iosDeploymentTarget:' scaffold.yaml | sed -E 's/iosDeploymentTarget:[[:space:]]*"?([^"]+)"?/\1/' | tr -d '[:space:]')"

if [[ -z "${APP_NAME}" || -z "${BUNDLE_ID}" || -z "${IOS_DEPLOYMENT_TARGET}" ]]; then
  echo "ERROR: Failed to parse appName, bundleId, or iosDeploymentTarget from scaffold.yaml." >&2
  exit 1
fi

echo "Generating Xcode project (APP_NAME=${APP_NAME}, BUNDLE_ID=${BUNDLE_ID}, IOS_DEPLOYMENT_TARGET=${IOS_DEPLOYMENT_TARGET})..."
xcodegen generate --spec iosApp/project.yml --project iosApp
echo "Generated iosApp/iosApp.xcodeproj."
