# План устранения недочетов для тестового релиза Synology NAS

**Дата создания:** 28 января 2026
**Версия проекта:** Alfa-0.0.1
**Цель:** Устранение всех недочетов для готовности к сборке тестового релиза

---

## 📋 ОБЩАЯ СТРУКТУРА ПЛАНА

План разделен на 3 приоритетных блока:
1. **КРИТИЧЕСКИЕ ПРОБЛЕМЫ** (P0) - блокируют сборку
2. **ТРЕБУЕТ ВНИМАНИЯ** (P1) - важные улучшения
3. **ЧЕКЛИСТ ПЕРЕД СБОРКОЙ** (P2) - финальные проверки

---

## 🔴 БЛОК 1: КРИТИЧЕСКИЕ ПРОБЛЕМЫ (P0)

### Задача 1.1: Создание иконок пакета

**Приоритет:** P0 - Критический
**Время:** 1-2 часа
**Статус:** ❌ Не начато

#### Шаги выполнения:

1. **Создать временные placeholder-иконки**
   - Файл: `platforms/nas-x86_64/packages/synology/icons/PACKAGE_ICON.PNG`
   - Размер: 72x72 пикселей
   - Формат: PNG с прозрачностью
   - Содержание: Простая иконка с текстом "IP-CSS" или логотип

2. **Создать большую иконку**
   - Файл: `platforms/nas-x86_64/packages/synology/icons/PACKAGE_ICON_256.PNG`
   - Размер: 256x256 пикселей
   - Формат: PNG с прозрачностью
   - Содержание: Та же иконка в высоком разрешении

3. **Создать иконки для ARM платформы**
   - Скопировать те же иконки в `platforms/nas-arm/packages/synology/icons/`

#### Инструменты для создания:
- **Вариант 1 (быстрый):** Использовать ImageMagick для создания простых placeholder'ов
- **Вариант 2 (рекомендуемый):** Создать иконки в графическом редакторе (GIMP, Inkscape)

#### Команды для создания placeholder'ов (ImageMagick):
```bash
# Создать иконку 72x72
convert -size 72x72 xc:transparent -fill "#2196F3" -draw "roundrectangle 0,0 72,72 10,10" -pointsize 20 -fill white -gravity center -annotate +0+0 "IP-CSS" platforms/nas-x86_64/packages/synology/icons/PACKAGE_ICON.PNG

# Создать иконку 256x256
convert -size 256x256 xc:transparent -fill "#2196F3" -draw "roundrectangle 0,0 256,256 30,30" -pointsize 60 -fill white -gravity center -annotate +0+0 "IP-CSS" platforms/nas-x86_64/packages/synology/icons/PACKAGE_ICON_256.PNG
```

---

### Задача 1.2: Обновление скрипта сборки для включения иконок

**Приоритет:** P0 - Критический
**Время:** 30 минут
**Статус:** ❌ Не начато

#### Файл для изменения:
- `scripts/build-nas-package.sh`

#### Изменения:

1. **Добавить копирование иконок в функцию `build_synology_package()`**
   ```bash
   # После строки 57 (mkdir -p "$BUILD_DIR/package/scripts")
   # Добавить:

   # Copy icons if they exist
   if [ -d "$PACKAGE_DIR/icons" ]; then
       mkdir -p "$BUILD_DIR/icons"
       cp "$PACKAGE_DIR/icons"/*.PNG "$BUILD_DIR/icons/" 2>/dev/null || true
       cp "$PACKAGE_DIR/icons"/*.png "$BUILD_DIR/icons/" 2>/dev/null || true
   fi
   ```

2. **Обновить создание SPK файла для включения иконок**
   ```bash
   # Заменить строку 80:
   # Было: tar -czf "$OUTPUT_FILE" INFO package.tgz
   # Стало:
   if [ -d "$BUILD_DIR/icons" ] && [ "$(ls -A $BUILD_DIR/icons 2>/dev/null)" ]; then
       tar -czf "$OUTPUT_FILE" INFO package.tgz -C "$BUILD_DIR" icons
   else
       echo -e "${YELLOW}Warning: Icons not found, creating package without icons${NC}"
       tar -czf "$OUTPUT_FILE" INFO package.tgz
   fi
   ```

