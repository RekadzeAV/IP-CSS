# Simple Connection Test
param([int]$Seconds = 30)

Write-Host "Testing connection for $Seconds seconds..."
$startTime = Get-Date
$success = 0
$fail = 0

for ($i = 1; $i -le $Seconds; $i++) {
    $result = Test-NetConnection -ComputerName "127.0.0.1" -Port 8554 -WarningAction SilentlyContinue -InformationLevel Quiet
    if ($result) { $success++ } else { $fail++ }
    Write-Host "Iteration $i / $Seconds - Success: $success, Fail: $fail"
    Start-Sleep -Seconds 1
}

Write-Host "Done! Success: $success, Fail: $fail"
