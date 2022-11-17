package fr.linkit.engine.gnom.packet

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.tag.{NameTag, NetworkFriendlyEngineTag, TagSelection}
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}

abstract class AbstractPacketWriter() extends PacketWriter {

    protected def selector: Network

    override def writePackets(packet: Packet, selection: TagSelection[NetworkFriendlyEngineTag]): Unit = {
        writePackets(packet, SimplePacketAttributes.empty, selection)
    }


    override def writePackets(packet: Packet, attributes: PacketAttributes, selection: TagSelection[NetworkFriendlyEngineTag]): Unit = {
        writePackets(packet, attributes, selector.listEngines(selection).map(_.nameTag))
    }

    protected def writePackets(packet: Packet, attributes: PacketAttributes, targets: Seq[NameTag]): Unit

}
