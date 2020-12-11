package fr.overridescala.linkkit.server.security

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.api.system.security.RelaySecurityManager
import fr.overridescala.linkkit.server.connection.ClientConnection

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
