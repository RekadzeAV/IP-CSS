# PowerShell скрипт для сборки RTSP библиотеки на Windows
# Требует: Visual Studio 2022, CMake 3.20+, FFmpeg 6.0+

param(
    [string]$Configuration = "Release",
    [string]$Platform = "x64",
    [switch]$Clean,
    [switch]$InstallDeps,
    [switch]$Help
)

# Цветовой вывод
function Write-Header {
    param([string]$Text)
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host "$Text" -ForegroundColor Cyan
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Success {
    param([string]$Text)
    Write-Host "✅ $Text" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Text)
    Write-Host "⚠️  $Text" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Text)
    Write-Host "❌ $Text" -ForegroundColor Red
}

function Write-Info {
    param([string]$Text)
    Write-Host "ℹ️  $Text" -ForegroundColor Gray
}

function Write-Step {
    param([string]$Text)
    Write-Host "🔹 $Text" -ForegroundColor Cyan
}

# Показать справку
if ($Help) {
    Write-Header "RTSP Native Library Builder (Windows)"
    Write-Host "Использование: .\build-windows.ps1 [OPTIONS] [CONFIGURATION]"
    Write-Host ""
    Write-Host "Параметры:"
    Write-Host "  CONFIGURATION  Release или Debug (по умолчанию: Release)"
    Write-Host "  -Platform      x64 или x86 (по умолчанию: x64)"
    Write-Host ""
    Write-Host "Опции:"
    Write-Host "  -Clean         Очистить предыдущую сборку"
    Write-Host "  -InstallDeps   Показать инструкции по установке зависимостей"
    Write-Host "  -Help          Показать эту справку"
    Write-Host ""
    Write-Host "Примеры:"
    Write-Host "  .\build-windows.ps1                    # Сборка Release x64"
    Write-Host "  .\build-windows.ps1 Debug              # Сборка Debug"
    Write-Host "  .\build-windows.ps1 -Clean             # Очистка и сборка"
    Write-Host "  .\build-windows.ps1 -InstallDeps       # Показать инструкции"
    exit 0
}

# Показать инструкции по установке зависимостей
if ($InstallDeps) {
    Write-Header "Инструкции по установке зависимостей"
    Write-Host ""
    Write-Host "1. Visual Studio 2022"
    Write-Host "   Скачайте: https://visualstudio.microsoft.com/downloads/"
    Write-Host "   Выберите рабочие нагрузки:"
    Write-Host "   - Разработка классических приложений на C++"
    Write-Host "   - Компоненты CMake для C++"
    Write-Host ""
    Write-Host "2. CMake 3.20+"
    Write-Host "   Скачайте: https://cmake.org/download/"
    Write-Host "   Или через winget: winget install Kitware.CMake"
    Write-Host ""
    Write-Host "3. FFmpeg 6.0+"
    Write-Host "   Скачайте: https://github.com/BtbN/FFmpeg-Builds/releases"
    Write-Host "   Распакуйте в C:\ffmpeg"
    Write-Host "   Или через winget: winget install Gyan.FFmpeg"
    Write-Host ""
    Write-Host "4. Java 17+"
    Write-Host "   Скачайте: https://adoptium.net/"
    Write-Host "   Или через winget: winget install Eclipse.Adoptium.17"
    Write-Host "   Установите переменную окружения JAVA_HOME"
    Write-Host ""
    Write-Host "5. Ninja (опционально, для ускорения сборки)"
    Write-Host "   winget install Ninja-build.Ninja"
    Write-Host ""
    exit 0
}

Write-Header "RTSP Native Library Builder (Windows)"

# Проверка требований
Write-Step "Проверка требований..."
Write-Host ""

$missingDeps = @()
$warnings = @()

# Проверка CMake
$cmakeVersion = $null
try {
    $cmakeOutput = cmake --version 2>&1 | Select-Object -First 1
    if ($cmakeOutput) {
        $cmakeVersion = ($cmakeOutput -match '\d+\.\d+\.\d+' | Out-Null) ? $Matches[0] : "unknown"
        Write-Success "CMake $cmakeVersion"
    } else {
        $missingDeps += "CMake"
        Write-Warning "CMake не найден"
    }
} catch {
    $missingDeps += "CMake"
    Write-Warning "CMake не найден"
}

