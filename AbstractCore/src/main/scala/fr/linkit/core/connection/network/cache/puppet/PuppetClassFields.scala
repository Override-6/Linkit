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

package fr.linkit.core.connection.network.cache.puppet

import fr.linkit.core.connection.network.cache.puppet.generation.InvalidPuppetDefException

import java.lang.reflect.{Constructor, Field, Method}

class PuppetClassFields private(sharedFields: Map[String, Field],
                                sharedMethods: Map[String, Method],
                                val chippedClass: Class[_],
                                val puppetConstructor: Constructor[_],
                                autoFlush: Boolean) {

    def isAutoFlush: Boolean = autoFlush

    def getSharedField(name: String): Option[Field] = {
        sharedFields.get(name)
    }

    def getSharedMethod(name: String): Option[Method] = sharedMethods.get(name)

    def foreachSharedFields(action: Field => Unit): Unit = {
        sharedFields.values.foreach(action)
    }

    def foreachSharedMethods(action: Method => Unit): Unit = {
        sharedMethods.values.foreach(action)
    }

}

object PuppetClassFields {

    def ofRef(anyRef: Serializable): PuppetClassFields = {
        ofClass(anyRef.getClass)
    }

    def ofClass[S <: Serializable](clazz: Class[S]): PuppetClassFields = {
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

        val isAutoFlush = clazz
                .getAnnotation(classOf[SharedObject])
                .autoFlush()

        val simpleName        = clazz.getSimpleName
        val puppetConstructor = clazz.getDeclaredConstructors
                .find(_.getParameterTypes sameElements Array(clazz))
                .getOrElse(throw new InvalidPuppetDefException(
            s"""For puppet class $clazz
               |This puppet must contain an accessible constructor 'x $simpleName($simpleName other)' in order to be extended by a generated class.
               | If you are not the maintainer of this class, you can simply extend the class, define the appointed constructor and give the implementation
               | to the puppet generator.
               |""".stripMargin))

        new PuppetClassFields(sharedFields, sharedMethods, clazz, puppetConstructor, isAutoFlush)
    }
}
