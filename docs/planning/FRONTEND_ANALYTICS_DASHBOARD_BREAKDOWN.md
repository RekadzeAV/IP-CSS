# Frontend Analytics Dashboard - Task Breakdown

**Дата:** 2026-04-27  
**Статус:** 🟡 READY FOR EXECUTION  
**Оценка:** 1-2 недели

---

## 📋 Обзор

Цель: Создать dashboard для отображения аналитики видеопотоков в реальном времени.

### Исходные данные:
- API endpoints уже реализованы (7 endpoints)
- Production Monitor интегрирован
- WebSocket для real-time обновлений

---

## 🎯 High-Level Requirements

### Функциональные требования:

1. **Real-time Metrics Display**
   - Отображение метрик в реальном времени
   - Автоматическое обновление каждые 5 секунд
   - Поддержка multi-camera view

2. **Camera Health Monitoring**
   - Статус каждой камеры (online/offline/error)
   - FPS, битрейт, качество
   - Ошибки и предупреждения

3. **Analytics Metrics**
   - Общее количество обработанных кадров
   - Количество детекций (motion, object, face)
   - Время обработки (latency)

4. **Recommendations Display**
   - Рекомендации по оптимизации
   - Alert'ы при проблемах
   - Historical trends

5. **Export & Reporting**
   - Export в CSV/JSON
   - Generate reports
   - Schedule automatic reports

### Нефункциональные требования:

- **Performance:** < 100ms latency для real-time updates
- **Scalability:** Поддержка 100+ камер
- **Responsiveness:** Mobile-friendly
- **Accessibility:** WCAG 2.1 AA

---

## 📊 Task Breakdown

### 3.1.1: Dashboard Layout и Структура

**Оценка:** 2-3 дня  
**Приоритет:** P0

#### Задачи:

1. **Project Setup** (4 часа)
   ```bash
   # Используем React + TypeScript + Vite
   npm create vite@latest analytics-dashboard -- --template react-ts
   cd analytics-dashboard
   npm install
   ```

2. **UI Framework Selection** (4 часа)
   - Выбор: Material-UI / Ant Design / Chakra UI
   - Установка и настройка
   - Theme configuration

3. **Dashboard Layout** (1 день)
   ```tsx
   // Структура компонентов
   <DashboardLayout>
     <Header />
     <Sidebar />
     <MainContent>
       <MetricsOverview />
       <CameraGrid />
       <AnalyticsCharts />
     </MainContent>
   </DashboardLayout>
   ```

4. **Routing Setup** (4 часа)
   - React Router v6
   - Protected routes
   - Route guards

5. **State Management** (4 часа)
   - Redux Toolkit / Zustand
   - API state management
   - Real-time state (WebSocket)

**Deliverables:**
- ✅ Dashboard skeleton
- ✅ Layout компоненты
- ✅ Routing setup

---

### 3.1.2: Metrics Widgets

**Оценка:** 3-4 дня  
**Приоритет:** P0

#### Widget 1: Global Metrics (4 часа)
```tsx
interface GlobalMetricsWidgetProps {
  totalCameras: number;
  onlineCameras: number;
  totalFramesProcessed: number;
  avgFps: number;
  avgLatency: number;
}

<GlobalMetricsWidget
  totalCameras={50}
  onlineCameras={48}
  totalFramesProcessed={1234567}
  avgFps={25.5}
  avgLatency={120}
/>
```

**API:** `GET /api/v1/analytics/metrics/global`

---

#### Widget 2: Camera Status Grid (1 день)
```tsx
interface CameraStatusCardProps {
  cameraId: string;
  cameraName: string;
  status: 'online' | 'offline' | 'error';
  fps: number;
  lastSeen: Date;
}

<CameraStatusGrid cameras={cameras} />
```

**Features:**
- Grid layout (responsive)
- Color coding по статусу
- Click для деталей
- Search и фильтры

**API:** `GET /api/v1/analytics/metrics/cameras`

---

