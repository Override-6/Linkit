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

package fr.linkit.engine.gnom.persistence.serializor

import fr.linkit.api.gnom.persistence.obj.PoolObject
import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.gnom.persistence.serializor.read.{NotInstantiatedArray, ObjectReader}
import fr.linkit.engine.gnom.persistence.serializor.write.ObjectWriter
import java.lang
import java.lang.reflect.{Array => RArray}
import java.nio.ByteBuffer

import scala.annotation.switch

object ArrayPersistence {

    def writeArrayContent(writer: ObjectWriter, array: Array[Any]): Unit = {
        val buff = writer.buff
        buff.putInt(array.length)
        for (n <- array) {
            writer.putPoolRef(n)
        }
    }

    def writePrimitiveArrayContent(writer: ObjectWriter, array: AnyRef, tpe: Byte, from: Int, to: Int): Unit = {
        val buff = writer.buff

        @inline
        def xs[X] = array.asInstanceOf[Array[X]]

        val pos      = buff.position()
        val itemSize = (tpe: @switch) match {
            case Int     => buff.asIntBuffer().put(xs, from, to); Integer.BYTES
            case Double  => buff.asDoubleBuffer().put(xs, from, to); lang.Double.BYTES
            case Long    => buff.asLongBuffer().put(xs, from, to); lang.Long.BYTES
            case Float   => buff.asFloatBuffer().put(xs, from, to); lang.Float.BYTES
            case Char    => buff.asCharBuffer().put(xs[Char], from, to); lang.Character.BYTES
            case Short   => buff.asShortBuffer().put(xs, from, to); lang.Short.BYTES
            case Byte    => buff.put(xs, from, to); lang.Byte.BYTES
            case Boolean =>
                var i   = from
                val x   = xs[Boolean]
                val lim = Math.min(x.length, to)
                while (i < lim) {
                    buff.put(if (x(i)) 1: Byte else 0: Byte)
                    i += 1
                }
                lang.Byte.BYTES
        }
        buff.position(pos + (to - from) * itemSize)
    }

    def writeArray(writer: ObjectWriter, array: AnyRef): Unit = {
        val buff = writer.buff
        val comp = array.getClass.componentType()
        if (comp.isPrimitive) {
            val flag = putPrimitiveArrayFlag(buff, comp)
            val len  = RArray.getLength(array)
            buff.putInt(len)
            writePrimitiveArrayContent(writer, array, flag, 0, len)
            return
        }
        writeObjectArray(array.asInstanceOf[Array[Any]], comp, writer)
    }

    @inline
    private def writeObjectArray(array: Array[Any], comp: Class[_], writer: ObjectWriter): Unit = {
        val buff = writer.buff
        if (comp eq classOf[String]) {
            buff.put(String)
        } else {
            buff.put(Object)
            val (absoluteComp, depth) = getAbsoluteCompType(array)
            buff.put(depth)
            writer.putTypeRef(absoluteComp, false)
        }
        writeArrayContent(writer, array)
    }

    private def readObjectArray(reader: ObjectReader): PoolObject[Array[AnyRef]] = {
        val buff    = reader.buff
        val depth   = buff.get() + lang.Byte.MAX_VALUE
        val compRef = reader.readNextRef
        val pool    = reader.getPool
        val comp    = pool.getType(compRef)
        val length  = buff.getInt()
        val array   = buildArray(comp, depth, length)
        val content = readArrayContent(reader, length)
        new NotInstantiatedArray[AnyRef](pool, content, array)
    }

    def readArray(reader: ObjectReader): PoolObject[_ <: AnyRef] = {
        val buff = reader.buff
        val kind = buff.get()
        kind match {
            case Object => readObjectArray(reader)
            case String =>
                val length = buff.getInt()
                val array = new Array[String](length)
                val content = readArrayContent(reader, length)
                new NotInstantiatedArray[String](reader.getPool, content, array)
            case _      =>
                new PoolObject[AnyRef] {
                    override val value   : AnyRef = readPrimitiveArray(kind, reader)
                    override val identity: Int    = System.identityHashCode(value)
                }
        }
    }

    def readPrimitiveArray(length: Int, kind: Int, reader: ObjectReader): AnyRef = {
        val buff              = reader.buff
        val pos               = buff.position()
        val (array, itemSize) = (kind: @switch) match {
            case Int     =>
                val a = new Array[Int](length)
                buff.asIntBuffer().get(a)
                (a, Integer.BYTES)
            case Byte    =>
                val a = new Array[Byte](length)
                buff.get(pos, a)
                (a, lang.Byte.BYTES)
            case Short   =>
                val a = new Array[Short](length)
                buff.asShortBuffer.get(a)
                (a, lang.Short.BYTES)
            case Long    =>
                val a = new Array[Long](length)
                buff.asLongBuffer().get(a)
                (a, lang.Long.BYTES)
            case Double  =>
                val a = new Array[Double](length)
                buff.asDoubleBuffer().get(a)
                (a, lang.Double.BYTES)
            case Float   =>
                val a = new Array[Float](length)
                buff.asFloatBuffer().get(a)
                (a, lang.Float.BYTES)
            case Boolean =>
                val a = new Array[Boolean](length)
                var i = 0
                while (i < length) {
                    a(i) = buff.get() == 1
                    i += 1
                }
                (a, 1)
            case Char    =>
                val a = new Array[Char](length)
                buff.asCharBuffer().get(a)
                (a, Character.BYTES)
        }
        buff.position(pos + length * itemSize)
        array
    }

    def readPrimitiveArray(kind: Int, reader: ObjectReader): AnyRef = {
        readPrimitiveArray(reader.buff.getInt(), kind, reader)
    }

    /*def readArrayContent(reader: PacketReader): PoolObject[Array[Any]] = {
        val buff  = reader.buff
        val size  = buff.getInt()
        val array = new Array[Any](size)
        readArrayContent(reader, size)
        array
    }*/

    def readArrayContent(reader: ObjectReader, len: Int): Array[Int] = {
        var n       = 0
        val content = new Array[Int](len)
        while (n < len) {
            content(n) = reader.readNextRef
            n += 1
        }
        content
    }

    private def putPrimitiveArrayFlag(buff: ByteBuffer, comp: Class[_]): Byte = {
        val compName = comp.getName
        val flag     = (compName: @switch) match {
            case "int"     => Int
            case "byte"    => Byte
            case "short"   => Short
            case "long"    => Long
            case "double"  => Double
            case "float"   => Float
            case "boolean" => Boolean
            case "char"    => Char
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
    def getAbsoluteCompType(array: Array[_]): (Class[_], Byte) = {
        var i    : Byte     = lang.Byte.MIN_VALUE
        var clazz: Class[_] = array.getClass
        while (clazz.isArray) {
            i = (i + 1).toByte
            clazz = clazz.componentType()
        }
        (clazz, i)
    }

    private def buildArray(compType: Class[_], arrayDepth: Int, arrayLength: Int): Array[AnyRef] = {
        var finalCompType = compType
        var i             = 0
        while (i < arrayDepth) {
            finalCompType = finalCompType.arrayType()
            i += 1
        }
        RArray.newInstance(finalCompType, arrayLength).asInstanceOf[Array[AnyRef]]
    }

}
