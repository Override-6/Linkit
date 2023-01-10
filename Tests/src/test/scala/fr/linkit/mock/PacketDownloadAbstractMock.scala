package fr.linkit.mock

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.gnom.persistence.PacketDownload

import java.nio.ByteBuffer

abstract class PacketDownloadAbstractMock(val ordinal: Int, val coords: PacketCoordinates) extends PacketDownload {
    private var deserialized      = false
    override val buff: ByteBuffer = null

    private var injected: Boolean = false

    override def makeDeserialization(): Unit = {
        onDeserialize()
        deserialized = true
    }

    override def isDeserialized: Boolean = deserialized

    override def isInjected: Boolean = injected

    override def informInjected: Unit = injected = true

    override def attributes: PacketAttributes = null

    override def packet: Packet = null

    def onDeserialize(): Unit
}
