# Remove original files that were archived
$docsDir = "docs"

$toRemove = @(
    "$docsDir/FINAL_EXECUTION_REPORT.md",
    "$docsDir/FINAL_RELEASE_SUMMARY.md",
    "$docsDir/FINAL_TESTING_STATUS.md",
    "$docsDir/EXECUTION_SUMMARY.md",
    "$docsDir/ONVIF_CLIENT_STAGE_4.4_DETAILS.md",
    "$docsDir/ONVIF_IMPLEMENTATION_COMPLETE_2026_01_27.md",
    "$docsDir/ONVIF_IMPLEMENTATION_STATUS_2026_01_27.md",
    "$docsDir/ONVIF_EVENT_SERVICE_INTEGRATION_PLAN.md",
    "$docsDir/ONVIF_CLIENT_IMPLEMENTATION_PLAN.md",
    "$docsDir/API_V2.md",
    "$docsDir/API_EXAMPLES.md",
    "$docsDir/DEPLOYMENT_GUIDE.md",
    "$docsDir/DEPLOYMENT_PHASE2_2026-06-11.md",
    "$docsDir/PHASE1_REFINEMENT_PLAN.md",
    "$docsDir/PHASE2_README.md",
    "$docsDir/PHASE2_COMPLETE_DOCUMENTATION_INDEX_2026-06-11.md",
    "$docsDir/PHASE2_FINAL_INSTRUCTIONS_2026-06-11.md",
    "$docsDir/PHASE3_QUICK_START_2026-06-11.md",
    "$docsDir/PHASE3_SPRINT1_RECONNECT_IMPLEMENTATION.md",
    "$docsDir/PHASE3_SPRINT1_STATUS.md"
)

# Remove DOCUMENTATION_* except INDEX
Get-ChildItem "$docsDir/DOCUMENTATION_*.md" | ForEach-Object {
    if ($_.Name -ne "DOCUMENTATION_INDEX.md") {
        $toRemove += $_.FullName
    }
}

$count = 0
foreach ($f in $toRemove) {
    if (Test-Path $f) {
        Remove-Item $f -Force
        $count++
        Write-Host "  Removed: $f"
    }
}
Write-Host "`nRemoved $count archived files from docs/"