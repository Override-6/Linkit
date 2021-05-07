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

    /**
     * Methods and fields that comes from those classes will not be available for RMI Invocations.
     * */
    private val blacklistedSuperClasses = Array[Class[_]](classOf[Object], classOf[Product])

    private val methods    = collectMethods()
    private val methodsMap = methods.map(desc => (desc.methodId, desc)).toMap
    private val fields     = genMethodsField()

    def listMethods(): Seq[MethodDescription] = {
        methods.toSeq
    }

    def getMethodDesc(methodID: Int): Option[MethodDescription] = {
        methodsMap.get(methodID)
    }

    def listFields(): Seq[FieldDescription] = {
        fields.values.toSeq
    }

    def getFieldDesc(fieldID: Int): Option[FieldDescription] = {
        fields
                .get(fieldID)
    }

    def isRMIEnabled(methodId: Int): Boolean = {
        getMethodDesc(methodId).forall(!_.isLocalOnly)
    }

    def isInvokeOnly(methodId: Int): Boolean = {
        getMethodDesc(methodId).forall(_.invokeOnly.isDefined)
    }

    private def collectMethods(): Array[MethodDescription] = {
        def collectMethods[S >: T](clazz: Class[_ <: S]): Array[MethodDescription] = {
            if (clazz == null)
                return Array()
            if (blacklistedSuperClasses.contains(clazz))
                return collectMethods(clazz.getSuperclass)
            clazz.getDeclaredMethods.map(genMethodDescription) ++ collectMethods(clazz.getSuperclass)
        }

        collectMethods(clazz)
    }

    private def genMethodsField(): Map[Int, FieldDescription] = {
        def getFieldsOfClass[S >: T](clazz: Class[_ <: S]): Array[FieldDescription] = {
            if (clazz == null || blacklistedSuperClasses.contains(clazz))
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

        def getReplacedReturnValue: String = {
            import java.lang
            invokeOnly.map(_.value())
                    .getOrElse {
                        val returnType = method.getReturnType
                        returnType match {
                            case lang.Boolean.TYPE                                                     => "false"
                            case lang.Float.TYPE | lang.Double.TYPE                                    => "-1.0"
                            case lang.Integer.TYPE | lang.Byte.TYPE | lang.Long.TYPE | lang.Short.TYPE => "-1"
                            case lang.Character.TYPE                                                   => "'\\u0000'"
                            case _ => "null"
                        }
                    }
        }

        val methodId: Int = {
            val parameters: Array[Class[_]] = method.getParameterTypes
            method.getName.hashCode + hashCode(parameters)
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
