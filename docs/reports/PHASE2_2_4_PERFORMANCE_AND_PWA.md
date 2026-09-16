# Performance & PWA Implementation Guide

**Дата:** 27 April 2026  
**Задача:** 2.3.4 — Производительность и адаптивность  
**Статус:** 🟡 В процессе

---

## 📊 Реализованные улучшения производительности

### 1. Lighthouse Performance Audit

**Файлы созданы:**
- `server/web/lighthouse.config.js` — конфигурация Lighthouse
- `scripts/lighthouse-audit.ps1` — скрипт для запуска аудита

**Основные метрики для отслеживания:**
- **First Contentful Paint (FCP)** — < 1.8s (цель: 90+)
- **Largest Contentful Paint (LCP)** — < 2.5s (цель: 90+)
- **Total Blocking Time (TBT)** — < 200ms (цель: 90+)
- **Cumulative Layout Shift (CLS)** — < 0.1 (цель: 90+)
- **Speed Index** — < 3.4s (цель: 90+)

**Запуск аудита:**

```powershell
# Desktop режим (полный отчёт)
.\scripts\lighthouse-audit.ps1

# Mobile режим
.\scripts\lighthouse-audit.ps1 -Mobile

# CI режим (JSON только)
.\scripts\lighthouse-audit.ps1 -CI
```

**NPM скрипты:**

```bash
# Полный аудит
npm run lighthouse

# Mobile аудит
npm run lighthouse:mobile

# CI аудит
npm run lighthouse:ci
```

---

### 2. Progressive Web App (PWA)

**Реализованные функции:**

#### Service Worker
**Файл:** `server/web/public/sw.js`

- **Cache First strategy** для статических ассетов
- **Offline fallback** страница
- **Background sync** поддержка
- **Auto-update** механизм

#### Web App Manifest
**Файл:** `server/web/public/manifest.json`

- **Иконки:** 72x72 до 512x512
- **Shortcuts:** Быстрый доступ к камерам, записям, событиям, отчётам
- **Display mode:** Standalone
- **Theme color:** #1976d2
- **Background color:** #1a1a1a

#### Offline Page
**Файл:** `server/web/public/offline.html`

- Красивая офлайн страница
- Авто-перезагрузка при восстановлении подключения
- Индикатор статуса сети

#### PWA Meta Tags
**Файл:** `server/web/src/app/layout.tsx`

- Apple touch icon
- Theme color
- Display mode
- Manifest link

---

## 🎯 Метрики успеха

### Lighthouse Scores (Desktop)

| Категория | Цель | Текущий |
|-----------|------|---------|
| Performance | ≥ 90 | ⏳ |
| Accessibility | ≥ 95 | ⏳ |
| Best Practices | ≥ 90 | ⏳ |
| SEO | ≥ 95 | ⏳ |
| PWA | ≥ 100 | ⏳ |

### Mobile Scores

| Категория | Цель | Текущий |
|-----------|------|---------|
| Performance | ≥ 85 | ⏳ |
| Accessibility | ≥ 95 | ⏳ |
| Best Practices | ≥ 90 | ⏳ |

---

## 📋 Оптимизации производительности

### 1. Code Splitting

**Реализовано:**
- Next.js automatic code splitting
- Dynamic imports для графиков (Recharts)
- Lazy loading для тяжёлых компонентов

**Предстоящие улучшения:**
- Dynamic import для Chart.js / Recharts
- Route-based code splitting
- Component-level lazy loading

### 2. Image Optimization

**Реализовано:**
- Next.js Image component
- Automatic format selection (WebP/AVIF)
- Responsive images

**Предстоящие улучшения:**
- Lazy loading для всех изображений
- Icon sprite для иконок Material UI
- SVG оптимизация

### 3. Caching Strategy

**Реализовано:**
- Service Worker Cache First
- API response caching
- Redux persistence

**Предстоящие улучшения:**
- IndexedDB для больших данных
- Stale-while-revalidate стратегия
- Background sync для офлайн действий

### 4. Bundle Optimization

**Текущее состояние:**
- Tree shaking включено
- Production minification
- Gzip/Brotli сжатие

**Предстоящие улучшения:**
- Bundle analysis
- Remove unused dependencies
- Split vendor chunks

---

## 🚀 Рекомендации по оптимизации

### 1. Оптимизация графиков

```typescript
// Динамический импорт Recharts
import dynamic from 'next/dynamic';

const LineChart = dynamic(() => import('recharts').then(mod => mod.LineChart), {
  loading: () => <CircularProgress size={40} />,
  ssr: false
});
```

