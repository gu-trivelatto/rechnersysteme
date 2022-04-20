cmake_minimum_required(VERSION 3.14)

if(DEFINED target)
    message("Using target ${target}!")
else()
    set(target "rv")
endif()

if(target STREQUAL "rv")
    if (NOT DEFINED ARCH)
        set(ARCH "rv32imfc")
        set(ABI "ilp32f")
    else()
        message("Using '${ARCH}' as target-architecture")
        message("Using '${ABI}' as target-ABI")
    endif()
    set(CMAKE_TOOLCHAIN_FILE GNU-RISCV-RV32G-newlib-Toolchain.cmake)
elseif(target STREQUAL "x86")
    if (NOT DEFINED ARCH)
        set(ARCH "x86_64")
    else()
        message("Using '${ARCH}' as target-architecture")
    endif()
else()
    message(FATAL_ERROR "No valid target architecture defined: ${target}!")
endif()

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
    message("Selecting Release Build Mode, because none was demanded on command-line!")
endif()

project(acquisition C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

if(CMAKE_BUILD_TYPE MATCHES Debug)
    set(opt_level -Og -g3)
    set(flagName Og)
else()
    set(opt_level -O3 -g3 -fno-optimize-sibling-calls)
    set(flagName O3)
endif()

set(elf_file ${PROJECT_NAME}.${ARCH}.${flagName}.elf)
set(map_file ${PROJECT_NAME}.${ARCH}.${flagName}.map)
set(dis_file ${PROJECT_NAME}.${ARCH}.${flagName}.dis)

set(SOURCES
        main.c
        acquisition.h
        acquisition.c
        testData.h
        testData.c
        )

add_executable(${elf_file} ${SOURCES})

target_link_libraries(${elf_file} PRIVATE m)

include(shared.additional.cmake)