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

package fr.linkit.engine.gnom.cache.sync.contract.description

import java.lang.reflect._

import fr.linkit.api.gnom.cache.sync.contract.description.{FieldDescription, MethodDescription, SyncStructureDescription}
import fr.linkit.engine.gnom.cache.sync.generation.SyncObjectClassResource.{WrapperPackage, WrapperSuffixName}

import scala.collection.mutable.ListBuffer

abstract class AbstractSyncStructureDescription[A <: AnyRef](override val clazz: Class[A]) extends SyncStructureDescription[A] {

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
        getFiltered
            .map(MethodDescription(_, this))
            .map(desc => (desc.methodId, desc))
            .toMap
    }

    private def getAllMethods: ListBuffer[Method] = {
        val buff         = ListBuffer.from[Method](clazz.getMethods)
        var cl: Class[_] = clazz
        while (cl != null) {
            buff ++= cl.getDeclaredMethods.filter(m => Modifier.isProtected(m.getModifiers))
            cl = cl.getSuperclass
        }
        buff
    }

    protected def applyNotFilter(e: Executable): Boolean

    private def getFiltered: Iterable[Method] = {
        getAllMethods
            .distinctBy(m => (m.getName, m.getParameterTypes))
            .filterNot(applyNotFilter)
    }

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

