package fr.overridescala.vps.ftp.api.packet

import java.io.BufferedOutputStream
import java.net.Socket
import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.TaskInitInfo

/**
 * this class is the implementation of [[PacketChannel]] and [[PacketChannelManager]]
 *
 * @param socket the socket where packets will be sent
 * @param channelID the identifier attributed to this PacketChannel
 * @param ownerIdentifier the relay identifier of this channel owner
 *
 * @see [[PacketChannel]]
 * @see [[PacketChannelManager]]
 * */
class SimplePacketChannel(private val socket: Socket,
                          override val connectedIdentifier: String,
                          override val ownerIdentifier: String,
                          override val channelID: Int,
                          private val cache: PacketChannelManagerCache,
                          private val packetManager: PacketManager) extends PacketChannel with PacketChannelManager {

    cache.registerPacketChannel(this)

    private val out = new BufferedOutputStream(socket.getOutputStream)

    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingDeque[Packet] = new LinkedBlockingDeque(200)


    //TODO doc
    override def sendInitPacket(initInfo: TaskInitInfo): Unit = {
        val packet = TaskInitPacket.of(ownerIdentifier, channelID, initInfo)
        sendPacket(packet)
    }

    override def sendPacket[P <: Packet](packet: P): Unit = {
        out.write(packetManager.toBytes(packet))
        out.flush()
    }

    /**
     * Waits until a data packet is received and concerned about this task.
     *
     * @return the received packet
     * @see [[DataPacket]]
     * */
    override def nextPacket(): Packet = {
        queue.takeLast()
    }

    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
     * */
    override def haveMorePackets: Boolean =
        !queue.isEmpty

    /**
     * add a packet into the PacketChannel. the PacketChannel will stop waiting in [[PacketChannel#nextPacket]] if it where waiting for a packet
     *
     * @param packet the packet to add
     * @throws UnexpectedPacketException if the packet id not equals the channel task ID
     * */
    override def addPacket(packet: Packet): Unit = {
        queue.addFirst(packet)
    }

    override def close(): Unit = {
        queue.clear()
        cache.unregisterPaketChannel(channelID)
    }
}