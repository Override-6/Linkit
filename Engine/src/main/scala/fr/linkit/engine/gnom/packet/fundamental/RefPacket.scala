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

package fr.linkit.engine.gnom.packet.fundamental

import fr.linkit.api.gnom.packet.Packet

import java.io.Serializable

/**
 * This trait is used to transport a packet for specific ref serializable types
 * such as strings or arrays.
 * */
sealed trait RefPacket[A] extends Packet {

    /**
     * The main value of the packet
     * */
    val value: A

    def casted[C <: A]: C = value.asInstanceOf[C]

    override def toString: String = {
        val valueString = value match {
            case array: Array[Any] => array.mkString("Array(", ", ", ")")
            case obj => String.valueOf(obj)
        }
        getClass.getSimpleName + s"($valueString)"
    }
}

object RefPacket {

    /**
     * Represents a packet that contains a string value
     * */
    case class StringPacket(override val value: String) extends RefPacket[String]

    /**
     * Represents a packet that contains a serializable value of a specified type
     * */
    case class AnyRefPacket[A] (override val value: A) extends RefPacket[A]

    /**
     * Represents a packet that contains a serializable value
     * */
    case class ObjectPacket(override val value: AnyRef) extends RefPacket[AnyRef]

    /**
     * Represents a packet that contains an array of serialized values of a specified type.
     * */
    //TODO Fix Array[Serializable] and Array[Any] cast exception
    case class ArrayRefPacket[A <: Any](override val value: Array[A]) extends RefPacket[Array[A]] {
                def apply(i: Int): Any = value(i)

        def isEmpty: Boolean = value.isEmpty

        def contains(a: Any): Boolean = {
            a match {
                case s: A => value.contains(s)
                case _    => false
            }
        }

        def length: Int = value.length

        override def toString: String = s"ArrayRefPacket(${if (value == null) "null" else value.mkString(",")})"
    }

    /**
     * Represents a packet that contains an array of serializable values
     * */
    //TODO Fix Array[Serializable] and Array[Any] cast exception
    class ArrayObjectPacket(array: Array[Any] = Array()) extends ArrayRefPacket[Any](array)

    object ArrayObjectPacket {

        def apply(array: Array[Any] = Array()): ArrayObjectPacket = new ArrayObjectPacket(array)
    }

    /**
     * Represents a packet that contains an array of primitive values
     * */
    case class ArrayValPacket[A <: AnyVal](override val value: Array[A]) extends RefPacket[Array[A]] {

        def apply(i: Int): A = value(i)

        def isEmpty: Boolean = value.isEmpty

        def contains(a: A): Boolean = value.contains(a)

        def length: Int = value.length
        
        override def toString: String = s"ArrayValPacket(${value.mkString(",")})"
    }

    /**
     * Alias for [[AnyRefPacket.apply()]]
     * */
    def apply[A](value: A): AnyRefPacket[A] = AnyRefPacket(value)

    /**
     * Implicit unboxing of a RefPacket's value.
     * */
    implicit def unbox[A <: Serializable](packet: RefPacket[A]): A = packet.value

}
