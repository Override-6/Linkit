package fr.`override`.linkit.core.connection.packet.traffic.channel

import fr.`override`.linkit.api.connection.packet.Packet
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.connection.packet.traffic._
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.system.CloseReason
import fr.`override`.linkit.core.connection.packet.traffic
import fr.`override`.linkit.core.local.concurrency.{BusyWorkerPool, PacketWorkerThread}


//TODO doc
class SyncPacketChannel protected(scope: ChannelScope,
                                  providable: Boolean) extends traffic.channel.AbstractPacketChannel(scope)
        with PacketSender with PacketReceiver {


    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingQueue[Packet] = {
        if (!providable)
            new LinkedBlockingQueue[Packet]()
        else {
            BusyWorkerPool
                    .ifCurrentWorkerOrElse(_.newBusyQueue, new LinkedBlockingQueue[Packet]())
        }
    }


    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        injection.getPackets.foreach(queue.add)
    }

    override def send(packet: Packet): Unit = scope.sendToAll(packet)

    override def sendTo(packet: Packet, targets: String*): Unit = {
        scope.sendTo(packet, targets:_*)
    }

    override def close(reason: CloseReason): Unit = {
        super.close(reason)
        queue.clear()
    }

    override def nextPacket[P <: Packet]: P = {
        if (queue.isEmpty)
            PacketWorkerThread.checkNotCurrent()

        val packet = queue.take()
        packet.asInstanceOf[P]
    }


    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] would not wait
     * */
    override def haveMorePackets: Boolean =
        !queue.isEmpty

}


object SyncPacketChannel extends PacketInjectableFactory[SyncPacketChannel] {

    override def createNew(scope: ChannelScope): SyncPacketChannel = {
        new SyncPacketChannel(scope, false)
    }

    def providable: PacketInjectableFactory[SyncPacketChannel] = new SyncPacketChannel(_, true)

}