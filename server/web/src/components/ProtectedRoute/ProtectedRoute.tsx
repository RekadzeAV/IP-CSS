'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Box, CircularProgress } from '@mui/material';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { fetchCurrentUser } from '@/store/slices/authSlice';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { isAuthenticated, loading } = useAppSelector((state) => state.auth);

  useEffect(() => {
    // Проверяем аутентификацию через Redux
    if (!isAuthenticated && !loading) {
      // Пытаемся получить текущего пользователя
      dispatch(fetchCurrentUser()).then((result) => {
        if (fetchCurrentUser.rejected.match(result)) {
          // Если не удалось получить пользователя, перенаправляем на логин
          router.push('/login');
        }
      });
    }
  }, [isAuthenticated, loading, dispatch, router]);

  // Показываем загрузку пока проверяем аутентификацию
  if (loading || (!isAuthenticated && !loading)) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh',
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  // Если не аутентифицирован, не показываем контент (будет редирект)
  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}



