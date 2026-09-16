# IP-CSS Documentation Management

**Version:** 1.0.0  
**Date:** 27 April 2026  
**Maintainer:** Documentation Team

---

## Overview

This directory contains all documentation for the IP-CSS (IP Camera Surveillance System) project.

## Documentation Structure

```
docs/
├── versions/                 # Versioned documentation
│   ├── v1.0.0/              # Current stable release
│   ├── v1.1.0/              # Next release (in development)
│   └── latest/              # Development version
├── languages/               # Multi-language support
│   ├── TRANSLATION_GUIDE.md
│   ├── en/                  # English translations
│   ├── fr/                  # French translations
│   └── de/                  # German translations
├── status/                  # Project status documents
│   └── CONSOLIDATED_PROJECT_STATUS_*.md
├── planning/                # Planning documents
│   └── *_PLAN_*.md
├── reports/                 # Task completion reports
│   └── *_COMPLETION_REPORT_*.md
├── archive/                 # Archived documents
│   └── YYYY-MM-DD/
├── API_V2.md               # API documentation
├── DEPLOYMENT_GUIDE_V2.md  # Deployment guide
├── E2E_TESTING.md          # E2E testing guide
├── ARCHITECTURE.md         # Architecture overview
├── VERSIONING_GUIDE.md     # Versioning guide
└── .mdlrc                  # Markdown linter rules
```

## Quick Links

- [API Documentation](../archive/docs/api/API_V2.md) - Complete API reference
- [Deployment Guide](DEPLOYMENT_GUIDE_V2.md) - Installation & deployment
- [E2E Testing](../archive/docs/guides/E2E_TESTING.md) - Testing guide
- [Architecture](ARCHITECTURE.md) - System architecture
- [Versioning Guide](VERSIONING_GUIDE.md) - Documentation versioning
- [Translation Guide](languages/TRANSLATION_GUIDE.md) - Translation process

## Documentation Lifecycle

### 1. Creation

New documents are created in the root `docs/` directory:

```bash
# Create new document
touch docs/NEW_DOCUMENT.md
```

### 2. Review

All documents undergo review:

1. **Self-review** by author
2. **Peer review** by team member
3. **Tech Lead approval** for critical documents

### 3. Versioning

When releasing:

```bash
# Copy to version directory
cp docs/API_V2.md docs/versions/v1.0.0/

# Tag release
git tag -a v1.0.0 -m "Documentation release v1.0.0"
```

### 4. Archiving

Old documents are archived:

```bash
# Create archive directory
mkdir docs/archive/2026-04-27/

# Move obsolete documents
mv docs/OBSOLETE.md docs/archive/2026-04-27/
```

## Doc Linter

### Installation

```bash
npm install -g markdown-link-check
gem install mdl
```

### Usage

```bash
# Run on all docs
./scripts/docs-lint.sh docs

# Run on specific file
./scripts/docs-lint.sh docs/API_V2.md
```

### CI/CD Integration

Doc linter runs automatically on:
- Pull requests
- Push to main branch
- Documentation updates

## Translation Process

### Priority Levels

1. **CRITICAL** - README, API, Deployment, Architecture
2. **HIGH** - E2E Testing, Development, Security
3. **MEDIUM** - User-facing documentation
4. **LOW** - Internal technical documents

### Workflow

1. Select document from priority list
2. Create translation in `docs/languages/<lang>/`
3. Follow [TRANSLATION_GUIDE.md](languages/TRANSLATION_GUIDE.md)
4. Submit for review
5. Merge after approval

## Version Control

### Branch Strategy

```
main (stable)
├── v1.0.0 (tagged)
├── v1.1.0 (tagged)
└── develop
    ├── feature/doc-updates
    ├── feature/translation-en
    └── feature/new-api-docs
```

### Commit Messages

```
docs: Update API documentation
docs(en): Add English translation of README
docs(versioning): Add v1.1.0 release notes
```

## Quality Standards

### Required Sections

All major documents must include:
- Overview
- Prerequisites
- Installation/Usage
- Configuration
- Troubleshooting

### Formatting

- Use consistent markdown style
- Include code examples
- Add alt text to images
- Test all links
- Use proper code block syntax

### Content

- Keep technical terms in English
- Provide clear examples
- Update regularly
- Reference related documents

## Maintenance

### Weekly Tasks

- Check for broken links
- Review recent changes
- Update outdated content

### Monthly Tasks

- Full documentation audit
- Review translation progress
- Archive obsolete documents

### Quarterly Tasks

- Major documentation review
- Update versioning strategy
- Review linter rules

## Contact

For documentation questions:
- **Tech Lead:** tech.lead@company.com
- **Documentation Team:** docs@company.com
- **Issues:** [GitHub Issues](https://github.com/RekadzeAV/IP-CSS/issues)

---

**Last Updated:** 27 April 2026  
**Maintainer:** Documentation Team
