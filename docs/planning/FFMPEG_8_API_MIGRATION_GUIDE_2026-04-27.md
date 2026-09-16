# FFmpeg 8.0 API Migration Guide

**Date:** 27 April 2026  
**Version:** 1.0.0  
**Status:** ⚠️ **In Progress**

---

## 📋 Overview

This document describes the changes needed to migrate from FFmpeg 7.x to 8.0 API in the IP-CSS project.

**Impact:** Critical for RTSP client video streaming functionality.

---

## 🔍 Breaking Changes in FFmpeg 8.0

### 1. Audio Channel Layout API

**FFmpeg 7.x (Deprecated):**
```c
AVCodecContext* ctx = avcodec_alloc_context3(codec);
ctx->channels = 2;
ctx->channel_layout = AV_CH_LAYOUT_STEREO;
```

**FFmpeg 8.0 (Current):**
```c
AVCodecContext* ctx = avcodec_alloc_context3(codec);
AVChannelLayout layout;
av_channel_layout_default(&layout, 2);
ctx->ch_layout = layout;
// DO NOT use channels or channel_layout fields
```

**Migration Steps:**
1. Replace `ctx->channels` with `ctx->ch_layout.nb_channels`
2. Replace `ctx->channel_layout` with `av_channel_layout_default()`
3. Use `av_channel_layout_uninit()` to cleanup

---

### 2. Swresample API Changes

**FFmpeg 7.x:**
```c
SwrContext* swr = swr_alloc_set_opts(
    NULL,
    AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16, 48000,
    AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16, 48000,
    0, NULL
);
swr_init(swr);
```

**FFmpeg 8.0:**
```c
SwrContext* swr = NULL;
AVChannelLayout in_layout, out_layout;
av_channel_layout_default(&in_layout, 2);
av_channel_layout_default(&out_layout, 2);

swr_alloc_set_opts2(
    &swr,
    &out_layout, AV_SAMPLE_FMT_S16, 48000,
    &in_layout, AV_SAMPLE_FMT_S16, 48000,
    0, NULL
);
swr_init(swr);

av_channel_layout_uninit(&in_layout);
av_channel_layout_uninit(&out_layout);
```

**Migration Steps:**
1. Use `swr_alloc_set_opts2()` instead of `swr_alloc_set_opts()`
2. Pass channel layout pointers, not layouts directly
3. Uninit channel layouts after use

---

### 3. Audio Decoding API

**FFmpeg 7.x (Deprecated):**
```c
AVFrame* frame = av_frame_alloc();
int got_frame = 0;
int ret = avcodec_decode_audio4(ctx, frame, &got_frame, packet);
if (ret >= 0 && got_frame) {
    // Process frame
}
```

**FFmpeg 8.0 (Current):**
```c
AVPacket* packet = av_packet_alloc();
AVFrame* frame = av_frame_alloc();

// Send packet
ret = avcodec_send_packet(ctx, packet);
if (ret >= 0) {
    // Receive frames (may be multiple per packet)
    while (avcodec_receive_frame(ctx, frame) >= 0) {
        // Process frame
        av_frame_unref(frame);
    }
}

av_packet_free(&packet);
av_frame_free(&frame);
```

**Migration Steps:**
1. Use `avcodec_send_packet()` / `avcodec_receive_frame()` pattern
2. Loop to receive all frames from a packet
3. Properly free resources

---

### 4. Video Decoding API

**FFmpeg 7.x (Deprecated):**
```c
AVFrame* frame = av_frame_alloc();
int got_frame = 0;
int ret = avcodec_decode_video2(ctx, frame, &got_frame, packet);
if (ret >= 0 && got_frame) {
    // Process frame
}
```

**FFmpeg 8.0 (Current):**
```c
AVPacket* packet = av_packet_alloc();
AVFrame* frame = av_frame_alloc();

ret = avcodec_send_packet(ctx, packet);
if (ret >= 0) {
    while (avcodec_receive_frame(ctx, frame) >= 0) {
        // Process frame
        av_frame_unref(frame);
    }
}

av_packet_free(&packet);
av_frame_free(&frame);
```

**Migration Steps:**
1. Use `avcodec_send_packet()` / `avcodec_receive_frame()` pattern
2. Handle multiple frames per packet
3. Properly free resources

---

## 🔧 Code Changes Required

### File: `native/video-processing/src/audio_decoder.cpp`

#### Change 1: AAC Decoder Initialization

**Before (FFmpeg 7.x):**
```c
decoder->codecContext->channels = stream.channels;
decoder->codecContext->channel_layout = AV_CH_LAYOUT_STEREO;
```

**After (FFmpeg 8.0):**
```c
AVChannelLayout layout;
av_channel_layout_default(&layout, stream.channels);
decoder->codecContext->ch_layout = layout;
```

