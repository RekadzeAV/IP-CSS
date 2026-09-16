# Second wave of documentation archiving
$docsDir = "docs"
$archiveDir = "archive/docs"

# Create archive subdirectories
New-Item -ItemType Directory -Force -Path "$archiveDir/reports", "$archiveDir/guides", "$archiveDir/analysis" | Out-Null

# Count current docs
$count = (Get-ChildItem "$docsDir/*.md" -File).Count
Write-Host "Current docs/ .md files: $count"

# Files to archive - analysis reports
$analysisFiles = @(
    "ANALYSIS_ERRORS.md",
    "ANALYSIS_SUMMARY_REPORT.md",
    "COMPREHENSIVE_PROJECT_ANALYSIS_2026.md",
    "FUNCTIONALITY_ANALYSIS.md",
    "IP_CAMERA_ANALYSIS.md",
    "NAS_PLATFORMS_ANALYSIS.md",
    "CORE_NETWORK_FIXES_ANALYSIS.md"
)

# Files to archive - guides (outdated)
$guideFiles = @(
    "ANDROID_RTSP_INTEGRATION_GUIDE.md",
    "ANDROID_RTSP_MVP.md",
    "BUILD_NATIVE_LIBRARIES_GUIDE.md",
    "BUILD_ORGANIZATION.md",
    "BUILD_STABILIZATION.md",
    "BUILD_TROUBLESHOOTING.md",
    "BUILD_QUICK_REFERENCE.md",
    "CACHING_STRATEGY_CAMERA_REPOSITORY.md",
    "CACHING_STRATEGY.md",
    "CHARTS_AND_DIAGRAMS.md",
    "COMPILATION_FIXES_STEP_BY_STEP.md",
    "COMPLETE_TESTING_SETUP_REPORT.md",
    "DATABASE_MIGRATIONS.md",
    "DECOMPOSITION_GUIDE.md",
    "DEPENDENCIES.md",
    "DESKTOP_DETAILED_PLAN.md",
    "DESKTOP_DOCUMENTATION_UPDATE.md",
    "DESKTOP_LOCAL_BUILD_REQUIREMENTS.md",
    "DESKTOP_OPTIMIZATION_GUIDE.md",
    "DESKTOP_REFINEMENT_PLAN.md",
    "DEVELOPER_CHEAT_SHEET_PHASE2_2026-06-11.md",
    "DEVELOPMENT_TOOLS.md",
    "DSM_SMOKE_AND_ROLLBACK.md",
    "E2E_TESTING.md",
    "ENGINEER_GUIDE.md",
    "ENVIRONMENTS.md",
    "FFMPEG_INSTALLATION.md",
    "FFMPEG_SETUP_REPORT.md",
    "GIT_AND_EXECUTION_OVERVIEW.md",
    "GIT_LOCAL_SAVE_REPORT.md",
    "GIT_REMOTES_CONFIGURATION.md",
    "GITHUB_SYNC_STATUS.md",
    "GITIGNORE_SECURITY_CHECK.md",
    "GRADLE_BUILD_CACHE_OPTIMIZATION.md",
    "HARDWARE_ACCELERATION_GUIDE.md",
    "HIGH_PRIORITY_ACTIVE_DOCS.md",
    "HLS_DASH_STREAMING.md",
    "HLS_LOW_LATENCY_OPTIMIZATION.md",
    "HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md",
    "HTTPS_SETUP.md",
    "IDE_LINTER_SETUP.md",
    "IDE_SETUP_COMPLETED.md",
    "IDE_SETUP_INSTRUCTIONS.md",
    "IMPLEMENTATION_CONTINUATION.md",
    "IMPLEMENTATION_PLAN_CRITICAL_TASKS.md",
    "IMPLEMENTATION_PROGRESS.md",
    "IMPLEMENTATION_STATUS_REPORT.md",
    "INFRASTRUCTURE_VERDICT_REPORT.md",
    "INTEGRATION_COMPLETE.md",
    "INTEGRATION_TESTING_QUICK_REFERENCE.md",
    "INTELLIJ_IDEA_FINAL_INSTRUCTIONS.md",
    "INTELLIJ_IDEA_QUICK_START.md",
    "INTELLIJ_IDEA_RUN_DEMO.md",
    "INTELLIJ_IDEA_TESTING_GUIDE.md",
    "ISSUE_DESCRIPTION_GUIDELINES.md",
    "JANUS_MEDIA_SERVER_INTEGRATION.md",
    "KMP_PHASE_1_INCOMPLETE_TASKS.md",
    "KMP_SOURCE_SETS_GUIDE.md",
    "KODA_MEMORY.md",
    "KODA_RULES.md",
    "LDAP_AD_INTEGRATION_GUIDE.md",
    "LDAP_INTEGRATION_FOR_IPCSS_API.md",
    "LIBRARIES_INTEGRATION_SUMMARY.md",
    "LIVE555_CINTEROP_INTEGRATION.md",
    "LOCAL_BUILD.md",
    "LOCAL_GIT_CONFIG_STATUS.md"
)

Write-Host "`nArchiving analysis files..."
$moved = 0
foreach ($f in $analysisFiles) {
    $src = "$docsDir/$f"
    $dst = "$archiveDir/analysis/$f"
    if (Test-Path $src) {
        Copy-Item $src $dst -Force
        Remove-Item $src -Force
        Write-Host "  Moved: $f"
        $moved++
    }
}

Write-Host "`nArchiving guide files..."
foreach ($f in $guideFiles) {
    $src = "$docsDir/$f"
    $dst = "$archiveDir/guides/$f"
    if (Test-Path $src) {
        Copy-Item $src $dst -Force
        Remove-Item $src -Force
        Write-Host "  Moved: $f"
        $moved++
    }
}

$remaining = (Get-ChildItem "$docsDir/*.md" -File).Count
Write-Host "`nDone! Moved $moved files to archive."
Write-Host "Remaining in docs/: $remaining .md files"