#### Widget 3: FPS Timeline (4 часа)
```tsx
<FpsTimeline
  cameraId="camera-123"
  timeRange="1h"
  data={[
    { timestamp: '10:00', fps: 25.2 },
    { timestamp: '10:05', fps: 24.8 },
    // ...
  ]}
/>
```

**Features:**
- Line chart (Recharts / Chart.js)
- Zoom и pan
- Comparison mode

---

#### Widget 4: Detection Counter (4 часа)
```tsx
<DetectionCounter
  motion={1234}
  objects={567}
  faces={89}
  anomalies={12}
/>
```

**Features:**
- Animated counters
- Trend indicators (↑ ↓)
- Time range selector

---

#### Widget 5: Latency Gauge (4 часа)
```tsx
<LatencyGauge
  current={120}
  min={0}
  max={500}
  thresholds={{
    good: 100,
    warning: 200,
    critical: 500
  }}
/>
```

**Features:**
- Real-time gauge
- Color thresholds
- History overlay

---

#### Widget 6: Bandwidth Monitor (4 часа)
```tsx
<BandwidthMonitor
  totalBandwidth="2.5 GB/s"
  perCamera={cameras.map(c => ({
    id: c.id,
    bandwidth: c.estimatedBandwidth
  }))}
/>
```

**Features:**
- Real-time bandwidth usage
- Per-camera breakdown
- Predictions

---

#### Widget 7: Error Rate Chart (4 часа)
```tsx
<ErrorRateChart
  data={[
    { timestamp: '10:00', errors: 2, total: 1000 },
    { timestamp: '10:05', errors: 5, total: 1200 },
  ]}
/>
```

**Features:**
- Error rate percentage
- Spike detection
- Correlation with other metrics

---

#### Widget 8: Camera Health Score (4 часа)
```tsx
<HealthScoreCard
  cameraId="camera-123"
  score={85}
  factors={[
    { name: 'FPS', score: 90, weight: 0.3 },
    { name: 'Latency', score: 75, weight: 0.2 },
    { name: 'Uptime', score: 100, weight: 0.3 },
    { name: 'Errors', score: 60, weight: 0.2 },
  ]}
/>
```

**Features:**
- Overall health score (0-100)
- Factor breakdown
- Recommendations

---

#### Widget 9: Alert Panel (4 часа)
```tsx
<AlertPanel
  alerts={[
    {
      id: 'alert-1',
      severity: 'warning',
      message: 'Camera camera-123 FPS dropped below 15',
      timestamp: new Date(),
      acknowledged: false
    }
  ]}
  onAcknowledge={handleAcknowledge}
/>
```

**Features:**
- Real-time alerts
- Severity levels (info, warning, error, critical)
- Acknowledge mechanism
- Filter и search

---

#### Widget 10: Recommendations List (4 часа)
```tsx
<RecommendationsList
  recommendations={[
    {
      id: 'rec-1',
      priority: 'high',
      title: 'Increase bitrate for camera-123',
      description: 'Current FPS is below target',
      action: 'Adjust camera settings'
    }
  ]}
/>
```

**API:** `GET /api/v1/analytics/metrics/cameras/{id}/recommendations`

**Features:**
- Priority sorting
- Action buttons
- Dismiss mechanism

---

### 3.1.3: Charts и Графики

**Оценка:** 2-3 дня  
**Приоритет:** P1

#### Chart 1: Multi-Camera FPS Comparison (1 день)
```tsx
<MultiCameraFpsChart
  cameras={cameras}
  timeRange="24h"
  onCameraSelect={handleCameraSelect}
/>
```

**Features:**
- Multi-line chart
- Camera selection
- Time range picker
- Export chart

---

#### Chart 2: Detection Trends (4 часа)
```tsx
<DetectionTrends
  timeRange="7d"
  types={['motion', 'object', 'face']}
/>
```

**Features:**
- Stacked area chart
- Type filtering
- Comparison mode

---

