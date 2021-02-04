package fr.`override`.linkit.api.packet.traffic.global

import fr.`override`.linkit.api.packet.traffic.GlobalPacketInjectable
import fr.`override`.linkit.api.packet.traffic.dedicated.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.system.CloseReason

trait PacketCollector extends GlobalPacketInjectable {

    override val ownerID: String

    override def close(reason: CloseReason): Unit

    override def isClosed: Boolean

    /**
     * Creates a PacketChannel that will be handled by this packet collector. <br>
     * The packet channel can send and receive packets from the target.  <br>
     * In other words, this will create a sub channel that is bound to a specific relay.  <br>
     * If the sub channel receives a packet, the parent collector will handle the packet as well if the
     * children authorises it
     *
     * @param targetID the bounded relay identifier that the sub channel will focus
     * @param transparent if ture, the children is set as 'transparent',
     *                    that means that if a packet is injected into the subChannel, the parent will
     *                    handle the packet as well
     * @param factory the factory that wil determine the kind of PacketChannel that will be used.
     * */
    def subChannel[C <: PacketChannel](targetID: String, factory: PacketChannelFactory[C], transparent: Boolean): C

}
