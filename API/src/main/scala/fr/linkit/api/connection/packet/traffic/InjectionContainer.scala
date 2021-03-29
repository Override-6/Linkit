package fr.linkit.api.connection.packet.traffic
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}

trait InjectionContainer {

    def makeInjection(packet: Packet, coordinates: DedicatedPacketCoordinates): PacketInjection

    def isProcessing(injection: PacketInjection): Boolean

}
