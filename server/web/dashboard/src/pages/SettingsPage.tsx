import { getStoredUser } from '../lib/api';

export function Settings() {
  const user = getStoredUser();

  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Настройки</h1>
      <p className="text-gray-600 dark:text-gray-400 mb-6">
        Базовый профиль сессии (полные настройки — в основном Next.js клиенте)
      </p>

      <div className="rounded-lg bg-white dark:bg-gray-800 shadow p-6 max-w-lg space-y-3">
        <div>
          <span className="text-sm text-gray-500 dark:text-gray-400">Пользователь</span>
          <p className="font-medium text-gray-900 dark:text-white">{user?.username ?? '—'}</p>
        </div>
        <div>
          <span className="text-sm text-gray-500 dark:text-gray-400">Роль</span>
          <p className="font-medium text-gray-900 dark:text-white">{user?.role ?? '—'}</p>
        </div>
        <div>
          <span className="text-sm text-gray-500 dark:text-gray-400">Email</span>
          <p className="font-medium text-gray-900 dark:text-white">{user?.email ?? '—'}</p>
        </div>
      </div>
    </div>
  );
}
