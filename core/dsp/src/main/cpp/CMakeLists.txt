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

# RNNoise v0.2 (BSD-3-Clause): fontes e modelo baixados POR HASH por scripts/buscar-rnnoise.sh (tarefa Gradle fetchRnnoise);
# nao ficam no git (o modelo e um C de ~29 MB). O shim cobre o os_support.h que o RNNoise inclui mas nao distribui.
if(NOT DEFINED RNNOISE_DIR OR NOT EXISTS "${RNNOISE_DIR}/src/rnnoise_data.c")
    message(FATAL_ERROR "RNNOISE_DIR ausente: rode scripts/buscar-rnnoise.sh <dir> (o Gradle faz isso pela tarefa fetchRnnoise)")
endif()
set(RNNOISE_SOURCES
    ${RNNOISE_DIR}/src/denoise.c ${RNNOISE_DIR}/src/rnn.c ${RNNOISE_DIR}/src/pitch.c ${RNNOISE_DIR}/src/kiss_fft.c
    ${RNNOISE_DIR}/src/celt_lpc.c ${RNNOISE_DIR}/src/nnet.c ${RNNOISE_DIR}/src/nnet_default.c
    ${RNNOISE_DIR}/src/parse_lpcnet_weights.c ${RNNOISE_DIR}/src/rnnoise_data.c ${RNNOISE_DIR}/src/rnnoise_tables.c)
add_library(rnnoise STATIC ${RNNOISE_SOURCES})
target_include_directories(rnnoise PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/rnnoise_shim ${RNNOISE_DIR}/include ${RNNOISE_DIR}/src)
target_compile_options(rnnoise PRIVATE -O3 -w -fPIC -DNDEBUG -ffunction-sections -fdata-sections -fvisibility=hidden)

add_library(gravasom_dsp SHARED dsp_chain.cpp dsp_jni.cpp vad_stream.cpp vad_jni.cpp
    resampler.cpp noise_reduce.cpp rnnoise_denoiser.cpp)
target_link_libraries(gravasom_dsp PRIVATE webrtc_vad rnnoise)
target_link_options(gravasom_dsp PRIVATE -Wl,--gc-sections)
# Sem contracao FMA: o mesmo codigo da o mesmo resultado entre execucoes (determinismo).
target_compile_options(gravasom_dsp PRIVATE -O2 -ffp-contract=off -Wall -Wextra)
target_link_options(gravasom_dsp PRIVATE -Wl,-z,max-page-size=16384)
