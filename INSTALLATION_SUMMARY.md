# Резюме проверки и установки компонентов

**Версия документации:** 1.0
**Дата последнего обновления:** 27 декабря 2025

---

## Дата проверки
27 декабря 2025

## Результаты проверки

### ✅ Установлено:
- **Java 19** - JDK доступен (javac работает)
- **Gradle Wrapper** - gradlew.bat найден в проекте

### ❌ Требуется установить:

1. **Node.js 20+** - для сборки веб-интерфейса (server/web)
2. **CMake 3.15+** - для сборки нативных библиотек (native/)
3. **FFmpeg** - для нативных библиотек (опционально, но рекомендуется)
4. **C++ компилятор** - для нативных библиотек (опционально)
5. **Android SDK** - для сборки Android приложения (опционально)

## Рекомендуемый способ установки

### Вариант 1: Через Chocolatey (самый простой)

1. **Запустите PowerShell от имени администратора**

2. **Установите Chocolatey** (если не установлен):
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

3. **Установите все необходимые компоненты:**
```powershell
choco install openjdk17 nodejs-lts cmake ffmpeg -y
```

4. **Перезапустите терминал**

5. **Проверьте установку:**
```powershell
.\scripts\check-dependencies.ps1
```

### Вариант 2: Ручная установка

См. подробные инструкции в файле `QUICK_INSTALL.md`

## Созданные файлы и скрипты

1. **scripts/check-dependencies.ps1** - скрипт проверки всех зависимостей
2. **scripts/install-build-dependencies.ps1** - скрипт проверки и установки (требует прав администратора)
3. **scripts/quick-install.ps1** - быстрая установка через Chocolatey (требует прав администратора)
4. **LOCAL_BUILD_REQUIREMENTS.md** - полная документация по требованиям для каждого модуля
5. **QUICK_INSTALL.md** - быстрая инструкция по установке
6. **INSTALL_INSTRUCTIONS.md** - подробные инструкции по установке

## Следующие шаги

1. Запустите PowerShell от имени администратора
2. Выполните установку через Chocolatey (см. выше)
3. Перезапустите терминал
4. Проверьте установку: `.\scripts\check-dependencies.ps1`
5. Начните сборку проекта

## Минимальная сборка (без нативных библиотек и веб-интерфейса)

Если вам не нужны нативные библиотеки и веб-интерфейс, вы можете собрать только Kotlin модули:

```powershell
.\gradlew.bat :core:common:build
.\gradlew.bat :core:network:build
.\gradlew.bat :shared:build
.\gradlew.bat :server:api:build
```

Для этого достаточно только Java и Gradle (которые уже установлены).

## Полная сборка

Для полной сборки всех компонентов потребуются все перечисленные выше инструменты.

