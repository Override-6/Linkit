/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.cache.sync.description

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.description.{FieldDescription, MethodDescription, SyncObjectSuperclassDescription}
import fr.linkit.engine.gnom.cache.sync.description.SimpleSyncObjectSuperClassDescription.SyntheticMod
import fr.linkit.engine.gnom.cache.sync.generation.SyncObjectClassResource.{WrapperPackage, WrapperSuffixName}

import java.lang.reflect.{Executable, Field, Method, Modifier}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class SimpleSyncObjectSuperClassDescription[A] private(override val clazz: Class[A],
                                                       val loader: ClassLoader) extends SyncObjectSuperclassDescription[A] {

    private val methodDescriptions: Map[Int, MethodDescription] = collectMethods()
    private val fieldDescriptions : Map[Int, FieldDescription]  = collectFields()

    //The generated class name
    override def classPackage: String = WrapperPackage + clazz.getPackageName

    override def className: String = clazz.getSimpleName + WrapperSuffixName

    override def parentLoader: ClassLoader = clazz.getClassLoader

    override def listMethods(): Iterable[MethodDescription] = {
        methodDescriptions.values
    }

    override def findMethodDescription(methodID: Int): Option[MethodDescription] = {
        methodDescriptions.get(methodID)
    }

    override def listFields(): Seq[FieldDescription] = {
        fieldDescriptions.values.toSeq
    }

    override def getFieldDescription(fieldID: Int): Option[FieldDescription] = {
        fieldDescriptions.get(fieldID)
    }

    private def collectMethods(): Map[Int, MethodDescription] = {
        getFiltered(clazz)
                .map(MethodDescription(_, this))
                .map(desc => (desc.methodId, desc))
                .toMap
    }

    private def getFiltered(clazz: Class[_]): Iterable[Method] = {
        val filtered = clazz.getMethods
        filtered.filterNot(isNotOverridable)
                .filterNot(m => m.getName == "equals" && m.getParameterTypes.length == 1)//FIXME Weird bug due to scala's Any and AnyRef stuff...
    }

    private def isNotOverridable(e: Executable): Boolean = {
        val mods = e.getModifiers
        import Modifier._
        isStatic(mods) || isFinal(mods) || isPrivate(mods) || isNative(mods) || (mods & SyntheticMod) != 0
    }

    /*def asJavaMethod(method: u.MethodSymbol): (Method, Int) = {
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
    }*/

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

object SimpleSyncObjectSuperClassDescription {

    private val SyntheticMod = 0x00001000

    private val cache = mutable.HashMap.empty[Class[_], SimpleSyncObjectSuperClassDescription[_]]

    def apply[A](clazz: Class[_]): SimpleSyncObjectSuperClassDescription[A] = cache.getOrElse(clazz, {
        if (classOf[SynchronizedObject[_]].isAssignableFrom(clazz))
            throw new IllegalArgumentException("Provided class already extends from SynchronizedObject")
        val AClass    = clazz.asInstanceOf[Class[A]]
        new SimpleSyncObjectSuperClassDescription[A](AClass, clazz.getClassLoader)
    }).asInstanceOf[SimpleSyncObjectSuperClassDescription[A]]

}