3. **Добавить проверку наличия иконок перед сборкой**
   ```bash
   # В начале функции build_synology_package(), после строки 48:
   # Check if icons exist
   if [ ! -f "$PACKAGE_DIR/icons/PACKAGE_ICON.PNG" ] || [ ! -f "$PACKAGE_DIR/icons/PACKAGE_ICON_256.PNG" ]; then
       echo -e "${YELLOW}Warning: Package icons not found. Package will be created without icons.${NC}"
       echo -e "${YELLOW}Expected files:${NC}"
       echo -e "${YELLOW}  - $PACKAGE_DIR/icons/PACKAGE_ICON.PNG${NC}"
       echo -e "${YELLOW}  - $PACKAGE_DIR/icons/PACKAGE_ICON_256.PNG${NC}"
   fi
   ```

---

### Задача 1.3: Сборка артефактов перед созданием пакета

**Приоритет:** P0 - Критический
**Время:** 20-30 минут
**Статус:** ❌ Не начато

#### Шаги выполнения:

1. **Собрать API сервер**
   ```bash
   ./gradlew :server:api:build --no-daemon
   ```
   - Проверить наличие файла: `server/api/build/libs/server-api-*.jar`
   - Если файл отсутствует, исправить ошибки сборки

2. **Собрать веб-интерфейс**
   ```bash
   cd server/web
   npm install
   npm run build
   cd ../..
   ```
   - Проверить наличие директории: `server/web/.next`
   - Если директория отсутствует, исправить ошибки сборки

3. **Добавить автоматическую сборку в скрипт `build-nas-package.sh`**

   Файл: `scripts/build-nas-package.sh`

   Изменить секцию "Build the server and web components first" (строки 32-38):
   ```bash
   # Build the server and web components first
   echo -e "${YELLOW}Building server components...${NC}"
   cd "$PROJECT_ROOT"

   # Build API server
   if [ ! -f "$PROJECT_ROOT/server/api/build/libs/server-api-"*.jar ]; then
       echo -e "${YELLOW}API server JAR not found, building...${NC}"
       ./gradlew :server:api:build --no-daemon
   else
       echo -e "${GREEN}API server JAR already exists, skipping build${NC}"
   fi

   # Build web interface
   if [ ! -d "$PROJECT_ROOT/server/web/.next" ]; then
       echo -e "${YELLOW}Web interface not found, building...${NC}"
       cd "$PROJECT_ROOT/server/web"
       if [ ! -d "node_modules" ]; then
           echo -e "${YELLOW}Installing npm dependencies...${NC}"
           npm install
       fi
       npm run build
       cd "$PROJECT_ROOT"
   else
       echo -e "${GREEN}Web interface already built, skipping build${NC}"
   fi
   ```

---

### Задача 1.4: Улучшение обработки отсутствия веб-интерфейса

**Приоритет:** P0 - Критический
**Время:** 15 минут
**Статус:** ❌ Не начато

#### Файлы для изменения:
- `scripts/build-nas-package.sh` (функция `build_synology_package()`)
- `platforms/nas-x86_64/packages/synology/package/bin/start.sh`

#### Изменения:

1. **В `build_synology_package()` сделать веб-интерфейс опциональным**
   ```bash
   # Заменить строку 62:
   # Было: cp -r "$PROJECT_ROOT/server/web/.next" "$BUILD_DIR/package/web/dist" 2>/dev/null || true
   # Стало:
   if [ -d "$PROJECT_ROOT/server/web/.next" ]; then
       cp -r "$PROJECT_ROOT/server/web/.next" "$BUILD_DIR/package/web/dist"
       echo -e "${GREEN}Web interface included${NC}"
   else
       echo -e "${YELLOW}Warning: Web interface not found, package will include only API server${NC}"
       mkdir -p "$BUILD_DIR/package/web/dist"
   fi
   ```

