cmake_minimum_required(VERSION 3.15)

# Fuzzing тесты для RTSP Client
project(fuzzing_tests VERSION 1.0.0 LANGUAGES CXX)

# Стандарт C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Опции
option(BUILD_FUZZING_TESTS "Build fuzzing tests" OFF)

if(NOT BUILD_FUZZING_TESTS)
    return()
endif()

# Проверка наличия libFuzzer
include(CheckCXXCompilerFlag)
check_cxx_compiler_flag("-fsanitize=fuzzer" HAS_FUZZER)

if(NOT HAS_FUZZER)
    message(WARNING "libFuzzer not supported, disabling fuzzing tests")
    return()
endif()

# Fuzzing тесты
set(FUZZING_SOURCES
    fuzz_sdp_parser.cpp
    fuzz_rtp_parser.cpp
    fuzz_url_parser.cpp
)

# Создание fuzzing исполняемых файлов
foreach(fuzz_source ${FUZZING_SOURCES})
    get_filename_component(fuzz_name ${fuzz_source} NAME_WE)
    add_executable(${fuzz_name} ${fuzz_source})
    target_compile_options(${fuzz_name} PRIVATE -fsanitize=fuzzer,address)
    target_link_options(${fuzz_name} PRIVATE -fsanitize=fuzzer,address)
    target_include_directories(${fuzz_name} PRIVATE
        ${CMAKE_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/src
    )
    message(STATUS "Added fuzzing target: ${fuzz_name}")
endforeach()

# Fuzzing corpora
set(FUZZING_CORPORA
    sdp_corpus/
    rtp_corpus/
    url_corpus/
)

foreach(corpus ${FUZZING_CORPORA})
    if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/${corpus})
        message(STATUS "Found corpus: ${corpus}")
    endif()
endforeach()

message(STATUS "Fuzzing tests enabled")
