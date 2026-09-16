# Анализ промта для сборки IP-CSS

## Сводка

Промт содержит комплексный план сборки, но имеет **критические несоответствия** с реальной структурой проекта IP-CSS. Без корректировок сборка **не завершится успешно**.

---

## 🔴 Критические проблемы

### 1. Несоответствие путей артефактов

| Компонент | Ожидает промт | Реальный путь |
|-----------|---------------|---------------|
| Native libs | `native/install/lib/*.so` | `native/video-processing/lib/` |
| API JAR | `server/api/build/libs/api.jar` | `server/api/build/outputs/` |
| Web | `server/web/.next/BUILD_ID` | `server/web/.next/BUILD_ID` ✓ |

**Решение**: Обновить `validate_artifacts()` с реальными путями.

### 2. Отсутствуют флаги сборки в .env/gradle.properties

Промт использует `BUILD_ANDROID`, `BUILD_NATIVE` и т.д., но проект использует:
- `ipcss.buildAndroidNative=false` в `gradle.properties`

**Решение**: Добавить в `gradle.properties`:
```properties
ipcss.buildNative=true
ipcss.buildKotlin=true
ipcss.buildWeb=true
ipcss.buildDocker=false
```

### 3. set -e ломает обработку ошибок

```bash
set -e  # Выходит при первой ошибке
```
Перехват `handle_error()` не сработает.

**Решение**: Удалить `set -e` и использовать `set -o pipefail` + явная проверка `${PIPESTATUS[0]}`.

---

## 🟠 Серьёзные проблемы

### 4. PowerShell команды в bash-блоках

На Windows:
```bash
CPU_CORES=$(Get-WmiObject Win32_Processor | ...)  # Не работает в bash
```

**Решение**: Выносить PowerShell-код в отдельные `.ps1` скрипты.

### 5. Gradle tasks не соответствуют build.gradle.kts

Промт:
```bash
:shared:compileKotlinLinuxX64
```

Реальность (build.gradle.kts):
- `ciKmpLinuxSanity`
- `ciBuildCoreSharedModules`
- `buildNativeLibraries`

**Решение**: Использовать существующие tasks из `build.gradle.kts`.

### 6. Неверный расчёт памяти на Windows

```powershell
FREE_RAM_KB = (TotalPhysicalMemory) / 1024  # Байты → KB, но дальше используется как GB
```

**Решение**: Исправить формулу:
```powershell
FREE_RAM_GB = [math]::Round($FREE_RAM_KB / 1MB, 2)
```

---

## 🟡 Мелкие проблемы

### 7. Node.js версия
- package.json требует `>=20.0.0`
- Промт проверяет только `>=18`

### 8. OpenSSL на Windows
- `openssl rand` не работает в PowerShell по умолчанию
- Использовать native PowerShell генерацию

### 9. Git LFS
- Нет предварительной проверки `git-lfs`
- Может сломать checkout больших файлов

---

## 📝 Итоговая оценка

| Категория | Статус | Примечание |
|-----------|--------|------------|
| Совместимость структуры | ❌ | Требует правки путей |
| Совместимость Gradle | ❌ | Tasks не существуют |
| Кроссплатформенность | ⚠️ | Bash/PowerShell смешаны |
| Проверка зависимостей | ⚠️ | Частично работает |
| Обработка ошибок | ❌ | set -e ломает перехват |
| Мониторинг ресурсов | ⚠️ | Windows формулы неверны |

**Вердикт**: Промт **нельзя применять напрямую**. Требуется адаптация под реальные tasks и структуру проекта.

---

## 🛠 Рекомендуемые исправления

### Файл 1: scripts/build-pipeline.sh (Linux/macOS)
```bash
#!/bin/bash
set -o pipefail  # Не set -e

# Использовать реальные tasks из build.gradle.kts
GRADLE_TASKS="ciBuildCoreSharedModules buildNativeLibraries"

# Путь к артефактам
validate_native_libs() {
    find native/video-processing/lib -name "*.so" -o -name "*.dylib"
}

validate_api_jar() {
    find server/api/build/outputs -name "*.jar"
}
```

### Файл 2: scripts/build-pipeline.ps1 (Windows)
```powershell
# Отдельный PowerShell скрипт
$CPU_CORES = (Get-CimInstance Win32_Processor | Measure-Object -Property NumberOfLogicalProcessors -Sum).Sum
$FREE_RAM_GB = [math]::Round((Get-CimInstance Win3_OperatingSystem | Select-Object -ExpandProperty FreePhysicalMemory) / 1MB, 2)
```

### Файл 3: gradle.properties (добавить)
```properties
# Build flags for pipeline
ipcss.buildNative=true
ipcss.buildKotlin=true
ipcss.buildWeb=true
ipcss.buildDocker=false
ipcss.buildAndroidNative=false
```

---

## 🔍 Следующие шаги

1. Создать `scripts/build-pipeline.sh` и `scripts/build-pipeline.ps1` на основе анализа
2. Обновить `validate_artifacts()` с реальными путями
3. Заменить Gradle tasks на существующие из `build.gradle.kts`
4. Протестировать на CI/CD перед использованием в продакшене
