<#
.SYNOPSIS
    Backup Manager with direction selection (Disk <-> NAS).
.DESCRIPTION
    This script provides interactive menu to select backup direction:
    1. Copy from Disk to NAS (\\rav2004\AI-prodgect-2)
    2. Copy from NAS to Disk
    3. Sync changes (bidirectional)
    4. Copy from Disk to NAS-2 (\\rav2004\AI-prodgect)
.PARAMETER SourcePath
    Source directory path
.PARAMETER DestPath
    Destination directory path
.EXAMPLE
    .\backup-manager.ps1 -SourcePath "E:\GitHub-Ai" -DestPath "\\rav2004\AI-prodgect-2"
#>

param(
    [Parameter(Mandatory=$false)]
    [string]$SourcePath,
    [Parameter(Mandatory=$false)]
    [string]$DestPath
)

# Configuration
$NAS_HOSTS = @{
    "NAS-Primary" = "rav2004"
}

$DEFAULT_PATHS = @{
    "Disk"  = "E:\GitHub-Ai"
    "NAS"   = "\\rav2004\AI-prodgect-2"
    "NAS2"  = "\\rav2004\AI-prodgect"
}

# Colors
$colors = @{
    Title = "Cyan"
    Menu = "Yellow"
    Success = "Green"
    Warning = "Yellow"
    Error = "Red"
    Info = "White"
    Highlight = "Magenta"
}

function Show-Header {
    Clear-Host
    Write-Host "==========================================" -ForegroundColor $colors.Title
    Write-Host "     Backup Manager - Disk <-> NAS        " -ForegroundColor $colors.Title
    Write-Host "==========================================" -ForegroundColor $colors.Title
    Write-Host ""
    Write-Host "  NAS-1: \\rav2004\AI-prodgect-2" -ForegroundColor $colors.Info
    Write-Host "  NAS-2: \\rav2004\AI-prodgect" -ForegroundColor $colors.Info
    Write-Host "  Disk:  E:\GitHub-Ai" -ForegroundColor $colors.Info
    Write-Host ""
}

function Show-Menu {
    Write-Host "Select backup direction:" -ForegroundColor $colors.Menu
    Write-Host ""
    Write-Host "  [1] Disk -> NAS-1 (Backup)" -ForegroundColor $colors.Info
    Write-Host "  [2] NAS-1 -> Disk (Restore)" -ForegroundColor $colors.Info
    Write-Host "  [3] Sync (Bidirectional)" -ForegroundColor $colors.Highlight
    Write-Host "  [4] Disk -> NAS-2 (Backup)" -ForegroundColor $colors.Info
    Write-Host "  [5] Exit" -ForegroundColor $colors.Info
    Write-Host ""
}

function Get-UserChoice {
    $choice = Read-Host "Enter your choice (1-5)"
    return $choice
}

function Get-Paths {
    param(
        [string]$Direction
    )
    
    Write-Host ""
    Write-Host "Configuring paths for: $Direction" -ForegroundColor $colors.Info
    Write-Host ""
    
    # Use provided parameters or ask for paths
    if ($SourcePath -and $DestPath) {
        return $SourcePath, $DestPath
    }
    
    # Default paths
    switch ($Direction) {
        "DiskToNAS" {
            $defaultSource = $DEFAULT_PATHS["Disk"]
            $defaultDest = $DEFAULT_PATHS["NAS"]
        }
        "DiskToNAS2" {
            $defaultSource = $DEFAULT_PATHS["Disk"]
            $defaultDest = $DEFAULT_PATHS["NAS2"]
        }
        "NAS" {
            $defaultSource = $DEFAULT_PATHS["NAS"]
            $defaultDest = $DEFAULT_PATHS["Disk"]
        }
        "Sync" {
            $defaultSource = $DEFAULT_PATHS["Disk"]
            $defaultDest = $DEFAULT_PATHS["NAS"]
        }
    }
    
    Write-Host "Default paths:" -ForegroundColor $colors.Info
    Write-Host "  Source: $defaultSource" -ForegroundColor $colors.Info
    Write-Host "  Destination: $defaultDest" -ForegroundColor $colors.Info
    Write-Host ""
    
    $useDefaults = Read-Host "Use default paths? (Y/N)"
    
    if ($useDefaults -ne "N") {
        return $defaultSource, $defaultDest
    }
    
    # Custom paths
    $customSource = Read-Host "Enter source path"
    $customDest = Read-Host "Enter destination path"
    
    return $customSource, $customDest
}

function Test-ConnectionToNAS {
    param(
        [string]$NAS_Host
    )
    
    Write-Host "Testing connection to NAS ($NAS_Host)..." -ForegroundColor $colors.Info
    
    $ping = Test-Connection -ComputerName $NAS_Host -Count 2 -Quiet -ErrorAction SilentlyContinue
    if ($ping) {
        Write-Host "  NAS is reachable" -ForegroundColor $colors.Success
        return $true
    } else {
        Write-Host "  Cannot reach NAS" -ForegroundColor $colors.Error
        return $false
    }
}

