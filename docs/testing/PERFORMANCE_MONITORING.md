# Мониторинг производительности тестов

**Дата:** 27 January 2026

---

## 📋 Обзор

Инструменты для мониторинга производительности во время выполнения тестов.

---

## 🚀 Использование

### Windows:
```powershell
.\scripts\monitor-test-performance.ps1 --Duration 120
```

### Linux/macOS:
```bash
./scripts/monitor-test-performance.sh --duration 120
```

---

## 📊 Собираемые метрики

### CPU Usage
- Процент использования CPU процессом тестов
- Обновляется каждую секунду

### Memory Usage
- Использование памяти (MB)
- Рабочий набор процесса

### FPS (Frames Per Second)
- Средний FPS декодирования
- Рассчитывается на основе количества декодированных кадров

### Frames Decoded
- Общее количество декодированных кадров
- Обновляется в реальном времени

---

## 📝 Формат данных

### CSV файл

Метрики сохраняются в CSV формате:

```csv
Timestamp,CPU%,MemoryMB,FPS,FramesDecoded
0,0.5,50.2,0,0
1,2.3,52.1,25.0,25
2,2.1,52.5,25.0,50
...
```

### Анализ данных

**Windows (PowerShell):**
```powershell
Import-Csv performance_metrics.csv | Format-Table
Import-Csv performance_metrics.csv | Measure-Object -Property CPU% -Average
```

**Linux/macOS:**
```bash
cat performance_metrics.csv | column -t -s,
awk -F',' 'NR>1 {sum+=$2; count++} END {print "Average CPU:", sum/count}' performance_metrics.csv
```

---

## 📈 Визуализация

### Excel / Google Sheets

1. Импортировать CSV файл
2. Создать графики:
   - CPU Usage over time
   - Memory Usage over time
   - FPS over time
   - Frames Decoded over time

### Python скрипт

```python
import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv('performance_metrics.csv')

plt.figure(figsize=(12, 6))
plt.subplot(2, 2, 1)
plt.plot(df['Timestamp'], df['CPU%'])
plt.title('CPU Usage')
plt.xlabel('Time (s)')
plt.ylabel('CPU %')

plt.subplot(2, 2, 2)
plt.plot(df['Timestamp'], df['MemoryMB'])
plt.title('Memory Usage')
plt.xlabel('Time (s)')
plt.ylabel('Memory (MB)')

plt.subplot(2, 2, 3)
plt.plot(df['Timestamp'], df['FPS'])
plt.title('FPS')
plt.xlabel('Time (s)')
plt.ylabel('FPS')

plt.subplot(2, 2, 4)
plt.plot(df['Timestamp'], df['FramesDecoded'])
plt.title('Frames Decoded')
plt.xlabel('Time (s)')
plt.ylabel('Frames')

plt.tight_layout()
plt.savefig('performance_graphs.png')
```

---

## 🎯 Использование результатов

### Выявление проблем производительности

1. **Высокое использование CPU:**
   - Проверить оптимизацию декодирования
   - Рассмотреть аппаратное ускорение

2. **Утечки памяти:**
   - Проверить освобождение ресурсов
   - Использовать профилировщик памяти

3. **Низкий FPS:**
   - Проверить производительность декодирования
   - Оптимизировать конвертацию форматов

### Сравнение версий

Сохраняйте метрики для разных версий и сравнивайте:
- CPU usage
- Memory usage
- FPS
- Стабильность

---

## 🔧 Расширенное использование

### Длительный мониторинг

```bash
# Мониторинг в течение часа
./scripts/monitor-test-performance.sh --duration 3600
```

### Параллельный мониторинг

Запустите мониторинг в отдельном терминале во время выполнения тестов.

---

**Последнее обновление:** 27 January 2026
