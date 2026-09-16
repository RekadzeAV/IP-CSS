import type Hls from 'hls.js';

type HlsConstructorOptions = NonNullable<ConstructorParameters<typeof Hls>[0]>;

function xhrSetupWithCredentials(xhr: XMLHttpRequest) {
  xhr.withCredentials = true;
}

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
