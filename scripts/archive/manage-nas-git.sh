#!/bin/bash
# Script to manage Git repositories on Synology NAS RS2416+
# Usage: ./manage-nas-git.sh [action] [repo-name]

NAS_HOST="192.168.10.38"
GIT_USER="VSCode"
GIT_SSH_PORT=22
GIT_BASE_PATH="/volume1/Git"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to test SSH connection
test_connection() {
    log_info "Testing SSH connection to $NAS_HOST..."
    ssh -o StrictHostKeyChecking=no -o BatchMode=yes ${GIT_USER}@${NAS_HOST} -p ${GIT_SSH_PORT} "echo 'Connection successful'" 2>&1
    if [ $? -eq 0 ]; then
        log_info "SSH connection successful"
        return 0
    else
        log_error "SSH connection failed"
        return 1
    fi
}

# Function to list all repositories
list_repos() {
    log_info "Listing all repositories on NAS..."
    ssh ${GIT_USER}@${NAS_HOST} -p ${GIT_SSH_PORT} "ls -la ${GIT_BASE_PATH}/*.git 2>/dev/null || echo 'No repositories found'"
}

# Function to clear all repositories
clear_all_repos() {
    log_warn "This will delete ALL repositories on NAS!"
    read -p "Are you sure? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_info "Operation cancelled"
        return 1
    fi
    
    log_info "Clearing all repositories..."
    ssh ${GIT_USER}@${NAS_HOST} -p ${GIT_SSH_PORT} "rm -rf ${GIT_BASE_PATH}/*.git"
    log_info "All repositories cleared"
}

# Function to create a new bare repository
create_repo() {
    local repo_name=$1
    
    if [ -z "$repo_name" ]; then
        log_error "Repository name is required"
        return 1
    fi
    
    log_info "Creating new bare repository: $repo_name.git"
    ssh ${GIT_USER}@${NAS_HOST} -p ${GIT_SSH_PORT} "mkdir -p ${GIT_BASE_PATH}/${repo_name}.git && cd ${GIT_BASE_PATH}/${repo_name}.git && git init --bare"
    
    if [ $? -eq 0 ]; then
        log_info "Repository created successfully"
        log_info "Clone URL: ${GIT_USER}@${NAS_HOST}:${GIT_BASE_PATH}/${repo_name}.git"
    else
        log_error "Failed to create repository"
        return 1
    fi
}

# Function to delete a repository
delete_repo() {
    local repo_name=$1
    
    if [ -z "$repo_name" ]; then
        log_error "Repository name is required"
        return 1
    fi
    
    log_warn "This will delete repository: $repo_name.git"
    read -p "Are you sure? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_info "Operation cancelled"
        return 1
    fi
    
    log_info "Deleting repository: $repo_name.git"
    ssh ${GIT_USER}@${NAS_HOST} -p ${GIT_SSH_PORT} "rm -rf ${GIT_BASE_PATH}/${repo_name}.git"
    log_info "Repository deleted"
}

# Function to mirror current project to NAS
mirror_to_nas() {
    local repo_name=$1
    
    if [ -z "$repo_name" ]; then
        repo_name="IP-CSS"
    fi
    
    log_info "Mirroring current project to NAS..."
    
    # Check if we're in a git repository
    if ! git rev-parse --is-inside-work-tree &>/dev/null; then
        log_error "Not a git repository"
        return 1
    fi
    
    # Add NAS as remote if not exists
    if ! git remote | grep -q "nas"; then
        log_info "Adding NAS as remote 'nas'..."
        git remote add nas ${GIT_USER}@${NAS_HOST}:${GIT_BASE_PATH}/${repo_name}.git
    fi
    
    # Push all branches and tags
    log_info "Pushing all branches and tags..."
    git push nas --all
    git push nas --tags
    
    if [ $? -eq 0 ]; then
        log_info "Mirror created successfully"
    else
        log_error "Failed to create mirror"
        return 1
    fi
}

# Function to show repository info
show_repo_info() {
    local repo_name=$1
    
    if [ -z "$repo_name" ]; then
        log_error "Repository name is required"
        return 1
    fi
    
    log_info "Getting information for repository: $repo_name.git"
    ssh ${GIT_USER}@${NAS_HOST} -p ${GIT_SSH_PORT} "
        REPO_PATH='${GIT_BASE_PATH}/${repo_name}.git'
        if [ -d \"\$REPO_PATH\" ]; then
            echo 'Repository exists'
            echo 'Size:' && du -sh \$REPO_PATH
            echo 'Branches:' && cd \$REPO_PATH && git branch -a
            echo 'Last commit:' && cd \$REPO_PATH && git log -1 --format='%ci %s'
        else
            echo 'Repository not found'
        fi
    "
}

# Function to show help
show_help() {
    echo "Usage: $0 <action> [arguments]"
    echo ""
    echo "Actions:"
    echo "  list                    - List all repositories"
    echo "  clear                   - Clear all repositories (WARNING: destructive!)"
    echo "  create <repo-name>      - Create a new bare repository"
    echo "  delete <repo-name>      - Delete a repository"
    echo "  mirror <repo-name>      - Mirror current project to NAS"
    echo "  info <repo-name>        - Show repository information"
    echo "  test-connection         - Test SSH connection to NAS"
    echo "  help                    - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 list"
    echo "  $0 create IP-CSS-open"
    echo "  $0 mirror IP-CSS-open"
    echo "  $0 info IP-CSS-open"
    echo "  $0 clear"
}

# Main script
case "$1" in
    "list")
        list_repos
        ;;
    "clear")
        clear_all_repos
        ;;
    "create")
        create_repo "$2"
        ;;
    "delete")
        delete_repo "$2"
        ;;
    "mirror")
        mirror_to_nas "$2"
        ;;
    "info")
        show_repo_info "$2"
        ;;
    "test-connection")
        test_connection
        ;;
    "help"|*)
        show_help
        ;;
esac
