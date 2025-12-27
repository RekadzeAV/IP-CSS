cmake_minimum_required(VERSION 3.15)

project(analytics)

# Исходные файлы
set(SOURCES
    src/object_detector.cpp
    src/object_tracker.cpp
    src/anpr_engine.cpp
    src/face_detector.cpp
    src/motion_detector.cpp
)

set(HEADERS
    include/object_detector.h
    include/object_tracker.h
    include/anpr_engine.h
    include/face_detector.h
    include/motion_detector.h
)

# Включение заголовочных файлов (до создания target)
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/src
)

# Создание библиотеки
add_library(analytics ${SOURCES} ${HEADERS})

# Включение заголовочных файлов (для экспорта)
target_include_directories(analytics
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Зависимости
target_link_libraries(analytics
    PRIVATE
        Threads::Threads
)

if(ENABLE_OPENCV AND OpenCV_FOUND)
    target_link_libraries(analytics PRIVATE ${OpenCV_LIBS})
    target_include_directories(analytics
        PUBLIC
            ${CMAKE_CURRENT_SOURCE_DIR}/include
            ${OpenCV_INCLUDE_DIRS}
    )
    target_compile_definitions(analytics PRIVATE ENABLE_OPENCV)
endif()

if(ENABLE_TENSORFLOW AND TFLITE_INCLUDE_DIR)
    target_include_directories(analytics
        PRIVATE
            ${TFLITE_INCLUDE_DIR}
    )
    target_compile_definitions(analytics PRIVATE ENABLE_TENSORFLOW)
    # TensorFlow Lite библиотеки должны быть добавлены вручную
    # target_link_libraries(analytics PRIVATE tensorflow-lite)
endif()

# GPU ускорение
if(ENABLE_CUDA)
    enable_language(CUDA)
    target_compile_definitions(analytics PRIVATE ENABLE_CUDA)
endif()

if(ENABLE_OPENCL)
    find_package(OpenCL REQUIRED)
    target_link_libraries(analytics PRIVATE ${OpenCL_LIBRARIES})
    target_include_directories(analytics PRIVATE ${OpenCL_INCLUDE_DIRS})
    target_compile_definitions(analytics PRIVATE ENABLE_OPENCL)
endif()

# Компилятор-специфичные опции
if(MSVC)
    target_compile_options(analytics PRIVATE /W4 /WX-)
else()
    target_compile_options(analytics PRIVATE -Wall -Wextra -Wpedantic)
endif()

