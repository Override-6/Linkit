/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.defaults

import fr.linkit.api.gnom.persistence.context.{ControlBox, TypePersistence}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.structure.ArrayObjectStructure
import fr.linkit.engine.internal.util.ScalaUtils

import scala.collection.IterableFactory
import scala.collection.immutable.ArraySeq
import scala.util.control.NonFatal

object ScalaIterableTypePersistence extends TypePersistence[Iterable[Any]] {

    override val structure: ObjectStructure = ArrayObjectStructure(classOf[Array[AnyRef]])

    override def initInstance(allocatedObject: Iterable[Any], args: Array[Any], box: ControlBox): Unit = {
        val factory = companionOf(allocatedObject.getClass, allocatedObject.getClass)
        val result  = factory.newBuilder
                .addAll(args.head.asInstanceOf[Array[AnyRef]])
                .result()
        ScalaUtils.pasteAllFields(allocatedObject, result)
    }

    override def toArray(t: Iterable[Any]): Array[Any] = Array(t.toArray)

    private def companionOf(origin: Class[_], clazz: Class[_]): IterableFactory[Iterable] = try {
        val companionClassName = clazz.getName + "$"
        val companionClass     = Class.forName(companionClassName)
        val moduleField        = companionClass.getField("MODULE$")
        moduleField.get(null).asInstanceOf[IterableFactory[Iterable]]
    } catch {
        case NonFatal(_) =>
            val superClass = clazz.getSuperclass
            if (superClass == null)
                throw new NoSuchElementException(s"Could not find suitable Iterable factory for $origin object instantiation")
            else companionOf(origin, superClass)
    }

}