# Проверка Visual Studio
$vsPath = $null
try {
    # Путь к Visual Studio 2022
    $vsWherePath = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
    if (Test-Path $vsWherePath) {
        $vsPath = & $vsWherePath -latest -property installationPath 2>$null
    }
    
    if (-not $vsPath) {
        # Поиск в реестре
        $vsPath = Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\VisualStudio\SxS\VSLoad" -ErrorAction SilentlyContinue | 
                  Select-Object -ExpandProperty "17.0" -ErrorAction SilentlyContinue
    }
    
    if ($vsPath) {
        Write-Success "Visual Studio 2022: $vsPath"
    } else {
        $warnings += "Visual Studio 2022 не найден (требуется для JNI биндингов)"
        Write-Warning "Visual Studio 2022 не найден"
    }
} catch {
    $warnings += "Не удалось определить Visual Studio"
    Write-Warning "Не удалось определить Visual Studio"
}

# Проверка FFmpeg
$ffmpegPath = "C:\ffmpeg"
$ffmpegLib = "$ffmpegPath\lib"
$ffmpegInclude = "$ffmpegPath\include"

if (Test-Path $ffmpegLib) {
    Write-Success "FFmpeg: $ffmpegPath"
    
    # Проверка необходимых библиотек
    $requiredLibs = @("avcodec", "avformat", "avutil", "swscale", "swresample")
    $missingLibs = @()
    
    foreach ($lib in $requiredLibs) {
        $libFile = Get-ChildItem "$ffmpegLib\$lib*.lib" -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $libFile) {
            $missingLibs += $lib
        }
    }
    
    if ($missingLibs.Count -gt 0) {
        $warnings += "Отсутствуют библиотеки FFmpeg: $($missingLibs -join ', ')"
        Write-Warning "Отсутствуют библиотеки FFmpeg: $($missingLibs -join ', ')"
    }
} else {
    $warnings += "FFmpeg не найден в $ffmpegPath"
    Write-Warning "FFmpeg не найден в $ffmpegPath"
    Write-Info "Скачайте: https://github.com/BtbN/FFmpeg-Builds/releases"
}

# Проверка Java
$javaFound = $false
if ($env:JAVA_HOME) {
    if (Test-Path "$env:JAVA_HOME\bin\java.exe") {
        $javaVersion = & "$env:JAVA_HOME\bin\java.exe" -version 2>&1 | Select-String -Pattern 'version "([\d.]+)"' | Select-Object -First 1
        $javaVer = if ($javaVersion) { $javaVersion.Matches[0].Groups[1].Value } else { "unknown" }
        Write-Success "JAVA_HOME: $env:JAVA_HOME (Java $javaVer)"
        $javaFound = $true
    }
}

if (-not $javaFound) {
    if (Test-Path "${env:ProgramFiles}\Java\jdk*" -ErrorAction SilentlyContinue) {
        $javaHome = Get-ChildItem "${env:ProgramFiles}\Java" -Filter "jdk*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if ($javaHome) {
            $env:JAVA_HOME = $javaHome.FullName
            Write-Success "Найдено: $env:JAVA_HOME"
            $javaFound = $true
        }
    }
    
    if (-not $javaFound) {
        $warnings += "Java 17+ не найдена (требуется для JNI биндингов)"
        Write-Warning "Java 17+ не найдена"
        Write-Info "Установите: winget install Eclipse.Adoptium.17"
    }
}

# Проверка Visual C++ Redistributable (для запуска DLL)
try {
    $vcredist = Get-ItemProperty "HKLM:\SOFTWARE\WOW6432Node\Microsoft\VisualStudio\14.0\VC\Runtimes\x64" -ErrorAction SilentlyContinue
    if ($vcredist) {
        Write-Success "Visual C++ Redistributable: $($vcredist.Version)"
    }
} catch {
    Write-Info "Visual C++ Redistributable не проверен"
}

