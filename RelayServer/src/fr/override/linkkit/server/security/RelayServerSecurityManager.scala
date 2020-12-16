package fr.`override`.linkkit.server.security

import fr.`override`.linkkit.api.Relay
import fr.`override`.linkkit.api.system.security.RelaySecurityManager
import fr.`override`.linkkit.server.connection.ClientConnection

trait RelayServerSecurityManager extends RelaySecurityManager {

    def leaveConnected(connection: ClientConnection): Boolean

}

object RelayServerSecurityManager {

    class Default extends RelayServerSecurityManager{
        override def leaveConnected(connection: ClientConnection): Boolean = true

        override def hashBytes(raw: Array[Byte]): Array[Byte] = raw

        override def deHashBytes(hashed: Array[Byte]): Array[Byte] = hashed

        override def checkRelay(relay: Relay): Unit = ()
    }

    def default(): RelayServerSecurityManager = new Default

}
