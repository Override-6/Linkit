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

package fr.linkit.core.local.utils

import java.lang

object NumberSerializer {

    def serializeLong(value: Long): Array[Byte] = {
        Array[Byte](
            ((value >> 56) & 0xff).toByte,
            ((value >> 48) & 0xff).toByte,
            ((value >> 40) & 0xff).toByte,
            ((value >> 32) & 0xff).toByte,
            ((value >> 24) & 0xff).toByte,
            ((value >> 16) & 0xff).toByte,
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    def deserializeLong(bytes: Array[Byte], index: Int): Long = {
        println("Deserializing long in zone " + new String(bytes.slice(index, index + 8)))
        println("raw bytes = " + bytes.mkString("Array(", ", ", ")"))
        ((0xff & bytes(index): Long) << 56 |
                (0xff & bytes(index + 1): Long) << 48 |
                (0xff & bytes(index + 2): Long) << 40 |
                (0xff & bytes(index + 3): Long) << 32 |
                (0xff & bytes(index + 4): Long) << 24 |
                (0xff & bytes(index + 5): Long) << 16 |
                (0xff & bytes(index + 6): Long) << 8 |
                (0xff & bytes(index + 7): Long) << 0)
    }

    def deserializeInt(bytes: Array[Byte], index: Int): Int = {
        println(s"Deserializing int from byte array ${ScalaUtils.toPresentableString(bytes.take(index + 4))}")
        val result = (0xff & bytes(index)) << 24 |
                ((0xff & bytes(index + 1)) << 16) |
                ((0xff & bytes(index + 2)) << 8) |
                ((0xff & bytes(index + 3)) << 0)
        println(s"result = ${result}")
        result
    }

    def serializeInt(value: Int): Array[Byte] = {
        Array[Byte](
            ((value >> 24) & 0xff).toByte,
            ((value >> 16) & 0xff).toByte,
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    def serializeShort(value: Short): Array[Byte] = {
        Array[Byte](
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    def deserializeShort(bytes: Array[Byte], index: Int): Short = {
        (((0xff & bytes(index)) << 8) |
                ((0xff & bytes(index + 1)) << 0)).toShort
    }

    def serializeNumber(value: Long, insertFlag: Boolean = false): Array[Byte] = {
        println(s"Serializing number $value, insertFlag: $insertFlag")

        def flagged(array: Array[Byte]): Array[Byte] = (if (insertFlag) Array(array.length.toByte) else Array()) ++ array

        if (Byte.MinValue < value && value < Byte.MaxValue) {
            println("Byte")
            return flagged(Array(value.toByte))
        }

        if (Short.MinValue < value && value < Short.MaxValue) {
            println(s"Short (${value.toShort}) - " + new String(serializeShort(value.toShort)))
            return flagged(serializeShort(value.toShort))
        }

        if (Int.MinValue < value && value < Int.MaxValue) {
            println("Int")
            return flagged(serializeInt(value.toInt))
        }

        println("Long")
        flagged(serializeLong(value))
    }

    /**
     * @return a pair with the deserialized number at left, and its length in the array at right.
     * */
    def deserializeFlaggedNumber[@specialized(Byte, Short, Int, Long) T <: AnyVal](bytes: Array[Byte], start: Int): (T, Byte) = {
        var result: Long = 0
        println(s"Number:|: ${bytes.slice(start, start + 8).mkString("Array(", ", ", ")")}")
        println(s"Number:|: ${new String(bytes.slice(start, start + 4))}")
        val numberLength = bytes(start)
        println(s"Deserializing number in region ${new String(bytes.slice(start, start + numberLength))}")
        if (numberLength == 1)
            return (bytes(start + 1).asInstanceOf[T], 2)

        val limit = start + numberLength

        println(s"from = ${start + 1}")
        println(s"limit = $limit")

        for (i <- (start + 1) to limit) {
            val b = bytes(i)
            println(s"i = ${i}")
            println(s"b = ${b} ('${new String(Array(b))}')")
            val place = (limit - i) * 8
            println(s"place = ${place}")
            result |= (0xff & b) << place
            println(s"result = ${result}")
        }
        println(s"result = ${result}")

        (result.asInstanceOf[T], (numberLength + 1).toByte)
    }

    def convertToShortArray(array: Array[Byte]): Array[Short] = {
        array.grouped(2).map(deserializeShort(_, 0)).toArray
    }

    def convertToIntArray(array: Array[Byte]): Array[Int] = {
        array.grouped(4).map(deserializeInt(_, 0)).toArray
    }

    def convertToLongArray(array: Array[Byte]): Array[Long] = {
        array.grouped(8).map(deserializeLong(_, 0)).toArray
    }

    def convertByteArray(array: Array[Short]): Array[Byte] = {
        array.flatMap(serializeShort)
    }

    def convertByteArray(array: Array[Int]): Array[Byte] = {
        array.flatMap(serializeInt)
    }

    def convertByteArray(array: Array[Long]): Array[Byte] = {
        array.flatMap(serializeLong)
    }

    def convertValue[A <: AnyVal](value: Any, converter: PrimitiveWrapper => A): A = {
        value match {
            case n: Number       => converter(new NumberWrapper(n))
            case b: lang.Boolean => converter(new BooleanNumber(b))
            case c: Character    => converter(new CharacterNumber(c))
        }
    }

    sealed trait PrimitiveWrapper extends Number {

        def booleanValue: Boolean

        def charValue: Char
    }

    class CharacterNumber(c: Character) extends PrimitiveWrapper {

        override def intValue: Int = c.toInt

        override def longValue: Long = c.toLong

        override def floatValue: Float = c.toFloat

        override def doubleValue: Double = c.toDouble

        override def booleanValue: Boolean = intValue == 1

        override def charValue: Char = c
    }

    class BooleanNumber(b: java.lang.Boolean) extends PrimitiveWrapper {

        override def intValue: Int = if (b) 1 else 0

        override def longValue: Long = intValue

        override def floatValue: Float = intValue

        override def doubleValue: Double = intValue

        override def booleanValue: Boolean = b

        override def charValue: Char = if (b) 'y' else 'n'
    }

    class NumberWrapper(n: Number) extends PrimitiveWrapper {

        override def booleanValue: Boolean = intValue == 1

        override def charValue: Char = intValue.toChar

        override def intValue: Int = n.intValue

        override def longValue: Long = n.longValue

        override def floatValue: Float = n.floatValue

        override def doubleValue: Double = n.doubleValue
    }

}
