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

package fr.linkit.engine.connection.packet.persistence.serializor

import scala.collection.mutable.ListBuffer

class PacketConstantPool() {

    private val objects = ListBuffer.empty[AnyRef]

    def indexOf(ref: AnyRef): Int = {
        objects.indexOf(ref)
    }

    def add(ref: AnyRef): Unit = objects += ref

    def size: Int = objects.length

}

