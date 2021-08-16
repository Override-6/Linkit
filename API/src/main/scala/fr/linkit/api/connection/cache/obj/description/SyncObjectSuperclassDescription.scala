/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.connection.cache.obj.description

import fr.linkit.api.local.generation.compilation.CompilationContext

import scala.reflect.runtime.universe.Type

/**
 * The description of the super class of a generated [[fr.linkit.api.connection.cache.obj.SynchronizedObject]] class.
 * @see [[fr.linkit.api.connection.cache.obj.SynchronizedObject]]
 * @see [[fr.linkit.api.connection.cache.obj.generation.ObjectWrapperClassCenter]]
 * @see [[fr.linkit.api.connection.cache.obj.generation.ObjectWrapperInstantiator]]
 * */
trait SyncObjectSuperclassDescription[A] extends CompilationContext {

    /**
     * The super class's type (for scala reflection api)
     * */
    val classType: Type
    /**
     * The super class's type (for java reflection api)
     * */
    val clazz: Class[A]

    /**
     * @return A list of [[MethodDescription]]
     * @see [[MethodDescription]]
     */
    def listMethods(): Iterable[MethodDescription]

    /**
     * @param methodID the method identifier to search.
     * @return Some(MethodDescription) if a method of the given identifier was found, None instead.
     * */
    def findMethodDescription(methodID: Int): Option[MethodDescription]

    /**
     * @return A list of [[FieldDescription]]
     * @see [[FieldDescription]]
     */
    def listFields(): Seq[FieldDescription]
    /**
     * @param fieldID the field identifier to search.
     * @return Some(FieldDescription) if a field of the given identifier was found, None instead.
     * */
    def getFieldDescription(fieldID: Int): Option[FieldDescription]
}
