package fr.overridescala.vps.ftp.api.packet

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.overridescala.vps.ftp.api.exceptions.{PacketException, UnexpectedPacketException}
import fr.overridescala.vps.ftp.api.packet.AsyncPacketChannel.send
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.task.TaskInitInfo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketChannel(override val ownerID: String,
                         override val connectedID: String,
                         override val channelID: Int,
                         val cache: PacketChannelManagerCache,
                         socket: DynamicSocket) extends PacketChannel.Async with PacketChannelManager {

    cache.registerPacketChannel(this)

    private var onPacketReceived: Packet => Unit = _

    override def sendPacket[P <: Packet](packet: P): Unit = {
        if (packet.isInstanceOf[TaskInitPacket])
            throw PacketException("can not send a TaskInitPacket.")
        send(packet, socket)
    }

    override def addPacket(packet: Packet): Unit = {
        Future {
            try {
                onPacketReceived(packet)
            } catch {
                case NonFatal(e) => e.printStackTrace()
            }
        }
    }

    override def sendInitPacket(initInfo: TaskInitInfo): Unit =
        throw new UnsupportedOperationException()

    override def close(): Unit =
        cache.unregisterPaketChannel(channelID)

    def setOnPacketReceived(event: Packet => Unit): Unit = {
        onPacketReceived = event
    }


}

object AsyncPacketChannel {

    private var uploader: UploadThread = _

    def launch(packetManager: PacketManager): Unit = {
        if (uploader == null)
            uploader = new UploadThread(packetManager)
        uploader.start()
    }

    class UploadThread(packetManager: PacketManager) extends Thread {
        val queue: BlockingDeque[PacketTicket] = new LinkedBlockingDeque()

        override def run(): Unit = {
            println("Async Upload Thread started !")
            while (true) {
                try {
                    queue.takeLast().send(packetManager)
                } catch {
                    case NonFatal(e) => e.printStackTrace()
                }
            }
        }

    }

    private[AsyncPacketChannel] class PacketTicket(packet: Packet, socket: DynamicSocket) {
        def send(packetManager: PacketManager): Unit = {
            socket.write(packetManager.toBytes(packet))
        }
    }

    private def send(packet: Packet, socket: DynamicSocket): Unit = {
        uploader.queue.addFirst(new PacketTicket(packet, socket))
    }

}
