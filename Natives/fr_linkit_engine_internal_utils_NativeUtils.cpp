#include "fr_linkit_engine_internal_utils_NativeUtils.h"
#include <jni.h>

jobject* GetJObjects(JNIEnv* env, jobjectArray array) {
	const int len = env->GetArrayLength(array);
	jobject* buff = new jobject[len];
	for (int i = 0; i < len; i++) {
		buff[i] = env->GetObjectArrayElement(array, i);
	}
	return buff;
}

JNIEXPORT void JNICALL Java_fr_linkit_engine_internal_utils_NativeUtils_callConstructor
  (JNIEnv* env, jclass clazz, jobject target, jstring signature, jobjectArray arguments) {
	jclass targetClass = env->GetObjectClass(target);
	const char* signatureUTF = env->GetStringUTFChars(signature, false);
	jmethodID constructorID = env->GetMethodID(targetClass, "<init>", signatureUTF);
	jobject* objects = GetJObjects(env, arguments);
	env->CallVoidMethod(target, constructorID, *objects);
}

JNIEXPORT jobject JNICALL Java_fr_linkit_engine_internal_utils_NativeUtils_allocate
(JNIEnv* env, jclass clazz, jclass target) {
	return env->AllocObject(target);
}

