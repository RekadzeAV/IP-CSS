# IP-CSS Build Pipeline for Windows
# Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

# Force UTF8 encoding
$encoding = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $encoding
[Console]::OutputEncoding = $encoding

$ErrorActionPreference = "Continue"
$BUILD_START = Get-Date
$ATTEMPT = 1
$MAX_ATTEMPTS = 3

# Configuration
$SAFE_JOBS_CPP = 3
$SAFE_JOBS_GRADLE = 2
$SAFE_JOBS_NPM = 5
$BUILD_NATIVE = $true
$BUILD_KOTLIN = $true
$BUILD_WEB = $true
$BUILD_DOCKER = $false  # Отключено для быстрой сборки

# Logging
$LOG_DIR = "build_logs"
if (-not (Test-Path $LOG_DIR)) { New-Item -ItemType Directory -Path $LOG_DIR | Out-Null }
$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"
$MAIN_LOG = "$LOG_DIR\build_${TIMESTAMP}.log"

function Write-Log {
    param([string]$Message, [string]$Color = "White")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logEntry = "[$timestamp] $Message"
    Write-Host $logEntry -ForegroundColor $Color
    Add-Content -Path $MAIN_LOG -Value $logEntry
}

function Write-Separator {
    param([string]$Title = "")
    Write-Log "============================================================" "Cyan"
    if ($Title) { Write-Log "  $Title" "Yellow" }
    Write-Log "============================================================" "Cyan"
}

# Check .env
if (-not (Test-Path ".env") -and (Test-Path ".env.example")) {
    Write-Log "Creating .env from .env.example..." "Yellow"
    Copy-Item ".env.example" ".env"
    Write-Log ".env created. Please review and set required values." "Yellow"
}

# ============================================================================
# STEP 1: Native C++ Build
# ============================================================================
if ($BUILD_NATIVE) {
    Write-Separator "Step 1: Building Native C++ Libraries"
    
    Set-Location native
    
    # Check vcpkg
    if (-not (Test-Path "vcpkg")) {
        Write-Log "Cloning vcpkg..." "Yellow"
        git clone https://github.com/Microsoft/vcpkg.git
        .\vcpkg\bootstrap-vcpkg.bat
    }
    
    # Create build directory
    if (-not (Test-Path "build")) {
        New-Item -ItemType Directory -Path "build" | Out-Null
    }
    Set-Location build
    
    # CMake configuration
    Write-Log "Configuring CMake..." "Green"
    
    # Проверяем наличие Visual Studio
    $vsFound = $false
    $vsPaths = @("Community", "Professional", "Enterprise")
    foreach ($edition in $vsPaths) {
        $vcvars = "C:\Program Files\Microsoft Visual Studio\2022\$edition\VC\Auxiliary\Build\vcvarsall.bat"
        if (Test-Path $vcvars) {
            $vsFound = $true
            Write-Log "Found Visual Studio $edition" "Green"
            break
        }
    }
    
    if ($vsFound) {
        $generator = "-G", "Visual Studio 17 2022"
    } else {
        Write-Log "Visual Studio not found, using MinGW Makefiles with clang" "Yellow"
        $generator = "-G", "MinGW Makefiles"
    }
    
    $cmakeArgs = @(
        "..",
        "-DCMAKE_BUILD_TYPE=RelWithDebInfo",
        "-DCMAKE_TOOLCHAIN_FILE=../vcpkg/scripts/buildsystems/vcpkg.cmake",
        "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
        "-DBUILD_SHARED_LIBS=ON",
        "-DENABLE_FFMPEG=ON",
        "-DENABLE_OPENCV=ON",
        "-DENABLE_GPU=OFF",
        "-DCMAKE_INSTALL_PREFIX=../install"
    )
    $cmakeArgs += $generator
    
    cmake $cmakeArgs 2>&1 | Tee-Object -FilePath "cmake_config.log"
    if ($LASTEXITCODE -ne 0) {
        Write-Log "CMake configuration failed!" "Red"
        Set-Location ..\..
        exit 1
    }
    
    # CMake build
    Write-Log "Building native libraries with -j $SAFE_JOBS_CPP..." "Green"
    
    if ($generator -contains "MinGW Makefiles") {
        cmake --build . --config RelWithDebInfo -j $SAFE_JOBS_CPP --target install 2>&1 | Tee-Object -FilePath "cmake_build.log"
    } else {
        cmake --build . --config RelWithDebInfo -j $SAFE_JOBS_CPP --target install 2>&1 | Tee-Object -FilePath "cmake_build.log"
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Log "Native build failed!" "Red"
        Set-Location ..\..
        exit 1
    }
    
    # Check artifacts
    if (Test-Path "../install/bin/video_processing.dll") {
        Write-Log "Native libraries built successfully" "Green"
    } else {
        Write-Log "Warning: Expected DLL not found, checking for alternatives..." "Yellow"
        Get-ChildItem "../install/bin" -Recurse -Filter "*.dll" | Select-Object FullName
    }
    
    Set-Location ..\..
}

