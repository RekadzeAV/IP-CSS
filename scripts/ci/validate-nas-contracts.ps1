param(
    [string]$ArtifactContractPath = "config/nas-artifact-contract.json",
    [string]$RuntimeEnvContractPath = "config/nas-runtime-env-contract.json",
    [string]$Version = "",
    [string]$Packages = "synology,qnap,asustor,truenas",
    [string]$Architectures = "x86_64,arm64",
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Validate NAS artifact/runtime contracts and current CI inputs."
    Write-Host "Usage: ./scripts/ci/validate-nas-contracts.ps1 [-Version Alfa-0.1.1] [-Packages synology,qnap] [-Architectures x86_64,arm64]"
    exit 0
}

function Assert-True {
    param(
        [object]$Condition,
        [string]$Message
    )
    if (-not [bool]$Condition) {
        throw $Message
    }
}

function Test-Pattern {
    param(
        [string]$Value,
        [string]$Pattern
    )
    return $Value -match $Pattern
}

$artifactPath = Join-Path (Get-Location) $ArtifactContractPath
$envPath = Join-Path (Get-Location) $RuntimeEnvContractPath

Assert-True (Test-Path $artifactPath) "Artifact contract not found: $artifactPath"
Assert-True (Test-Path $envPath) "Runtime env contract not found: $envPath"

$artifactContract = Get-Content -Path $artifactPath -Raw | ConvertFrom-Json
$runtimeContract = Get-Content -Path $envPath -Raw | ConvertFrom-Json

Assert-True ($null -ne $artifactContract.schemaVersion) "Artifact contract: schemaVersion is required"
Assert-True ($null -ne $runtimeContract.schemaVersion) "Runtime env contract: schemaVersion is required"
Assert-True ($artifactContract.packageTypes.PSObject.Properties.Count -gt 0) "Artifact contract: packageTypes must not be empty"
Assert-True ($runtimeContract.required.Count -gt 0) "Runtime env contract: required list must not be empty"

$packagesList = $Packages.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
$archList = $Architectures.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }

foreach ($pkg in $packagesList) {
    $pkgDef = $artifactContract.packageTypes.$pkg
    Assert-True ($null -ne $pkgDef) "Unknown package '$pkg' is not declared in artifact contract"
}

foreach ($arch in $archList) {
    Assert-True ($artifactContract.architectures -contains $arch) "Unknown architecture '$arch' is not declared in artifact contract"
}

if ($Version -ne "") {
    Assert-True (Test-Pattern -Value $Version -Pattern $artifactContract.releaseVersionPattern) "Version '$Version' does not match releaseVersionPattern"
    foreach ($pkg in $packagesList) {
        if ($pkg -eq "truenas") {
            continue
        }
        $namePattern = $artifactContract.packageTypes.$pkg.namePattern
        foreach ($arch in $archList) {
            $ext = $artifactContract.packageTypes.$pkg.extension
            $candidate = "ip-css-$Version-$pkg-$arch$ext"
            Assert-True (Test-Pattern -Value $candidate -Pattern $namePattern) "Artifact candidate '$candidate' violates '$pkg' naming contract"
        }
    }
}

foreach ($forbidden in $runtimeContract.forbiddenExactValues) {
    foreach ($req in $runtimeContract.required) {
        if ($req.PSObject.Properties.Name -contains "default") {
            Assert-True ($req.default -ne $forbidden) "Required env '${req.name}' uses forbidden default '$forbidden'"
        }
    }
}

Write-Host "NAS contract validation passed." -ForegroundColor Green
