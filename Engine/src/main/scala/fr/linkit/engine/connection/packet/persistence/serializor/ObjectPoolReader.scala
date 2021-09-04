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

import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext}
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.local.mapping.ClassMappings

import java.nio.ByteBuffer

class ObjectPoolReader(config: PacketConfig, context: PersistenceContext, buff: ByteBuffer) {

    private val objects = new Array[AnyRef](buff.getChar())

    def readPool(): Unit = {
        for (i <- objects.indices) {
            if (objects(i) == null)
                objects(i) = nextObject()
        }
    }

    private def nextObject(): AnyRef = {
        buff.get() match {
            case Object => readObject()
            case Array  => readArray()
            case String => readString()
        }
    }

    private def readObject(): AnyRef = {
        val classCode = buff.getInt()
        val clazz     = ClassMappings.getClass(classCode)
        if (clazz == null)
            throw new ClassNotMappedException(s"Class of code '$classCode' is not mapped.")
        val content = readArray()
        val profile = config.getProfile[AnyRef](clazz, context)
        profile.newInstance(content)
    }

    private def readArray(): Array[Any] = {
        val size = buff.getInt()
        val compType = buff.getC
    }

    private def readString(): String = {
        val size  = buff.getInt()
        val array = new Array[Byte](size)
        buff.get(array)
        new String(array)
    }
}
