package fr.overridescala.linkkit.api.packet

import fr.overridescala.linkkit.api.exception.RelayCloseException
import fr.overridescala.linkkit.api.system.security.RelaySecurityManager


class PacketReader(socket: DynamicSocket, securityManager: RelaySecurityManager) {

    //TODO exceptions catches
    def readNextPacketBytes(): Array[Byte] = synchronized {
        val nextLength = nextPacketLength()
        if (nextLength == -1 || !socket.isOpen)
            return null

        val bytes = socket.read(nextLength)
        securityManager.deHashBytes(bytes)
    }

    private def nextPacketLength(): Int = {
        try {
            val int = new String(socket.read(1))
            val packetLengthFlagLength = Integer.parseInt(int, 16)
            Integer.parseInt(new String(socket.read(packetLengthFlagLength)))
        } catch {
            case _: RelayCloseException => -1
        }
    }


}
