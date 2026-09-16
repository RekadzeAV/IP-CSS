# E2E Testing Guide

**Версия:** 1.0  
**Дата:** 27 April 2026  
**Статус:** 🟢 **PRODUCTION READY**

---

## 📋 Обзор

E2E (End-to-End) тесты проверяют критические сценарии использования IP-CSS приложения от начала до конца, эмулируя действия реального пользователя в браузере.

### Что покрывают E2E тесты:

- ✅ **Login/Logout** - аутентификация и сессии
- ✅ **Camera CRUD** - создание, чтение, обновление, удаление камер
- ✅ **Recording Lifecycle** - старт, пауза, возобновление, остановка записей
- ✅ **User Management** - управление пользователями и ролями
- ✅ **Settings Management** - настройка параметров системы

### Технологии:

- **Selenium WebDriver** - автоматизация браузера
- **Chrome Driver** - headless Chrome браузер
- **Kotlin Test** - фреймворк для тестирования
- **TestApiClient** - REST API клиент для бэкенда

---

## 📦 Требования

### Системные требования:

- **OS:** Windows 10+, Linux, macOS
- **RAM:** Минимум 4 GB
- **Disk:** Минимум 2 GB свободного места

### Программные требования:

- **JDK:** 17+
- **Gradle:** 7.0+
- **Chrome Browser:** Latest version
- **ChromeDriver:** Версия соответствует Chrome

---

## ⚙️ Настройка окружения

### 1. Установка зависимостей

```bash
# Убедитесь, что установлен Chrome
chrome --version

# Установите ChromeDriver (если не используется автоматическое управление)
# Windows:
winget install Google.ChromeDriver

# Linux:
sudo apt-get install chromium-chromedriver

# macOS:
brew install --cask chromedriver
```

### 2. Настройка проекта

```bash
# Клонируйте репозиторий
git clone https://github.com/RekadzeAV/IP-CSS.git
cd IP-CSS

# Соберите проект
./gradlew build

# Установите зависимости для E2E тестов
./gradlew :platforms:client-desktop-x86_64:app:buildE2eTestDependencies
```

### 3. Запуск сервера

**Важно:** E2E тесты требуют запущенного сервера и веб-интерфейса.

```bash
# Запуск сервера API
./gradlew :server:api:run

# Запуск веб-интерфейса (в отдельной терминале)
cd server/web
npm run dev
```

**Или используйте Docker:**

```bash
docker-compose up -d
```

**Проверка готовности:**

```bash
# Проверка API
curl http://localhost:8080/api/v1/health

# Проверка веб-интерфейса
curl http://localhost:8080
```

---

## 🧪 Сценарии тестирования

### 1. Login/Logout Authentication Flow

**ID:** E2E-001  
**Приоритет:** 🔴 CRITICAL  
**Время выполнения:** ~30 секунд

**Описание:**
Проверка полного цикла аутентификации пользователя.

**Шаги:**

1. Start application
2. Navigate to login page (`/login`)
3. Enter credentials (username: `admin`, password: `admin123`)
4. Click login button
5. Verify redirect to dashboard (`/dashboard`)
6. Verify user info displayed
7. Logout
8. Verify redirect to login page

**Ожидаемый результат:**
- ✅ Пользователь успешно авторизован
- ✅ Перенаправление на dashboard
- ✅ UserInfo отображается
- ✅ При logout возвращается на login

**Тест:**
```kotlin
@Test
fun `E2E - Login authentication flow`() = runTest {
    val fixture = E2ETestFixture()
    
    try {
        fixture.startApplication()
        fixture.navigateTo("/login")
        fixture.fillLoginForm("admin", "admin123")
        fixture.clickLoginButton()
        fixture.waitForUrl("/dashboard")
        assertTrue(fixture.isUserLoggedIn())
    } finally {
        fixture.close()
    }
}
```

---

### 2. Camera CRUD Operations

**ID:** E2E-002  
**Приоритет:** 🔴 CRITICAL  
**Время выполнения:** ~60 секунд

**Описание:**
Проверка полного цикла управления камерами (Create, Read, Update, Delete).

**Шаги:**