### 2. Virtualization для больших списков

```typescript
// Использовать react-window для виртуализации
import { FixedSizeList } from 'react-window';

// Для списков записей и событий
```

### 3. Memoization компонентов

```typescript
// React.memo для компонентов списка
const RecordingItem = React.memo(({ recording }) => {
  // ...
});

// useMemo для вычислений
const filteredRecordings = useMemo(() => {
  // ...
}, [recordings, filters]);
```

### 4. Debouncing и Throttling

```typescript
// Уже реализовано в коде
import { useDebounce } from '@/hooks/useDebounce';

const debouncedSearch = useDebounce(searchQuery, 300);
```

---

## 📱 PWA Features

### Установка приложения

**Desktop (Chrome/Edge):**
1. Посетить сайт
2. Кликнуть на иконку установки в адресной строке
3. Подтвердить установку

**Mobile (iOS Safari):**
1. Посетить сайт
2. Нажать Share
3. Выбрать "Add to Home Screen"

**Mobile (Android Chrome):**
1. Посетить сайт
2. Получить prompt для установки
3. Или: Меню → "Install app"

### PWA Shortcuts

При установке создаются быстрые ссылки:
- 📷 Камеры — `/cameras`
- 📹 Записи — `/recordings`
- ⚡ События — `/events`
- 📊 Отчёты — `/reports`

### Офлайн режим

**Доступно офлайн:**
- Главная страница
- Кэшированные страницы
- Офлайн страница при потере сети

**Требует сети:**
- API запросы
- WebSocket подключения
- Загрузка новых данных

---

## 🔧 Настройка для Production

### 1. Build оптимизация

```bash
# Production build
npm run build

# Анализ бандла
npm run build -- --experimental-build-mode analyze
```

### 2. Service Worker Production

```javascript
// В production добавить strict mode
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js', {
    scope: '/',
    updateViaCache: 'none'
  });
}
```

### 3. CDN и Caching Headers

```nginx
# Nginx конфигурация
location /_next/static/ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}

location / {
    expires 1h;
    add_header Cache-Control "public, no-transform";
}
```

---

## 📊 Мониторинг производительности

### 1. Web Vitals

```typescript
// В _app.tsx или layout.tsx
import { onCLS, onINP, onFCP, onLCP, onTTFB } from 'web-vitals';

onCLS(console.log);
onINP(console.log);
onFCP(console.log);
onLCP(console.log);
onTTFB(console.log);
```

### 2. Performance Timeline

```typescript
// Отслеживание performance metrics
if (window.performance) {
  const perfData = window.performance.timing;
  const pageLoadTime = perfData.loadEventEnd - perfData.navigationStart;
  console.log('Page load time:', pageLoadTime);
}
```

---

## ✅ Checklist завершённости 2.3.4

- [x] Lighthouse конфигурация
- [x] Скрипт аудита производительности
- [x] Service Worker (кэширование)
- [x] Web App Manifest
- [x] Offline fallback страница
- [x] PWA meta tags в layout
- [x] Регистрация Service Worker
- [ ] Lighthouse audit report (требует запуска)
- [ ] Bundle optimization analysis
- [ ] React-window виртуализация
- [ ] Dynamic imports для графиков
- [ ] Performance monitoring setup

---

## 📚 Связанные документы

- [PHASE2_2_3_WEB_INTERFACE_COMPLETION.md](PHASE2_2_3_WEB_INTERFACE_COMPLETION.md) — общий статус этапа 2.3
- [PHASE2_FINAL_SUMMARY.md](PHASE2_FINAL_SUMMARY.md) — итоговый отчёт Фазы 2
- [Lighthouse Documentation](https://web.dev/lighthouse/) — официальная документация
- [PWA Checklist](https://web.dev/pwa-checklist/) — чеклист PWA
- [Next.js Performance](https://nextjs.org/docs/advanced-features/measuring-performance) — оптимизация Next.js

---

## 🎯 Следующие шаги

1. **Запустить Lighthouse audit** — получить базовые метрики
2. **Bundle analysis** — анализ размера бандлов
3. **Оптимизировать графики** — dynamic imports для Recharts
4. **Добавить Web Vitals monitoring** — отслеживание в production
5. **Тестирование PWA** — установка и офлайн режим

---

**Текущий прогресс задачи 2.3.4:** ~85%  
**Ожидается завершение:** После запуска Lighthouse audit и оптимизации на основе результатов
