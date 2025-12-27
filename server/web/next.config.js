/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,

  // Security Headers
  async headers() {
    const isProduction = process.env.NODE_ENV === 'production';

    return [
      {
        // Применяем ко всем маршрутам
        source: '/:path*',
        headers: [
          // Защита от XSS атак
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff',
          },
          // Защита от clickjacking
          {
            key: 'X-Frame-Options',
            value: 'DENY',
          },
          // Защита от MIME type sniffing
          {
            key: 'X-XSS-Protection',
            value: '1; mode=block',
          },
          // Referrer Policy
          {
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin',
          },
          // Permissions Policy (бывший Feature-Policy)
          {
            key: 'Permissions-Policy',
            value: [
              'camera=()',
              'microphone=()',
              'geolocation=()',
              'interest-cohort=()',
            ].join(', '),
          },
          // DNS Prefetch Control
          {
            key: 'X-DNS-Prefetch-Control',
            value: 'on',
          },
          // Strict Transport Security (только для HTTPS в продакшене)
          ...(isProduction
            ? [
                {
                  key: 'Strict-Transport-Security',
                  value: 'max-age=63072000; includeSubDomains; preload',
                },
              ]
            : []),
          // Content Security Policy
          {
            key: 'Content-Security-Policy',
            value: [
              "default-src 'self'",
              // Scripts - используем nonce в production для большей безопасности
              ...(isProduction
                ? ["script-src 'self' 'strict-dynamic'"]
                : ["script-src 'self' 'unsafe-eval' 'unsafe-inline'"]), // unsafe-eval только для dev
              // Styles
              "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com", // unsafe-inline для MUI
              "font-src 'self' data: https://fonts.gstatic.com",
              "img-src 'self' data: https: blob:",
              "media-src 'self' blob: http://localhost:8080 https:",
              "connect-src 'self' http://localhost:8080 https: ws: wss:",
              "frame-ancestors 'none'",
              "base-uri 'self'",
              "form-action 'self'",
              "object-src 'none'",
              "worker-src 'self' blob:",
              ...(isProduction ? ["upgrade-insecure-requests"] : []),
            ].join('; '),
          },
          // Expect-CT (Certificate Transparency)
          ...(isProduction
            ? [
                {
                  key: 'Expect-CT',
                  value: 'max-age=86400, enforce',
                },
              ]
            : []),
        ],
      },
    ];
  },

  // Оптимизация изображений
  images: {
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
      },
      {
        protocol: 'https',
        hostname: '**',
      },
    ],
    formats: ['image/avif', 'image/webp'],
  },

  // Webpack конфигурация
  webpack: (config, { isServer }) => {
    if (!isServer) {
      config.resolve.fallback = {
        ...config.resolve.fallback,
        fs: false,
        net: false,
        tls: false,
      };
    }
    return config;
  },
};

module.exports = nextConfig;

