cmake_minimum_required(VERSION 3.22.1)
project("gravasom_mp3" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(gravasom_mp3 SHARED lame_bridge.cpp)

find_library(log-lib log)
target_link_libraries(gravasom_mp3 ${log-lib})
