cmake_minimum_required(VERSION 3.15)

project(LinkitNativesHelper)

file(GLOB src
     "*.h"
     "*.cpp"
)

include_directories(LinkitNativesHelper 
 	$ENV{JAVA_HOME}/include
 	$ENV{JAVA_HOME}/include/win32
	$ENV{JAVA_HOME}/include/win32/bridge
	D:/Perso/VisualStudio/VCTools/MSVC/14.29.30133/include
    ${src})

add_library(LinkitNativesHelper SHARED ${src})