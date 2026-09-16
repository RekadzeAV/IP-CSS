# Check Dependency Conflicts (PowerShell)
# Проверяет отсутствие конфликтов зависимостей перед установкой новых модулей/библиотек

param(
    [string]$Package,
    [switch]$List,
    [string]$Report
)

# Цвета
$Colors = @{
    OK = '[32m'
    WARN = '[33m'
    FAIL = '[31m'
    END = '[0m'
    BOLD = '[1m'
}

function Write-Color($Color, $Message) {
    Write-Host "$($Colors[$Color])$Message$($Colors.END)"
}

function Write-Bold($Message) {
    Write-Host "$($Colors.BOLD)$Message$($Colors.END)"
}

$Checks = @()
$Errors = @()
$Warnings = @()

Write-Bold "Проверка зависимостей проекта IP-CSS"
Write-Host ("=" * 60)
Write-Host ""

if ($Package) {
    # Проверка конкретного пакета
    
    Write-Bold "Проверка пакета: $Package"
    Write-Host ("-" * 60)
    
    $AllVersions = @()
    $AllLocations = @()
    
    # Проверка Python
    $pipShow = pip show $Package 2>&1
    if ($LASTEXITCODE -eq 0) {
        $version = ($pipShow | Where-Object { $_ -match "^Version:" }) -replace "Version:\s*", ""
        $location = ($pipShow | Where-Object { $_ -match "^Location:" }) -replace "Location:\s*", ""
        
        if ($version) {
            $AllVersions += $version
            $AllLocations += $location
        }
    }
    
    # Проверка npm global
    $npmGlobal = npm list -g $Package 2>&1
    if ($npmGlobal -match "$Package@(\d+\.\d+\.\d+)") {
        $version = $Matches[1]
        if ($version -notin $AllVersions) {
            $AllVersions += $version
        }
        if ("npm global" -notin $AllLocations) {
            $AllLocations += "npm global"
        }
    }
    
    # Проверка npm local
    $npmLocal = npm list $Package 2>&1
    if ($npmLocal -match "$Package@(\d+\.\d+\.\d+)") {
        $version = $Matches[1]
        if ($version -notin $AllVersions) {
            $AllVersions += $version
        }
        if ("npm local" -notin $AllLocations) {
            $AllLocations += "npm local"
        }
    }
    
    $Installed = $AllVersions.Count -gt 0
    $HasConflict = $AllVersions.Count -gt 1
    
    if (-not $Installed) {
        $Message = "✅ $Package не установлен (можно устанавливать)"
    } elseif ($HasConflict) {
        $Message = "⚠️ $Package имеет несколько версий: $($AllVersions -join ', ')"
        $Warnings += "Конфликт версий для $Package: $($AllVersions -join ', ')"
    } else {
        $Message = "ℹ️ $Package v$($AllVersions[0]) установлен в $($AllLocations[0])"
    }
    
    Write-Host $Message
    
    $Checks += [PSCustomObject]@{
        Package = $Package
        Installed = $Installed
        Versions = $AllVersions
        Locations = $AllLocations
        HasConflict = $HasConflict
        Message = $Message
    }
} else {
    # Проверка всех пакетов
    
    Write-Bold "Python пакеты"
    Write-Host ("-" * 60)
    
    $pipList = pip list --format=json 2>&1
    if ($LASTEXITCODE -eq 0) {
        $Packages = $pipList | ConvertFrom-Json
        $Count = 0
        foreach ($pkg in $Packages) {
            $Count++
            if ($Count -le 10) {
                Write-Host "ℹ️ $($pkg.name) v$($pkg.version)"
            }
            
            $Checks += [PSCustomObject]@{
                Package = $pkg.name
                Installed = $true
                Versions = @($pkg.version)
                Locations = @("pip")
                HasConflict = $false
                Message = "ℹ️ $($pkg.name) v$($pkg.version)"
            }
        }
        if ($Count -gt 10) {
            Write-Host "  ... и ещё $($Count - 10) пакетов"
        }
    }
    
    Write-Host ""
    Write-Bold "NPM пакеты (global)"
    Write-Host ("-" * 60)
    
    $npmGlobal = npm list -g --json 2>&1
    if ($LASTEXITCODE -eq 0) {
        try {
            $Data = $npmGlobal | ConvertFrom-Json
            if ($Data.PSObject.Properties.Name -contains "dependencies") {
                $Count = 0
                foreach ($key in $Data.dependencies.PSObject.Properties.Name) {
                    $Count++
                    $Version = $Data.dependencies.$key.version
                    if ($Count -le 10) {
                        Write-Host "ℹ️ $key v$Version"
                    }
                    
                    $Checks += [PSCustomObject]@{
                        Package = $key
                        Installed = $true
                        Versions = @($Version)
                        Locations = @("npm global")
                        HasConflict = $false
                        Message = "ℹ️ $key v$Version"
                    }
                }
                if ($Count -gt 10) {
                    Write-Host "  ... и ещё $($Count - 10) пакетов"
                }
            }
        } catch {
            # JSON parse error
        }
    }
}

# Поиск конфликтов
$Conflicts = $Checks | Where-Object { $_.HasConflict }
if ($Conflicts) {
    Write-Host ""
    Write-Color "FAIL" "Обнаружены конфликты:"
    foreach ($conflict in $Conflicts) {
        Write-Host "  $($conflict.Message)"
    }
}

Write-Host ""
Write-Host ("=" * 60)
Write-Host "Всего проверено пакетов: $($Checks.Count)"
Write-Host "Конфликтов: $($Conflicts.Count)"

if ($Conflicts) {
    Write-Color "FAIL" "⚠️ Найдены конфликты зависимостей!"
} else {
    Write-Color "OK" "✅ Конфликтов не найдено"
}

# Сохранение отчёта
if ($Report) {
    $reportDir = Split-Path $Report -Parent
    if ($reportDir -and -not (Test-Path $reportDir)) {
        New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
    }
    
    $reportData = [PSCustomObject]@{
        timestamp = Get-Date -Format "o"
        package_checked = $Package
        checks = $Checks
        errors = $Errors
        warnings = $Warnings
    }
    
    $reportData | ConvertTo-Json -Depth 10 | Out-File -FilePath $Report -Encoding UTF8
    Write-Host ""
    Write-Host "Отчёт сохранён: $Report"
}

# Возврат кода
$hasConflicts = $Conflicts.Count -gt 0
if ($hasConflicts) {
    exit 1
} else {
    exit 0
}
