package fr.linkit.api.connection.packet

import fr.linkit.api.connection.packet.serialization.{PacketTransferResult, TransferInfo}

case class PacketInfo(coords: PacketCoordinates, attributes: PacketAttributes, packet: Packet) {

}

object PacketInfo {

    def apply(result: PacketTransferResult): PacketInfo = {
        new PacketInfo(result.coords, result.attributes, result.packet)
    }

    def apply(transferInfo: TransferInfo): PacketInfo = {
        new PacketInfo(transferInfo.coords, transferInfo.attributes, transferInfo.packet)
    }
}