2. **В `start.sh` улучшить обработку отсутствия веб-интерфейса**
   ```bash
   # Заменить строки 82-95 на:
   # Start Web server if Next.js dist exists
   if [ -d "$INSTALL_DIR/web/dist" ] && [ -f "$INSTALL_DIR/web/dist/server.js" ]; then
       cd "$INSTALL_DIR/web/dist"
       export NEXT_PUBLIC_API_URL="http://localhost:8081"
       nohup node server.js \
           --port 8080 \
           >> "$LOG_FILE" 2>&1 &

       WEB_PID=$!
       echo "$WEB_PID" >> "$PID_FILE"
       echo "IP-CSS started (API PID: $API_PID, Web PID: $WEB_PID)"
   elif [ -d "$INSTALL_DIR/web/dist" ]; then
       echo "IP-CSS API started (PID: $API_PID)"
       echo "Warning: Web UI directory exists but server.js not found. Only API is running."
   else
       echo "IP-CSS API started (PID: $API_PID)"
       echo "Info: Web UI not included in package. Only API server is running."
       echo "Info: Access API at http://your-nas-ip:8081"
   fi
   ```

---

## 🟠 БЛОК 2: ТРЕБУЕТ ВНИМАНИЯ (P1)

### Задача 2.1: Синхронизация версии в INFO файлах

**Приоритет:** P1 - Высокий
**Время:** 10 минут
**Статус:** ❌ Не начато

#### Файлы для изменения:
- `platforms/nas-x86_64/packages/synology/INFO`
- `platforms/nas-arm/packages/synology/INFO`
- `scripts/build-nas-package.sh` (улучшить синхронизацию)

#### Изменения:

1. **Обновить скрипт сборки для чтения версии из gradle.properties**

   В `scripts/build-nas-package.sh`, в начале функции `build_synology_package()`:
   ```bash
   # Read version from gradle.properties if not provided
   if [ -z "$VERSION" ] || [ "$VERSION" = "Alfa-0.0.1" ]; then
       if [ -f "$PROJECT_ROOT/gradle.properties" ]; then
           GRADLE_VERSION=$(grep "^version=" "$PROJECT_ROOT/gradle.properties" | cut -d'=' -f2)
           if [ -n "$GRADLE_VERSION" ]; then
               VERSION="$GRADLE_VERSION"
               echo -e "${GREEN}Using version from gradle.properties: $VERSION${NC}"
           fi
       fi
   fi
   ```

2. **Проверить соответствие версий в INFO файлах**
   - Убедиться, что версия в INFO файлах соответствует версии в `gradle.properties`
   - Текущая версия: `Alfa-0.0.1` (должна совпадать)

---

### Задача 2.2: Добавление проверки Node.js в preinst

**Приоритет:** P1 - Высокий
**Время:** 20 минут
**Статус:** ❌ Не начато

#### Файлы для изменения:
- `platforms/nas-x86_64/packages/synology/scripts/preinst`
- `platforms/nas-arm/packages/synology/scripts/preinst`

#### Изменения:

Добавить проверку Node.js после проверки Java (после строки 32):
```bash
# Check Node.js version (required for web interface, optional)
if command -v node > /dev/null 2>&1; then
    NODE_VERSION=$(node -v | sed 's/v//' | cut -d'.' -f1)
    if [ "$NODE_VERSION" -lt 18 ]; then
        echo "Warning: Node.js 18 or later is recommended for web interface. Found Node.js $(node -v)"
        echo "Web interface may not work correctly with Node.js $(node -v)"
    else
        echo "Node.js $(node -v) found - OK"
    fi
else
    echo "Warning: Node.js is not installed. Web interface will not be available."
    echo "To install Node.js, use Synology Package Center or install manually."
    echo "Package will install, but only API server will be available."
fi
```

**Примечание:** Сделать проверку предупреждением, а не ошибкой, так как веб-интерфейс опционален.

---

### Задача 2.3: Улучшение проверки версии Java

