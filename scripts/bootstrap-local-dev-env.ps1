param(
    [ValidateSet("dev", "test", "release", "full")]
    [string]$BootstrapProfile = "dev",
    [switch]$InstallOptional,
    [switch]$InstallAndroidSdkPackages,
    [switch]$SkipProjectBootstrap,
    [switch]$SkipValidation,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

function Write-Title {
    param([string]$Text)
    Write-Host ""
    Write-Host ("=" * 72) -ForegroundColor Cyan
    Write-Host $Text -ForegroundColor Cyan
    Write-Host ("=" * 72) -ForegroundColor Cyan
}

function Write-Step {
    param([string]$Text)
    Write-Host "-> $Text" -ForegroundColor Yellow
}

function Write-Ok {
    param([string]$Text)
    Write-Host "[OK] $Text" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Text)
    Write-Host "[WARN] $Text" -ForegroundColor Yellow
}

function Write-Err {
    param([string]$Text)
    Write-Host "[ERR] $Text" -ForegroundColor Red
}

function Test-Command {
    param([string]$Name)
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-ExecutableInKnownLocations {
    param([string]$Name)

    $candidateFileNames = @($Name)
    if (-not $Name.EndsWith(".exe", [System.StringComparison]::OrdinalIgnoreCase)) {
        $candidateFileNames += "$Name.exe"
    }

    $roots = @(
        (Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links"),
        (Join-Path ${env:ProgramFiles} "LLVM\bin"),
        (Join-Path ${env:ProgramFiles(x86)} "LLVM\bin"),
        (Join-Path ${env:ProgramFiles} "Ninja"),
        (Join-Path ${env:ProgramFiles(x86)} "Ninja"),
        (Join-Path ${env:ProgramFiles} "OpenSSL-Win64\bin"),
        (Join-Path ${env:ProgramFiles(x86)} "OpenSSL-Win32\bin")
    )

    foreach ($root in $roots | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) {
        foreach ($fileName in $candidateFileNames) {
            $candidate = Join-Path $root $fileName
            if (Test-Path $candidate) {
                return $true
            }
        }
    }

    return $false
}

function Test-ToolAvailable {
    param([string]$Name)
    return (Test-Command $Name) -or (Test-ExecutableInKnownLocations $Name)
}

function Get-IsAdmin {
    return ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).
        IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Set-EnvVarIfNeeded {
    param(
        [string]$Name,
        [string]$Value,
        [string]$Scope = "User"
    )
    $current = [Environment]::GetEnvironmentVariable($Name, $Scope)
    if ([string]::IsNullOrWhiteSpace($current) -or $current -ne $Value) {
        [Environment]::SetEnvironmentVariable($Name, $Value, $Scope)
        Write-Ok "Set $Name ($Scope) = $Value"
    } else {
        Write-Ok "$Name ($Scope) already configured"
    }
}

function Install-Package {
    param(
        [string]$DisplayName,
        [string]$CheckCommand,
        [string]$WingetId,
        [string]$ChocoId
    )

    if (Test-ToolAvailable $CheckCommand) {
        Write-Ok "$DisplayName already installed"
        return $true
    }

    Write-Step "Installing $DisplayName"

    if (Test-Command "winget") {
        & winget install --id $WingetId --exact --silent --accept-package-agreements --accept-source-agreements
        if ($LASTEXITCODE -eq 0 -and (Test-ToolAvailable $CheckCommand)) {
            Write-Ok "$DisplayName installed via winget"
            return $true
        }
    }

    if (Test-Command "choco") {
        & choco install $ChocoId -y --no-progress
        if ($LASTEXITCODE -eq 0 -and (Test-ToolAvailable $CheckCommand)) {
            Write-Ok "$DisplayName installed via Chocolatey"
            return $true
        }
    }

    Write-Err "Failed to install $DisplayName automatically"
    return $false
}

function Get-NodeMajor {
    if (-not (Test-Command "node")) { return 0 }
    try {
        $v = (& node --version 2>$null | Select-Object -First 1).ToString().Trim()
    } catch {
        return 0
    }
    if ($v -match "^v(\d+)\.") { return [int]$Matches[1] }
    return 0
}

function Install-ChocolateyIfMissing {
    if (Test-Command "choco") {
        Write-Ok "Chocolatey already available"
        return
    }

    if (-not (Get-IsAdmin)) {
        Write-Warn "Chocolatey missing. Install manually or run script as Administrator."
        return
    }

    Write-Step "Installing Chocolatey"
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
    Invoke-Expression ((New-Object System.Net.WebClient).DownloadString("https://community.chocolatey.org/install.ps1"))
}

function Set-AndroidEnvIfDetected {
    $androidStudioDefault = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $androidStudioDefault) {
        Set-EnvVarIfNeeded -Name "ANDROID_HOME" -Value $androidStudioDefault -Scope "User"
        Set-EnvVarIfNeeded -Name "ANDROID_SDK_ROOT" -Value $androidStudioDefault -Scope "User"
    } else {
        Write-Warn "Android SDK path not found at $androidStudioDefault"
        Write-Warn "Install Android Studio once and open SDK Manager."
    }
}

function Install-AndroidSdkPackages {
    $sdkRoot = [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", "User")
    if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
        $sdkRoot = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "User")
    }

    if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
        Write-Warn "ANDROID_SDK_ROOT/ANDROID_HOME not configured. Skipping sdkmanager packages."
        return
    }

    $sdkManager = Join-Path $sdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
    if (-not (Test-Path $sdkManager)) {
        Write-Warn "sdkmanager not found: $sdkManager"
        Write-Warn "Install Android Command-line Tools from Android Studio SDK Manager."
        return
    }

    Write-Step "Installing Android SDK packages (platform-tools, Android 34, build-tools 34.0.0)"
    & $sdkManager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
    if ($LASTEXITCODE -eq 0) {
        Write-Ok "Android SDK packages installed"
    } else {
        Write-Warn "Android SDK package install returned non-zero exit code"
    }
}

