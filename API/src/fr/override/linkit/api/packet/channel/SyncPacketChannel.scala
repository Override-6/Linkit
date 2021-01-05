package fr.`override`.linkit.api.packet.channel

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.packet.traffic.PacketSender
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason


//TODO doc
class SyncPacketChannel protected(override val connectedID: String,
                                  override val identifier: Int,
                                  sender: PacketSender) extends PacketChannel.Sync(sender) {


    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingDeque[Packet] = new LinkedBlockingDeque()

    /**
     * add a packet into the PacketChannel. the PacketChannel will stop waiting in [[PacketChannel#nextPacket]] if it where waiting for a packet
     *
     * @param packet the packet to add
     * @param coords the packet coordinates
     *
     * @throws IllegalArgumentException if the packet coordinates does not match with this channel coordinates
     * */
    override def injectPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        queue.addFirst(packet)
    }

    override def close(reason: CloseReason): Unit = {
        super.close(reason)
        queue.clear()
    }

    override def nextPacket(): Packet = {
        if (queue.isEmpty)
            sender.checkThread()
        val packet = queue.takeLast()
        //handler.notifyPacketUsed(packet, coordinates)
        packet
    }


    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] would not wait
     * */
    override def haveMorePackets: Boolean =
        !queue.isEmpty


}


object SyncPacketChannel extends PacketChannelFactory[SyncPacketChannel] {
    override val channelClass: Class[SyncPacketChannel] = classOf[SyncPacketChannel]

    override def createNew(sender: PacketSender, channelId: Int, connectedID: String): SyncPacketChannel = {
        new SyncPacketChannel(connectedID, channelId, sender)
    }
}
