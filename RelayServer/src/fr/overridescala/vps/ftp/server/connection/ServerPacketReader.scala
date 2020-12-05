package fr.overridescala.vps.ftp.server.connection

import java.net.SocketException

import fr.overridescala.vps.ftp.api.exceptions.RelayException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.server.RelayServer
import org.jetbrains.annotations.Nullable

class ServerPacketReader(socket: DynamicSocket, server: RelayServer, @Nullable identifier: String) {

    @Nullable private val remoteConsoleErr = server.getConsoleErr(identifier).orNull
    private val packetReader = new PacketReader(socket, remoteConsoleErr)
    private val manager = server.connectionsManager
    private val packetManager = server.packetManager

    def nextPacket(onPacketReceived: (Packet, PacketCoordinates) => Unit): Unit = {
        try {
            listenNextConcernedPacket(onPacketReceived)
        } catch {
            case e: RelayException =>
                if (remoteConsoleErr != null)
                    remoteConsoleErr.reportExceptionSimplified(e)
                e.printStackTrace();
            case e: SocketException if e.getMessage == "Connection reset" =>
                val msg =
                    if (identifier == null) "socket connection reset while initialising connection."
                    else s"client '$identifier' disconnected."
                Console.err.println(msg)
        }
    }

    private def listenNextConcernedPacket(event: (Packet, PacketCoordinates) => Unit): Unit = {
        val bytes = packetReader.readNextPacketBytes()
        if (bytes == null)
            return

        val target = getTargetID(bytes)

        if (target == RelayServer.Identifier) { //check if packet concerns server
            val (packet, coordinates) = packetManager.toPacket(bytes)
            event(packet, coordinates)
            return
        }

        manager.deflectTo(bytes, target)
    }

    private def getTargetID(bytes: Array[Byte]): String =
        PacketUtils.cutString(PacketManager.SenderSeparator, PacketManager.TargetSeparator)(bytes)

}
