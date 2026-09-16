#!/bin/bash

# Documentation Linter Script for IP-CSS
# Version: 1.0.0
# Date: 27 April 2026

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DOCS_DIR="${1:-docs}"
ERRORS=0
WARNINGS=0

echo "=========================================="
echo "IP-CSS Documentation Linter"
echo "=========================================="
echo "Scanning directory: $DOCS_DIR"
echo ""

# Check if markdown-link-check is installed
if ! command -v markdown-link-check &> /dev/null; then
    echo -e "${YELLOW}Installing markdown-link-check...${NC}"
    npm install -g markdown-link-check
fi

# Check if mdl is installed
if ! command -v mdl &> /dev/null; then
    echo -e "${YELLOW}Installing mdl (markdown linter)...${NC}"
    gem install mdl
fi

# 1. Check for broken links
echo "=========================================="
echo "1. Checking for broken links..."
echo "=========================================="

find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" -not -path "*/versions/*" | while read -r file; do
    echo -e "Checking: ${GREEN}$file${NC}"
    if ! markdown-link-check -q "$file"; then
        echo -e "${RED}✗ Broken links found in $file${NC}"
        ERRORS=$((ERRORS + 1))
    else
        echo -e "${GREEN}✓ No broken links${NC}"
    fi
done

# 2. Check markdown style
echo ""
echo "=========================================="
echo "2. Checking markdown style..."
echo "=========================================="

find "$DOCS_DIR" -maxdepth 1 -name "*.md" | while read -r file; do
    echo -e "Checking: ${GREEN}$file${NC}"
    if ! mdl "$file" --style=.mdlrc 2>/dev/null; then
        echo -e "${YELLOW}⚠ Style issues found (non-blocking)${NC}"
        WARNINGS=$((WARNINGS + 1))
    else
        echo -e "${GREEN}✓ Style OK${NC}"
    fi
done

# 3. Check required sections
echo ""
echo "=========================================="
echo "3. Checking required sections..."
echo "=========================================="

REQUIRED_SECTIONS=("Overview" "Prerequisites" "Installation" "Configuration")

for file in "$DOCS_DIR"/*.md; do
    if [ -f "$file" ]; then
        echo -e "Checking: ${GREEN}$file${NC}"
        for section in "${REQUIRED_SECTIONS[@]}"; do
            if grep -q "## $section" "$file" || grep -q "## $section" "$file" 2>/dev/null; then
                echo -e "  ✓ Has $section section"
            else
                if [ "$section" != "Installation" ] && [ "$section" != "Configuration" ]; then
                    echo -e "  ${YELLOW}⚠ Missing $section section (optional)${NC}"
                    WARNINGS=$((WARNINGS + 1))
                fi
            fi
        done
    fi
done

# 4. Validate YAML files
echo ""
echo "=========================================="
echo "4. Validating YAML files..."
echo "=========================================="

find "$DOCS_DIR" -name "*.yaml" -o -name "*.yml" | while read -r file; do
    echo -e "Validating: ${GREEN}$file${NC}"
    if command -v python3 &> /dev/null; then
        if python3 -c "import yaml, sys; yaml.safe_load(open('$file'))" 2>/dev/null; then
            echo -e "${GREEN}✓ Valid YAML${NC}"
        else
            echo -e "${RED}✗ Invalid YAML syntax${NC}"
            ERRORS=$((ERRORS + 1))
        fi
    else
        echo -e "${YELLOW}⚠ Python3 not available, skipping YAML validation${NC}"
    fi
done

# 5. Check for code block syntax
echo ""
echo "=========================================="
echo "5. Checking code block syntax..."
echo "=========================================="

find "$DOCS_DIR" -name "*.md" | while read -r file; do
    echo -e "Checking: ${GREEN}$file${NC}"
    
    # Check for unclosed code blocks
    BACKTICK_COUNT=$(grep -c '```' "$file" 2>/dev/null || echo 0)
    
    if [ $((BACKTICK_COUNT % 2)) -eq 0 ]; then
        echo -e "${GREEN}✓ All code blocks properly closed${NC}"
    else
        echo -e "${RED}✗ Unclosed code blocks detected${NC}"
        ERRORS=$((ERRORS + 1))
    fi
done

# 6. Check for API endpoint format
echo ""
echo "=========================================="
echo "6. Checking API endpoint format..."
echo "=========================================="

find "$DOCS_DIR" -name "API*.md" | while read -r file; do
    echo -e "Checking: ${GREEN}$file${NC}"
    
    # Check if endpoints use proper HTTP methods
    if grep -qE "(GET|POST|PUT|DELETE|PATCH) /api" "$file"; then
        echo -e "${GREEN}✓ API endpoints properly formatted${NC}"
    else
        echo -e "${YELLOW}⚠ No API endpoints found or format may be incorrect${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
done

# 7. Check for image alt text
echo ""
echo "=========================================="
echo "7. Checking image alt text..."
echo "=========================================="

find "$DOCS_DIR" -name "*.md" | while read -r file; do
    if grep -q '!\[' "$file"; then
        echo -e "Checking: ${GREEN}$file${NC}"
        
        # Check for images without alt text
        if grep -E '!\[\]\(' "$file" &> /dev/null; then
            echo -e "${YELLOW}⚠ Images without alt text found${NC}"
            WARNINGS=$((WARNINGS + 1))
        else
            echo -e "${GREEN}✓ All images have alt text${NC}"
        fi
    fi
done

# Summary
echo ""
echo "=========================================="
echo "SUMMARY"
echo "=========================================="
echo -e "Errors: ${RED}$ERRORS${NC}"
echo -e "Warnings: ${YELLOW}$WARNINGS${NC}"
echo ""

if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}✗ Lint failed with $ERRORS error(s)${NC}"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}⚠ Lint passed with $WARNINGS warning(s)${NC}"
    exit 0
else
    echo -e "${GREEN}✓ Lint passed successfully${NC}"
    exit 0
fi
