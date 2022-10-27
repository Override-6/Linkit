package fr.linkit.engine.gnom.packet

import fr.linkit.api.gnom.network.{EngineTag, Network}
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}

abstract class AbstractPacketWriter extends PacketWriter {

    protected def network: Network

    override def writePackets(packet: Packet, targetIDs: Array[EngineTag], excludeTargets: Boolean): Unit = {
        writePackets(packet, SimplePacketAttributes.empty, targetIDs, excludeTargets)
    }

    override def writePackets(packet: Packet, attributes: PacketAttributes, targetTags: Array[EngineTag], excludeTargets: Boolean): Unit = {
        val targets = listTargets(targetTags) //lists all engines targeted by the given tags.
        if (excludeTargets) writePacketsExclude(packet, attributes, targets)
        else writePacketsInclude(packet, attributes, targets)
    }

    private def listTargets(tags: Array[EngineTag]): Array[String] = {
        val engines = network.listEngines
        engines.filter(e => tags.exists(e.isTagged)).map(_.name).toArray
    }

    protected def writePacketsInclude(packet: Packet, attributes: PacketAttributes, includedTags: Array[String]): Unit

    protected def writePacketsExclude(packet: Packet, attributes: PacketAttributes, discardedIDs: Array[String]): Unit

}