1. Login as admin
2. Navigate to cameras page (`/cameras`)
3. Add new camera:
   - Name: `E2E Test Camera`
   - URL: `rtsp://127.0.0.1:8554/test`
   - Username: `admin`
   - Password: `password`
4. Verify camera appears in list
5. Edit camera (change name)
6. Verify camera updated
7. Delete camera
8. Verify camera removed from list

**Ожидаемый результат:**
- ✅ Камера успешно создана
- ✅ Камера отображается в списке
- ✅ Редактирование успешно
- ✅ Удаление успешно

**Тест:**
```kotlin
@Test
fun `E2E - Camera CRUD operations`() = runTest {
    val fixture = E2ETestFixture()
    
    try {
        fixture.loginAsAdmin()
        fixture.navigateTo("/cameras")
        
        val cameraName = "E2E Test Camera ${System.currentTimeMillis()}"
        fixture.addCamera(
            name = cameraName,
            url = "rtsp://127.0.0.1:8554/test",
            username = "admin",
            password = "password"
        )
        
        assertTrue(fixture.cameraExists(cameraName))
        
        fixture.editCamera(cameraName, newName = "E2E Updated Camera")
        assertTrue(fixture.cameraExists("E2E Updated Camera"))
        
        fixture.deleteCamera("E2E Updated Camera")
        assertTrue(!fixture.cameraExists("E2E Updated Camera"))
    } finally {
        fixture.close()
    }
}
```

---

### 3. Recording Lifecycle

**ID:** E2E-003  
**Приоритет:** 🟡 HIGH  
**Время выполнения:** ~15 секунд

**Описание:**
Проверка полного цикла управления записью видео.

**Шаги:**

1. Login as admin
2. Navigate to cameras page
3. Select camera
4. Start recording
5. Verify recording status = "Recording"
6. Wait 5 seconds
7. Pause recording
8. Verify recording status = "Paused"
9. Resume recording
10. Verify recording status = "Recording"
11. Stop recording
12. Verify recording saved

**Ожидаемый результат:**
- ✅ Запись успешно запущена
- ✅ Статус обновляется в реальном времени
- ✅ Пауза работает корректно
- ✅ Возобновление работает корректно
- ✅ Запись успешно сохранена

**Тест:**
```kotlin
@Test
fun `E2E - Recording lifecycle`() = runTest {
    val fixture = E2ETestFixture()
    
    try {
        fixture.loginAsAdmin()
        fixture.navigateTo("/cameras")
        
        fixture.startRecording("test-camera")
        assertTrue(fixture.isRecording("test-camera"))
        
        kotlinx.coroutines.delay(5000)
        
        fixture.pauseRecording("test-camera")
        assertTrue(fixture.isPaused("test-camera"))
        
        fixture.resumeRecording("test-camera")
        assertTrue(fixture.isRecording("test-camera"))
        
        fixture.stopRecording("test-camera")
        assertTrue(!fixture.isRecording("test-camera"))
        assertTrue(fixture.recordingExists("test-camera"))
    } finally {
        fixture.close()
    }
}
```

---

### 4. User Management

**ID:** E2E-004  
**Приоритет:** 🟡 HIGH  
**Время выполнения:** ~45 секунд

**Описание:**
Проверка полного цикла управления пользователями.

**Шаги:**

1. Login as admin
2. Navigate to users page (`/users`)
3. Create new user:
   - Username: `e2e_test_user`
   - Password: `testpass123`
   - Role: `OPERATOR`
4. Verify user appears in list
5. Edit user (change password)
6. Verify user updated
7. Delete user
8. Verify user removed from list

**Ожидаемый результат:**
- ✅ Пользователь успешно создан
- ✅ Пользователь отображается в списке
- ✅ Редактирование успешно
- ✅ Удаление успешно

