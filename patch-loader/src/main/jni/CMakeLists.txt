cmake_minimum_required(VERSION 3.31.1)
project(npatch)

set(CMAKE_CXX_STANDARD 23)

file(GLOB_RECURSE CORE_SEARCH "${CMAKE_SOURCE_DIR}/core/**/CMakeLists.txt")

if(CORE_SEARCH)
    list(GET CORE_SEARCH 0 ACTUAL_CORE_FILE)
    get_filename_component(CORE_JNI_DIR "${ACTUAL_CORE_FILE}" DIRECTORY)
    message(STATUS "🔮 Đã bắt sống hang ổ tại: ${CORE_JNI_DIR}")
    add_subdirectory("${CORE_JNI_DIR}" "core_build")
else()
    message(FATAL_ERROR "💀 Vẫn không thấy xác!")
endif()

aux_source_directory(src SRC_LIST)
aux_source_directory(src/jni SRC_LIST)
set(SRC_LIST ${SRC_LIST} api/patch_main.cpp)
set(SRC_LIST ${SRC_LIST} src/jni/bypass_sig.cpp)
set(SRC_LIST ${SRC_LIST} src/jni/bypass_svc.cpp)

add_library(${PROJECT_NAME} SHARED ${SRC_LIST})

target_include_directories(${PROJECT_NAME} PUBLIC include)
target_include_directories(${PROJECT_NAME} PRIVATE src)

target_link_libraries(${PROJECT_NAME} core log)

if (DEFINED DEBUG_SYMBOLS_PATH)
    set(DEBUG_SYMBOLS_PATH ${DEBUG_SYMBOLS_PATH}/${API})
    message(STATUS "Debug symbols will be placed at ${DEBUG_SYMBOLS_PATH}")
    add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E make_directory ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}
            COMMAND ${CMAKE_OBJCOPY} --only-keep-debug $<TARGET_FILE:${PROJECT_NAME}>
            ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/${PROJECT_NAME}.debug
            COMMAND ${CMAKE_STRIP} --strip-all $<TARGET_FILE:${PROJECT_NAME}>
            COMMAND ${CMAKE_OBJCOPY} --add-gnu-debuglink ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/${PROJECT_NAME}.debug
            $<TARGET_FILE:${PROJECT_NAME}>)
endif()
