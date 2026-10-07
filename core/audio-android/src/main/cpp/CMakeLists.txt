cmake_minimum_required(VERSION 3.22.1)
project("gravasom_mp3" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# LAME (LGPL-2.0+) como biblioteca COMPARTILHADA separada (libmp3lame.so): o usuario pode
# substituir a biblioteca por uma versao modificada, como a LGPL exige. Ver docs/adr/007-*.md.
file(GLOB LAME_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/lame/libmp3lame/*.c)
add_library(mp3lame SHARED ${LAME_SOURCES})
target_include_directories(mp3lame PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/lame
    ${CMAKE_CURRENT_SOURCE_DIR}/lame/include
    ${CMAKE_CURRENT_SOURCE_DIR}/lame/libmp3lame)
target_compile_definitions(mp3lame PRIVATE HAVE_CONFIG_H NDEBUG)
target_compile_options(mp3lame PRIVATE -w -O2)
target_link_options(mp3lame PRIVATE -Wl,-z,max-page-size=16384)

# Ponte JNI propria (codigo do Gravasom), ligada dinamicamente ao LAME.
add_library(gravasom_mp3 SHARED lame_bridge.cpp mp3_encoder.cpp)
target_include_directories(gravasom_mp3 PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/lame/include)
target_link_options(gravasom_mp3 PRIVATE -Wl,-z,max-page-size=16384)
find_library(log-lib log)
target_link_libraries(gravasom_mp3 mp3lame ${log-lib})
