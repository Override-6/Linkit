#include "fr_linkit_engine_internal_utils_NativeUtils.h"
#include <jni.h>
#include <string>
#include <typeinfo>

using namespace std;
jvalue* GetJObjects(JNIEnv* env, jobjectArray array) {
	const int len = env->GetArrayLength(array);
	jvalue* buff = new jvalue[len];
	for (int i = 0; i < len; i++) {
		buff[i] = JObjectToJValue(env, env->GetObjectArrayElement(array, i));
	}
	return buff;
}

jvalue JObjectToJValue(JNIEnv* env, jobject object) {
	jvalue val;
	const char* objectClassName = GetJClassName(env, object);
	
}

const char* GetJClassName(JNIEnv* env, jobject object) {
	jclass classClass = env->FindClass("java/lang/Class");
	jmethodID mid_getName = env->GetMethodID(classClass, "getName", "()Ljava/lang/String;");
	jstring str = (jstring)env->CallObjectMethod(classClass, mid_getName);
	return env->GetStringUTFChars(str, false);
}

JNIEXPORT void JNICALL Java_fr_linkit_engine_internal_utils_NativeUtils_callConstructor
  (JNIEnv* env, jclass clazz, jobject target, jstring signature, jobjectArray arguments) {
	jclass targetClass = env->GetObjectClass(target);
	const char* signatureUTF = env->GetStringUTFChars(signature, false);
	jmethodID constructorID = env->GetMethodID(targetClass, "<init>", signatureUTF);
	const jvalue* objects = GetJObjects(env, arguments);
	jobject argumentsLength = env->NewStringUTF(std::to_string(env->GetArrayLength(arguments)).c_str());
	jobject x = env->NewStringUTF("x");
	env->CallVoidMethodA(target, constructorID, objects);
}

JNIEXPORT jobject JNICALL Java_fr_linkit_engine_internal_utils_NativeUtils_allocate
  (JNIEnv* env, jclass clazz, jclass target) {
	return env->AllocObject(target);
}

JNIEXPORT jint JNICALL Java_fr_linkit_engine_internal_utils_NativeUtils_testArgs
(JNIEnv* env, jclass clazz, jobjectArray args) {
	return env->GetArrayLength(args);
}


