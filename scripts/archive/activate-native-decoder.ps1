# Скрипт для активации нативного декодера в VideoDecoder.native.kt
# Использование: .\scripts\activate-native-decoder.ps1 [--check-only]

param(
    [switch]$CheckOnly = $false,
    [switch]$Help = $false,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($Help -or $ShowHelp) {
    Write-Host "Uncomment native videoprocessing imports in VideoDecoder.native.kt when cinterop klib exists."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\activate-native-decoder.ps1 -ShowHelp"
    Write-Host "  .\scripts\activate-native-decoder.ps1 [-CheckOnly] [-Help]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -CheckOnly, -c  Only verify klib paths; do not modify source"
    Write-Host "  -Help, -h       Show help"
    Write-Host "  -ShowHelp       Same as -Help"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Check-only OK, already active, or activation done"
    Write-Host "  1  No cinterop klib found"
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$NativeDecoderFile = Join-Path $ProjectRoot "core\network\src\nativeMain\kotlin\com\company\ipcamera\core\network\video\VideoDecoder.native.kt"

Write-Host "=== Activating Native Decoder ===" -ForegroundColor Cyan
Write-Host ""

# Проверка наличия cinterop биндингов
$BindingsPaths = @(
    "build\classes\kotlin\nativeLinux\cinterop\videoProcessing.klib",
    "build\classes\kotlin\nativeMacosX64\cinterop\videoProcessing.klib",
    "build\classes\kotlin\nativeMacosArm64\cinterop\videoProcessing.klib",
    "build\classes\kotlin\nativeWindows\cinterop\videoProcessing.klib"
)

$BindingsFound = $false
foreach ($bindingPath in $BindingsPaths) {
    $fullPath = Join-Path $ProjectRoot $bindingPath
    if (Test-Path $fullPath) {
        Write-Host "✅ Found bindings: $bindingPath" -ForegroundColor Green
        $BindingsFound = $true
    }
}

if (-not $BindingsFound) {
    Write-Host "[FAIL] CInterop bindings not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please generate bindings first:" -ForegroundColor Yellow
    Write-Host "  .\scripts\generate-cinterop-bindings.ps1" -ForegroundColor White
    exit 1
}

if ($CheckOnly) {
    Write-Host ""
    Write-Host "[OK] Bindings are available. Ready to activate." -ForegroundColor Green
    exit 0
}

# Чтение файла
$content = Get-Content $NativeDecoderFile -Raw

# Проверка, не активирован ли уже
if ($content -match "import com\.company\.ipcamera\.core\.network\.native\.videoprocessing\.\*") {
    Write-Host "⚠️  Native decoder appears to be already activated" -ForegroundColor Yellow
    Write-Host "   Checking if code is uncommented..." -ForegroundColor Gray

    if ($content -notmatch "// import com\.company\.ipcamera\.core\.network\.native\.videoprocessing\.\*") {
        Write-Host "[OK] Code is already activated!" -ForegroundColor Green
        exit 0
    }
}

Write-Host "Activating native decoder code..." -ForegroundColor Yellow

# Раскомментирование импортов
$content = $content -replace "// import com\.company\.ipcamera\.core\.network\.native\.videoprocessing\.\*", "import com.company.ipcamera.core.network.native.videoprocessing.*"

# Раскомментирование кода в init
$content = $content -replace "(?s)            /\*.*?            \*/", {
    param($match)
    $code = $match.Value
    # Удаляем комментарии /* и */
    $code = $code -replace "/\*", ""
    $code = $code -replace "\*/", ""
    # Удаляем лишние пробелы в начале строк
    $lines = $code -split "`n"
    $uncommented = $lines | ForEach-Object {
        if ($_ -match "^\s+//\s+(.+)$") {
            $matches[1]
        } else {
            $_
        }
    }
    $uncommented -join "`n"
}

# Более точная замена для блока init
$initPattern = "(?s)(init \{[^}]*?)            /\*.*?            \*/"
$initReplacement = {
    param($match)
    $initBlock = $match.Groups[1].Value
    $commentedCode = $match.Groups[0].Value

    # Извлекаем закомментированный код
    $codeBlock = $commentedCode -replace ".*?/\*", "" -replace "\*/.*", ""

    # Удаляем комментарии из строк
    $lines = $codeBlock -split "`n"
    $uncommented = $lines | ForEach-Object {
        if ($_ -match "^\s+//\s+(.+)$") {
            "            " + $matches[1]
        } elseif ($_ -match "^\s+//") {
            ""
        } else {
            $_
        }
    }

    $initBlock + "`n" + ($uncommented -join "`n")
}

$content = $content -replace $initPattern, $initReplacement

# Раскомментирование кода в decode
$content = $content -replace "(?s)                    /\*.*?                    \*/", {
    param($match)
    $code = $match.Value
    $code = $code -replace "/\*", "" -replace "\*/", ""
    $lines = $code -split "`n"
    $uncommented = $lines | ForEach-Object {
        if ($_ -match "^\s+//\s+(.+)$") {
            "                    " + $matches[1]
        } else {
            $_
        }
    }
    $uncommented -join "`n"
}

# Раскомментирование кода в getInfo
$content = $content -replace "(?s)                /\*.*?                \*/", {
    param($match)
    $code = $match.Value
    $code = $code -replace "/\*", "" -replace "\*/", ""
    $lines = $code -split "`n"
    $uncommented = $lines | ForEach-Object {
        if ($_ -match "^\s+//\s+(.+)$") {
            "                " + $matches[1]
        } else {
            $_
        }
    }
    $uncommented -join "`n"
}

# Раскомментирование кода в release
$content = $content -replace "(?s)            /\*.*?            \*/", {
    param($match)
    $code = $match.Value
    $code = $code -replace "/\*", "" -replace "\*/", ""
    $lines = $code -split "`n"
    $uncommented = $lines | ForEach-Object {
        if ($_ -match "^\s+//\s+(.+)$") {
            "            " + $matches[1]
        } else {
            $_
        }
    }
    $uncommented -join "`n"
}

# Сохранение файла
Set-Content -Path $NativeDecoderFile -Value $content -NoNewline

Write-Host "[OK] Native decoder code activated!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Build the project: .\gradlew.bat :core:network:build" -ForegroundColor White
Write-Host "  2. Test the native decoder" -ForegroundColor White
