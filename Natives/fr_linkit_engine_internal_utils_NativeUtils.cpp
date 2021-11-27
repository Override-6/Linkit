#include "fr_linkit_engine_internal_utils_NativeUtils.h"
#include <jni.h>
#include <string>
#include <typeinfo>
#include <iostream>
#include <fstream>
using namespace std;

std::ofstream debugfile("C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home\\NativesCommunication.txt");

const short BYTE_FLAG = 0;
const short BOOLEAN_FLAG = 1;
const short CHAR_FLAG = 2;
const short SHORT_FLAG = 3;
const short INT_FLAG = 4;
const short LONG_FLAG = 5;
const short FLOAT_FLAG = 6;
const short DOUBLE_FLAG = 7;


const char* GetJClassName(JNIEnv* env, jclass clazz) {
	jclass classClass = env->FindClass("java/lang/Class");
	jmethodID mid_getName = env->GetMethodID(classClass, "getName", "()Ljava/lang/String;");
	jstring str = (jstring)env->CallObjectMethod(clazz, mid_getName);
	return env->GetStringUTFChars(str, false);
}

jvalue ConvertToJValue(JNIEnv* env, jbyte type, double n) {
	jvalue val{};
	debugfile << "\tConvertToJValue : type = " << type << " value = " << n << endl;
	switch (type) {
	case BYTE_FLAG:
		val.b = n;
		break;
	case BOOLEAN_FLAG:
		val.z = n;
		break;
	case CHAR_FLAG:
		val.c = n;
		break;
	case SHORT_FLAG:
		val.s = n;
		break;
	case INT_FLAG:
		val.i = n;
		break;
	case LONG_FLAG:
		val.j = n;
		break;
	case FLOAT_FLAG:
		val.f = n;
		break;
	case DOUBLE_FLAG:
		val.d = n;
		break;
	}
	return val;
}

jvalue JObjectToJValue(JNIEnv* env, jbyte type, jobject object) {
	jclass clazz = env->GetObjectClass(object);
	const char* name = GetJClassName(env, clazz);
	debugfile << "JObjectToValue :" << endl;
	debugfile << "class name : " << name << endl;

	if (strcmp(name, "java.lang.Boolean") == 0) {
		jfieldID valueID = env->GetFieldID(clazz, "value", "Z");
		return ConvertToJValue(env, type, env->GetBooleanField(object, valueID));
	} 
	else if (strcmp(name, "java.lang.Byte") == 0) {
		jfieldID valueID = env->GetFieldID(clazz, "value", "B");
		return ConvertToJValue(env, type, env->GetByteField(object, valueID));
	}
	else if (strcmp(name, "java.lang.Character") == 0) {
		jfieldID valueID = env->GetFieldID(clazz, "value", "C");
		return ConvertToJValue(env, type, env->GetCharField(object, valueID));
	}
	else if (strcmp(name, "java.lang.Short") == 0) {
		jfieldID valueID = env->GetFieldID(clazz, "value", "S");
		return ConvertToJValue(env, type, env->GetShortField(object, valueID));
	}
	else if (strcmp(name, "java.lang.Integer") == 0) {
		jfieldID valueID = env->GetFieldID(clazz, "value", "I");
		return ConvertToJValue(env, type, env->GetIntField(object, valueID));
	}
	else if (strcmp(name, "java.lang.Long") == 0) {
		jfieldID valueID = env->GetFieldID(clazz, "value", "J");
		return ConvertToJValue(env, type, env->GetLongField(object, valueID));
	}
	else if (strcmp(name, "java.lang.Float") == 0) {
		jfieldID valueID = env->GetFieldID(clazz, "value", "F");
		return ConvertToJValue(env, type, env->GetFloatField(object, valueID));
	}
	else if (strcmp(name, "java.lang.Double") == 0) {
		jfieldID valueID = env->GetFieldID(clazz, "value", "D");
		return ConvertToJValue(env, type, env->GetDoubleField(object, valueID));
	}
	else {
		jvalue val;
		val.l = object;
		return val;
	}
}

jvalue* GetJObjects(JNIEnv* env, jbyte* types, jobjectArray array) {
	const int len = env->GetArrayLength(array);
	jvalue* buff = new jvalue[len];
	for (int i = 0; i < len; i++) {
		buff[i] = JObjectToJValue(env, types[i], env->GetObjectArrayElement(array, i));
	}
	return buff;
}

JNIEXPORT void JNICALL Java_fr_linkit_engine_internal_utils_NativeUtils_callConstructor
  (JNIEnv* env, jclass clazz, jobject target, jstring signature, jbyteArray types, jobjectArray arguments) {
	jclass targetClass = env->GetObjectClass(target);
	const char* signatureUTF = env->GetStringUTFChars(signature, false);
	jmethodID constructorID = env->GetMethodID(targetClass, "<init>", signatureUTF);

	const jvalue* objects = GetJObjects(env, env ->GetByteArrayElements(types, false), arguments);
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


