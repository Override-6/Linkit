package fr.`override`.linkit.api.local.system.security

import fr.`override`.linkit.api.Relay

trait ConnectionSecurityManager {

    def hashBytes(raw: Array[Byte]): Unit

    def deHashBytes(hashed: Array[Byte]): Unit

    /**
     * Proceeds to two checks : before any load, then once the relay completely loaded and connected.
     *
     * @throws RelaySecurityException if the relay is invalid, this will cause relay to close automatically.
     * */
    def checkRelay(relay: Relay): Unit


}