**Приоритет:** P1 - Высокий
**Время:** 15 минут
**Статус:** ❌ Не начато

#### Файлы для изменения:
- `platforms/nas-x86_64/packages/synology/scripts/preinst`
- `platforms/nas-arm/packages/synology/scripts/preinst`

#### Изменения:

Улучшить проверку версии Java (заменить строки 22-32):
```bash
# Check Java version (required for Ktor server)
if ! command -v java > /dev/null 2>&1; then
    echo "Error: Java is not installed. Please install Java 17 or later."
    echo "You can install Java from Synology Package Center (Java Manager) or manually."
    exit 1
fi

# Get Java version more reliably
JAVA_VERSION_OUTPUT=$(java -version 2>&1)
JAVA_VERSION=$(echo "$JAVA_VERSION_OUTPUT" | head -n 1 | sed -n 's/.*version "\([0-9]*\)\..*/\1/p')

if [ -z "$JAVA_VERSION" ]; then
    # Try alternative method
    JAVA_VERSION=$(echo "$JAVA_VERSION_OUTPUT" | grep -oP 'version "?\K[0-9]+' | head -1)
fi

if [ -z "$JAVA_VERSION" ] || [ "$JAVA_VERSION" -lt 17 ]; then
    if [ -n "$JAVA_VERSION" ]; then
        echo "Error: Java 17 or later is required. Found Java $JAVA_VERSION"
    else
        echo "Error: Could not determine Java version. Please ensure Java 17+ is installed."
    fi
    echo "Current Java installation:"
    java -version 2>&1
    exit 1
fi

echo "Java $JAVA_VERSION found - OK"
```

---

### Задача 2.4: Исправление дублирования полей в INFO файле

**Приоритет:** P1 - Высокий
**Время:** 5 минут
**Статус:** ❌ Не начато

#### Файлы для изменения:
- `platforms/nas-x86_64/packages/synology/INFO`
- `platforms/nas-arm/packages/synology/INFO`

#### Проблема:
В INFO файлах есть дублирующиеся поля:
- `instuninst_restart_services=""` (строки 21 и 23)
- `install_dep_services=""` (строки 17 и 25)
- `start_dep_services=""` (строки 20 и 26)
- `startable="yes"` (строки 8 и 27)

#### Изменения:

Удалить дублирующиеся строки, оставить только первые вхождения:
```bash
package="ip-css"
version="Alfa-0.0.1"
displayname="IP Camera Surveillance System"
arch="x86_64"  # или "armv8" для ARM
maintainer="Company"
description="Кроссплатформенная система видеонаблюдения с IP-камер. Поддержка RTSP потоков, запись видео, AI-аналитика."
firmware="7.0-"
startable="yes"
support_center="no"
support_url="https://github.com/company/ip-css"
thirdparty="yes"
distributor="Company"
distributor_url="https://github.com/company/ip-css"
package_icon="PACKAGE_ICON.PNG"
package_icon_256="PACKAGE_ICON_256.PNG"
install_dep_packages=""
install_dep_services=""
install_conflict_packages=""
install_replace_packages=""
start_dep_services=""
instuninst_restart_services=""
instuninst_restart_httpd="yes"
install_type="package"
status="beta"
```

---

## 🟡 БЛОК 3: ЧЕКЛИСТ ПЕРЕД СБОРКОЙ (P2)

### Задача 3.1: Проверка прав доступа на скрипты

**Приоритет:** P2 - Средний
**Время:** 5 минут
**Статус:** ❌ Не начато

#### Файлы для проверки:
- `platforms/nas-x86_64/packages/synology/package/bin/*.sh`
- `platforms/nas-x86_64/packages/synology/scripts/*`
- `platforms/nas-arm/packages/synology/package/bin/*.sh`
- `platforms/nas-arm/packages/synology/scripts/*`

#### Действия:

1. **Проверить права доступа:**
   ```bash
   # Проверить текущие права
   ls -la platforms/nas-x86_64/packages/synology/package/bin/
   ls -la platforms/nas-x86_64/packages/synology/scripts/
   ```

