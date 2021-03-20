package fr.`override`.linkit.core.connection.packet.traffic.channel

import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectableFactory, PacketInjection}
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool
import fr.`override`.linkit.core.local.utils.ConsumerContainer

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

class CommunicationPacketChannel(scope: ChannelScope,
                                 providable: Boolean)
        extends AbstractPacketChannel(scope) {

    private val responses: BlockingQueue[Packet] = {
        if (!providable)
            new LinkedBlockingQueue[Packet]()
        else {
            BusyWorkerPool
                    .ifCurrentWorkerOrElse(_.newBusyQueue, new LinkedBlockingQueue[Packet]())
        }
    }

    private val requestListeners = new ConsumerContainer[(Packet, DedicatedPacketCoordinates)]

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val packets = injection.getPackets
        val coordinates = injection.coordinates
        packets.foreach {
            case WrappedPacket(tag, subPacket) =>
                tag match {
                    case "res" => responses.add(subPacket)
                    case "req" => requestListeners.applyAll((subPacket, coordinates))
                    case _ =>
                }
            case _ =>
        }
        //println(s"<$identifier> responses = ${responses}")
    }


    def addRequestListener(action: (Packet, DedicatedPacketCoordinates) => Unit): Unit =
        requestListeners += (tuple => action(tuple._1, tuple._2))

    def nextResponse[P <: Packet]: P = responses.take().asInstanceOf[P]

    def sendResponse(packet: Packet): Unit = {
        scope.sendToAll(WrappedPacket("res", packet))
    }

    def sendRequest(packet: Packet): Unit = {
        scope.sendToAll(WrappedPacket("req", packet))
    }

    def sendResponse(packet: Packet, targets: String*): Unit = {
        scope.sendTo(WrappedPacket("res", packet), targets: _*)
    }

    def sendRequest(packet: Packet, targets: String*): Unit = {
        scope.sendTo(WrappedPacket("req", packet), targets: _*)
    }

}

object CommunicationPacketChannel extends PacketInjectableFactory[CommunicationPacketChannel] {
    override def createNew(scope: ChannelScope): CommunicationPacketChannel = {
        new CommunicationPacketChannel(scope, false)
    }

    def providable: PacketInjectableFactory[CommunicationPacketChannel] = new CommunicationPacketChannel(_, true)

}
