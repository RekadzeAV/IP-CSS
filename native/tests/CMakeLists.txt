cmake_minimum_required(VERSION 3.15)

project(NativeLibrariesTests)

# Включение Google Test
include(FetchContent)
FetchContent_Declare(
    googletest
    GIT_REPOSITORY https://github.com/google/googletest.git
    GIT_TAG v1.14.0
)
FetchContent_MakeAvailable(googletest)

# Добавляем include директории
include_directories(
    ${CMAKE_SOURCE_DIR}/../video-processing/include
    ${CMAKE_SOURCE_DIR}/../analytics/include
    ${CMAKE_SOURCE_DIR}/../codecs/include
)

# Тесты для codecs
add_executable(test_codecs
    test_codecs.cpp
)

target_link_libraries(test_codecs
    PRIVATE
        GTest::gtest
        GTest::gtest_main
        ${CMAKE_SOURCE_DIR}/../codecs/libcodecs.a
)

# Тесты для analytics (если доступны)
if(ENABLE_OPENCV)
    add_executable(test_analytics
        test_analytics.cpp
    )

    target_link_libraries(test_analytics
        PRIVATE
            GTest::gtest
            GTest::gtest_main
            ${CMAKE_SOURCE_DIR}/../analytics/libanalytics.a
            ${OpenCV_LIBS}
    )
endif()

# Включение тестов
enable_testing()
add_test(NAME CodecsTests COMMAND test_codecs)

if(ENABLE_OPENCV)
    add_test(NAME AnalyticsTests COMMAND test_analytics)
endif()

