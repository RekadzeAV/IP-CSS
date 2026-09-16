import apiClient from '@/utils/api';
import type { Report, ReportFilter, ExportFormat } from '@/types';

export interface ReportParams {
  type: 'EVENTS_SUMMARY' | 'RECORDINGS_SUMMARY' | 'LICENSE_PLATES_SUMMARY' | 'ANALYTICS_DASHBOARD';
  format: 'CSV' | 'PDF';
  cameraId?: string;
  fromTimestamp?: number;
  toTimestamp?: number;
}

const reportService = {
  /**
   * Получить список отчётов
   */
  async getReports(filter: ReportFilter): Promise<Report[]> {
    const response = await apiClient.get<Report[]>('/reports', { params: filter });
    if (response.data) {
      return Array.isArray(response.data) ? response.data : [];
    }
    return [];
  },

  /**
   * Получить отчёт по ID
   */
  async getReportById(id: string): Promise<Report> {
    const response = await apiClient.get<Report>(`/reports/${id}`);
    return response.data;
  },

  /**
   * Сгенерировать новый отчёт
   */
  async generateReport(data: Partial<Report>): Promise<Report> {
    const response = await apiClient.post<Report>('/reports', data);
    return response.data;
  },

  /**
   * Экспортировать отчёт
   */
  async exportReport(format: ExportFormat): Promise<void> {
    const response = await apiClient.get(`/reports/export?format=${format}`, {
      responseType: 'blob',
    });
    
    // Создаём ссылку для скачивания
    const blob = new Blob([response.data], { type: `application/${format}` });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `report.${format}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};

export { reportService };
