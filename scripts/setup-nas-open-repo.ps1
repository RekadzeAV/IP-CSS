# Script to create IP-CSS-open repository on NAS and push current project
# Usage: .\setup-nas-open-repo.ps1

$NAS_HOST = "192.168.10.38"
$GIT_USER = "Andrey"
$GIT_SSH_PORT = 22
$GIT_BASE_PATH = "/volume1/Git"
$REPO_NAME = "IP-CSS-open"
$PLINK_PATH = "plink.exe"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Setup NAS Repository for IP-CSS-open" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check current git status
Write-Host "[1/6] Checking current git status..." -ForegroundColor Yellow
$currentBranch = git branch --show-current
Write-Host "      Current branch: $currentBranch" -ForegroundColor Gray

# Step 2: Test SSH connection
Write-Host "[2/6] Testing SSH connection to NAS..." -ForegroundColor Yellow
$result = & $PLINK_PATH -ssh -batch -timeout 5 "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "echo 'Connection successful'" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "      ERROR: Cannot connect to NAS" -ForegroundColor Red
    Write-Host "      Please ensure GIT_PASSWORD environment variable is set" -ForegroundColor Red
    exit 1
}
Write-Host "      Connection successful" -ForegroundColor Green

# Step 3: Clear existing repository if exists
Write-Host "[3/6] Checking for existing repository..." -ForegroundColor Yellow
$existingRepo = & $PLINK_PATH -ssh -batch -timeout 5 "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "test -d ${GIT_BASE_PATH}/${REPO_NAME}.git && echo 'exists' || echo 'not-found'" 2>&1

if ($existingRepo -eq "exists") {
    Write-Host "      Repository already exists, clearing..." -ForegroundColor Yellow
    $confirm = Read-Host "      Delete existing repository? (yes/no)"
    if ($confirm -ne "yes") {
        Write-Host "      Operation cancelled" -ForegroundColor Yellow
        exit 0
    }
    & $PLINK_PATH -ssh -batch -timeout 5 "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "rm -rf ${GIT_BASE_PATH}/${REPO_NAME}.git"
    Write-Host "      Existing repository removed" -ForegroundColor Green
} else {
    Write-Host "      No existing repository found" -ForegroundColor Gray
}

# Step 4: Create new bare repository
Write-Host "[4/6] Creating new bare repository on NAS..." -ForegroundColor Yellow
$result = & $PLINK_PATH -ssh -batch -timeout 10 "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "mkdir -p ${GIT_BASE_PATH}/${REPO_NAME}.git && cd ${GIT_BASE_PATH}/${REPO_NAME}.git && git init --bare" 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "      ERROR: Failed to create repository" -ForegroundColor Red
    Write-Host "      Details: $result" -ForegroundColor Red
    exit 1
}
Write-Host "      Repository created successfully" -ForegroundColor Green
Write-Host "      Path: ${GIT_BASE_PATH}/${REPO_NAME}.git" -ForegroundColor Gray

# Step 5: Add remote and push
Write-Host "[5/6] Adding NAS as remote and pushing..." -ForegroundColor Yellow

# Remove existing 'nas' remote if it exists
git remote | ForEach-Object {
    if ($_ -eq "nas") {
        git remote remove nas
    }
}

# Add new remote
$nasUrl = "${GIT_USER}@${NAS_HOST}:${GIT_BASE_PATH}/${REPO_NAME}.git"
git remote add nas $nasUrl
Write-Host "      Remote 'nas' added: $nasUrl" -ForegroundColor Gray

# Push all branches
Write-Host "      Pushing all branches..." -ForegroundColor Gray
git push nas --all
if ($LASTEXITCODE -ne 0) {
    Write-Host "      WARNING: Failed to push branches" -ForegroundColor Yellow
}

# Push all tags
Write-Host "      Pushing all tags..." -ForegroundColor Gray
git push nas --tags
if ($LASTEXITCODE -ne 0) {
    Write-Host "      WARNING: No tags to push or failed" -ForegroundColor Yellow
}

Write-Host "      Push completed" -ForegroundColor Green

# Step 6: Verify
Write-Host "[6/6] Verifying repository..." -ForegroundColor Yellow
$branches = & $PLINK_PATH -ssh -batch -timeout 5 "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "cd ${GIT_BASE_PATH}/${REPO_NAME}.git && git branch -a" 2>&1
Write-Host "      Branches in repository:" -ForegroundColor Gray
$branches | ForEach-Object { Write-Host "        $_" -ForegroundColor Gray }

$size = & $PLINK_PATH -ssh -batch -timeout 5 "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "du -sh ${GIT_BASE_PATH}/${REPO_NAME}.git" 2>&1
Write-Host "      Repository size: $($size.Split()[0])" -ForegroundColor Gray

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Repository URL:" -ForegroundColor Cyan
Write-Host "  git clone ${GIT_USER}@${NAS_HOST}:${GIT_BASE_PATH}/${REPO_NAME}.git" -ForegroundColor White
Write-Host ""
Write-Host "To add as remote in other projects:" -ForegroundColor Cyan
Write-Host "  git remote add nas ${GIT_USER}@${NAS_HOST}:${GIT_BASE_PATH}/${REPO_NAME}.git" -ForegroundColor White
Write-Host "  git push nas --all --tags" -ForegroundColor White
Write-Host ""
