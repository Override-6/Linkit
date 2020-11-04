package fr.overridescala.vps.ftp.api.packet

import java.io.BufferedOutputStream
import java.net.Socket
import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.AsyncPacketChannel.send
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.task.TaskInitInfo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketChannel(override val ownerIdentifier: String,
                         override val connectedIdentifier: String,
                         override val channelID: Int,
                         val cache: PacketChannelManagerCache,
                         socket: Socket) extends PacketChannel.Async with PacketChannelManager {

    cache.registerPacketChannel(this)

    private var onPacketReceived: Packet => Unit = _
    private val out = new BufferedOutputStream(socket.getOutputStream)

    override def sendPacket[P <: Packet](packet: P): Unit = {
        if (packet.isInstanceOf[TaskInitPacket])
            throw UnexpectedPacketException("can not send a TaskInitPacket.")
        send(packet, out)
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
        sendPacket(TaskInitPacket.of(ownerIdentifier, channelID, initInfo))

    override def close(): Unit =
        cache.unregisterPaketChannel(channelID)

    def setOnPacketReceived(event: Packet => Unit): Unit = {
        onPacketReceived = event
    }


}

object AsyncPacketChannel {

    private var uploader: UploadThread = _

    def launchThreadIfNot(packetManager: PacketManager): Unit = {
        if (uploader == null)
            uploader = new UploadThread(packetManager)
    }

    class UploadThread(packetManager: PacketManager) extends Thread {
        val queue: BlockingDeque[PacketTicket] = new LinkedBlockingDeque()

        override def run(): Unit = {
            println(getClass.getCanonicalName + " started !")
            while (true) {
                try {
                    queue.pollLast().send(packetManager)
                } catch {
                    case NonFatal(e) => e.printStackTrace()
                }
            }
        }

    }

    private[AsyncPacketChannel] class PacketTicket(packet: Packet, out: BufferedOutputStream) {
        def send(packetManager: PacketManager): Unit = {
            out.write(packetManager.toBytes(packet))
        }
    }

    private def send(packet: Packet, owner: BufferedOutputStream): Unit = {
        uploader.queue.addFirst(new PacketTicket(packet, owner))
    }

}
