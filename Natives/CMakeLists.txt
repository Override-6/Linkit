cmake_minimum_required(VERSION 3.15)

project(LinkitNativesHelper)

include_directories(LinkitNativesHelper SHARED
 	./include
 	./include/win32
	./include/win32/bridge)

add_library(LinkitNativesHelper SHARED 
	fr_linkit_engine_internal_utils_NativeUtils.h
	fr_linkit_engine_internal_utils_NativeUtils.cpp
)