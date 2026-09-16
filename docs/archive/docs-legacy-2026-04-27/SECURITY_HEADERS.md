# Security Headers - Документация

**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** 26 January 2026
**Статус:** ✅ Реализовано

> **📚 Связанные документы:**
> - [SECURITY_BEST_PRACTICES.md](SECURITY_BEST_PRACTICES.md) - Лучшие практики безопасности
> - [SECURITY_AUDIT_REPORT.md](SECURITY_AUDIT_REPORT.md) - Отчет аудита безопасности
> - [SECURITY_REMEDIATION_PLAN.md](SECURITY_REMEDIATION_PLAN.md) - План устранения уязвимостей

---

## 📋 Содержание

1. [Обзор](#обзор)
2. [Реализация](#реализация)
3. [Настроенные Headers](#настроенные-headers)
4. [Конфигурация](#конфигурация)
5. [Тестирование](#тестирование)
6. [Рекомендации](#рекомендации)
7. [Устранение неполадок](#устранение-неполадок)

---

## Обзор

Security Headers (заголовки безопасности) - это HTTP заголовки, которые помогают защитить веб-приложение от различных типов атак, таких как XSS, clickjacking, MIME-sniffing и другие.

В проекте IP-CSS Security Headers реализованы для двух компонентов:
- **Серверная часть (Ktor API)** - middleware для добавления заголовков
- **Веб-интерфейс (Next.js)** - конфигурация в `next.config.js`

---

## Реализация

### Серверная часть (Ktor API)

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/SecurityHeadersMiddleware.kt`

**Подключение:** Middleware автоматически подключается в `Application.kt`:

```kotlin
// Security Headers
configureSecurityHeaders()
```

**Особенности:**
- Middleware применяется ко всем HTTP ответам
- HSTS (Strict-Transport-Security) добавляется только для HTTPS соединений
- Все заголовки добавляются через `intercept(ApplicationCallPipeline.Call)`

### Веб-интерфейс (Next.js)

**Файл:** `server/web/next.config.js`

**Конфигурация:** Headers настраиваются через функцию `headers()` в конфигурации Next.js:

```javascript
async headers() {
  const isProduction = process.env.NODE_ENV === 'production';
  return [
    {
      source: '/:path*',
      headers: [ /* ... */ ]
    }
  ];
}
```

**Особенности:**
- Разные настройки для development и production
- Применяется ко всем маршрутам (`/:path*`)
- CSP настроен с учетом особенностей Next.js и React

---

## Настроенные Headers

### 1. Content-Security-Policy (CSP)

**Назначение:** Защита от XSS атак путем контроля источников загружаемых ресурсов.

#### Серверная часть (API):
```
default-src 'self';
script-src 'self' 'unsafe-inline' 'unsafe-eval';
style-src 'self' 'unsafe-inline';
img-src 'self' data: https:;
font-src 'self' data:;
connect-src 'self' ws: wss:;
frame-ancestors 'self';
base-uri 'self';
form-action 'self'
```

#### Веб-интерфейс (Next.js):
**Development:**
```
default-src 'self';
script-src 'self' 'unsafe-eval' 'unsafe-inline';
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
font-src 'self' data: https://fonts.gstatic.com;
img-src 'self' data: https: blob:;
media-src 'self' blob: http://localhost:8080 https:;
connect-src 'self' http://localhost:8080 https: ws: wss:;
frame-ancestors 'none';
base-uri 'self';
form-action 'self';
object-src 'none';
worker-src 'self' blob:
```

**Production:**
```
default-src 'self';
script-src 'self' 'strict-dynamic';
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
font-src 'self' data: https://fonts.gstatic.com;
img-src 'self' data: https: blob:;
media-src 'self' blob: http://localhost:8080 https:;
connect-src 'self' http://localhost:8080 https: ws: wss:;
frame-ancestors 'none';
base-uri 'self';
form-action 'self';
object-src 'none';
worker-src 'self' blob:;
upgrade-insecure-requests
```

**Различия:**
- В production используется `strict-dynamic` для скриптов (более строгая политика)
- В production добавлен `upgrade-insecure-requests` (автоматическое обновление HTTP до HTTPS)

### 2. X-Frame-Options

**Назначение:** Защита от clickjacking атак.

**Серверная часть (API):**
```
X-Frame-Options: SAMEORIGIN
```
- Разрешает встраивание в iframe только с того же домена

**Веб-интерфейс:**
```
X-Frame-Options: DENY
```
- Полностью запрещает встраивание в iframe

**Рекомендация:** Для веб-интерфейса используется `DENY` для максимальной защиты. Для API `SAMEORIGIN` позволяет встраивание в собственные iframe при необходимости.

### 3. X-Content-Type-Options

**Назначение:** Защита от MIME-sniffing атак.

**Значение:**
```
X-Content-Type-Options: nosniff
```

**Эффект:** Браузер не будет пытаться определить тип контента автоматически, используя только указанный в заголовке `Content-Type`.

### 4. Strict-Transport-Security (HSTS)

**Назначение:** Принудительное использование HTTPS.

**Серверная часть (API):**
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```
- Применяется только для HTTPS соединений
- `max-age=31536000` - 1 год
- `includeSubDomains` - применяется ко всем поддоменам
- `preload` - включение в HSTS preload list

**Веб-интерфейс:**
```
Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
```
- Применяется только в production
- `max-age=63072000` - 2 года

**Важно:** HSTS добавляется только для HTTPS соединений. Для HTTP соединений заголовок не отправляется.

### 5. Referrer-Policy

**Назначение:** Контроль информации, передаваемой в заголовке Referer.

**Значение:**
```
Referrer-Policy: strict-origin-when-cross-origin
```

**Поведение:**
- Внутри одного домена: передается полный URL
- При переходе на другой домен по HTTPS: передается только origin (домен)
- При переходе на другой домен по HTTP: referrer не передается

### 6. Permissions-Policy (бывший Feature-Policy)

**Назначение:** Контроль доступа к браузерным API и функциям.

**Серверная часть (API):**
```
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

**Веб-интерфейс:**
```
Permissions-Policy: camera=(), microphone=(), geolocation=(), interest-cohort=()
```

**Эффект:**
- `camera=()` - отключен доступ к камере
- `microphone=()` - отключен доступ к микрофону
- `geolocation=()` - отключен доступ к геолокации
- `interest-cohort=()` - отключен FLoC (Federated Learning of Cohorts)

**Примечание:** Для приложения IP-CSS доступ к камере может потребоваться для работы с IP-камерами. При необходимости политику можно изменить на `camera=(self)`.

### 7. X-XSS-Protection

**Назначение:** Включение встроенной защиты от XSS в старых браузерах.

**Значение:**
```
X-XSS-Protection: 1; mode=block
```

**Статус:** Заголовок устарел, но сохраняется для совместимости со старыми браузерами. Современные браузеры используют CSP для защиты от XSS.

### 8. X-DNS-Prefetch-Control

**Назначение:** Контроль DNS prefetching.

**Значение (только веб-интерфейс):**
```
X-DNS-Prefetch-Control: on
```

**Эффект:** Разрешает браузеру предварительно разрешать DNS имена для ускорения загрузки.

### 9. Expect-CT

**Назначение:** Certificate Transparency - контроль сертификатов.

**Значение (только веб-интерфейс, production):**
```
Expect-CT: max-age=86400, enforce
```

**Эффект:**
- `max-age=86400` - 1 день
- `enforce` - принудительное применение

**Статус:** Заголовок устарел (deprecated), но сохраняется для совместимости.

---

## Конфигурация

### Изменение Security Headers

#### Серверная часть (API)

Для изменения заголовков отредактируйте файл:
```
server/api/src/main/kotlin/com/company/ipcamera/server/middleware/SecurityHeadersMiddleware.kt
```

**Пример изменения CSP:**
```kotlin
call.response.headers.append(
    HttpHeaders.ContentSecurityPolicy,
    "default-src 'self'; " +
    "script-src 'self'; " +  // Убрали 'unsafe-inline' и 'unsafe-eval'
    "style-src 'self' 'unsafe-inline'; " +
    // ... остальные директивы
)
```

#### Веб-интерфейс (Next.js)

Для изменения заголовков отредактируйте файл:
```
server/web/next.config.js
```

**Пример изменения CSP:**
```javascript
{
  key: 'Content-Security-Policy',
  value: [
    "default-src 'self'",
    "script-src 'self'",  // Убрали 'unsafe-inline' и 'unsafe-eval'
    // ... остальные директивы
  ].join('; '),
}
```

### Переменные окружения

В веб-интерфейсе используется переменная `NODE_ENV` для определения окружения:
- `NODE_ENV=production` - production настройки (строгие CSP, HSTS, Expect-CT)
- `NODE_ENV=development` - development настройки (более мягкие CSP)

---

## Тестирование

### Проверка через браузер

1. Откройте DevTools (F12)
2. Перейдите на вкладку Network
3. Выберите любой запрос
4. Проверьте вкладку Headers → Response Headers

**Ожидаемые заголовки:**
- `Content-Security-Policy`
- `X-Frame-Options`
- `X-Content-Type-Options`
- `Strict-Transport-Security` (только для HTTPS)
- `Referrer-Policy`
- `Permissions-Policy`
- `X-XSS-Protection`

### Онлайн инструменты

1. **Mozilla Observatory**
   - URL: https://observatory.mozilla.org/
   - Введите URL вашего сайта
   - Получите оценку безопасности и рекомендации

2. **Security Headers**
   - URL: https://securityheaders.com/
   - Проверка всех security headers
   - Оценка по шкале A-F

3. **CSP Evaluator**
   - URL: https://csp-evaluator.withgoogle.com/
   - Анализ Content-Security-Policy
   - Выявление потенциальных проблем

### Проверка через curl

```bash
# Проверка заголовков API
curl -I http://localhost:8080/api/v1/health

# Проверка заголовков веб-интерфейса
curl -I http://localhost:3000/
```

### Автоматическое тестирование

Создайте тест для проверки заголовков:

```kotlin
// Пример теста для Ktor
@Test
fun `should include security headers in response`() {
    withTestApplication(Application::module) {
        val response = handleRequest(HttpMethod.Get, "/api/v1/health")

        assertEquals("nosniff", response.response.headers["X-Content-Type-Options"])
        assertEquals("SAMEORIGIN", response.response.headers["X-Frame-Options"])
        assertNotNull(response.response.headers["Content-Security-Policy"])
    }
}
```

---

## Рекомендации

### Для Production

1. **Ужесточить CSP:**
   - Убрать `'unsafe-inline'` и `'unsafe-eval'` из `script-src`
   - Использовать nonce или hash для inline скриптов
   - Ограничить источники изображений и медиа

2. **Настроить HSTS:**
   - Увеличить `max-age` до 1-2 лет
   - Добавить в HSTS preload list (https://hstspreload.org/)

3. **Мониторинг:**
   - Настроить алерты на нарушение CSP
   - Логировать CSP violations
   - Регулярно проверять через security scanners

### Для Development

1. **Более мягкие правила:**
   - Разрешить `'unsafe-inline'` и `'unsafe-eval'` для удобства разработки
   - Не включать HSTS (чтобы можно было использовать HTTP)

2. **Отладка CSP:**
   - Использовать `Content-Security-Policy-Report-Only` для тестирования
   - Настроить `report-uri` для сбора нарушений

### Общие рекомендации

1. **Регулярный аудит:**
   - Проверять заголовки ежемесячно
   - Обновлять CSP при добавлении новых ресурсов
   - Следить за изменениями в стандартах

2. **Документация:**
   - Документировать все изменения в CSP
   - Объяснять причины использования `'unsafe-inline'` или других директив
   - Ведение changelog для security headers

3. **Обучение команды:**
   - Обучить разработчиков работе с CSP
   - Объяснить важность security headers
   - Создать гайд по добавлению новых ресурсов

---

## Устранение неполадок

### Проблема: CSP блокирует легитимный контент

**Симптомы:**
- Изображения не загружаются
- Скрипты не выполняются
- Стили не применяются

**Решение:**
1. Проверьте консоль браузера на ошибки CSP
2. Добавьте необходимый источник в соответствующую директиву CSP
3. Используйте `Content-Security-Policy-Report-Only` для тестирования

**Пример:**
```javascript
// Если нужно разрешить загрузку изображений с любого HTTPS источника
"img-src 'self' data: https: blob:"
```

### Проблема: HSTS не работает

**Симптомы:**
- Заголовок `Strict-Transport-Security` не отправляется

**Решение:**
1. Убедитесь, что соединение использует HTTPS
2. Проверьте, что middleware правильно определяет схему
3. Для локальной разработки используйте HTTP (HSTS не нужен)

### Проблема: X-Frame-Options блокирует iframe

**Симптомы:**
- Страница не загружается в iframe
- Ошибка "Refused to display in a frame"

**Решение:**
1. Если iframe необходим, измените `X-Frame-Options` на `SAMEORIGIN`
2. Или используйте CSP `frame-ancestors` вместо `X-Frame-Options`
3. Убедитесь, что iframe загружается с того же домена

### Проблема: Permissions-Policy блокирует камеру

**Симптомы:**
- Не удается получить доступ к камере
- Ошибка "Permission denied"

**Решение:**
1. Измените `Permissions-Policy` для разрешения камеры:
   ```
   Permissions-Policy: camera=(self)
   ```
2. Или удалите директиву `camera=()` если доступ необходим

---

## Чеклист внедрения

### Перед развертыванием

- [ ] Все security headers настроены
- [ ] CSP протестирован и не блокирует легитимный контент
- [ ] HSTS настроен для production
- [ ] Headers проверены через security scanner
- [ ] Документация обновлена

### После развертывания

- [ ] Мониторинг CSP violations настроен
- [ ] Регулярные проверки через security scanners
- [ ] Логирование нарушений безопасности
- [ ] План обновления security headers

---

## Дополнительные ресурсы

- [MDN: Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [OWASP: Secure Headers](https://owasp.org/www-project-secure-headers/)
- [Mozilla Observatory](https://observatory.mozilla.org/)
- [CSP Evaluator](https://csp-evaluator.withgoogle.com/)
- [HSTS Preload](https://hstspreload.org/)

---

**Последнее обновление:** 26 January 2026
**Версия документа:** 1.0
