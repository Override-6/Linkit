/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.description

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.description.{FieldDescription, MethodDescription, SyncObjectSuperclassDescription}
import fr.linkit.engine.gnom.cache.sync.description.SimpleSyncObjectSuperClassDescription.SyntheticMod
import fr.linkit.engine.gnom.cache.sync.generation.SyncObjectClassResource.{WrapperPackage, WrapperSuffixName}
import java.lang.reflect._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}

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

    override def findMethodDescription(methodName: String): Option[MethodDescription] = {
        methodDescriptions.values.find(_.javaMethod.getName == methodName)
    }

    override def findFieldDescription(fieldName: String): Option[FieldDescription] = {
        fieldDescriptions.values.find(_.javaField.getName == fieldName)
    }

    override def listFields(): Seq[FieldDescription] = {
        fieldDescriptions.values.toSeq
    }

    override def findFieldDescription(fieldID: Int): Option[FieldDescription] = {
        fieldDescriptions.get(fieldID)
    }

    private def collectMethods(): Map[Int, MethodDescription] = {
        getFiltered(clazz)
                .map(MethodDescription(_, this))
                .map(desc => (desc.methodId, desc))
                .toMap
    }

    private def getFiltered(clazz: Class[_]): Iterable[Method] = {
        clazz.getMethods
                .distinctBy(m => (m.getName, m.getParameterTypes))
                .filterNot(isNotOverridable)

                //FIXME Weird bug due to scala's Any and AnyRef stuff...
                .filterNot(m => m.getName == "equals" && m.getParameterTypes.length == 1)

                //FIXME Bug occurred for objects that extends NetworkObject[A].
                // as SynchronizedObject trait also extends NetworkObject[B],
                // a collision may occur as the generated method would be
                // syncClass#reference: A, which overrides SynchronizedObject#reference: B (there is an incompatible type definition)
                // Maybe making the GNOLinkage able to support multiple references to an object would help
                .filterNot(m => m.getName == "reference" && m.getParameterTypes.isEmpty)
    }

    private def isNotOverridable(e: Executable): Boolean = {
        val mods = e.getModifiers
        import Modifier._
        isPrivate(mods) || isStatic(mods) || isFinal(mods) || isNative(mods) || (mods & SyntheticMod) != 0
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
                    .filterNot(f => Modifier.isStatic(f.getModifiers))
                    .filter(setAccessible)
            clazz = clazz.getSuperclass
        }
        fields.map(FieldDescription(_, this))
                .map(desc => (desc.fieldId, desc))
                .toMap
    }

    private def setAccessible(f: Field): Boolean = {
        try {
            f.setAccessible(true)
            true
        } catch {
            case _: InaccessibleObjectException => false
        }
    }

}

object SimpleSyncObjectSuperClassDescription {

    private val SyntheticMod = 0x00001000

    private val cache = mutable.HashMap.empty[Class[_], SimpleSyncObjectSuperClassDescription[_]]

    implicit def fromTag[A: ClassTag]: SimpleSyncObjectSuperClassDescription[A] = apply[A](classTag[A].runtimeClass)

    def apply[A](clazz: Class[_]): SimpleSyncObjectSuperClassDescription[A] = cache.getOrElse(clazz, {
        if (classOf[SynchronizedObject[_]].isAssignableFrom(clazz))
            throw new IllegalArgumentException("Provided class already extends from SynchronizedObject")
        val AClass = clazz.asInstanceOf[Class[A]]
        new SimpleSyncObjectSuperClassDescription[A](AClass, clazz.getClassLoader)
    }).asInstanceOf[SimpleSyncObjectSuperClassDescription[A]]


}
