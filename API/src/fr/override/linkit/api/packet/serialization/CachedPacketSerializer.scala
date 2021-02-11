package fr.`override`.linkit.api.packet.serialization

import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.cache.map.SharedMap
import fr.`override`.linkit.api.packet.PacketCoordinates
import fr.`override`.linkit.api.packet.fundamental.{ValPacket, WrappedPacket}

class CachedPacketSerializer(cache: SharedCacheHandler) extends PacketSerializer {

    private val objectMap = cache.get(14, SharedMap[Int, String])
    objectMap.addListener(_ => println("MAP UPDATED : " + objectMap))

    override protected def serializeType(clazz: Class[_]): Array[Byte] = {
        val name = clazz.getName
        val key = name.hashCode

        val keyBytes = serializeInt(key)
        if (objectMap.contains(key))
            return keyBytes //The type is already registered.

        println(s"REGISTERING TYPE $key...")
        objectMap.put(key, name)
        println("Registered !")
        keyBytes
    }

    /**
     * @return a tuple with the Class and his value length into the array
     * */
    override protected def deserializeType(bytes: Array[Byte]): (Class[_], Int) = {
        (Class.forName(objectMap.getOrWait(deserializeInt(bytes, 0))), 4) //4 is the byte length of one integer
    }

    override protected val signature: Array[Byte] = Array(0)


    serializeType(classOf[PacketCoordinates]) //registering it by default.
    serializeType(classOf[WrappedPacket]) //registering it by default.
    serializeType(classOf[ValPacket]) //registering it by default.

}
