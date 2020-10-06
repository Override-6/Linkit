package fr.overridescala.vps.ftp.api.packet

import java.io.BufferedOutputStream
import java.net.Socket
import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.task.TaskInitInfo

import scala.collection.mutable
import scala.util.control.NonFatal

/**
 * this class is the implementation of [[PacketChannel]] and [[PacketChannelManager]]
 *
 * @param socket the socket where packets will be sent
 * @param taskID the taskID attributed to this PacketChannel
 * @param connectedRelayIdentifier the identifier of connected relay
 * @param ownerIdentifier the relay identifier of this channel owner
 *
 * @see [[PacketChannel]]
 * @see [[PacketChannelManager]]
 * */
class SimplePacketChannel(private val socket: Socket,
                          private val connectedRelayIdentifier: String,
                          private val ownerIdentifier: String,
                          override val taskID: Int)
        extends PacketChannel with PacketChannelManager {

    private val out = new BufferedOutputStream(socket.getOutputStream)

    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingDeque[DataPacket] = new LinkedBlockingDeque(200)
    private val listeners: mutable.Map[String, PacketEventTicket] = mutable.Map.empty

    /**
     * Builds a [[DataPacket]] from a header string and a content byte array,
     * then send it to the targeted Relay that complete the task
     *
     * @param header the packet header
     * @param content the packet content
     * */
    override def sendPacket(header: String, content: Array[Byte] = Array()): Unit = {
        val packet = DataPacket(taskID, header, connectedRelayIdentifier, ownerIdentifier, content)
        out.write(packet)
        out.flush()
    }

    //TODO doc
    override def sendInitPacket(initInfo: TaskInitInfo): Unit = {
        val packet = TaskInitPacket.of(ownerIdentifier, taskID, initInfo)
        out.write(packet)
        out.flush()
    }

    /**
     * Waits until a data packet is received and concerned about this task.
     *
     * @return the received packet
     * @see [[DataPacket]]
     * */
    override def nextPacket(): DataPacket = {
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
    override def addPacket(packet: DataPacket): Unit = {
        if (packet.taskID != taskID)
            throw UnexpectedPacketException(s"packet sessions differs ! ($packet)")
        if (handleListener(packet))
            queue.addFirst(packet)
    }

    /**
     * Targets a event when a specified packet with the targeted header is received.
     * @param uses the number of time the event can be fired
     * @param header the header to target.
     * @param onReceived the event to call
     * */
    override def putListener(header: String, onReceived: DataPacket => Unit, uses: Int, enqueuePacket: Boolean): Unit =
        listeners.put(header, PacketEventTicket(uses, enqueuePacket, onReceived))


    override def removeListener(header: String): Unit =
        listeners.remove(header)

    /**
     * @return true if the packet have to be enqueued
     * */
    private def handleListener(packet: DataPacket): Boolean = {
        val header = packet.header
        if (!listeners.contains(header))
            return true
        val ticket = listeners(header)
        if (ticket.uses == 0) {
            listeners.remove(header)
            return true
        }
        ticket.uses -= 0
        try {
            ticket.onReceived(packet)
        } catch {
            case NonFatal(ex) => ex.printStackTrace()
        }
        ticket.enqueuePacket
    }

    private case class PacketEventTicket(var uses: Int, enqueuePacket: Boolean, onReceived: DataPacket => Unit)

}