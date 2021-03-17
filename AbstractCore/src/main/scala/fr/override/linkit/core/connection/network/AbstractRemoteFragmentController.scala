package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.skull.connection.packet.{Packet, PacketCoordinates}

abstract class AbstractRemoteFragmentController(controller: RemoteFragmentController) {

    def handleRequest(packet: Packet, coordinates: PacketCoordinates): Unit

    def sendRequest(packet: Packet): Unit = controller.sendRequest(packet)

    controller.addOnRequestReceived(handleRequest)

}
