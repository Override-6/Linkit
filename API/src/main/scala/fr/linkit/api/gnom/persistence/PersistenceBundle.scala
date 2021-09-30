package fr.linkit.api.gnom.persistence

import java.nio.ByteBuffer

import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.persistence.context.PersistenceConfig

trait PersistenceBundle {

    val buff       : ByteBuffer
    val coordinates: PacketCoordinates
    val config     : PersistenceConfig
    val

}
