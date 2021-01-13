package fr.`override`.linkit.api.`extension`.fragment

import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel

trait RemoteFragment extends ExtensionFragment {

    val nameIdentifier: String

    def handleRequest(packet: Packet, responseChannel: CommunicationPacketChannel): Unit

}
