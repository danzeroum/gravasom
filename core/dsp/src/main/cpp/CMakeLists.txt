cmake_minimum_required(VERSION 3.22.1)
project("gravasom_dsp" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(gravasom_dsp SHARED dsp_chain.cpp dsp_jni.cpp)
# Sem contracao FMA: o mesmo codigo da o mesmo resultado entre execucoes (determinismo).
target_compile_options(gravasom_dsp PRIVATE -O2 -ffp-contract=off -Wall -Wextra)
target_link_options(gravasom_dsp PRIVATE -Wl,-z,max-page-size=16384)
