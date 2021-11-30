cmake_minimum_required(VERSION 3.15)

project(LinkitNativesHelper)

include_directories(LinkitNativesHelper SHARED
 	$ENV{JAVA_HOME}/include
 	$ENV{JAVA_HOME}/include/win32
	$ENV{JAVA_HOME}/include/win32/bridge
	D:/Perso/VisualStudio/VCTools/MSVC/14.29.30133/include/vector.h
	)

file(GLOB src
     "*.h"
     "*.cpp"
)

add_library(LinkitNativesHelper SHARED ${src})