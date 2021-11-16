/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.description

import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceInstantiator
import fr.linkit.api.internal.generation.compilation.CompilationContext

/**
 * The description of the super class of a generated [[fr.linkit.api.gnom.cache.sync.SynchronizedObject]] class.
 * @see [[fr.linkit.api.gnom.cache.sync.SynchronizedObject]]
 * @see [[fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter]]
 * @see [[SyncInstanceInstantiator]]
 * */
trait SyncStructureDescription[A] extends CompilationContext {

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

    def findMethodDescription(methodName: String): Option[MethodDescription]

    /**
     * @return A list of [[FieldDescription]]
     * @see [[FieldDescription]]
     */
    def listFields(): Seq[FieldDescription]

    /**
     * @param fieldID the field identifier to search.
     * @return Some(FieldDescription) if a field of the given identifier was found, None instead.
     * */
    def findFieldDescription(fieldID: Int): Option[FieldDescription]

    def findFieldDescription(fieldName: String): Option[FieldDescription]
}
