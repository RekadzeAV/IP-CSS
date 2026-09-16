cmake_minimum_required(VERSION 3.15)

# Unit тесты для HW Decoder
project(hw_decoder_tests VERSION 1.0.0 LANGUAGES CXX)

# Стандарт C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Опции
option(ENABLE_FFMPEG "Enable FFmpeg" ON)
option(BUILD_HW_DECODER_TESTS "Build HW Decoder tests" ON)

if(NOT BUILD_HW_DECODER_TESTS)
    return()
endif()

# Исходные файлы тестов
set(HW_DECODER_TEST_SOURCES
    test_hw_decoder.cpp
)

# Создание исполняемого файла
add_executable(hw_decoder_tests ${HW_DECODER_TEST_SOURCES})

# Включение заголовков
target_include_directories(hw_decoder_tests
    PRIVATE
        ${CMAKE_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/src
        ${CMAKE_CURRENT_SOURCE_DIR}
)

# macOS: VideoToolbox
if(APPLE)
    target_link_libraries(hw_decoder_tests PRIVATE
        "-framework VideoToolbox"
        "-framework CoreMedia"
        "-framework CoreVideo"
    )
    target_compile_definitions(hw_decoder_tests PRIVATE __APPLE__)
endif()

# Windows: DXVA2/D3D11
if(WIN32)
    target_link_libraries(hw_decoder_tests PRIVATE
        d3d11
        dxva2
        dxguid
    )
    target_compile_definitions(hw_decoder_tests PRIVATE _WIN32)
endif()

# Threads
find_package(Threads REQUIRED)
target_link_libraries(hw_decoder_tests PRIVATE Threads::Threads)

# Компилятор-специфичные опции
if(MSVC)
    target_compile_options(hw_decoder_tests PRIVATE /W4)
else()
    target_compile_options(hw_decoder_tests PRIVATE -Wall -Wextra -Wpedantic)
endif()

# Добавление тестов в CTest
enable_testing()
add_test(NAME hw_decoder_tests COMMAND hw_decoder_tests)

message(STATUS "HW Decoder tests enabled")
