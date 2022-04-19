


set(additional_compiler_flags ${opt_level} ${gcc_opts})
set(additional_linker_flags -Wl,-Map=${map_file} ${opt_level} ${gcc_opts})

#target_compile_definitions(${elf_file} PRIVATE TRACE OS_USE_TRACE_SEMIHOSTING_STDOUT)
target_compile_features(${elf_file} PRIVATE c_std_11) # cxx_std_14 # mixed compilation not supported by eclipse when importing from cmake!
target_compile_options(${elf_file}
        PRIVATE
        ${additional_compiler_flags}
        -DTARGET=${target}
        #            -Wall -Wextra -Werror -Wno-error=unused-parameter -Wno-error=unused-function -Wno-error=unused-variable -Wno-error=unused-const-variable
        )
target_link_libraries(${elf_file} PRIVATE ${additional_linker_flags})

# remove unused sections
target_link_libraries(${elf_file} PUBLIC "-g -Wl,--gc-sections")

add_custom_command(
        OUTPUT ${dis_file}
        COMMAND ${CMAKE_OBJDUMP} -D ${elf_file} > ${dis_file}
        DEPENDS ${elf_file}
)

add_custom_target(disassemble DEPENDS ${dis_file})

if(target STREQUAL "riscv")
    set(disasm_dir "${CMAKE_CURRENT_LIST_DIR}/../../risc-v/disasm/src/test/resources/")
elseif(target STREQUAL "z")
    set(disasm_dir "${CMAKE_CURRENT_LIST_DIR}/../../zArch/disasm/src/test/resources/")
endif()

add_custom_target(copyToResources
        COMMAND cp ${elf_file} ${dis_file} ${disasm_dir}
        DEPENDS ${elf_file} ${dis_file}
        )