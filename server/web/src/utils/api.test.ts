import type { InternalAxiosRequestConfig } from 'axios';

type RequestHandler = (config: InternalAxiosRequestConfig) => InternalAxiosRequestConfig;
type ResponseErrorHandler = (error: unknown) => Promise<unknown>;

const requestHandlers: RequestHandler[] = [];
const responseErrorHandlers: ResponseErrorHandler[] = [];

const mockApiClient = Object.assign(jest.fn(async (config: InternalAxiosRequestConfig) => ({ config })), {
  interceptors: {
    request: {
      use: jest.fn((fulfilled: RequestHandler) => {
        requestHandlers.push(fulfilled);
        return requestHandlers.length - 1;
      }),
    },
    response: {
      use: jest.fn((_fulfilled: unknown, rejected: ResponseErrorHandler) => {
        responseErrorHandlers.push(rejected);
        return responseErrorHandlers.length - 1;
      }),
    },
  },
});

jest.mock('axios', () => ({
  __esModule: true,
  default: {
    create: jest.fn(() => mockApiClient),
  },
}));

const refreshTokenMock = jest.fn();
jest.mock('../services/authService', () => ({
  authService: {
    refreshToken: () => refreshTokenMock(),
  },
}));

jest.mock('./certificatePinning', () => ({
  validateCertificatePinning: jest.fn(() => true),
}));

describe('api client interceptors', () => {
  beforeEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
    requestHandlers.length = 0;
    responseErrorHandlers.length = 0;
    document.cookie = 'csrf_token=test-csrf-token';
  });

  it('adds CSRF token for state-changing methods', async () => {
    await import('./api');
    const csrfInterceptor = requestHandlers.at(-1);
    expect(csrfInterceptor).toBeDefined();

    if (!csrfInterceptor) {
      throw new Error('CSRF interceptor was not registered');
    }

    const config = csrfInterceptor({
      method: 'post',
      headers: {},
    } as InternalAxiosRequestConfig);

    expect(config.headers?.['X-CSRF-Token']).toBe('test-csrf-token');
  });

  it('refreshes token and retries request on 401', async () => {
    refreshTokenMock.mockResolvedValue(undefined);
    await import('./api');

    const errorInterceptor = responseErrorHandlers.at(-1);
    expect(errorInterceptor).toBeDefined();
    if (!errorInterceptor) {
      throw new Error('Error interceptor was not registered');
    }

    const originalRequest = {
      url: '/cameras',
      method: 'get',
      headers: {},
    } as InternalAxiosRequestConfig & { _retry?: boolean };

    await errorInterceptor({
      response: { status: 401 },
      config: originalRequest,
    });

    expect(refreshTokenMock).toHaveBeenCalledTimes(1);
    expect(originalRequest._retry).toBe(true);
    expect(mockApiClient).toHaveBeenCalledWith(originalRequest);
  });
});