function Copy-DiskToNAS {
    param(
        [string]$Source,
        [string]$Destination,
        [string]$Label = "NAS-1"
    )
    
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor $colors.Title
    Write-Host "  BACKUP: Disk -> $Label" -ForegroundColor $colors.Title
    Write-Host "==========================================" -ForegroundColor $colors.Title
    Write-Host ""
    
    # Validate source
    if (-not (Test-Path $Source)) {
        Write-Host "ERROR: Source path does not exist: $Source" -ForegroundColor $colors.Error
        return $false
    }
    
    # Ensure destination exists
    if (-not (Test-Path $Destination)) {
        Write-Host "Creating destination directory: $Destination" -ForegroundColor $colors.Warning
        New-Item -ItemType Directory -Path $Destination -Force | Out-Null
        Write-Host "  Destination created" -ForegroundColor $colors.Success
    }
    
    # Show summary
    Write-Host ""
    Write-Host "Backup Summary:" -ForegroundColor $colors.Info
    Write-Host "  From: $Source" -ForegroundColor $colors.Info
    Write-Host "  To:   $Destination" -ForegroundColor $colors.Info
    Write-Host ""
    
    $confirm = Read-Host "Proceed with backup? (Y/N)"
    if ($confirm -ne "Y") {
        Write-Host "Operation cancelled" -ForegroundColor $colors.Warning
        return $false
    }
    
    # Perform copy with progress
    Write-Host ""
    Write-Host "Copying files..." -ForegroundColor $colors.Info
    
    $robocopyArgs = @(
        $Source,
        $Destination,
        "/E",
        "/Z",
        "/R:3",
        "/W:5",
        "/TBD",
        "/NP",
        "/NDL",
        "/NJH",
        "/NJS"
    )
    
    $process = Start-Process -FilePath "robocopy" -ArgumentList $robocopyArgs -Wait -PassThru -NoNewWindow
    
    if ($process.ExitCode -le 7) {
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor $colors.Success
        Write-Host "  Backup completed successfully!" -ForegroundColor $colors.Success
        Write-Host "==========================================" -ForegroundColor $colors.Success
        return $true
    } else {
        Write-Host ""
        Write-Host "Backup completed with warnings (Exit code: $($process.ExitCode))" -ForegroundColor $colors.Warning
        return $true
    }
}

function Copy-NASDisk {
    param(
        [string]$Source,
        [string]$Destination,
        [string]$Label = "NAS-1"
    )
    
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor $colors.Title
    Write-Host "  RESTORE: $Label -> Disk" -ForegroundColor $colors.Title
    Write-Host "==========================================" -ForegroundColor $colors.Title
    Write-Host ""
    
    # Validate source
    if (-not (Test-Path $Source)) {
        Write-Host "ERROR: Source path does not exist: $Source" -ForegroundColor $colors.Error
        Write-Host "Make sure NAS is accessible and path is correct." -ForegroundColor $colors.Warning
        return $false
    }
    
    # Ensure destination exists
    if (-not (Test-Path $Destination)) {
        Write-Host "Creating destination directory: $Destination" -ForegroundColor $colors.Warning
        New-Item -ItemType Directory -Path $Destination -Force | Out-Null
        Write-Host "  Destination created" -ForegroundColor $colors.Success
    }
    
    # Show summary
    Write-Host ""
    Write-Host "Restore Summary:" -ForegroundColor $colors.Info
    Write-Host "  From: $Source" -ForegroundColor $colors.Info
    Write-Host "  To:   $Destination" -ForegroundColor $colors.Info
    Write-Host ""
    
    $confirm = Read-Host "Proceed with restore? (Y/N)"
    if ($confirm -ne "Y") {
        Write-Host "Operation cancelled" -ForegroundColor $colors.Warning
        return $false
    }
    
    # Perform copy with progress
    Write-Host ""
    Write-Host "Copying files..." -ForegroundColor $colors.Info
    
    $robocopyArgs = @(
        $Source,
        $Destination,
        "/E",
        "/Z",
        "/R:3",
        "/W:5",
        "/TBD",
        "/NP",
        "/NDL",
        "/NJH",
        "/NJS"
    )
    
    $process = Start-Process -FilePath "robocopy" -ArgumentList $robocopyArgs -Wait -PassThru -NoNewWindow
    
    if ($process.ExitCode -le 7) {
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor $colors.Success
        Write-Host "  Restore completed successfully!" -ForegroundColor $colors.Success
        Write-Host "==========================================" -ForegroundColor $colors.Success
        return $true
    } else {
        Write-Host ""
        Write-Host "Restore completed with warnings (Exit code: $($process.ExitCode))" -ForegroundColor $colors.Warning
        return $true
    }
}

