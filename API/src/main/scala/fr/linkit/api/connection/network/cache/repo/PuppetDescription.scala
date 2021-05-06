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

import fr.linkit.api.connection.network.cache.repo.PuppetDescription.{FieldDescription, MethodDescription}
import fr.linkit.api.connection.network.cache.repo.annotations.{FieldControl, InvokeOnly, MethodControl}

import java.lang.reflect.{Field, Method}

class PuppetDescription[T <: Serializable](val clazz: Class[_ <: T]) {

    private val methods      = genMethodsMap()
    private val methodsMap = methods.map(desc => (desc.methodId, desc)).toMap
    private val fields       = genMethodsField()

    private var currentMethodIndex = -1

    def foreachMethods(action: MethodDescription => Unit): Unit = {
        methods.foreach(action)
    }

    def getMethodDesc(methodID: Int): Option[MethodDescription] = methodsMap.get(methodID)

    def foreachFields(action: FieldDescription => Unit): Unit = {
        fields.values.foreach(action)
    }

    def getFieldDesc(fieldID: Int): Option[FieldDescription] = fields.get(fieldID)

    def nextMethod: MethodDescription = {
        currentMethodIndex += 1
        methods(currentMethodIndex)
    }

    def resetMethodIteration: Unit = currentMethodIndex = 0

    def isRMIEnabled(methodId: Int): Boolean = {
        methodsMap.get(methodId).forall(!_.isLocalOnly)
    }

    private def genMethodsMap(): Array[MethodDescription] = {
        def getMethodsOfClass[S >: T](clazz: Class[_ <: S]): Array[MethodDescription] = {
            if (clazz == null)
                return Array()
            clazz.getDeclaredMethods.map(genMethodDescription) ++ getMethodsOfClass(clazz.getSuperclass)
        }

        getMethodsOfClass(clazz)
    }

    private def genMethodsField(): Map[Int, FieldDescription] = {
        def getFieldsOfClass[S >: T](clazz: Class[_ <: S]): Array[FieldDescription] = {
            if (clazz == null)
                return Array()
            clazz.getDeclaredFields.map(genFieldDescription) ++ getFieldsOfClass(clazz.getSuperclass)
        }

        getFieldsOfClass(clazz)
                .map(desc => (desc.fieldID, desc))
                .toMap
    }

    private def genMethodDescription(method: Method): MethodDescription = {
        val control                          = Option(method.getAnnotation(classOf[MethodControl]))
        val synchronizedParamNumbers         = control
                .map(_.mutates()
                        .split(",")
                        .filterNot(s => s == "this" || s.isBlank)
                        .map(s => s.trim
                                .dropRight(s.lastIndexWhere(!_.isDigit))
                                .toInt)
                        .distinct)
        val synchronizedParams: Seq[Boolean] = for (n <- 1 to method.getParameterCount) yield {
            synchronizedParamNumbers.exists(_.contains(n))
        }
        val invokeOnly                       = method.getAnnotation(classOf[InvokeOnly])
        val isPure                           = control.exists(c => c.pure() && c.mutates().nonEmpty)
        val isHidden                         = control.exists(_.hide())
        val syncReturnValue                  = control.exists(_.synchronizeReturnValue())
        val isLocalOnly                      = control.exists(_.localOnly())
        MethodDescription(method, Option(invokeOnly), synchronizedParams, syncReturnValue, isLocalOnly, isPure, isHidden)
    }

    private def genFieldDescription(field: Field): FieldDescription = {
        val control        = Option(field.getAnnotation(classOf[FieldControl]))
        val isSynchronized = control.exists(_.synchronize())
        val isHidden       = control.exists(_.hide())
        FieldDescription(field, isSynchronized, isHidden)
    }

}

object PuppetDescription {

    case class MethodDescription(method: Method,
                                 invokeOnly: Option[InvokeOnly],
                                 synchronizedParams: Seq[Boolean],
                                 var syncReturnValue: Boolean,
                                 var isLocalOnly: Boolean,
                                 var isPure: Boolean,
                                 var isHidden: Boolean) {

        def getReplacedReturnValue: Option[String] = invokeOnly.map(_.value())

        val methodId: Int = {
            val any: Array[Class[_]] = method.getParameterTypes
            method.hashCode() + hashCode(any)
        }

        private def hashCode(a: Array[Class[_]]): Int = {
            if (a == null) return 0
            var result = 1
            for (element <- a) {
                result = 31 * result + (if (element == null) 0
                else element.hashCode)
            }
            result
        }
    }

    case class FieldDescription(field: Field,
                                isSynchronized: Boolean,
                                isHidden: Boolean) {

        val fieldID: Int = {
            field.hashCode() + field.getType.hashCode()
        }
    }

}
