# PowerShell скрипт сборки IP-CSS для Windows
# Отдельный скрипт для корректной работы на Windows

[CmdletBinding()]
param(
    [ValidateSet("full", "quick", "native", "kotlin", "web", "clean")]
    [string]$BuildMode = "full",
    
    [switch]$CleanFirst,
    [int]$MaxAttempts = 3
)

# =============================================================================
# Константы и конфигурация
# =============================================================================
$BuildStart = Get-Date
$SafeJobsCpp = 4
$SafeJobsGradle = 2
$SafeJobsNpm = 4

# Флаги сборки (по умолчанию)
$BuildNative = $true
$BuildKotlin = $true
$BuildWeb = $true
$BuildDocker = $false

# Цвета
$Colors = @{
    Info = "Green"
    Warn = "Yellow"
    Error = "Red"
    Success = "Cyan"
}

# =============================================================================
# Функции детекции и подготовки
# =============================================================================

function Detect-Environment {
    Write-Host "=== Environment Detection ===" -ForegroundColor $Colors.Info
    
    # Получение информации о системе
    $CPU_CORES = (Get-CimInstance Win32_Processor | Measure-Object -Property NumberOfLogicalProcessors -Sum).Sum
    $TotalRamBytes = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory
    $FreeRamBytes = (Get-CimInstance Win3_OperatingSystem).FreePhysicalMemory * 1KB
    
    $TotalRamGB = [math]::Round($TotalRamBytes / 1GB, 2)
    $FreeRamGB = [math]::Round($FreeRamBytes / 1GB, 2)
    
    # Расчёт SAFE_JOBS
    $SafeJobsCpp = [math]::Max(1, [math]::Min($FreeRamGB / 3, $CPU_CORES))
    $SafeJobsGradle = [math]::Max(1, [math]::Min($FreeRamGB / 4, 4))
    $SafeJobsNpm = [math]::Max(1, [math]::Min($FreeRamGB / 2, 8))
    
    Write-Host "[INFO] System: CPU=$CPU_CORES cores, RAM=${TotalRamGB}GB total, ${FreeRamGB}GB free" -ForegroundColor $Colors.Info
    Write-Host "[INFO] SAFE_JOBS: CPP=$SafeJobsCpp, GRADLE=$SafeJobsGradle, NPM=$SafeJobsNpm" -ForegroundColor $Colors.Info
    
    return @{
        CpuCores = $CPU_CORES
        TotalRamGB = $TotalRamGB
        FreeRamGB = $FreeRamGB
        SafeJobsCpp = [int]$SafeJobsCpp
        SafeJobsGradle = [int]$SafeJobsGradle
        SafeJobsNpm = [int]$SafeJobsNpm
    }
}

