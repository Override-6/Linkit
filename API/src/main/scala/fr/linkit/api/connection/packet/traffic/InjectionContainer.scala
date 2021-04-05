package fr.linkit.api.connection.packet.traffic
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}

trait InjectionContainer {

    def makeInjection(packet: Packet, attributes: PacketAttributes, coordinates: DedicatedPacketCoordinates): PacketInjection

    def isProcessing(injection: PacketInjection): Boolean

}
