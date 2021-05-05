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

package fr.linkit.api.connection.network.cache.repo

import fr.linkit.api.connection.network.cache.repo.PuppetBlueprint.MethodDescription
import org.jetbrains.annotations.Contract

import java.lang.reflect.Method
import java.util
import scala.collection.mutable

class PuppetBlueprint[T] private(clazz: Class[T]) {

    private val methods = genMethodsMap()

    def foreachMethods(action: MethodDescription => Unit): Unit = {
        methods.values.foreach(action)
    }

    def isRMIEnabled(methodId: Int): Boolean = {
        methods.get(methodId).forall(!_.isLocalOnly)
    }

    private def genMethodsMap(): Map[Int, MethodDescription] = {
        def getMethodsOfClass(clazz: Class[_ >: T]): Array[MethodDescription] = {
            if (clazz == null)
                return Array()
            clazz.getDeclaredMethods.map(genDescription) ++ getMethodsOfClass(clazz.getSuperclass)
        }
        getMethodsOfClass(clazz)
                .map(desc => (desc.hashCode(), desc))
                .toMap
    }

    private def genDescription(method: Method): MethodDescription = {
        val isLocalOnly = if (method.isAnnotationPresent(classOf[Contract])) {
            method.getAnnotation(classOf[Contract]).pure()
        } else method.isAnnotationPresent(classOf[LocalOnly])
        val isHidden    = method.isAnnotationPresent(classOf[Hidden])
        val invokeOnly  = method.getAnnotation(classOf[InvokeOnly])
        MethodDescription(method, isLocalOnly, isHidden, invokeOnly)
    }

}

object PuppetBlueprint {

    case class MethodDescription(method: Method, isLocalOnly: Boolean, isHidden: Boolean, private val invokeOnly: InvokeOnly) {

        def getReplacedReturnValue: Option[String] = Option(invokeOnly).map(_.value())

        override def hashCode(): Int = {
            method.hashCode() + util.Arrays.hashCode(method.getParameterTypes)
        }
    }

}
