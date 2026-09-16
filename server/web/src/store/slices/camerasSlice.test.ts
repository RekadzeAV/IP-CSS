import { configureStore } from '@reduxjs/toolkit';
import camerasReducer, {
  createCamera,
  fetchCameraById,
  setSelectedCamera,
} from '@/store/slices/camerasSlice';
import { cameraService } from '@/services/cameraService';
import type { Camera } from '@/types';

jest.mock('@/services/cameraService', () => ({
  cameraService: {
    getCameras: jest.fn(),
    getCameraById: jest.fn(),
    createCamera: jest.fn(),
    updateCamera: jest.fn(),
    deleteCamera: jest.fn(),
    testConnection: jest.fn(),
    discoverCameras: jest.fn(),
  },
}));

const createStore = () =>
  configureStore({
    reducer: {
      cameras: camerasReducer,
    },
  });

const buildCamera = (id: string): Camera => ({
  id,
  name: `Camera ${id}`,
  url: `rtsp://example.com/${id}`,
  status: 'UNKNOWN',
  fps: 25,
  bitrate: 2048,
  codec: 'H264',
  audio: true,
  createdAt: 1_700_000_000_000,
  updatedAt: 1_700_000_000_000,
});

describe('camerasSlice regressions', () => {
  const mockedCameraService = cameraService as jest.Mocked<typeof cameraService>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('clears stale selectedCamera on fetchCameraById pending', () => {
    const withSelected = camerasReducer(undefined, setSelectedCamera(buildCamera('cam-1')));
    const next = camerasReducer(withSelected, fetchCameraById.pending('req-1', 'cam-2'));

    expect(next.selectedCamera).toBeNull();
    expect(next.loading).toBe(true);
  });

  it('keeps selectedCamera null on fetchCameraById rejected', () => {
    const withSelected = camerasReducer(undefined, setSelectedCamera(buildCamera('cam-1')));
    const next = camerasReducer(
      withSelected,
      fetchCameraById.rejected(new Error('Not found'), 'req-2', 'cam-2', 'Camera not found')
    );

    expect(next.selectedCamera).toBeNull();
    expect(next.error).toBe('Camera not found');
  });

  it('returns backend validation message for createCamera axios error', async () => {
    mockedCameraService.createCamera.mockRejectedValue({
      isAxiosError: true,
      message: 'Request failed with status code 400',
      response: {
        data: {
          message: 'Invalid camera URL: RTSP URL must contain a path',
        },
      },
    });

    const store = createStore();
    const action = await store.dispatch(
      createCamera({
        name: 'Bad camera',
        url: 'rtsp://example.com',
        username: 'admin',
        password: 'secret',
      })
    );

    expect(createCamera.rejected.match(action)).toBe(true);
    expect(action.payload).toBe('Invalid camera URL: RTSP URL must contain a path');
  });
});

