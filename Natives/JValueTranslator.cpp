#include <jni.h>
#include "JValueTranslator.h"
#include <string.h>
#include <vector>
#include <string>
#include <algorithm>
using namespace std;

jvalue NumberToJValue(JNIEnv* env, JValueType type, double n) {
	jvalue val;
	switch (type) {
	case JValueType::BYTE_FLAG:
		val.b = n;
		break;
	case JValueType::BOOLEAN_FLAG:
		val.z = n;
		break;
	case JValueType::CHAR_FLAG:
		val.c = n;
		break;
	case JValueType::SHORT_FLAG:
		val.s = n;
		break;
	case JValueType::INT_FLAG:
		val.i = n;
		break;
	case JValueType::LONG_FLAG:
		val.j = n;
		break;
	case JValueType::FLOAT_FLAG:
		val.f = n;
		break;
	case JValueType::DOUBLE_FLAG:
		val.d = n;
		break;
	}
	return val;
}

JValueType ClassNameToType(std::string name) {
	if (name == "java.lang.Boolean" || name == "boolean" || name == "Z") {
		return JValueType::BOOLEAN_FLAG;
	}
	else if (name == "java.lang.Byte" || name == "byte" || name == "B") {
		return JValueType::BYTE_FLAG;
	}
	else if (name == "java.lang.Character" || name == "char" || name == "C") {
		return JValueType::CHAR_FLAG;
	}
	else if (name == "java.lang.Short" || name == "short" || name == "S") {
		return JValueType::SHORT_FLAG;
	}
	else if (name == "java.lang.Integer" || name == "int" || name == "I") {
		return JValueType::INT_FLAG;
	}
	else if (name == "java.lang.Long" || name == "long" || name == "J") {
		return JValueType::LONG_FLAG;
	}
	else if (name == "java.lang.Float" || name == "float" || name == "F") {
		return JValueType::FLOAT_FLAG;
	}
	else if (name == "java.lang.Double" || name == "double" || name == "D") {
		return JValueType::DOUBLE_FLAG;
	}
	else if (name == "void" || name == "V") {
		return JValueType::VOID_FLAG;
	}
	else {
		return JValueType::OBJECT_FLAG;
	}
}

jobject WrapPrimitive(JNIEnv* env, string className, string paramSignature, double value) {
	const jvalue* val = &NumberToJValue(env, ClassNameToType(paramSignature), value);
	replace(className.begin(), className.end(), '.', '/');
	jclass clazz = env->FindClass(className.data());
	string signature = "(" + paramSignature + ")L" + className + ";";
	jmethodID methodID = env->GetStaticMethodID(clazz, "valueOf", signature.data());
	return env->CallStaticObjectMethodA(clazz, methodID, val);
}

double UnwrapPrimitive(JNIEnv* env, JValueType objectType, jobject object) {
	auto clazz = env->GetObjectClass(object);
	jfieldID valueID;
	switch (objectType) {
	case JValueType::BOOLEAN_FLAG:
		valueID = env->GetFieldID(clazz, "value", "Z");
		return env->GetBooleanField(object, valueID);
	case JValueType::BYTE_FLAG:
		valueID = env->GetFieldID(clazz, "value", "B");
		return env->GetByteField(object, valueID);
	case JValueType::CHAR_FLAG:
		valueID = env->GetFieldID(clazz, "value", "C");
		return env->GetCharField(object, valueID);
	case JValueType::SHORT_FLAG:
		valueID = env->GetFieldID(clazz, "value", "S");
		return env->GetShortField(object, valueID);
	case JValueType::INT_FLAG: {
		valueID = env->GetFieldID(clazz, "value", "I");
		const int i = env->GetIntField(object, valueID);
		return i;
	}
	case JValueType::LONG_FLAG:
		valueID = env->GetFieldID(clazz, "value", "J");
		return env->GetLongField(object, valueID);
	case JValueType::FLOAT_FLAG:
		valueID = env->GetFieldID(clazz, "value", "F");
		return env->GetFloatField(object, valueID);
	case JValueType::DOUBLE_FLAG:
		valueID = env->GetFieldID(clazz, "value", "D");
		return env->GetDoubleField(object, valueID);
	default: return NULL;
	}
}

const char* GetJClassName(JNIEnv* env, jclass clazz) {
	jclass classClass = env->FindClass("java/lang/Class");
	jmethodID mid_getName = env->GetMethodID(classClass, "getName", "()Ljava/lang/String;");
	jstring str = static_cast<jstring> (env->CallObjectMethod(clazz, mid_getName));
	return env->GetStringUTFChars(str, false);
}


jvalue JObjectToJValue(JNIEnv* env, JValueType fieldType, jobject object) {
	auto clazz = env->GetObjectClass(object);
	const char* name = GetJClassName(env, clazz);
	JValueType objectType = ClassNameToType(name);
	if (objectType == JValueType::OBJECT_FLAG) {
		jvalue val;
		val.l = object;
		return val;
	}
	double number = UnwrapPrimitive(env, objectType, object);
	return NumberToJValue(env, fieldType, number);
}


