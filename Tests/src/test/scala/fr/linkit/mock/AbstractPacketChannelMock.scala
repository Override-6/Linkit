package fr.linkit.mock

import fr.linkit.api.gnom.network.tag.NameTag
import fr.linkit.api.gnom.packet.ChannelPacketBundle
import fr.linkit.api.gnom.packet.channel.PacketChannel
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.api.internal.system.Reason
import fr.linkit.engine.gnom.packet.AbstractAttributesPresence
import fr.linkit.engine.gnom.referencing.presence.SystemNetworkObjectPresence

abstract class AbstractPacketChannelMock extends AbstractAttributesPresence with PacketChannel {
    override val ownerTag: NameTag       = NameTag("NONE")
    override val traffic : PacketTraffic = null

    override def storeBundle(bundle: ChannelPacketBundle): Unit = throw new UnsupportedOperationException()

    override def injectStoredBundles(): Unit = throw new UnsupportedOperationException()


    override def presence: NetworkObjectPresence = SystemNetworkObjectPresence

    override def close(reason: Reason): Unit = ()

    override def isClosed: Boolean = false

    override val trafficPath: Array[Int] = Array(-1)

    override def reference: TrafficReference = TrafficReference / -1
}
