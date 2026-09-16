import type { Metadata, Viewport } from 'next';
import { Providers } from './providers';

export const metadata: Metadata = {
  title: {
    default: 'IP Camera Surveillance System',
    template: '%s | IPCSS'
  },
  description: 'Web interface for IP Camera Surveillance System',
  manifest: '/manifest.json',
  themeColor: '#1976d2',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'default',
    title: 'IPCSS'
  },
  formatDetection: {
    telephone: false
  }
};

export const viewport: Viewport = {
  themeColor: '#1976d2',
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
  userScalable: false
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ru">
      <head>
        <link rel="icon" href="/favicon.ico" sizes="any" />
        <link rel="icon" href="/icon.svg" type="image/svg+xml" />
        <link rel="apple-touch-icon" href="/icons/icon-180x180.png" />
        <link rel="manifest" href="/manifest.json" />
        <meta name="theme-color" content="#1976d2" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="default" />
        <meta name="apple-mobile-web-app-title" content="IPCSS" />
      </head>
      <body>
        <Providers>{children}</Providers>
        <script
          dangerouslySetInnerHTML={{
            __html: `
              if ('serviceWorker' in navigator) {
                window.addEventListener('load', () => {
                  navigator.serviceWorker.register('/sw.js')
                    .then(registration => {
                      console.log('ServiceWorker registered:', registration.scope);
                      
                      registration.addEventListener('updatefound', () => {
                        const newWorker = registration.installing;
                        if (newWorker) {
                          newWorker.addEventListener('statechange', () => {
                            if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                              // New content available
                              if (confirm('Доступна новая версия. Обновить?')) {
                                window.location.reload();
                              }
                            }
                          });
                        }
                      });
                    })
                    .catch(error => {
                      console.log('ServiceWorker registration failed:', error);
                    });
                });
              }
            `
          }}
        />
      </body>
    </html>
  );
}



