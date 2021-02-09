package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.api.system.JustifiedCloseable

trait PacketInjectable extends JustifiedCloseable {
    val identifier: Int
    val ownerID: String
    /**
     * The traffic handler that is directly or not injecting the packets
     * */
    val traffic: PacketTraffic

    @relayWorkerExecution
    def inject(injection: PacketInjection): Unit

    def canInjectFrom(identifier: String): Boolean

    /**
     * Creates a PacketInjectable that will be handled by this packet channel. <br>
     * The packet channel can send and receive packets from the target.  <br>
     * In other words, this will create a sub channel that is bound to a specific relay.  <br>
     * If the sub channel receives a packet, the parent collector will handle the packet as well if the
     * children authorises it
     *
     * @param scopes the identifiers that the injectable will listen to
     * @param factory the factory that wil determine the kind of PacketInjectable that will be used.
     * @param transparent if true, the children is set as a 'transparent' injectable.
     *                    That means that if a packet is injected into the subChannel, the parent will
     *                    handle the packet as well
     * */
    def subInjectable[C <: PacketInjectable](scopes: Array[String],
                                             factory: PacketInjectableFactory[C],
                                             transparent: Boolean): C

    def subInjectable[C <: PacketInjectable](scope: String,
                                             factory: PacketInjectableFactory[C],
                                             transparent: Boolean = false): C = {
        subInjectable(Array(scope), factory, transparent)
    }

    /**
     * The children behaves and is handled like the scopped [[subInjectable]] method describes it.
     * The exception here is that it will be linked with the same scope of the parent.
     *
     * @param factory the factory that wil determine the kind of PacketInjectable that will be used.
     * @param transparent if true, the children is set as a 'transparent' injectable.
     *                    That means that if a packet is injected into the subChannel, the parent will
     *                    handle the packet as well
     * */
    def subInjectable[C <: PacketInjectable](factory: PacketInjectableFactory[C],
                                             transparent: Boolean): C

}
