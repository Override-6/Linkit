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

import fr.linkit.api.connection.packet.PacketException
import fr.linkit.core.local.mapping.ClassMappings

object HashCodeTypesObjectSerializer extends ObjectSerializer {

    val Signature: Array[Byte] = Array(1)

    override def serializeType(clazz: Class[_]): Array[Byte] = {
        val hashCode = clazz.getName.hashCode
        NumberSerializer.serializeInt(hashCode)
    }

    /**
     * @return a tuple with the Class and his value length into the array
     * */
    override def deserializeType(bytes: Array[Byte]): (Class[_], Int) = {
        val hash = NumberSerializer.deserializeInt(bytes, 0)
        val name = ClassMappings.getClassName(hash)
        if (name == null)
            throw new PacketException(s"Received unmapped class hashcode ($hash)")

        (Class.forName(name), 4) //4 is the byte length of one integer
    }

    override val signature: Array[Byte] = Signature
}
