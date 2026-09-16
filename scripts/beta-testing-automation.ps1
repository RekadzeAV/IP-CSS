# Beta Testing Automation Script

**Назначение:** Автоматизация сбора метрик и генерации отчётов beta-тестирования

```powershell
<#
.SYNOPSIS
    IP-CSS Beta Testing Automation Script

.DESCRIPTION
    Automates beta testing metrics collection, survey distribution,
    and report generation for IP-CSS v0.3.0-beta

.VERSION
    1.0.0

.AUTHOR
    NLP-Core-Team

.LAST_UPDATED
    28 January 2026
#>

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet('collect', 'survey', 'report', 'dashboard', 'all')]
    [string]$Action = 'all',
    
    [Parameter(Mandatory=$false)]
    [int]$Week = 1,
    
    [Parameter(Mandatory=$false)]
    [string]$OutputPath = ".\beta-testing-output",
    
    [Parameter(Mandatory=$false)]
    [switch]$Verbose
)

# ============================================================================
# Configuration
# ============================================================================

$Config = @{
    ProjectName = "IP-CSS"
    Version = "0.3.0-beta"
    Phase = 4
    BetaDurationWeeks = 4
    TargetTesters = 15
    MinActiveTesters = 10
    SurveyDeadlineDay = "Friday"
    SurveyDeadlineTime = "23:59"
    Timezone = "UTC"
    EmailFrom = "beta-testing@ip-css.com"
    EmailTo = @()  # Populate with beta tester emails
    SMTPServer = "smtp.ip-css.com"
    SMTPPort = 587
    GitHubRepo = "nlp-core-team/ip-css"
    SentryOrg = "nlp-core-team"
    SentryProject = "ip-css"
    GrafanaURL = "http://grafana.ip-css.local"
}

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Log {
    param(
        [string]$Message,
        [ValidateSet('Info', 'Warning', 'Error', 'Success')]
        [string]$Level = 'Info'
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $color = switch($Level) {
        'Info' { 'White' }
        'Warning' { 'Yellow' }
        'Error' { 'Red' }
        'Success' { 'Green' }
    }
    
    Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor $color
}

function Test-Prerequisites {
    Write-Log "Checking prerequisites..." -Level Info
    
    $required = @('git', 'gh')  # GitHub CLI
    $missing = @()
    
    foreach ($cmd in $required) {
        if (!(Get-Command $cmd -ErrorAction SilentlyContinue)) {
            $missing += $cmd
        }
    }
    
    if ($missing.Count -gt 0) {
        Write-Log "Missing prerequisites: $($missing -join ', ')" -Level Error
        return $false
    }
    
    Write-Log "All prerequisites met" -Level Success
    return $true
}

function Get-GitHubIssues {
    param(
        [string]$Label = "beta-bug",
        [string]$State = "open"
    )
    
    Write-Log "Fetching GitHub issues (Label: $Label, State: $State)..." -Level Info
    
    try {
        $issues = gh issue list --repo $Config.GitHubRepo --label $Label --state $State --json number,title,labels,createdAt,state --limit 100 | ConvertFrom-Json
        Write-Log "Found $($issues.Count) issues" -Level Success
        return $issues
    }
    catch {
        Write-Log "Failed to fetch GitHub issues: $_" -Level Error
        return @()
    }
}

function Get-SentryErrors {
    param(
        [string]$Project = "ip-css",
        [int]$Days = 7
    )
    
    Write-Log "Fetching Sentry errors (Project: $Project, Days: $Days)..." -Level Info
    
    # Note: Requires Sentry CLI authentication
    # This is a placeholder - implement based on your Sentry setup
    
    return @()
}

function Send-SurveyEmail {
    param(
        [string[]]$Recipients,
        [int]$Week
    )
    
    $subject = "[IP-CSS Beta] Week $Week Feedback Survey"
    $body = @"
Hello Beta Tester,

Thank you for participating in the IP-CSS v0.3.0-beta testing program!

Week $Week has ended. Please take 10-15 minutes to complete the feedback survey:

📝 Survey Link: https://forms.google.com/ip-css-beta-week-$Week

📅 Deadline: $($Config.SurveyDeadlineDay), $($Config.SurveyDeadlineTime) $($Config.Timezone)

Your feedback is crucial for making IP-CSS production-ready.

What to include:
- Features tested
- Bugs encountered
- Performance observations
- Usability feedback
- Suggestions for improvement

If you encountered any critical issues, please report them immediately:
- GitHub Issues: https://github.com/$($Config.GitHubRepo)/issues
- Emergency Email: beta-emergency@ip-css.com

Thank you for your contribution!

Best regards,
IP-CSS Beta Testing Team

---
IP-CSS (IP Camera Surveillance System)
Version: $($Config.Version)
Website: https://ip-css.com
"@

    Write-Log "Sending survey emails to $($Recipients.Count) recipients..." -Level Info
    
    # Note: Implement email sending based on your email system
    # This is a placeholder
    
    foreach ($recipient in $Recipients) {
        Write-Log "  → $recipient" -Level Info
        # Send-MailMessage -To $recipient -From $Config.EmailFrom -Subject $subject -Body $body -SmtpServer $Config.SMTPServer -Port $Config.SMTPPort
    }
    
    Write-Log "Survey emails sent" -Level Success
}

# ============================================================================
# Main Functions
# ============================================================================

function Collect-Metrics {
    Write-Log "=== Collecting Beta Testing Metrics ===" -Level Info
    
    $metrics = @{
        Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        Week = $Week
        GitHubIssues = @{
            Total = (Get-GitHubIssues -Label "beta-bug" -State "all").Count
            Open = (Get-GitHubIssues -Label "beta-bug" -State "open").Count
            Closed = (Get-GitHubIssues -Label "beta-bug" -State "closed").Count
            Critical = (Get-GitHubIssues -Label "beta-critical" -State "open").Count
            High = (Get-GitHubIssues -Label "beta-high" -State "open").Count
            Medium = (Get-GitHubIssues -Label "beta-medium" -State "open").Count
            Low = (Get-GitHubIssues -Label "beta-low" -State "open").Count
        }
        SentryErrors = @{
            Total = (Get-SentryErrors).Count
            # Add more details as needed
        }
        Testers = @{
            Target = $Config.TargetTesters
            Active = 0  # Update based on actual activity
            Inactive = 0
        }
    }
    
    # Save metrics
    $metricsPath = Join-Path $OutputPath "metrics-week-$Week.json"
    $metrics | ConvertTo-Json -Depth 10 | Out-File $metricsPath -Encoding UTF8
    
    Write-Log "Metrics saved to $metricsPath" -Level Success
    
    return $metrics
}

function Send-WeeklySurveys {
    param(
        [int]$Week
    )
    
    Write-Log "=== Sending Week $Week Surveys ===" -Level Info
    
    if ($Config.EmailTo.Count -eq 0) {
        Write-Log "No email recipients configured. Skipping..." -Level Warning
        return
    }
    
    Send-SurveyEmail -Recipients $Config.EmailTo -Week $Week
    
    Write-Log "Surveys sent successfully" -Level Success
}

function Generate-Report {
    param(
        [int]$Week
    )
    
    Write-Log "=== Generating Week $Week Report ===" -Level Info
    
    # Collect metrics
    $metrics = Collect-Metrics
    
    # Get survey responses (implement based on your survey tool)
    $surveyResponses = @()  # Placeholder
    
    # Generate markdown report
    $report = @"
# IP-CSS Beta Testing Report - Week $Week

**Version:** $($Config.Version)  
**Report Date:** $(Get-Date -Format "yyyy-MM-dd")  
**Week:** $Week of $($Config.BetaDurationWeeks)

---

## 📊 Executive Summary

This week, **$($metrics.Testers.Active)** of **$($Config.TargetTesters)** target beta testers were active.

**Key Metrics:**
- Total Bugs Reported: $($metrics.GitHubIssues.Total)
- Critical Bugs: $($metrics.GitHubIssues.Critical)
- High-Priority Bugs: $($metrics.GitHubIssues.High)
- Survey Response Rate: $($surveyResponses.Count)/$($Config.TargetTesters) ($([math]::Round($surveyResponses.Count/$Config.TargetTesters*100, 1))%)

---

## 🐛 Bug Summary

### By Severity

| Severity | Open | Closed | Total |
|----------|------|--------|-------|
| Critical | $($metrics.GitHubIssues.Critical) | - | $($metrics.GitHubIssues.Critical) |
| High | $($metrics.GitHubIssues.High) | - | $($metrics.GitHubIssues.High) |
| Medium | $($metrics.GitHubIssues.Medium) | - | $($metrics.GitHubIssues.Medium) |
| Low | $($metrics.GitHubIssues.Low) | - | $($metrics.GitHubIssues.Low) |
| **Total** | **$($metrics.GitHubIssues.Open)** | **$($metrics.GitHubIssues.Closed)** | **$($metrics.GitHubIssues.Total)** |

### Critical Issues

$(if ($metrics.GitHubIssues.Critical -eq 0) {
    "✅ No critical issues reported this week."
} else {
    "⚠️ **$($metrics.GitHubIssues.Critical) critical issue(s) require immediate attention.**"
})

---

## 📈 Test Progress

### Scenarios Completed

| Scenario | Week 1 | Week 2 | Week 3 | Week 4 |
|----------|--------|--------|--------|--------|
| Camera Integration | $(if($Week -ge 1){"✅"}else{"⏸️"}) | - | - | - |
| Recording & Playback | $(if($Week -ge 1){"✅"}else{"⏸️"}) | - | - | - |
| Analytics | $(if($Week -ge 1){"✅"}else{"⏸️"}) | - | - | - |
| Mobile Apps | - | $(if($Week -ge 2){"✅"}else{"⏸️"}) | - | - |
| NAS Platforms | - | $(if($Week -ge 2){"✅"}else{"⏸️"}) | - | - |
| Security | - | $(if($Week -ge 2){"✅"}else{"⏸️"}) | - | - |
| Performance | - | - | $(if($Week -ge 3){"✅"}else{"⏸️"}) | - |
| Usability | - | - | $(if($Week -ge 3){"✅"}else{"⏸️"}) | - |

---

## 📊 Survey Results

### Overall Satisfaction

**Average Score:** X.X / 5.0

### Net Promoter Score (NPS)

**Score:** XX / 100

### Top Feedback Themes

1. Theme 1
2. Theme 2
3. Theme 3

---

## 🎯 Go/No-Go Status

**Current Status:** $($(if ($metrics.GitHubIssues.Critical -eq 0 -and $metrics.GitHubIssues.High -le 5) {"🟡 ON TRACK"} else {"🔴 AT RISK"}))

**Criteria Status:**
- Critical Bugs: $(if($metrics.GitHubIssues.Critical -eq 0){"✅"}else{"❌"}) (Target: 0)
- High-Priority Bugs: $(if($metrics.GitHubIssues.High -le 5){"✅"}else{"❌"}) (Target: ≤5)
- Active Testers: $(if($metrics.Testers.Active -ge $Config.MinActiveTesters){"✅"}else{"❌"}) (Target: ≥$($Config.MinActiveTesters))

---

## 📋 Next Week Plan

**Priorities:**
1. Fix critical bugs
2. Continue testing scenarios
3. Collect feedback

**Focus Areas:**
- Area 1
- Area 2
- Area 3

---

**Report Generated:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")  
**Generated By:** Beta Testing Automation Script v1.0
"@

    # Save report
    $reportPath = Join-Path $OutputPath "report-week-$Week.md"
    $report | Out-File $reportPath -Encoding UTF8
    
    Write-Log "Report saved to $reportPath" -Level Success
    
    return $report
}

function Update-Dashboard {
    Write-Log "=== Updating Dashboard ===" -Level Info
    
    # This would integrate with Grafana or another dashboard tool
    # Placeholder for dashboard update logic
    
    Write-Log "Dashboard updated at $($Config.GrafanaURL)" -Level Success
}

# ============================================================================
# Main Execution
# ============================================================================

function Main {
    Write-Log "========================================" -Level Info
    Write-Log "IP-CSS Beta Testing Automation Script" -Level Info
    Write-Log "Version: 1.0.0" -Level Info
    Write-Log "========================================" -Level Info
    
    # Create output directory
    if (!(Test-Path $OutputPath)) {
        New-Item -ItemType Directory -Path $OutputPath | Out-Null
        Write-Log "Created output directory: $OutputPath" -Level Success
    }
    
    # Check prerequisites
    if (!(Test-Prerequisites)) {
        Write-Log "Prerequisites check failed. Exiting..." -Level Error
        exit 1
    }
    
    # Execute action
    switch ($Action) {
        'collect' {
            Collect-Metrics
        }
        'survey' {
            Send-WeeklySurveys -Week $Week
        }
        'report' {
            Generate-Report -Week $Week
        }
        'dashboard' {
            Update-Dashboard
        }
        'all' {
            Collect-Metrics
            Send-WeeklySurveys -Week $Week
            Generate-Report -Week $Week
            Update-Dashboard
        }
    }
    
    Write-Log "========================================" -Level Info
    Write-Log "Script completed successfully!" -Level Success
    Write-Log "========================================" -Level Info
}

# Run main function
Main

```

