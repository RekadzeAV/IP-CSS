# Анализ локализации сборки на Raspberry Pi: Jenkins + Blue Ocean

**Дата создания:** Январь 2025
**Версия проекта:** Alfa-0.0.1
**Целевые устройства:** Raspberry Pi 4 и Raspberry Pi 5

---

## Содержание

1. [Raspberry Pi 4: Jenkins + Blue Ocean](#raspberry-pi-4-jenkins--blue-ocean)
2. [Raspberry Pi 5: Jenkins + Blue Ocean (максимальная конфигурация)](#raspberry-pi-5-jenkins--blue-ocean-максимальная-конфигурация)
3. [Сравнительный анализ](#сравнительный-анализ)
4. [Рекомендации по выбору платформы](#рекомендации-по-выбору-платформы)

---

## Raspberry Pi 4: Jenkins + Blue Ocean

### Аппаратные характеристики

- **Модель:** Raspberry Pi 4 Model B
- **Процессор:** Broadcom BCM2711, Quad-core Cortex-A72 @ 1.8 GHz
- **Память:** 4 ГБ LPDDR4-3200
- **Хранилище:** M.2 SSD 256 ГБ (через USB 3.0 адаптер)
- **Сеть:** Gigabit Ethernet, Wi-Fi 802.11ac, Bluetooth 5.0
- **Порты:** 2x USB 3.0, 2x USB 2.0, 2x micro-HDMI
- **ОС:** Raspberry Pi OS x64 (Debian-based)

### Критические ограничения

#### Проблема с памятью

**Текущая конфигурация проекта:**
- `gradle.properties`: `org.gradle.jvmargs=-Xmx4096m` (4 ГБ) ❌
- Это **недопустимо** для системы с 4 ГБ RAM

**Распределение памяти (4 ГБ):**
- Система (Raspberry Pi OS): ~500 МБ
- Jenkins JVM: ~512 МБ
- Gradle JVM: **максимум 1536 МБ** (не 4096 МБ!)
- Node.js/npm: ~256 МБ
- Резерв для системы: ~700 МБ
- **Итого доступно для сборки: ~1.5 ГБ**

### Установка и настройка

#### Шаг 1: Подготовка системы

```bash
# Обновление системы
sudo apt update && sudo apt upgrade -y

# Отключение ненужных сервисов для освобождения памяти
sudo systemctl disable bluetooth
sudo systemctl disable avahi-daemon
sudo systemctl disable cups

# Настройка swap (критически важно!)
# Вариант 1: Традиционный swap на M.2 (быстро)
sudo dphys-swapfile swapoff
sudo nano /etc/dphys-swapfile
# Изменить: CONF_SWAPSIZE=2048 (2 ГБ)
sudo dphys-swapfile setup
sudo dphys-swapfile swapon

# Вариант 2: ZRAM (более эффективно)
sudo apt install zram-tools -y
sudo nano /etc/default/zramswap
# ZRAM_SIZE=1024 (1 ГБ zram)
sudo systemctl enable zramswap
sudo systemctl start zramswap
```

#### Шаг 2: Установка Java 17

```bash
# Установка OpenJDK 17
sudo apt install openjdk-17-jdk -y

# Проверка версии
java -version
javac -version

# Настройка JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64' >> ~/.bashrc
source ~/.bashrc
```

#### Шаг 3: Установка Jenkins

```bash
# Добавление репозитория Jenkins
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

# Установка Jenkins
sudo apt update
sudo apt install jenkins -y

# Настройка Jenkins для ограниченной памяти
sudo nano /etc/default/jenkins
```

**Критически важные настройки Jenkins:**

```bash
JENKINS_JAVA_OPTIONS="-Xmx512m -Xms128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.awt.headless=true"
JENKINS_USER=jenkins
JENKINS_HOME=/var/lib/jenkins
JENKINS_PORT=8080
JENKINS_LISTEN_ADDRESS=0.0.0.0
```

```bash
# Запуск Jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins
sudo systemctl status jenkins

# Получение начального пароля
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

#### Шаг 4: Установка Blue Ocean

```bash
# Через веб-интерфейс:
# 1. Управление Jenkins > Управление плагинами
# 2. Вкладка "Доступные"
# 3. Поиск "Blue Ocean"
# 4. Установка с зависимостями

# Или через CLI:
sudo jenkins-plugin-cli --plugins blueocean:latest
sudo systemctl restart jenkins
```

#### Шаг 5: Установка дополнительных зависимостей

```bash
# Node.js 20 LTS
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node --version
npm --version

# CMake и инструменты сборки
sudo apt install cmake build-essential g++ pkg-config -y

# FFmpeg и dev библиотеки (для native модулей)
sudo apt install ffmpeg libavformat-dev libavcodec-dev \
  libavutil-dev libswscale-dev libswresample-dev -y
```

#### Шаг 6: Оптимизация Gradle для Pi 4

**Создание оптимизированного gradle.properties:**

```bash
# Создание gradle.properties.pi4
cat > gradle.properties.pi4 << 'EOF'
# Оптимизированные настройки для Raspberry Pi 4 (4 ГБ RAM)
org.gradle.jvmargs=-Xmx1536m -Xms256m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC -XX:+UseStringDeduplication -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.daemon=true
org.gradle.workers.max=2

# Kotlin
kotlin.code.style=official
kotlin.incremental=true
kotlin.native.cacheKind=static
kotlin.native.ignoreDisabledTargets=true
kotlin.mpp.applyDefaultHierarchyTemplate=false

# Android
android.useAndroidX=true
android.enableJetifier=true
android.defaults.buildfeatures.buildconfig=true
android.nonTransitiveRClass=false
android.nonFinalResIds=false
android.overridePathCheck=true

# Compose
compose.kotlinCompilerExtensionVersion=1.5.3

# Version
version=Alfa-0.0.1
group=com.company.ipcamera
EOF
```

#### Шаг 7: Настройка M.2 диска

```bash
# Проверка монтирования M.2
lsblk
df -h

# Если M.2 смонтирован отдельно, настроить workspace на M.2
# Создание директории для Jenkins workspace
sudo mkdir -p /mnt/m2/jenkins-workspace
sudo chown jenkins:jenkins /mnt/m2/jenkins-workspace

# Настройка Gradle cache на M.2
sudo mkdir -p /mnt/m2/.gradle
sudo chown jenkins:jenkins /mnt/m2/.gradle
```

### Jenkinsfile для Raspberry Pi 4

```groovy
pipeline {
    agent any

    tools {
        jdk 'jdk17'
        nodejs 'node20'
    }

    environment {
        // Критически важно для Pi 4 с 4 ГБ RAM
        GRADLE_OPTS = '-Xmx1536m -Xms256m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC'
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-arm64'
        GRADLE_BUILD_OPTS = '--no-daemon --max-workers=2'
        GRADLE_USER_HOME = '/mnt/m2/.gradle'
        NODE_OPTIONS = '--max-old-space-size=1024'
    }

    options {
        timeout(time: 2, unit: 'HOURS')
        cleanWs()
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/your-org/IP-CSS.git'
                sh 'cp gradle.properties.pi4 gradle.properties || true'
            }
        }

        stage('Build Core Common') {
            steps {
                sh './gradlew :core:common:build ${GRADLE_BUILD_OPTS}'
            }
        }

        stage('Build Core Network') {
            steps {
                sh './gradlew :core:network:build ${GRADLE_BUILD_OPTS}'
            }
        }

        stage('Build Shared Module') {
            steps {
                sh './gradlew :shared:build ${GRADLE_BUILD_OPTS}'
            }
        }

        stage('Build Server API') {
            steps {
                sh './gradlew :server:api:build ${GRADLE_BUILD_OPTS}'
            }
        }

        stage('Build Web Interface') {
            steps {
                dir('server/web') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Package') {
            steps {
                archiveArtifacts artifacts: 'server/api/build/libs/*.jar', fingerprint: true
            }
        }
    }

    post {
        always {
            sh './gradlew --stop || true'
            cleanWs()
        }
    }
}
```

### Ограничения и возможности Pi 4

#### ✅ Что можно собирать:

- Core модули (common, network)
- Shared модуль
- Server API
- Web интерфейс (с ограничениями)
- Простые native библиотеки (без OpenCV/TensorFlow)

#### ⚠️ Ограничения:

- Native библиотеки с OpenCV/TensorFlow (слишком ресурсоемко)
- Android сборки (требуют много памяти)
- Параллельные сборки (максимум 2 workers)
- Время сборки: 1-2 часа для полной сборки

#### ❌ Не рекомендуется:

- Собирать Android на Pi 4
- Собирать native библиотеки с AI/ML
- Параллельные сборки нескольких проектов
- Запуск других тяжелых сервисов одновременно

---

## Raspberry Pi 5: Jenkins + Blue Ocean (максимальная конфигурация)

### Аппаратные характеристики (максимальная конфигурация)

- **Модель:** Raspberry Pi 5 Model B
- **Процессор:** Broadcom BCM2712, Quad-core Cortex-A76 @ 2.4 GHz (до 3.0 GHz с разгоном)
- **Память:** **8 ГБ LPDDR4X-4267** (максимальная конфигурация)
- **Хранилище:** M.2 SSD 256+ ГБ (через PCIe 2.0 x1, до 500 МБ/с)
- **Сеть:** Gigabit Ethernet, Wi-Fi 6 (802.11ax), Bluetooth 5.0
- **Порты:** 2x USB 3.0, 2x USB 2.0, 2x micro-HDMI, PCIe 2.0 x1
- **Графика:** VideoCore VII GPU
- **ОС:** Raspberry Pi OS x64 (Debian-based)

### Преимущества Raspberry Pi 5

#### Производительность

- **CPU:** ~2x быстрее Pi 4 (Cortex-A76 vs A72)
- **Память:** 8 ГБ vs 4 ГБ (в 2 раза больше)
- **Хранилище:** PCIe 2.0 x1 (до 500 МБ/с) vs USB 3.0 (~400 МБ/с)
- **GPU:** VideoCore VII (улучшенная производительность)

#### Возможности для сборки

- **Gradle JVM:** До 4-5 ГБ (vs 1.5 ГБ на Pi 4)
- **Параллельные сборки:** До 4-6 workers (vs 2 на Pi 4)
- **Время сборки:** ~30-60 минут (vs 1-2 часа на Pi 4)
- **Поддержка:** Native библиотеки с OpenCV (ограниченно)

### Установка и настройка

#### Шаг 1: Подготовка системы

```bash
# Обновление системы
sudo apt update && sudo apt upgrade -y

# Настройка разгона (опционально, для максимальной производительности)
sudo nano /boot/firmware/config.txt
# Добавить:
# over_voltage=2
# arm_freq=3000
# gpu_freq=750

# Настройка охлаждения (критически важно при разгоне!)
# Убедиться в наличии активного охлаждения

# Настройка swap (меньше требуется, но рекомендуется)
sudo dphys-swapfile swapoff
sudo nano /etc/dphys-swapfile
# CONF_SWAPSIZE=1024 (1 ГБ достаточно)
sudo dphys-swapfile setup
sudo dphys-swapfile swapon
```

#### Шаг 2: Установка Java 17

```bash
# Установка OpenJDK 17
sudo apt install openjdk-17-jdk -y

# Проверка версии
java -version
javac -version

# Настройка JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64' >> ~/.bashrc
source ~/.bashrc
```

#### Шаг 3: Установка Jenkins

```bash
# Добавление репозитория Jenkins
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

# Установка Jenkins
sudo apt update
sudo apt install jenkins -y

# Настройка Jenkins для Pi 5 (больше памяти доступно)
sudo nano /etc/default/jenkins
```

**Настройки Jenkins для Pi 5:**

```bash
JENKINS_JAVA_OPTIONS="-Xmx1024m -Xms256m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.awt.headless=true"
JENKINS_USER=jenkins
JENKINS_HOME=/var/lib/jenkins
JENKINS_PORT=8080
JENKINS_LISTEN_ADDRESS=0.0.0.0
```

```bash
# Запуск Jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins
sudo systemctl status jenkins
```

#### Шаг 4: Установка Blue Ocean

```bash
# Через веб-интерфейс или CLI
sudo jenkins-plugin-cli --plugins blueocean:latest
sudo systemctl restart jenkins
```

#### Шаг 5: Установка дополнительных зависимостей

```bash
# Node.js 20 LTS
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# CMake и инструменты сборки
sudo apt install cmake build-essential g++ pkg-config -y

# FFmpeg и dev библиотеки
sudo apt install ffmpeg libavformat-dev libavcodec-dev \
  libavutil-dev libswscale-dev libswresample-dev -y

# OpenCV (опционально, для native библиотек)
sudo apt install libopencv-dev python3-opencv -y
```

#### Шаг 6: Оптимизация Gradle для Pi 5

**Создание оптимизированного gradle.properties:**

```bash
# Создание gradle.properties.pi5
cat > gradle.properties.pi5 << 'EOF'
# Оптимизированные настройки для Raspberry Pi 5 (8 ГБ RAM)
org.gradle.jvmargs=-Xmx4096m -Xms512m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC -XX:+UseStringDeduplication -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.daemon=true
org.gradle.workers.max=4

# Kotlin
kotlin.code.style=official
kotlin.incremental=true
kotlin.native.cacheKind=static
kotlin.native.ignoreDisabledTargets=true
kotlin.mpp.applyDefaultHierarchyTemplate=false

# Android
android.useAndroidX=true
android.enableJetifier=true
android.defaults.buildfeatures.buildconfig=true
android.nonTransitiveRClass=false
android.nonFinalResIds=false
android.overridePathCheck=true

# Compose
compose.kotlinCompilerExtensionVersion=1.5.3

# Version
version=Alfa-0.0.1
group=com.company.ipcamera
EOF
```

#### Шаг 7: Настройка M.2 диска (PCIe)

```bash
# Проверка PCIe устройства
lsblk
lspci | grep -i nvme

# Монтирование M.2 (если не автоматически)
sudo mkdir -p /mnt/m2
sudo mount /dev/nvme0n1p1 /mnt/m2

# Настройка автоматического монтирования
sudo nano /etc/fstab
# Добавить: /dev/nvme0n1p1 /mnt/m2 ext4 defaults 0 2

# Настройка workspace на M.2
sudo mkdir -p /mnt/m2/jenkins-workspace
sudo chown jenkins:jenkins /mnt/m2/jenkins-workspace

# Настройка Gradle cache на M.2
sudo mkdir -p /mnt/m2/.gradle
sudo chown jenkins:jenkins /mnt/m2/.gradle
```

### Jenkinsfile для Raspberry Pi 5

```groovy
pipeline {
    agent any

    tools {
        jdk 'jdk17'
        nodejs 'node20'
    }

    environment {
        // Оптимизировано для Pi 5 с 8 ГБ RAM
        GRADLE_OPTS = '-Xmx4096m -Xms512m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC'
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-arm64'
        GRADLE_BUILD_OPTS = '--no-daemon --max-workers=4'
        GRADLE_USER_HOME = '/mnt/m2/.gradle'
        NODE_OPTIONS = '--max-old-space-size=2048'
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        cleanWs()
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/your-org/IP-CSS.git'
                sh 'cp gradle.properties.pi5 gradle.properties || true'
            }
        }

        stage('Build Core Modules') {
            parallel {
                stage('Core Common') {
                    steps {
                        sh './gradlew :core:common:build ${GRADLE_BUILD_OPTS}'
                    }
                }
                stage('Core Network') {
                    steps {
                        sh './gradlew :core:network:build ${GRADLE_BUILD_OPTS}'
                    }
                }
            }
        }

        stage('Build Shared Module') {
            steps {
                sh './gradlew :shared:build ${GRADLE_BUILD_OPTS}'
            }
        }

        stage('Build Server API') {
            steps {
                sh './gradlew :server:api:build ${GRADLE_BUILD_OPTS}'
            }
        }

        stage('Build Web Interface') {
            steps {
                dir('server/web') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Build Native Libraries') {
            when {
                expression { env.BUILD_NATIVE == 'true' }
            }
            steps {
                dir('native') {
                    sh 'mkdir -p build && cd build'
                    sh 'cmake .. -DCMAKE_BUILD_TYPE=Release -DENABLE_OPENCV=ON -DENABLE_TENSORFLOW=OFF'
                    sh 'cmake --build . --config Release -j4'
                }
            }
        }

        stage('Package') {
            steps {
                archiveArtifacts artifacts: 'server/api/build/libs/*.jar', fingerprint: true
                archiveArtifacts artifacts: 'server/web/.next/**/*', fingerprint: true, allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            sh './gradlew --stop || true'
            cleanWs()
        }
    }
}
```

### Возможности Pi 5

#### ✅ Что можно собирать:

- Все модули проекта (core, shared, server)
- Web интерфейс (без ограничений)
- Native библиотеки с OpenCV (ограниченно)
- Параллельные сборки модулей
- Android сборки (базовые, без полного тестирования)

#### ⚠️ Ограничения:

- Native библиотеки с TensorFlow (все еще ресурсоемко)
- Полные Android сборки с тестами (медленно)
- Одновременные сборки нескольких больших проектов

#### ✅ Преимущества:

- Время сборки: 30-60 минут (vs 1-2 часа на Pi 4)
- Параллельные сборки: до 4-6 workers
- Поддержка более сложных native библиотек
- Лучшая производительность при сборке web интерфейса

---

## Сравнительный анализ

### Таблица сравнения

| Параметр | Raspberry Pi 4 (4 ГБ) | Raspberry Pi 5 (8 ГБ) |
|----------|----------------------|----------------------|
| **CPU** | Cortex-A72 @ 1.8 GHz | Cortex-A76 @ 2.4-3.0 GHz |
| **RAM** | 4 ГБ | 8 ГБ |
| **Хранилище** | USB 3.0 M.2 (~400 МБ/с) | PCIe 2.0 M.2 (~500 МБ/с) |
| **Jenkins JVM** | 512 МБ | 1024 МБ |
| **Gradle JVM** | 1536 МБ | 4096 МБ |
| **Gradle Workers** | 2 | 4-6 |
| **Node.js Memory** | 1024 МБ | 2048 МБ |
| **Время сборки** | 1-2 часа | 30-60 минут |
| **Параллельные сборки** | Очень ограничено | Поддерживается |
| **Native + OpenCV** | ❌ Нет | ⚠️ Ограниченно |
| **Android сборки** | ❌ Нет | ⚠️ Базовые |
| **Стоимость** | ~$75 | ~$80-100 |

### Распределение памяти

#### Raspberry Pi 4 (4 ГБ):

```
┌─────────────────────────────────────┐
│ Система (Raspberry Pi OS)   500 МБ │
│ Jenkins JVM                  512 МБ │
│ Gradle JVM                 1536 МБ │
│ Node.js/npm                 256 МБ │
│ Резерв системы              700 МБ │
│ Swap/ZRAM                  1024 МБ │
└─────────────────────────────────────┘
Итого: ~4 ГБ (с swap)
```

#### Raspberry Pi 5 (8 ГБ):

```
┌─────────────────────────────────────┐
│ Система (Raspberry Pi OS)   500 МБ │
│ Jenkins JVM                 1024 МБ │
│ Gradle JVM                 4096 МБ │
│ Node.js/npm                2048 МБ │
│ Резерв системы             1000 МБ │
│ Swap (опционально)         1024 МБ │
└─────────────────────────────────────┘
Итого: ~8 ГБ (с swap)
```

---

## Рекомендации по выбору платформы

### Выбор Raspberry Pi 4 (4 ГБ)

**Подходит для:**
- ✅ Базовые сборки (core, shared, server API)
- ✅ Web интерфейс (с ограничениями)
- ✅ Небольшие проекты
- ✅ Ограниченный бюджет
- ✅ Простые CI/CD задачи

**Не подходит для:**
- ❌ Сложные native библиотеки
- ❌ Android сборки
- ❌ Параллельные сборки
- ❌ Быстрые сборки

### Выбор Raspberry Pi 5 (8 ГБ)

**Подходит для:**
- ✅ Все модули проекта
- ✅ Параллельные сборки
- ✅ Native библиотеки (ограниченно)
- ✅ Более быстрые сборки
- ✅ Более сложные CI/CD задачи

**Не подходит для:**
- ❌ Полные Android сборки с тестами
- ❌ TensorFlow native библиотеки
- ❌ Профессиональные CI/CD (лучше использовать сервер)

### Рекомендации

1. **Для проекта IP-CSS:**
   - **Raspberry Pi 4:** Подходит для базовых сборок серверных модулей
   - **Raspberry Pi 5:** Рекомендуется для полной сборки проекта

2. **Оптимизация:**
   - Использовать M.2 диск для workspace и cache
   - Настроить swap/zram
   - Использовать Gradle build cache
   - Очищать workspace после сборок

3. **Мониторинг:**
   - Настроить мониторинг температуры
   - Мониторить использование памяти
   - Логировать время сборки

4. **Охлаждение:**
   - **Pi 4:** Пассивное охлаждение обычно достаточно
   - **Pi 5:** Активное охлаждение рекомендуется (особенно при разгоне)

---

## Итоговые конфигурации

### Raspberry Pi 4: Финальная конфигурация

```bash
# Jenkins
JENKINS_JAVA_OPTIONS="-Xmx512m -Xms128m -XX:MaxMetaspaceSize=256m"

# Gradle
org.gradle.jvmargs=-Xmx1536m -Xms256m -XX:MaxMetaspaceSize=512m
org.gradle.workers.max=2

# Node.js
NODE_OPTIONS="--max-old-space-size=1024"

# Время сборки: 1-2 часа
```

### Raspberry Pi 5: Финальная конфигурация

```bash
# Jenkins
JENKINS_JAVA_OPTIONS="-Xmx1024m -Xms256m -XX:MaxMetaspaceSize=512m"

# Gradle
org.gradle.jvmargs=-Xmx4096m -Xms512m -XX:MaxMetaspaceSize=1024m
org.gradle.workers.max=4

# Node.js
NODE_OPTIONS="--max-old-space-size=2048"

# Время сборки: 30-60 минут
```

---

**Последнее обновление:** Январь 2025
**Версия документа:** 1.0

