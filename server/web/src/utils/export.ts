/**
 * Утилиты для экспорта данных
 */

export interface ExportOptions {
  format: 'csv' | 'json';
  filename?: string;
}

/**
 * Экспорт массива объектов в CSV
 */
export function exportToCSV<T extends Record<string, any>>(
  data: T[],
  filename: string = 'export.csv',
  headers?: string[]
): void {
  if (data.length === 0) {
    console.warn('No data to export');
    return;
  }

  // Определяем заголовки
  const csvHeaders = headers || Object.keys(data[0]);

  // Создаем CSV строку
  const csvRows: string[] = [];

  // Заголовки
  csvRows.push(csvHeaders.map((h) => `"${h}"`).join(','));

  // Данные
  data.forEach((row) => {
    const values = csvHeaders.map((header) => {
      const value = row[header];
      if (value === null || value === undefined) {
        return '';
      }
      // Экранируем кавычки и запятые
      const stringValue = String(value).replace(/"/g, '""');
      return `"${stringValue}"`;
    });
    csvRows.push(values.join(','));
  });

  const csvContent = csvRows.join('\n');
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/**
 * Экспорт массива объектов в JSON
 */
export function exportToJSON<T>(
  data: T[],
  filename: string = 'export.json'
): void {
  const jsonContent = JSON.stringify(data, null, 2);
  const blob = new Blob([jsonContent], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/**
 * Экспорт событий
 */
export function exportEvents(
  events: Array<{
    id: string;
    type: string;
    severity: string;
    timestamp: number;
    description?: string;
    cameraName?: string;
  }>,
  format: 'csv' | 'json',
  filename?: string
): void {
  const defaultFilename = `events-${new Date().toISOString().split('T')[0]}`;

  if (format === 'csv') {
    const csvData = events.map((event) => ({
      ID: event.id,
      Тип: event.type,
      Важность: event.severity,
      Время: new Date(event.timestamp).toLocaleString('ru-RU'),
      Камера: event.cameraName || '',
      Описание: event.description || '',
    }));
    exportToCSV(csvData, `${filename || defaultFilename}.csv`);
  } else {
    exportToJSON(events, `${filename || defaultFilename}.json`);
  }
}

/**
 * Экспорт записей
 */
export function exportRecordings(
  recordings: Array<{
    id: string;
    cameraId: string;
    cameraName?: string;
    startTime: number;
    endTime: number;
    duration: number;
    format: string;
    fileSize?: number;
  }>,
  format: 'csv' | 'json',
  filename?: string
): void {
  const defaultFilename = `recordings-${new Date().toISOString().split('T')[0]}`;

  if (format === 'csv') {
    const csvData = recordings.map((recording) => ({
      ID: recording.id,
      Камера: recording.cameraName || recording.cameraId,
      Начало: new Date(recording.startTime).toLocaleString('ru-RU'),
      Конец: new Date(recording.endTime).toLocaleString('ru-RU'),
      Длительность: `${recording.duration}с`,
      Формат: recording.format,
      Размер: recording.fileSize ? `${(recording.fileSize / 1024 / 1024).toFixed(2)} МБ` : 'N/A',
    }));
    exportToCSV(csvData, `${filename || defaultFilename}.csv`);
  } else {
    exportToJSON(recordings, `${filename || defaultFilename}.json`);
  }
}

/**
 * Экспорт камер
 */
export function exportCameras(
  cameras: Array<{
    id: string;
    name: string;
    url: string;
    status: string;
    model?: string;
  }>,
  format: 'csv' | 'json',
  filename?: string
): void {
  const defaultFilename = `cameras-${new Date().toISOString().split('T')[0]}`;

  if (format === 'csv') {
    const csvData = cameras.map((camera) => ({
      ID: camera.id,
      Название: camera.name,
      URL: camera.url,
      Статус: camera.status,
      Модель: camera.model || '',
    }));
    exportToCSV(csvData, `${filename || defaultFilename}.csv`);
  } else {
    exportToJSON(cameras, `${filename || defaultFilename}.json`);
  }
}

