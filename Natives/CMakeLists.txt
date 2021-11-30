cmake_minimum_required(VERSION 3.15)

project(LinkitNativesHelper)



add_library(LinkitNativesHelper SHARED ${SRC}
    "JValueTranslator.cpp"
    "fr_linkit_engine_internal_manipulation_creation_ObjectCreator.cpp"
    "fr_linkit_engine_internal_manipulation_invokation_ObjectInvocator.cpp")

target_include_directories(LinkitNativesHelper PRIVATE
 	$ENV{JAVA_HOME}/include
 	$ENV{JAVA_HOME}/include/win32
	$ENV{JAVA_HOME}/include/win32/bridge
    ${SRC})

target_sources(LinkitNativesHelper PRIVATE ${SRC})