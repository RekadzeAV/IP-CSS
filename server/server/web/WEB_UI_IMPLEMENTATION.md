# Реализация Web UI для IP Camera Surveillance System

## Обзор

Реализован полнофункциональный веб-интерфейс для системы видеонаблюдения на базе Next.js 14 с использованием React, TypeScript, Material-UI и Redux Toolkit.

## Архитектура

### Технологический стек

- **Next.js 14** - React фреймворк с App Router
- **React 18** - UI библиотека
- **TypeScript** - типизация
- **Material-UI (MUI) v5** - компоненты UI
- **Redux Toolkit** - управление состоянием
- **Axios** - HTTP клиент
- **Notistack** - уведомления (toast)

### Структура проекта

```
server/web/
├── src/
│   ├── app/                      # Next.js App Router
│   │   ├── layout.tsx           # Корневой layout с провайдерами
│   │   ├── page.tsx             # Главная страница (редирект)
│   │   ├── login/               # Страница входа
│   │   ├── dashboard/           # Главная панель
│   │   ├── cameras/             # Страницы камер
│   │   │   ├── page.tsx        # Список камер
│   │   │   └── [id]/page.tsx   # Детали камеры
│   │   ├── events/              # Страница событий (заглушка)
│   │   ├── recordings/          # Страница записей (заглушка)
│   │   └── settings/            # Страница настроек (заглушка)
│   ├── components/              # React компоненты
│   │   ├── Layout/             # Layout с навигацией
│   │   ├── CameraCard/         # Карточка камеры
│   │   └── VideoPlayer/        # Видеоплеер
│   ├── store/                   # Redux store
│   │   ├── slices/
│   │   │   ├── authSlice.ts    # Аутентификация
│   │   │   └── camerasSlice.ts # Управление камерами
│   │   ├── index.ts            # Store конфигурация
│   │   └── hooks.ts            # Typed hooks
│   ├── services/                # API сервисы
│   │   ├── authService.ts      # Сервис аутентификации
│   │   └── cameraService.ts    # Сервис камер
│   ├── types/                   # TypeScript типы
│   │   └── index.ts            # Все типы данных
│   ├── utils/                   # Утилиты
│   │   └── api.ts              # Axios конфигурация
│   └── middleware.ts            # Next.js middleware для защиты маршрутов
├── package.json
├── tsconfig.json
├── next.config.js
└── README.md
```

## Реализованные функции

### ✅ Аутентификация

- Вход в систему (`/login`)
- Выход из системы
- Сохранение токена в localStorage
- Автоматический редирект при истечении токена
- Middleware для защиты маршрутов

**Файлы:**
- `src/app/login/page.tsx`
- `src/store/slices/authSlice.ts`
- `src/services/authService.ts`
- `src/middleware.ts`

### ✅ Главная панель (Dashboard)

- Статистика по камерам (всего, онлайн)
- Карточки с метриками
- Плейсхолдер для событий

**Файлы:**
- `src/app/dashboard/page.tsx`

### ✅ Управление камерами

#### Список камер (`/cameras`)

- Отображение всех камер в виде карточек
- Статус камеры (онлайн/офлайн/ошибка)
- Добавление новой камеры через диалог
- Удаление камеры
- Навигация к деталям камеры

**Файлы:**
- `src/app/cameras/page.tsx`
- `src/components/CameraCard/CameraCard.tsx`

#### Детали камеры (`/cameras/[id]`)

- Видеоплеер (плейсхолдер)
- Информация о камере (URL, модель, разрешение, FPS, битрейт, кодек)
- Тестирование подключения к камере
- Редактирование камеры (заглушка)

**Файлы:**
- `src/app/cameras/[id]/page.tsx`
- `src/components/VideoPlayer/VideoPlayer.tsx`

### ✅ Layout и навигация

- Боковое меню (sidebar)
- Адаптивный дизайн (mobile/desktop)
- Навигация между разделами
- Кнопка выхода

**Файлы:**
- `src/components/Layout/Layout.tsx`

### ✅ Redux Store

#### Auth Slice

- Состояние аутентификации
- Токен пользователя
- Async actions: `login`, `logout`, `fetchCurrentUser`

#### Cameras Slice

- Список камер
- Выбранная камера
- Состояние загрузки
- Ошибки
- Async actions: `fetchCameras`, `fetchCameraById`, `createCamera`, `updateCamera`, `deleteCamera`, `testConnection`, `discoverCameras`

**Файлы:**
- `src/store/slices/authSlice.ts`
- `src/store/slices/camerasSlice.ts`
- `src/store/index.ts`
- `src/store/hooks.ts`

