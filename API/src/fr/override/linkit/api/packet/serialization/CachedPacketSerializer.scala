package fr.`override`.linkit.api.packet.serialization

import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.cache.map.{MapModification, SharedMap}
import fr.`override`.linkit.api.packet.PacketCoordinates
import fr.`override`.linkit.api.packet.fundamental.{ValPacket, WrappedPacket}

class CachedPacketSerializer(cache: SharedCacheHandler) extends PacketSerializer {

    private val objectMap = cache.get(14, SharedMap[Int, String])

    override protected def serializeType(clazz: Class[_]): Array[Byte] = {
        val name = clazz.getName
        val key = name.hashCode

        val keyBytes = serializeInt(key)
        if (objectMap.contains(key))
            return keyBytes //The type is already registered.

        objectMap.put(key, name)
        keyBytes
    }

    /**
     * @return a tuple with the Class and his value length into the array
     * */
    override protected def deserializeType(bytes: Array[Byte]): (Class[_], Int) = {
        (Class.forName(objectMap.getOrWait(deserializeInt(bytes, 0))), 4) //4 is the byte length of one integer
    }

    override protected val signature: Array[Byte] = Array(0)


    /*
    * Those 4 types are directly registered because they are used by the packet that
    * is used to notify to other relays that a new type has been registered.
    * If these types are not directly registered, the serialisation/deserialization would block in order
    * to wait that one of these types are registered. But, they are used to notify that a new type has been registered.
    * So, a sort of deadlock will occur.
    * */
    serializeType(classOf[PacketCoordinates])
    serializeType(classOf[WrappedPacket])
    serializeType(classOf[ValPacket])
    serializeType(classOf[MapModification])
    serializeType(classOf[(_, _, _)])

}
