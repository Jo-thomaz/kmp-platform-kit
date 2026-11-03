$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path
Set-Location $RepoRoot

if (-not $env:ANDROID_HOME) {
    $userAndroidHome = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "User")
    $machineAndroidHome = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "Machine")
    if ($userAndroidHome -and (Test-Path $userAndroidHome)) {
        $env:ANDROID_HOME = $userAndroidHome
    } elseif ($machineAndroidHome -and (Test-Path $machineAndroidHome)) {
        $env:ANDROID_HOME = $machineAndroidHome
    }
}

$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
$adbPath = $null
if ($adbCmd) {
    $adbPath = $adbCmd.Source
} elseif ($env:ANDROID_HOME -and (Test-Path (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"))) {
    $adbPath = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
} elseif (Test-Path (Join-Path $RepoRoot "local.properties")) {
    $sdkLine = Get-Content (Join-Path $RepoRoot "local.properties") | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
    if ($sdkLine) {
        $rawDir = ($sdkLine -replace '^sdk\.dir=', '').Replace('\:', ':').Replace('\\', '\').Trim()
        $candidate = Join-Path $rawDir "platform-tools\adb.exe"
        if (Test-Path $candidate) {
            $adbPath = $candidate
        }
    }
}

if (-not $adbPath) {
    Write-Error "'adb' not found on PATH or under ANDROID_HOME\platform-tools. See CONTRIBUTING.md."
    exit 1
}

$adbOutput = & $adbPath devices
$onlineDevices = @($adbOutput | Select-Object -Skip 1 | Where-Object { $_ -match '^\S+\s+device\b' } | ForEach-Object { ($_ -split '\s+')[0] })

if ($onlineDevices.Count -eq 0) {
    Write-Error "No connected Android emulator or device detected ('adb devices' returned no online devices). Start an Android emulator or connect a device, then re-run .\scripts\run-android.ps1."
    exit 1
}

$packageLine = Get-Content (Join-Path $RepoRoot "scaffold.yaml") | Where-Object { $_ -match '^packageId:' } | Select-Object -First 1
$packageId = ($packageLine -replace '^packageId:\s*', '').Trim('"').Trim()

Write-Host "==> Connected Android device(s): $($onlineDevices -join ', ')"
Write-Host "==> Installing debug APK for applicationId '$packageId' via './gradlew.bat :androidApp:app:installDebug'..."
& .\gradlew.bat :androidApp:app:installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "'./gradlew.bat :androidApp:app:installDebug' failed with exit code $LASTEXITCODE."
    exit $LASTEXITCODE
}

$resolvedLines = & $adbPath shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $packageId
$resolvedComponent = ($resolvedLines | Where-Object { $_ -and $_.Trim() -ne "" } | Select-Object -Last 1).Trim()
if (-not $resolvedComponent -or $resolvedComponent -eq "No activity found" -or $resolvedComponent -notmatch "/") {
    Write-Error "Could not resolve launcher activity for applicationId '$packageId' (got: '$resolvedComponent')."
    exit 1
}

Write-Host "==> Launching resolved activity $resolvedComponent..."
& $adbPath shell am start -n $resolvedComponent
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to launch $resolvedComponent via adb."
    exit $LASTEXITCODE
}
