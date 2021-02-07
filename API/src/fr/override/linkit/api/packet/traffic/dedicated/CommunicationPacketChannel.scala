package fr.`override`.linkit.api.packet.traffic.dedicated

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool
import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCompanion, PacketCoordinates}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

class CommunicationPacketChannel(writer: PacketWriter,
                                 connectedID: String,
                                 providable: Boolean)
        extends AbstractPacketChannel(writer, connectedID) {

    private val responses: BlockingQueue[Packet] = {
        if (!providable)
            new LinkedBlockingQueue[Packet]()
        else {
            RelayWorkerThreadPool
                    .ifCurrentOrElse(_.newProvidedQueue, new LinkedBlockingQueue[Packet]())
        }
    }


    private val requestListeners = new ConsumerContainer[(Packet, PacketCoordinates)]

    var enablePacketSending = true
    var packetTransform: Packet => Packet = p => p

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        packet match {
            case WrappedPacket(tag, subPacket) =>
                tag match {
                    case "res" => responses.add(subPacket)
                    case "req" => requestListeners.applyAll((subPacket, coordinates))
                    case _ =>
                }
            case _ =>
        }
    }

    def addRequestListener(action: (Packet, PacketCoordinates) => Unit): Unit =
        requestListeners += (tuple => action(tuple._1, tuple._2))

    def nextResponse[P <: Packet](factory: PacketCompanion[P]): P = nextResponse().asInstanceOf[P]

    def nextResponse(): Packet = responses.take()

    def sendResponse(packet: Packet): Unit = if (enablePacketSending) {
        writer.writePacket(WrappedPacket("res", packet), connectedID)
    }

    def sendRequest(packet: Packet): Unit = if (enablePacketSending) {
        writer.writePacket(WrappedPacket("req", packet), connectedID)
    }

}

object CommunicationPacketChannel extends PacketChannelFactory[CommunicationPacketChannel] {
    override def createNew(writer: PacketWriter,
                           connectedID: String): CommunicationPacketChannel = {
        new CommunicationPacketChannel(writer, connectedID, false)
    }

    def providable: PacketChannelFactory[CommunicationPacketChannel] = new CommunicationPacketChannel(_, _, true)

}
