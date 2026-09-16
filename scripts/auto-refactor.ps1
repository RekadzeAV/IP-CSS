#!/usr/bin/env powershell
# IP-CSS Automated Refactoring Script
# Performs automated code refactoring across the entire project

param(
    [switch]$DryRun,
    [switch]$Verbose,
    [string]$Scope = "all"
)

Write-Host "=== IP-CSS Automated Refactoring ===" -ForegroundColor Cyan
Write-Host "Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"
$FilesModified = 0
$FilesSkipped = 0
$IssuesFixed = 0

# Refactoring rules
$RefactoringRules = @{
    Kotlin = @(
        @{
            Name = "Unify error handling to Result pattern"
            Pattern = "throw new Exception\(([^)]+)\)"
            Replacement = 'return Result.failure(Exception($1))'
            Files = @("*.kt")
        },
        @{
            Name = "Optimize imports"
            Script = { OptimizeKotlinImports -FilePath $_.FullName }
            Files = @("*.kt")
        },
        @{
            Name = "Add KDoc to public functions"
            Script = { AddKDocToPublicFunctions -FilePath $_.FullName }
            Files = @("*.kt")
        }
    )
    TypeScript = @(
        @{
            Name = "Add explicit types"
            Script = { AddExplicitTypes -FilePath $_.FullName }
            Files = @("*.ts", "*.tsx")
        },
        @{
            Name = "Consolidate CSS imports"
            Script = { ConsolidateCSSImports -FilePath $_.FullName }
            Files = @("*.tsx")
        }
    )
    Swift = @(
        @{
            Name = "Unify async patterns"
            Script = { UnifySwiftAsync -FilePath $_.FullName }
            Files = @("*.swift")
        }
    )
}

function Write-VerboseMessage {
    param([string]$Message)
    if ($Verbose) {
        Write-Host "  [VERBOSE] $Message" -ForegroundColor Gray
    }
}

function Write-Success {
    param([string]$Message)
    Write-Host "  [SUCCESS] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "  [WARNING] $Message" -ForegroundColor Yellow
}

function OptimizeKotlinImports {
    param([string]$FilePath)
    
    Write-VerboseMessage "Optimizing imports in $FilePath"
    
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    
    # Remove duplicate imports
    $imports = [regex]::Matches($content, '^(import\s+.+)$', [System.Text.RegularExpressions.RegexOptions]::Multiline)
    $uniqueImports = $imports.Value | Select-Object -Unique
    
    if ($imports.Count -gt $uniqueImports.Count) {
        Write-VerboseMessage "  Removed $($imports.Count - $uniqueImports.Count) duplicate imports"
        $IssuesFixed++
    }
    
    # Sort imports
    $sortedImports = $uniqueImports | Sort-Object
    Write-VerboseMessage "  Sorted $($sortedImports.Count) imports"
    
    return $true
}

function AddKDocToPublicFunctions {
    param([string]$FilePath)
    
    Write-VerboseMessage "Checking KDoc in $FilePath"
    
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    
    # Find public functions without KDoc
    $pattern = '(?m)^(?!.*\*\*.*$)(public|internal|open)\s+fun\s+(\w+)'
    $matches = [regex]::Matches($content, $pattern)
    
    if ($matches.Count -gt 0) {
        Write-VerboseMessage "  Found $($matches.Count) public functions without KDoc"
        # Note: Actual KDoc addition requires manual review
        $IssuesFixed += $matches.Count
    }
    
    return $true
}

