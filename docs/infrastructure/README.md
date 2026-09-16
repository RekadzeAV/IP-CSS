# Инфраструктура CI/CD Pipeline

**Дата создания:** 26 January 2026
**Версия проекта:** Alfa-0.0.1

---

## Описание

Этот каталог содержит документацию по настройке инфраструктуры для CI/CD процессов проекта IP-CSS.

## 📚 Основной документ

### [INFRASTRUCTURE_ANALYSIS.md](INFRASTRUCTURE_ANALYSIS.md) ⭐

**Комплексный анализ инфраструктуры** - объединенный документ, содержащий все исследования и анализы:

- ✅ Анализ Git зеркала на Synology RS2416+
- ✅ Анализ Jenkins на Raspberry Pi (Pi 4 и Pi 5)
- ✅ Анализ сервера логов на Raspberry Pi 4
- ✅ Сравнительный анализ платформ
- ✅ Рекомендации по архитектуре инфраструктуры
- ✅ План внедрения

**Рекомендуется начать с этого документа для получения полного представления об инфраструктуре.**

---

## Детальные документы

### 1. [RASPBERRY_PI_JENKINS_ANALYSIS.md](RASPBERRY_PI_JENKINS_ANALYSIS.md)

Детальный анализ локализации сборки на Raspberry Pi с использованием Jenkins + Blue Ocean:

- **Raspberry Pi 4** (4 ГБ RAM, M.2 256 ГБ) - настройка для ограниченных ресурсов
- **Raspberry Pi 5** (8 ГБ RAM, максимальная конфигурация) - настройка для максимальной производительности
- Сравнительный анализ платформ
- Рекомендации по выбору платформы
- Оптимизированные конфигурации Jenkins и Gradle
- Примеры Jenkinsfile

### 2. [SYNOLOGY_GIT_MIRROR_ANALYSIS.md](SYNOLOGY_GIT_MIRROR_ANALYSIS.md)

Детальный анализ создания копии репозитория из GitHub в Git на Synology RS2416+ (DSM 7.2):

- Установка Git Server на DSM 7.2
- Создание зеркала репозитория GitHub
- Настройка автоматической синхронизации
- Настройка доступа и безопасности
- Резервное копирование
- Мониторинг и обслуживание
- Устранение неполадок

### 3. ../Log-server/RASPBERRY_PI_4_ANALYSIS.md *(утерян/в архиве)*

Детальный анализ создания сервера сбора, хранения и анализа логов на Raspberry Pi 4:

- Сравнение технологий (ELK, Loki, Graylog и др.)
- Рекомендуемая архитектура (Loki + Promtail + Grafana)
- Оценка производительности
- План реализации
- Риски и ограничения

---

## Быстрый доступ

- 📊 **[INFRASTRUCTURE_ANALYSIS.md](INFRASTRUCTURE_ANALYSIS.md)** ⭐ - Комплексный анализ (НАЧНИТЕ ОТСЮДА)
- 📦 **[RASPBERRY_PI_JENKINS_ANALYSIS.md](RASPBERRY_PI_JENKINS_ANALYSIS.md)** - Настройка Jenkins на Raspberry Pi
- 🔄 **[SYNOLOGY_GIT_MIRROR_ANALYSIS.md](SYNOLOGY_GIT_MIRROR_ANALYSIS.md)** - Настройка Git зеркала на Synology
- 📝 **../Log-server/RASPBERRY_PI_4_ANALYSIS.md *(утерян/в архиве)*** - Настройка сервера логов

---

## Структура документации

```
Inf-pipeline/
├── README.md                              # Этот файл
├── INFRASTRUCTURE_ANALYSIS.md            # ⭐ Объединенный анализ
├── RASPBERRY_PI_JENKINS_ANALYSIS.md      # Детальный анализ Jenkins
└── SYNOLOGY_GIT_MIRROR_ANALYSIS.md       # Детальный анализ Git зеркала

Log-server/
└── RASPBERRY_PI_4_ANALYSIS.md            # Детальный анализ сервера логов
```

---

**Последнее обновление:** 26 January 2026


