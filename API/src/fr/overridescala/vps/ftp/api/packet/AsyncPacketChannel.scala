package fr.overridescala.vps.ftp.api.packet

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.overridescala.vps.ftp.api.exceptions.PacketException
import fr.overridescala.vps.ftp.api.packet.AsyncPacketChannel.enqueue
import fr.overridescala.vps.ftp.api.packet.fundamental.TaskInitPacket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketChannel(override val ownerID: String,
                         override val connectedID: String,
                         override val channelID: Int,
                         handler: PacketChannelsHandler) extends PacketChannel.Async(handler) with PacketChannelManager {

    handler.registerManager(this)

    private var onPacketReceived: Packet => Unit = _

    override def sendPacket[P <: Packet](packet: P): Unit = {
        if (packet.isInstanceOf[TaskInitPacket])
            throw PacketException("can not send a TaskInitPacket.")
        enqueue(packet, coordinates, handler)
    }

    override def addPacket(packet: Packet): Unit = {
        Future {
            try {
                onPacketReceived(packet)
                handler.notifyPacketUsed(packet)
            } catch {
                case NonFatal(e) => e.printStackTrace()
            }
        }
    }

    def onPacketReceived(event: Packet => Unit): Unit = {
        onPacketReceived = event
    }


}

object AsyncPacketChannel {


    object UploadThread extends Thread {

        private[AsyncPacketChannel] val queue: BlockingDeque[PacketTicket] = new LinkedBlockingDeque()

        override def run(): Unit = {

            while (true) {
                try {
                    queue.takeLast().send()
                } catch {
                    case NonFatal(e) => e.printStackTrace()
                }
            }
        }

    }

    class PacketTicket(packet: Packet, coordinates: PacketCoordinates, handler: PacketChannelsHandler) {
        def send(): Unit =
            handler.sendPacket(packet, coordinates)
    }

    private[AsyncPacketChannel] def enqueue(packet: Packet, coords: PacketCoordinates, handler: PacketChannelsHandler): Unit =
        UploadThread.queue.addFirst(new PacketTicket(packet, coords, handler))

}