function Test-Dependencies {
    Write-Host "=== Dependency Validation ===" -ForegroundColor $Colors.Info
    $Missing = 0
    
    # Java 17+
    try {
        $JavaVersion = java -version 2>&1 | Select-String -Pattern 'version "(\d+)' | ForEach-Object { $_.Matches.Groups[1].Value }
        if ([int]$JavaVersion -lt 17) {
            Write-Host "[ERROR] Java 17+ required, found $JavaVersion" -ForegroundColor $Colors.Error
            $Missing++
        } else {
            Write-Host "[OK] Java $JavaVersion" -ForegroundColor $Colors.Success
        }
    } catch {
        Write-Host "[ERROR] Java not found" -ForegroundColor $Colors.Error
        $Missing++
    }
    
    # CMake 3.15+
    try {
        $CMakeVersion = cmake --version | Select-Object -First 1 | Select-String -Pattern '(\d+\.\d+)' | ForEach-Object { $_.Matches.Groups[1].Value }
        $CMakeMajor, $CMakeMinor = $CMakeVersion -split '\.' | ForEach-Object { [int]$_ }
        if ($CMakeMajor -lt 3 -or ($CMakeMajor -eq 3 -and $CMakeMinor -lt 15)) {
            Write-Host "[ERROR] CMake 3.15+ required, found $CMakeVersion" -ForegroundColor $Colors.Error
            $Missing++
        } else {
            Write-Host "[OK] CMake $CMakeVersion" -ForegroundColor $Colors.Success
        }
    } catch {
        Write-Host "[ERROR] CMake not found" -ForegroundColor $Colors.Error
        $Missing++
    }
    
    # Node.js 20+
    try {
        $NodeVersion = node --version | Select-String -Pattern 'v(\d+)' | ForEach-Object { $_.Matches.Groups[1].Value }
        if ([int]$NodeVersion -lt 20) {
            Write-Host "[ERROR] Node.js 20+ required, found $NodeVersion" -ForegroundColor $Colors.Error
            $Missing++
        } else {
            Write-Host "[OK] Node.js $NodeVersion" -ForegroundColor $Colors.Success
        }
    } catch {
        Write-Host "[ERROR] Node.js not found" -ForegroundColor $Colors.Error
        $Missing++
    }
    
    # Git
    try {
        $GitVersion = git --version | Select-String -Pattern '(\d+\.\d+)' | ForEach-Object { $_.Matches.Groups[1].Value }
        Write-Host "[OK] Git $GitVersion" -ForegroundColor $Colors.Success
    } catch {
        Write-Host "[ERROR] Git not found" -ForegroundColor $Colors.Error
        $Missing++
    }
    
    # Git LFS (опционально)
    try {
        $GitLfsVersion = git-lfs version | Select-Object -First 1 | Select-String -Pattern '(\d+\.\d+\.\d+)' | ForEach-Object { $_.Matches.Groups[1].Value }
        Write-Host "[OK] Git LFS $GitLfsVersion" -ForegroundColor $Colors.Success
    } catch {
        Write-Host "[WARN] Git LFS not found (large files may fail to checkout)" -ForegroundColor $Colors.Warn
    }
    
    if ($Missing -gt 0) {
        Write-Host "[ERROR] $Missing dependencies missing" -ForegroundColor $Colors.Error
        exit 1
    }
}

function Prepare-Environment {
    Write-Host "=== Environment Preparation ===" -ForegroundColor $Colors.Info
    
    # Создание .env если отсутствует
    if (-not (Test-Path ".env") -and (Test-Path ".env.example")) {
        Write-Host "[INFO] Creating .env from .env.example..." -ForegroundColor $Colors.Info
        Copy-Item ".env.example" ".env"
        
        # Генерация секретов (PowerShell native)
        $RandomBytes = New-Object byte[] 32
        $Random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        $Random.GetBytes($RandomBytes)
        $DbPass = [Convert]::ToBase64String($RandomBytes) -replace '[^a-zA-Z0-9]', '' | Select-Object -First 1
        
        $RandomBytes = New-Object byte[] 64
        $Random.GetBytes($RandomBytes)
        $JwtSec = [Convert]::ToBase64String($RandomBytes) -replace '[^a-zA-Z0-9]', '' | Select-Object -First 1
        
        (Get-Content ".env") | ForEach-Object {
            if ($_ -match '^DB_PASSWORD=') { "DB_PASSWORD=$DbPass" }
            elseif ($_ -match '^JWT_SECRET=') { "JWT_SECRET=$JwtSec" }
            else { $_ }
        } | Set-Content ".env"
        
        Write-Host "[INFO] .env created with random secrets" -ForegroundColor $Colors.Success
    }
    
    # Настройка ccache (если доступен)
    if (Get-Command sccache -ErrorAction SilentlyContinue) {
        $env:CC = "sccache cl"
        $env:CXX = "sccache cl"
        $env:CMAKE_C_COMPILER_LAUNCHER = "sccache"
        $env:CMAKE_CXX_COMPILER_LAUNCHER = "sccache"
        Write-Host "[INFO] sccache enabled" -ForegroundColor $Colors.Success
    } elseif (Get-Command ccache -ErrorAction SilentlyContinue) {
        $env:CC = "ccache cl"
        $env:CXX = "ccache cl"
        $env:CMAKE_C_COMPILER_LAUNCHER = "ccache"
        $env:CMAKE_CXX_COMPILER_LAUNCHER = "ccache"
        Write-Host "[INFO] ccache enabled" -ForegroundColor $Colors.Success
    }
    
    # Подготовка gradle.properties
    if (Test-Path "gradle.properties") {
        $GradleProps = Get-Content "gradle.properties"
        if ($GradleProps -notcontains "org.gradle.caching=true") {
            Add-Content "gradle.properties" "org.gradle.caching=true"
        }
        if ($GradleProps -notcontains "org.gradle.parallel=true") {
            Add-Content "gradle.properties" "org.gradle.parallel=true"
        }
        Write-Host "[INFO] Gradle caching enabled" -ForegroundColor $Colors.Success
    }
}

