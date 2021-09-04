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

import java.lang
import java.nio.ByteBuffer

class ObjectPoolWriter(config: PacketConfig, context: PersistenceContext, buff: ByteBuffer) {

    val headerIndex: Int = buff.position()
    buff.position(headerIndex + 2) //skip 2 bytes for the pool length
    private val objects = new PacketConstantPool()

    def writeObjects(objects: Array[AnyRef]): Unit = {
        for (o <- objects) putAny(o)
    }

    def writeHeaderSize(): Unit = {
        val size = objects.size
        if (size > lang.Short.MAX_VALUE * 2 + 1)
            throw new PacketPoolTooLongException(s"Packet pool size exceeds ${lang.Short.MAX_VALUE * 2 + 1}")
        buff.putChar(headerIndex, size.toChar) //write the pool length at the starting point.
    }

    def getPool: PacketConstantPool = objects

    private def putAny(obj: Any): Unit = {
        obj match {
            case i: Int     => buff.putInt(i)
            case b: Byte    => buff.put(b)
            case s: Short   => buff.putShort(s)
            case l: Long    => buff.putLong(l)
            case d: Double  => buff.putDouble(d)
            case f: Float   => buff.putFloat(f)
            case b: Boolean => buff.put((if (b) 1 else 0): Byte)
            case c: Char    => buff.putChar(c)

            case other: AnyRef => putNonPrimitive(other)
        }
    }

    //Non primitives can be stored in the pool
    private def putNonPrimitive(ref: AnyRef): Unit = {
        val idx = objects.indexOf(ref)
        if (idx >= 0) {
            putRef(idx.toChar)
            return
        }
        objects add ref
        ref match {
            case array: Array[Any] => putArray(array)
            case str: String       => putString(str)
            case ref: AnyRef       => putObject(ref)
        }
    }

    private def putString(str: String): Unit = {
        //TODO Ensure that the same charset is used at the other side
        expand(String).putInt(str.length).put(str.getBytes())
    }

    private def putObject(obj: AnyRef): Unit = {
        val clazz   = obj.getClass
        val profile = config.getProfile[AnyRef](clazz, context)
        val code    = config.getReferencedCode(obj)
        if (code.isDefined) {
            buff.put(ContextRef)
                    .putInt(code.get)
            return
        }

        val array = profile.toArray(obj)
        expand(Object)
        buff.putInt(ClassMappings.codeOfClass(clazz))
        putArrayContent(array)
    }

    private def putArrayContent(array: Array[Any]): Unit = {

        buff.putInt(array.length)
        for (n <- array)
            putAny(n)
    }

    private def putRef(idx: Char): Unit = {
        buff.put(PoolRef)
                .putChar(idx)
    }

    def putArray(array: Array[Any]): Unit = {
        val comp = array.getClass.componentType()
        expand(Array)
        if (comp.isPrimitive) {
            putPrimitiveArrayFlag(comp)
        }
        if (comp eq classOf[String]) {
            buff.put(String)
        } else {
            buff.put(Object)
        }
        putArrayContent(array)
    }

    private def putPrimitiveArrayFlag(comp: Class[_]): Unit = {
        comp match {
            case c if c eq classOf[Integer]      => buff.put(Int)
            case c if c eq classOf[lang.Byte]    => buff.put(Byte)
            case c if c eq classOf[lang.Short]   => buff.put(Short)
            case c if c eq classOf[lang.Long]    => buff.put(Long)
            case c if c eq classOf[lang.Double]  => buff.put(Double)
            case c if c eq classOf[lang.Float]   => buff.put(Float)
            case c if c eq classOf[lang.Boolean] => buff.put(Boolean)
            case c if c eq classOf[Character]    => buff.put(Char)
        }
    }

    private def expand(flag: Byte): ByteBuffer = {
        buff.putChar(objects.size.toChar)
                .put(flag)
    }
}
