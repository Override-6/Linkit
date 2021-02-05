package fr.`override`.linkit.api.packet.traffic.dedicated

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCompanion, PacketCoordinates}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

class CommunicationPacketChannel(identifier: Int,
                                 connectedID: String,
                                 traffic: PacketTraffic,
                                 providable: Boolean)
        extends AbstractPacketChannel(connectedID, identifier, traffic) {

    private val responses: BlockingQueue[Packet] = {
        if (!providable)
            new LinkedBlockingQueue[Packet]()
        else {
            RelayWorkerThreadPool
                    .ifCurrentOrElse(_.newProvidedQueue, new LinkedBlockingQueue[Packet]())
        }
    }


    private val requestListeners = new ConsumerContainer[(Packet, PacketCoordinates)]
    private val normalPacketListeners = new ConsumerContainer[(Packet, PacketCoordinates)]

    var enablePacketSending = true
    var packetTransform: Packet => Packet = p => p

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        def injectAsNormal(): Unit = normalPacketListeners.applyAll((packet, coordinates))
        packet match {
            case wrapped: WrappedPacket =>
                val subPacket = wrapped.subPacket
                wrapped.tag match {
                    case "res" => responses.add(subPacket)
                    case "req" => requestListeners.applyAll((subPacket, coordinates))
                    case _ => injectAsNormal()
                }
            case _ => injectAsNormal()
        }
    }

    def addRequestListener(action: (Packet, PacketCoordinates) => Unit): Unit =
        requestListeners += (tuple => action(tuple._1, tuple._2))

    def nextResponse[P <: Packet](factory: PacketCompanion[P]): P = nextResponse().asInstanceOf[P]

    def nextResponse(): Packet = responses.take()

    def sendResponse(packet: Packet): Unit = if (enablePacketSending) {
        traffic.writePacket(WrappedPacket("res", packetTransform(packet)), coordinates)
    }

    def sendRequest(packet: Packet): Unit = if (enablePacketSending) {
        traffic.writePacket(WrappedPacket("req", packetTransform(packet)), coordinates)
    }

}

object CommunicationPacketChannel extends PacketChannelFactory[CommunicationPacketChannel] {
    override val channelClass: Class[CommunicationPacketChannel] = classOf[CommunicationPacketChannel]

    override def createNew(traffic: PacketTraffic, channelId: Int, connectedID: String): CommunicationPacketChannel =
        new CommunicationPacketChannel(channelId, connectedID, traffic, false)

    def providable: PacketChannelFactory[CommunicationPacketChannel] = providableFactory

    private val providableFactory: PacketChannelFactory[CommunicationPacketChannel] = new PacketChannelFactory[CommunicationPacketChannel] {
        override val channelClass: Class[CommunicationPacketChannel] = classOf[CommunicationPacketChannel]

        override def createNew(traffic: PacketTraffic, channelId: Int, connectedID: String): CommunicationPacketChannel = {
            new CommunicationPacketChannel(channelId, connectedID, traffic, true)
        }
    }
    
}
