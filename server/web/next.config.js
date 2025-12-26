/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  
  // Переменные окружения
  env: {
    API_URL: process.env.API_URL || 'http://localhost:8080/api/v1',
    WS_URL: process.env.WS_URL || 'ws://localhost:8080/api/v1/ws',
  },
  
  // Перезапись путей для API прокси
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.API_URL || 'http://localhost:8080/api/v1'}/:path*`,
      },
    ];
  },
  
  // Оптимизация изображений
  images: {
    domains: ['localhost'],
    formats: ['image/avif', 'image/webp'],
  },
  
  // Экспериментальные функции
  experimental: {
    appDir: true,
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

