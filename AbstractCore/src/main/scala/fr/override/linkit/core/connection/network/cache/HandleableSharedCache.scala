package fr.`override`.linkit.core.connection.network.cache

import fr.`override`.linkit.api.connection.network.cache.SharedCache
import fr.`override`.linkit.api.connection.packet.traffic.PacketChannel
import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.local.system.{CloseReason, JustifiedCloseable}
import fr.`override`.linkit.core.connection.packet.fundamental.RefPacket.ArrayObjectPacket
import fr.`override`.linkit.core.connection.packet.fundamental.ValPacket.LongPacket
import fr.`override`.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.`override`.linkit.core.local.utils.ScalaUtils
import org.jetbrains.annotations.Nullable

import scala.reflect.ClassTag

abstract class HandleableSharedCache[A <: Serializable : ClassTag](@Nullable handler: AbstractSharedCacheManager,
                                                                   identifier: Long,
                                                                   channel: PacketChannel) extends SharedCache with JustifiedCloseable {

    override val family: String = if (handler == null) "" else handler.family

    override def close(reason: CloseReason): Unit = channel.close(reason)

    override def isClosed: Boolean = channel.isClosed

    override def update(): this.type = {
        if (handler == null)
            return this

        //asking server to give us his content version of our cache
        println(s"<$family> UPDATING CACHE $identifier")
        channel.send(WrappedPacket(family, LongPacket(identifier)), Relay.ServerIdentifier)
        val content = channel.nextResponse[ArrayObjectPacket].value
        println(s"<$family> RECEIVED UPDATED CONTENT FOR CACHE $identifier : ${content.mkString("Array(", ", ", ")")}")

        setCurrentContent(ScalaUtils.slowCopy(content))
        this
    }

    def handlePacket(packet: Packet, coords: PacketCoordinates): Unit

    def currentContent: Array[Any]

    protected def sendRequest(packet: Packet): Unit = channel.sendRequest(WrappedPacket(s"$family", WrappedPacket(identifier.toString, packet)))

    protected def setCurrentContent(content: Array[A]): Unit

}
