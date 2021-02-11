package fr.`override`.linkit.api.packet.serialization

import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.cache.map.SharedMap

class CachedPacketSerializer(cache: SharedCacheHandler) extends PacketSerializer {

    private val objectMap = cache.open(14, SharedMap[Int, String])

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
        (Class.forName(objectMap(deserializeInt(bytes, 0))), 4) //4 is the serialized byte length for an int
    }

    override protected val signature: Array[Byte] = Array(1)
}
