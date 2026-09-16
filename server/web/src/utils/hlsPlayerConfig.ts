import type Hls from 'hls.js';

type HlsConstructorOptions = NonNullable<ConstructorParameters<typeof Hls>[0]>;

function xhrSetupWithCredentials(xhr: XMLHttpRequest) {
  xhr.withCredentials = true;
}

/** Живой поток (камера): JWT в httpOnly cookie — для cross-origin к API нужен withCredentials на XHR hls.js */
export function createHlsLivePlayerConfig(): HlsConstructorOptions {
  return {
    enableWorker: true,
    lowLatencyMode: true,
    backBufferLength: 30,
    maxBufferLength: 15,
    maxMaxBufferLength: 30,
    liveSyncDurationCount: 2,
    liveMaxLatencyDurationCount: 5,
    debug: false,
    xhrSetup: xhrSetupWithCredentials,
  };
}

/** Воспроизведение записи через защищённые маршруты /api/v1/recordings/.../hls/ */
export function createHlsRecordingPlayerConfig(): HlsConstructorOptions {
  return {
    enableWorker: true,
    lowLatencyMode: false,
    backBufferLength: 90,
    maxBufferLength: 30,
    maxMaxBufferLength: 60,
    debug: false,
    xhrSetup: xhrSetupWithCredentials,
  };
}
