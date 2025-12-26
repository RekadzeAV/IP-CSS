cmake_minimum_required(VERSION 3.15)

project(video_processing)

# Исходные файлы
set(SOURCES
    src/video_decoder.cpp
    src/video_encoder.cpp
    src/frame_processor.cpp
    src/rtsp_client.cpp
    src/stream_manager.cpp
)

set(HEADERS
    include/video_decoder.h
    include/video_encoder.h
    include/frame_processor.h
    include/rtsp_client.h
    include/stream_manager.h
)

# Создание библиотеки
add_library(video_processing ${SOURCES} ${HEADERS})

# Зависимости
target_link_libraries(video_processing
    PRIVATE
        Threads::Threads
        ${OpenCV_LIBS}
)

if(ENABLE_OPENCV)
    target_include_directories(video_processing
        PUBLIC
            ${CMAKE_CURRENT_SOURCE_DIR}/include
            ${OpenCV_INCLUDE_DIRS}
    )
endif()

# Компилятор-специфичные опции
if(MSVC)
    target_compile_options(video_processing PRIVATE /W4 /WX-)
else()
    target_compile_options(video_processing PRIVATE -Wall -Wextra -Wpedantic)
endif()

# Оптимизации
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(video_processing PRIVATE /O2)
    else()
        target_compile_options(video_processing PRIVATE -O3 -march=native)
    endif()
endif()

