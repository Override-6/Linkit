package fr.`override`.linkit.api.packet.serialization

import fr.`override`.linkit.api.packet.serialization.NumberSerializer.serializeInt

import scala.collection.mutable

/**
 * Used for tests only.
 * */
object LocalCachedObjectSerializer extends ObjectSerializer {
    private val cache = new mutable.HashMap[Int, String]()

    override protected val signature: Array[Byte] = Array(0)

    override protected def serializeType(clazz: Class[_]): Array[Byte] = {
        val name = clazz.getName
        val hash = name.hashCode
        //println(s"name = ${name}")
        //println(s"hash = ${hash}")
        //println(s"cache = ${cache}")

        cache.put(hash, name)
        serializeInt(hash)
    }

    /**
     * @return a tuple with the Class and his value length into the array
     * */
    override protected def deserializeType(bytes: Array[Byte]): (Class[_], Int) = {
        val numberLong = deserializeNumber(bytes, 0, 4)
        //println(s"numberLong = ${numberLong}")
        //println(s"numberInt = ${numberLong.toInt}")
        (Class.forName(cache(numberLong.toInt)), 4)
    }
}
