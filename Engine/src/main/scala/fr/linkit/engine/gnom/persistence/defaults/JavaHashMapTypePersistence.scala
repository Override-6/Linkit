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

import fr.linkit.api.gnom.persistence.context.TypePersistence
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.context.structure.ArrayObjectStructure
import fr.linkit.engine.internal.utils.ScalaUtils

import java.util
class JavaHashMapTypePersistence extends TypePersistence[util.HashMap[Any, Any]]{

    override val structure: ObjectStructure = ArrayObjectStructure(classOf[Array[AnyRef]])

    override def initInstance(allocatedObject: util.HashMap[Any, Any], args: Array[Any]): Unit = {
        val data = new util.HashMap[Any, Any]()
        args.head.asInstanceOf[Array[AnyRef]]
                .foreach(x => {
                    val (key, value) = x.asInstanceOf[(Any, Any)]
                    data.put(key, value)
                })
        ScalaUtils.pasteAllFields(allocatedObject, data)
    }

    override def toArray(t: util.HashMap[Any, Any]): Array[Any] = {
        val array = new Array[Any](t.size())
        val entries = t.entrySet().toArray(new Array[util.Map.Entry[Any, Any]](_))
        for (i <- array.indices) {
            val entry = entries(i)
            array(i) = (entry.getKey, entry.getValue)
        }
        Array(array)
    }
}