**Тест:**
```kotlin
@Test
fun `E2E - User creation and management`() = runTest {
    val fixture = E2ETestFixture()
    
    try {
        fixture.loginAsAdmin()
        fixture.navigateTo("/users")
        
        val username = "e2e_test_user_${System.currentTimeMillis()}"
        fixture.createUser(
            username = username,
            password = "testpass123",
            role = "OPERATOR"
        )
        
        assertTrue(fixture.userExists(username))
        
        fixture.editUser(username, newPassword = "newpass123")
        assertTrue(fixture.userExists(username))
        
        fixture.deleteUser(username)
        assertTrue(!fixture.userExists(username))
    } finally {
        fixture.close()
    }
}
```

---

### 5. Settings Management

**ID:** E2E-005  
**Приоритет:** 🟠 MEDIUM  
**Время выполнения:** ~30 секунд

**Описание:**
Проверка управления системными настройками.

**Шаги:**

1. Login as admin
2. Navigate to settings page (`/settings`)
3. Update video settings:
   - Resolution: `1920x1080`
   - FPS: `30`
   - Bitrate: `4000kbps`
4. Save settings
5. Verify settings persisted

**Ожидаемый результат:**
- ✅ Настройки успешно обновлены
- ✅ Настройки успешно сохранены
- ✅ Настройки отображаются корректно

**Тест:**
```kotlin
@Test
fun `E2E - Settings management`() = runTest {
    val fixture = E2ETestFixture()
    
    try {
        fixture.loginAsAdmin()
        fixture.navigateTo("/settings")
        
        fixture.updateVideoSettings(
            resolution = "1920x1080",
            fps = 30,
            bitrate = "4000kbps"
        )
        
        fixture.saveSettings()
        assertTrue(fixture.getVideoSettings().resolution == "1920x1080")
    } finally {
        fixture.close()
    }
}
```

---

## 🚀 Запуск тестов

### Запуск всех E2E тестов

```bash
# Через Gradle
./gradlew :platforms:client-desktop-x86_64:app:e2eTest

# Windows
gradlew.bat :platforms:client-desktop-x86_64:app:e2eTest
```

### Запуск конкретного теста

```bash
# По имени теста
./gradlew :platforms:client-desktop-x86_64:app:e2eTest \
  --tests "com.company.ipcamera.e2e.CriticalScenariosE2ETest.Login authentication flow"

# По классу
./gradlew :platforms:client-desktop-x86_64:app:e2eTest \
  --tests "com.company.ipcamera.e2e.CriticalScenariosE2ETest"
```

### Запуск в режиме отладки

```bash
# С выводом логов браузера
./gradlew :platforms:client-desktop-x86_64:app:e2eTest \
  -Dselenium.log.level=DEBUG

# Без headless режима (для визуального контроля)
# Отредактируйте E2ETestFixture.kt:
# addArguments("--headless=new") → закомментируйте
```

---

## 📊 Результаты тестов

### Отчёт о прохождении тестов

После выполнения тестов отчёты доступны по путям:

```
platforms/client-desktop-x86_64/app/build/reports/tests/e2eTest/
├── index.html                    # Главный отчёт
├── classes/                      # Детали по классам
└── packages/                     # Детали по пакетам
```

### Пример успешного выполнения:

```
> Task :platforms:client-desktop-x86_64:app:e2eTest

BUILD SUCCESSFUL in 2m 30s
5 actionable tests:
  ✅ E2E - Login authentication flow (28s)
  ✅ E2E - Camera CRUD operations (55s)
  ✅ E2E - Recording lifecycle (12s)
  ✅ E2E - User creation and management (42s)
  ✅ E2E - Settings management (25s)

All tests passed!
Code coverage: 74%
```

---

## 🔧 Troubleshooting

### Проблема: ChromeDriver не найден

**Симптом:**
```
SessionNotCreatedException: Could not find a valid ChromeDriver
```

**Решение:**

1. Проверьте версию Chrome:
   ```bash
   chrome --version
   ```

2. Установите соответствующую версию ChromeDriver:
   ```bash
   # Windows
   winget install Google.ChromeDriver
   
   # Или скачайте с https://chromedriver.chromium.org/
   ```

3. Укажите путь к ChromeDriver:
   ```bash
   export CHROMEDRIVER_PATH=/path/to/chromedriver
   ```

---

### Проблема: Тесты падают с timeout

**Симптом:**
```
TimeoutException: Timed out waiting for element
```

