#include <jni.h>
#include <vector>
#include "JValueTranslator.h"
#include <string>
#include <algorithm>
#include "fr_linkit_engine_internal_manipulation_invokation_ObjectInvocator.h"
using namespace std;
using namespace std::literals;


jobject WrapPrimitive(JNIEnv* env, string className, string paramSignature, double value) {
	jclass clazz = env->FindClass(className.data());
	replace(className.begin(), className.end(), '.', '/');
	string signature = "(" + paramSignature + ")" + className + ";";
	jmethodID methodID = env->GetStaticMethodID(clazz, "valueOf", signature.data());
	return env->CallStaticObjectMethod(clazz, methodID, value);
}

std::vector<jvalue> GetJObjects(JNIEnv* env, jbyte* types, jobjectArray array) {
	const int len = env->GetArrayLength(array);
	std::vector<jvalue> vec(len);
	for (int i = 0; i < len; i++) {
		JValueType type = static_cast<JValueType>(types[i]);
		jvalue val = JObjectToJValue(env, type, env->GetObjectArrayElement(array, i));
		vec[i] = val;
	}
	return vec;
}

JNIEXPORT jobject JNICALL Java_fr_linkit_engine_internal_manipulation_invokation_ObjectInvocator_invokeMethod0
(JNIEnv* env, jclass clazz, jobject target, jstring name, jstring signature, jbyteArray paramTypes, jbyte returnType, jobjectArray arguments) {
	jclass targetClass = env->GetObjectClass(target);
	const char* signatureUTF = env->GetStringUTFChars(signature, false);
	const char* nameChars = env->GetStringUTFChars(name, false);
	jmethodID methodID = env->GetMethodID(targetClass, nameChars, signatureUTF);

	std::vector<jvalue> values = GetJObjects(env, env->GetByteArrayElements(paramTypes, false), arguments);
	const jvalue* valuesArray = values.data();
	switch (static_cast<JValueType>(returnType)) {
	case JValueType::VOID_FLAG:
		env->CallVoidMethodA(target, methodID, valuesArray);
		return NULL;
	case JValueType::OBJECT_FLAG:
		return env->CallObjectMethodA(target, methodID, valuesArray);
	};

	double val;
	switch (static_cast<JValueType>(returnType)) {
	case JValueType::BOOLEAN_FLAG:
		val = env->CallBooleanMethodA(target, methodID, valuesArray);
		return WrapPrimitive(env, "java.lang.Boolean", "Z", val);
	case JValueType::BYTE_FLAG:
		val = env->CallByteMethodA(target, methodID, valuesArray);
		return WrapPrimitive(env, "java.lang.Byte", "B", val);
	case JValueType::CHAR_FLAG:
		val = env->CallCharMethodA(target, methodID, valuesArray);
		return WrapPrimitive(env, "java.lang.Character", "C", val);
	case JValueType::DOUBLE_FLAG:
		val = env->CallDoubleMethodA(target, methodID, valuesArray);
		return WrapPrimitive(env, "java.lang.Double", "D", val);
	case JValueType::FLOAT_FLAG:
		val = env->CallFloatMethodA(target, methodID, valuesArray);
		return WrapPrimitive(env, "java.lang.Float", "F", val);
	case JValueType::INT_FLAG:
		val = env->CallIntMethodA(target, methodID, valuesArray);
		return WrapPrimitive(env, "java.lang.Int", "I", val);
	case JValueType::LONG_FLAG:
		val = env->CallLongMethodA(target, methodID, valuesArray);
		return WrapPrimitive(env, "java.lang.Long", "J", val);
	case JValueType::SHORT_FLAG:
		val = env->CallShortMethodA(target, methodID, valuesArray);
		return WrapPrimitive(env, "java.lang.Short", "S", val);
	default:
		return NULL;
	}

}

