package fr.`override`.linkit.skull.internal.system.security

import fr.`override`.linkit.skull.Relay

trait RelaySecurityManager {

    def hashBytes(raw: Array[Byte]): Unit

    def deHashBytes(hashed: Array[Byte]): Unit

    /**
     * Proceeds to two checks : before any load, then once the relay completely loaded and connected.
     *
     * @throws RelaySecurityException if the relay is invalid, this will cause relay to close automatically.
     * */
    def checkRelay(relay: Relay): Unit


}