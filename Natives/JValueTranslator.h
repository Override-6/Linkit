
#include <string>
using namespace std;
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

jvalue NumberToJValue(JValueType type, double n);

jvalue JObjectToJValue(JNIEnv*, JValueType, jobject);

JValueType ClassNameToType(std::string);

double UnwrapPrimitive(JNIEnv*, JValueType, jobject);

jobject WrapPrimitive(JNIEnv*, string, string, double);