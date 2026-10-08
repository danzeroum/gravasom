cmake_minimum_required(VERSION 3.22.1)
project("gravasom_dsp" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# WebRTC VAD vendorizado sem alteracoes (webrtc_vad/VENDOR.sha256). C de terceiros: sem -Wall do projeto.
file(GLOB WEBRTC_VAD_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/webrtc_vad/webrtc/common_audio/signal_processing/*.c
    ${CMAKE_CURRENT_SOURCE_DIR}/webrtc_vad/webrtc/common_audio/vad/*.c)
add_library(webrtc_vad STATIC ${WEBRTC_VAD_SOURCES})
target_include_directories(webrtc_vad PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/webrtc_vad)
target_compile_definitions(webrtc_vad PRIVATE WEBRTC_POSIX)
target_compile_options(webrtc_vad PRIVATE -O2 -w -fPIC)

add_library(gravasom_dsp SHARED dsp_chain.cpp dsp_jni.cpp vad_stream.cpp vad_jni.cpp)
target_link_libraries(gravasom_dsp PRIVATE webrtc_vad)
# Sem contracao FMA: o mesmo codigo da o mesmo resultado entre execucoes (determinismo).
target_compile_options(gravasom_dsp PRIVATE -O2 -ffp-contract=off -Wall -Wextra)
target_link_options(gravasom_dsp PRIVATE -Wl,-z,max-page-size=16384)
