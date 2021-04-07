/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.connection.packet.serialization

import fr.linkit.core.connection.packet.serialization.NumberSerializer.serializeInt

import scala.collection.mutable

/**
 * Used for tests only.
 * */
object LocalCachedObjectSerializer extends ObjectSerializer {

    private val cache = new mutable.HashMap[Int, String]()

    override val signature: Array[Byte] = Array(0)

    override def serializeType(clazz: Class[_]): Array[Byte] = {
        val name = clazz.getName
        val hash = name.hashCode

        println(s"name = ${name}")
        println(s"hash = ${hash}")

        cache.put(hash, name)
        serializeInt(hash)
    }

    /**
     * @return a tuple with the Class and his value length into the array
     * */
    override def deserializeType(bytes: Array[Byte]): (Class[_], Int) = {
        val numberLong = deserializeNumber(bytes, 0, 4)
        (Class.forName(cache(numberLong.toInt)), 4)
    }
}
