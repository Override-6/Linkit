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

package fr.linkit.engine.gnom.persistence.context.structure

import fr.linkit.api.gnom.persistence.obj.ObjectStructure

import scala.collection.mutable.ListBuffer

class ObjectStructureBuilder {

    private val types = ListBuffer.empty[Class[_]]

    def >(cl: Class[_]): this.type = {
        types += cl
        this
    }

}

object ObjectStructureBuilder {
    implicit def build(builder: ObjectStructureBuilder): ObjectStructure = {
        new ArrayObjectStructure {
            override val types: Array[Class[_]] = builder.types.toArray
        }
    }
}