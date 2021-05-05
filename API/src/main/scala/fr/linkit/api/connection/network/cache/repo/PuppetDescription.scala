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

import fr.linkit.api.connection.network.cache.repo.PuppetDescription.MethodDescription
import fr.linkit.api.connection.network.cache.repo.annotations.{Hidden, InvokeOnly, LocalOnly}
import org.jetbrains.annotations.Contract

import java.lang.reflect.Method
import java.util

class PuppetDescription[T <: Serializable] private(clazz: Class[T]) {

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
        val contract                         = Option(method.getAnnotation(classOf[Contract]))
        val synchronizedParamNumbers         = contract
                .map(_.mutates()
                        .split(",")
                        .filterNot(_ == "this")
                        .map(_.trim
                                .last
                                .toInt)
                        .distinct)
        val synchronizedParams: Seq[Boolean] = for (n <- 1 to method.getParameterCount) yield {
            synchronizedParamNumbers.exists(_.contains(n))
        }
        val isLocalOnly                      = if (method.isAnnotationPresent(classOf[Contract])) {
            method.getAnnotation(classOf[Contract]).pure()
        } else method.isAnnotationPresent(classOf[LocalOnly])
        val isHidden                         = method.isAnnotationPresent(classOf[Hidden])
        val invokeOnly                       = method.getAnnotation(classOf[InvokeOnly])

        MethodDescription(method, synchronizedParams, )
    }

}

object PuppetDescription {

    case class MethodDescription(method: Method,
                                 synchronizedParams: Seq[Boolean],
                                 var syncReturnType: Boolean,
                                 var isLocalOnly: Boolean,
                                 var isHidden: Boolean,
                                 var invokeOnly: InvokeOnly) {

        def getReplacedReturnValue: Option[String] = Option(invokeOnly).map(_.value())

        override def hashCode(): Int = {
            method.hashCode() + util.Arrays.hashCode(method.getParameterTypes)
        }
    }

}
