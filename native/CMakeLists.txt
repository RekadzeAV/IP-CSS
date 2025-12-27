cmake_minimum_required(VERSION 3.15)
project(IPCameraSurveillanceNative)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Опции сборки
option(BUILD_SHARED_LIBS "Build shared libraries" ON)
option(ENABLE_GPU "Enable GPU acceleration" ON)
option(ENABLE_OPENCV "Enable OpenCV" ON)
option(ENABLE_TENSORFLOW "Enable TensorFlow Lite" ON)

# Директории
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)

# Общие зависимости (должны быть найдены до add_subdirectory)
find_package(Threads REQUIRED)

# Поддиректории
add_subdirectory(video-processing)
add_subdirectory(analytics)
add_subdirectory(codecs)

# FFmpeg для RTSP, декодирования и кодирования видео
option(ENABLE_FFMPEG "Enable FFmpeg" ON)
if(ENABLE_FFMPEG)
    find_package(PkgConfig QUIET)
    if(PkgConfig_FOUND)
        pkg_check_modules(FFMPEG QUIET libavformat libavcodec libavutil libswscale libswresample)
    endif()

    if(NOT FFMPEG_FOUND)
        # Попытка найти через find_package
        find_path(FFMPEG_INCLUDE_DIR
            NAMES libavformat/avformat.h
            PATHS
                ${CMAKE_SOURCE_DIR}/third_party/ffmpeg
                /usr/local/include
                /usr/include
                C:/ffmpeg/include
        )

        if(FFMPEG_INCLUDE_DIR)
            set(FFMPEG_FOUND TRUE)
            set(FFMPEG_INCLUDE_DIRS ${FFMPEG_INCLUDE_DIR})
            message(STATUS "FFmpeg found: ${FFMPEG_INCLUDE_DIR}")
        else()
            message(WARNING "FFmpeg not found. Install FFmpeg or set FFMPEG_INCLUDE_DIR")
            set(ENABLE_FFMPEG OFF)
        endif()
    else()
        message(STATUS "FFmpeg found via pkg-config")
    endif()
endif()

# OpenCV
if(ENABLE_OPENCV)
    find_package(OpenCV REQUIRED)
    if(OpenCV_FOUND)
        message(STATUS "OpenCV version: ${OpenCV_VERSION}")
        include_directories(${OpenCV_INCLUDE_DIRS})
    endif()
endif()

# TensorFlow Lite
if(ENABLE_TENSORFLOW)
    find_path(TFLITE_INCLUDE_DIR
        NAMES tensorflow/lite/interpreter.h
        PATHS
            ${CMAKE_SOURCE_DIR}/third_party/tensorflow
            /usr/local/include
            /usr/include
    )

    if(TFLITE_INCLUDE_DIR)
        include_directories(${TFLITE_INCLUDE_DIR})
        message(STATUS "TensorFlow Lite found: ${TFLITE_INCLUDE_DIR}")
    else()
        message(WARNING "TensorFlow Lite not found, analytics will be disabled")
        set(ENABLE_TENSORFLOW OFF)
    endif()
endif()

# GPU ускорение
if(ENABLE_GPU)
    # CUDA для NVIDIA
    find_package(CUDA QUIET)
    if(CUDA_FOUND)
        enable_language(CUDA)
        message(STATUS "CUDA found: ${CUDA_VERSION}")
        set(ENABLE_CUDA ON)
    endif()

    # OpenCL
    find_package(OpenCL QUIET)
    if(OpenCL_FOUND)
        message(STATUS "OpenCL found")
        set(ENABLE_OPENCL ON)
    endif()
endif()

# Платформо-специфичные настройки
if(WIN32)
    add_definitions(-DWIN32_LEAN_AND_MEAN)
    add_definitions(-DNOMINMAX)
elseif(APPLE)
    set(CMAKE_OSX_DEPLOYMENT_TARGET "11.0")
    set(CMAKE_OSX_ARCHITECTURES "arm64;x86_64")
elseif(UNIX)
    # Linux
    add_definitions(-D_LINUX)
endif()

# Установка
install(TARGETS video_processing analytics codecs
    RUNTIME DESTINATION bin
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
)

install(DIRECTORY include/ DESTINATION include)

