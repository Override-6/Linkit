
#include <string>

enum class JValueType {
	VOID_FLAG = -2,
	OBJECT_FLAG,
	BYTE_FLAG,
	BOOLEAN_FLAG,
	CHAR_FLAG,
	SHORT_FLAG,
	INT_FLAG,
	LONG_FLAG,
	FLOAT_FLAG,
	DOUBLE_FLAG
};

jvalue NumberToJValue(JNIEnv*, JValueType, double);

jvalue JObjectToJValue(JNIEnv*, int, jobject);

JValueType ClassNameToType(std::string);

double ExtractNumber(JNIEnv*, JValueType, jobject);
