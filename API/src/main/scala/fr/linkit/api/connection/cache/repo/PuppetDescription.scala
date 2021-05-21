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

package fr.linkit.api.connection.cache.repo

import fr.linkit.api.connection.cache.repo.PuppetDescription.{DefaultMethodControl, FieldDescription, MethodDescription}
import fr.linkit.api.connection.cache.repo.annotations.{FieldControl, InvocationKind, InvokeOnly, MethodControl}

import java.lang.annotation.Annotation
import java.lang.reflect.{Field, Method}
import scala.collection.mutable.ListBuffer

class PuppetDescription[+T] private(val clazz: Class[_ <: T]) {

    /**
     * Methods and fields that comes from those classes will not be available for RMI Invocations.
     * */
    private val BlacklistedSuperClasses = Array[Class[_]](classOf[Object], classOf[Product])

    private val methods    = collectMethods()
    private val methodsMap = methods.map(desc => (desc.methodId, desc)).toMap
    private val fields     = collectFields()

    def listMethods(): Seq[MethodDescription] = {
        methods
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
        getMethodDesc(methodId).exists(_.invocationKind != InvocationKind.ONLY_LOCAL)
    }

    def isLocalInvocationForced(methodId: Int): Boolean = {
        getMethodDesc(methodId).exists(_.invocationKind.isLocalInvocationForced)
    }

    def isInvokeOnly(methodId: Int): Boolean = {
        getMethodDesc(methodId).exists(_.invokeOnly.isDefined)
    }

    def isReturnValueSynchronized(methodID: Int): Boolean = {
        getMethodDesc(methodID).exists(_.syncReturnValue)
    }

    private def collectMethods(): Seq[MethodDescription] = {
        def collectMethods[S >: T](clazz: Class[_ <: S]): Array[MethodDescription] = {
            if (clazz == null)
                return Array()
            if (BlacklistedSuperClasses.contains(clazz))
                return collectMethods(clazz.getSuperclass)
            val declaredMethods = clazz
                    .getDeclaredMethods
                    .filterNot(m => BlacklistedSuperClasses.contains(m.getDeclaringClass))
                    .map(genMethodDescription)
            val superMethods    = collectMethods(clazz.getSuperclass)
            superMethods ++ declaredMethods
        }

        val methods         = collectMethods(clazz)
        val filteredMethods = ListBuffer.empty[MethodDescription]
        methods.foreach(desc => {
            filteredMethods.find(_.methodId == desc.methodId)
                    .fold[Unit](filteredMethods += desc) { otherDesc =>
                        if (hierarchyLevel(desc.method.getReturnType) > hierarchyLevel(otherDesc.method.getReturnType)) {
                            filteredMethods -= otherDesc
                            filteredMethods += desc
                        }
                    }
        })
        filteredMethods.toSeq
    }

    private def collectFields(): Map[Int, FieldDescription] = {
        def getFieldsOfClass[S >: T](clazz: Class[_ <: S]): Array[FieldDescription] = {
            if (clazz == null || BlacklistedSuperClasses.contains(clazz))
                return Array()
            getFieldsOfClass(clazz.getSuperclass) ++ clazz.getDeclaredFields.map(genFieldDescription)
        }

        getFieldsOfClass(clazz)
                .map(desc => (desc.fieldID, desc))
                .toMap
    }

    private def genMethodDescription(method: Method): MethodDescription = {
        val control                          = Option(method.getDeclaredAnnotation(classOf[MethodControl])).getOrElse(DefaultMethodControl)
        val synchronizedParamNumbers         = control
                .mutates()
                .split(",")
                .filterNot(s => s == "this" || s.isBlank)
                .map(s => s.trim
                        .dropRight(s.lastIndexWhere(!_.isDigit))
                        .toInt)
                .distinct
        val synchronizedParams: Seq[Boolean] = for (n <- 1 to method.getParameterCount) yield {
            synchronizedParamNumbers.contains(n)
        }
        val invocationKind                   = control.value()
        val invokeOnly                       = method.getAnnotation(classOf[InvokeOnly])
        val isPure                           = control.pure() && control.mutates().nonEmpty
        val isHidden                         = control.hide()
        val syncReturnValue                  = control.synchronizeReturnValue()
        val desc                             = MethodDescription(method, Option(invokeOnly), synchronizedParams, invocationKind, syncReturnValue, isPure, isHidden)
        desc
    }

    private def genFieldDescription(field: Field): FieldDescription = {
        val control        = Option(field.getAnnotation(classOf[FieldControl]))
        val isSynchronized = control.exists(_.synchronize())
        val isHidden       = control.exists(_.hide())
        FieldDescription(field, isSynchronized, isHidden)
    }

    private def hierarchyLevel(clazz: Class[_]): Int = {
        var superClass = clazz.getSuperclass
        var level      = 0
        while (superClass != null) {
            superClass = superClass.getSuperclass
            level += 1
        }

        def hierarchyInterfaceLevel(clazz: Class[_]): Int = {
            val interfaces = clazz.getInterfaces
            interfaces.length + interfaces.map(hierarchyLevel).sum
        }

        level + hierarchyInterfaceLevel(clazz)
    }

}

object PuppetDescription {

    def apply[T](clazz: Class[_ <: T]): PuppetDescription[T] = {
        if (classOf[PuppetWrapper[T]].isAssignableFrom(clazz))
            throw new IllegalArgumentException("Provided class can't extend PuppetWrapper")
        new PuppetDescription(clazz)
    }

    case class MethodDescription(method: Method,
                                 invokeOnly: Option[InvokeOnly],
                                 synchronizedParams: Seq[Boolean],
                                 var invocationKind: InvocationKind,
                                 var syncReturnValue: Boolean,
                                 var isPure: Boolean,
                                 var isHidden: Boolean) {

        val clazz: Class[_] = method.getDeclaringClass

        def getReplacedReturnValue: String = {
            invokeOnly.map(_.value())
                    .getOrElse {
                        val returnType = method.getReturnType
                        import java.lang
                        returnType match {
                            case lang.Boolean.TYPE                                                     => "false"
                            case lang.Float.TYPE | lang.Double.TYPE                                    => "-1.0"
                            case lang.Integer.TYPE | lang.Byte.TYPE | lang.Long.TYPE | lang.Short.TYPE => "-1"
                            case lang.Character.TYPE                                                   => "'\\u0000'"
                            case _                                                                     => "null"
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

    private val DefaultMethodControl: MethodControl = {
        new MethodControl {
            override def value(): InvocationKind = InvocationKind.LOCAL_AND_REMOTES

            override def pure(): Boolean = false

            override def mutates(): String = ""

            override def synchronizeReturnValue(): Boolean = false

            override def hide(): Boolean = false

            override def annotationType(): Class[_ <: Annotation] = getClass
        }
    }
}
