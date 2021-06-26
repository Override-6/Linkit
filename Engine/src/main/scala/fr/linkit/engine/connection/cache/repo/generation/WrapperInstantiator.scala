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

package fr.linkit.engine.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.PuppetWrapper
import fr.linkit.engine.local.utils.ScalaUtils
import fr.linkit.engine.local.utils.ScalaUtils.{allocate, pasteAllFields}

object WrapperInstantiator {

    private val TheUnsafe = ScalaUtils.findUnsafe()

    def instantiate[T <: PuppetWrapper[T]](clazz: Class[T]): T = {
        allocate[T](clazz)
    }

    def instantiateFromOrigin[A](wrapperClass: Class[A with PuppetWrapper[A]], origin: A): A with PuppetWrapper[A] = {
        val instance  = allocate[A with PuppetWrapper[A]](wrapperClass)
        pasteAllFields(instance, origin)
        instance
    }

    def detachedClone[A](origin: A): A = {
        val instance = allocate[A](origin.getClass.getSuperclass)
        pasteAllFields(instance, origin)
        instance
    }

}
