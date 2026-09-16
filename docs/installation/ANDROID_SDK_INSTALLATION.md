# Инструкция по установке Android SDK

## Текущий статус

**Android SDK:** ❌ НЕ УСТАНОВЛЕН

## Способы установки

### Способ 1: Android Studio (рекомендуется)

1. **Скачайте Android Studio:**
   - Перейдите на: https://developer.android.com/studio
   - Скачайте установщик для Windows

2. **Установите Android Studio:**
   - Запустите установщик
   - Следуйте инструкциям мастера установки
   - Android SDK будет установлен автоматически в: `C:\Users\<Username>\AppData\Local\Android\Sdk`

3. **Настройте переменную окружения:**
   ```powershell
   [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk", "User")
   ```

4. **Установите необходимые компоненты:**
   - Запустите Android Studio
   - Перейдите в: Settings > Appearance & Behavior > System Settings > Android SDK
   - Установите:
     - Android SDK Platform 34
     - Android SDK Build-Tools 34.0.0
     - Android SDK Platform-Tools
     - Android SDK Command-line Tools (latest)

5. **Перезапустите терминал** для применения изменений PATH

### Способ 2: Android SDK Command-line Tools (только SDK)

1. **Скачайте Command-line Tools:**
   - Перейдите на: https://developer.android.com/studio#command-tools
   - Скачайте архив для Windows

2. **Распакуйте архив:**
   - Создайте директорию: `C:\Android\sdk\cmdline-tools\latest`
   - Распакуйте содержимое архива в эту директорию

3. **Установите переменную окружения:**
   ```powershell
   [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Android\sdk", "User")
   ```

4. **Добавьте в PATH:**
   ```powershell
   $currentPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
   $newPath = "$currentPath;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools"
   [System.Environment]::SetEnvironmentVariable("Path", $newPath, "User")
   ```

5. **Перезапустите терминал** и установите компоненты:
   ```powershell
   sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```

### Способ 3: Через Chocolatey (автоматическая установка)

Если у вас установлен Chocolatey и есть права администратора:

```powershell
# Запустите PowerShell от имени администратора
choco install android-sdk -y

# Установите переменную окружения
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\ProgramData\Android\android-sdk", "User")

# Перезапустите терминал и установите компоненты
$env:ANDROID_HOME = "C:\ProgramData\Android\android-sdk"
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

## Проверка установки

После установки выполните:

```powershell
# Проверка переменной окружения
if ($env:ANDROID_HOME) {
    Write-Host "ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Green
} else {
    Write-Host "ANDROID_HOME не установлена" -ForegroundColor Red
}

# Проверка sdkmanager
$sdkManager = Join-Path $env:ANDROID_HOME "cmdline-tools\latest\bin\sdkmanager.bat"
if (Test-Path $sdkManager) {
    Write-Host "sdkmanager найден" -ForegroundColor Green
} else {
    Write-Host "sdkmanager не найден" -ForegroundColor Red
}

# Проверка установленных компонентов
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --list_installed
```

## Быстрая проверка через скрипт

Запустите скрипт проверки:

```powershell
.\scripts\check-dependencies.ps1
```

## Примечания

- Android SDK требуется **только для сборки Android приложения** (`:android:app`)
- Для сборки серверных модулей, веб-интерфейса и нативных библиотек Android SDK **не требуется**
- Размер Android SDK может быть довольно большим (несколько гигабайт)
- Рекомендуется использовать Android Studio для разработки Android приложений

## Следующие шаги

После установки Android SDK:

1. Перезапустите терминал
2. Проверьте установку: `.\scripts\check-dependencies.ps1`
3. Попробуйте собрать Android приложение:
   ```powershell
   .\gradlew.bat :android:app:assembleDebug
   ```

