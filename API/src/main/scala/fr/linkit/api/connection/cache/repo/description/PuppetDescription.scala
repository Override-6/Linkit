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

package fr.linkit.api.connection.cache.repo.description

import fr.linkit.api.connection.cache.repo.PuppetWrapper
import fr.linkit.api.connection.cache.repo.annotations.{FieldControl, InvocationKind, InvokeOnly, MethodControl}
import fr.linkit.api.connection.cache.repo.description.PuppetDescription._
import fr.linkit.api.local.generation.ClassDescription

import java.lang.reflect.{Field, Method}
import scala.reflect.runtime.{universe => u}

class PuppetDescription[+T] private(val tpe: u.Type, val clazz: Class[_ <: T], val loader: ClassLoader) extends ClassDescription {

    import u._

    /**
     * Methods and fields that comes from those classes will not be available for RMI Invocations.
     * */
    private val BlacklistedSuperClasses: Array[String] = Array(fullNameOf[Any], fullNameOf[Object], fullNameOf[Product])

    private val mirror = u.runtimeMirror(loader)

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
        fields.get(fieldID)
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
        def getFiltered(tpe: Type): Iterable[MethodSymbol] = {
            tpe.members
                    .filter(_.isMethod)
                    .map(_.asMethod)
                    .filter(_.owner.isClass)
                    .filterNot(f => f.isFinal || f.isStatic || f.isConstructor || f.isPrivate || f.isPrivateThis || f.privateWithin != NoSymbol)
                    .filterNot(f => f.owner.fullName.startsWith("scala.Function") || f.owner.fullName.startsWith("scala.PartialFunction"))
            //.filterNot(f => f.owner == symbolOf[Any])
        }

        val methods = getFiltered(tpe)
                .filterNot(m => BlacklistedSuperClasses.contains(m.owner.fullName))
                .concat(if (tpe.typeSymbol.isJava) Seq() else getFiltered(typeOf[Any])
                        .filter(_.name.toString != "getClass")) //don't know why, but the "getClass" method if scala.Any is not defined as final
                .filter(_.name.toString != "equals") //FIXME scalac error : "name clash between defined and inherited member"
                .map(genMethodDescription)

        /*val filteredMethods = ListBuffer.empty[MethodDescription]
        methods.foreach(desc => {
            filteredMethods.find(_.methodId == desc.methodId)
                    .fold[Unit](filteredMethods += desc) { otherDesc =>
                        if (desc.symbol.returnType.baseClasses.size > otherDesc.symbol.returnType.baseClasses.size) {
                            filteredMethods -= otherDesc
                            filteredMethods += desc
                        }
                    }
        })
        filteredMethods.toSeq*/
        methods.toSeq
    }

    private def collectFields(): Map[Int, FieldDescription] = {
        tpe.decls
                .filter(_.isMethod)
                .map(_.asMethod)
                .filter(_.isGetter)
                .filterNot(f => BlacklistedSuperClasses.contains(f.owner.fullName))
                .map(genFieldDescription)
                .map(desc => (desc.fieldID, desc))
                .toMap
    }

    def findAnnotation[A <: java.lang.annotation.Annotation](method: u.Symbol, clazz: Class[A]): Option[A] = {
        method.annotations
                .find(_.tree
                        .tpe
                        .typeSymbol
                        .asClass
                        .fullName == clazz.getName)
                .map { ann =>
                    import scala.tools.reflect.ToolBox

                    val toolBox = mirror.mkToolBox()
                    toolBox.eval(toolBox.untypecheck(ann.tree)).asInstanceOf[A]
                }
    }

    private def genMethodDescription(symbol: u.MethodSymbol): MethodDescription = {
        val javaMethod         = asJavaMethod(symbol)
        val control            = Option(javaMethod.getAnnotation(classOf[MethodControl])).getOrElse(DefaultMethodControl)
        val synchronizedParams = toSyncParamsIndexes(control.mutates(), symbol)
        val invocationKind     = control.value()
        val invokeOnly         = Option(javaMethod.getAnnotation(classOf[InvokeOnly]))
        val isPure             = control.pure() && control.mutates().nonEmpty
        val isHidden           = control.hide()
        val syncReturnValue    = control.synchronizeReturnValue()
        MethodDescription(
            symbol, javaMethod, this, invokeOnly, synchronizedParams,
            invocationKind, syncReturnValue, isPure, isHidden
        )
    }

    private def genFieldDescription(field: MethodSymbol): FieldDescription = {
        val control        = findAnnotation(field, classOf[FieldControl])
        val isSynchronized = control.exists(_.synchronize())
        val isHidden       = control.exists(_.hide())
        FieldDescription(field, this, isSynchronized, isHidden)
    }

    def asJavaMethod(method: u.MethodSymbol): Method = {
        val symbolParams = method.paramLists
                .flatten
                .map(t => {
                    val argClass = t.typeSignature.erasure.typeSymbol.asClass
                    if (argClass.fullName == "scala.Array")
                        "array"
                    else mirror
                            .runtimeClass(argClass)
                            .descriptorString()
                }
                )
        clazz
                .getMethods
                .filter(_.getName == method.name.encodedName.toString)
                .find(x => {
                    val v = x.getParameterTypes
                            .map(t => if (t.isArray) "array" else t.descriptorString())
                    v sameElements symbolParams
                })
                .get
    }

}

