# 📋 Быстрая справка по интеграционному тестированию

**Актуально с:** 27 мая 2026  
**Phase 1.4 Readiness:** 88.6%

---

## 🚀 Быстрый старт

### Запустить все проверки

```powershell
# KMP verification + Unit тесты
.\scripts\ci\verify-kmp-phase1.ps1

# Сетевой smoke-test
.\scripts\network-layer-smoke-test.ps1 -ConfigPath "config\test-cameras.local.json"

# ONVIF права
.\scripts\onvif-user-rights-check.ps1 -ConfigPath "config\test-cameras.local.json"
```

---

## 📊 Текущий статус

| Метрика | Значение | Статус |
|---|---|---|
| Unit тесты | 471 (426 ✓, 45 ⏭, 0 ✗) | ✅ |
| KMP проверки | 6/6 | ✅ |
| RTSP доступность | 7/7 (100%) | ✅ |
| HTTP доступность | 7/7 (100%) | ✅ |
| ONVIF Media+Events | 5/7 (71.4%) | ⚠️ |
| ONVIF полные права | 2/7 (28.6%) | ⚠️ |

---

## 🎯 Камеры

### Полная информация

| Камера | RTSP | HTTP | ONVIF Auth | Media+Events | PullPoint | ONVIF Права |
|---|---|---|---|---|---|---|
| 192.168.10.17 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ Нужны права |
| 192.168.10.20 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ Нужны права |
| 192.168.10.21 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ Нужны права |
| 192.168.10.22 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ Нужны права |
| 192.168.10.23 | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ OK |
| 192.168.10.24 | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ OK |
| 192.168.10.26 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ Нужны права |

### Credentials

- **Username:** `survival`
- **Password:** `1234567890qazxs`
- **RTSP URL:** `rtsp://[IP]:554/stream`
- **HTTP Ports:** 80, 8080, 443

---

## 🔧 Распространённые задачи

### Настроить ONVIF права на камере

1. Открыть веб-интерфейс камеры (http://[IP]:80)
2. Войти под `admin` / `admin_password`
3. **Настройки → Network → ONVIF → Users**
4. Выбрать пользователя `survival`
5. Включить права:
   - ✅ **Events** (GetEventProperties)
   - ✅ **PullPoint** (CreatePullPoint)
6. Сохранить и перезагрузить камеру

### Установить FFmpeg

```powershell
choco install ffmpeg
ffmpeg -version  # Проверить установку

# Запустить тесты HLS сегментации
.\gradlew.bat :core:network:desktopTest --tests "*JvmHlsSegmenterTest*"
```

### Перезапустить тесты после изменений

```powershell
# Очистить кэш и пересобрать
.\gradlew.bat clean :core:network:desktopTest --rerun-tasks --no-daemon

# Или только конкретный тест
.\gradlew.bat :core:network:desktopTest --tests "*RtspClientTest*" --no-daemon
```

---

## 📁 Полезные файлы

### Отчёты

| Файл | Описание |
|---|---|
| `docs/reports/INTEGRATION_TESTING_INDEX_2026-05-27.md` | Главный индекс всех отчётов |
| `docs/reports/SESSION_SUMMARY_2026-05-27.md` | Полный отчёт сессии |
| `docs/reports/KMP_VERIFICATION_REPORT_2026-05-27.md` | KMP verification |
| `diagnostics/network-smoke/*/` | Сетевые отчёты |
| `diagnostics/onvif-rights/*/` | Отчёты ONVIF прав |

### Конфигурации

| Файл | Описание |
|---|---|
| `config/test-cameras.local.json` | Список тестовых камер |
| `config/video-runtime-matrix.local.json` | Конфиг long-run тестов |

### Скрипты

| Файл | Описание |
|---|---|
| `scripts/ci/verify-kmp-phase1.ps1` | KMP verification |
| `scripts/network-layer-smoke-test.ps1` | Сетевой smoke-test |
| `scripts/onvif-user-rights-check.ps1` | Проверка ONVIF прав |
| `scripts/video-e2e-go-no-go.ps1` | E2E тестирование |

---

## ❓ Частые вопросы

### Q: Почему тесты падают с ошибкой подключения?

**A:** Проверьте:
1. Камера включена и доступна в сети
2. Правильность credentials в `config/test-cameras.local.json`
3. Нет ли блокировки по IP в настройках камеры

### Q: Как отключить тест, требующий оборудование?

**A:** Добавьте `@Ignore` аннотацию:
```kotlin
@Ignore("Requires FFmpeg installed")
@Test
fun testHlsSegmentation() { ... }
```

### Q: Камеры 23 и 24 показывают разные результаты

**A:** Известная проблема. Они имеют полные ONVIF права, но не поддерживают Media+Events в smoke-test. Требуется дополнительная диагностика URL endpoints.

### Q: Как добавить новую камеру?

**A:**
1. Отредактировать `config/test-cameras.local.json`
2. Добавить запись с IP, credentials, портами
3. Запустить smoke-test для проверки

---

## 🐛 Troubleshooting

### Ошибка: "Connection to tcp://[IP]:554 failed"

**Решение:**
```powershell
# Проверить доступность порта
Test-NetConnection -ComputerName [IP] -Port 554

# Проверить RTSP поток
# Использовать VLC: rtsp://[IP]:554/stream
```

### Ошибка: "401 Unauthorized" на ONVIF

**Решение:**
- Проверить credentials в конфиге
- Убедиться, что пользователь имеет права на ONVIF
- Проверить, включён ли ONVIF в настройках камеры

### Ошибка: "No enabled scenarios in config"

**Решение:**
- Отредактировать `config/video-runtime-matrix.local.json`
- Включить хотя бы один сценарий (`"enabled": true`)
- Заполнить `playlistUrls` или убрать проверку

---

## 📞 Поддержка

- **Документация:** [docs/README.md](docs/README.md)
- **Issues:** https://github.com/RekadzeAV/IP-CSS/issues
- **Полный отчёт:** [docs/reports/INTEGRATION_TESTING_INDEX_2026-05-27.md](docs/reports/INTEGRATION_TESTING_INDEX_2026-05-27.md)

---

**Последнее обновление:** 27 мая 2026  
**Версия:** 1.0