function Sync-Changes {
    param(
        [string]$Path1,
        [string]$Path2
    )
    
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor $colors.Title
    Write-Host "  SYNC: Bidirectional Sync" -ForegroundColor $colors.Title
    Write-Host "==========================================" -ForegroundColor $colors.Title
    Write-Host ""
    
    # Validate paths
    if (-not (Test-Path $Path1)) {
        Write-Host "ERROR: Path 1 does not exist: $Path1" -ForegroundColor $colors.Error
        return $false
    }
    
    if (-not (Test-Path $Path2)) {
        Write-Host "ERROR: Path 2 does not exist: $Path2" -ForegroundColor $colors.Error
        return $false
    }
    
    # Show summary
    Write-Host ""
    Write-Host "Sync Summary:" -ForegroundColor $colors.Info
    Write-Host "  Path 1: $Path1" -ForegroundColor $colors.Info
    Write-Host "  Path 2: $Path2" -ForegroundColor $colors.Info
    Write-Host ""
    Write-Host "This will synchronize both locations with the newest files." -ForegroundColor $colors.Warning
    Write-Host ""
    
    $confirm = Read-Host "Proceed with sync? (Y/N)"
    if ($confirm -ne "Y") {
        Write-Host "Operation cancelled" -ForegroundColor $colors.Warning
        return $false
    }
    
    # Perform sync
    Write-Host ""
    Write-Host "Syncing files..." -ForegroundColor $colors.Info
    
    # Sync Path1 -> Path2
    Write-Host ""
    Write-Host "Phase 1: Syncing Path 1 -> Path 2" -ForegroundColor $colors.Info
    
    $robocopyArgs1 = @(
        $Path1,
        $Path2,
        "/E",
        "/Z",
        "/R:3",
        "/W:5",
        "/TBD",
        "/NP",
        "/NDL",
        "/NJH",
        "/NJS",
        "/XO"
    )
    
    $process1 = Start-Process -FilePath "robocopy" -ArgumentList $robocopyArgs1 -Wait -PassThru -NoNewWindow
    
    # Sync Path2 -> Path1
    Write-Host ""
    Write-Host "Phase 2: Syncing Path 2 -> Path 1" -ForegroundColor $colors.Info
    
    $robocopyArgs2 = @(
        $Path2,
        $Path1,
        "/E",
        "/Z",
        "/R:3",
        "/W:5",
        "/TBD",
        "/NP",
        "/NDL",
        "/NJH",
        "/NJS",
        "/XO"
    )
    
    $process2 = Start-Process -FilePath "robocopy" -ArgumentList $robocopyArgs2 -Wait -PassThru -NoNewWindow
    
    if ($process1.ExitCode -le 7 -and $process2.ExitCode -le 7) {
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor $colors.Success
        Write-Host "  Sync completed successfully!" -ForegroundColor $colors.Success
        Write-Host "==========================================" -ForegroundColor $colors.Success
        return $true
    } else {
        Write-Host ""
        Write-Host "Sync completed with warnings" -ForegroundColor $colors.Warning
        return $true
    }
}

function Show-CompletionMessage {
    param(
        [bool]$Success,
        [string]$Operation
    )
    
    Write-Host ""
    if ($Success) {
        Write-Host "$Operation completed successfully!" -ForegroundColor $colors.Success
    } else {
        Write-Host "$Operation failed!" -ForegroundColor $colors.Error
    }
    Write-Host ""
    Read-Host "Press Enter to continue..."
}

# Main execution loop
do {
    Show-Header
    Show-Menu
    $choice = Get-UserChoice
    
    switch ($choice) {
        "1" {
            # Disk to NAS-1
            $source, $dest = Get-Paths -Direction "DiskToNAS"
            $success = Copy-DiskToNAS -Source $source -Destination $dest -Label "NAS-1"
            Show-CompletionMessage -Success $success -Operation "Backup (Disk to NAS-1)"
        }
        "2" {
            # NAS-1 to Disk
            $source, $dest = Get-Paths -Direction "NAS"
            $success = Copy-NASDisk -Source $source -Destination $dest -Label "NAS-1"
            Show-CompletionMessage -Success $success -Operation "Restore (NAS-1 to Disk)"
        }
        "3" {
            # Sync
            $path1, $path2 = Get-Paths -Direction "Sync"
            $success = Sync-Changes -Path1 $path1 -Path2 $path2
            Show-CompletionMessage -Success $success -Operation "Sync"
        }
        "4" {
            # Disk to NAS-2
            $source, $dest = Get-Paths -Direction "DiskToNAS2"
            $success = Copy-DiskToNAS -Source $source -Destination $dest -Label "NAS-2"
            Show-CompletionMessage -Success $success -Operation "Backup (Disk to NAS-2)"
        }
        "5" {
            Write-Host ""
            Write-Host "Exiting Backup Manager..." -ForegroundColor $colors.Info
            exit 0
        }
        default {
            Write-Host ""
            Write-Host "Invalid choice. Please try again." -ForegroundColor $colors.Error
            Start-Sleep -Seconds 2
        }
    }
} while ($true)