cmake_minimum_required(VERSION 3.15)

# Интеграционные тесты для RTSP Client
project(integration_tests VERSION 1.0.0 LANGUAGES CXX)

# Стандарт C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Опции
option(BUILD_INTEGRATION_TESTS "Build integration tests" ON)

if(NOT BUILD_INTEGRATION_TESTS)
    return()
endif()

# Интеграционные тесты
set(INTEGRATION_TEST_SOURCES
    test_av_sync.cpp
    test_rtp_streaming.cpp
    test_reconnect.cpp
)

# Создание исполняемого файла
add_executable(integration_tests ${INTEGRATION_TEST_SOURCES})

# Включение заголовков
target_include_directories(integration_tests
    PRIVATE
        ${CMAKE_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/src
        ${CMAKE_CURRENT_SOURCE_DIR}
)

# Threads
find_package(Threads REQUIRED)
target_link_libraries(integration_tests PRIVATE Threads::Threads)

# Windows sockets
if(WIN32)
    target_link_libraries(integration_tests PRIVATE ws2_32)
endif()

# Компилятор-специфичные опции
if(MSVC)
    target_compile_options(integration_tests PRIVATE /W4)
else()
    target_compile_options(integration_tests PRIVATE -Wall -Wextra -Wpedantic)
endif()

# Добавление тестов в CTest
enable_testing()
add_test(NAME integration_tests COMMAND integration_tests)

message(STATUS "Integration tests enabled")
