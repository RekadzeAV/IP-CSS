# Lighthouse Performance Audit Plan

**Дата:** 27 April 2026  
**Задача:** 2.3.4 — Производительность и адаптивность  
**Статус:** 🟡 В процессе (~95%)

---

## 📊 План аудита производительности

### Целевые метрики (Web Vitals)

| Метрика | Целевое значение | Текущее | Статус |
|---------|------------------|---------|--------|
| **LCP (Largest Contentful Paint)** | < 2.5s | TBD | ⏳ |
| **FID (First Input Delay)** | < 100ms | TBD | ⏳ |
| **CLS (Cumulative Layout Shift)** | < 0.1 | TBD | ⏳ |
| **FCP (First Contentful Paint)** | < 1.8s | TBD | ⏳ |
| **TBT (Total Blocking Time)** | < 200ms | TBD | ⏳ |
| **Speed Index** | < 3.4s | TBD | ⏳ |

### Scores (0-100)

| Категория | Цель | Текущее | Статус |
|-----------|------|---------|--------|
| **Performance** | ≥ 90 | TBD | ⏳ |
| **Accessibility** | ≥ 90 | TBD | ⏳ |
| **Best Practices** | ≥ 90 | TBD | ⏳ |
| **SEO** | ≥ 90 | TBD | ⏳ |
| **PWA** | ≥ 90 | TBD | ⏳ |

---

## 🚀 Запуск аудита

### 1. Desktop аудит

```bash
# Основной запуск
.\scripts\lighthouse-audit.ps1

# CI режим (только JSON)
.\scripts\lighthouse-audit.ps1 -CI -OutputDir ./lighthouse-report/desktop

# Открыть отчёт автоматически
.\scripts\lighthouse-audit.ps1
```

### 2. Mobile аудит

```bash
# Мобильный режим
.\scripts\lighthouse-audit.ps1 -Mobile

# CI режим для mobile
.\scripts\lighthouse-audit.ps1 -Mobile -CI -OutputDir ./lighthouse-report/mobile
```

### 3. Полная проверка

```powershell
# Desktop + Mobile
.\scripts\lighthouse-audit.ps1 -CI -OutputDir ./lighthouse-report/desktop
.\scripts\lighthouse-audit.ps1 -Mobile -CI -OutputDir ./lighthouse-report/mobile
```

---

## 📁 Структура отчётов

```
lighthouse-report/
├── desktop/
│   ├── report.html
│   ├── report.json
│   └── README.md
└── mobile/
    ├── report.html
    ├── report.json
    └── README.md
```

---

## 🔍 Критические проблемы для исправления

### Высокий приоритет

1. **Reduce JavaScript execution time** — оптимизация бандла
2. **Eliminate render-blocking resources** — критический CSS
3. **Properly size images** — WebP, lazy loading
4. **Minify JavaScript/CSS** — минификация

### Средний приоритет

1. **Reduce unused CSS** — purge unused styles
2. **Use efficient cache policy** — long cache headers
3. **Avoid enormous network payloads** — code splitting

### Низкий приоритет

1. **Preload key requests** — preload critical assets
2. **Minimize main-thread work** — web workers
3. **Reduce JavaScript payload** — tree shaking

---

## 📊 Web Vitals Thresholds

### Performance Budget (next.config.js)

```javascript
module.exports = {
  experimental: {
    optimizePackageImports: ['@mui/material', '@mui/icons-material'],
  },
  webpack: (config, { isServer }) => {
    if (!isServer) {
      // Reduce bundle size
      config.optimization = {
        ...config.optimization,
        splitChunks: {
          chunks: 'all',
          cacheGroups: {
            default: false,
            vendors: false,
            lib: {
              test: /[\\/]node_modules[\\/]/,
              priority: 20,
              enforce: true,
              name: 'libs'
            }
          }
        }
      };
    }
    return config;
  }
};
```

---

## 🧪 Оптимизации

### 1. Image Optimization

```jsx
// Использовать Next.js Image
import Image from 'next/image';

<Image
  src="/snapshot.jpg"
  alt="Snapshot"
  width={800}
  height={600}
  priority
  quality={75}
/>
```

### 2. Lazy Loading

```jsx
// Dynamic imports
import dynamic from 'next/dynamic';

const HeavyComponent = dynamic(() => import('../components/Heavy'), {
  loading: () => <p>Loading...</p>,
  ssr: false,
});
```

### 3. Code Splitting

```jsx
// Route-based splitting
// Automatic in Next.js
```

### 4. Caching Strategy

```javascript
// next.config.js
module.exports = {
  async headers() {
    return [
      {
        source: '/_next/static/:path*',
        headers: [
          {
            key: 'Cache-Control',
            value: 'public, max-age=31536000, immutable',
          },
        ],
      },
    ];
  },
};
```

---

## ✅ Checklist завершения 2.3.4

- [ ] Запустить Desktop Lighthouse audit
- [ ] Запустить Mobile Lighthouse audit
- [ ] Проанализировать результаты
- [ ] Исправить критические проблемы
- [ ] Оптимизировать bundle size
- [ ] Настроить caching
- [ ] Запустить повторный аудит
- [ ] Достичь целевых показателей
- [ ] Документация по оптимизации

---

## 📈 Прогресс

| Этап | Статус | Примечание |
|------|--------|------------|
| Скрипт аудита создан | ✅ | `scripts/lighthouse-audit.ps1` |
| CI режим готов | ✅ | JSON output для CI/CD |
| Mobile режим готов | ✅ | Mobile form factor |
| Запуск аудита | ⏳ | Pending |
| Оптимизации | ⏳ | Pending results |
| Final score ≥ 90 | ⏳ | Pending |

---

**Текущий статус:** ~95% (скрипт готов, требуется запуск и оптимизация)

**Дата обновления:** 27 April 2026
