#!/usr/bin/env powershell
# IP-CSS Automated Code Review Script
# Performs comprehensive code quality analysis

param(
    [switch]$GenerateReport,
    [string]$OutputPath = "docs/reports/CODE_REVIEW_ANALYSIS.md",
    [switch]$Verbose
)

Write-Host "=== IP-CSS Automated Code Review ===" -ForegroundColor Cyan
Write-Host "Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Continue"

# Analysis results
$AnalysisResults = @{
    TotalFiles = 0
    TotalLines = 0
    CodeSmells = 0
    Duplications = 0
    ComplexFunctions = 0
    MissingDocs = 0
    StyleIssues = 0
    SecurityIssues = 0
}

# Issue tracking
$Issues = @()

function Add-Issue {
    param(
        [string]$Type,
        [string]$Severity,
        [string]$File,
        [int]$Line,
        [string]$Message,
        [string]$Recommendation
    )
    
    $Issues += @{
        Type = $Type
        Severity = $Severity
        File = $File
        Line = $Line
        Message = $Message
        Recommendation = $Recommendation
    }
}

function Analyze-KotlinFile {
    param([string]$FilePath)
    
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    $lines = Get-Content -Path $FilePath -Encoding UTF8
    $lineCount = $lines.Count
    
    $AnalysisResults.TotalFiles++
    $AnalysisResults.TotalLines += $lineCount
    
    # Check for long functions (>50 lines)
    $functionPattern = '(?m)^\s*(public|private|internal|open)?\s*fun\s+\w+\s*\([^)]*\)\s*[^{]*\{'
    $functions = [regex]::Matches($content, $functionPattern)
    
    foreach ($match in $functions) {
        $startIndex = $match.Index
        $braceCount = 0
        $endIndex = $startIndex
        $inFunction = $false
        
        for ($i = $startIndex; $i -lt $content.Length; $i++) {
            if ($content[$i] -eq '{') {
                $braceCount++
                $inFunction = $true
            }
            elseif ($content[$i] -eq '}') {
                $braceCount--
                if ($inFunction -and $braceCount -eq 0) {
                    $endIndex = $i
                    break
                }
            }
        }
        
        if ($endIndex -gt $startIndex) {
            $functionBody = $content.Substring($startIndex, $endIndex - $startIndex + 1)
            $functionLines = ($functionBody -split "`n").Count
            
            if ($functionLines -gt 50) {
                $AnalysisResults.ComplexFunctions++
                $lineNum = ($content.Substring(0, $startIndex) -split "`n").Count
                
                Add-Issue -Type "Complexity" -Severity "Medium" -File $FilePath `
                    -Line $lineNum -Message "Function too long ($functionLines lines)" `
                    -Recommendation "Consider breaking into smaller functions"
            }
        }
    }
    
    # Check for missing KDoc
    $publicFuncPattern = '(?m)^(?!.*\*\*.*$)(public|internal|open)\s+fun\s+(\w+)'
    $missingDocs = [regex]::Matches($content, $publicFuncPattern)
    $AnalysisResults.MissingDocs += $missingDocs.Count
    
    # Check for TODO comments
    $todoPattern = '//\s*TODO[:\s]*(.+)'
    $todos = [regex]::Matches($content, $todoPattern)
    if ($todos.Count -gt 0) {
        Write-Verbose "  Found $($todos.Count) TODO comments"
    }
    
    # Check for empty catch blocks
    $emptyCatchPattern = '}\s*catch\s*\([^)]+\)\s*{\s*}'
    $emptyCatches = [regex]::Matches($content, $emptyCatchPattern)
    if ($emptyCatches.Count -gt 0) {
        $AnalysisResults.CodeSmells += $emptyCatches.Count
        Add-Issue -Type "CodeSmell" -Severity "High" -File $FilePath `
            -Line 0 -Message "Empty catch block detected" `
            -Recommendation "Add proper error handling or logging"
    }
    
    # Check for magic numbers
    $magicNumberPattern = '(?<!const val \w+\s*=\s*)(?<!//.*)(\b[2-9]\d{2,}\b)'
    $magicNumbers = [regex]::Matches($content, $magicNumberPattern)
    if ($magicNumbers.Count -gt 5) {
        $AnalysisResults.StyleIssues += 1
    }
}

