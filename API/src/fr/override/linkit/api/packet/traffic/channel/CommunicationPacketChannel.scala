package fr.`override`.linkit.api.packet.traffic.channel

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.{RelayWorkerThreadPool, relayWorkerExecution}
import fr.`override`.linkit.api.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketInjectableFactory}
import fr.`override`.linkit.api.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.utils.ConsumerContainer

class CommunicationPacketChannel(scope: ChannelScope,
                                 providable: Boolean)
        extends AbstractPacketChannel(scope) {

    private val responses: BlockingQueue[Packet] = {
        if (!providable)
            new LinkedBlockingQueue[Packet]()
        else {
            RelayWorkerThreadPool
                    .ifCurrentOrElse(_.newProvidedQueue, new LinkedBlockingQueue[Packet]())
        }
    }

    private val requestListeners = new ConsumerContainer[(Packet, DedicatedPacketCoordinates)]

    @relayWorkerExecution
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
        println(s"Injected packet $packets")
        println(s"Responses $responses")
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
