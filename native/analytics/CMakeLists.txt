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

# Создание библиотеки
add_library(analytics ${SOURCES} ${HEADERS})

# Зависимости
target_link_libraries(analytics
    PRIVATE
        Threads::Threads
        ${OpenCV_LIBS}
)

if(ENABLE_OPENCV)
    target_include_directories(analytics
        PUBLIC
            ${CMAKE_CURRENT_SOURCE_DIR}/include
            ${OpenCV_INCLUDE_DIRS}
    )
endif()

if(ENABLE_TENSORFLOW)
    target_include_directories(analytics
        PRIVATE
            ${TFLITE_INCLUDE_DIR}
    )
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

