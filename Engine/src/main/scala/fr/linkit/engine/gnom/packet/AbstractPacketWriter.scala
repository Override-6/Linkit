package fr.linkit.engine.gnom.packet

import fr.linkit.api.gnom.network.{EngineTag, GroupTag, Network, NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.packet.traffic.{PacketTraffic, PacketWriter}
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}

abstract class AbstractPacketWriter extends PacketWriter {

    protected def network: Network


    override def writePackets(packet: Packet, included: Seq[NetworkFriendlyEngineTag], excluded: Seq[NetworkFriendlyEngineTag]): Unit = {
        writePackets(packet, SimplePacketAttributes.empty, included, excluded)
    }

    override def writePackets(packet: Packet, attributes: PacketAttributes, included: Seq[NetworkFriendlyEngineTag], excluded: Seq[NetworkFriendlyEngineTag]): Unit = {
        val targets = listTargets(included, excluded) //lists all engines targeted by the given tags.
        writePackets(packet, attributes, targets)
    }

    private def listTargets(included: Seq[NetworkFriendlyEngineTag], excluded: Seq[NetworkFriendlyEngineTag]): Array[String] = {
        included.flatMap {
            case tag: NetworkFriendlyEngineTag => ???
            case tag: UniqueTag                => ???
            case GroupTag(name)                => ???
        }
        val engines = network.listEngines
        engines.filter(e => tags.exists(e.isTagged)).map(_.name).toArray
    }

    protected def writePackets(packet: Packet, attributes: PacketAttributes, targets: Array[String]): Unit

}
