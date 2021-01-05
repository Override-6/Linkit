package fr.`override`.linkit.server.connection

import java.net.SocketException

import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.traffic.{DynamicSocket, PacketReader}
import fr.`override`.linkit.server.RelayServer
import org.jetbrains.annotations.Nullable

class ConnectionPacketReader(socket: DynamicSocket, server: RelayServer, @Nullable identifier: String) {

    private val packetReader = new PacketReader(socket, server.securityManager)
    private val manager = server.connectionsManager
    private val packetTranslator = server.packetTranslator

    //TODO exceptions catches
    def nextPacket(onPacketReceived: (Packet, PacketCoordinates) => Unit): Unit = {
        try {
            nextConcernedPacket(onPacketReceived)
        } catch {
            case e: SocketException if e.getMessage == "Connection reset" =>
                val msg =
                    if (identifier == null) "socket connection reset while initialising connection."
                    else s"client '$identifier' disconnected."
                Console.err.println(msg)
        }
    }

    private def nextConcernedPacket(event: (Packet, PacketCoordinates) => Unit): Unit = {
        val bytes = packetReader.readNextPacketBytes()
        //NETWORK-DEBUG-MARK
        //println(s"received : ${new String(bytes)}")
        if (bytes == null) {
            return
        }
        val target = getTargetID(bytes)

        if (target == server.identifier) { //check if the packet concerns server
            val (packet, coordinates) = packetTranslator.toPacket(bytes)
            event(packet, coordinates)
            return
        }
        //println("Deflected " + new String(bytes))
        manager.deflectTo(bytes, target)
    }

    private def getTargetID(bytes: Array[Byte]): String =
        PacketUtils.cutString(PacketTranslator.SenderSeparator, PacketTranslator.TargetSeparator)(bytes)

}