# =============================================================================
# Функции сборки
# =============================================================================

function Build-Native {
    Write-Host "=== Building Native Libraries ===" -ForegroundColor $Colors.Info
    
    Set-Location native
    
    # Проверка vcpkg
    if (-not (Test-Path "vcpkg")) {
        Write-Host "[INFO] Cloning vcpkg..." -ForegroundColor $Colors.Info
        git clone https://github.com/Microsoft/vcpkg.git | Out-Null
        .\vcpkg\bootstrap-vcpkg.bat | Out-Null
    }
    
    # Создаём build директорию
    if (-not (Test-Path "build")) { New-Item -ItemType Directory -Path build | Out-Null }
    Set-Location build
    
    # Конфигурация CMake
    Write-Host "[INFO] Configuring CMake..." -ForegroundColor $Colors.Info
    cmake .. `
        -DCMAKE_BUILD_TYPE=RelWithDebInfo `
        -DCMAKE_TOOLCHAIN_FILE=../vcpkg/scripts/buildsystems/vcpkg.cmake `
        -DCMAKE_EXPORT_COMPILE_COMMANDS=ON `
        -DBUILD_SHARED_LIBS=ON `
        -DENABLE_FFMPEG=ON `
        -DENABLE_OPENCV=ON `
        -DENABLE_GPU=OFF `
        -DCMAKE_INSTALL_PREFIX=../install `
        -G "Visual Studio 17 2022" | Out-Null
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] CMake configuration failed" -ForegroundColor $Colors.Error
        Get-Content "CMakeFiles\CMakeOutput.log" -Tail 50
        Set-Location ..\..
        return 1
    }
    
    # Сборка
    Write-Host "[INFO] Building with -j $SafeJobsCpp..." -ForegroundColor $Colors.Info
    cmake --build . --config RelWithDebInfo -j $SafeJobsCpp --target install 2>&1 | Tee-Object -FilePath ..\..\native_build.log
    
    $ExitCode = $LASTEXITCODE
    Set-Location ..\..
    
    if ($ExitCode -eq 0) {
        Write-Host "[INFO] Native build completed" -ForegroundColor $Colors.Success
        return 0
    } else {
        Write-Host "[ERROR] Native build failed with exit code $ExitCode" -ForegroundColor $Colors.Error
        return 1
    }
}

function Build-Gradle {
    Write-Host "=== Building Kotlin Modules ===" -ForegroundColor $Colors.Info
    
    # Получаем задачи сборки
    $GradleTasks = "ciBuildCoreSharedModules", "buildNativeLibraries", ":server:api:build"
    
    Write-Host "[INFO] Running: ./gradlew $GradleTasks" -ForegroundColor $Colors.Info
    
    # Запуск Gradle
    .\gradlew.bat $GradleTasks `
        --no-daemon `
        --max-workers=$SafeJobsGradle `
        -Dorg.gradle.jvmargs="-Xmx6g -XX:MaxMetaspaceSize=768m" `
        -Porg.gradle.parallel=true `
        2>&1 | Tee-Object -FilePath gradle_build.log
    
    $ExitCode = $LASTEXITCODE
    
    if ($ExitCode -eq 0) {
        Write-Host "[INFO] Gradle build completed" -ForegroundColor $Colors.Success
        return 0
    } else {
        Write-Host "[ERROR] Gradle build failed with exit code $ExitCode" -ForegroundColor $Colors.Error
        Get-Content gradle_build.log -Tail 100 | Select-String -Pattern "error|exception|failed" | Select-Object -First 20
        return 1
    }
}

