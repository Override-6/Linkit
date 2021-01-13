package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

abstract class AbstractRemoteFragment(controller: RemoteFragmentController) {

    def handleRequest(packet: Packet, coordinates: PacketCoordinates): Unit

    def sendRequest(packet: Packet): Unit = controller.sendRequest(packet)

    controller.addOnRequestReceived(handleRequest)

}
