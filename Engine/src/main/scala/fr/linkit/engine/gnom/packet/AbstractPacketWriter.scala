package fr.linkit.engine.gnom.packet

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.tag.{EngineSelector, NameTag, NetworkFriendlyEngineTag, TagSelection}
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}

abstract class AbstractPacketWriter() extends PacketWriter {


    override def writePackets(packet: Packet, selection: TagSelection[NetworkFriendlyEngineTag]): Unit = {
        writePackets(packet, SimplePacketAttributes.empty, selection)
    }


    override def writePackets(packet: Packet, attributes: PacketAttributes, selection: TagSelection[NetworkFriendlyEngineTag]): Unit = {
        val targets = selector.listNameTags(selection)
        if (targets.isEmpty)
            throw new IllegalStateException("Cannot write packet to empty engines")
        writePackets(packet, attributes, targets)
    }

    protected def writePackets(packet: Packet, attributes: PacketAttributes, targets: Seq[NameTag]): Unit

}
