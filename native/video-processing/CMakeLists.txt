cmake_minimum_required(VERSION 3.15)

project(video_processing VERSION 1.0.0 LANGUAGES CXX)

# Генерация compile_commands.json для clangd и других инструментов
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Установка стандарта C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Политики CMake
if(POLICY CMP0077)
    cmake_policy(SET CMP0077 NEW)
endif()

# Определение платформы
if(ANDROID)
    set(PLATFORM_NAME "android")
    set(PLATFORM_ARCH ${ANDROID_ABI})
elseif(IOS)
    set(PLATFORM_NAME "ios")
    if(CMAKE_OSX_ARCHITECTURES MATCHES "arm64")
        set(PLATFORM_ARCH "arm64")
    elseif(CMAKE_OSX_ARCHITECTURES MATCHES "x86_64")
        set(PLATFORM_ARCH "x64")
    elseif(CMAKE_OSX_ARCHITECTURES MATCHES "arm64" AND CMAKE_SYSTEM_NAME MATCHES "Simulator")
        set(PLATFORM_ARCH "simulator-arm64")
    else()
        set(PLATFORM_ARCH "arm64")
    endif()
elseif(APPLE)
    set(PLATFORM_NAME "macos")
    if(CMAKE_OSX_ARCHITECTURES MATCHES "arm64")
        set(PLATFORM_ARCH "arm64")
    else()
        set(PLATFORM_ARCH "x64")
    endif()
elseif(WIN32)
    set(PLATFORM_NAME "windows")
    if(CMAKE_SIZEOF_VOID_P EQUAL 8)
        set(PLATFORM_ARCH "x64")
    else()
        set(PLATFORM_ARCH "x86")
    endif()
elseif(UNIX)
    set(PLATFORM_NAME "linux")
    if(CMAKE_SIZEOF_VOID_P EQUAL 8)
        set(PLATFORM_ARCH "x64")
    else()
        set(PLATFORM_ARCH "x86")
    endif()
endif()

message(STATUS "Building for platform: ${PLATFORM_NAME}/${PLATFORM_ARCH}")

# Исходные файлы
set(SOURCES
    src/video_decoder.cpp  # Включен для H.264/H.265/MJPEG декодирования
    src/rtsp_client.cpp  # Включен для тестирования FFmpeg декодирования
    src/audio_decoder.cpp  # Включен для FFmpeg 8.0 API
    # src/video_encoder.cpp  # Временно отключен для ускорения сборки
    # src/frame_processor.cpp  # Временно отключен для ускорения сборки
    # src/stream_manager.cpp  # Временно отключен для ускорения сборки
)

# JNI обертки
if(ANDROID)
    list(APPEND SOURCES src/jni/rtsp_client_jni.cpp)
    message(STATUS "JNI support enabled for Android")
elseif(JAVA_HOME OR JNI_FOUND)
    # Для Desktop платформ (JVM) используем Desktop JNI обертку
    # Проверяем наличие JNI
    find_package(JNI QUIET)
    if(JNI_FOUND)
        list(APPEND SOURCES src/jni/rtsp_client_jni_desktop.cpp)
        include_directories(${JNI_INCLUDE_DIRS})
        message(STATUS "JNI support enabled for Desktop (JVM)")
        message(STATUS "JNI include dirs: ${JNI_INCLUDE_DIRS}")
    else()
        message(WARNING "JNI not found - Desktop JNI wrapper will not be compiled")
    endif()
endif()

set(HEADERS
    include/video_decoder.h
    include/audio_decoder.h
    include/video_encoder.h
    include/frame_processor.h
    include/rtsp_client.h
    include/stream_manager.h
)

# Включение заголовочных файлов (до создания target)
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/src
)

# Определение типа библиотеки
if(ANDROID OR IOS)
    # Для мобильных платформ используем SHARED библиотеку
    add_library(video_processing SHARED ${SOURCES} ${HEADERS})
else()
    # Для десктопных платформ можно использовать как SHARED, так и STATIC
    option(BUILD_SHARED_LIBS "Build shared libraries" ON)
    if(BUILD_SHARED_LIBS)
        add_library(video_processing SHARED ${SOURCES} ${HEADERS})
    else()
        add_library(video_processing STATIC ${SOURCES} ${HEADERS})
    endif()
