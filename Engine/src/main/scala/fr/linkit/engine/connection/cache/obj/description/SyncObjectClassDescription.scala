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

package fr.linkit.engine.connection.cache.obj.description

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.description.{FieldDescription, MethodDescription, fullNameOf}
import fr.linkit.api.local.generation.PuppetClassDescription
import fr.linkit.engine.connection.cache.obj.description.SyncObjectClassDescription.PrimitivesNameMap
import fr.linkit.engine.connection.cache.obj.generation.SyncObjectClassResource.{WrapperPackage, WrapperSuffixName}

import java.lang.reflect.{Field, Method, Modifier}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.{universe => u}

class SyncObjectClassDescription[A] private(override val classType: u.Type,
                                            override val clazz: Class[A], val loader: ClassLoader) extends PuppetClassDescription[A] {

    import u._

    /**
     * Methods and fields that comes from those classes will not be available for RMI Invocations.
     * */
    private val BlacklistedSuperClasses: Array[String] = Array(fullNameOf[Any], fullNameOf[Object], fullNameOf[Product])

    //cache for optimisation in collectMethods()
    private val filteredMethodsOfClassAny = getFiltered(typeOf[Any]).filter(_.name.toString != "getClass")
    private val filteredJavaMethods       = {
        clazz.getMethods
                .filterNot(m => Modifier.isFinal(m.getModifiers) || Modifier.isStatic(m.getModifiers))
                .filterNot(f => f.getDeclaringClass.getName.startsWith("scala.Function") || f.getDeclaringClass.getName.startsWith("scala.PartialFunction"))
    }

    private val methodDescriptions = collectMethods()
    private val fieldDescriptions  = collectFields()

    //The generated class name
    override def classPackage: String = WrapperPackage + clazz.getPackageName

    override def className: String = clazz.getSimpleName + WrapperSuffixName

    override def parentLoader: ClassLoader = clazz.getClassLoader

    override def listMethods(): Iterable[MethodDescription] = {
        methodDescriptions.values
    }

    override def getMethodDescription(methodID: Int): Option[MethodDescription] = {
        methodDescriptions.get(methodID)
    }

    override def listFields(): Seq[FieldDescription] = {
        fieldDescriptions.values.toSeq
    }

    override def getFieldDescription(fieldID: Int): Option[FieldDescription] = {
        fieldDescriptions.get(fieldID)
    }

    private def collectMethods(): Map[Int, MethodDescription] = {
        getFiltered(classType)
                .filterNot(m => BlacklistedSuperClasses.contains(m.owner.fullName))
                .concat(if (classType.typeSymbol.isJava) Seq() else filteredMethodsOfClassAny) //don't know why, but the "getClass" method if scala.Any is not defined as final
                .filter(_.name.toString != "equals") //FIXME scalac error : "name clash between defined and inherited member"
                .map(genMethodDescription)
                .map(desc => (desc.methodId, desc))
                .toMap
    }

    private def getFiltered(tpe: Type): Iterable[MethodSymbol] = {
        val filtered = tpe.members
                .filter(_.isMethod)
                .map(_.asMethod)
        filtered.filterNot(f => f.isFinal || f.isStatic || f.isConstructor || f.isPrivate || f.isPrivateThis || f.privateWithin != NoSymbol)
                .filterNot(f => f.owner.fullName.startsWith("scala.Function") || f.owner.fullName.startsWith("scala.PartialFunction"))
    }

    private def genMethodDescription(symbol: u.MethodSymbol): MethodDescription = {
        val (javaMethod, ordinal) = asJavaMethod(symbol)
        MethodDescription(symbol, javaMethod, this, ordinal)
    }

    def asJavaMethod(method: u.MethodSymbol): (Method, Int) = {
        val symbolParams = method.paramLists
                .flatten
                .map(t => {
                    val argClass = t.typeSignature.erasure.typeSymbol
                    val name     = argClass.fullName
                    PrimitivesNameMap.getOrElse(name, name)
                }
                )
        var ordinal      = 0
        val filtered     = filteredJavaMethods.filter(_.getName == method.name.encodedName.toString)
        val javaMethod   = filtered
                .find(x => {
                    val v = x.getParameterTypes
                            .map(t => if (t.isArray) "array" else t.getTypeName)
                    ordinal += 1
                    v sameElements symbolParams
                })
                .get
        (javaMethod, ordinal)
    }

    private def collectFields(): Map[Int, FieldDescription] = {
        val fields          = ListBuffer.empty[Field]
        var clazz: Class[_] = this.clazz
        while (clazz != null) {
            fields ++= clazz.getDeclaredFields
                    .tapEach(_.setAccessible(true))
                    .filterNot(f => Modifier.isStatic(f.getModifiers))
            clazz = clazz.getSuperclass
        }
        fields.map(FieldDescription(_, this))
                .map(desc => (desc.fieldId, desc))
                .toMap
    }

}

object SyncObjectClassDescription {

    import u._

    private val SyntheticMod = 0x00001000

    private val PrimitivesNameMap = Map(
        "scala.Int" -> "int", "scala.Char" -> "char", "scala.Long" -> "long",
        "scala.Boolean" -> "boolean", "scala.Float" -> "float", "scala.Double" -> "double",
        "scala.Byte" -> "byte", "scala.Short" -> "short", "scala.Array" -> "array")

    private val cache = mutable.HashMap.empty[Class[_], SyncObjectClassDescription[_]]

    def apply[A](clazz: Class[A]): SyncObjectClassDescription[A] = cache.getOrElse(clazz, {
        if (classOf[SynchronizedObject[_]].isAssignableFrom(clazz))
            throw new IllegalArgumentException("Provided class already extends from SynchronizedObject")

        val tpe = runtimeMirror(clazz.getClassLoader).classSymbol(clazz).selfType
        new SyncObjectClassDescription(tpe, clazz, clazz.getClassLoader)
    }).asInstanceOf[SyncObjectClassDescription[A]]

}
