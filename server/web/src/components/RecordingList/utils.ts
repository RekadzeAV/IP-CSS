export const formatDate = (timestamp: number): string => {
  return new Date(timestamp).toLocaleString('ru-RU');
};

export const formatDuration = (duration: number): string => {
  const hours = Math.floor(duration / 3600);
  const minutes = Math.floor((duration % 3600) / 60);
  const seconds = duration % 60;
  return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
};

export const formatFileSize = (bytes?: number): string => {
  if (!bytes) return 'Неизвестно';
  const mb = bytes / (1024 * 1024);
  return `${mb.toFixed(2)} МБ`;
};

export const getStatusColor = (status: string): 'success' | 'info' | 'error' | 'warning' | 'default' => {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'RECORDING':
      return 'info';
    case 'FAILED':
      return 'error';
    case 'PENDING':
      return 'warning';
    default:
      return 'default';
  }
};

export const getStatusLabel = (status: string): string => {
  switch (status) {
    case 'COMPLETED':
      return 'Завершена';
    case 'RECORDING':
      return 'Идет запись';
    case 'FAILED':
      return 'Ошибка';
    case 'PENDING':
      return 'Ожидание';
    default:
      return status;
  }
};
