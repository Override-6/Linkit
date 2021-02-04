package fr.`override`.linkit.api.`extension`.fragment

import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.traffic.dedicated.{CommunicationPacketChannel, DedicatedPacketSender}

abstract class RemoteFragment(protected val broadcaster: DedicatedPacketSender) extends ExtensionFragment {

    val nameIdentifier: String

    def handleRequest(packet: Packet, responseChannel: CommunicationPacketChannel): Unit

}
