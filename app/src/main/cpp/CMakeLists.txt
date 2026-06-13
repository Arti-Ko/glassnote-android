cmake_minimum_required(VERSION 3.22)
project(glassnote)

set(CMAKE_CXX_STANDARD 17)

# ggml/whisper в Debug по умолчанию -O0 (катастрофически медленно). Форсируем -O3.
set(CMAKE_C_FLAGS_DEBUG "-O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_DEBUG "-O3 -DNDEBUG")

# Собираем whisper.cpp (определяет target `whisper`, тянет ggml).
add_subdirectory(whisper.cpp)

add_library(glassnote SHARED whisper_jni.cpp)

target_include_directories(glassnote PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/include)

target_link_libraries(glassnote whisper log android)
