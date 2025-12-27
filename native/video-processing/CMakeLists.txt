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

# Создание shared библиотеки для динамической загрузки
add_library(video_processing SHARED ${SOURCES} ${HEADERS})

# Установка visibility символов (для правильного экспорта)
set_target_properties(video_processing PROPERTIES
    CXX_VISIBILITY_PRESET default
    VISIBILITY_INLINES_HIDDEN ON
    POSITION_INDEPENDENT_CODE ON
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
    )
endif()

# Зависимости
target_link_libraries(video_processing
    PRIVATE
        Threads::Threads
)

# FFmpeg для RTSP, декодирования и кодирования видео
option(ENABLE_FFMPEG "Enable FFmpeg" ON)
if(ENABLE_FFMPEG)
    find_package(PkgConfig REQUIRED)
    if(PkgConfig_FOUND)
        pkg_check_modules(FFMPEG REQUIRED 
            libavformat 
            libavcodec 
            libavutil 
            libswscale
            libswresample
        )
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
        message(WARNING "FFmpeg not found. Install FFmpeg or set FFMPEG_INCLUDE_DIR")
        set(ENABLE_FFMPEG OFF)
    endif()
endif()

# OpenCV для обработки изображений
option(ENABLE_OPENCV "Enable OpenCV" ON)
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
        target_compile_options(video_processing PRIVATE -O3 -march=native)
    endif()
endif()

