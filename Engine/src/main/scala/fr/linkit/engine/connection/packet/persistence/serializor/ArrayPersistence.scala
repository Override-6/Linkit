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

import fr.linkit.engine.connection.packet.persistence.pool.PoolChunk
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.connection.packet.persistence.serializor.read.PacketReader
import fr.linkit.engine.connection.packet.persistence.serializor.write.PacketWriter
import fr.linkit.engine.local.mapping.ClassMappings

import java.lang
import java.lang.reflect.{Array => RArray}
import java.nio.ByteBuffer
import scala.annotation.switch

object ArrayPersistence {

    def writeArrayContent(writer: PacketWriter, array: Array[AnyRef]): Unit = {
        val buff = writer.buff
        buff.putInt(array.length)
        for (n <- array) {
            writer.putPoolRef(n, false)
        }
    }

    def writePrimitiveArrayContent(writer: PacketWriter, array: AnyRef, tpe: Byte, from: Int, to: Int): Unit = {
        val buff = writer.buff

        @inline
        def xs[X] = array.asInstanceOf[Array[X]]

        (tpe: @switch) match {
            case Int     => buff.asIntBuffer().put(xs, from, to)
            case Double  => buff.asDoubleBuffer().put(xs, from, to)
            case Long    => buff.asLongBuffer().put(xs, from, to)
            case Float   => buff.asFloatBuffer().put(xs, from, to)
            case Char    => buff.asCharBuffer().put(xs[Char], from, to)
            case Short   => buff.asShortBuffer().put(xs, from, to)
            case Byte    => buff.put(xs, from, to)
            case Boolean =>
                var i   = from
                val x   = xs
                val len = x.length
                while (i < Math.min(len, to)) {
                    buff.put(if (x(i)) 1: Byte else 0: Byte);
                    i = i + 1
                }
        }
    }

    def writeArray(writer: PacketWriter, tpeChunk: PoolChunk[Class[_]], array: Array[Any]): Unit = {
        val buff = writer.buff
        val comp = array.getClass.componentType()
        if (comp.isPrimitive) {
            val flag = putPrimitiveArrayFlag(buff, comp)
            writePrimitiveArrayContent(writer, array.asInstanceOf[Array[AnyVal]], flag, 0, array.length)
            return
        }
        writeObjectArray(array, comp, writer, tpeChunk)
    }

    @inline
    private def writeObjectArray(array: Array[Any], comp: Class[_], writer: PacketWriter, tpeChunk: PoolChunk[Class[_]]): Unit = {
        val buff = writer.buff
        if (comp eq classOf[String]) {
            buff.put(String)
        } else {
            buff.put(Object)
            val (absoluteComp, depth) = getAbsoluteCompType(array)
            buff.put(depth)
            var tpeIdx = tpeChunk.indexOf(absoluteComp)
            if (tpeIdx == -1) {
                tpeIdx = tpeChunk.size
                tpeChunk.add(absoluteComp)
            }
            writer.putRef(tpeIdx)
        }
        writeArrayContent(writer, array.asInstanceOf[Array[AnyRef]]) //we handled any primitive array before
    }

    def readArray(reader: PacketReader): Array[_] = {
        val buff   = reader.buff
        val kind   = buff.get()
        val length = buff.getInt()
        kind match {
            case Object | String => readObjectArray(length, reader)
            case _               => readPrimitiveArray(length, kind, reader)
        }
    }

    def readPrimitiveArray(length: Int, kind: Int, reader: PacketReader): Array[_] = {
        val buff            = reader.buff
        val array: Array[_] = kind match {
            case Int     =>
                val a = new Array[Int](length)
                buff.asIntBuffer().get(a)
                a
            case Byte    =>
                val a = new Array[Byte](length)
                buff.get(a)
                a
            case Short   =>
                val a = new Array[Short](length)
                buff.asShortBuffer.get(a)
                a
            case Long    =>
                val a = new Array[Long](length)
                buff.asLongBuffer().get(a)
                a
            case Double  =>
                val a = new Array[Double](length)
                buff.asDoubleBuffer().get(a)
                a
            case Float   =>
                val a = new Array[Float](length)
                buff.asFloatBuffer().get(a)
                a
            case Boolean =>
                val a = new Array[Boolean](length)
                var i = 0
                while (i < 0) {
                    a(i) = buff.get(i) == 1
                    i += 1
                }
                a
            case Char    =>
                val a = new Array[Char](length)
                buff.asCharBuffer().get(a)
                a
        }
        array
    }

    private def readObjectArray(length: Int, reader: PacketReader): Array[Any] = {
        val buff  = reader.buff
        val depth = buff.get() + lang.Byte.MAX_VALUE
        val comp  = ClassMappings.getClass(buff.getInt())
        val array = buildArray(comp, depth, length)
        readArrayContent(reader, array)
        array
    }

    def readArrayContent(reader: PacketReader): Array[Any] = {
        val buff  = reader.buff
        val size  = buff.getInt()
        val array = new Array[Any](size)
        readArrayContent(reader, array)
        array
    }

    def readArrayContent(reader: PacketReader, buff: Array[Any]): Unit = {
        val pool = reader.getPool
        for (n <- buff.indices)
            buff(n) = pool.getAny(reader.readNextRef)
    }

    private def putPrimitiveArrayFlag(buff: ByteBuffer, comp: Class[_]): Byte = {
        val flag = comp match {
            case c if c eq classOf[Integer]      => Int
            case c if c eq classOf[lang.Byte]    => Byte
            case c if c eq classOf[lang.Short]   => Short
            case c if c eq classOf[lang.Long]    => Long
            case c if c eq classOf[lang.Double]  => Double
            case c if c eq classOf[lang.Float]   => Float
            case c if c eq classOf[lang.Boolean] => Boolean
            case c if c eq classOf[Character]    => Char
        }
        buff.put(flag)
        flag
    }

    /**
     *
     * @param array the array to test
     * @return a tuple where the left index is the absolute component type of the array and the right index
     *         is an unsigned byte of the depth of the absolute component type in the array
     */
    private def getAbsoluteCompType(array: Array[_]): (Class[_], Byte) = {
        var i    : Byte     = lang.Byte.MIN_VALUE
        var clazz: Class[_] = array.getClass
        while (clazz.isArray) {
            i = (i + 1).toByte
            clazz = clazz.componentType()
        }
        (clazz, i)
    }

    private def buildArray(compType: Class[_], arrayDepth: Int, arrayLength: Int): Array[Any] = {
        var finalCompType = compType
        for (_ <- 0 until arrayDepth) {
            finalCompType = finalCompType.arrayType()
        }
        RArray.newInstance(finalCompType, arrayLength).asInstanceOf[Array[Any]]
    }

}
