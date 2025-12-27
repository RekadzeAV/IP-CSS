'use client';

import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { useSnackbar } from 'notistack';
import type { RootState } from '@/store';
import type { WebSocketMessage } from '@/utils/websocket';

/**
 * Компонент для обработки WebSocket уведомлений и показа их через notistack
 */
export function WebSocketNotificationHandler() {
  const { enqueueSnackbar } = useSnackbar();
  const { lastMessage } = useSelector((state: RootState) => state.websocket);

  useEffect(() => {
    if (lastMessage && lastMessage.type === 'event' && lastMessage.channel === 'notifications') {
      const data = lastMessage.data;
      if (data && (data.title || data.message)) {
        const title = data.title || 'Уведомление';
        const notificationMessage = data.message || '';
        const severity = data.severity || 'info';

        enqueueSnackbar(`${title}: ${notificationMessage}`, {
          variant: severity === 'error' ? 'error' : severity === 'warning' ? 'warning' : 'info',
          autoHideDuration: severity === 'error' ? 6000 : 4000,
        });
      }
    }
  }, [lastMessage, enqueueSnackbar]);

  return null;
}