if ($ShowHelp) {
    Write-Host "Bootstrap Windows dev stack for IP-CSS (Chocolatey/winget installs, PATH, optional Android SDK packages, project npm, validation gates by profile)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\bootstrap-local-dev-env.ps1 -ShowHelp"
    Write-Host "  .\scripts\bootstrap-local-dev-env.ps1 [-BootstrapProfile dev|test|release|full] [-InstallOptional] [-InstallAndroidSdkPackages] [-SkipProjectBootstrap] [-SkipValidation]"
    Write-Host ""
    Write-Host "Profiles:"
    Write-Host "  dev     Core tools + light validation"
    Write-Host "  test    Adds FFmpeg/Android Studio path checks, KMP metadata compile"
    Write-Host "  release Adds release assemble/bundle smoke (long)"
    Write-Host "  full    Optional OpenSSL + broadest validation"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Completed (warnings may be printed)"
    Write-Host "  Non-zero  Unhandled failure (rare; most checks are warn-only)"
    exit 0
}

Write-Title "IP-CSS Local Environment Bootstrap (Windows)"
Write-Ok "Selected profile: $BootstrapProfile"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot
Write-Ok "Project root: $projectRoot"

$isAdmin = Get-IsAdmin
if ($isAdmin) {
    Write-Ok "Script runs with Administrator rights"
} else {
    Write-Warn "Script runs without Administrator rights (some installs may fail)"
}

Write-Title "1) Package Manager Preparation"
Install-ChocolateyIfMissing

