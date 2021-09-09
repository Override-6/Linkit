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

import java.lang
import java.lang.reflect.{Array => RArray}
import java.nio.ByteBuffer

import fr.linkit.engine.connection.packet.persistence.MalFormedPacketException
import fr.linkit.engine.connection.packet.persistence.pool.PoolChunk
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.connection.packet.persistence.serializor.read.ObjectPoolReader
import fr.linkit.engine.connection.packet.persistence.serializor.read.instance.{InstanceObject, InstantiatedObject}
import fr.linkit.engine.connection.packet.persistence.serializor.write.ObjectPoolWriter
import fr.linkit.engine.local.mapping.ClassMappings

object ArrayPersistence {

    def writeArrayContent(writer: ObjectPoolWriter, array: Array[AnyRef]): Unit = {
        val buff = writer.buff
        buff.putInt(array.length)
        for (n <- array.indices) {
            writer.putPoolRef(array(n), false)
        }
    }

    def writePrimitiveArrayContent(writer: ObjectPoolWriter, array: Array[Any], from: Int, to: Int): Unit = {
        val len  = array.length
        val buff = writer.buff
        (array: Any) match {
            case xs: Array[Int]     => buff.asIntBuffer().put(xs, from, to)
            case xs: Array[Double]  => buff.asDoubleBuffer().put(xs, from, to)
            case xs: Array[Long]    => buff.asLongBuffer().put(xs, from, to)
            case xs: Array[Float]   => buff.asFloatBuffer().put(xs, from, to)
            case xs: Array[Char]    => buff.asCharBuffer().put(xs, from, to)
            case xs: Array[Short]   => buff.asShortBuffer().put(xs, from, to)
            case xs: Array[Byte]    => buff.put(xs, from, to)
            case xs: Array[Boolean] =>
                var i = from
                while (i < Math.min(len, to)) {
                    buff.put(if (xs(i)) 1: Byte else 0: Byte);
                    i = i + 1
                }
        }
    }

    def writeArray(writer: ObjectPoolWriter, tpeChunk: PoolChunk[Class[_]], array: Array[Any]): Unit = {
        val buff = writer.buff
        val comp = array.getClass.componentType()
        if (comp.isPrimitive) {
            putPrimitiveArrayFlag(buff, comp)
            writePrimitiveArrayContent(writer, array, 0, array.length)
            return
        }

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
            buff.putChar(tpeIdx.toChar)
        }
        writeArrayContent(writer, array.asInstanceOf[Array[AnyRef]]) //we handled any primitive array before
    }

    def readArray(reader: ObjectPoolReader): Array[_] = {
        val buff   = reader.buff
        val kind   = buff.get()
        val length = buff.getInt()
        kind match {
            case Object => readObjectArray(length, reader)
            case String => readStringArray(length, reader)
            case _      => readNonObjectArray(length, kind, reader)
        }
    }

    private def readNonObjectArray(length: Int, kind: Int, reader: ObjectPoolReader): Array[_] = {
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

    private def readStringArray(length: Int, reader: ObjectPoolReader): Array[String] = {
        val sBuff = new Array[String](length)
        var i     = 0
        while (i < length) {
            sBuff(i) = reader.nextInstanceObject() match {
                case InstantiatedObject(str: String) => str
                case _                               => throw new MalFormedPacketException("Received non string item into string array.")
            }
            i += 1
        }
        sBuff
    }

    private def readObjectArray(length: Int, reader: ObjectPoolReader): Array[Any] = {
        val buff  = reader.buff
        val depth = buff.get() + lang.Byte.MAX_VALUE
        val comp  = ClassMappings.getClass(buff.getInt())
        val array = buildArray(comp, depth, length)
        val oBuff = new Array[InstanceObject[_]](length)
        readArrayContent(reader, oBuff)
        var i = 0
        while (i < length) {
            array(i) = oBuff(i).instance
            i += 1
        }
        array
    }

    def readArrayContent(reader: ObjectPoolReader): Array[InstanceObject[_]] = {
        val buff  = reader.buff
        val size  = buff.getInt()
        val array = new Array[InstanceObject[_]](size)
        readArrayContent(reader, array)
        array
    }

    def readArrayContent(reader: ObjectPoolReader, buff: Array[InstanceObject[_]]): Unit = {
        for (n <- buff.indices)
            buff(n) = reader.nextInstanceObject()
    }

    private def cast[T](a: Any): T = a.asInstanceOf[T]

    private def putPrimitiveArrayFlag(buff: ByteBuffer, comp: Class[_]): Unit = {
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
