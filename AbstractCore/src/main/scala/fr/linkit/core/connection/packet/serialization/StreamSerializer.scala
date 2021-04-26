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

package fr.linkit.core.connection.packet.serialization

import fr.linkit.api.connection.packet.serialization.Serializer
import fr.linkit.api.local.system.security.BytesHasher
import fr.linkit.core.local.utils.ScalaUtils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

class StreamSerializer(hasher: BytesHasher) extends Serializer {

    override val signature: Array[Byte] = Array(5)

    override def serialize(serializable: Serializable, withSignature: Boolean): Array[Byte] = {
        signature ++ hasher.hashBytes(serializeObj(serializable))
    }

    override def isSameSignature(bytes: Array[Byte]): Boolean = {
        bytes.startsWith(signature)
    }

    override def deserialize(bytes: Array[Byte]): Any = {
        if (!isSameSignature(bytes))
            throw new IllegalSignatureException(s"Signature mismatches (${ScalaUtils.toPresentableString(bytes)}")
        deserializeObj(hasher.deHashBytes(bytes.drop(1)))
    }

    override def deserializeAll(bytes: Array[Byte]): Array[Any] = {
        deserialize(bytes) match {
            case array: Array[Any] => array
            case _                 => throw new UnexpectedSerializationException("An array was expected from bytes deserialization.")
        }
    }

    private def serializeObj(any: Any): Array[Byte] = {
        val baos = new ByteArrayOutputStream()
        val oos  = new ObjectOutputStream(baos)
        oos.writeObject(any)
        val bytes = baos.toByteArray
        oos.close()
        baos.close()
        bytes
    }

    private def deserializeObj(array: Array[Byte]): AnyRef = {
        val bais = new ByteArrayInputStream(array)
        val ois  = new ObjectInputStream(bais)
        val obj  = ois.readObject()
        ois.close()
        bais.close()
        obj
    }
}
