# Documentation Translation Guide

**Version:** 1.0.0  
**Date:** 27 April 2026

---

## Overview

This guide provides instructions for translating IP-CSS documentation into multiple languages.

## Target Languages

1. **English (en)** - Base language (primary)
2. **French (fr)** - Secondary
3. **German (de)** - Secondary
4. **Russian (ru)** - Original language (keep)

## Translation Priorities

### Priority 1 - CRITICAL (Translate First)
- README.md
- API.md
- DEPLOYMENT_GUIDE.md
- ARCHITECTURE.md

### Priority 2 - HIGH
- E2E_TESTING.md
- DEVELOPMENT.md
- SECURITY_AUDIT_REPORT.md

### Priority 3 - MEDIUM
- All other user-facing documentation

### Priority 4 - LOW
- Internal technical documents
- Archive documents

## Translation Process

### 1. Document Selection

Choose a document to translate from the priority list.

### 2. Create Translation Structure

```bash
docs/languages/<lang-code>/
├── README.md
├── API.md
├── DEPLOYMENT_GUIDE.md
└...
```

### 3. Translation Guidelines

#### Do's:
- ✅ Maintain markdown structure
- ✅ Keep code blocks unchanged
- ✅ Preserve API endpoints exactly
- ✅ Keep technical terms in English (with translation in parentheses)
- ✅ Use consistent terminology
- ✅ Translate UI elements and user-facing text

#### Don'ts:
- ❌ Don't translate code, variable names, or file paths
- ❌ Don't change API endpoint URLs
- ❌ Don't modify JSON examples
- ❌ Don't translate error codes

### 4. Translation Quality Check

- [ ] All sections translated
- [ ] Code blocks intact
- [ ] Links work correctly
- [ ] Tables formatted properly
- [ ] No broken images

## Bilingual Approach

We recommend a **bilingual approach**:

```
docs/
├── README.md          # Russian (original)
├── README_EN.md       # English
├── README_FR.md       # French
└── README_DE.md       # German
```

Or organized by language:

```
docs/languages/
├── ru/
│   └── README.md
├── en/
│   └── README.md
├── fr/
│   └── README.md
└── de/
    └── README.md
```

## Translation Tools

### Recommended Tools

1. **DeepL Pro** - Best quality for technical content
2. **Google Translate API** - Good for quick drafts
3. **Crowdin** - For collaborative translation
4. **Transifex** - Alternative to Crowdin

### Automation

```bash
# Example: Automated translation draft
python translate_doc.py \
  --input docs/README.md \
  --output docs/languages/en/README.md \
  --source-lang ru \
  --target-lang en \
  --preserve code_blocks,api_endpoints,json
```

## Common Terms Translation

| English | Russian | French | German |
|---------|---------|--------|--------|
| Camera | Камера | Caméra | Kamera |
| Recording | Запись | Enregistrement | Aufnahme |
| Event | Событие | Événement | Ereignis |
| User | Пользователь | Utilisateur | Benutzer |
| Settings | Настройки | Paramètres | Einstellungen |
| API | API | API | API |
| Deployment | Развёртывание | Déploiement | Bereitstellung |
| Database | База данных | Base de données | Datenbank |
| Server | Сервер | Serveur | Server |
| Client | Клиент | Client | Client |

## Review Process

### 1. Self-Review
- [ ] Check translation accuracy
- [ ] Verify technical terms
- [ ] Test all links

### 2. Peer Review
- [ ] Native speaker review
- [ ] Technical accuracy check
- [ ] Style guide compliance

### 3. Final Approval
- [ ] Tech Lead approval
- [ ] PM approval (for user-facing docs)
- [ ] Merge to main branch

## Version Control

### Branch Naming
```
docs/translation/<lang-code>-<document-name>
docs/translation/en-api
docs/translation/fr-readme
```

### Commit Messages
```
docs(en): Add English translation of API.md
docs(fr): Translate README.md to French
docs(de): Update German translation of DEPLOYMENT_GUIDE.md
```

## Maintenance

### Update Strategy

When original document changes:
1. Mark translation as "needs update"
2. Update changelog
3. Notify translators
4. Schedule translation update

### Changelog Example

```markdown
# Translation Changelog

## 2026-04-27
- [en] Initial English translation of README.md
- [fr] Initial French translation of README.md
- [de] Initial German translation of README.md

## 2026-05-05
- [en] Updated API.md with new endpoints
- [fr] Updated README.md - Section 3
```

## Resources

### Style Guides
- [Google Developer Documentation Style Guide](https://developers.google.com/style)
- [Microsoft Writing Style Guide](https://learn.microsoft.com/en-us/style-guide/)
- [MDN Web Docs Style Guide](https://developer.mozilla.org/en-US/docs/MDN/Contribute/Guidelines)

### Glossaries
- Create project-specific glossary
- Maintain consistent terminology
- Share across translation team

## Contact

For translation questions:
- **Tech Lead:** tech.lead@company.com
- **PM:** project.manager@company.com
- **Translation Team:** translations@company.com

---

**Last Updated:** 27 April 2026  
**Maintainer:** Documentation Team
