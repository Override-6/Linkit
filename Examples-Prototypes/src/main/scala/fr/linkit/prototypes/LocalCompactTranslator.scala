package fr.linkit.prototypes

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.packet.serialization.strategy.{SerialStrategy, StrategyHolder}
import fr.linkit.api.connection.packet.serialization.{PacketSerializationResult, PacketTransferResult, PacketTranslator, TransferInfo}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.core.connection.packet.serialization.{LazyPacketDeserializationResult, LazyPacketSerializationResult, RawObjectSerializer}

class LocalCompactTranslator extends PacketTranslator {

    override def translate(packetInfo: TransferInfo): PacketSerializationResult = {
        LazyPacketSerializationResult(packetInfo, () => RawObjectSerializer)
    }

    override def translate(bytes: Array[Byte]): PacketTransferResult = {
        LazyPacketDeserializationResult(bytes, () => RawObjectSerializer)
    }

    override def translateCoords(coords: PacketCoordinates, target: String = ""): Array[Byte] = {
        RawObjectSerializer.serialize(coords, false)
    }

    override def translateAttributes(attribute: PacketAttributes, target: String = ""): Array[Byte] = {
        RawObjectSerializer.serialize(attribute, false)
    }

    override def translatePacket(packet: Packet, target: String = ""): Array[Byte] = {
        RawObjectSerializer.serialize(packet, false)
    }

    override def updateCache(manager: SharedCacheManager): Unit = ()

    override val signature: Array[Byte] = Array[Byte](5)

    override def attachStrategy(strategy: SerialStrategy[_]): Unit = RawObjectSerializer.attachStrategy(strategy)

    override def drainAllStrategies(holder: StrategyHolder): Unit = RawObjectSerializer.drainAllStrategies(holder)
}
