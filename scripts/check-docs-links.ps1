# Script to check markdown links in documentation
# Usage: .\scripts\check-docs-links.ps1

$ErrorActionPreference = "Stop"

$docsPath = "docs"
$rootFiles = @("README.md", "DOCUMENTATION_INDEX.md", "CONTRIBUTING.md", "PROJECT_STRUCTURE.md")
$brokenLinks = @()

Write-Host "Checking markdown links..." -ForegroundColor Cyan

# Function to extract markdown links
function Get-MarkdownLinks {
    param($filePath)
    $content = Get-Content -Path $filePath -Raw
    $links = [regex]::Matches($content, '\[([^\]]+)\]\(([^)]+)\)') | ForEach-Object {
        $text = $_.Groups[1].Value
        $url = $_.Groups[2].Value
        [PSCustomObject]@{
            Text = $text
            Url = $url
        }
    }
    return $links
}

# Function to check if a link exists
function Test-LinkExists {
    param($link, $sourceFile)
    
    $sourceDir = Split-Path -Parent $sourceFile
    
    # Skip external links
    if ($link -match '^https?://|^mailto:') {
        return $true
    }
    
    # Skip anchor links
    if ($link -match '^#') {
        return $true
    }
    
    # Resolve relative path
    if ([string]::IsNullOrWhiteSpace($sourceDir)) {
        $resolvedPath = [System.IO.Path]::GetFullPath($link)
    } else {
        $resolvedPath = Join-Path $sourceDir $link
        $resolvedPath = [System.IO.Path]::GetFullPath($resolvedPath)
    }
    
    # Check if file exists
    if (-not (Test-Path $resolvedPath)) {
        return $false
    }
    
    return $true
}

# Check root files
foreach ($file in $rootFiles) {
    if (Test-Path $file) {
        Write-Host "Checking $file..." -ForegroundColor Gray
        $links = Get-MarkdownLinks -filePath $file
        
        foreach ($link in $links) {
            if (-not (Test-LinkExists -link $link.Url -sourceFile $file)) {
                $brokenLinks += [PSCustomObject]@{
                    Source = $file
                    Link = $link.Url
                    Text = $link.Text
                }
            }
        }
    }
}

# Check docs folder
Write-Host "Checking docs/ folder..." -ForegroundColor Cyan
$mdFiles = Get-ChildItem -Path $docsPath -Filter "*.md" -Recurse | Select-Object -First 100

foreach ($mdFile in $mdFiles) {
    Write-Host "Checking $($mdFile.FullName)..." -ForegroundColor Gray
    $links = Get-MarkdownLinks -filePath $mdFile.FullName
    
    foreach ($link in $links) {
        if (-not (Test-LinkExists -link $link.Url -sourceFile $mdFile.FullName)) {
            $brokenLinks += [PSCustomObject]@{
                Source = $mdFile.FullName
                Link = $link.Url
                Text = $link.Text
            }
        }
    }
}

# Report results
Write-Host "`n" -NoNewline
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "LINK CHECKING RESULTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($brokenLinks.Count -eq 0) {
    Write-Host "✅ No broken links found!" -ForegroundColor Green
} else {
    Write-Host "❌ Found $($brokenLinks.Count) broken link(s):" -ForegroundColor Red
    Write-Host ""
    $brokenLinks | Format-Table -AutoSize
}

Write-Host ""
Write-Host "Total files checked: $($rootFiles.Count + $mdFiles.Count)" -ForegroundColor Gray
