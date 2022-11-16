package fr.linkit.engine.gnom.packet

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.tag.{NameTag, NetworkFriendlyEngineTag}
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}

abstract class AbstractPacketWriter extends PacketWriter {

    protected def network: Network


    override def writePackets(packet: Packet, selection: NetworkFriendlyEngineTag): Unit = {
        writePackets(packet, SimplePacketAttributes.empty, selection)
    }


    override def writePackets(packet: Packet, attributes: PacketAttributes, selection: NetworkFriendlyEngineTag): Unit = {
        writePackets(packet, attributes, network.listEngines(selection).map(_.nameTag))
    }

    protected def writePackets(packet: Packet, attributes: PacketAttributes, targets: Seq[NameTag]): Unit

}
