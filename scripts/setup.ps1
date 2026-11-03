$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path
Set-Location $RepoRoot

Write-Host "==> Checking prerequisites..."

$javaExe = $null
if (-not $env:JAVA_HOME) {
    $userJavaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
    $machineJavaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
    if ($userJavaHome -and (Test-Path $userJavaHome)) {
        $env:JAVA_HOME = $userJavaHome
    } elseif ($machineJavaHome -and (Test-Path $machineJavaHome)) {
        $env:JAVA_HOME = $machineJavaHome
    }
}

if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaExe = $javaCmd.Source
    }
}

if (-not $javaExe) {
    Write-Error "Java 17+ is required to launch Gradle, but neither JAVA_HOME\bin\java.exe nor 'java' on PATH was found. See CONTRIBUTING.md."
    exit 1
}

$javaVersionOutput = (cmd /c "`"$javaExe`" -version 2>&1" | Select-Object -First 1 | Out-String)
if ($javaVersionOutput -match '"(\d+)(?:\.(\d+))?') {
    $major = [int]$Matches[1]
    if ($major -eq 1 -and $Matches[2]) {
        $major = [int]$Matches[2]
    }
} else {
    $major = 0
}

if ($major -lt 17) {
    Write-Error "Java 17+ is required (found: $($javaVersionOutput.Trim())). See CONTRIBUTING.md."
    exit 1
}
Write-Host "  [OK] Java $major ($($javaVersionOutput.Trim()))"

$androidSdkDir = $null
if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
    $androidSdkDir = $env:ANDROID_HOME
} else {
    $userAndroidHome = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "User")
    $machineAndroidHome = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "Machine")
    if ($userAndroidHome -and (Test-Path $userAndroidHome)) {
        $env:ANDROID_HOME = $userAndroidHome
        $androidSdkDir = $userAndroidHome
    } elseif ($machineAndroidHome -and (Test-Path $machineAndroidHome)) {
        $env:ANDROID_HOME = $machineAndroidHome
        $androidSdkDir = $machineAndroidHome
    } elseif (Test-Path (Join-Path $RepoRoot "local.properties")) {
        $sdkLine = Get-Content (Join-Path $RepoRoot "local.properties") | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) {
            $rawDir = ($sdkLine -replace '^sdk\.dir=', '').Replace('\:', ':').Replace('\\', '\').Trim()
            if (Test-Path $rawDir) {
                $androidSdkDir = $rawDir
            }
        }
    }
}

if (-not $androidSdkDir) {
    Write-Error "Android SDK not found. Set ANDROID_HOME in your User environment or create local.properties with sdk.dir. See CONTRIBUTING.md."
    exit 1
}
$localPropsPath = Join-Path $RepoRoot "local.properties"
if (-not (Test-Path $localPropsPath)) {
    $escapedSdkDir = $androidSdkDir.Replace('\', '\\').Replace(':', '\:')
    Set-Content -Path $localPropsPath -Value "sdk.dir=$escapedSdkDir" -Encoding ASCII
}
Write-Host "  [OK] Android SDK ($androidSdkDir)"

Write-Host "==> Running './gradlew.bat verifyAll'..."
& .\gradlew.bat verifyAll
if ($LASTEXITCODE -ne 0) {
    Write-Error "'./gradlew.bat verifyAll' failed with exit code $LASTEXITCODE."
    exit $LASTEXITCODE
}
Write-Host "  [OK] './gradlew.bat verifyAll' passed (design tokens, Swift & machine path guards, detekt, Konsist, KMP allTests, Android assembleDebug, Android lint, and Roborazzi screenshot verification)."

Write-Host ""
Write-Host "=========================================="
Write-Host "Setup complete!"
Write-Host "Next steps to run the apps:"
Write-Host "  - Android (Windows PowerShell): .\scripts\run-android.ps1"
Write-Host "  - Android (macOS/Linux):        ./scripts/run-android.sh"
Write-Host "  - iOS (macOS only):             ./scripts/run-ios.sh"
Write-Host "=========================================="
