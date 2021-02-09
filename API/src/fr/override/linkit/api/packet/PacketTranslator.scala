package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.{PacketException, UnexpectedPacketException}
import fr.`override`.linkit.api.packet.PacketUtils.wrap
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.system.SystemPacket
import fr.`override`.linkit.api.utils.{ScalaUtils, Utils, WrappedPacket}

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}


object PacketTranslator {
    val ChannelIDSeparator: Array[Byte] = "<channel>".getBytes
    val SenderSeparator: Array[Byte] = "<sender>".getBytes
    val TargetSeparator: Array[Byte] = "<target>".getBytes
}

class PacketTranslator(relay: Relay) { //Notifier is accessible from api to reduce parameter number in (A)SyncPacketChannel

    registerDefaults()

    def toPacketAndCoords(bytes: Array[Byte]): (Packet, PacketCoordinates) = {
        val (coordinates, coordsLength) = PacketUtils.getCoordinates(bytes)

        val customPacketBytes = bytes.slice(coordsLength, bytes.length)
        (toPacket(customPacketBytes), coordinates)
    }

    def toPacket(bytes: Array[Byte]): Packet = {
        if (bytes.length > relay.configuration.maxPacketLength)
            throw PacketException("Custom packet bytes length exceeded configuration limit")
        val packetTypeInt = ScalaUtils.toInt(bytes.slice(0, 4))
        val packetClassOpt = PacketKindBag.getKind(packetTypeInt)

        if (packetClassOpt.isEmpty)
            throw new UnexpectedPacketException(s"Could not find packet factory of identifier $packetTypeInt")

        val packetContentBytes = bytes.drop(4)
        PacketKindBag.getStrategy(packetTypeInt)
                .fold(Utils.deserialize(packetContentBytes): Packet)(_.deserialize(packetContentBytes): Packet)
    }

    def fromPacketAndCoords(packet: Packet, coordinates: PacketCoordinates): Array[Byte] = {
        wrap(fromPacketAndCoordsNoWrap(packet, coordinates))
    }

    def fromPacketAndCoordsNoWrap(packet: Packet, coordinates: PacketCoordinates): Array[Byte] = {
        val packetBytes = fromPacket(packet)
        val bytes = PacketUtils.getCoordinatesBytes(coordinates) ++ packetBytes
        relay.securityManager.hashBytes(bytes)
    }

    def fromPacket(packet: Packet): Array[Byte] = {
        val kind = packet.getClass
        val identifierOpt = PacketKindBag.getIdentifier(kind)
        if (identifierOpt.isEmpty)
            throw PacketException(s"Could not serialize packet : $kind is not registered.")

        val identifier = identifierOpt.get
        val serialized = PacketKindBag.getStrategy(identifier)
                .fold(Utils.serialize(packet))(_.serialize(packet))

        ScalaUtils.fromInt(identifier) ++ serialized
    }

    def register[P <: Packet : ClassTag](packetCompanion: PacketCompanion[P]): Unit = {
        val identifier = packetCompanion.identifier
        register(identifier, classTag[P].runtimeClass.asInstanceOf[Class[P]])

        packetCompanion match {
            case strategy: PacketSerialisationStrategy[P] =>
                PacketKindBag.putStrategy(identifier, strategy)
            case _ =>
        }
    }


    def register(identifier: Int, packetClass: Class[_ <: Packet]): Unit = {
        if (PacketKindBag.containsID(identifier))
            throw PacketException("This companion identifier is already registered !")

        PacketKindBag.add(identifier, packetClass)
    }

    private def registerDefaults(): Unit = {
        register(EmptyPacket.Companion)
        register(TaskInitPacket)
        register(SystemPacket)
        register(WrappedPacket)
        register(ValPacket)
        register(PairPacket)
        register(StringPacket)
        register(IntPacket)
    }

    //Not very calisthenic...
    object PacketKindBag {
        private val classMap = mutable.Map.empty[Int, Class[_ <: Packet]]
        private val idMap = mutable.Map.empty[Class[_ <: Packet], Int]
        private val strategyMap = mutable.Map.empty[Int, PacketSerialisationStrategy[Packet]]

        def add(identifier: Int, clazz: Class[_ <: Packet]): Unit = {
            classMap.put(identifier, clazz)
            idMap.put(clazz, identifier)
        }

        def getKind(identifier: Int): Option[Class[_ <: Packet]] = {
            classMap.get(identifier)
        }

        def getIdentifier(kind: Class[_ <: Packet]): Option[Int] = {
            idMap.get(kind)
        }

        def containsID(identifier: Int): Boolean = {
            classMap.contains(identifier)
        }

        def putStrategy(identifier: Int, strategy: PacketSerialisationStrategy[_ <: Packet]): Unit = {
            strategyMap.put(identifier, strategy.asInstanceOf[PacketSerialisationStrategy[Packet]])
        }

        def getStrategy(identifier: Int): Option[PacketSerialisationStrategy[Packet]] = {
            strategyMap.get(identifier)
        }

    }

}