### ✅ API интеграция

- Axios клиент с interceptors
- Автоматическое добавление токена в заголовки
- Обработка ошибок (редирект при 401)
- Типизированные API сервисы

**Файлы:**
- `src/utils/api.ts`
- `src/services/authService.ts`
- `src/services/cameraService.ts`

### ✅ Обработка ошибок

- Отображение ошибок через Alert компоненты
- Toast уведомления (Notistack)
- Loading states для всех async операций
- Обработка ошибок в Redux slices

## TypeScript типы

Все типы данных соответствуют моделям из Kotlin shared модуля:

- `Camera`, `CameraStatus`, `PTZConfig`, `CameraSettings`, etc.
- `Event`, `EventType`, `EventSeverity`
- `Recording`, `RecordingStatus`, `Quality`
- `User`, `UserRole`
- `License`, `LicenseType`, `LicenseStatus`
- `SystemSettings`, `SecuritySettings`, etc.
- API DTOs: `CameraDto`, `CreateCameraRequest`, `UpdateCameraRequest`, etc.

**Файлы:**
- `src/types/index.ts`

## API Endpoints

Веб-интерфейс использует следующие endpoints:

### Аутентификация
- `POST /api/v1/auth/login` - вход
- `POST /api/v1/auth/logout` - выход
- `POST /api/v1/auth/refresh` - обновление токена
- `GET /api/v1/users/me` - текущий пользователь

### Камеры
- `GET /api/v1/cameras` - список камер
- `GET /api/v1/cameras/{id}` - детали камеры
- `POST /api/v1/cameras` - создание камеры
- `PUT /api/v1/cameras/{id}` - обновление камеры
- `DELETE /api/v1/cameras/{id}` - удаление камеры
- `POST /api/v1/cameras/{id}/test` - тест подключения
- `GET /api/v1/cameras/discover` - обнаружение камер

## Стилизация

- Material-UI тема (dark mode по умолчанию)
- Адаптивный дизайн (responsive)
- Консистентная цветовая схема
- Иконки из Material Icons

## Следующие шаги

### ✅ Реализовано (обновлено: Декабрь 2025)

1. **Страница событий** (`/events`) ✅
   - ✅ Список событий с фильтрацией по типу, камере, важности, статусу подтверждения
   - ✅ Подтверждение событий (одиночное и массовое)
   - ✅ Статистика событий (по типам и важности)
   - ✅ Удаление событий
   - ⚠️ Real-time обновления через WebSocket (сервер готов, клиент требует интеграции)

2. **Страница записей** (`/recordings`) ✅
   - ✅ Список записей с фильтрацией по камере и датам
   - ✅ Скачивание записей
   - ✅ Экспорт записей
   - ✅ Удаление записей
   - ⚠️ Воспроизведение записей (требует видеоплеера)

3. **Страница настроек** (`/settings`) ✅
   - ✅ Системные настройки (просмотр и редактирование)
   - ✅ Управление настройками записи
   - ✅ Импорт/экспорт настроек
   - ✅ Сброс настроек к значениям по умолчанию
   - ⚠️ Управление пользователями (через API, UI требует доработки)

4. **Redux slices** ✅
   - ✅ eventsSlice - управление состоянием событий
   - ✅ recordingsSlice - управление состоянием записей
   - ✅ settingsSlice - управление настройками

5. **API сервисы** ✅
   - ✅ eventService - полный CRUD для событий
   - ✅ recordingService - управление записями
   - ✅ settingsService - управление настройками

### В разработке

1. **Видеоплеер**
   - Интеграция с медиа-сервером для трансляции RTSP потоков
   - Поддержка HLS/WebRTC
   - Управление качеством видео

2. **WebSocket интеграция**
   - Real-time обновления статуса камер
   - Real-time события
   - Уведомления

6. **Дополнительные функции**
   - Обнаружение камер в сети
   - Экспорт/импорт настроек
   - Графики и аналитика
   - Многоязычность

## Запуск проекта

```bash
cd server/web
npm install
npm run dev
```

Приложение будет доступно по адресу [http://localhost:3000](http://localhost:3000)

Для работы необходимо запустить API сервер на `http://localhost:8080/api/v1`

## Переменные окружения

Создайте файл `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/api/v1/ws
```

## Заключение

Реализована базовая структура веб-интерфейса с полной интеграцией с API, управлением состоянием через Redux, аутентификацией и основными функциями для работы с камерами. Интерфейс готов к дальнейшему развитию и добавлению новых функций.