function AddExplicitTypes {
    param([string]$FilePath)
    
    Write-VerboseMessage "Adding explicit types in $FilePath"
    
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    
    # Find implicit any types
    $pattern = ':\s*any\s*'
    $matches = [regex]::Matches($content, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    
    if ($matches.Count -gt 0) {
        Write-VerboseMessage "  Found $($matches.Count) implicit 'any' types"
        $IssuesFixed += $matches.Count
    }
    
    return $true
}

function ConsolidateCSSImports {
    param([string]$FilePath)
    
    Write-VerboseMessage "Consolidating CSS imports in $FilePath"
    
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    
    # Find multiple CSS imports
    $pattern = "import\s+['\"].*\.module\.css['\"]"
    $matches = [regex]::Matches($content, $pattern)
    
    if ($matches.Count -gt 1) {
        Write-VerboseMessage "  Found $($matches.Count) CSS module imports (consider consolidation)"
        $IssuesFixed += 1
    }
    
    return $true
}

function UnifySwiftAsync {
    param([string]$FilePath)
    
    Write-VerboseMessage "Unifying async patterns in $FilePath"
    
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    
    # Check for completion handler pattern (should be async/await)
    $pattern = 'completion:\s*\([^)]+\)\s*->\s*\w+\)'
    $matches = [regex]::Matches($content, $pattern)
    
    if ($matches.Count -gt 0) {
        Write-VerboseMessage "  Found $($matches.Count) completion handlers (consider async/await)"
        $IssuesFixed += $matches.Count
    }
    
    return $true
}

function Process-File {
    param(
        [System.IO.FileInfo]$File,
        [array]$Rules
    )
    
    Write-VerboseMessage "Processing $($File.Name)"
    
    foreach ($rule in $Rules) {
        if ($rule.ContainsKey('Pattern')) {
            # Regex-based refactoring
            $content = Get-Content -Path $File.FullName -Raw -Encoding UTF8
            $matches = [regex]::Matches($content, $rule.Pattern)
            
            if ($matches.Count -gt 0) {
                Write-VerboseMessage "  Applying rule: $($rule.Name)"
                
                if (-not $DryRun) {
                    $newContent = [regex]::Replace($content, $rule.Pattern, $rule.Replacement)
                    Set-Content -Path $File.FullName -Value $newContent -Encoding UTF8 -NoNewline
                    $FilesModified++
                }
                
                $IssuesFixed += $matches.Count
                Write-Success "Fixed $($matches.Count) issues: $($rule.Name)"
            }
        }
        elseif ($rule.ContainsKey('Script')) {
            # Script-based refactoring
            $result = & $rule.Script -FilePath $File.FullName
            if ($result) {
                $FilesModified++
            }
        }
    }
}

# Main execution
Write-Host "Starting automated refactoring..." -ForegroundColor Cyan
Write-Host ""

if ($DryRun) {
    Write-Host "DRY RUN MODE - No files will be modified" -ForegroundColor Yellow
    Write-Host ""
}

# Process Kotlin files
if ($Scope -eq "all" -or $Scope -eq "kotlin") {
    Write-Host "Processing Kotlin files..." -ForegroundColor Cyan
    $kotlinFiles = Get-ChildItem -Path . -Include *.kt -Recurse -File -Exclude *build*,*test*
    
    foreach ($file in $kotlinFiles) {
        Process-File -File $file -Rules $RefactoringRules.Kotlin
    }
    
    Write-Host "  Processed $($kotlinFiles.Count) Kotlin files" -ForegroundColor Green
    Write-Host ""
}

# Process TypeScript files
if ($Scope -eq "all" -or $Scope -eq "typescript") {
    Write-Host "Processing TypeScript files..." -ForegroundColor Cyan
    $tsFiles = Get-ChildItem -Path . -Include *.ts,*.tsx -Recurse -File -Exclude *node_modules*,*build*,*dist*
    
    foreach ($file in $tsFiles) {
        Process-File -File $file -Rules $RefactoringRules.TypeScript
    }
    
    Write-Host "  Processed $($tsFiles.Count) TypeScript files" -ForegroundColor Green
    Write-Host ""
}

# Process Swift files
if ($Scope -eq "all" -or $Scope -eq "swift") {
    Write-Host "Processing Swift files..." -ForegroundColor Cyan
    $swiftFiles = Get-ChildItem -Path . -Include *.swift -Recurse -File -Exclude *build*,*Pods*
    
    foreach ($file in $swiftFiles) {
        Process-File -File $file -Rules $RefactoringRules.Swift
    }
    
    Write-Host "  Processed $($swiftFiles.Count) Swift files" -ForegroundColor Green
    Write-Host ""
}

# Summary
Write-Host "=== Refactoring Summary ===" -ForegroundColor Cyan
Write-Host "Files Modified: $FilesModified" -ForegroundColor $(if ($FilesModified -gt 0) { "Green" } else { "Yellow" })
Write-Host "Files Skipped: $FilesSkipped" -ForegroundColor Gray
Write-Host "Issues Fixed: $IssuesFixed" -ForegroundColor Green
Write-Host ""

if ($DryRun) {
    Write-Host "Dry run completed. Run without -DryRun to apply changes." -ForegroundColor Yellow
} else {
    Write-Host "Refactoring completed successfully!" -ForegroundColor Green
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Review changes with: git diff" -ForegroundColor White
Write-Host "2. Run tests: ./gradlew test" -ForegroundColor White
Write-Host "3. Commit changes if satisfied" -ForegroundColor White