# Вывод предупреждений
if ($warnings.Count -gt 0) {
    Write-Host ""
    Write-Warning "Обнаружены проблемы:"
    foreach ($warning in $warnings) {
        Write-Host "  - $warning" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Info "Сборка может не удалиться. Продолжить? [Y/n] " -ForegroundColor Gray
    $response = Read-Host
    if ($response -eq "n" -or $response -eq "N") {
        exit 0
    }
}

# Проверка критических зависимостей
if ($missingDeps.Count -gt 0) {
    Write-Host ""
    Write-Error "Не хватает критических зависимостей: $($missingDeps -join ', ')"
    Write-Host ""
    Write-Info "Установите зависимости и запустите скрипт снова"
    Write-Info "Или используйте: .\build-windows.ps1 -InstallDeps"
    exit 1
}

Write-Host ""

# Очистка (если требуется)
if ($Clean) {
    Write-Step "Очистка предыдущей сборки..."
    if (Test-Path "build") {
        Remove-Item -Recurse -Force "build"
        Write-Success "Каталог build удален"
    } else {
        Write-Info "Каталог build не существует"
    }
    Write-Host ""
}

# Создание директории сборки
if (!(Test-Path "build")) {
    New-Item -ItemType Directory -Path "build" | Out-Null
    Write-Success "Создан каталог build"
}

Write-Host ""
Write-Step "Конфигурация CMake"
Write-Host ""

# Настройка CMake
$cmakeArgs = @()
$cmakeArgs += "-DCMAKE_BUILD_TYPE=$Configuration"

if ($javaFound) {
    $cmakeArgs += "-DJAVA_HOME=$env:JAVA_HOME"
    Write-Info "JAVA_HOME: $env:JAVA_HOME"
}

# Выбор генератора
$generator = ""
if ($vsPath) {
    $generator = "Visual Studio 17 2022"
    $cmakeArgs += "-G$generator"
    $cmakeArgs += "-A$Platform"
    Write-Info "Генератор: $generator ($Platform)"
} else {
    # Попробуем использовать Ninja если доступен
    if (Get-Command ninja -ErrorAction SilentlyContinue) {
        $generator = "Ninja"
        $cmakeArgs += "-G$generator"
        Write-Info "Генератор: Ninja"
    } else {
        Write-Warning "Visual Studio не найден, используем MinGW Makefiles"
        $generator = "MinGW Makefiles"
        $cmakeArgs += "-G$generator"
    }
}

# Путь к FFmpeg
if (Test-Path $ffmpegPath) {
    $cmakeArgs += "-DFFMPEG_DIR=$ffmpegPath"
    $cmakeArgs += "-DCMAKE_PREFIX_PATH=$ffmpegPath"
    Write-Info "FFmpeg: $ffmpegPath"
}

Write-Info "Вызов: cmake $($cmakeArgs -join ' ') .."
Write-Host ""

# Конфигурация
Set-Location build
cmake $cmakeArgs ..
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Error "Ошибка конфигурации CMake"
    Set-Location ..
    exit 1
}
Set-Location ..

Write-Host ""
Write-Step "Сборка"
Write-Host ""

# Сборка
Set-Location build
cmake --build . --config $Configuration
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Error "Ошибка сборки"
    Set-Location ..
    exit 1
}
Set-Location ..

Write-Host ""
Write-Step "Установка библиотеки"
Write-Host ""

# Показать результаты
$libPath = "build\bin\$Platform\$Configuration\video_processing.dll"
$libDir = "lib\windows\$Platform"

if (!(Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir -Force | Out-Null
}

if (Test-Path $libPath) {
    $size = [math]::Round((Get-Item $libPath).Length / 1MB, 2)
    
    Copy-Item $libPath "$libDir\video_processing.dll" -Force
    
    Write-Success "Сборка завершена!"
    Write-Host ""
    Write-Host "Библиотека: $libDir\video_processing.dll" -ForegroundColor Green
    Write-Host "Размер: $size MB" -ForegroundColor Gray
} else {
    Write-Warning "Библиотека не найдена в ожидаемом месте: $libPath"
    Write-Info "Проверьте каталог build\bin\$Platform\$Configuration\"
}

# Проверка экспорта символов
if (Get-Command dumpbin -ErrorAction SilentlyContinue) {
    Write-Host ""
    Write-Step "Проверка экспортируемых символов..."
    
    $dllToCheck = if (Test-Path "$libDir\video_processing.dll") {
        "$libDir\video_processing.dll"
    } else {
        $libPath
    }
    
    if (Test-Path $dllToCheck) {
        $exports = & dumpbin /exports $dllToCheck 2>&1 | Out-String
        if ($exports -match "rtsp_client") {
            Write-Success "Символы RTSP экспортированы правильно"
        } else {
            Write-Warning "Символы RTSP не найдены (проверьте CMakeLists.txt)"
        }
    }
}

Write-Host ""
Write-Header "Готово!"

Write-Info "Следующие шаги:"
Write-Info "1. Протестируйте: .\gradlew.bat :core:network:desktopTest"
Write-Info "2. Проверьте в приложении: запустите с RTSP камерой"
Write-Info "3. Создайте релиз: git tag v1.0.0 && git push origin v1.0.0"
Write-Host ""
