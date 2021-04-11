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

import fr.linkit.api.connection.packet.Packet
import fr.linkit.core.connection.packet.UnexpectedPacketException

import java.lang.reflect.Field
import scala.reflect.{ClassTag, classTag}
import scala.util.control.NonFatal

object ScalaUtils {

    def slowCopy[A: ClassTag](origin: Array[_ <: Any]): Array[A] = {
        val buff = new Array[A](origin.length)
        var i    = 0
        try {
            origin.foreach(anyRef => {
                buff(i) = anyRef.asInstanceOf[A]
                i += 1
            })
            buff
        } catch {
            case NonFatal(e) =>
                println("Was Casting to " + classTag[A].runtimeClass)
                println(s"Origin = ${origin.mkString("Array(", ", ", ")")}")
                println(s"Failed when casting ref : ${origin(i)} at index $i")
                throw e
        }
    }

    def ensurePacketType[P <: Packet : ClassTag](packet: Packet): P = {
        val rClass = classTag[P].runtimeClass
        packet match {
            case p: P => p
            case null => throw new NullPointerException("Received null packet.")
            case p    => throw UnexpectedPacketException(s"Received unexpected packet type (${p.getClass.getName}), requested : ${rClass.getName}")
        }
    }

    implicit def deepToString(array: Array[Any]): String = {
        val sb = new StringBuilder("Array(")
        array.foreach {
            case subArray: Array[Any] => sb.append(deepToString(subArray))
            case any: Any             => sb.append(any).append(", ")
        }
        sb.dropRight(2) //remove last ", " string.
                .append(')')
                .toString()
    }

    implicit def deepToString(any: Any): String = {
        any match {
            case array: Array[Any] => deepToString(array)
            case any               => any.toString
        }
    }

    def setFieldValue(field: Field, owner: Any, value: Any): Unit = {
        value match {
            case v: Int => field.setInt(owner, v)
            case v: Long => field.setLong(owner, v)
            case v: Double => field.setDouble(owner, v)
            case v: Float => field.setFloat(owner, v)
            case v: Boolean => field.setBoolean(owner, v)
            case v: Byte => field.setByte(owner, v)
            case v: Short => field.setShort(owner, v)
            case v: Char => field.set(owner, v)
            case _ => field.set(owner, value)
        }
    }

}
