package fr.`override`.linkit.api.concurency

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

abstract class RelayWorkerThread extends Thread {

    private val queue: BlockingDeque[() => Unit] = new LinkedBlockingDeque()

    def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = addActionToPerform(() => {
        makeHandle(packet, coordinates)
    })

    def addActionToPerform(action: () => Unit): Unit = queue.addLast(action)

    override def run(): Unit = queue.takeFirst().apply()

    abstract protected def makeHandle(packet: Packet, coordinates: PacketCoordinates): Unit

}