object PuppetDescription {

    import u._

    val JavaEqualsMethodID = 21

    def toSyncParamsIndexes(literal: String, method: u.Symbol): Seq[Boolean] = {
        val synchronizedParamNumbers = literal
                .split(",")
                .filterNot(s => s == "this" || s.isBlank)
                .map(s => s.trim
                        .dropRight(s.lastIndexWhere(!_.isDigit))
                        .toInt)
                .distinct
        for (n <- 1 to method.asMethod.paramLists.flatten.size) yield {
            synchronizedParamNumbers.contains(n)
        }
    }

    def apply[T](clazz: Class[_ <: T]): PuppetDescription[T] = {
        if (classOf[PuppetWrapper[T]].isAssignableFrom(clazz))
            throw new IllegalArgumentException("Provided class already extends from PuppetWrapper")
        val tpe = runtimeMirror(clazz.getClassLoader).classSymbol(clazz).selfType
        new PuppetDescription[T](tpe, clazz, clazz.getClassLoader)
    }

    case class MethodDescription(symbol: u.MethodSymbol,
                                 javaMethod: Method,
                                 classDesc: PuppetDescription[_],
                                 invokeOnly: Option[InvokeOnly],
                                 var synchronizedParams: Seq[Boolean], //TODO make synchronization
                                 var invocationKind: InvocationKind,
                                 var syncReturnValue: Boolean,
                                 var isPure: Boolean,
                                 var isHidden: Boolean) {

        val methodId: Int = {
            val parameters: Array[u.Type] = symbol
                    .paramLists
                    .flatten
                    .map(_.typeSignature.asSeenFrom(classDesc.tpe, symbol.owner))
                    .toArray
            symbol.name.toString.hashCode + hashCode(parameters)
        }

        def getDefaultReturnValue: String = {
            invokeOnly
                    .map(_.value())
                    .getOrElse {
                        getDefaultTypeReturnValue
                    }
        }

        private val numberTypes = Array(fullNameOf[Float], fullNameOf[Double], fullNameOf[Int], fullNameOf[Byte], fullNameOf[Long], fullNameOf[Short])

        def getDefaultTypeReturnValue: String = {
            val nme = symbol.returnType.typeSymbol.fullName

            if (nme == fullNameOf[Boolean]) "false"
            else if (numberTypes.contains(fullNameOf)) "-1"
            else if (nme == fullNameOf[Char]) "'\\u0000'"
            else "nl()" //contracted call to JavaUtils.getNull
        }

        private def hashCode(a: Array[u.Type]): Int = {
            if (a == null) return 0
            var result = 1
            for (clazz <- a) {
                result = 31 * result +
                        (if (clazz == null) 0
                        else clazz.typeSymbol.name.hashCode)
            }
            result
        }
    }

    //TODO make synchronization
    case class FieldDescription(fieldGetter: MethodSymbol,
                                classDesc: PuppetDescription[_],
                                isSynchronized: Boolean,
                                isHidden: Boolean) {

        val javaField: Field = classDesc.clazz.getDeclaredField(fieldGetter.name.toString)

        val fieldID: Int = {
            fieldGetter.hashCode() + fieldGetter.returnType.hashCode()
        }
    }

    private val DefaultMethodControl: MethodControl = {
        new MethodControl {
            override def value(): InvocationKind = InvocationKind.ONLY_LOCAL

            override def pure(): Boolean = false

            override def mutates(): String = ""

            override def synchronizeReturnValue(): Boolean = false

            override def hide(): Boolean = false

            override def annotationType(): Class[_ <: java.lang.annotation.Annotation] = getClass
        }
    }
}