2. **Установить права выполнения:**
   ```bash
   chmod +x platforms/nas-x86_64/packages/synology/package/bin/*.sh
   chmod +x platforms/nas-x86_64/packages/synology/scripts/*
   chmod +x platforms/nas-arm/packages/synology/package/bin/*.sh
   chmod +x platforms/nas-arm/packages/synology/scripts/*
   ```

3. **Добавить в скрипт сборки автоматическую установку прав:**
   В `scripts/build-nas-package.sh`, в функции `build_synology_package()`, после строки 67:
   ```bash
   # Ensure all scripts are executable
   find "$BUILD_DIR/package" -name "*.sh" -type f -exec chmod +x {} \;
   ```

---

### Задача 3.2: Валидация путей в скриптах

**Приоритет:** P2 - Средний
**Время:** 15 минут
**Статус:** ❌ Не начато

#### Файлы для проверки:
- `platforms/nas-x86_64/packages/synology/package/bin/start.sh`
- `platforms/nas-x86_64/packages/synology/package/bin/stop.sh`
- `platforms/nas-x86_64/packages/synology/package/bin/detect-nas-paths.sh`

#### Действия:

1. **Проверить все пути в скриптах на корректность**
2. **Добавить проверки существования директорий**
3. **Улучшить обработку ошибок**

Пример улучшения для `start.sh`:
```bash
# После строки 30 (mkdir -p "$LOGS_DIR")
# Добавить проверки:

# Verify directories were created
if [ ! -d "$LOGS_DIR" ]; then
    echo "Error: Failed to create logs directory: $LOGS_DIR"
    exit 1
fi

if [ ! -d "$DATA_DIR/db" ]; then
    echo "Error: Failed to create database directory: $DATA_DIR/db"
    exit 1
fi
```

---

### Задача 3.3: Создание инструкции по установке SPK пакета

**Приоритет:** P2 - Средний
**Время:** 30 минут
**Статус:** ❌ Не начато

#### Файл для создания:
- `platforms/nas-x86_64/packages/synology/INSTALLATION.md`
- `platforms/nas-arm/packages/synology/INSTALLATION.md`

#### Содержание инструкции:

```markdown
# Установка IP-CSS на Synology NAS

## Системные требования

- Synology DSM 7.0 или выше
- Java 17 или выше
- Node.js 18+ (опционально, для веб-интерфейса)
- Минимум 500MB свободного места
- Минимум 2GB RAM

## Подготовка

1. Установите Java через Package Center:
   - Откройте Package Center
   - Найдите "Java Manager" или установите Java вручную

2. (Опционально) Установите Node.js для веб-интерфейса

## Установка пакета

1. Скачайте файл `.spk` пакета
2. Откройте Package Center на вашем Synology NAS
3. Нажмите "Ручная установка"
4. Выберите скачанный `.spk` файл
5. Следуйте инструкциям установщика

## Доступ к приложению

После установки:
- **Web UI:** http://your-nas-ip:8080
- **API:** http://your-nas-ip:8081

## Устранение неполадок

### Ошибка: "Java is not installed"
Решение: Установите Java через Package Center

### Ошибка: "Insufficient disk space"
Решение: Освободите минимум 500MB на системном разделе

### Веб-интерфейс не доступен
Решение: Убедитесь, что Node.js установлен и порт 8080 не занят

## Удаление

1. Откройте Package Center
2. Найдите "IP Camera Surveillance System"
3. Нажмите "Удалить"
```

---

### Задача 3.4: Создание тестового SPK пакета

**Приоритет:** P2 - Средний
**Время:** 10 минут
**Статус:** ❌ Не начато

#### Действия:

1. **Выполнить сборку пакета:**
   ```bash
   ./scripts/build-nas-package.sh synology x86_64 Alfa-0.0.1
   ```

2. **Проверить созданный файл:**
   ```bash
   ls -lh build/ip-css-Alfa-0.0.1-synology-x86_64.spk
   ```