Write-Title "2) Required Tools Installation"
$required = @(
    @{ Name = "Git"; Check = "git"; Winget = "Git.Git"; Choco = "git" },
    @{ Name = "JDK 17"; Check = "java"; Winget = "Microsoft.OpenJDK.17"; Choco = "openjdk17" },
    @{ Name = "Python 3"; Check = "python"; Winget = "Python.Python.3.11"; Choco = "python" },
    @{ Name = "Node.js LTS"; Check = "node"; Winget = "OpenJS.NodeJS.LTS"; Choco = "nodejs-lts" },
    @{ Name = "CMake"; Check = "cmake"; Winget = "Kitware.CMake"; Choco = "cmake" },
    @{ Name = "Docker Desktop"; Check = "docker"; Winget = "Docker.DockerDesktop"; Choco = "docker-desktop" }
)

foreach ($pkg in $required) {
    Install-Package -DisplayName $pkg.Name -CheckCommand $pkg.Check -WingetId $pkg.Winget -ChocoId $pkg.Choco | Out-Null
}

 $nodeMajor = Get-NodeMajor
if ($nodeMajor -gt 0 -and $nodeMajor -lt 20) {
    Write-Warn "Node.js major version < 20. Upgrade recommended for web/build pipeline."
}

Write-Title "3) Optional Tools Installation"
if ($InstallOptional -or $BootstrapProfile -in @("test", "release", "full")) {
    $optional = @()
    if ($BootstrapProfile -in @("test", "release", "full")) {
        $optional += @{ Name = "FFmpeg"; Check = "ffmpeg"; Winget = "Gyan.FFmpeg"; Choco = "ffmpeg" }
        $optional += @{ Name = "Android Studio"; Check = "adb"; Winget = "Google.AndroidStudio"; Choco = "androidstudio" }
    }
    if ($BootstrapProfile -in @("release", "full") -or $InstallOptional) {
        $optional += @{ Name = "LLVM/clangd"; Check = "clangd"; Winget = "LLVM.LLVM"; Choco = "llvm" }
        $optional += @{ Name = "Ninja"; Check = "ninja"; Winget = "Ninja-build.Ninja"; Choco = "ninja" }
    }
    if ($BootstrapProfile -eq "full") {
        $optional += @{ Name = "OpenSSL"; Check = "openssl"; Winget = "ShiningLight.OpenSSL"; Choco = "openssl" }
    }

    $optional = $optional | Sort-Object Name -Unique
    foreach ($pkg in $optional) {
        Install-Package -DisplayName $pkg.Name -CheckCommand $pkg.Check -WingetId $pkg.Winget -ChocoId $pkg.Choco | Out-Null
    }
} else {
    Write-Warn "Optional tools skipped. Re-run with -InstallOptional if needed."
}

Write-Title "4) Environment Configuration"
Set-AndroidEnvIfDetected

$javaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    Write-Warn "JAVA_HOME is not set explicitly. Usually not required if java is in PATH."
} else {
    Write-Ok "JAVA_HOME is set: $javaHome"
}

Write-Title "5) Project Bootstrap"
if (-not $SkipProjectBootstrap) {
    if (Test-Path ".\server\web\package.json") {
        Write-Step "Installing web dependencies (npm ci)"
        Push-Location ".\server\web"
        try {
            npm ci
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "server/web dependencies installed"
            } else {
                Write-Warn "npm ci returned non-zero exit code"
            }
        } finally {
            Pop-Location
        }
    } else {
        Write-Warn "server/web/package.json not found, skipping npm ci"
    }

    if (Test-Path ".\gradlew.bat") {
        Write-Step "Gradle wrapper warm-up"
        .\gradlew.bat --version
        if ($LASTEXITCODE -eq 0) {
            Write-Ok "Gradle wrapper is ready"
        } else {
            Write-Warn "Gradle wrapper warm-up returned non-zero exit code"
        }
    } else {
        Write-Warn "gradlew.bat not found in repo root"
    }
} else {
    Write-Warn "Project bootstrap skipped by flag"
}

