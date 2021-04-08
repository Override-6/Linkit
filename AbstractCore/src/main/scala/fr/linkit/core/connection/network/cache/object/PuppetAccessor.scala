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

package fr.linkit.core.connection.network.cache.`object`

import java.lang.reflect.{Field, Method}

class PuppetAccessor private(sharedFields: Map[String, Field],
                             sharedMethods: Map[String, Method]) {

    def getSharedField(name: String): Option[Field] = Option(sharedFields(name))

    def getSharedMethod(name: String): Option[Method] = Option(sharedMethods(name))

    def foreachSharedFields(action: Field => Unit): Unit = sharedFields.values.foreach(action)

}

object PuppetAccessor {

    def ofRef(anyRef: AnyRef): PuppetAccessor = {
        val clazz        = anyRef.getClass

        val sharedFields = clazz.getDeclaredFields
                .filter(_.isAnnotationPresent(classOf[Shared]))
                .tapEach(_.setAccessible(true))
                .map(field => (field.getName, field))
                .toMap

        val sharedMethods = clazz.getDeclaredMethods
                .filter(_.isAnnotationPresent(classOf[Shared]))
                .tapEach(_.setAccessible(true))
                .map(method => (method.getName, method))
                .toMap

        new PuppetAccessor(sharedFields, sharedMethods)
    }
}
