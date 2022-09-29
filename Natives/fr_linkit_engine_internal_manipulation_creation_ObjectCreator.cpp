#include <jni.h>
#include <string>
#include <algorithm>
#include <fstream>
#include "JValueTranslator.h"
#include "fr_linkit_engine_internal_manipulation_creation_ObjectCreator.h"
#include <utility>
#include <vector>

using namespace std;


jobject GetField(JNIEnv* env, jobject target, jobject field, string returnType) {
	jfieldID fieldID = env->FromReflectedField(field);
	JValueType tpe = ClassNameToType(std::move(returnType));
	if (tpe == JValueType::OBJECT_FLAG) {
		return env->GetObjectField(target, fieldID);
	}
	switch (tpe) {
	case JValueType::BOOLEAN_FLAG: {
		jboolean val = env->GetBooleanField(target, fieldID);
		return WrapPrimitive(env, "java.lang.Boolean", "Z", val);
	}
	case JValueType::BYTE_FLAG: {
		jbyte val = env->GetByteField(target, fieldID);
		return WrapPrimitive(env, "java.lang.Byte", "B", val);
	}
	case JValueType::CHAR_FLAG: {
		jchar val = env->GetCharField(target, fieldID);
		return WrapPrimitive(env, "java.lang.Character", "C", val);
	}
	case JValueType::DOUBLE_FLAG: {
		jdouble val = env->GetDoubleField(target, fieldID);
		return WrapPrimitive(env, "java.lang.Double", "D", val);
	}
	case JValueType::FLOAT_FLAG: {
		jfloat val = env->GetFloatField(target, fieldID);
		return WrapPrimitive(env, "java.lang.Float", "F", val);
	}
	case JValueType::INT_FLAG: {
		jint val = env->GetIntField(target, fieldID);
		return WrapPrimitive(env, "java.lang.Integer", "I", val);
	}
	case JValueType::LONG_FLAG: {
		jlong val = env->GetLongField(target, fieldID);
		return WrapPrimitive(env, "java.lang.Long", "J", val);
	}
	case JValueType::SHORT_FLAG: {
		jshort val = env->GetShortField(target, fieldID);
		return WrapPrimitive(env, "java.lang.Short", "S", val);
	}
	default:
		return NULL;
	}
}

void PutField(JNIEnv* env, jobject target, jobject field, string returnType, jobject data) {
	jfieldID fieldID = env->FromReflectedField(field);
	JValueType tpe = ClassNameToType(returnType);
	if (tpe == JValueType::OBJECT_FLAG) {
		env->SetObjectField(target, fieldID, data);
		return;
	}
	//setting primitive
	double primitive = UnwrapPrimitive(env, tpe, data);
	switch (tpe) {
	case JValueType::BOOLEAN_FLAG:
		env->SetBooleanField(target, fieldID, primitive);
		break;
	case JValueType::BYTE_FLAG:
		env->SetByteField(target, fieldID, primitive);
		break;
	case JValueType::CHAR_FLAG:
		env->SetCharField(target, fieldID, primitive);
		break;
	case JValueType::SHORT_FLAG:
		env->SetShortField(target, fieldID, primitive);
		break;
	case JValueType::INT_FLAG:
		env->SetIntField(target, fieldID, primitive);
		break;
	case JValueType::LONG_FLAG:
		env->SetLongField(target, fieldID, primitive);
		break;
	case JValueType::FLOAT_FLAG:
		env->SetFloatField(target, fieldID, primitive);
		break;
	case JValueType::DOUBLE_FLAG:
		env->SetDoubleField(target, fieldID, primitive);
	};
}

JNIEXPORT jobject JNICALL Java_fr_linkit_engine_internal_manipulation_creation_ObjectCreator_allocate
(JNIEnv* env, jclass clazz, jclass target) {
	return env->AllocObject(target);
}


JNIEXPORT void JNICALL Java_fr_linkit_engine_internal_manipulation_creation_ObjectCreator_pasteAllFields0
(JNIEnv* env, jclass clazz, jobject target, jobjectArray fields, jobjectArray fieldReturnTypes, jobjectArray fieldValues) {
	int len = env->GetArrayLength(fields);

	for (int i = 0; i < len; i++) {
		jobject field = env->GetObjectArrayElement(fields, i);
		string returnType(env->GetStringUTFChars(static_cast<jstring>(env->GetObjectArrayElement(fieldReturnTypes, i)), JNI_FALSE));
		jobject value = env->GetObjectArrayElement(fieldValues, i);
		PutField(env, target, field, returnType, value);
	}
}

JNIEXPORT jobjectArray JNICALL Java_fr_linkit_engine_internal_manipulation_creation_ObjectCreator_getAllFields
(JNIEnv* env, jclass clazz, jobject target, jobjectArray fields, jobjectArray fieldsReturnTypes) {
	int len = env->GetArrayLength(fields);
	jclass objectClass = env->FindClass("java/lang/Object");
	jobjectArray jobjectValues = env->NewObjectArray(len, objectClass, target);
	for (int i = 0; i < len; i++) {
		string returnType = env->GetStringUTFChars(static_cast<jstring>(env->GetObjectArrayElement(fieldsReturnTypes, i)), JNI_FALSE);
		jobject field = env->GetObjectArrayElement(fields, i);
		jobject value = GetField(env, target, field, returnType);
		env->SetObjectArrayElement(jobjectValues, i, value);
	}
	return jobjectValues;
}