# ============================================================================
# STEP 2: Gradle (Kotlin) Build
# ============================================================================
if ($BUILD_KOTLIN) {
    Write-Separator "Step 2: Building Kotlin Modules"
    
    # Set Gradle options
    $env:GRADLE_OPTS = "-Xmx6g -XX:MaxMetaspaceSize=768m -XX:+UseG1GC"
    $env:JAVA_TOOL_OPTIONS = "-Xmx6g"
    
    # Check Gradle wrapper
    if (-not (Test-Path "gradlew.bat")) {
        Write-Log "Gradle wrapper not found!" "Red"
        exit 1
    }
    
    # Build tasks (without androidNative based on gradle.properties)
    $gradleTasks = @(
        ":shared:compileKotlinLinuxX64",
        ":shared:compileKotlinWindowsX64",
        ":server:api:build",
        ":core:common:build",
        ":core:network:build",
        "--no-daemon",
        "--max-workers=$SAFE_JOBS_GRADLE",
        "-Porg.gradle.parallel=true"
    )
    
    Write-Log "Running Gradle build: $($gradleTasks -join ' ')..." "Green"
    .\gradlew.bat $gradleTasks 2>&1 | Tee-Object -FilePath "gradle_build.log"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Log "Gradle build failed!" "Red"
        exit 1
    }
    
    # Check artifacts
    $jarFiles = Get-ChildItem -Recurse -Path "server\api\build\libs" -Filter "*.jar" -ErrorAction SilentlyContinue
    if ($jarFiles) {
        Write-Log "Gradle build successful - found JAR: $($jarFiles.FullName)" "Green"
    } else {
        Write-Log "Warning: No JAR files found in server/api/build/libs/" "Yellow"
    }
}

# ============================================================================
# STEP 3: Web (Next.js) Build
# ============================================================================
if ($BUILD_WEB) {
    Write-Separator "Step 3: Building Web Interface"
    
    Set-Location "server\web"
    
    # Install dependencies if needed
    if (-not (Test-Path "node_modules")) {
        Write-Log "Installing npm dependencies..." "Green"
        npm ci --legacy-peer-deps --fetch-timeout=600000
        if ($LASTEXITCODE -ne 0) {
            Write-Log "npm ci failed, trying npm install..." "Yellow"
            npm install --legacy-peer-deps
        }
    }
    
    # Set Node memory
    $env:NODE_OPTIONS = "--max-old-space-size=4096"
    
    # Build
    Write-Log "Running Next.js build..." "Green"
    npm run build 2>&1 | Tee-Object -FilePath "../../web_build.log"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Log "Web build failed!" "Red"
        Set-Location ..\..
        exit 1
    }
    
    # Check artifacts
    if (Test-Path ".next\BUILD_ID") {
        Write-Log "Web build successful - .next directory created" "Green"
    } else {
        Write-Log "Warning: .next/BUILD_ID not found" "Yellow"
    }
    
    Set-Location ..\..
}

# ============================================================================
# Final Summary
# ============================================================================
$BUILD_END = Get-Date
$DURATION = [math]::Round(($BUILD_END - $BUILD_START).TotalSeconds, 2)

Write-Separator "BUILD SUMMARY"
Write-Log "Status: SUCCESS" "Green"
Write-Log "Duration: ${DURATION} seconds"
Write-Log "Start: $BUILD_START"
Write-Log "End: $BUILD_END"
Write-Log ""
Write-Log "Configuration:"
Write-Log "  SAFE_JOBS_CPP: $SAFE_JOBS_CPP"
Write-Log "  SAFE_JOBS_GRADLE: $SAFE_JOBS_GRADLE"
Write-Log "  SAFE_JOBS_NPM: $SAFE_JOBS_NPM"
Write-Log ""
Write-Log "Log files:"
Write-Log "  Main: $MAIN_LOG"
Write-Log "  Native: native/build/cmake_build.log"
Write-Log "  Gradle: gradle_build.log"
Write-Log "  Web: web_build.log"
Write-Separator

Write-Host "`nBuild completed successfully!" -ForegroundColor Green
exit 0
