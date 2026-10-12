cmake_minimum_required(VERSION 3.22.1)
project("gravasom_transcribe" LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)

# whisper.cpp (MIT) vem de WHISPER_SRC, apontado por -PwhisperSrc no build (mesma fonte do spike, ADR-013).
# Estatico: so o necessario entra na .so; a lib externa (whisper) e ligada dentro da gravasom_transcribe.
if(NOT DEFINED WHISPER_SRC)
    message(FATAL_ERROR "WHISPER_SRC nao definido: rode com -PwhisperSrc=<caminho do whisper.cpp>")
endif()
add_subdirectory(${WHISPER_SRC} whisper_build EXCLUDE_FROM_ALL)

add_library(gravasom_transcribe SHARED whisper_jni.cpp)
target_include_directories(gravasom_transcribe PRIVATE ${WHISPER_SRC}/include ${WHISPER_SRC}/ggml/include)
target_link_libraries(gravasom_transcribe PRIVATE whisper log)
target_compile_options(gravasom_transcribe PRIVATE -O3 -fvisibility=hidden)
target_link_options(gravasom_transcribe PRIVATE -Wl,--gc-sections -Wl,-z,max-page-size=16384)
