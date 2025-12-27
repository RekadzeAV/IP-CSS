# Управление документацией - Краткая инструкция

## Быстрый старт

### Создание документа
```powershell
.\scripts\manage-documentation.ps1 -Action create -Document "docs/NEW_DOC.md"
```

### Обновление документа
```powershell
.\scripts\manage-documentation.ps1 -Action update -Document "docs/ARCHITECTURE.md"
```

### Слияние документов
```powershell
.\scripts\manage-documentation.ps1 -Action merge -SourceDocuments @("doc1.md", "doc2.md") -OutputDocument "docs/MERGED.md"
```

Подробная документация: [docs/DOCUMENTATION_MANAGEMENT.md](../docs/DOCUMENTATION_MANAGEMENT.md)

