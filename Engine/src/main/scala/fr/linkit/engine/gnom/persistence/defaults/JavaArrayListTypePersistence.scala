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

package fr.linkit.engine.gnom.persistence.defaults

import fr.linkit.api.gnom.persistence.context.{ControlBox, TypePersistence}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.context.structure.ArrayObjectStructure
import fr.linkit.engine.internal.utils.ScalaUtils

import java.util

class JavaArrayListTypePersistence extends TypePersistence[util.ArrayList[AnyRef]] {

    override val structure: ObjectStructure = ArrayObjectStructure(classOf[Array[AnyRef]])

    override def initInstance(allocatedObject: util.ArrayList[AnyRef], args: Array[Any], box: ControlBox): Unit = {
        val clazz = allocatedObject.getClass
        val data = clazz.getConstructor().newInstance()
        args.head.asInstanceOf[Array[AnyRef]].foreach(data.add)
        ScalaUtils.pasteAllFields(allocatedObject, data)
    }

    override def toArray(t: util.ArrayList[AnyRef]): Array[Any] = Array(t.toArray)
}
