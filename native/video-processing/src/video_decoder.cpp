#include "video_decoder.h"

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
}
#endif

#include <cstring>
#include <cstdlib>
#include <memory>

#ifdef ENABLE_FFMPEG

struct VideoDecoder {
    AVCodecContext* codecContext;
    AVFrame* frame;
    AVFrame* frameRGB;
    AVPacket* packet;
    SwsContext* swsContext;
    VideoCodec codec;
    int width;
    int height;
    FrameDecodedCallback callback;
    void* userData;
    
    VideoDecoder() : codecContext(nullptr), frame(nullptr), 
                     frameRGB(nullptr), packet(nullptr), swsContext(nullptr),
                     codec(VIDEO_CODEC_UNKNOWN), width(0), height(0),
                     callback(nullptr), userData(nullptr) {}
};

VideoDecoder* video_decoder_create(VideoCodec codec, int width, int height) {
    AVCodecID avCodecId;
    
    switch (codec) {
        case VIDEO_CODEC_H264:
            avCodecId = AV_CODEC_ID_H264;
            break;
        case VIDEO_CODEC_H265:
            avCodecId = AV_CODEC_ID_H265;
            break;
        case VIDEO_CODEC_MJPEG:
            avCodecId = AV_CODEC_ID_MJPEG;
            break;
        default:
            return nullptr;
    }
    
    const AVCodec* avCodec = avcodec_find_decoder(avCodecId);
    if (!avCodec) {
        return nullptr;
    }
    
    std::unique_ptr<VideoDecoder> decoder(new VideoDecoder());
    decoder->codec = codec;
    decoder->width = width;
    decoder->height = height;
    
    decoder->codecContext = avcodec_alloc_context3(avCodec);
    if (!decoder->codecContext) {
        return nullptr;
    }
    
    decoder->codecContext->width = width;
    decoder->codecContext->height = height;
    decoder->codecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    
    if (avcodec_open2(decoder->codecContext, avCodec, nullptr) < 0) {
        avcodec_free_context(&decoder->codecContext);
        return nullptr;
    }
    
    decoder->frame = av_frame_alloc();
    decoder->frameRGB = av_frame_alloc();
    decoder->packet = av_packet_alloc();
    
    if (!decoder->frame || !decoder->frameRGB || !decoder->packet) {
        video_decoder_destroy(decoder.release());
        return nullptr;
    }
    
    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGB24, width, height, 1);
    uint8_t* buffer = (uint8_t*)av_malloc(numBytes * sizeof(uint8_t));
    av_image_fill_arrays(decoder->frameRGB->data, decoder->frameRGB->linesize,
                         buffer, AV_PIX_FMT_RGB24, width, height, 1);
    
    decoder->swsContext = sws_getContext(
        width, height, AV_PIX_FMT_YUV420P,
        width, height, AV_PIX_FMT_RGB24,
        SWS_BILINEAR, nullptr, nullptr, nullptr
    );
    
    return decoder.release();
}

void video_decoder_destroy(VideoDecoder* decoder) {
    if (!decoder) return;
    
    if (decoder->swsContext) {
        sws_freeContext(decoder->swsContext);
    }
    
    if (decoder->frameRGB) {
        av_free(decoder->frameRGB->data[0]);
        av_frame_free(&decoder->frameRGB);
    }
    
    if (decoder->frame) {
        av_frame_free(&decoder->frame);
    }
    
    if (decoder->packet) {
        av_packet_free(&decoder->packet);
    }
    
    if (decoder->codecContext) {
        avcodec_free_context(&decoder->codecContext);
    }
    
    delete decoder;
}

bool video_decoder_decode(
    VideoDecoder* decoder,
    const uint8_t* data,
    size_t dataSize,
    int64_t timestamp
) {
    if (!decoder || !data || dataSize == 0) {
        return false;
    }
    
    decoder->packet->data = const_cast<uint8_t*>(data);
    decoder->packet->size = static_cast<int>(dataSize);
    decoder->packet->pts = timestamp;
    
    int ret = avcodec_send_packet(decoder->codecContext, decoder->packet);
    if (ret < 0) {
        return false;
    }
    
    ret = avcodec_receive_frame(decoder->codecContext, decoder->frame);
    if (ret < 0) {
        return false;
    }
    
    // Конвертируем YUV в RGB
    sws_scale(decoder->swsContext,
              decoder->frame->data, decoder->frame->linesize, 0, decoder->height,
              decoder->frameRGB->data, decoder->frameRGB->linesize);
    
    if (decoder->callback) {
        DecodedFrame decodedFrame;
        decodedFrame.width = decoder->width;
        decodedFrame.height = decoder->height;
        decodedFrame.timestamp = timestamp;
        decodedFrame.format = 1; // RGB24
        decodedFrame.dataSize = decoder->width * decoder->height * 3;
        
        // Копируем данные кадра
        decodedFrame.data = (uint8_t*)malloc(decodedFrame.dataSize);
        memcpy(decodedFrame.data, decoder->frameRGB->data[0], decodedFrame.dataSize);
        
        decoder->callback(&decodedFrame, decoder->userData);
    }
    
    return true;
}

void video_decoder_set_callback(
    VideoDecoder* decoder,
    FrameDecodedCallback callback,
    void* userData
) {
    if (decoder) {
        decoder->callback = callback;
        decoder->userData = userData;
    }
}

bool video_decoder_get_info(
    VideoDecoder* decoder,
    int* width,
    int* height,
    VideoCodec* codec
) {
    if (!decoder) {
        return false;
    }
    
    if (width) *width = decoder->width;
    if (height) *height = decoder->height;
    if (codec) *codec = decoder->codec;
    
    return true;
}

void decoded_frame_release(DecodedFrame* frame) {
    if (frame && frame->data) {
        free(frame->data);
        frame->data = nullptr;
    }
}

#else
// Заглушки если FFmpeg не включен
VideoDecoder* video_decoder_create(VideoCodec codec, int width, int height) {
    return nullptr;
}

void video_decoder_destroy(VideoDecoder* decoder) {
}

bool video_decoder_decode(
    VideoDecoder* decoder,
    const uint8_t* data,
    size_t dataSize,
    int64_t timestamp
) {
    return false;
}

void video_decoder_set_callback(
    VideoDecoder* decoder,
    FrameDecodedCallback callback,
    void* userData
) {
}

bool video_decoder_get_info(
    VideoDecoder* decoder,
    int* width,
    int* height,
    VideoCodec* codec
) {
    return false;
}

void decoded_frame_release(DecodedFrame* frame) {
}
#endif
