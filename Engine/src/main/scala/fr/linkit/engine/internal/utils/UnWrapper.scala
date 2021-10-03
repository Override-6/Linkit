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

package fr.linkit.engine.internal.utils

import java.lang

object UnWrapper {

    def unwrap[A <: AnyVal](value: Any, converter: PrimitiveWrapper => A): A = {
        value match {
            case n: Number       => converter(new NumberWrapper(n))
            case b: lang.Boolean => converter(new BooleanNumber(b))
            case c: Character    => converter(new CharacterNumber(c))
        }
    }

    def getPrimitiveClass(any: Any): Class[_] = {
        any match {
            case _: Integer      => Integer.TYPE
            case _: lang.Byte    => lang.Byte.TYPE
            case _: lang.Short   => lang.Short.TYPE
            case _: lang.Long    => lang.Long.TYPE
            case _: lang.Double  => lang.Double.TYPE
            case _: lang.Float   => lang.Float.TYPE
            case _: lang.Boolean => lang.Boolean.TYPE
            case _: Character    => Character.TYPE
            case o => o.getClass
        }
    }

    @inline
    def isPrimitiveWrapper(obj: Any): Boolean = {
        obj.isInstanceOf[Number] || obj.isInstanceOf[lang.Boolean] || obj.isInstanceOf[Character]
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
