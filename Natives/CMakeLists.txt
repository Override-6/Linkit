cmake_minimum_required(VERSION 3.15)

project(LinkitNatives)
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/bin)

add_library(LinkitNatives SHARED ${SRC}
    "JValueTranslator.cpp"
    "fr_linkit_engine_internal_manipulation_creation_ObjectCreator.cpp"
    "fr_linkit_engine_internal_manipulation_invokation_ObjectInvocator.cpp")

target_include_directories(LinkitNatives PRIVATE
 	$ENV{JAVA_HOME}/include
 	$ENV{JAVA_HOME}/include/linux
	$ENV{JAVA_HOME}/include/linux/bridge
    ${SRC})

target_sources(LinkitNatives PRIVATE ${SRC})
set_target_properties(LinkitNatives PROPERTIES PREFIX "")