---

## 📖 Usage Examples

### Run All Actions (Default)
```powershell
.\beta-testing-automation.ps1 -Week 1
```

### Collect Metrics Only
```powershell
.\beta-testing-automation.ps1 -Action collect -Week 1
```

### Send Surveys Only
```powershell
.\beta-testing-automation.ps1 -Action survey -Week 2
```

### Generate Report Only
```powershell
.\beta-testing-automation.ps1 -Action report -Week 3
```

### Update Dashboard Only
```powershell
.\beta-testing-automation.ps1 -Action dashboard
```

### Verbose Mode
```powershell
.\beta-testing-automation.ps1 -Week 1 -Verbose
```

---

## 📁 Output Files

The script generates the following files in the output directory:

| File | Description |
|------|-------------|
| `metrics-week-{N}.json` | Raw metrics data for week N |
| `report-week-{N}.md` | Markdown report for week N |

---

## ⚙️ Configuration

Edit the `$Config` hashtable at the top of the script to customize:

| Setting | Description |
|---------|-------------|
| `ProjectName` | Project name |
| `Version` | Beta version |
| `BetaDurationWeeks` | Total beta duration |
| `TargetTesters` | Target number of testers |
| `EmailTo` | Beta tester email list |
| `GitHubRepo` | GitHub repository |
| `GrafanaURL` | Grafana dashboard URL |

---

## 🔧 Prerequisites

- PowerShell 7.0+
- Git
- GitHub CLI (`gh`)
- Sentry CLI (optional, for error tracking)
- SMTP access (for email sending)

---

*Script Version: 1.0*  
*Created: 28 January 2026*  
*Author: NLP-Core-Team*
