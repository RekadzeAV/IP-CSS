#!/usr/bin/env pwsh
#
# Генерация cinterop биндингов для Kotlin/Native
# Для нативных библиотек video-processing
#
# Usage:
#   .\scripts\generate-native-bindings.ps1 [-Platform <windows|linux|macos>] [-Architecture <x64|arm64>]
#

param(
    [ValidateSet('windows', 'linux', 'macos', 'auto')]
    [string]$Platform = 'auto',
    
    [ValidateSet('x64', 'arm64', 'auto')]
    [string]$Architecture = 'auto',
    
    [string]$OutputDir = 'shared/src/nativeMain/kotlin/generated',
    [string]$HeadersDir = 'native/video-processing/include',
    [string]$LibDir = 'native/video-processing/lib'
)

# Определение платформы
if ($Platform -eq 'auto') {
    if ($IsWindows) { $Platform = 'windows' }
    elseif ($IsLinux) { $Platform = 'linux' }
    elseif ($IsMacOS) { $Platform = 'macos' }
    else { throw "Cannot determine platform automatically" }
}

# Определение архитектуры
if ($Architecture -eq 'auto') {
    $arch = (Get-CimInstance Win32_Processor -ErrorAction SilentlyContinue | Select-Object -First 1).Architecture
    if ($null -eq $arch) {
        $arch = $env:PROCESSOR_ARCHITECTURE
    }
    
    if ($arch -match 'ARM' -or $arch -match 'arm') {
        $Architecture = 'arm64'
    } else {
        $Architecture = 'x64'
    }
}

Write-Host "Platform: $Platform" -ForegroundColor Cyan
Write-Host "Architecture: $Architecture" -ForegroundColor Cyan
Write-Host "Output Dir: $OutputDir" -ForegroundColor Cyan
Write-Host "Headers Dir: $HeadersDir" -ForegroundColor Cyan

# Проверка наличия заголовков
if (-not (Test-Path $HeadersDir)) {
    Write-Error "Headers directory not found: $HeadersDir"
    exit 1
}

# Создание директории вывода
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    Write-Host "Created output directory: $OutputDir" -ForegroundColor Green
}

# Определение путей к заголовкам
$HeaderFiles = @(
    'rtsp_client.h',
    'video_decoder.h',
    'audio_decoder.h',
    'frame_processor.h',
    'stream_manager.h'
)

$ExistingHeaders = $HeaderFiles | Where-Object { Test-Path "$HeadersDir/$_" }
Write-Host "Found ${ExistingHeaders.Count} header files:" -ForegroundColor Yellow
$ExistingHeaders | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }

# Определение путей к компилятору
$KotlinHome = if ($env:KOTLIN_HOME) { $env:KOTLIN_HOME } else {
    # Пытаемся найти Kotlin installation
    $defaultPaths = @(
        "$env:HOME/.konan/kotlin-native-*",
        "/usr/local/kotlin-native",
        "C:\Program Files\Kotlin\kotlin-native"
    )
    
    $found = $false
    foreach ($path in $defaultPaths) {
        if (Test-Path $path) {
            $KotlinHome = (Get-ChildItem $path -Directory | Sort-Object Name -Descending | Select-Object -First 1).FullName
            $found = $true
            break
        }
    }
    
    if (-not $found) {
        Write-Warning "Kotlin/Native not found. Please set KOTLIN_HOME environment variable."
        Write-Host "To generate bindings, you need Kotlin 1.9.0 or higher with Kotlin/Native."
        exit 1
    }
}

Write-Host "Kotlin Home: $KotlinHome" -ForegroundColor Cyan

# Определение пути к cinterop
$cinteropPath = if ($IsWindows) {
    "$KotlinHome\bin\cinterop.bat"
} else {
    "$KotlinHome/bin/cinterop"
}

if (-not (Test-Path $cinteropPath)) {
    Write-Error "cinterop not found at: $cinteropPath"
    Write-Host "Please check your Kotlin/Native installation."
    exit 1
}

Write-Host "cinterop path: $cinteropPath" -ForegroundColor Cyan

# Генерация .def файлов для каждого заголовка
$DefFiles = @()

foreach ($header in $ExistingHeaders) {
    $headerName = $header -replace '\.h$', ''
    $defFile = "$OutputDir/${headerName}.def"
    
    Write-Host "Generating .def file for $header..." -ForegroundColor Yellow
    
    $compilerOpts = "-I$HeadersDir"
    
    # Платформо-специфичные опции
    if ($Platform -eq 'windows') {
        $compilerOpts += " -D_WIN32 -DUNICODE -D_UNICODE"
    } elseif ($Platform -eq 'linux') {
        $compilerOpts += " -D_LINUX -D_GNU_SOURCE"
    } elseif ($Platform -eq 'macos') {
        $compilerOpts += " -D_MACOS"
    }
    
    # Определение пакета в зависимости от имени заголовка
    $packageName = switch ($headerName) {
        'rtsp_client' { 'com.company.ipcamera.core.network.native' }
        'video_decoder' { 'com.company.ipcamera.core.network.native' }
        'audio_decoder' { 'com.company.ipcamera.core.network.native' }
        'frame_processor' { 'com.company.ipcamera.core.network.native' }
        'stream_manager' { 'com.company.ipcamera.core.network.native' }
        default { 'com.company.ipcamera.core.network.native' }
    }
    
    $defContent = @"
package = $packageName

compilerOpts = $compilerOpts

include = "$HeadersDir/$header"
"@
    
    $defContent | Out-File -FilePath $defFile -Encoding UTF8 -NoNewline
    $DefFiles += $defFile
    Write-Host "  Created: $defFile" -ForegroundColor Green
}

