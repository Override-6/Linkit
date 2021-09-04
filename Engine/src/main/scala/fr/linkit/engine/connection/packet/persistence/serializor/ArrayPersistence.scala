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

import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol.{Boolean, Byte, Char, Double, Float, Int, Long, Object, Short, String}
import fr.linkit.engine.local.mapping.ClassMappings

import java.lang
import java.lang.reflect.{Array => RArray}
import java.nio.ByteBuffer

object ArrayPersistence {

    def writeArrayContent(writer: ObjectPoolWriter, array: Array[Any]): Unit = {
        val buff = writer.buff
        buff.putInt(array.length)
        for (n <- array)
            writer.putAny(n)
    }

    def writeArray(writer: ObjectPoolWriter, array: Array[Any]): Unit = {
        val buff = writer.buff
        val comp = array.getClass.componentType()
        if (comp.isPrimitive) {
            putPrimitiveArrayFlag(buff, comp)
        } else if (comp eq classOf[String]) {
            buff.put(String)
        } else {
            buff.put(Object)
            val (absoluteComp, depth) = getAbsoluteCompType(array)
            buff.put(depth)
            buff.putInt(ClassMappings.codeOfClass(absoluteComp))

        }
        writeArrayContent(writer, array)
    }

    def readArray(reader: ObjectPoolReader): Array[_] = {
        val buff   = reader.buff
        val kind   = buff.get()
        val length = buff.getInt()
        kind match {
            case Object => readObjectArray(length, reader)
            case _      => readNonObjectArray(length, kind, reader)
        }
    }

    private def readNonObjectArray(length: Int, kind: Int, reader: ObjectPoolReader): Array[_] = {
        val array: Array[_] = kind match {
            case Int     => new Array[Int](length)
            case Byte    => new Array[Byte](length)
            case Short   => new Array[Short](length)
            case Long    => new Array[Long](length)
            case Double  => new Array[Double](length)
            case Float   => new Array[Float](length)
            case Boolean => new Array[Boolean](length)
            case Char    => new Array[Char](length)
            case String  => new Array[String](length)
        }
        readArrayContent(reader, array)
        array
    }

    private def readObjectArray(length: Int, reader: ObjectPoolReader): Array[Any] = {
        val buff  = reader.buff
        val depth = buff.get() + lang.Byte.MAX_VALUE
        val comp  = ClassMappings.getClass(buff.getInt())
        val array = buildArray(comp, depth, length)
        readArrayContent(reader, array)
        array
    }

    def readArrayContent(reader: ObjectPoolReader): Array[Any] = {
        val buff  = reader.buff
        val size  = buff.getInt()
        val array = new Array[Any](size)
        readArrayContent(reader, array)
        array
    }

    def readArrayContent(reader: ObjectPoolReader, buff: Array[_]): Unit = {
        for (n <- buff.indices)
            buff(n) = cast(reader.nextPoolConstant())
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
