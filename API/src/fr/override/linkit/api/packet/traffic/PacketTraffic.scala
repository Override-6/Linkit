package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.traffic.dedicated.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.traffic.global.{PacketCollector, PacketCollectorFactory}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.JustifiedCloseable

import scala.reflect.ClassTag


trait PacketTraffic extends JustifiedCloseable {

    val relayID: String

    def register(dedicated: DedicatedPacketInjectable): Unit

    def register(global: GlobalPacketInjectable): Unit

    def openChannel[C <: PacketChannel : ClassTag](injectableID: Int, targetID: String, factory: PacketChannelFactory[C]): C

    def openCollector[C <: PacketCollector : ClassTag](injectableID: Int, factory: PacketCollectorFactory[C]): C

    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit

    def isRegistered(identifier: Int, boundTarget: String): Boolean

    def newWriter(identifier: Int, transform: Packet => Packet = p => p): PacketWriter

}

object PacketTraffic {
    val SystemChannel = 1
    val RemoteConsoles = 2
}
