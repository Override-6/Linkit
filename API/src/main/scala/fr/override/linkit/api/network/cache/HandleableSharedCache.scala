package fr.`override`.linkit.api.network.cache

import fr.`override`.linkit.api.packet.fundamental.RefPacket.ArrayObjectPacket
import fr.`override`.linkit.api.packet.fundamental.ValPacket.LongPacket
import fr.`override`.linkit.api.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}
import fr.`override`.linkit.api.utils.ScalaUtils
import org.jetbrains.annotations.Nullable

import scala.reflect.ClassTag

abstract class HandleableSharedCache[A <: Serializable : ClassTag](@Nullable handler: SharedCacheHandler,
                                                                   identifier: Long,
                                                                   channel: CommunicationPacketChannel) extends SharedCache with JustifiedCloseable {

    override val family: String = if (handler == null) "" else handler.family

    override def close(reason: CloseReason): Unit = channel.close(reason)

    override def isClosed: Boolean = channel.isClosed

    override def update(): this.type = {
        if (handler == null)
            return this

        val owner = handler.getOwner(identifier)
        //Simulating an handler content retrieval request
        channel.sendRequest(WrappedPacket(family, LongPacket(identifier)), owner)
        val content = channel.nextResponse[ArrayObjectPacket].value

        setCurrentContent(ScalaUtils.slowCopy(content))
        this
    }

    def handlePacket(packet: Packet, coords: PacketCoordinates): Unit

    def currentContent: Array[Any]

    protected def sendRequest(packet: Packet): Unit = channel.sendRequest(WrappedPacket(s"$family", WrappedPacket(identifier.toString, packet)))

    protected def setCurrentContent(content: Array[A]): Unit

}
