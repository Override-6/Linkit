package fr.`override`.linkit.api.packet.traffic.channel

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.{RelayWorkerThreadPool, relayWorkerExecution}
import fr.`override`.linkit.api.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketInjectableFactory}
import fr.`override`.linkit.api.packet.{Packet, PacketCompanion, PacketCoordinates}
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

    private val requestListeners = new ConsumerContainer[(Packet, PacketCoordinates)]

    @relayWorkerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val packets = injection.getPackets
        val coordinates = injection.coordinates
        packets.foreach(_ match {
            case WrappedPacket(tag, subPacket) =>
                tag match {
                    case "res" => responses.add(subPacket)
                    case "req" => requestListeners.applyAll((subPacket, coordinates))
                    case _ =>
                }
            case _ =>
        })
    }

    def addRequestListener(action: (Packet, PacketCoordinates) => Unit): Unit =
        requestListeners += (tuple => action(tuple._1, tuple._2))

    def nextResponse[P <: Packet](factory: PacketCompanion[P]): P = nextResponse().asInstanceOf[P]

    def nextResponse(): Packet = responses.take()

    def sendResponse(packet: Packet): Unit = {
        scope.sendToAll(WrappedPacket("res", packet))
    }

    def sendRequest(packet: Packet): Unit = {
        scope.sendToAll(WrappedPacket("req", packet))
    }

    def sendResponse(packet: Packet, target: String): Unit = {
        scope.sendTo(target, WrappedPacket("res", packet))
    }

    def sendRequest(packet: Packet, target: String): Unit = {
        scope.sendTo(target, WrappedPacket("req", packet))
    }

}

object CommunicationPacketChannel extends PacketInjectableFactory[CommunicationPacketChannel] {
    override def createNew(scope: ChannelScope): CommunicationPacketChannel = {
        new CommunicationPacketChannel(scope, false)
    }

    def providable: PacketInjectableFactory[CommunicationPacketChannel] = new CommunicationPacketChannel(_, true)

}