3. **Проверить структуру SPK пакета:**
   ```bash
   # Создать временную директорию
   mkdir -p /tmp/spk-test
   cd /tmp/spk-test

   # Распаковать SPK
   tar -xzf /path/to/ip-css-Alfa-0.0.1-synology-x86_64.spk

   # Проверить содержимое
   ls -la
   cat INFO
   tar -tzf package.tgz | head -20
   ```

4. **Проверить наличие всех необходимых файлов:**
   - INFO файл
   - package.tgz с содержимым
   - Иконки (если включены)

---

## 📊 СВОДНАЯ ТАБЛИЦА ЗАДАЧ

| ID | Задача | Приоритет | Время | Статус |
|----|--------|-----------|------|--------|
| 1.1 | Создание иконок пакета | P0 | 1-2ч | ❌ |
| 1.2 | Обновление скрипта сборки для иконок | P0 | 30мин | ❌ |
| 1.3 | Сборка артефактов | P0 | 20-30мин | ❌ |
| 1.4 | Улучшение обработки веб-интерфейса | P0 | 15мин | ❌ |
| 2.1 | Синхронизация версии | P1 | 10мин | ❌ |
| 2.2 | Проверка Node.js в preinst | P1 | 20мин | ❌ |
| 2.3 | Улучшение проверки Java | P1 | 15мин | ❌ |
| 2.4 | Исправление дублирования в INFO | P1 | 5мин | ❌ |
| 3.1 | Проверка прав доступа | P2 | 5мин | ❌ |
| 3.2 | Валидация путей | P2 | 15мин | ❌ |
| 3.3 | Инструкция по установке | P2 | 30мин | ❌ |
| 3.4 | Создание тестового пакета | P2 | 10мин | ❌ |

**Общее время:** ~4-5 часов

---

## 🚀 ПОРЯДОК ВЫПОЛНЕНИЯ

### Этап 1: Критические проблемы (P0) - ~2.5 часа
1. Задача 1.1: Создание иконок (1-2ч)
2. Задача 1.2: Обновление скрипта сборки (30мин)
3. Задача 1.3: Сборка артефактов (20-30мин)
4. Задача 1.4: Улучшение обработки веб-интерфейса (15мин)

### Этап 2: Требует внимания (P1) - ~50 минут
5. Задача 2.1: Синхронизация версии (10мин)
6. Задача 2.2: Проверка Node.js (20мин)
7. Задача 2.3: Улучшение проверки Java (15мин)
8. Задача 2.4: Исправление дублирования (5мин)

### Этап 3: Чеклист перед сборкой (P2) - ~1 час
9. Задача 3.1: Проверка прав доступа (5мин)
10. Задача 3.2: Валидация путей (15мин)
11. Задача 3.3: Инструкция по установке (30мин)
12. Задача 3.4: Создание тестового пакета (10мин)

---

## ✅ КРИТЕРИИ ГОТОВНОСТИ

После выполнения всех задач проект будет готов к сборке тестового релиза, если:

- ✅ Иконки созданы и включены в пакет
- ✅ Все артефакты собраны (API сервер и веб-интерфейс)
- ✅ Скрипт сборки обновлен и протестирован
- ✅ INFO файлы исправлены и синхронизированы
- ✅ Скрипты установки проверяют все зависимости
- ✅ Тестовый SPK пакет успешно создан
- ✅ Документация по установке создана

---

## 📝 ПРИМЕЧАНИЯ

1. **Иконки:** Для тестового релиза можно использовать простые placeholder'ы. Для production релиза потребуются профессиональные иконки.

2. **Веб-интерфейс:** Сделать опциональным, чтобы пакет мог работать только с API сервером.

3. **Тестирование:** После создания пакета рекомендуется протестировать его на виртуальной машине Synology или реальном устройстве.

4. **Версия:** Убедиться, что версия во всех файлах синхронизирована.

---

**Дата создания плана:** 28 января 2026
**Последнее обновление:** 28 января 2026
