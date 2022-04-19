set(CMAKE_SYSTEM_NAME Generic)
set(CMAKE_SYSTEM_PROCESSOR risc-v)

set(CMAKE_TRY_COMPILE_TARGET_TYPE STATIC_LIBRARY)

set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)

if(NOT DEFINED CMAKE_C_COMPILER)
    set(CMAKE_C_COMPILER riscv32-unknown-elf-gcc)
else()
    message("Using C Compiler from Command-Line")
endif()
if(NOT DEFINED CMAKE_CXX_COMPILER)
    set(CMAKE_CXX_COMPILER riscv32-unknown-elf-g++)
else()
    message("Using CXX Compiler from Command-Line")
endif()
if(NOT DEFINED CMAKE_ASM_COMPILER)
    set(CMAKE_CXX_COMPILER riscv32-unknown-elf-gcc)
else()
    message("Using ASM Compiler from Command-Line")
endif()
#set(CMAKE_OBJCOPY arm-none-eabi-objcopy) # These should be inferred from the compiler
#if(NOT DEFINED CMAKE_OBJDUMP)
#    set(CMAKE_OBJDUMP riscv32-unknown-elf-objdump)
#else()
#    message("Using Objdump from Command-Line")
#endif()

if (NOT DEFINED ABI)
    set(ABI "ilp32d")
else()
    message("Using '${ABI}' as target-architecture")
endif()

set(CORE_FLAGS "-march=${ARCH} -mabi=${ABI}")

set(COMMON_FLAGS "")

# compiler: language specific flags
set(CMAKE_C_FLAGS "${CORE_FLAGS} ${COMMON_FLAGS}" CACHE INTERNAL "c compiler flags")
set(CMAKE_C_FLAGS_DEBUG "-DDEBUG" CACHE INTERNAL "c compiler flags: Debug")
set(CMAKE_C_FLAGS_RELEASE "" CACHE INTERNAL "c compiler flags: Release")

set(CMAKE_CXX_FLAGS "${CORE_FLAGS} ${COMMON_FLAGS} -fno-rtti -fno-exceptions" CACHE INTERNAL "cxx compiler flags")
set(CMAKE_CXX_FLAGS_DEBUG "-DDEBUG" CACHE INTERNAL "cxx compiler flags: Debug")
set(CMAKE_CXX_FLAGS_RELEASE "" CACHE INTERNAL "cxx compiler flags: Release")

set(CMAKE_ASM_FLAGS "${CORE_FLAGS} -g -ggdb3 -D__USES_CXX" CACHE INTERNAL "asm compiler flags")
set(CMAKE_ASM_FLAGS_DEBUG "" CACHE INTERNAL "asm compiler flags: Debug")
set(CMAKE_ASM_FLAGS_RELEASE "" CACHE INTERNAL "asm compiler flags: Release")
