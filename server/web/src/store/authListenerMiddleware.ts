import { createListenerMiddleware } from '@reduxjs/toolkit';
import { login, logout } from '@/store/slices/authSlice';
import { clearScheduledRefresh, scheduleAccessTokenRefresh } from '@/utils/accessTokenRefreshScheduler';

export const authListenerMiddleware = createListenerMiddleware();

authListenerMiddleware.startListening({
  matcher: login.fulfilled.match,
  effect: (action) => {
    const payload = action.payload;
    if (payload?.needs2fa) {
      return;
    }
    const exp = payload?.expiresIn;
    if (typeof exp === 'number' && exp > 0) {
      scheduleAccessTokenRefresh(exp);
    }
  },
});

authListenerMiddleware.startListening({
  matcher: logout.fulfilled.match,
  effect: () => {
    clearScheduledRefresh();
  },
});
