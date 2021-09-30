package fr.linkit.api.gnom.persistence

import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.reference.NetworkObjectsLinker

import java.nio.ByteBuffer

trait PersistenceBundle {

    val buff       : ByteBuffer
    val coordinates: PacketCoordinates
    val config     : PersistenceConfig
    val gnol       : NetworkObjectsLinker

}