function Build-Web {
    Write-Host "=== Building Web Interface ===" -ForegroundColor $Colors.Info
    
    Set-Location server\web
    
    # Проверка node_modules
    if (-not (Test-Path "node_modules")) {
        Write-Host "[INFO] Installing npm dependencies..." -ForegroundColor $Colors.Info
        npm ci --legacy-peer-deps --fetch-timeout=600000 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[WARN] npm ci failed, trying npm install..." -ForegroundColor $Colors.Warn
            npm install --legacy-peer-deps | Out-Null
        }
    }
    
    # Установка переменной памяти
    $env:NODE_OPTIONS = "--max-old-space-size=4096"
    
    # Сборка
    npm run build 2>&1 | Tee-Object -FilePath ..\..\web_build.log
    
    $ExitCode = $LASTEXITCODE
    Set-Location ..\..
    
    if ($ExitCode -eq 0) {
        if (Test-Path "server\web\.next") -and (Test-Path "server\web\.next\BUILD_ID") {
            Write-Host "[INFO] Web build completed successfully" -ForegroundColor $Colors.Success
            return 0
        } else {
            Write-Host "[ERROR] Web build incomplete - .next directory missing" -ForegroundColor $Colors.Error
            return 1
        }
    } else {
        Write-Host "[ERROR] Web build failed with exit code $ExitCode" -ForegroundColor $Colors.Error
        Get-Content web_build.log -Tail 100 | Select-String -Pattern "error|failed" | Select-Object -First 20
        return 1
    }
}

# =============================================================================
# Обработка ошибок
# =============================================================================

function Handle-Error {
    param(
        [string]$LogFile,
        [string]$Stage,
        [int]$ExitCode,
        [int]$Attempt = 1
    )
    
    Write-Host "[ERROR] Build stage '$Stage' failed with exit code $ExitCode (attempt $Attempt)" -ForegroundColor $Colors.Error
    
    # Анализ последних строк лога
    if (Test-Path $LogFile) {
        Write-Host "[INFO] Last 50 lines of log:" -ForegroundColor $Colors.Info
        Get-Content $LogFile -Tail 50
    }
    
    # Автоматические исправления
    switch ($Stage) {
        "native" {
            if (Get-Content $LogFile -ErrorAction SilentlyContinue | Select-String -Pattern "No space left on device") {
                Write-Host "[INFO] Cleaning cache to free space..." -ForegroundColor $Colors.Info
                ccache -C 2>$null
                Remove-Item -Recurse -Force native\build -ErrorAction SilentlyContinue
            } elseif (Get-Content $LogFile -ErrorAction SilentlyContinue | Select-String -Pattern "Killed") {
                Write-Host "[INFO] Out of memory - reducing SafeJobsCpp" -ForegroundColor $Colors.Info
                $Script:SafeJobsCpp = [math]::Max(1, $Script:SafeJobsCpp / 2)
            }
        }
        "gradle" {
            if (Get-Content $LogFile -ErrorAction SilentlyContinue | Select-String -Pattern "OutOfMemoryError") {
                Write-Host "[INFO] Out of memory - reducing SafeJobsGradle" -ForegroundColor $Colors.Info
                $Script:SafeJobsGradle = 1
                $env:GRADLE_OPTS = "-Xmx8g"
            }
        }
        "web" {
            if (Get-Content $LogFile -ErrorAction SilentlyContinue | Select-String -Pattern "JavaScript heap out of memory") {
                Write-Host "[INFO] Increasing Node.js memory" -ForegroundColor $Colors.Info
                $env:NODE_OPTIONS = "--max-old-space-size=8192"
            }
        }
    }
    
    return $Attempt -ge $MaxAttempts
}

# =============================================================================
# Валидация и сводка
# =============================================================================

