package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.packet.PacketCoordinates
import fr.`override`.linkit.api.packet.traffic.{DedicatedPacketInjectable, PacketTraffic, PacketWriter}
import fr.`override`.linkit.api.system.CloseReason

abstract class AbstractPacketChannel(writer: PacketWriter,
                                     override val connectedID: String) extends PacketChannel with DedicatedPacketInjectable {

    override val ownerID: String = writer.relayID
    override val identifier: Int = writer.identifier
    override val traffic: PacketTraffic = writer.traffic

    override val coordinates: PacketCoordinates = PacketCoordinates(identifier, connectedID, ownerID)
    @volatile private var closed = true

    override def close(reason: CloseReason): Unit = closed = true

    override def isClosed: Boolean = closed
}
