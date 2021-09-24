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

package fr.linkit.engine.connection.packet.persistence.context.structure

import fr.linkit.api.connection.packet.persistence.obj.ObjectStructure

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