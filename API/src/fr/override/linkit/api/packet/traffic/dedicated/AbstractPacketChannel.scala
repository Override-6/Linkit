package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.packet.PacketCoordinates
import fr.`override`.linkit.api.packet.traffic.{DedicatedPacketInjectable, PacketTraffic}
import fr.`override`.linkit.api.system.CloseReason

abstract class AbstractPacketChannel(override val connectedID: String,
                                     override val identifier: Int,
                                     override val injector: PacketTraffic) extends PacketChannel with DedicatedPacketInjectable {

    override val ownerID: String = injector.ownerID

    override val coordinates: PacketCoordinates = PacketCoordinates(identifier, connectedID, ownerID)
    @volatile private var closed = true

    override def close(reason: CloseReason): Unit = closed = true

    override def isClosed: Boolean = closed
}
