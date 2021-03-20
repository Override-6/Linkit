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

package fr.`override`.linkit.api.packet.serialization

object RawObjectSerializer extends ObjectSerializer {

    val Separator: Array[Byte] = ";".getBytes

    override protected def serializeType(clazz: Class[_]): Array[Byte] = {
        clazz.getName.getBytes ++ Separator
    }

    /**
     * @return a tuple with the Class and his value length into the array
     * */
    override protected def deserializeType(bytes: Array[Byte]): (Class[_], Int) = {
        val length = bytes.indexOfSlice(Separator)
        val className = new String(bytes.take(length))
        (Class.forName(className), length + 1) //add the ';' character
    }

    override protected val signature: Array[Byte] = Array(1)
}
