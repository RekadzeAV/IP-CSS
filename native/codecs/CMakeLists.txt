cmake_minimum_required(VERSION 3.15)

project(codecs)

# Исходные файлы
set(SOURCES
    src/h264_codec.cpp
    src/h265_codec.cpp
    src/mjpeg_codec.cpp
    src/codec_manager.cpp
)

set(HEADERS
    include/h264_codec.h
    include/h265_codec.h
    include/mjpeg_codec.h
    include/codec_manager.h
)

# Создание библиотеки
add_library(codecs ${SOURCES} ${HEADERS})

# Зависимости
target_link_libraries(codecs
    PRIVATE
        Threads::Threads
)

# FFmpeg для кодеков
if(ENABLE_FFMPEG AND FFMPEG_FOUND)
    target_include_directories(codecs
        PRIVATE
            ${FFMPEG_INCLUDE_DIRS}
    )
    if(FFMPEG_LIBRARIES)
        target_link_libraries(codecs PRIVATE ${FFMPEG_LIBRARIES})
    else()
        target_link_libraries(codecs PRIVATE
            avformat avcodec avutil swscale swresample
        )
    endif()
    target_compile_definitions(codecs PRIVATE ENABLE_FFMPEG)
    message(STATUS "FFmpeg enabled for codecs")
endif()

# Компилятор-специфичные опции
if(MSVC)
    target_compile_options(codecs PRIVATE /W4 /WX-)
else()
    target_compile_options(codecs PRIVATE -Wall -Wextra -Wpedantic)
endif()

# Оптимизации
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(codecs PRIVATE /O2)
    else()
        target_compile_options(codecs PRIVATE -O3 -march=native)
    endif()
endif()

