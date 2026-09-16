# Documentation Versioning Guide

**Version:** 1.0.0  
**Date:** 27 April 2026

---

## Overview

This guide describes the documentation versioning strategy for IP-CSS project.

## Versioning Strategy

We follow **Semantic Versioning** for documentation:

- **MAJOR** (v1.0.0) - Breaking changes to documentation structure
- **MINOR** (v1.1.0) - New features, added content
- **PATCH** (v1.1.1) - Bug fixes, typo corrections

## Directory Structure

```
docs/
├── versions/
│   ├── v1.0.0/              # Current stable release
│   │   ├── README.md
│   │   ├── API.md
│   │   ├── ARCHITECTURE.md
│   │   └...
│   ├── v1.1.0/              # Next release (in development)
│   │   └...
│   └── latest/              # Development version (unstable)
│       └...
├── archive/
│   ├── 2026-04-27/          # Archived versions
│   └...
├── VERSIONING_GUIDE.md      # This file
└── docs_lint_config.yaml    # Linter configuration
```

## Git Flow for Documentation

### Branch Strategy

```
main (stable releases)
├── v1.0.0 (tagged)
├── v1.0.1 (tagged)
└── develop (development)
    ├── feature/doc-updates
    ├── feature/new-api-docs
    └── feature/translation-en
```

### Release Process

1. **Create release branch**
   ```bash
   git checkout develop
   git checkout -b release/v1.1.0
   ```

2. **Update documentation**
   - Add new content
   - Update version numbers
   - Update changelog

3. **Test documentation**
   ```bash
   ./scripts/docs-lint.sh
   ./scripts/check-links.sh
   ```

4. **Tag and release**
   ```bash
   git tag -a v1.1.0 -m "Documentation release v1.1.0"
   git push origin v1.1.0
   ```

5. **Copy to versions directory**
   ```bash
   cp -r docs/* docs/versions/v1.1.0/
   ```

## Versioned Content

### Current Version (v1.0.0)

Stable, production-ready documentation.

**Location:** `docs/versions/v1.0.0/`

**Characteristics:**
- ✅ Fully tested
- ✅ Reviewed by Tech Lead
- ✅ No known issues
- ✅ Suitable for production use

### Development Version (latest)

Unstable, in-development documentation.

**Location:** `docs/versions/latest/`

**Characteristics:**
- ⚠️ May contain bugs
- ⚠️ Under active development
- ⚠️ Not for production use
- ✅ Latest features

### Archive

Older versions kept for reference.

**Location:** `docs/archive/`

## Doc Linter

### Configuration

**File:** `docs_lint_config.yaml`

```yaml
# Documentation Linter Configuration

rules:
  # Broken Links Check
  - name: no-broken-links
    severity: error
    description: All internal links must point to existing files
    
  # Formatting Consistency
  - name: consistent-formatting
    severity: warning
    description: Maintain consistent markdown formatting
    
  # Required Sections
  - name: required-sections
    severity: error
    description: Documents must contain required sections
    required_sections:
      - Overview
      - Prerequisites
      - Installation/Usage
      - Configuration
      - Troubleshooting
      
  # Language Consistency
  - name: language-consistency
    severity: warning
    description: Maintain consistent language within document
    
  # Code Blocks
  - name: valid-code-blocks
    severity: error
    description: Code blocks must have valid syntax highlighting
    
  # API Documentation
  - name: api-endpoint-format
    severity: error
    description: API endpoints must follow REST format
    
  # Images
  - name: image-alt-text
    severity: warning
    description: All images must have alt text

# File patterns to check
include:
  - "*.md"
  - "*.mdx"

exclude:
  - "archive/**"
  - "versions/**"
  - "**/node_modules/**"

# Severity levels
severity_levels:
  error:
    - fail_build: true
    - block_merge: true
  warning:
    - fail_build: false
    - block_merge: false
```

### Running the Linter

```bash
# Install dependencies
npm install -g markdown-link-check mdl

# Run linter
./scripts/docs-lint.sh

# Check specific file
./scripts/docs-lint.sh docs/README.md

# Check all files
./scripts/docs-lint.sh --all
```

### Linter Script

**File:** `scripts/docs-lint.sh`

```bash
#!/bin/bash

# Documentation Linter Script

echo "Running documentation linter..."

# Check for broken links
echo "Checking for broken links..."
find docs -name "*.md" -not -path "*/archive/*" -not -path "*/versions/*" | \
  xargs markdown-link-check -q

# Check markdown style
echo "Checking markdown style..."
mdl docs/*.md --style=.mdlrc

# Validate YAML files
echo "Validating YAML files..."
find docs -name "*.yaml" -o -name "*.yml" | \
  xargs python -c "import yaml, sys; yaml.safe_load(sys.stdin)"

# Check for required sections
echo "Checking required sections..."
for file in docs/*.md; do
  if ! grep -q "## Overview" "$file"; then
    echo "Warning: $file missing Overview section"
  fi
done

echo "Linter completed."
```

## CI/CD Integration

### GitHub Actions

**File:** `.github/workflows/docs-lint.yml`

```yaml
name: Documentation Lint

on:
  pull_request:
    paths:
      - 'docs/**'
      - '*.md'

jobs:
  lint:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install dependencies
        run: |
          npm install -g markdown-link-check mdl
      
      - name: Run linter
        run: |
          ./scripts/docs-lint.sh
      
      - name: Check links
        run: |
          find docs -name "*.md" | \
            xargs markdown-link-check -q
      
      - name: Upload report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: docs-lint-report
          path: docs-lint-report.json
```

## Changelog

### v1.0.0 (2026-04-27)

**Initial Release**
- ✅ Complete API documentation
- ✅ Deployment guide with Docker & Kubernetes
- ✅ E2E testing guide
- ✅ Architecture documentation
- ✅ Versioning infrastructure
- ✅ Translation infrastructure

**Changes:**
- Created 6 major documents
- Documented 85+ API endpoints
- Added production-ready configs
- Set up CI/CD pipelines

### v0.9.0 (2026-04-20)

**Development**
- Initial documentation structure
- Basic API documentation
- Deployment examples

---

## Maintenance

### Update Schedule

- **Major updates:** Every release (monthly)
- **Minor updates:** As needed (weekly)
- **Patches:** Continuous

### Review Process

1. **Weekly:** Check for outdated content
2. **Monthly:** Review all major documents
3. **Quarterly:** Full documentation audit

### Deprecation Policy

- Documents older than 6 months marked as "legacy"
- Archived after 1 year
- Kept in `docs/archive/` for reference

---

**Last Updated:** 27 April 2026  
**Next Review:** 2026-05-27  
**Maintainer:** Documentation Team