function Test-Artifacts {
    Write-Host "=== Validating Build Artifacts ===" -ForegroundColor $Colors.Info
    $Errors = 0
    
    # Native libraries
    Write-Host "[INFO] Checking native libraries..." -ForegroundColor $Colors.Info
    $NativeLibs = Get-ChildItem -Recurse -Path "native" -Include "*.dll", "*.lib" -ErrorAction SilentlyContinue
    if ($NativeLibs) {
        $NativeLibs | ForEach-Object { Write-Host "[OK] Found: $($_.FullName)" -ForegroundColor $Colors.Success }
    } else {
        Write-Host "[WARN] No native libraries found (may be expected)" -ForegroundColor $Colors.Warn
    }
    
    # Gradle JARs
    Write-Host "[INFO] Checking Gradle artifacts..." -ForegroundColor $Colors.Info
    $Jars = Get-ChildItem -Recurse -Path "server\api\build" -Include "*.jar" -ErrorAction SilentlyContinue
    if ($Jars) {
        $Jars | ForEach-Object { Write-Host "[OK] Found: $($_.FullName)" -ForegroundColor $Colors.Success }
    } else {
        Write-Host "[WARN] No API JAR found" -ForegroundColor $Colors.Warn
    }
    
    # Web build
    Write-Host "[INFO] Checking web artifacts..." -ForegroundColor $Colors.Info
    if (Test-Path "server\web\.next\BUILD_ID") {
        Write-Host "[OK] Web build validated" -ForegroundColor $Colors.Success
    } else {
        Write-Host "[ERROR] Web build incomplete" -ForegroundColor $Colors.Error
        $Errors++
    }
    
    return $Errors -eq 0
}

function Write-Summary {
    $BuildEnd = Get-Date
    $Duration = New-TimeSpan -Start $BuildStart -End $BuildEnd
    
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor $Colors.Success
    Write-Host "                    BUILD SUMMARY" -ForegroundColor $Colors.Success
    Write-Host "================================================================" -ForegroundColor $Colors.Success
    Write-Host "Status:                    SUCCESS" -ForegroundColor $Colors.Success
    Write-Host "Duration:                  $($Duration.Minutes)m $($Duration.Seconds)s" -ForegroundColor $Colors.Info
    Write-Host "Start time:                $($BuildStart.ToString())" -ForegroundColor $Colors.Info
    Write-Host "End time:                  $($BuildEnd.ToString())" -ForegroundColor $Colors.Info
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor $Colors.Success
}

# =============================================================================
# Основной pipeline
# =============================================================================

function Main {
    Write-Host "================================================================" -ForegroundColor $Colors.Success
    Write-Host "         IP-CSS Build Pipeline - Starting" -ForegroundColor $Colors.Success
    Write-Host "================================================================" -ForegroundColor $Colors.Success
    
    # 1. Детекция окружения
    $EnvInfo = Detect-Environment
    Test-Dependencies
    Prepare-Environment
    
    # 2. Основной цикл сборки
    $Attempt = 1
    while ($Attempt -le $MaxAttempts) {
        Write-Host "=== Build Attempt $Attempt/$MaxAttempts ===" -ForegroundColor $Colors.Info
        
        # Native
        if ($BuildNative) {
            if (-not (Build-Native)) {
                if (Handle-Error -LogFile "native_build.log" -Stage "native" -ExitCode $LASTEXITCODE -Attempt $Attempt) {
                    Write-Host "[ERROR] Native build failed after $MaxAttempts attempts" -ForegroundColor $Colors.Error
                    exit 1
                }
                $Attempt++
                continue
            }
        }
        
        # Gradle
        if ($BuildKotlin) {
            if (-not (Build-Gradle)) {
                if (Handle-Error -LogFile "gradle_build.log" -Stage "gradle" -ExitCode $LASTEXITCODE -Attempt $Attempt) {
                    Write-Host "[ERROR] Gradle build failed after $MaxAttempts attempts" -ForegroundColor $Colors.Error
                    exit 1
                }
                $Attempt++
                continue
            }
        }
        
        # Web
        if ($BuildWeb) {
            if (-not (Build-Web)) {
                if (Handle-Error -LogFile "web_build.log" -Stage "web" -ExitCode $LASTEXITCODE -Attempt $Attempt) {
                    Write-Host "[ERROR] Web build failed after $MaxAttempts attempts" -ForegroundColor $Colors.Error
                    exit 1
                }
                $Attempt++
                continue
            }
        }
        
        break
    }
    
    # 3. Валидация
    if (-not (Test-Artifacts)) {
        Write-Host "[ERROR] Artifact validation failed" -ForegroundColor $Colors.Error
        exit 1
    }
    
    # 4. Сводка
    Write-Summary
    
    Write-Host "[INFO] Pipeline completed successfully" -ForegroundColor $Colors.Success
    exit 0
}

# Запуск
Main
