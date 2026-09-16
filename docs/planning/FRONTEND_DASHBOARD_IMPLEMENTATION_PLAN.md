# Frontend Analytics Dashboard - Implementation Plan

**Дата:** 2026-04-27  
**ETA:** 2-3 недели  
**Status:** 🟡 Planning Phase

---

## 📋 Overview

Создание modern React + TypeScript dashboard для мониторинга и управления IP-CSS системой.

### Цели:
- Real-time monitoring всех камер
- Analytics visualization
- System health dashboard
- User-friendly interface
- Mobile responsive

---

## 🏗️ Architecture

### Tech Stack:
```
Frontend:
├── React 18+ (with Hooks)
├── TypeScript 5.x
├── Vite (build tool)
├── TailwindCSS (styling)
├── Recharts (charts)
├── React Query (data fetching)
├── Zustand (state management)
├── WebSocket (real-time updates)
└── React Router (navigation)
```

### Project Structure:
```
server/web/
├── src/
│   ├── components/
│   │   ├── common/
│   │   │   ├── Button.tsx
│   │   │   ├── Card.tsx
│   │   │   ├── Modal.tsx
│   │   │   └── Table.tsx
│   │   ├── dashboard/
│   │   │   ├── CameraGrid.tsx
│   │   │   ├── AnalyticsChart.tsx
│   │   │   ├── SystemHealth.tsx
│   │   │   └── EventList.tsx
│   │   ├── camera/
│   │   │   ├── CameraView.tsx
│   │   │   ├── CameraControls.tsx
│   │   │   └── CameraSettings.tsx
│   │   └── layout/
│   │       ├── Header.tsx
│   │       ├── Sidebar.tsx
│   │       └── Footer.tsx
│   ├── pages/
│   │   ├── DashboardPage.tsx
│   │   ├── CamerasPage.tsx
│   │   ├── AnalyticsPage.tsx
│   │   ├── SettingsPage.tsx
│   │   └── LoginPage.tsx
│   ├── services/
│   │   ├── api.ts
│   │   ├── websocket.ts
│   │   └── auth.ts
│   ├── store/
│   │   ├── cameraStore.ts
│   │   ├── authStore.ts
│   │   └── analyticsStore.ts
│   ├── hooks/
│   │   ├── useWebSocket.ts
│   │   ├── useCameras.ts
│   │   └── useAnalytics.ts
│   ├── types/
│   │   ├── camera.ts
│   │   ├── analytics.ts
│   │   └── user.ts
│   ├── utils/
│   │   └── helpers.ts
│   ├── styles/
│   │   └── globals.css
│   ├── App.tsx
│   └── main.tsx
├── public/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── tailwind.config.js
```

---

## 🎨 Widgets (10 total)

### 1. Camera Status Widget
**Цель:** Показывает статус всех камер в реальном времени

**Данные:**
```typescript
interface CameraStatus {
  id: string
  name: string
  status: 'online' | 'offline' | 'error'
  fps: number
  resolution: string
  lastSeen: Date
}
```

**Features:**
- Grid view всех камер
- Color-coded status indicators
- Quick actions (play, stop, settings)
- Real-time updates via WebSocket

---

### 2. System Health Widget
**Цель:** Общее состояние системы

**Данные:**
```typescript
interface SystemHealth {
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  networkUsage: number
  activeStreams: number
  totalCameras: number
}
```

**Features:**
- CPU/Memory/Disk gauges
- Network bandwidth chart
- Active streams counter
- Alerts for high usage

---

### 3. Analytics Overview Widget
**Цель:** Summary всех аналитических событий

**Данные:**
```typescript
interface AnalyticsSummary {
  totalEvents: number
  motionDetected: number
  objectsDetected: number
  facesDetected: number
  licensePlates: number
  last24Hours: number
}
```

**Features:**
- Event counters
- Trend arrows (↑↓)
- Top events list
- Filter by type

---

### 4. Real-time Events Feed Widget
**Цель:** Live stream последних событий

**Данные:**
```typescript
interface Event {
  id: string
  type: 'motion' | 'object' | 'face' | 'license_plate'
  cameraId: string
  timestamp: Date
  confidence: number
  thumbnail?: string
}
```

**Features:**
- Scrollable event list
- Thumbnails
- Priority badges
- Quick view modal

---

### 5. Performance Metrics Widget
**Цель:** FPS и latency monitoring

**Данные:**
```typescript
interface PerformanceMetrics {
  avgFps: number
  minFps: number
  maxFps: number
  avgLatency: number
  droppedFrames: number
  reconnections: number
}
```

**Features:**
- Line chart (FPS over time)
- Latency gauge
- Performance trends
- Alerts for degradation

---

### 6. Storage Usage Widget
**Цель:** Monitoring дискового пространства

**Данные:**
```typescript
interface StorageUsage {
  totalSpace: number
  usedSpace: number
  freeSpace: number
  recordingsSize: number
  snapshotsSize: number
  estimatedDaysRemaining: number
}
```

**Features:**
- Pie chart
- Usage breakdown
- Days remaining estimate
- Cleanup suggestions

---

### 7. Network Traffic Widget
**Цель:** Network bandwidth monitoring

**Данные:**
```typescript
interface NetworkTraffic {
  uploadSpeed: number
  downloadSpeed: number
  totalBandwidth: number
  peakBandwidth: number
  activeConnections: number
}
```

**Features:**
- Real-time bandwidth chart
- Upload/Download split
- Peak usage indicator
- Connection counter

---

### 8. User Activity Widget
**Цель:** Monitoring активности пользователей

