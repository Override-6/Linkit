package fr.`override`.linkit.server.security

import fr.`override`.linkit.server.connection.ClientConnection
import fr.`override`.linkit.skull.Relay
import fr.`override`.linkit.skull.internal.system.security.RelaySecurityManager

trait RelayServerSecurityManager extends RelaySecurityManager {

    def canConnect(connection: ClientConnection): Boolean

}

object RelayServerSecurityManager {

    class Default extends RelayServerSecurityManager{
        override def canConnect(connection: ClientConnection): Boolean = true

        override def hashBytes(raw: Array[Byte]): Array[Byte] = raw

        override def deHashBytes(hashed: Array[Byte]): Array[Byte] = hashed

        override def checkRelay(relay: Relay): Unit = ()
    }

    def default(): RelayServerSecurityManager = new Default

}