function Analyze-TypeScriptFile {
    param([string]$FilePath)
    
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    $lines = Get-Content -Path $FilePath -Encoding UTF8
    $lineCount = $lines.Count
    
    $AnalysisResults.TotalFiles++
    $AnalysisResults.TotalLines += $lineCount
    
    # Check for any types
    $anyPattern = ':\s*any\b'
    $anyTypes = [regex]::Matches($content, $anyPattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if ($anyTypes.Count -gt 0) {
        $AnalysisResults.StyleIssues += $anyTypes.Count
        Add-Issue -Type "TypeScript" -Severity "Medium" -File $FilePath `
            -Line 0 -Message "Implicit or explicit 'any' type used $($anyTypes.Count) times" `
            -Recommendation "Use explicit types for better type safety"
    }
    
    # Check for console.log
    $consolePattern = 'console\.(log|warn|error|debug)\s*\('
    $consoleLogs = [regex]::Matches($content, $consolePattern)
    if ($consoleLogs.Count -gt 3) {
        $AnalysisResults.CodeSmells += 1
        Add-Issue -Type "CodeSmell" -Severity "Low" -File $FilePath `
            -Line 0 -Message "Multiple console.log statements ($($consoleLogs.Count))" `
            -Recommendation "Remove debug logs before production"
    }
    
    # Check for long functions
    $functionPattern = '(?m)(async\s+)?(function|const|let)\s+\w+\s*=\s*(async\s+)?\([^)]*\)\s*=>'
    $functions = [regex]::Matches($content, $functionPattern)
    
    foreach ($match in $functions) {
        $startIndex = $match.Index
        $functionLines = 0
        $braceCount = 0
        
        for ($i = $startIndex; $i -lt $content.Length; $i++) {
            if ($content[$i] -eq '{') {
                $braceCount++
            }
            elseif ($content[$i] -eq '}') {
                $braceCount--
                if ($braceCount -eq 0) {
                    $functionLines = ($content.Substring($startIndex, $i - $startIndex + 1) -split "`n").Count
                    break
                }
            }
        }
        
        if ($functionLines -gt 50) {
            $AnalysisResults.ComplexFunctions++
            $lineNum = ($content.Substring(0, $startIndex) -split "`n").Count
            
            Add-Issue -Type "Complexity" -Severity "Medium" -File $FilePath `
                -Line $lineNum -Message "Function too long ($functionLines lines)" `
                -Recommendation "Consider breaking into smaller functions"
        }
    }
}

function Analyze-SwiftFile {
    param([string]$FilePath)
    
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    $lines = Get-Content -Path $FilePath -Encoding UTF8
    $lineCount = $lines.Count
    
    $AnalysisResults.TotalFiles++
    $AnalysisResults.TotalLines += $lineCount
    
    # Check for force unwrap (!)
    $forceUnwrapPattern = '!\s*(?!\s*=)'
    $forceUnwraps = [regex]::Matches($content, $forceUnwrapPattern)
    if ($forceUnwraps.Count -gt 5) {
        $AnalysisResults.CodeSmells += 1
        Add-Issue -Type "Swift" -Severity "High" -File $FilePath `
            -Line 0 -Message "Multiple force unwraps ($($forceUnwraps.Count))" `
            -Recommendation "Use optional binding or guard statements"
    }
    
    # Check for completion handlers (should use async/await)
    $completionPattern = 'completion:\s*\([^)]+\)\s*->\s*\w+\)'
    $completions = [regex]::Matches($content, $completionPattern)
    if ($completions.Count -gt 0) {
        $AnalysisResults.StyleIssues += $completions.Count
    }
}

function Generate-Report {
    param([string]$OutputPath)
    
    $report = @"
# Automated Code Review Report

**Date:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  
**Tool:** IP-CSS Code Review Script  
**Scope:** Entire Project

---

## 📊 Summary

| Metric | Value |
|--------|-------|
| **Total Files** | $($AnalysisResults.TotalFiles) |
| **Total Lines** | $($AnalysisResults.TotalLines) |
| **Code Smells** | $($AnalysisResults.CodeSmells) |
| **Complex Functions** | $($AnalysisResults.ComplexFunctions) |
| **Missing Documentation** | $($AnalysisResults.MissingDocs) |
| **Style Issues** | $($AnalysisResults.StyleIssues) |
| **Security Issues** | $($AnalysisResults.SecurityIssues) |

---

## 📈 Quality Score

``" -replace '``\"', '```'
    
    $totalIssues = $AnalysisResults.CodeSmells + $AnalysisResults.ComplexFunctions + $AnalysisResults.MissingDocs + $AnalysisResults.StyleIssues
    $qualityScore = [Math]::Max(0, 100 - ($totalIssues / [Math]::Max(1, $AnalysisResults.TotalFiles) * 10))
    
    $report += @"
Quality Score: $([Math]::Round($qualityScore, 1))/100
"@
    
    if ($qualityScore -ge 90) {
        $report += " - Excellent ✅"
    } elseif ($qualityScore -ge 75) {
        $report += " - Good 🟢"
    } elseif ($qualityScore -ge 60) {
        $report += " - Fair 🟡"
    } else {
        $report += " - Needs Improvement 🔴"
    }
    
    $report += @"


---

## 🚨 Issues by Severity

### Critical ($($Issues.Where({ $_.Severity -eq 'Critical' }).Count))
"@
    
    $criticalIssues = $Issues.Where({ $_.Severity -eq 'Critical' })
    if ($criticalIssues.Count -eq 0) {
        $report += "`n`nNone ✅"
    } else {
        foreach ($issue in $criticalIssues) {
            $report += @"

- **$($issue.File):$($issue.Line)** - $($issue.Message)
  - Recommendation: $($issue.Recommendation)
"@
        }
    }
    
    $report += @"


### High ($($Issues.Where({ $_.Severity -eq 'High' }).Count))
"@
    
    $highIssues = $Issues.Where({ $_.Severity -eq 'High' })
    if ($highIssues.Count -eq 0) {
        $report += "`n`nNone ✅"
    } else {
        foreach ($issue in $highIssues) {
            $report += @"

- **$($issue.File):$($issue.Line)** - $($issue.Message)
  - Recommendation: $($issue.Recommendation)
"@
        }
    }
    
    $report += @"


### Medium ($($Issues.Where({ $_.Severity -eq 'Medium' }).Count))
"@
    
    $mediumIssues = $Issues.Where({ $_.Severity -eq 'Medium' })
    if ($mediumIssues.Count -eq 0) {
        $report += "`n`nNone ✅"
    } else {
        $report += "`n`n$($mediumIssues.Count) issues found (see detailed report)"
    }
    
    $report += @"


### Low ($($Issues.Where({ $_.Severity -eq 'Low' }).Count))
"@
    
    $lowIssues = $Issues.Where({ $_.Severity -eq 'Low' })
    if ($lowIssues.Count -eq 0) {
        $report += "`n`nNone ✅"
    } else {
        $report += "`n`n$($lowIssues.Count) issues found (see detailed report)"
    }
    
    $report += @"


---

## 📁 Files by Language

| Language | Files | Lines | Avg Lines/File |
|----------|-------|-------|----------------|
"@
    
    # This would require tracking per-language stats
    $report += @"
| Kotlin | - | - | - |
| TypeScript | - | - | - |
| Swift | - | - | - |
| Total | $($AnalysisResults.TotalFiles) | $($AnalysisResults.TotalLines) | $([Math]::Round($AnalysisResults.TotalLines / [Math]::Max(1, $AnalysisResults.TotalFiles), 1)) |

---

## ✅ Recommendations

### Immediate Actions
1. Fix all Critical and High severity issues
2. Review complex functions (>50 lines)
3. Add missing documentation

### Short-term Improvements
1. Address code smells
2. Standardize code style
3. Remove debug code (console.log, etc.)

### Long-term Goals
1. Maintain quality score >90
2. Reduce code duplication
3. Improve test coverage

---

**Generated:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  
**Tool Version:** 1.0.0
"@
    
    $report | Out-File -FilePath $OutputPath -Encoding UTF8
    Write-Host "Report saved to: $OutputPath" -ForegroundColor Green
}

# Main execution
Write-Host "Starting code review analysis..." -ForegroundColor Cyan
Write-Host ""

# Analyze Kotlin files
Write-Host "Analyzing Kotlin files..." -ForegroundColor Cyan
$kotlinFiles = Get-ChildItem -Path . -Include *.kt -Recurse -File -Exclude *build*,*test* | Select-Object -First 100
foreach ($file in $kotlinFiles) {
    if ($Verbose) {
        Write-Verbose "  Analyzing $($file.Name)"
    }
    Analyze-KotlinFile -FilePath $file.FullName
}
Write-Host "  Analyzed $($kotlinFiles.Count) Kotlin files" -ForegroundColor Green

# Analyze TypeScript files
Write-Host "Analyzing TypeScript files..." -ForegroundColor Cyan
$tsFiles = Get-ChildItem -Path . -Include *.ts,*.tsx -Recurse -File -Exclude *node_modules*,*build*,*dist* | Select-Object -First 100
foreach ($file in $tsFiles) {
    if ($Verbose) {
        Write-Verbose "  Analyzing $($file.Name)"
    }
    Analyze-TypeScriptFile -FilePath $file.FullName
}
Write-Host "  Analyzed $($tsFiles.Count) TypeScript files" -ForegroundColor Green

# Analyze Swift files
Write-Host "Analyzing Swift files..." -ForegroundColor Cyan
$swiftFiles = Get-ChildItem -Path . -Include *.swift -Recurse -File -Exclude *build*,*Pods* | Select-Object -First 50
foreach ($file in $swiftFiles) {
    if ($Verbose) {
        Write-Verbose "  Analyzing $($file.Name)"
    }
    Analyze-SwiftFile -FilePath $file.FullName
}
Write-Host "  Analyzed $($swiftFiles.Count) Swift files" -ForegroundColor Green

# Summary
Write-Host ""
Write-Host "=== Code Review Summary ===" -ForegroundColor Cyan
Write-Host "Total Files Analyzed: $($AnalysisResults.TotalFiles)" -ForegroundColor White
Write-Host "Total Lines: $($AnalysisResults.TotalLines)" -ForegroundColor White
Write-Host "Code Smells: $($AnalysisResults.CodeSmells)" -ForegroundColor $(if ($AnalysisResults.CodeSmells -gt 0) { "Yellow" } else { "Green" })
Write-Host "Complex Functions: $($AnalysisResults.ComplexFunctions)" -ForegroundColor $(if ($AnalysisResults.ComplexFunctions -gt 0) { "Yellow" } else { "Green" })
Write-Host "Missing Docs: $($AnalysisResults.MissingDocs)" -ForegroundColor $(if ($AnalysisResults.MissingDocs -gt 0) { "Yellow" } else { "Green" })
Write-Host "Style Issues: $($AnalysisResults.StyleIssues)" -ForegroundColor $(if ($AnalysisResults.StyleIssues -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# Generate report
if ($GenerateReport) {
    Write-Host "Generating detailed report..." -ForegroundColor Cyan
    Generate-Report -OutputPath $OutputPath
}

Write-Host "Code review completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Issues found: $($Issues.Count)" -ForegroundColor $(if ($Issues.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host "Critical: $($Issues.Where({ $_.Severity -eq 'Critical' }).Count)" -ForegroundColor Red
Write-Host "High: $($Issues.Where({ $_.Severity -eq 'High' }).Count)" -ForegroundColor Orange
Write-Host "Medium: $($Issues.Where({ $_.Severity -eq 'Medium' }).Count)" -ForegroundColor Yellow
Write-Host "Low: $($Issues.Where({ $_.Severity -eq 'Low' }).Count)" -ForegroundColor Gray