#### Chart 3: Latency Distribution (4 часа)
```tsx
<LatencyDistribution
  data={latencySamples}
  bins={100}
/>
```

**Features:**
- Histogram
- Percentile markers (p50, p95, p99)
- Outlier highlighting

---

#### Chart 4: Resource Usage Over Time (4 часа)
```tsx
<ResourceUsageChart
  metrics={['cpu', 'memory', 'network']}
  timeRange="1h"
/>
```

**Features:**
- Multi-axis chart
- Metric toggles
- Correlation view

---

#### Chart 5: Camera Uptime Calendar (1 день)
```tsx
<UptimeCalendar
  cameraId="camera-123"
  month="2026-04"
/>
```

**Features:**
- Calendar heatmap
- Per-day uptime %
- Click для деталей

---

### 3.1.4: Real-time WebSocket Updates

**Оценка:** 2 дня  
**Приоритет:** P0

#### Task 1: WebSocket Connection (4 часа)
```tsx
// WebSocket hook
function useAnalyticsWebSocket() {
  const [metrics, setMetrics] = useState<AnalyticsMetrics | null>(null);
  
  useEffect(() => {
    const ws = new WebSocket('ws://localhost:8080/api/v1/analytics/stream');
    
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setMetrics(data);
    };
    
    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
    
    return () => ws.close();
  }, []);
  
  return metrics;
}
```

**Features:**
- Auto-reconnect
- Heartbeat/ping-pong
- Error handling

---

#### Task 2: Real-time Data Sync (1 день)
```tsx
// Redux slice для real-time data
const analyticsSlice = createSlice({
  name: 'analytics',
  initialState,
  reducers: {
    updateMetrics(state, action) {
      state.global = action.payload.global;
      state.cameras = action.payload.cameras;
      state.lastUpdate = new Date();
    }
  }
});
```

**Features:**
- Optimistic updates
- Conflict resolution
- Batch updates

---

#### Task 3: Update Indicators (4 часа)
```tsx
<LastUpdateIndicator
  lastUpdate={lastUpdate}
  isConnected={isConnected}
/>
```

**Features:**
- "Live" indicator
- Last update timestamp
- Connection status

---

### 3.1.5: Export и Reporting

**Оценка:** 1-2 дня  
**Приоритет:** P2

#### Export 1: CSV Export (4 часа)
```tsx
const exportToCsv = (data: AnalyticsMetrics) => {
  const csv = convertToCsv(data);
  downloadFile(csv, 'analytics-export.csv');
};
```

**Features:**
- Select time range
- Select metrics
- Auto-download

---

#### Export 2: JSON Export (4 часа)
```tsx
const exportToJson = (data: AnalyticsMetrics) => {
  const json = JSON.stringify(data, null, 2);
  downloadFile(json, 'analytics-export.json');
};
```

---

#### Export 3: PDF Report (1 день)
```tsx
const generatePdfReport = async (data: AnalyticsMetrics) => {
  const report = await generateReport(data);
  downloadFile(report, 'analytics-report.pdf');
};
```

**Features:**
- Custom templates
- Charts in PDF
- Scheduled reports

---

#### Export 4: API Integration (4 часа)
```tsx
// Export API endpoints
POST /api/v1/analytics/export/csv
POST /api/v1/analytics/export/json
POST /api/v1/analytics/export/pdf
```

---

### 3.1.6: Authentication и Authorization

**Оценка:** 1 день  
**Приоритет:** P1

#### Task 1: Login Page (4 часа)
```tsx
<LoginPage
  onLogin={handleLogin}
  error={loginError}
/>
```

**Features:**
- Email/password
- Remember me
- Forgot password

---

#### Task 2: Auth Guard (4 часа)
```tsx
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useAuth();
  
  return isAuthenticated ? children : <Navigate to="/login" />;
};
```

---

#### Task 3: Role-based Access (4 часа)
```tsx
const AdminOnly = ({ children }) => {
  const { role } = useAuth();
  
  return role === 'admin' ? children : <ForbiddenPage />;
};
```

