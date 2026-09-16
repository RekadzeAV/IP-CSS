# PowerShell script to manage Git repositories on Synology NAS RS2416+
# Usage: .\manage-nas-git.ps1 [-Action] <action> [-RepoName <name>]

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("List", "Clear", "Create", "Delete", "Mirror", "Info", "TestConnection", "Help")]
    [string]$Action = "Help",
    
    [Parameter(Mandatory=$false)]
    [string]$RepoName,
    
    [Parameter(Mandatory=$false)]
    [switch]$Confirm
)

$NAS_HOST = "192.168.10.38"
$GIT_USER = "Andrey"
$GIT_SSH_PORT = 22
$GIT_BASE_PATH = "/volume1/Git"
$PLINK_PATH = "plink.exe"

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    [Console]::Error.WriteLine("[ERROR] $Message")
}

function Test-Connection {
    Write-Info "Testing SSH connection to $NAS_HOST..."
    $result = & $PLINK_PATH -ssh -batch -pw $env:GIT_PASSWORD "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "echo 'Connection successful'" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Info "SSH connection successful"
        return $true
    } else {
        Write-Error "SSH connection failed: $result"
        return $false
    }
}

function List-Repos {
    Write-Info "Listing all repositories on NAS..."
    & $PLINK_PATH -ssh -batch -pw $env:GIT_PASSWORD "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "ls -la ${GIT_BASE_PATH}/*.git 2>/dev/null || echo 'No repositories found'"
}

function Clear-AllRepos {
    Write-Warning "This will delete ALL repositories on NAS!"
    $confirm = Read-Host "Are you sure? (yes/no)"
    if ($confirm -ne "yes") {
        Write-Info "Operation cancelled"
        return
    }
    
    Write-Info "Clearing all repositories..."
    & $PLINK_PATH -ssh -batch -pw $env:GIT_PASSWORD "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "rm -rf ${GIT_BASE_PATH}/*.git"
    Write-Info "All repositories cleared"
}

function Create-Repo {
    param([string]$Name)
    
    if ([string]::IsNullOrEmpty($Name)) {
        Write-Error "Repository name is required"
        return
    }
    
    Write-Info "Creating new bare repository: $Name.git"
    $result = & $PLINK_PATH -ssh -batch -pw $env:GIT_PASSWORD "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "mkdir -p ${GIT_BASE_PATH}/${Name}.git && cd ${GIT_BASE_PATH}/${Name}.git && git init --bare"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Repository created successfully"
        Write-Info "Clone URL: ${GIT_USER}@${NAS_HOST}:${GIT_BASE_PATH}/${Name}.git"
    } else {
        Write-Error "Failed to create repository: $result"
    }
}

function Delete-Repo {
    param([string]$Name)
    
    if ([string]::IsNullOrEmpty($Name)) {
        Write-Error "Repository name is required"
        return
    }
    
    Write-Warning "This will delete repository: $Name.git"
    $confirm = Read-Host "Are you sure? (yes/no)"
    if ($confirm -ne "yes") {
        Write-Info "Operation cancelled"
        return
    }
    
    Write-Info "Deleting repository: $Name.git"
    & $PLINK_PATH -ssh -batch -pw $env:GIT_PASSWORD "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT "rm -rf ${GIT_BASE_PATH}/${Name}.git"
    Write-Info "Repository deleted"
}

function Mirror-ToNAS {
    param([string]$Name)
    
    if ([string]::IsNullOrEmpty($Name)) {
        $Name = "IP-CSS"
    }
    
    Write-Info "Mirroring current project to NAS..."
    
    # Check if we're in a git repository
    if (-not (Test-Path .git)) {
        Write-Error "Not a git repository"
        return
    }
    
    # Add NAS as remote if not exists
    $remotes = git remote
    if ($remotes -notcontains "nas") {
        Write-Info "Adding NAS as remote 'nas'..."
        git remote add nas "${GIT_USER}@${NAS_HOST}:${GIT_BASE_PATH}/${Name}.git"
    }
    
    # Push all branches and tags
    Write-Info "Pushing all branches and tags..."
    git push nas --all
    git push nas --tags
    
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Mirror created successfully"
    } else {
        Write-Error "Failed to create mirror"
    }
}

function Show-RepoInfo {
    param([string]$Name)
    
    if ([string]::IsNullOrEmpty($Name)) {
        Write-Error "Repository name is required"
        return
    }
    
    Write-Info "Getting information for repository: $Name.git"
    & $PLINK_PATH -ssh -batch -pw $env:GIT_PASSWORD "${GIT_USER}@${NAS_HOST}" -P $GIT_SSH_PORT @"
        REPO_PATH='${GIT_BASE_PATH}/${Name}.git'
        if [ -d "\$REPO_PATH" ]; then
            echo 'Repository exists'
            echo 'Size:' && du -sh \$REPO_PATH
            echo 'Branches:' && cd \$REPO_PATH && git branch -a
            echo 'Last commit:' && cd \$REPO_PATH && git log -1 --format='%ci %s'
        else
            echo 'Repository not found'
        fi
"@
}

function Show-HelpText {
    Write-Host @"
Usage: .\$MYInvocation.MyCommand.Name [-Action] <action> [-RepoName <name>]

Actions:
  List                    - List all repositories
  Clear                   - Clear all repositories (WARNING: destructive!)
  Create <repo-name>      - Create a new bare repository
  Delete <repo-name>      - Delete a repository
  Mirror <repo-name>      - Mirror current project to NAS
  Info <repo-name>        - Show repository information
  TestConnection          - Test SSH connection to NAS
  Help                    - Show this help message

Examples:
  .\$MYInvocation.MyCommand.Name -Action List
  .\$MYInvocation.MyCommand.Name -Action Create -RepoName IP-CSS-open
  .\$MYInvocation.MyCommand.Name -Action Mirror -RepoName IP-CSS-open
  .\$MYInvocation.MyCommand.Name -Action Info -RepoName IP-CSS-open
  .\$MYInvocation.MyCommand.Name -Action Clear

NOTE: Set environment variable GIT_PASSWORD before running commands that require authentication.
"@
}

# Main execution
switch ($Action) {
    "List" { List-Repos }
    "Clear" { Clear-AllRepos }
    "Create" { Create-Repo -Name $RepoName }
    "Delete" { Delete-Repo -Name $RepoName }
    "Mirror" { Mirror-ToNAS -Name $RepoName }
    "Info" { Show-RepoInfo -Name $RepoName }
    "TestConnection" { Test-Connection }
    "Help" { Show-HelpText }
}
