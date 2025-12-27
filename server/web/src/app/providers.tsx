'use client';

import { Provider, useSelector } from 'react-redux';
import { store, RootState } from '@/store';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { SnackbarProvider } from 'notistack';
import { WebSocketProvider } from '@/components/WebSocketProvider';
import { useMemo } from 'react';

function ThemeProviderWrapper({ children }: { children: React.ReactNode }) {
  const themeMode = useSelector((state: RootState) => state.settings.themeMode);

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: themeMode,
          primary: {
            main: '#1976d2',
          },
          secondary: {
            main: '#dc004e',
          },
        },
      }),
    [themeMode]
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <Provider store={store}>
      <ThemeProviderWrapper>
        <SnackbarProvider maxSnack={3} anchorOrigin={{ vertical: 'top', horizontal: 'right' }}>
          <WebSocketProvider>{children}</WebSocketProvider>
        </SnackbarProvider>
      </ThemeProviderWrapper>
    </Provider>
  );
}


