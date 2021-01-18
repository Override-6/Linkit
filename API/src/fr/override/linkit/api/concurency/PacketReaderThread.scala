package fr.`override`.linkit.api.concurency

import fr.`override`.linkit.api.concurency.PacketReaderThread.packetReaderThreadGroup
import fr.`override`.linkit.api.exception.IllegalPacketWorkerLockException
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}

abstract class PacketReaderThread(relayWorkerThread: RelayWorkerThread) extends Thread(packetReaderThreadGroup, "Packet Read Worker") with JustifiedCloseable {

    private var open = true

    override def isClosed: Boolean = open

    override def run(): Unit = {
        while (open) {
            val (packet, coordinates) = readPacket()
            relayWorkerThread.handlePacket(packet, coordinates)
        }
    }

    override def close(reason: CloseReason): Unit = {
        closeSocket()
        open = false
    }

    protected def readPacket(): (Packet, PacketCoordinates)

    protected def sendPacket(packet: Packet, coordinates: PacketCoordinates)

    protected def closeSocket(): Unit

}

object PacketReaderThread {

    /**
     * Packet Worker Threads have to be registered in this ThreadGroup in order to throw an exception when a relay worker thread
     * is about to be locked by a monitor, that concern packet reception (example: lockers of BlockingQueues in PacketChannels)
     *
     * @see [[IllegalPacketWorkerLockException]]
     * */
    val packetReaderThreadGroup: ThreadGroup = new ThreadGroup("Relay Packet Readers")
}
