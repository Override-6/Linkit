/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class fr_linkit_engine_internal_manipulation_creation_ObjectCreator */

#ifndef _Included_fr_linkit_engine_internal_manipulation_creation_ObjectCreator
#define _Included_fr_linkit_engine_internal_manipulation_creation_ObjectCreator
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     fr_linkit_engine_internal_manipulation_creation_ObjectCreator
 * Method:    allocate
 * Signature: (Ljava/lang/Class;)Ljava/lang/Object;
 */
extern "C" JNIEXPORT jobject JNICALL Java_fr_linkit_engine_internal_manipulation_creation_ObjectCreator_allocate
(JNIEnv*, jclass, jclass);

/*
 * Class:     fr_linkit_engine_internal_manipulation_creation_ObjectCreator
 * Method:    pasteAllFields0
 * Signature: (Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_fr_linkit_engine_internal_manipulation_creation_ObjectCreator_pasteAllFields0
(JNIEnv*, jclass, jobject, jobjectArray, jobjectArray, jobjectArray);
/*
 * Class:     fr_linkit_engine_internal_manipulation_creation_ObjectCreator
 * Method:    getAllFields
 * Signature: (Ljava/lang/Object;[Ljava/lang/reflect/Field;)[Ljava/lang/Object;
 */
JNIEXPORT jobjectArray JNICALL Java_fr_linkit_engine_internal_manipulation_creation_ObjectCreator_getAllFields
  (JNIEnv *, jclass, jobject, jobjectArray, jobjectArray);


#ifdef __cplusplus
}
#endif
#endif