#### Change 2: Audio Resampler

**Before:**
```c
resampler->swrContext = swr_alloc_set_opts(
    NULL,
    ctx->ch_layout, ctx->sample_fmt, ctx->sample_rate,
    in_layout, in_fmt, in_rate,
    0, NULL
);
```

**After:**
```c
AVChannelLayout out_layout, in_layout;
av_channel_layout_default(&out_layout, out_channels);
av_channel_layout_default(&in_layout, in_channels);

SwrContext* newContext = NULL;
swr_alloc_set_opts2(&newContext,
                    &out_layout, out_fmt, out_rate,
                    &in_layout, in_fmt, in_rate,
                    0, NULL);

if (newContext) {
    swr_free(&resampler->swrContext);
    resampler->swrContext = newContext;
}

av_channel_layout_uninit(&in_layout);
av_channel_layout_uninit(&out_layout);
```

#### Change 3: AAC Decoding Loop

**Before:**
```c
AVFrame* frame = av_frame_alloc();
int got_frame = 0;
int ret = avcodec_decode_audio4(decoder->codecContext, frame, &got_frame, packet);
if (ret >= 0 && got_frame) {
    // Process frame
}
av_frame_free(&frame);
```

**After:**
```c
AVPacket* av_packet = av_packet_alloc();
AVFrame* frame = av_frame_alloc();

av_packet->data = (uint8_t*)data;
av_packet->size = dataSize;

int ret = avcodec_send_packet(decoder->codecContext, av_packet);
if (ret >= 0) {
    while (avcodec_receive_frame(decoder->codecContext, frame) >= 0) {
        // Process frame
        // Frame may contain multiple samples
        int samples = frame->nb_samples;
        int channels = frame->ch_layout.nb_channels;
        
        // Copy samples
        memcpy(output->samples.data(), frame->data[0], 
               samples * channels * sizeof(int16_t));
        
        av_frame_unref(frame);
    }
}

av_packet_free(&av_packet);
av_frame_free(&frame);
```

---

## 📝 Complete Migration Checklist

### Audio Decoder
- [ ] Replace `channels` with `ch_layout.nb_channels`
- [ ] Replace `channel_layout` with `av_channel_layout_default()`
- [ ] Update `swr_alloc_set_opts()` to `swr_alloc_set_opts2()`
- [ ] Add `av_channel_layout_uninit()` calls
- [ ] Update `avcodec_decode_audio4()` to send/receive pattern
- [ ] Handle multiple frames per packet
- [ ] Test AAC decoding
- [ ] Test G.711 decoding
- [ ] Test audio resampling

### Video Decoder (if implemented)
- [ ] Update `avcodec_decode_video2()` to send/receive pattern
- [ ] Handle multiple frames per packet
- [ ] Test H.264 decoding
- [ ] Test H.265 decoding
- [ ] Test MJPEG decoding

### Build System
- [ ] Update CMakeLists.txt for FFmpeg 8.0
- [ ] Update pkg-config paths
- [ ] Fix symbol visibility
- [ ] Test on Linux
- [ ] Test on Android
- [ ] Test on iOS
- [ ] Test on Desktop

---

## 🧪 Testing

### Unit Tests
```bash
# Test AAC decoder
./gradlew :native:video-processing:test --tests "*AacDecoderTest*"

# Test G.711 decoder
./gradlew :native:video-processing:test --tests "*G711DecoderTest*"

# Test audio resampler
./gradlew :native:video-processing:test --tests "*AudioResamplerTest*"
```

### Integration Tests
```bash
# Test with real camera
./scripts/test-rtsp-cameras.sh --cameras hikvision --duration 60

# Test all codecs
./scripts/test-rtsp-cameras.sh --cameras hikvision,dahua,axis --duration 300
```

### Performance Tests
```bash
# Benchmark decoding performance
./scripts/rtsp-benchmark.sh --duration 300 --output build/benchmark.json
```

---

## 📚 References

- [FFmpeg 8.0 Migration Guide](https://ffmpeg.org/download.html#build-8.0)
- [FFmpeg API Documentation](https://ffmpeg.org/doxygen/8.0/)
- [FFmpeg Release Notes](https://ffmpeg.org/download.html#release-8.0)
- [FFmpeg Source Code](https://github.com/FFmpeg/FFmpeg)

---

## ✅ Acceptance Criteria

- [ ] All audio decoder tests pass
- [ ] All video decoder tests pass
- [ ] Clean build on all platforms
- [ ] No compiler warnings
- [ ] No memory leaks (Valgrind clean)
- [ ] Performance within 10% of FFmpeg 7.x
- [ ] Documentation updated

---

**Created:** 27 April 2026  
**Owner:** Tech Lead  
**Status:** ⚠️ **In Progress**  
**Target Completion:** Week 1 of P0-1 task
