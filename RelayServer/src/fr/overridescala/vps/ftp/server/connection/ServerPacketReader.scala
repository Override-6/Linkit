package fr.overridescala.vps.ftp.server.connection

import java.net.SocketException

import com.sun.istack.internal.Nullable
import fr.overridescala.vps.ftp.api.`extension`.packet.{PacketManager, PacketUtils}
import fr.overridescala.vps.ftp.api.exceptions.RelayException
import fr.overridescala.vps.ftp.api.packet.{DynamicSocket, Packet, PacketReader}
import fr.overridescala.vps.ftp.server.RelayServer

class ServerPacketReader(socket: DynamicSocket, server: RelayServer, @Nullable identifier: String) {

    private val packetReader = new PacketReader(socket)
    private val manager = server.connectionsManager
    private val packetManager = server.packetManager

    def nextPacket(onPacketReceived: Packet => Unit): Unit = {
        try {
            listenNextConcernedPacket(onPacketReceived)
        } catch {
            case e: RelayException => e.printStackTrace();
            case e: SocketException if e.getMessage == "Connection reset" =>
                val msg =
                    if (identifier == null) "socket connection reset while initialising connection."
                    else s"client '$identifier' disconnected."
                Console.err.println(msg)
        }
    }

    private def listenNextConcernedPacket(event: Packet => Unit): Unit = {
        val bytes = packetReader.readNextPacketBytes()
        if (bytes == null)
            return

        val target = getTargetID(bytes)

        if (target == RelayServer.Identifier) { //check if packet concerns server
            val packet = packetManager.toPacket(bytes)
            event(packet)
            return
        }

        manager.deflectTo(bytes, target)
    }

    private def getTargetID(bytes: Array[Byte]): String =
        PacketUtils.cutString(PacketManager.SenderSeparator, PacketManager.TargetSeparator)(bytes)

}
