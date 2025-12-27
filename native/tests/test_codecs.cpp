#include <gtest/gtest.h>
#include "codec_manager.h"

TEST(CodecManagerTest, H264Supported) {
    EXPECT_TRUE(codec_is_supported(CODEC_TYPE_H264));
}

TEST(CodecManagerTest, H265Supported) {
    EXPECT_TRUE(codec_is_supported(CODEC_TYPE_H265));
}

TEST(CodecManagerTest, MjpegSupported) {
    EXPECT_TRUE(codec_is_supported(CODEC_TYPE_MJPEG));
}

TEST(CodecManagerTest, GetCodecInfo) {
    CodecInfo info;
    EXPECT_TRUE(codec_get_info(CODEC_TYPE_H264, &info));
    EXPECT_EQ(info.type, CODEC_TYPE_H264);
    EXPECT_GT(info.maxWidth, 0);
    EXPECT_GT(info.maxHeight, 0);
}

TEST(CodecManagerTest, SelectBestCodec) {
    CodecType best = codec_select_best(CODEC_TYPE_H264, true);
    EXPECT_TRUE(best == CODEC_TYPE_H264 || best == CODEC_TYPE_H265 || best == CODEC_TYPE_MJPEG);
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}

