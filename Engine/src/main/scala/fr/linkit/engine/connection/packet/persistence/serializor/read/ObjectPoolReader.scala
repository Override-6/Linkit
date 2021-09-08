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

package fr.linkit.engine.connection.packet.persistence.serializor.read

import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext}
import fr.linkit.engine.connection.packet.persistence.MalFormedPacketException
import fr.linkit.engine.connection.packet.persistence.obj.instance.{InstanceObject, InstantiatedObject, NotInstantiatedObject}
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.connection.packet.persistence.serializor.{ArrayPersistence, ClassNotMappedException}
import fr.linkit.engine.local.mapping.ClassMappings

import java.nio.ByteBuffer

class ObjectPoolReader(config: PacketConfig, context: PersistenceContext, val buff: ByteBuffer) {

    private val objects = new Array[InstanceObject[_]](buff.getChar())
    private var isInit  = false

    def initPool(): Unit = {
        if (isInit)
            throw new IllegalStateException("This pool is already initialized.")
        isInit = true
        for (i <- objects.indices) {
            if (objects(i) == null)
                objects(i) = nextInstanceObject()
        }
    }

    def getObject(idx: Char): Any = {
        objects(idx) match {
            case obj: InstantiatedObject[_]    => obj.instance
            case obj: NotInstantiatedObject[_] =>
                obj.initObject()
                obj.instance
        }
    }

    private[serializor] def nextInstanceObject(): InstanceObject[_] = {
        buff.get() match {
            case Object => readObject()
            case String => readString()
            case Array  => readArray()

            case PoolRef => objects(buff.getChar)
            case f       =>
                val obj = f match {
                    case ContextRef =>
                        val id = buff.getInt()
                        config.getReferenced(buff.getInt()).getOrElse {
                            throw new NoSuchElementException(s"Could not find contextual object of id '${id}' for this configuration.")
                        }
                    case Int        => buff.getInt
                    case Byte       => buff.get
                    case Short      => buff.getShort
                    case Long       => buff.getLong
                    case Double     => buff.getDouble
                    case Float      => buff.getFloat
                    case Boolean    => buff.get == 1
                    case Char       => buff.getDouble
                    case f          => throw new MalFormedPacketException(s"Unknown value flag '${f}' as pool constant")
                }
                InstantiatedObject(obj)
        }
    }

    private def readObject(): NotInstantiatedObject[AnyRef] = {
        val classCode = buff.getInt()
        val clazz     = ClassMappings.getClass(classCode)
        if (clazz == null)
            throw new ClassNotMappedException(s"Class of code '$classCode' is not mapped.")
        val profile     = config.getProfile[AnyRef](clazz, context)
        val pos         = buff.position()
        // The next int is the content size,
        // we skip the array content for now
        // because we need only parse the object type
        // as we return a NotInstantiatedObject, However, the pos in front
        // of the array content is kept in order to read the object content after
        val contentSize = buff.getInt
        val content = readObjectContent(contentSize)
        new NotInstantiatedObject[AnyRef](profile, content, this, clazz)
    }

    private def readObjectContent(length: Int): Array[Char] = {
        var i = 0
        val content = new Array[Char](length)
        while (i < length) {
            content(i) = buff.getChar
            i += 1
        }
        content
    }

    private def readArray(): InstantiatedObject[Array[_]] = {
        InstantiatedObject(ArrayPersistence.readArray(this))
    }

    private def readString(): InstantiatedObject[String] = {
        val size  = buff.getInt()
        val array = new Array[Byte](size)
        buff.get(array)
        InstantiatedObject(new String(array))
    }
}
