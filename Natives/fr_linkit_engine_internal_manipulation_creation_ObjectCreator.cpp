#include <jni.h>
#include <string>
#include <algorithm>
#include <fstream>
#include "JValueTranslator.h"
#include "fr_linkit_engine_internal_manipulation_creation_ObjectCreator.h"

using namespace std;

std::ofstream debugfile("C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home\\NativesCommunication-ObjectCreator.txt");


void PutField(JNIEnv* env, jobject target, const char* fieldname, string signature, jobject data) {
	jclass clazz = env->GetObjectClass(target);

	jfieldID fieldID = env->GetFieldID(clazz, fieldname, signature.data());
	replace(signature.begin(), signature.end(), '/', '.');
	JValueType tpe = ClassNameToType(signature);
	if (tpe == JValueType::OBJECT_FLAG) {
		env->SetObjectField(target, fieldID, data);
		return;
	}
	//setting primitive
	double primitive = ExtractNumber(env, tpe, data);
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
(JNIEnv* env, jclass clazz, jobject target, jobjectArray fieldNames, jobjectArray fieldSignatures, jobjectArray fieldValues) {
	int len = env->GetArrayLength(fieldSignatures);

	for (int i = 0; i < len; i++) {
		const char* name = env->GetStringUTFChars(static_cast<jstring>(env->GetObjectArrayElement(fieldNames, i)), false);
		string signature(env->GetStringUTFChars(static_cast<jstring>(env->GetObjectArrayElement(fieldSignatures, i)), false));
		jobject value = env->GetObjectArrayElement(fieldValues, i);
		PutField(env, target, name, signature, value);
	}
}

