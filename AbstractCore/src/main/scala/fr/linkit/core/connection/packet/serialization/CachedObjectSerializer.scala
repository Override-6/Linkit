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

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates, PacketCoordinates}
import fr.linkit.core.connection.network.cache.map.{MapModification, SharedMap}
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.linkit.core.connection.packet.serialization.CachedObjectSerializer.Signature
import fr.linkit.core.connection.packet.serialization.NumberSerializer.serializeInt

class CachedObjectSerializer(cache: SharedCacheManager) extends ObjectSerializer {

    private val objectMap = cache.get(14, SharedMap[Int, String])

    /**
     * @return the hashcode of the class name under a byte sequences
     *         If the class name is not registered into the cache, it will be added
     * */
    override def serializeType(clazz: Class[_]): Array[Byte] = {
        val name = clazz.getName
        val hash = name.hashCode

        val hashBytes = serializeInt(hash)
        if (objectMap.contains(hash))
            return hashBytes //The type is already registered.

        objectMap.put(hash, name)
        hashBytes
    }

    /**
     * @return a tuple with the Class and his value length into the array
     * */
    override def deserializeType(bytes: Array[Byte]): (Class[_], Int) = {
        (Class.forName(objectMap.getOrWait(deserializeNumber(bytes, 0, 4).toInt)), 4) //4 is the byte length of one integer
    }

    override val signature: Array[Byte] = Signature

    /*
    * Those types are directly registered because they are potentially used by the packet that
    * is used to notify to other relays that a new type has been registered.
    * If these types are not directly registered, the serialisation/deserialization will logically block in order
    * to wait that one of these types are registered. But, they are used to notify that a new type has been registered.
    * so, a sort of deadlock will occur.
    * */
    serializeType(classOf[PacketCoordinates])
    serializeType(classOf[DedicatedPacketCoordinates])
    serializeType(classOf[BroadcastPacketCoordinates])
    serializeType(classOf[WrappedPacket])
    serializeType(classOf[ObjectPacket])
    serializeType(classOf[MapModification])
    serializeType(classOf[(_, _, _)])
    serializeType(Nil.getClass)

}

object CachedObjectSerializer {
    val Signature: Array[Byte] = Array(0)
}
