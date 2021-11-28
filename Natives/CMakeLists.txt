cmake_minimum_required(VERSION 3.15)

project(LinkitNativesHelper)

include_directories(LinkitNativesHelper SHARED
 	$ENV{JAVA_HOME}/include
 	$ENV{JAVA_HOME}/include/win32
	$ENV{JAVA_HOME}/include/win32/bridge)

add_library(LinkitNativesHelper SHARED 
	fr_linkit_engine_internal_utils_NativeUtils.h
	fr_linkit_engine_internal_utils_NativeUtils.cpp
)