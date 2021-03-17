package fr.`override`.linkit.core.connection.packet.traffic

import fr.`override`.linkit.skull.internal.system.security.RelaySecurityManager


class PacketReader(socket: DynamicSocket, securityManager: RelaySecurityManager) {

    def readNextPacketBytes(): Array[Byte] = synchronized {
        val nextLength = socket.readInt()
        if (nextLength == -1 || !socket.isOpen)
            return null

        val bytes = socket.read(nextLength)
        securityManager.deHashBytes(bytes)
    }

}