---

### 3.1.7: Testing

**Оценка:** 2-3 дня  
**Приоритет:** P1

#### Tests 1: Unit Tests (1 день)
```tsx
describe('GlobalMetricsWidget', () => {
  it('renders correctly', () => {
    render(<GlobalMetricsWidget {...mockProps} />);
    expect(screen.getByText('50')).toBeInTheDocument();
  });
});
```

**Coverage target:** > 80%

---

#### Tests 2: Integration Tests (1 день)
```tsx
describe('Dashboard Integration', () => {
  it('loads and displays metrics', async () => {
    render(<Dashboard />);
    await waitFor(() => expect(screen.getByText('Analytics')).toBeInTheDocument());
  });
});
```

---

#### Tests 3: E2E Tests (1 день)
```tsx
describe('Dashboard E2E', () => {
  it('user can view camera metrics', () => {
    cy.visit('/dashboard');
    cy.get('[data-testid="camera-grid"]').should('be.visible');
  });
});
```

**Tools:** Cypress / Playwright

---

### 3.1.8: Performance Optimization

**Оценка:** 1 день  
**Приоритет:** P2

#### Optimization 1: Code Splitting (4 часа)
```tsx
const AnalyticsDashboard = lazy(() => import('./AnalyticsDashboard'));
```

---

#### Optimization 2: Memoization (4 часа)
```tsx
const CameraGrid = memo(({ cameras }) => {
  // ...
});
```

---

#### Optimization 3: Virtual Scrolling (4 часа)
```tsx
<FixedSizeList
  height={600}
  itemCount={cameras.length}
  itemSize={100}
>
  {({ index }) => <CameraCard camera={cameras[index]} />}
</FixedSizeList>
```

---

#### Optimization 4: Caching (4 часа)
```tsx
const queryClient = new QueryClient();

<QueryClientProvider client={queryClient}>
  <Dashboard />
</QueryClientProvider>
```

---

## 📊 Итоговая матрица задач

| Категория | Задач | Оценка (дней) | Приоритет |
|-----------|-------|---------------|-----------|
| 3.1.1: Layout | 5 | 2-3 | P0 |
| 3.1.2: Widgets | 10 | 3-4 | P0 |
| 3.1.3: Charts | 5 | 2-3 | P1 |
| 3.1.4: WebSocket | 3 | 2 | P0 |
| 3.1.5: Export | 4 | 1-2 | P2 |
| 3.1.6: Auth | 3 | 1 | P1 |
| 3.1.7: Testing | 3 | 2-3 | P1 |
| 3.1.8: Performance | 4 | 1 | P2 |
| **TOTAL** | **37** | **14-19** | - |

---

## 🚀 Execution Plan

### Week 1: Foundation

**Day 1-2:** Project setup и layout
- React + TypeScript project
- UI framework
- Dashboard skeleton
- Routing

**Day 3-5:** Core widgets
- Global Metrics
- Camera Status Grid
- FPS Timeline
- Detection Counter

### Week 2: Advanced Features

**Day 6-7:** Charts
- Multi-Camera FPS
- Detection Trends
- Latency Distribution

**Day 8-9:** Real-time updates
- WebSocket integration
- Data sync
- Update indicators

**Day 10:** Export и reporting

### Week 3: Polish

**Day 11-12:** Authentication
- Login page
- Auth guards
- Role-based access

**Day 13-14:** Testing
- Unit tests
- Integration tests
- E2E tests

**Day 15:** Performance optimization
- Code splitting
- Memoization
- Caching

---

## 📁 Deliverables

1. **Code**
   - React TypeScript project
   - All components
   - Tests

2. **Documentation**
   - README
   - Component docs
   - API integration guide

3. **Deployment**
   - Docker image
   - Deployment guide
   - Environment variables

---

**Plan created:** 2026-04-27  
**Status:** Ready for execution  
**Estimated time:** 2-3 weeks