endif()

# Включение заголовочных файлов (для экспорта)
target_include_directories(video_processing
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Установка visibility символов (для правильного экспорта)
set_target_properties(video_processing PROPERTIES
    CXX_VISIBILITY_PRESET default
    VISIBILITY_INLINES_HIDDEN ON
    POSITION_INDEPENDENT_CODE ON
    VERSION ${PROJECT_VERSION}
    SOVERSION ${PROJECT_VERSION_MAJOR}
)

# Настройка выходных директорий
set_target_properties(video_processing PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib/${PLATFORM_NAME}/${PLATFORM_ARCH}
    ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib/${PLATFORM_NAME}/${PLATFORM_ARCH}
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin/${PLATFORM_NAME}/${PLATFORM_ARCH}
)

# Экспорт всех символов для C функций (нужно для cinterop)
if(UNIX AND NOT APPLE)
    set_target_properties(video_processing PROPERTIES
        LINK_FLAGS "-Wl,--export-dynamic"
    )
elseif(APPLE)
    set_target_properties(video_processing PROPERTIES
        MACOSX_RPATH ON
        BUILD_WITH_INSTALL_RPATH ON
        INSTALL_NAME_DIR "@rpath"
    )
elseif(WIN32)
    # Для Windows экспортируем все символы через .def файл или __declspec(dllexport)
    set_target_properties(video_processing PROPERTIES
        WINDOWS_EXPORT_ALL_SYMBOLS ON
    )
endif()

# Имя библиотеки в зависимости от платформы
if(WIN32)
    set_target_properties(video_processing PROPERTIES
        OUTPUT_NAME "video_processing"
        PREFIX ""
    )
elseif(APPLE)
    set_target_properties(video_processing PROPERTIES
        OUTPUT_NAME "video_processing"
    )
else()
    set_target_properties(video_processing PROPERTIES
        OUTPUT_NAME "video_processing"
    )
endif()

# Зависимости
# Threads для многопоточности
find_package(Threads REQUIRED)
target_link_libraries(video_processing
    PRIVATE
        Threads::Threads
)

# Windows sockets
if(WIN32)
    target_link_libraries(video_processing PRIVATE ws2_32)
endif()

# Android специфичные настройки
if(ANDROID)
    # Логирование для Android
    target_link_libraries(video_processing PRIVATE log)
    # JNI
    find_library(ANDROID_JNI_LIB jni PATHS ${ANDROID_NDK}/platforms/${ANDROID_PLATFORM}/arch-${ANDROID_ARCH}/usr/lib)
    if(ANDROID_JNI_LIB)
        target_link_libraries(video_processing PRIVATE ${ANDROID_JNI_LIB})
    endif()
    message(STATUS "Android-specific libraries linked")
endif()

# iOS специфичные настройки
if(IOS)
    # iOS использует встроенные фреймворки
    find_library(VIDEOTOOLBOX VideoToolbox)
    find_library(AVFOUNDATION AVFoundation)
    find_library(COREMEDIA CoreMedia)
    find_library(COREVIDEO CoreVideo)

    if(VIDEOTOOLBOX)
        target_link_libraries(video_processing PRIVATE ${VIDEOTOOLBOX})
    endif()
    if(AVFOUNDATION)
        target_link_libraries(video_processing PRIVATE ${AVFOUNDATION})
    endif()
    if(COREMEDIA)
        target_link_libraries(video_processing PRIVATE ${COREMEDIA})
    endif()
    if(COREVIDEO)
        target_link_libraries(video_processing PRIVATE ${COREVIDEO})
    endif()
    message(STATUS "iOS-specific frameworks linked")
endif()

# FFmpeg для RTSP, декодирования и кодирования видео
option(ENABLE_FFMPEG "Enable FFmpeg" ON)
if(ENABLE_FFMPEG)
    # Для Android используем специальный поиск
    if(ANDROID)
        find_library(FFMPEG_AVFORMAT_LIB avformat PATHS ${ANDROID_NDK}/sources/third_party/ffmpeg/${ANDROID_ABI})
        find_library(FFMPEG_AVCODEC_LIB avcodec PATHS ${ANDROID_NDK}/sources/third_party/ffmpeg/${ANDROID_ABI})
        find_library(FFMPEG_AVUTIL_LIB avutil PATHS ${ANDROID_NDK}/sources/third_party/ffmpeg/${ANDROID_ABI})
        find_library(FFMPEG_SWSCALE_LIB swscale PATHS ${ANDROID_NDK}/sources/third_party/ffmpeg/${ANDROID_ABI})
        find_library(FFMPEG_SWRESAMPLE_LIB swresample PATHS ${ANDROID_NDK}/sources/third_party/ffmpeg/${ANDROID_ABI})

        if(FFMPEG_AVFORMAT_LIB AND FFMPEG_AVCODEC_LIB AND FFMPEG_AVUTIL_LIB)
            set(FFMPEG_FOUND TRUE)
            set(FFMPEG_LIBRARIES
                ${FFMPEG_AVFORMAT_LIB}
                ${FFMPEG_AVCODEC_LIB}
                ${FFMPEG_AVUTIL_LIB}
                ${FFMPEG_SWSCALE_LIB}
                ${FFMPEG_SWRESAMPLE_LIB}
            )
            find_path(FFMPEG_INCLUDE_DIR libavformat/avformat.h PATHS ${ANDROID_NDK}/sources/third_party/ffmpeg/include)
            set(FFMPEG_INCLUDE_DIRS ${FFMPEG_INCLUDE_DIR})
        endif()
    # Для iOS используем специальный поиск
    elseif(IOS)
        find_library(FFMPEG_AVFORMAT_LIB avformat PATHS ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/ios/${PLATFORM_ARCH}/lib)
        find_library(FFMPEG_AVCODEC_LIB avcodec PATHS ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/ios/${PLATFORM_ARCH}/lib)
        find_library(FFMPEG_AVUTIL_LIB avutil PATHS ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/ios/${PLATFORM_ARCH}/lib)
        find_library(FFMPEG_SWSCALE_LIB swscale PATHS ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/ios/${PLATFORM_ARCH}/lib)
        find_library(FFMPEG_SWRESAMPLE_LIB swresample PATHS ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/ios/${PLATFORM_ARCH}/lib)

        if(FFMPEG_AVFORMAT_LIB AND FFMPEG_AVCODEC_LIB AND FFMPEG_AVUTIL_LIB)
            set(FFMPEG_FOUND TRUE)
            set(FFMPEG_LIBRARIES
                ${FFMPEG_AVFORMAT_LIB}
                ${FFMPEG_AVCODEC_LIB}
                ${FFMPEG_AVUTIL_LIB}
                ${FFMPEG_SWSCALE_LIB}
                ${FFMPEG_SWRESAMPLE_LIB}
            )
            find_path(FFMPEG_INCLUDE_DIR libavformat/avformat.h PATHS ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/ios/${PLATFORM_ARCH}/include)
            set(FFMPEG_INCLUDE_DIRS ${FFMPEG_INCLUDE_DIR})
        endif()
    # Для десктопных платформ используем pkg-config или find_package
    else()
        find_package(PkgConfig QUIET)
        if(PkgConfig_FOUND)
            pkg_check_modules(FFMPEG QUIET
                libavformat
                libavcodec
                libavutil
                libswscale
                libswresample
            )
        endif()

        if(NOT FFMPEG_FOUND)
            # Попытка найти через find_path
            find_path(FFMPEG_INCLUDE_DIR
                NAMES libavformat/avformat.h
                PATHS
                    ${CMAKE_SOURCE_DIR}/third_party/ffmpeg
                    /usr/local/include
                    /usr/include
                    C:/ffmpeg/include
                    "$ENV{FFMPEG_DIR}/include"
            )

            if(FFMPEG_INCLUDE_DIR)
                set(FFMPEG_FOUND TRUE)
                set(FFMPEG_INCLUDE_DIRS ${FFMPEG_INCLUDE_DIR})

                # Поиск библиотек
                find_library(FFMPEG_AVFORMAT_LIB avformat PATHS
                    ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/lib
                    /usr/local/lib
                    /usr/lib
                    C:/ffmpeg/lib
                    "$ENV{FFMPEG_DIR}/lib"
                )
                find_library(FFMPEG_AVCODEC_LIB avcodec PATHS
                    ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/lib
                    /usr/local/lib
                    /usr/lib
                    C:/ffmpeg/lib
                    "$ENV{FFMPEG_DIR}/lib"
                )
                find_library(FFMPEG_AVUTIL_LIB avutil PATHS
                    ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/lib
                    /usr/local/lib
                    /usr/lib
                    C:/ffmpeg/lib
                    "$ENV{FFMPEG_DIR}/lib"
                )
                find_library(FFMPEG_SWSCALE_LIB swscale PATHS
                    ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/lib
                    /usr/local/lib
                    /usr/lib
                    C:/ffmpeg/lib
                    "$ENV{FFMPEG_DIR}/lib"
                )
                find_library(FFMPEG_SWRESAMPLE_LIB swresample PATHS
                    ${CMAKE_SOURCE_DIR}/third_party/ffmpeg/lib
                    /usr/local/lib
                    /usr/lib
                    C:/ffmpeg/lib
                    "$ENV{FFMPEG_DIR}/lib"
                )

                if(FFMPEG_AVFORMAT_LIB AND FFMPEG_AVCODEC_LIB AND FFMPEG_AVUTIL_LIB)
                    set(FFMPEG_LIBRARIES
                        ${FFMPEG_AVFORMAT_LIB}
                        ${FFMPEG_AVCODEC_LIB}
                        ${FFMPEG_AVUTIL_LIB}
                        ${FFMPEG_SWSCALE_LIB}
                        ${FFMPEG_SWRESAMPLE_LIB}
                    )
                endif()
            endif()
        endif()
    endif()

    if(FFMPEG_FOUND)
        target_include_directories(video_processing
            PUBLIC
                ${FFMPEG_INCLUDE_DIRS}
                ${CMAKE_CURRENT_SOURCE_DIR}/include
        )
        target_link_libraries(video_processing PRIVATE ${FFMPEG_LIBRARIES})
        target_compile_definitions(video_processing PRIVATE ENABLE_FFMPEG)
        message(STATUS "FFmpeg enabled for video_processing")
        message(STATUS "FFmpeg libraries: ${FFMPEG_LIBRARIES}")
        message(STATUS "FFmpeg include dirs: ${FFMPEG_INCLUDE_DIRS}")
    else()
        message(WARNING "FFmpeg not found. Install FFmpeg or set FFMPEG_DIR environment variable")
        message(WARNING "Building without FFmpeg support - RTSP client will have limited functionality")
        set(ENABLE_FFMPEG OFF)
    endif()
endif()

# OpenCV для обработки изображений
option(ENABLE_OPENCV "Enable OpenCV" OFF)
if(ENABLE_OPENCV)
    find_package(OpenCV REQUIRED COMPONENTS core imgproc)
    if(OpenCV_FOUND)
        message(STATUS "OpenCV version: ${OpenCV_VERSION}")
        message(STATUS "OpenCV libraries: ${OpenCV_LIBS}")
        message(STATUS "OpenCV include dirs: ${OpenCV_INCLUDE_DIRS}")

        target_link_libraries(video_processing PRIVATE ${OpenCV_LIBS})
        target_include_directories(video_processing
            PUBLIC
                ${CMAKE_CURRENT_SOURCE_DIR}/include
                ${OpenCV_INCLUDE_DIRS}
        )
        target_compile_definitions(video_processing PRIVATE ENABLE_OPENCV)
        message(STATUS "OpenCV enabled for video_processing")
    else()
        message(WARNING "OpenCV not found. Install OpenCV or disable ENABLE_OPENCV")
        set(ENABLE_OPENCV OFF)
    endif()
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
        # Для Android и iOS не используем -march=native (кросс-компиляция)
        if(ANDROID OR IOS)
            target_compile_options(video_processing PRIVATE -O3)
        else()
            target_compile_options(video_processing PRIVATE -O3 -march=native)
        endif()
    endif()
endif()

# Android и iOS специфичные определения
if(ANDROID)
    target_compile_definitions(video_processing PRIVATE ANDROID __ANDROID_API__=${ANDROID_PLATFORM_LEVEL})
endif()

if(IOS)
    target_compile_definitions(video_processing PRIVATE IOS TARGET_OS_IPHONE=1)
endif()
