# План выпуска тестовых сборок (установка, настройка, запуск)

**Версия проекта:** 0.5.1.1-beta
**Дата:** 08 August 2026

---

## Предварительно
Задать окружение (обязательно для сервера):
- `ENVIRONMENT=production`, `HTTP_PORT=8080`, `HOST=0.0.0.0`
- PostgreSQL: `DB_MODE=postgres`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- Redis: `REDIS_PASSWORD`
- `JWT_SECRET` (≥32 симв.), `ENABLE_FLYWAY=true`

## 1. Сервер (JAR)
```bash
./gradlew :server:api:installDist
server/api/build/install/api/bin/api
```
Swagger UI: `http://localhost:8080/api/v1/docs`

## 2. Docker (рекомендуемый стек)
```bash
docker-compose up -d        # nginx + api + postgres + redis
```
Проверка: `docker-compose ps`, `curl http://localhost:8080/api/v1/health`

## 3. Desktop
```bash
./gradlew :platforms:client-desktop-x86_64:app:packageDistributionForCurrentOS
```
Артефакты: MSI (Windows), DEB (Linux), DMG (macOS).

## 4. Android (APK)
```bash
./gradlew :android:app:assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx12g -XX:MaxMetaspaceSize=2g"
```
Установка: `adb install app-debug.apk`. Требуется `google-services.json` для FCM.

## 5. iOS (только macOS)
Собрать проект из `platforms/client-ios` через Xcode.

## 6. NAS
```bash
./scripts/build-nas-packages.sh all   # SPK/QPKG/APK
```

## Чеклист после установки
- [ ] Health-check отвечает
- [ ] Авторизация (JWT/OAuth2) работает
- [ ] Камеры добавляются/обнаруживаются (ONVIF/WS-Discovery)
- [ ] HLS-стримы открываются
- [ ] Записи/события пишутся в PostgreSQL/Redis
