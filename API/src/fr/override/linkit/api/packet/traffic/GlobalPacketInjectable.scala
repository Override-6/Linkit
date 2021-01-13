package fr.`override`.linkit.api.packet.traffic

/**
 * This trait does not add any behaviour or data compared to PacketInjectable, it only determines if the
 * handled packets that will be injected are global (shared between multiple relays).
 * The opposite would be [[DedicatedPacketInjectable]]
 *
 * @see [[DedicatedPacketInjectable]]
 * */
trait GlobalPacketInjectable extends PacketInjectable {

}
