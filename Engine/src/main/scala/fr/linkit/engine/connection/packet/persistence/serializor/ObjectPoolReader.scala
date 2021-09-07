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
import fr.linkit.engine.connection.packet.persistence.MalFormedPacketException
import fr.linkit.engine.connection.packet.persistence.obj.{InstanceObject, InstantiatedObject, NotInstantiatedObject}
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._
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
                objects(i) = nextAny()
        }
    }

    def getObject(idx: Int): Any = objects(idx)

    private[serializor] def nextPoolConstant(): Any = {
        buff.get() match {
            case PoolRef => objects(buff.getChar)

            case Int     => buff.getInt
            case Byte    => buff.get
            case Short   => buff.getShort
            case Long    => buff.getLong
            case Double  => buff.getDouble
            case Float   => buff.getFloat
            case Boolean => buff.get == 1
            case Char    => buff.getDouble
            case o => throw new MalFormedPacketException(s"Unknown value flag '${o}' as pool constant")
        }
    }

    private[serializor] def nextAny(): InstanceObject[_] = {
        buff.get() match {
            case Object => readObject()
            case String => readString()
            case Array  => readArray()
            case _      =>
                buff.position(buff.position() - 1)
                nextPoolConstant() match {
                    case i: InstanceObject[_] => i
                    case o => new InstantiatedObject[Any](o)
                }
        }
    }

    private def readObject(): NotInstantiatedObject[AnyRef] = {
        val classCode = buff.getInt()
        val clazz     = ClassMappings.getClass(classCode)
        if (clazz == null)
            throw new ClassNotMappedException(s"Class of code '$classCode' is not mapped.")
        val content = ArrayPersistence.readArrayContent(this)
        val profile = config.getProfile[AnyRef](clazz, context)
        new NotInstantiatedObject[AnyRef](profile, content, clazz)
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
