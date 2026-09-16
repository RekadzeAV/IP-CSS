cmake_minimum_required(VERSION 3.15)

# Тесты для нативной библиотеки video_processing
project(video_processing_tests VERSION 1.0.0 LANGUAGES CXX)

# Стандарт C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Поиск библиотеки video_processing
find_library(VIDEO_PROCESSING_LIB
    NAMES video_processing
    PATHS
        ${CMAKE_SOURCE_DIR}/build/lib
        ${CMAKE_SOURCE_DIR}/../build/lib
        ${CMAKE_BINARY_DIR}/../lib
)

# Поиск Google Test (опционально, можно использовать другой фреймворк)
find_package(GTest QUIET)

if(NOT GTest_FOUND)
    message(STATUS "Google Test not found, using simple test framework")
    # Простой тестовый фреймворк без зависимостей
    set(USE_SIMPLE_TESTS ON)
else()
    set(USE_SIMPLE_TESTS OFF)
    message(STATUS "Google Test found, using GTest")
endif()

# FFmpeg для тестов
option(ENABLE_FFMPEG "Enable FFmpeg" ON)
if(ENABLE_FFMPEG)
    find_package(PkgConfig QUIET)
    if(PkgConfig_FOUND)
        pkg_check_modules(FFMPEG QUIET
            libavformat
            libavcodec
            libavutil
            libswscale
        )
    endif()

    if(NOT FFMPEG_FOUND)
        find_path(FFMPEG_INCLUDE_DIR
            NAMES libavformat/avformat.h
            PATHS
                ${CMAKE_SOURCE_DIR}/../third_party/ffmpeg
                /usr/local/include
                /usr/include
                C:/ffmpeg/include
                "$ENV{FFMPEG_DIR}/include"
        )

        if(FFMPEG_INCLUDE_DIR)
            set(FFMPEG_FOUND TRUE)
            set(FFMPEG_INCLUDE_DIRS ${FFMPEG_INCLUDE_DIR})
        endif()
    endif()

    if(FFMPEG_FOUND)
        target_compile_definitions(video_processing_tests PRIVATE ENABLE_FFMPEG)
        message(STATUS "FFmpeg enabled for tests")
    endif()
endif()

# Тестовые исходники
set(TEST_SOURCES
    test_video_decoder.cpp
    test_rtp_processing.cpp
    test_audio_decoder.cpp  # Включен для тестирования аудио декодера
    test_main.cpp
    test_utils.cpp
)

# Создание тестового исполняемого файла
if(USE_SIMPLE_TESTS)
    add_executable(video_processing_tests ${TEST_SOURCES})
else()
    add_executable(video_processing_tests ${TEST_SOURCES})
    target_link_libraries(video_processing_tests PRIVATE GTest::gtest GTest::gtest_main)
endif()

# Линковка с video_processing
if(VIDEO_PROCESSING_LIB)
    target_link_libraries(video_processing_tests PRIVATE ${VIDEO_PROCESSING_LIB})
else()
    message(WARNING "video_processing library not found, tests may not compile")
endif()

# Включение заголовков
target_include_directories(video_processing_tests
    PRIVATE
        ${CMAKE_SOURCE_DIR}/../include
        ${CMAKE_SOURCE_DIR}/../src
        ${CMAKE_CURRENT_SOURCE_DIR}  # Для test_utils.h
        ${FFMPEG_INCLUDE_DIRS}
)

# Линковка FFmpeg
if(FFMPEG_FOUND)
    if(FFMPEG_LIBRARIES)
        target_link_libraries(video_processing_tests PRIVATE ${FFMPEG_LIBRARIES})
    else()
        find_library(FFMPEG_AVCODEC_LIB avcodec PATHS
            ${CMAKE_SOURCE_DIR}/../third_party/ffmpeg/lib
            /usr/local/lib
            /usr/lib
            C:/ffmpeg/lib
            "$ENV{FFMPEG_DIR}/lib"
        )
        find_library(FFMPEG_AVUTIL_LIB avutil PATHS
            ${CMAKE_SOURCE_DIR}/../third_party/ffmpeg/lib
            /usr/local/lib
            /usr/lib
            C:/ffmpeg/lib
            "$ENV{FFMPEG_DIR}/lib"
        )
        if(FFMPEG_AVCODEC_LIB AND FFMPEG_AVUTIL_LIB)
            target_link_libraries(video_processing_tests PRIVATE
                ${FFMPEG_AVCODEC_LIB}
                ${FFMPEG_AVUTIL_LIB}
            )
        endif()
    endif()
endif()

# Threads
find_package(Threads REQUIRED)
target_link_libraries(video_processing_tests PRIVATE Threads::Threads)

# Windows sockets
if(WIN32)
    target_link_libraries(video_processing_tests PRIVATE ws2_32)
endif()

# Компилятор-специфичные опции
if(MSVC)
    target_compile_options(video_processing_tests PRIVATE /W4 /WX-)
else()
    target_compile_options(video_processing_tests PRIVATE -Wall -Wextra)
endif()

# Тестовый анализатор
add_executable(test_analyzer test_analyzer.cpp)
target_include_directories(test_analyzer PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# Добавление тестов в CTest
enable_testing()
add_test(NAME video_processing_tests COMMAND video_processing_tests)
