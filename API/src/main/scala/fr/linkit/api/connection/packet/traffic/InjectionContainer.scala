package fr.linkit.api.connection.packet.traffic
import fr.linkit.api.connection.packet.traffic.injection.{PacketInjection, PacketInjectionController}
import fr.linkit.api.connection.packet.{Bundle, DedicatedPacketCoordinates, Packet, PacketAttributes}

trait InjectionContainer {

    def makeInjection(packet: Packet, attributes: PacketAttributes, coordinates: DedicatedPacketCoordinates): PacketInjectionController

    def makeInjection(bundle: Bundle): PacketInjectionController

    def isInjecting(injection: PacketInjection): Boolean

}