**Данные:**
```typescript
interface UserActivity {
  activeUsers: number
  totalLogins: number
  recentLogins: Array<{
    username: string
    timestamp: Date
    action: string
  }>
}
```

**Features:**
- Active users counter
- Login history
- User roles breakdown
- Security alerts

---

### 9. Camera Performance Comparison Widget
**Цель:** Сравнение производительности камер

**Данные:**
```typescript
interface CameraPerformance {
  cameraId: string
  name: string
  avgFps: number
  avgLatency: number
  uptime: number
  errorRate: number
}
```

**Features:**
- Bar chart comparison
- Ranking by performance
- Detailed stats per camera
- Export to CSV

---

### 10. Alert & Notifications Widget
**Цель:** Центральное место для всех алертов

**Данные:**
```typescript
interface Alert {
  id: string
  severity: 'low' | 'medium' | 'high' | 'critical'
  title: string
  message: string
  timestamp: Date
  acknowledged: boolean
}
```

**Features:**
- Severity badges
- Acknowledge button
- Filter by severity
- Auto-dismiss option

---

## 📅 Implementation Timeline

### Week 1: Setup & Foundation

**Day 1-2: Project Setup**
- [ ] Create React + TypeScript project with Vite
- [ ] Setup TailwindCSS
- [ ] Configure ESLint + Prettier
- [ ] Setup folder structure
- [ ] Create basic layout components

**Day 3-4: Core Components**
- [ ] Button, Card, Modal, Table components
- [ ] Header, Sidebar, Footer layouts
- [ ] Navigation setup
- [ ] Authentication flow

**Day 5: API Integration**
- [ ] Create API service layer
- [ ] Setup React Query
- [ ] WebSocket service
- [ ] Auth service

---

### Week 2: Widgets & Pages

**Day 1-3: First 5 Widgets**
- [ ] Camera Status Widget
- [ ] System Health Widget
- [ ] Analytics Overview Widget
- [ ] Real-time Events Feed Widget
- [ ] Performance Metrics Widget

**Day 4-5: Next 5 Widgets**
- [ ] Storage Usage Widget
- [ ] Network Traffic Widget
- [ ] User Activity Widget
- [ ] Camera Performance Comparison Widget
- [ ] Alert & Notifications Widget

**Day 6-7: Pages**
- [ ] DashboardPage (all widgets)
- [ ] CamerasPage (detailed view)
- [ ] AnalyticsPage (charts)
- [ ] SettingsPage

---

### Week 3: Polish & Testing

**Day 1-2: Styling & UX**
- [ ] Responsive design
- [ ] Dark mode
- [ ] Animations
- [ ] Loading states

**Day 3-4: Testing**
- [ ] Unit tests (Vitest)
- [ ] Integration tests
- [ ] E2E tests (Playwright)

**Day 5: Deployment**
- [ ] Build optimization
- [ ] CI/CD setup
- [ ] Production deployment

---

## 🛠️ Technical Details

### State Management (Zustand):
```typescript
// store/cameraStore.ts
import { create } from 'zustand'

interface CameraStore {
  cameras: Camera[]
  isLoading: boolean
  error: string | null
  fetchCameras: () => Promise<void>
  updateCamera: (id: string, data: Partial<Camera>) => void
}

export const useCameraStore = create<CameraStore>((set) => ({
  cameras: [],
  isLoading: false,
  error: null,
  fetchCameras: async () => { /* ... */ },
  updateCamera: (id, data) => { /* ... */ }
}))
```

### WebSocket Service:
```typescript
// services/websocket.ts
class WebSocketService {
  private ws: WebSocket | null = null
  
  connect(url: string) {
    this.ws = new WebSocket(url)
    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      this.handleMessage(data)
    }
  }
  
  subscribe(channel: string, callback: (data: any) => void) {
    // Subscribe to channel
  }
}
```

### API Service:
```typescript
// services/api.ts
const apiClient = axios.create({
  baseURL: '/api/v1',
  withCredentials: true
})

export const cameraApi = {
  getCameras: () => apiClient.get('/cameras'),
  getCamera: (id: string) => apiClient.get(`/cameras/${id}`),
  updateCamera: (id: string, data: any) => apiClient.put(`/cameras/${id}`, data)
}
```

---

## 🎯 Success Criteria

### Functional:
- ✅ All 10 widgets working
- ✅ Real-time updates via WebSocket
- ✅ Authentication & authorization
- ✅ Responsive design (mobile/tablet/desktop)
- ✅ Dark mode support

### Performance:
- ✅ < 3s initial load
- ✅ < 100ms widget updates
- ✅ < 1s page navigation
- ✅ Optimized bundle size (< 500KB)

### Quality:
- ✅ 80%+ test coverage
- ✅ No TypeScript errors
- ✅ No ESLint warnings
- ✅ Lighthouse score > 90

---

## 📦 Dependencies

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "@tanstack/react-query": "^5.12.0",
    "zustand": "^4.4.0",
    "axios": "^1.6.0",
    "recharts": "^2.10.0",
    "tailwindcss": "^3.3.0",
    "date-fns": "^3.0.0",
    "clsx": "^2.0.0"
  },
  "devDependencies": {
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "@vitejs/plugin-react": "^4.2.0",
    "vitest": "^1.0.0",
    "playwright": "^1.40.0",
    "eslint": "^8.54.0",
    "prettier": "^3.1.0"
  }
}
```

---

**Created:** 2026-04-27  
**ETA:** 3 weeks  
**Status:** 🟡 Ready to Implement