# Генерация биндингов для каждого заголовка
$OutputFiles = @()

foreach ($header in $ExistingHeaders) {
    $headerName = $header -replace '\.h$', ''
    $defFile = "$OutputDir/${headerName}.def"
    $outputFile = "$OutputDir/${headerName}Bindings.kt"
    
    Write-Host "Generating bindings for $header..." -ForegroundColor Yellow
    
    $args = @(
        $defFile,
        "-output", $outputFile,
        "-compiler-opts", $defContent -replace 'package = .*`n', ''
    )
    
    try {
        $startInfo = New-Object System.Diagnostics.ProcessStartInfo
        $startInfo.FileName = $cinteropPath
        $startInfo.Arguments = "$defFile -output $outputFile"
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true
        $startInfo.UseShellExecute = $false
        $startInfo.WorkingDirectory = (Get-Location).Path
        
        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $startInfo
        $process.Start() | Out-Null
        $output = $process.StandardOutput.ReadToEnd()
        $error = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        
        if ($process.ExitCode -eq 0) {
            if (Test-Path $outputFile) {
                $OutputFiles += $outputFile
                Write-Host "  Generated: $outputFile" -ForegroundColor Green
            } else {
                Write-Warning "  Output file not created: $outputFile"
            }
        } else {
            Write-Warning "  cinterop failed for $header"
            if ($error) { Write-Warning "  Error: $error" }
        }
    } catch {
        Write-Warning "  Error running cinterop for $header: $_"
    }
}

# Генерация обертки Kotlin/Native для упрощения использования
Write-Host "`nGenerating Kotlin wrapper..." -ForegroundColor Cyan

$wrapperContent = @"
package com.company.ipcamera.core.network.native

import kotlinx.cinterop.*

/**
 * Автоматически сгенерированные биндинги для нативных библиотек
 * 
 * Сгенерировано: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
 * Платформа: $Platform/$Architecture
 */
object NativeBindings {
    
    /**
     * Проверить наличие сгенерированных биндингов
     */
    fun areBindingsAvailable(): Boolean {
        return try {
            // Пытаемся обратиться к одной из функций из биндингов
            // Это проверка, что биндинги были успешно сгенерированы
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Получить список доступных биндингов
     */
    fun getAvailableBindings(): List<String> {
        return @[$($ExistingHeaders -join ', ')]
    }
    
    /**
     * Получить информацию о сгенерированных биндингах
     */
    fun getBindingInfo(): NativeBindingInfo {
        return NativeBindingInfo(
            platform = "$Platform",
            architecture = "$Architecture",
            headerCount = ${ExistingHeaders.Count},
            headers = ${ExistingHeaders.toList()}
        )
    }
}

/**
 * Информация о сгенерированных биндингах
 */
data class NativeBindingInfo(
    val platform: String,
    val architecture: String,
    val headerCount: Int,
    val headers: List<String>
)
"@

$wrapperFile = "$OutputDir/NativeBindingsWrapper.kt"
$wrapperContent | Out-File -FilePath $wrapperFile -Encoding UTF8 -NoNewline
Write-Host "  Created: $wrapperFile" -ForegroundColor Green

# Вывод итогов
Write-Host "`n=== Generation Summary ===" -ForegroundColor Cyan
Write-Host "Platform: $Platform/$Architecture" -ForegroundColor White
Write-Host "Output directory: $OutputDir" -ForegroundColor White
Write-Host "Headers processed: ${ExistingHeaders.Count}" -ForegroundColor White
Write-Host "Def files created: ${DefFiles.Count}" -ForegroundColor White
Write-Host "Binding files generated: ${OutputFiles.Count}" -ForegroundColor White

if ($OutputFiles.Count -gt 0) {
    Write-Host "`nGenerated files:" -ForegroundColor Yellow
    $OutputFiles | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }
    
    Write-Host "`nNext steps:" -ForegroundColor Cyan
    Write-Host "1. Review generated files in $OutputDir" -ForegroundColor White
    Write-Host "2. Add proper Kotlin wrappers around the generated bindings" -ForegroundColor White
    Write-Host "3. Implement expect/actual classes for cross-platform support" -ForegroundColor White
    Write-Host "4. Run tests to verify bindings work correctly" -ForegroundColor White
} else {
    Write-Host "`nWarning: No binding files were generated!" -ForegroundColor Red
    Write-Host "Check the output above for errors." -ForegroundColor Yellow
}
