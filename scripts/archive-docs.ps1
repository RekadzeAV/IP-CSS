# Archive old documentation files
$docsDir = "docs"
$archiveDir = "archive/docs"

# Create archive directories
$dirs = @("api", "deployment", "onvif", "reports", "phases", "meta")
foreach ($dir in $dirs) {
    New-Item -ItemType Directory -Force -Path "$archiveDir/$dir" | Out-Null
}

# Reports
$reports = @(
    "FINAL_EXECUTION_REPORT.md",
    "FINAL_RELEASE_SUMMARY.md",
    "FINAL_TESTING_STATUS.md",
    "EXECUTION_SUMMARY.md"
)
foreach ($f in $reports) { Copy-Item "$docsDir/$f" "$archiveDir/reports/" -Force }

# ONVIF
$onvif = @(
    "ONVIF_CLIENT_STAGE_4.4_DETAILS.md",
    "ONVIF_IMPLEMENTATION_COMPLETE_2026_01_27.md",
    "ONVIF_IMPLEMENTATION_STATUS_2026_01_27.md",
    "ONVIF_EVENT_SERVICE_INTEGRATION_PLAN.md",
    "ONVIF_CLIENT_IMPLEMENTATION_PLAN.md"
)
foreach ($f in $onvif) { Copy-Item "$docsDir/$f" "$archiveDir/onvif/" -Force }

# API
$api = @("API_V2.md", "API_EXAMPLES.md")
foreach ($f in $api) { Copy-Item "$docsDir/$f" "$archiveDir/api/" -Force }

# Deployment
$deploy = @("DEPLOYMENT_GUIDE.md", "DEPLOYMENT_PHASE2_2026-06-11.md")
foreach ($f in $deploy) { Copy-Item "$docsDir/$f" "$archiveDir/deployment/" -Force }

# Phases
$phases = @(
    "PHASE1_REFINEMENT_PLAN.md",
    "PHASE2_README.md",
    "PHASE2_COMPLETE_DOCUMENTATION_INDEX_2026-06-11.md",
    "PHASE2_FINAL_INSTRUCTIONS_2026-06-11.md",
    "PHASE3_QUICK_START_2026-06-11.md",
    "PHASE3_SPRINT1_RECONNECT_IMPLEMENTATION.md",
    "PHASE3_SPRINT1_STATUS.md"
)
foreach ($f in $phases) { Copy-Item "$docsDir/$f" "$archiveDir/phases/" -Force }

# Meta (DOCUMENTATION_* except INDEX)
Get-ChildItem "$docsDir/DOCUMENTATION_*.md" | ForEach-Object {
    if ($_.Name -ne "DOCUMENTATION_INDEX.md") {
        Copy-Item $_.FullName "$archiveDir/meta/" -Force
        Write-Host "  Copied: $($_.Name)"
    }
}

Write-Host "`nPhase 0.4 complete! Files archived to $archiveDir"
Write-Host "`nSummary:"
foreach ($dir in $dirs) {
    $count = (Get-ChildItem "$archiveDir/$dir").Count
    Write-Host "  $archiveDir/$dir : $count files"
}