**Решение:**

1. Увеличьте время ожидания в `E2ETestFixture.kt`:
   ```kotlin
   private val implicitWaitMs = 20000L // Было 10000L
   ```

2. Проверьте, что сервер запущен:
   ```bash
   curl http://localhost:8080/api/v1/health
   ```

3. Проверьте логи сервера на ошибки

---

### Проблема: Браузер не запускается в headless режиме

**Симптом:**
```
WebDriverException: unknown error: Chrome failed to start
```

**Решение:**

1. Добавьте дополнительные аргументы в `E2ETestFixture.kt`:
   ```kotlin
   val options = ChromeOptions().apply {
       addArguments("--headless=new")
       addArguments("--no-sandbox")
       addArguments("--disable-dev-shm-usage")
       addArguments("--disable-gpu")
       addArguments("--window-size=1920,1080")
       addArguments("--disable-setuid-sandbox")
   }
   ```

2. На Linux установите зависимости:
   ```bash
   sudo apt-get install -y \
     libglib2.0-0 \
     libnss3 \
     libnspr4 \
     libatk1.0-0 \
     libatk-bridge2.0-0 \
     libcups2 \
     libdrm2 \
     libdbus-1-3 \
     libxkbcommon0 \
     libxcomposite1 \
     libxdamage1 \
     libxfixes3 \
     libxrandr2 \
     libgbm1 \
     libasound2
   ```

---

### Проблема: Тесты не находят элементы

**Симптом:**
```
NoSuchElementException: Unable to find element
```

**Решение:**

1. Проверьте селекторы в приложении:
   - Откройте DevTools в браузере
   - Проверьте CSS селекторы

2. Добавьте больше wait времени:
   ```kotlin
   Thread.sleep(2000) // Wait for page load
   ```

3. Используйте более надёжные селекторы:
   ```kotlin
   // Вместо
   driver.findElement(By.cssSelector("button"))
   
   // Используйте
   driver.findElement(By.cssSelector("button[data-testid='submit']"))
   ```

---

### Проблема: Тестовые данные не очищаются

**Симптом:**
```
Duplicate entry error
```

**Решение:**

1. Запустите cleanup вручную:
   ```bash
   curl -X DELETE http://localhost:8080/api/v1/cleanup
   ```

2. Очистите базу данных:
   ```bash
   # Через SQLDelight
   ./gradlew :shared:sqlDelight
   ```

3. Проверьте метод `cleanup()` в `E2ETestFixture.kt`

---

## 🔄 CI/CD Интеграция

### GitHub Actions

```yaml
name: E2E Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Chrome
        uses: browser-actions/setup-chrome@latest
      
      - name: Start server
        run: |
          ./gradlew :server:api:run &
          sleep 30
      
      - name: Start web interface
        run: |
          cd server/web && npm install && npm run dev &
          sleep 15
      
      - name: Run E2E tests
        run: |
          ./gradlew :platforms:client-desktop-x86_64:app:e2eTest
      
      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: e2e-test-results
          path: platforms/client-desktop-x86_64/app/build/reports/tests/e2eTest/
```

---

## 📝 Поддержка и развитие

### Добавление новых тестов

1. Создайте новый тест в `CriticalScenariosE2ETest.kt`:
   ```kotlin
   @Test
   fun `E2E - My new test scenario`() = runTest {
       val fixture = E2ETestFixture()
       
       try {
           // Ваш тестовый код
       } finally {
           fixture.close()
       }
   }
   ```

2. Добавьте описание в этот документ

3. Обновите CI/CD конфигурацию

### Обновление fixture

1. Изменения в `E2ETestFixture.kt` требуют:
   - Обновления всех ссылок на элементы
   - Перепроверки всех тестов
   - Обновления документации

---

## 📚 Связанная документация

- [API.md](../API.md) - REST API документация
- [TESTING.md](../TESTING.md) - Руководство по тестированию
- [platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md](../../client-desktop-x86_64/IMPLEMENTATION_STATUS.md) - Статус реализации Desktop клиента

---

**Последнее обновление:** 27 April 2026  
**Следующий пересмотр:** 2026-05-27
