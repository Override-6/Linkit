/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.instantiation.SyncObjectInstantiator
import fr.linkit.api.internal.compilation.CompilationContext

/**
 * The description of the super class of a generated [[fr.linkit.api.gnom.cache.sync.SynchronizedObject]] class.
 *
 * @see [[fr.linkit.api.gnom.cache.sync.SynchronizedObject]]
 * @see [[fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter]]
 * @see [[SyncObjectInstantiator]]
 * */
trait SyncStructureDescription[A <: AnyRef] extends CompilationContext {

    val structureHash: Int

    /**
     * The super class's type (for java reflection api)
     * */
    val specs: SyncClassDef

    /**
     * @return A list of [[MethodDescription]]
     * @see [[MethodDescription]]
     */
    def listMethods(): Iterable[MethodDescription]
    

    /**
     * @return A list of [[FieldDescription]]
     * @see [[FieldDescription]]
     */
    def listFields(): Iterable[FieldDescription]
    /**
     * @param methodID the method identifier to search.
     * @return Some(MethodDescription) if a method of the given identifier was found, None instead.
     * */
    def findMethodDescription(methodID: Int): Option[MethodDescription]

    def findMethodDescription(methodName: String, params: Seq[Class[_]]): Option[MethodDescription]


    /**
     * @param fieldID the field identifier to search.
     * @return Some(FieldDescription) if a field of the given identifier was found, None instead.
     * */
    def findFieldDescription(fieldID: Int): Option[FieldDescription]

    def findFieldDescription(fieldName: String): Option[FieldDescription]
}
