cmake_minimum_required(VERSION 3.15)

project(LinkitNatives)
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/bin)
set(JNI_INCLUDE $ENV{JAVA_HOME}/include)



add_library(LinkitNatives SHARED
        "JValueTranslator.cpp"
        "fr_linkit_engine_internal_manipulation_creation_ObjectCreator.cpp"
        "fr_linkit_engine_internal_manipulation_invokation_ObjectInvocator.cpp")

if (UNIX)
    target_include_directories(LinkitNatives PRIVATE ${JNI_INCLUDE} ${JNI_INCLUDE}/linux ${JNI_INCLUDE}/linux/bridge ${SRC})
elseif (APPLE)
    target_include_directories(LinkitNatives PRIVATE ${JNI_INCLUDE} ${JNI_INCLUDE}/macos ${JNI_INCLUDE}/macos/bridge ${SRC})
elseif(WIN32)
    target_include_directories(LinkitNatives PRIVATE ${JNI_INCLUDE} ${JNI_INCLUDE}/windows ${JNI_INCLUDE}/windows/bridge ${SRC})
endif()
target_sources(LinkitNatives PRIVATE ${SRC})
set_target_properties(LinkitNatives PROPERTIES PREFIX "")