#include <jni.h>
#include "JValueTranslator.h"
#include <string.h>
#include <vector>
#include <string>
#include <algorithm>
using namespace std;

jvalue NumberToJValue(JValueType type, double n) {
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

constexpr unsigned int str2int(const char* str, int h = 0) {
    return !str[h] ? 5381 : (str2int(str, h+1) * 33) ^ str[h];
}
JValueType ClassNameToType(const std::string& name) {
    switch(str2int(name.c_str())) {
        case str2int("java.lang.Boolean"):
        case str2int("boolean"):
        case str2int("Z"):
            return JValueType::BOOLEAN_FLAG;
        case str2int("java.lang.Byte"):
        case str2int("byte"):
        case str2int("B"):
            return JValueType::BYTE_FLAG;
        case str2int("java.lang.Character"):
        case str2int("char"):
        case str2int("C"):
            return JValueType::CHAR_FLAG;
        case str2int("java.lang.Short"):
        case str2int("short"):
        case str2int("S"):
            return JValueType::SHORT_FLAG;
        case str2int("java.lang.Integer"):
        case str2int("int"):
        case str2int("I"):
            return JValueType::INT_FLAG;
        case str2int("java.lang.Long"):
        case str2int("long"):
        case str2int("j"):
            return JValueType::LONG_FLAG;
        case str2int("java.lang.Float"):
        case str2int("float"):
        case str2int("F"):
            return JValueType::FLOAT_FLAG;
        case str2int("java.lang.Double"):
        case str2int("double"):
        case str2int("D"):
            return JValueType::DOUBLE_FLAG;
        case str2int("void"):
        case str2int("V"):
            return JValueType::VOID_FLAG;
        default:
            return JValueType::OBJECT_FLAG;
    }
}



jobject WrapPrimitive(JNIEnv* env, string className, const string& paramSignature, double value) {
	const jvalue val0 = NumberToJValue(ClassNameToType(paramSignature), value);
    const jvalue* val = &val0;
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
	default: return 0.0;
	}
}



const char* GetJClassName(JNIEnv* env, jclass clazz) {
	jclass classClass = env->FindClass("java/lang/Class");
	jmethodID mid_getName = env->GetMethodID(classClass, "getName", "()Ljava/lang/String;");
	auto str = static_cast<jstring>(env->CallObjectMethod(clazz, mid_getName));
	return env->GetStringUTFChars(str, JNI_FALSE);
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
	return NumberToJValue(fieldType, number);
}


