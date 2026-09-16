#!/bin/bash
# Проверка и настройка Git LFS для IP-CSS

check_git_lfs() {
    echo "=== Git LFS Check ==="
    
    if ! command -v git-lfs &> /dev/null; then
        echo "[WARN] Git LFS not installed"
        echo "Install instructions:"
        
        case "$(uname -s)" in
            Linux*)
                if [ -f /etc/debian_version ]; then
                    echo "  sudo apt install git-lfs"
                elif [ -f /etc/redhat-release ]; then
                    echo "  sudo dnf install git-lfs"
                fi
                ;;
            Darwin*)
                echo "  brew install git-lfs"
                ;;
            MINGW*|MSYS*|CYGWIN*)
                echo "  choco install git-lfs"
                ;;
        esac
        return 1
    fi
    
    echo "[OK] Git LFS $(git-lfs version | head -1 | cut -d' ' -f3)"
    
    if ! git lfs ls-files &> /dev/null; then
        echo "[INFO] Initializing Git LFS..."
        git lfs install
    fi
    
    local lfs_files=$(git lfs ls-files 2>/dev/null | wc -l)
    if [ "$lfs_files" -gt 0 ]; then
        echo "[INFO] Found $lfs_files Git LFS tracked files"
    fi
    
    return 0
}

check_git_lfs
