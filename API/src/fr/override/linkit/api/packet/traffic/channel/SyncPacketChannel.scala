package fr.`override`.linkit.api.packet.traffic.channel

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.{PacketWorkerThread, RelayWorkerThreadPool, relayWorkerExecution}
import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.api.packet.traffic._
import fr.`override`.linkit.api.packet.traffic.channel.AbstractPacketChannel
import fr.`override`.linkit.api.system.CloseReason


//TODO doc
class SyncPacketChannel protected(scope: ChannelScope,
                                  providable: Boolean) extends AbstractPacketChannel(scope)
        with PacketSender with PacketSyncReceiver {


    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingQueue[Packet] = {
        if (!providable)
            new LinkedBlockingQueue[Packet]()
        else {
            RelayWorkerThreadPool
                    .ifCurrentOrElse(_.newProvidedQueue, new LinkedBlockingQueue[Packet]())
        }
    }


    @relayWorkerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        injection.getPackets.foreach(queue.add)
        println(s"queue = $queue")
    }

    override def send(packet: Packet): Unit = scope.sendToAll(packet)

    override def sendTo(target: String, packet: Packet): Unit = {
        scope.sendTo(target, packet)
    }

    override def close(reason: CloseReason): Unit = {
        super.close(reason)
        queue.clear()
    }

    override def nextPacket(): Packet = {
        if (queue.isEmpty)
            PacketWorkerThread.checkNotCurrent()
        val packet = queue.take()
        packet
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