if ($InstallAndroidSdkPackages) {
    Write-Title "6) Android SDK Packages"
    Install-AndroidSdkPackages
} elseif ($BootstrapProfile -in @("test", "release", "full")) {
    Write-Title "6) Android SDK Packages"
    Write-Warn "Profile '$BootstrapProfile' usually needs Android SDK packages."
    Write-Warn "Run with -InstallAndroidSdkPackages to install via sdkmanager."
}

Write-Title "7) Validation"
if (-not $SkipValidation) {
    $checks = @(
        @{ Name = "git"; Cmd = "git --version" },
        @{ Name = "java"; Cmd = "java -version" },
        @{ Name = "python"; Cmd = "python --version" },
        @{ Name = "node"; Cmd = "node --version" },
        @{ Name = "npm"; Cmd = "npm --version" },
        @{ Name = "cmake"; Cmd = "cmake --version" },
        @{ Name = "docker"; Cmd = "docker --version" },
        @{ Name = "adb"; Cmd = "adb version" }
    )

    foreach ($c in $checks) {
        Write-Step "Check $($c.Name)"
        try {
            Invoke-Expression $c.Cmd | Select-Object -First 1 | ForEach-Object { Write-Host "   $_" }
        } catch {
            Write-Warn "$($c.Name) check failed"
        }
    }

    if (Test-Path ".\scripts\ci\verify-kmp-phase1.ps1") {
        Write-Step "Running KMP preflight checks (skip gradle)"
        .\scripts\ci\verify-kmp-phase1.ps1 -SkipGradle
        if ($LASTEXITCODE -eq 0) {
            Write-Ok "KMP preflight checks passed"
        } else {
            Write-Warn "KMP preflight checks failed"
        }
    }

    if ($BootstrapProfile -in @("test", "release", "full")) {
        if (Test-Path ".\gradlew.bat") {
            Write-Step "Running KMP metadata compile gate"
            .\gradlew.bat :core:common:compileKotlinMetadata :core:network:compileKotlinMetadata :shared:compileKotlinMetadata --no-daemon
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "KMP metadata compile gate passed"
            } else {
                Write-Warn "KMP metadata compile gate failed"
            }
        }
    }

    if ($BootstrapProfile -in @("release", "full")) {
        if (Test-Path ".\gradlew.bat") {
            Write-Step "Running release build smoke checks"
            .\gradlew.bat :android:app:assembleRelease :android:app:bundleRelease :server:api:build --no-daemon
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "Release smoke checks passed"
            } else {
                Write-Warn "Release smoke checks failed (inspect task output)"
            }
        }
    }
} else {
    Write-Warn "Validation skipped by flag"
}

Write-Title "Completed"
Write-Host "Recommended next commands:" -ForegroundColor Cyan
if ($BootstrapProfile -eq "dev") {
    Write-Host "  .\scripts\ci\verify-kmp-phase1.ps1 -SkipGradle" -ForegroundColor White
    Write-Host "  .\gradlew.bat --version" -ForegroundColor White
} elseif ($BootstrapProfile -eq "test") {
    Write-Host "  .\scripts\ci\mvp-automated-acceptance.ps1" -ForegroundColor White
    Write-Host "    -> video gate: local profile or MVP_VIDEO_ACCEPTANCE_PROFILE; CI uses mvp-ci (see MVP_PHASE1_AUTOMATED_ACCEPTANCE.md)" -ForegroundColor DarkGray
    Write-Host "  .\scripts\ci\verify-kmp-phase1.ps1" -ForegroundColor White
} else {
    Write-Host "  .\gradlew.bat :android:app:assembleRelease :android:app:bundleRelease --no-daemon" -ForegroundColor White
    Write-Host "  .\gradlew.bat :server:api:build --no-daemon" -ForegroundColor White
}
Write-Host ""
Write-Host "If new tools were installed, restart terminal/IDE to refresh PATH." -ForegroundColor Yellow
