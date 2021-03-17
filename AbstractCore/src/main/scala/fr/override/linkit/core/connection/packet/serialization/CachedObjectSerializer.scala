package fr.`override`.linkit.core.connection.packet.serialization

import fr.`override`.linkit.core.connection.network.cache
import fr.`override`.linkit.core.connection.network.cache.map
import fr.`override`.linkit.skull.connection.network.cache.SharedCacheHandler
import fr.`override`.linkit.skull.connection.network.cache.map.{MapModification, SharedMap}
import fr.`override`.linkit.skull.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.`override`.linkit.skull.connection.packet.fundamental.WrappedPacket
import .serializeInt
import fr.`override`.linkit.skull.connection.packet.PacketCoordinates

class CachedObjectSerializer(cache: cache.SharedCacheHandler) extends ObjectSerializer {

    private val objectMap = cache.get(14, map.SharedMap[Int, String])
    //objectMap.addListener(_ => s"MODIFIED : $objectMap")

    /**
     * @return the hashcode of the class name under a byte sequences
     *         If the class name is not registered into the cache, it will be added
     * */
    override protected def serializeType(clazz: Class[_]): Array[Byte] = {
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
    override protected def deserializeType(bytes: Array[Byte]): (Class[_], Int) = {
        (Class.forName(objectMap.getOrWait(deserializeNumber(bytes, 0, 4).toInt)), 4) //4 is the byte length of one integer
    }

    override protected val signature: Array[Byte] = Array(0)

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
    serializeType(classOf[map.MapModification])
    serializeType(classOf[(_, _, _)])

    private val NilName = "scala.collection.immutable.Nil$" //This class is odd, could not find it from my IDE, but it still present at runtime.
    if (!objectMap.contains(NilName.hashCode))
        objectMap.put(NilName.hashCode, NilName)

}
