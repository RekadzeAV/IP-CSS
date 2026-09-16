'use client';

import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import type { RootState } from '@/store';
import {
  clearScheduledRefresh,
  getDefaultAccessTokenExpiresSeconds,
  restoreScheduleFromStorageIfNeeded,
  scheduleAccessTokenRefresh,
} from '@/utils/accessTokenRefreshScheduler';

/**
 * После входа или при перезагрузке с сессией — проактивный refresh по expiresIn.
 */
export function SessionRefreshProvider({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);

  useEffect(() => {
    if (!isAuthenticated) {
      clearScheduledRefresh();
      return;
    }
    if (!restoreScheduleFromStorageIfNeeded()) {
      scheduleAccessTokenRefresh(getDefaultAccessTokenExpiresSeconds());
    }
  }, [isAuthenticated]);

  return <>{children}</>;
}
