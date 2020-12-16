package fr.`override`.linkkit.api.system.security

import fr.`override`.linkkit.api.Relay

trait RelaySecurityManager {

    def hashBytes(raw: Array[Byte]): Array[Byte]

    def deHashBytes(hashed: Array[Byte]): Array[Byte]

    /**
     * Proceeds to two checks : before any load, then once the relay completely loaded and connected.
     *
     * @throws RelaySecurityException if the relay is invalid, this will cause relay to close automatically.
     * */
    def checkRelay(relay: Relay): Unit


}