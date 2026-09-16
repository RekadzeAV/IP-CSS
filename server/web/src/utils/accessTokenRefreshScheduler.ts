/**
 * Проактивное обновление access token по expiresIn (httpOnly cookie), без ожидания 401.
 * См. P1-4 / W3-2 — lifecycle сессии.
 */

const STORAGE_TTL_KEY = 'ipcss_access_expires_in_sec';
const STORAGE_SET_AT_KEY = 'ipcss_access_set_at_ms';

const DEFAULT_EXPIRES_SEC = Number(process.env.NEXT_PUBLIC_ACCESS_TOKEN_EXPIRES_SECONDS) || 900;
/** Обновление до истечения срока (доля TTL), чтобы снизить гонки с параллельными запросами */
const REFRESH_RATIO = 0.85;

let timerId: ReturnType<typeof setTimeout> | null = null;
let proactiveRefreshInFlight = false;

export function getDefaultAccessTokenExpiresSeconds(): number {
  return DEFAULT_EXPIRES_SEC;
}

export function clearScheduledRefresh(): void {
  if (timerId !== null) {
    clearTimeout(timerId);
    timerId = null;
  }
  try {
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(STORAGE_TTL_KEY);
      sessionStorage.removeItem(STORAGE_SET_AT_KEY);
    }
  } catch {
    // ignore
  }
}

async function runProactiveRefresh(): Promise<void> {
  if (proactiveRefreshInFlight) {
    return;
  }
  proactiveRefreshInFlight = true;
  try {
    const { authService } = await import('@/services/authService');
    const expiresIn = await authService.refreshToken();
    scheduleAccessTokenRefresh(expiresIn);
  } catch (e) {
    console.warn('[Auth] Proactive token refresh failed:', e);
    clearScheduledRefresh();
  } finally {
    proactiveRefreshInFlight = false;
  }
}

/**
 * Запланировать следующий refresh: через ~REFRESH_RATIO * TTL с момента вызова.
 */
export function scheduleAccessTokenRefresh(expiresInSeconds: number): void {
  clearScheduledRefresh();
  const safe = Math.max(60, Math.min(expiresInSeconds, 24 * 3600));
  const now = Date.now();
  try {
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem(STORAGE_TTL_KEY, String(safe));
      sessionStorage.setItem(STORAGE_SET_AT_KEY, String(now));
    }
  } catch {
    // ignore
  }
  const delayMs = Math.max(5_000, Math.floor(safe * 1000 * REFRESH_RATIO));
  timerId = setTimeout(() => {
    void runProactiveRefresh();
  }, delayMs);
}

/**
 * Восстановить таймер после перезагрузки страницы (если есть метки в sessionStorage).
 * @returns true, если расписание восстановлено или таймер уже активен
 */
export function restoreScheduleFromStorageIfNeeded(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  if (timerId !== null) {
    return true;
  }
  try {
    const ttl = sessionStorage.getItem(STORAGE_TTL_KEY);
    const setAt = sessionStorage.getItem(STORAGE_SET_AT_KEY);
    if (!ttl || !setAt) {
      return false;
    }
    const ttlSec = Number(ttl);
    const setAtMs = Number(setAt);
    if (!Number.isFinite(ttlSec) || !Number.isFinite(setAtMs)) {
      return false;
    }
    const deadline = setAtMs + Math.floor(ttlSec * 1000 * REFRESH_RATIO);
    const delayMs = deadline - Date.now();
    if (delayMs <= 2000) {
      void runProactiveRefresh();
    } else {
      timerId = setTimeout(() => void runProactiveRefresh(), delayMs);
    }
    return true;
  } catch {
    return false;
  }
}
